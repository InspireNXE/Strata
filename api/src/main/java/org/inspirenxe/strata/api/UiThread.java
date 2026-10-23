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
package org.inspirenxe.strata.api;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * The thread that dispatches a host's input and lays it out. Components, overlays and handlers belong to it: once a
 * component is attached to a host, adding or removing children, any setter that lays out, opening or closing an
 * overlay, {@code onEvent}, {@code subscribe} and {@code bind} must run on it, and Strata throws
 * {@link IllegalStateException} when they do not. A {@link org.inspirenxe.strata.api.state.State} may be set from
 * any thread, and this executor is how other work gets there:
 *
 * <pre>{@code
 * Strata.uiThread().execute(() -> {
 *     try (CauseFrame frame = ui.causes().pushFrame().put(CauseKeys.ORIGIN, new Origin.Remote(update.seller()))) {
 *         listings.setItems(update.listings());
 *     }
 * });
 * }</pre>
 *
 * <p>A tree built on another thread may be added on the UI thread, since only attached components are checked.</p>
 *
 * <p>Tasks run in the order they were given, each with no cause frame open, so what a task changes reports
 * {@code Origin.CODE} unless it opens a frame of its own. Every method may be called from any thread. The hosts of
 * one platform share one UI thread.</p>
 *
 * <p>Get it from {@link ComponentFactory#uiThread()} or {@link Strata#uiThread()}. Do not implement this
 * interface.</p>
 */
public interface UiThread extends Executor {

    /**
     * Returns whether the calling thread is the UI thread. Before the platform's first frame or input, when the
     * engine has not named its thread, Strata does not know it yet and this returns false.
     *
     * @return true on the UI thread
     */
    boolean isCurrent();

    /**
     * Runs a task on the UI thread, after the tasks given before it. Called on the UI thread it still queues the
     * task, which runs after the current dispatch, so code behaves the same from any thread; check
     * {@link #isCurrent()} first to run inline. A task that throws is logged through {@link System.Logger} and the
     * next one runs. Queuing a task asks the platform's hosts for a frame.
     *
     * @param task the task
     */
    @Override
    void execute(Runnable task);

    /**
     * Runs a task on the UI thread as {@link #execute} does and completes a future with what it returns. A task that
     * throws completes the future exceptionally and is not logged.
     *
     * @param <T> the result type
     * @param task the task
     * @return a future completed on the UI thread with the task's result, which may be null
     */
    <T> CompletableFuture<T> submit(Supplier<? extends T> task);

    /**
     * Checks that the calling thread is the UI thread. Strata calls this where a call from another thread would
     * corrupt a host's state. Before Strata knows the thread, as {@link #isCurrent()} describes, every thread
     * passes.
     *
     * @throws IllegalStateException if the calling thread is not the UI thread
     */
    void checkCurrent();
}
