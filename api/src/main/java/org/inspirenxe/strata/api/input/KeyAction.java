package org.inspirenxe.strata.api.input;

/**
 * What happened to a key.
 *
 * <p>A held key produces one {@link #PRESS}, then {@link #REPEAT}s at the system rate for keys that repeat, then
 * one {@link #RELEASE}. Strata delivers repeats on every engine: when an engine does not send them, Strata makes
 * them for navigation and delete keys.</p>
 */
public enum KeyAction {
    /** The key went down. */
    PRESS,
    /** The key is still held and repeating. */
    REPEAT,
    /** The key came up. */
    RELEASE;

    /**
     * Returns whether this is a press or a repeat: the moments when a held key should act, such as moving a caret
     * again.
     *
     * @return true for {@link #PRESS} and {@link #REPEAT}
     */
    public boolean isDown() {
        return this != RELEASE;
    }
}
