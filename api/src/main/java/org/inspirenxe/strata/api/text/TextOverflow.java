package org.inspirenxe.strata.api.text;

/**
 * What a label does with a line wider than its bounds.
 */
public enum TextOverflow {
    /**
     * Draw the whole line, past the bounds if need be. A clipping parent cuts it off.
     */
    VISIBLE,
    /**
     * Cut the line between code points so it fits with {@code ...} after it, in the style of the last character
     * kept. If not even the dots fit, nothing of the line is drawn.
     */
    ELLIPSIS
}
