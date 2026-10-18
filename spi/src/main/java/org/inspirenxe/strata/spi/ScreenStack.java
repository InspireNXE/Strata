package org.inspirenxe.strata.spi;

import org.inspirenxe.strata.api.screen.Screen;
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
 * <p>The same requirements for callers apply as for {@link ScreenHost}.</p>
 */
public interface ScreenStack extends InputSink {

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
     * Replaces the theme of every screen in the stack and of screens pushed later.
     *
     * @param theme the new theme
     * @throws org.inspirenxe.strata.api.theme.ThemeException if the theme lacks keys a screen's tree needs
     */
    void setTheme(Theme theme);

    /**
     * Replaces the theme of every screen in the stack and of screens pushed later with one resolved from the
     * platform's {@link ThemeSource}, as {@link ScreenHost#setTheme(ThemeRef)} does.
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
     * Draws the visible screens, bottom to top.
     *
     * @param canvas the engine canvas for this frame
     */
    void render(Canvas canvas);
}
