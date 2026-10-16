package org.inspirenxe.strata.api.layout;

/**
 * Where a component sits across the height of the slot its parent gives it. See
 * {@link org.inspirenxe.strata.api.component.Component#setAlignment}.
 */
public enum VerticalAlignment {
    /** Against the slot's top edge (after the margin), at the desired height. */
    TOP,
    /** Centered in the slot, at the desired height. */
    MIDDLE,
    /** Against the slot's bottom edge (before the margin), at the desired height. */
    BOTTOM,
    /** As tall as the slot minus the margin, unless the component has a fixed height; the minimum and maximum still apply. */
    STRETCH
}
