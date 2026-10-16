package org.inspirenxe.strata.api.input;

public enum KeyAction {
    PRESS,
    REPEAT,
    RELEASE;

    /**
     * True for a press or a repeat: the moments a held key should act.
     */
    public boolean isDown() {
        return this != RELEASE;
    }
}
