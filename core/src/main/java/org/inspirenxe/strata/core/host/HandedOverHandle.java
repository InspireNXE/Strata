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
package org.inspirenxe.strata.core.host;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.UiThread;
import org.inspirenxe.strata.api.screen.Screen;
import org.inspirenxe.strata.api.screen.ScreenHandle;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.theme.ThemeRef;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The handle a screen stack returns when a screen is opened from a thread other than the UI thread (design 4.42).
 * The opening is handed to the UI thread with {@link UiThread#execute}, and the handle answers at once.
 *
 * <ul>
 *     <li>{@link #invalidate()}, {@link #close()}, {@link #setTitle} and {@link #open} hand themselves to the UI thread
 *     too, in the order they were called, and reach the real handle once the screen has opened. Called on the UI
 *     thread after the screen opened, they run at once.</li>
 *     <li>{@link #close()} before the screen opened cancels the opening, so the screen never builds.</li>
 *     <li>{@link #requestClose()} before the screen opened cancels it as {@link #close()} does and returns true. After
 *     it opened, on the UI thread it asks the screen; on another thread it hands the request over and returns false,
 *     since the answer is not known yet.</li>
 *     <li>{@link #isOpen()} is true while the opening waits, false once it was canceled or failed, and the real
 *     handle's answer after that.</li>
 *     <li>{@link #factory()} throws {@link IllegalStateException} until the screen has opened, since its host does
 *     not exist before then.</li>
 * </ul>
 *
 * <p>An opening that throws on the UI thread, such as for a theme that lacks a key, is logged by the UI thread, and
 * the handle then counts as closed.</p>
 */
final class HandedOverHandle implements ScreenHandle {
    private final UiThread ui;
    // Guarded by this
    private final List<Consumer<ScreenHandle>> waiting;
    private ScreenHandle opened;
    private boolean ended;

    /**
     * Hands an opening to the UI thread.
     *
     * @param ui the UI thread
     * @param opener opens the screen on the UI thread and returns its real handle
     */
    HandedOverHandle(final UiThread ui, final Supplier<ScreenHandle> opener) {
        this.ui = ui;
        this.waiting = new ArrayList<>();
        ui.execute(() -> this.openNow(opener));
    }

    private void openNow(final Supplier<ScreenHandle> opener) {
        synchronized (this) {
            if (this.ended) {
                return;
            }
        }
        final ScreenHandle handle;
        try {
            handle = opener.get();
        } catch (final RuntimeException e) {
            synchronized (this) {
                this.ended = true;
                this.waiting.clear();
            }
            throw e;
        }
        final List<Consumer<ScreenHandle>> actions;
        synchronized (this) {
            this.opened = handle;
            actions = List.copyOf(this.waiting);
            this.waiting.clear();
        }
        for (final Consumer<ScreenHandle> action : actions) {
            action.accept(handle);
        }
    }

    /**
     * Runs an action on the real handle on the UI thread: now if the screen is open and this is the UI thread,
     * later otherwise. Does nothing once the opening was canceled or failed.
     */
    private void act(final Consumer<ScreenHandle> action) {
        final ScreenHandle handle;
        synchronized (this) {
            if (this.ended) {
                return;
            }
            handle = this.opened;
            if (handle == null) {
                this.waiting.add(action);
                return;
            }
        }
        if (this.ui.isCurrent()) {
            action.accept(handle);
        } else {
            this.ui.execute(() -> action.accept(handle));
        }
    }

    /**
     * Cancels the opening if it has not run yet.
     *
     * @return true if it was canceled now
     */
    private synchronized boolean cancel() {
        if (this.opened != null || this.ended) {
            return false;
        }
        this.ended = true;
        this.waiting.clear();
        return true;
    }

    @Override
    public void invalidate() {
        this.act(ScreenHandle::invalidate);
    }

    @Override
    public void close() {
        if (!this.cancel()) {
            this.act(ScreenHandle::close);
        }
    }

    @Override
    public boolean requestClose() {
        if (this.cancel()) {
            return true;
        }
        final ScreenHandle handle;
        synchronized (this) {
            handle = this.opened;
        }
        if (handle != null && this.ui.isCurrent()) {
            return handle.requestClose();
        }
        this.act(ScreenHandle::requestClose);
        return false;
    }

    @Override
    public synchronized boolean isOpen() {
        if (this.ended) {
            return false;
        }
        return this.opened == null || this.opened.isOpen();
    }

    @Override
    public ScreenHandle setTitle(final Text title) {
        Objects.requireNonNull(title, "title");
        this.act(handle -> handle.setTitle(title));
        return this;
    }

    @Override
    public ComponentFactory factory() {
        final ScreenHandle handle;
        synchronized (this) {
            handle = this.opened;
        }
        if (handle == null) {
            throw new IllegalStateException("The screen was opened from another thread and has not opened yet;"
                    + " ask for its factory from its opened() or on the UI thread once it has opened");
        }
        return handle.factory();
    }

    @Override
    public ScreenHandle open(final Screen screen, final ThemeRef theme) {
        Objects.requireNonNull(screen, "screen");
        Objects.requireNonNull(theme, "theme");
        final HandedOverHandle above = new HandedOverHandle(this.ui, () -> {
            final ScreenHandle handle;
            synchronized (this) {
                handle = this.opened;
            }
            if (handle == null) {
                throw new IllegalStateException("The screen below never opened, so " + screen + " cannot open above it");
            }
            return handle.open(screen, theme);
        });
        return above;
    }
}
