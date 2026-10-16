package org.inspirenxe.strata.api.input;

/**
 * Mouse buttons, independent of any engine. Adapters map their engine's button numbers to these constants and map
 * anything else to {@link #UNKNOWN}.
 */
public enum MouseButton {
    /** The primary button, usually the left one. */
    LEFT,
    /** The secondary button, usually the right one. */
    RIGHT,
    /** The middle button or wheel click. */
    MIDDLE,
    /** The "back" side button (button 4). */
    BACK,
    /** The "forward" side button (button 5). */
    FORWARD,
    /** A button the adapter could not map. */
    UNKNOWN
}
