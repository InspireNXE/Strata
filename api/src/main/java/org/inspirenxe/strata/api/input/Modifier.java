package org.inspirenxe.strata.api.input;

/**
 * A modifier key that may be held during input. The left and right keys of each kind count as the same modifier,
 * which is what shortcut matching needs; use {@link Key} to tell them apart.
 */
public enum Modifier {
    /** Either Shift key. */
    SHIFT,
    /** Either Control key. */
    CONTROL,
    /** Either Alt key, including AltGr. */
    ALT,
    /** Either Super key: the Windows key, or Command on macOS. */
    SUPER;

    final int bit() {
        return 1 << this.ordinal();
    }
}
