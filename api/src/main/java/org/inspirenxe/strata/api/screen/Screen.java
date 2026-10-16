package org.inspirenxe.strata.api.screen;

import org.inspirenxe.strata.api.Ui;
import org.inspirenxe.strata.api.component.Component;

/**
 * One full screen of UI. A host (a window, or a Minecraft screen) opens it, calls {@link #build} for its tree, and
 * builds again after {@link ScreenHandle#invalidate()}.
 */
@FunctionalInterface
public interface Screen {

    /**
     * Creates the component tree with the given factory.
     */
    Component<?> build(Ui ui);

    /**
     * Called once the screen is open and its first tree is built.
     */
    default void opened(final ScreenHandle handle) {
    }

    /**
     * Called before the screen's tree is torn down.
     */
    default void closed() {
    }

    /**
     * Whether screens beneath this one can be skipped when painting.
     */
    default boolean hidesBackground() {
        return false;
    }
}
