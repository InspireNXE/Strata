package org.inspirenxe.strata.spi;

import org.inspirenxe.strata.api.screen.Screen;
import org.inspirenxe.strata.api.theme.Theme;

import java.util.Optional;

/**
 * A stack of screens for engines that have no screen system of their own. Only the top screen receives input.
 * Painting starts at the topmost screen that {@linkplain Screen#hidesBackground() hides the background}.
 *
 * <p>Engines that already stack screens (Minecraft) host each Strata screen in a {@link UiHost} of its own
 * instead.</p>
 */
public interface ScreenStack extends InputSink {

    void push(Screen screen);

    Optional<Screen> pop();

    Optional<Screen> top();

    boolean isEmpty();

    void clear();

    void setTheme(Theme theme);

    void resize(float width, float height);

    void render(Canvas canvas);
}
