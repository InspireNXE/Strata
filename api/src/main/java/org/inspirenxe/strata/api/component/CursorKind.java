package org.inspirenxe.strata.api.component;

/**
 * Mouse cursor shapes a component can ask for with {@link Component#setCursor}. An engine without one of these
 * shapes shows the closest it has; the Swing adapter shows the default arrow for {@link #NOT_ALLOWED}.
 */
public enum CursorKind {
    /** The default pointer. */
    ARROW,
    /** The text I-beam, for editable text. */
    TEXT,
    /** The pointing hand, for clickable things. */
    HAND,
    /** A crosshair, for precise picking. */
    CROSSHAIR,
    /** A left-right arrow, for resizing horizontally. */
    RESIZE_HORIZONTAL,
    /** An up-down arrow, for resizing vertically. */
    RESIZE_VERTICAL,
    /** A four-way arrow, for moving. */
    RESIZE_ALL,
    /** A "no" sign, for something that cannot be used now. */
    NOT_ALLOWED
}
