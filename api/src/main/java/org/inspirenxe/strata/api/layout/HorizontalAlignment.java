package org.inspirenxe.strata.api.layout;

/**
 * Where a component sits across the width of the slot its parent gives it. See
 * {@link org.inspirenxe.strata.api.component.Component#setAlignment}.
 */
public enum HorizontalAlignment {
    /** Against the slot's left edge (after the margin), at the desired width. */
    LEFT,
    /** Centered in the slot, at the desired width. */
    CENTER,
    /** Against the slot's right edge (before the margin), at the desired width. */
    RIGHT,
    /** As wide as the slot minus the margin, unless the component has a fixed width; the minimum and maximum still apply. */
    STRETCH
}
