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

import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.screen.Screen;
import org.inspirenxe.strata.api.screen.ScreenController;
import org.inspirenxe.strata.api.screen.ScreenHandle;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.ThemeRef;

import java.util.Optional;

/**
 * A stack of screens, for engines with no screen system of their own. Implemented by Strata and created with
 * {@code StrataCore.createScreenStack}; adapters drive it. Do not implement this interface.
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
     * through its handle is removed from wherever it is in the stack. Called off the UI thread, it hands the opening
     * to the UI thread and returns at once; the screen opens there in order with other work handed over, and an
     * opening that throws there is logged.
     *
     * @param screen the screen to open
     * @throws org.inspirenxe.strata.api.theme.ThemeException if the theme lacks keys the screen's tree needs, when
     *                                                         called on the UI thread; the screen is not pushed
     */
    void push(Screen screen);

    /**
     * Opens a screen on top of the stack, at the stack's current size, in a theme of its own that
     * {@link #setTheme} leaves alone. A screen that asks to close through its handle is removed from wherever it is
     * in the stack.
     *
     * <p>Called off the UI thread, it hands the opening to the UI thread and returns a handle at once, as an engine
     * adapter's controller does. The handle's methods hand themselves over too and reach
     * the screen once it has opened; closing it before then cancels the opening, and its {@code factory()} throws
     * {@link IllegalStateException} until then. An opening that throws on the UI thread is logged, and the handle
     * then counts as closed.</p>
     *
     * @param screen the screen to open
     * @param theme the theme for its components
     * @return the screen's handle
     * @throws org.inspirenxe.strata.api.theme.ThemeException if the ref cannot be resolved, or the theme lacks keys
     *                                                         the screen's tree needs, when called on the UI thread;
     *                                                         the screen is not pushed
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
     * Closes and removes every screen, top first. The screens are not asked first; see {@link #requestClose()}.
     */
    void clear();

    /**
     * Asks the screens to close as the user would, for the window's own close button: the top screen is asked
     * through its {@link Screen#closeRequested} and removed if it agrees, then the next, and so on, stopping at the
     * first screen that refuses.
     *
     * @return true if the stack is empty afterwards; false if a screen refused, which stays open with every screen
     *     below it
     */
    boolean requestClose();

    /**
     * Returns the top screen's title as of now, as {@link ScreenHost#title()} describes.
     *
     * @return the title, or {@link Text#empty()} if the stack is empty
     */
    Text title();

    /**
     * Replaces the theme of every screen in the stack and of screens pushed later, except screens opened with a
     * theme of their own. Those screens stop following a ref set before with {@link #setTheme(ThemeRef)}, so a
     * reload of the theme source no longer changes them.
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
     * Sets the density of every screen, and of screens pushed later, as
     * {@link ScreenHost#setPixelsPerUnit(float)} does.
     *
     * @param pixelsPerUnit device pixels per UI unit
     * @throws IllegalArgumentException if the value is not finite and greater than 0
     */
    void setPixelsPerUnit(float pixelsPerUnit);

    /**
     * Returns the size the top screen's content wants in a given space, as {@link ScreenHost#preferredSize} describes.
     * The screens below are left out.
     *
     * @param availableWidth the width offered, in UI units; {@link Float#POSITIVE_INFINITY} for no limit
     * @param availableHeight the height offered, in UI units; {@link Float#POSITIVE_INFINITY} for no limit
     * @return the content's desired size, margin included, or {@link Size#ZERO} if the stack is empty
     * @throws IllegalArgumentException if either value is negative or NaN
     */
    Size preferredSize(float availableWidth, float availableHeight);

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
     * Returns whether something changed since the last {@link #render} in a screen that would be drawn, or in the
     * stack itself, such as a screen pushed or removed, as {@link ScreenHost#needsFrame()} describes. Any thread may
     * call this.
     *
     * @return true if a frame is needed
     */
    boolean needsFrame();

    /**
     * Returns the earliest {@link ScreenHost#nextFrameNanos()} of the screens that would be drawn. Call it on the UI
     * thread, after a render.
     *
     * @return the {@link Clock#nanoTime()} reading at which a frame is due, or {@link Long#MAX_VALUE} if nothing
     *     timed is waiting
     */
    long nextFrameNanos();

    /**
     * Sets what runs when {@link #needsFrame()} turns from false to true, as
     * {@link ScreenHost#setFrameRequester(Runnable)} describes for a host.
     *
     * @param requester the requester, or {@code null} for none
     */
    void setFrameRequester(Runnable requester);

    /**
     * Draws the visible screens, bottom to top.
     *
     * @param canvas the engine canvas for this frame
     */
    void render(Canvas canvas);
}
