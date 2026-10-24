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
package org.inspirenxe.strata.api.screen;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.event.Subscription;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.theme.ThemeRef;

/**
 * What an open screen can ask of its host. Each opening of a screen gets its own handle; once that opening ends
 * (the screen closed, or its host showed another screen in its place), the handle's methods do nothing, except
 * {@link #open}, which throws. Opening a screen above this one with {@link #open} does not end this opening: the
 * screen is covered, keeps its handle, and shows again when the screen above closes.
 *
 * <p>Hosts create handles. Do not implement this interface.</p>
 */
public interface ScreenHandle {

    /**
     * Asks for the tree to be built again before the next frame. Use it when the screen's structure changes;
     * simple changes such as new label text only need setters.
     */
    void invalidate();

    /**
     * Asks the host to close the screen, without asking the screen's {@link Screen#closeRequested}: a close from code
     * is always carried out. The host decides what closing means: a screen stack removes the screen, and an engine
     * with its own screens may close the engine screen that shows it.
     */
    void close();

    /**
     * Asks to close the screen as the user would, for a Cancel or Back button. The host calls the screen's
     * {@link Screen#closeRequested} with this handle, and closes the screen as {@link #close()} does if it returns
     * true.
     *
     * @return true if the screen agreed and the host was asked to close it; false if the screen refused, or this
     *     opening has already ended
     */
    boolean requestClose();

    /**
     * Returns whether this opening of the screen is still going on. It ends when the screen closes, or when its host
     * shows another screen in its place; a screen covered by another one it opened is still open.
     *
     * @return false once this opening has ended
     */
    boolean isOpen();

    /**
     * Replaces the title hosts show for the screen, which starts as {@link Screen#title()}. An engine that shows the
     * title, such as in a window's title bar, shows the new one from its next frame. Does nothing once this opening
     * has ended.
     *
     * @param title the new title
     * @return this handle
     */
    ScreenHandle setTitle(Text title);

    /**
     * Runs a task every interval while this opening of the screen goes on, for a screen that polls: refreshing
     * figures, checking on a download, enabling a button after a delay. The engine schedules it on the UI thread, so
     * the task may change components. The first run comes about one interval from now. An engine with a coarser beat,
     * such as a tick, rounds the interval up to whole beats, and may drop runs it missed while paused.
     *
     * <p>The task stops when the subscription closes or the opening ends, whichever comes first, and never runs
     * after that. Closing the subscription twice, or after the screen closed, does nothing. Once this opening has
     * ended the task is never run, and the subscription returned is already closed.</p>
     *
     * <pre>{@code
     * public void opened(ScreenHandle handle) {
     *     handle.every(100, this::refresh);
     * }
     * }</pre>
     *
     * <p>For motion, use {@code ComponentFactory.animator()} instead: animations step once for every frame drawn,
     * where a task runs on the engine's own beat.</p>
     *
     * @param millis the interval in milliseconds; above 0
     * @param task what to run
     * @return the subscription that stops the task
     * @throws IllegalArgumentException if {@code millis} is 0 or less
     */
    Subscription every(long millis, Runnable task);

    /**
     * Returns the factory of the host showing the screen, the same one passed to {@link Screen#build}.
     *
     * @return the factory
     */
    ComponentFactory factory();

    /**
     * Opens a screen above this one, in the platform's default theme. When the new screen closes, this one shows
     * again with its tree as it was. The same as {@code open(screen, ThemeRef.platform())}.
     *
     * @param screen the screen to open
     * @return the handle of the new screen
     * @throws IllegalStateException if this screen has closed
     * @throws org.inspirenxe.strata.api.theme.ThemeException if the theme cannot be resolved or lacks keys the
     *     screen's tree needs, on platforms that build the screen at once
     */
    default ScreenHandle open(final Screen screen) {
        return this.open(screen, ThemeRef.platform());
    }

    /**
     * Opens a screen above this one, in a theme. When the new screen closes, this one shows again with its tree as
     * it was. Where the new screen goes when this one is not on top depends on the host: a screen stack puts it
     * straight above this one, and an engine with screens of its own may put it on top.
     *
     * @param screen the screen to open
     * @param theme the theme for its components
     * @return the handle of the new screen
     * @throws IllegalStateException if this screen has closed
     * @throws org.inspirenxe.strata.api.theme.ThemeException if the theme cannot be resolved or lacks keys the
     *     screen's tree needs, on platforms that build the screen at once
     */
    ScreenHandle open(Screen screen, ThemeRef theme);
}
