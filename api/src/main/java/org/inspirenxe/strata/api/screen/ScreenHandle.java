package org.inspirenxe.strata.api.screen;

import org.inspirenxe.strata.api.ComponentFactory;

/**
 * What an open screen can ask of its host. Each opening of a screen gets its own handle; once that opening ends
 * (the screen closed, or the host opened another screen), the handle's methods do nothing.
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
}
