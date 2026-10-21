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
package org.inspirenxe.strata.spi;

import org.inspirenxe.strata.api.screen.Screen;
import org.inspirenxe.strata.api.screen.ScreenController;
import org.inspirenxe.strata.api.screen.ScreenHandle;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.ThemeRef;

import java.util.Optional;

/**
 * A stack of screens, for engines with no screen system of their own. Implemented by Strata and created with
 * {@code StrataCore.createScreenStack}; adapters drive it.
 *
 * <p>Only the top screen receives input. Rendering starts at the topmost screen that
 * {@linkplain Screen#hidesBackground() hides the background} (or the bottom screen if none does) and draws each
 * screen above it in order, calling {@link Canvas#nextLayer()} between screens. Each screen has its own
 * {@link ScreenHost} inside, with its own factory, focus and hover.</p>
 *
 * <p>A stack is also a {@link ScreenController}. {@link #open(Screen, ThemeRef)} puts a screen on top in a theme of
 * its own, and a screen's {@link ScreenHandle#open} puts the new screen straight above that screen, so closing it
 * shows that screen again.</p>
 *
 * <p>The same requirements for callers apply as for {@link ScreenHost}.</p>
 */
public interface ScreenStack extends InputSink, ScreenController {

    /**
     * Opens a screen on top of the stack, at the stack's current size and theme. A screen that asks to close
     * through its handle is removed from wherever it is in the stack.
     *
     * @param screen the screen to open
     * @throws org.inspirenxe.strata.api.theme.ThemeException if the theme lacks keys the screen's tree needs; the
     *                                                         screen is not pushed
     */
    void push(Screen screen);

    /**
     * Opens a screen on top of the stack, at the stack's current size, in a theme of its own that
     * {@link #setTheme} leaves alone. A screen that asks to close through its handle is removed from wherever it is
     * in the stack.
     *
     * @param screen the screen to open
     * @param theme the theme for its components
     * @return the screen's handle
     * @throws org.inspirenxe.strata.api.theme.ThemeException if the ref cannot be resolved, or the theme lacks keys
     *                                                         the screen's tree needs; the screen is not pushed
     */
    @Override
    ScreenHandle open(Screen screen, ThemeRef theme);

    /**
     * Closes and removes the top screen.
     *
     * @return the removed screen, or empty if the stack was empty
     */
    Optional<Screen> pop();

    /**
     * Returns the top screen, the one receiving input.
     *
     * @return the top screen, or empty
     */
    Optional<Screen> top();

    /**
     * Returns whether no screen is open.
     *
     * @return true if the stack is empty
     */
    boolean isEmpty();

    /**
     * Closes and removes every screen, top first.
     */
    void clear();

    /**
     * Replaces the theme of every screen in the stack and of screens pushed later, except screens opened with a
     * theme of their own.
     *
     * @param theme the new theme
     * @throws org.inspirenxe.strata.api.theme.ThemeException if the theme lacks keys a screen's tree needs
     */
    void setTheme(Theme theme);

    /**
     * Replaces the theme of every screen in the stack and of screens pushed later with one resolved from the
     * platform's {@link ThemeSource}, as {@link ScreenHost#setTheme(ThemeRef)} does, except screens opened with a
     * theme of their own.
     *
     * @param theme the theme to resolve
     * @throws org.inspirenxe.strata.api.theme.ThemeException if the ref cannot be resolved, or the result lacks
     *                                                         keys a screen's tree needs
     */
    void setTheme(ThemeRef theme);

    /**
     * Sets the size of every screen, and of screens pushed later.
     *
     * @param width the width in UI units
     * @param height the height in UI units
     */
    void resize(float width, float height);

    /**
     * Sets the tooltip delay of every screen, and of screens pushed later, as
     * {@link ScreenHost#setTooltipDelay(long)} does.
     *
     * @param millis the delay in milliseconds; 0 shows tooltips as soon as the pointer arrives
     * @throws IllegalArgumentException if {@code millis} is negative
     */
    void setTooltipDelay(long millis);

    /**
     * Flushes the bindings of every screen in the stack, bottom to top, as {@link ScreenHost#flush()} does.
     */
    void flush();

    /**
     * Draws the visible screens, bottom to top.
     *
     * @param canvas the engine canvas for this frame
     */
    void render(Canvas canvas);
}
