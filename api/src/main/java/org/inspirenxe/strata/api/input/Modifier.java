package org.inspirenxe.strata.api.input;

/**
 * Modifier keys. Left and right sides collapse into one flag, which is what shortcut matching wants.
 */
public enum Modifier {
    SHIFT,
    CONTROL,
    ALT,
    SUPER;

    final int bit() {
        return 1 << this.ordinal();
    }
}
