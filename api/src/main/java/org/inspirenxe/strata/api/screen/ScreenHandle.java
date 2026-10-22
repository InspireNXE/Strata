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
     * Asks the host to close the screen. The host decides what that means: a Swing screen stack pops the screen,
     * and an engine with its own screens may close the engine screen that shows it.
     */
    void close();

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
