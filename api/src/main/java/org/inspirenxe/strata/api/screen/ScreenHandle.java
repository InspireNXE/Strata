package org.inspirenxe.strata.api.screen;

import org.inspirenxe.strata.api.Ui;

/**
 * What an open screen can ask of its host.
 */
public interface ScreenHandle {

    /**
     * Throws the tree away and builds a new one before the next frame.
     */
    void invalidate();

    /**
     * Asks the host to close the screen. On Minecraft this returns to the previous screen.
     */
    void close();

    Ui ui();
}
