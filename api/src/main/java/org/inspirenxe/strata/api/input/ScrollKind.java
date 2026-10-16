package org.inspirenxe.strata.api.input;

/**
 * The kind of device a scroll came from, so scrolling components can choose step sizes.
 */
public enum ScrollKind {
    /**
     * A wheel that moves in notches. Each notch reports a scroll of 1.
     */
    WHEEL,
    /**
     * A trackpad or smooth wheel that reports fractional amounts.
     */
    PRECISE
}
