/*
 * Copyright (c) InspireNXE <https://github.com/InspireNXE>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.inspirenxe.strata.core.frame;

import org.inspirenxe.strata.api.UiThread;
import org.inspirenxe.strata.spi.Platform;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * The {@link UiThread} of one platform, shared by every host made with it; {@link #of} finds or makes it.
 *
 * <p>With the platform's {@link Platform#uiExecutor()}, tasks go to that executor, and after each one runs every
 * host of the platform is asked for a frame. Without one, tasks wait in a queue that the first host of the platform to
 * flush, lay out or render runs through {@link #runQueued()}, and queuing a task asks every host for a frame. Either
 * way a task that throws is logged as an error and the next one runs.</p>
 *
 * <p>With the platform's {@link Platform#uiThreadCheck()}, that test decides which thread is current. Without one,
 * the first thread that calls {@link #record()} becomes the UI thread for good; until then {@link #isCurrent()} is
 * false and {@link #checkCurrent()} lets every thread pass.</p>
 */
public final class UiThreadImpl implements UiThread {
    private static final System.Logger LOGGER = System.getLogger(UiThreadImpl.class.getName());
    // Weak, so a platform no host uses any more can go
    private static final Map<Platform, UiThreadImpl> BY_PLATFORM = new WeakHashMap<>();

    private final Executor executor;
    private final BooleanSupplier check;
    private final Queue<Runnable> queue;
    private final Set<FrameRequests> hosts;
    private volatile Thread thread;

    private UiThreadImpl(final Platform platform) {
        this.executor = platform.uiExecutor().orElse(null);
        this.check = platform.uiThreadCheck().orElse(null);
        this.queue = new ConcurrentLinkedQueue<>();
        this.hosts = Collections.newSetFromMap(new WeakHashMap<>());
    }

    /**
     * Returns the UI thread of a platform, made the first time it is asked for, reading the platform's
     * {@link Platform#uiExecutor()} and {@link Platform#uiThreadCheck()} then. Platforms are told apart by
     * {@code equals}. Any thread may call this.
     *
     * @param platform the platform
     * @return its UI thread, the same one every time
     */
    public static UiThreadImpl of(final Platform platform) {
        Objects.requireNonNull(platform, "platform");
        synchronized (BY_PLATFORM) {
            return BY_PLATFORM.computeIfAbsent(platform, UiThreadImpl::new);
        }
    }

    /**
     * Notes the calling thread as the UI thread if the platform has no test of its own and no thread was noted yet.
     * Hosts call this whenever they dispatch input, flush, lay out or render.
     */
    public void record() {
        if (this.check == null && this.thread == null) {
            this.thread = Thread.currentThread();
        }
    }

    /**
     * Adds a host whose frame requests a task should wake. The host is held weakly.
     *
     * @param frames the host's frame requests
     */
    public void register(final FrameRequests frames) {
        synchronized (this.hosts) {
            this.hosts.add(Objects.requireNonNull(frames, "frames"));
        }
    }

    @Override
    public boolean isCurrent() {
        return this.check != null ? this.check.getAsBoolean() : this.thread == Thread.currentThread();
    }

    @Override
    public void checkCurrent() {
        final boolean ok;
        if (this.check != null) {
            ok = this.check.getAsBoolean();
        } else {
            final Thread ui = this.thread;
            ok = ui == null || ui == Thread.currentThread();
        }
        if (!ok) {
            throw new IllegalStateException("Called on " + Thread.currentThread().getName() + ", but components "
                    + "shown by a host must be used on its UI thread" + (this.thread == null ? ""
                    : " (" + this.thread.getName() + ")") + ". Hand the work over with UiThread.execute, or set a State");
        }
    }

    @Override
    public void execute(final Runnable task) {
        Objects.requireNonNull(task, "task");
        if (this.executor != null) {
            this.executor.execute(() -> {
                UiThreadImpl.run(task);
                this.wakeHosts();
            });
        } else {
            this.queue.add(task);
            this.wakeHosts();
        }
    }

    @Override
    public <T> CompletableFuture<T> submit(final Supplier<? extends T> task) {
        Objects.requireNonNull(task, "task");
        final CompletableFuture<T> future = new CompletableFuture<>();
        this.execute(() -> {
            try {
                future.complete(task.get());
            } catch (final RuntimeException e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    /**
     * Returns whether tasks wait in core's queue, which only happens when the platform has no executor.
     *
     * @return true if {@link #runQueued()} would run something
     */
    public boolean hasQueued() {
        return !this.queue.isEmpty();
    }

    /**
     * Runs the tasks that were queued when the call began, in order, on the calling thread, which must be the UI
     * thread. Tasks those tasks queue wait for the next call. Does nothing when the platform has an executor.
     */
    public void runQueued() {
        if (this.queue.isEmpty()) {
            return;
        }
        for (int left = this.queue.size(); left > 0; left--) {
            final Runnable task = this.queue.poll();
            if (task == null) {
                return;
            }
            UiThreadImpl.run(task);
        }
    }

    private static void run(final Runnable task) {
        try {
            task.run();
        } catch (final RuntimeException e) {
            LOGGER.log(System.Logger.Level.ERROR, "A task handed to the UI thread threw", e);
        }
    }

    private void wakeHosts() {
        final FrameRequests[] targets;
        synchronized (this.hosts) {
            targets = this.hosts.toArray(new FrameRequests[0]);
        }
        for (final FrameRequests frames : targets) {
            frames.request();
        }
    }
}
