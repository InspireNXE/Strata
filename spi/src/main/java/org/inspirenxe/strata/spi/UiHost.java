package org.inspirenxe.strata.spi;

import org.inspirenxe.strata.api.Ui;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.screen.Screen;
import org.inspirenxe.strata.api.theme.Theme;

import java.util.Optional;

/**
 * Shows one screen at a time and is the single entry point an engine drives: {@link #resize}, {@link #render} and
 * the {@link InputSink} methods. Single-threaded: call everything from the engine's UI thread.
 */
public interface UiHost extends InputSink {

    /**
     * The factory for components shown by this host.
     */
    Ui ui();

    /**
     * Closes the current screen, if any, then builds and opens this one.
     *
     * @throws org.inspirenxe.strata.api.theme.ThemeException if the theme lacks keys the screen's tree needs
     */
    void open(Screen screen);

    /**
     * Closes the current screen. Does nothing if none is open.
     */
    void close();

    Optional<Screen> screen();

    Theme theme();

    /**
     * @throws org.inspirenxe.strata.api.theme.ThemeException if the theme lacks keys the open tree needs; the old
     *                                                         theme stays
     */
    void setTheme(Theme theme);

    void resize(float width, float height);

    /**
     * Runs layout now if anything changed. {@link #render} does this itself.
     */
    void layout();

    void render(Canvas canvas);

    Optional<Component<?>> focused();

    Optional<Component<?>> hovered();
}
