package org.inspirenxe.strata.api.component;

/**
 * Shorthand for a pair of alignments, as MalisisCore's anchors were.
 */
public enum Anchor {
    TOP_LEFT(HorizontalAlignment.LEFT, VerticalAlignment.TOP),
    TOP_CENTER(HorizontalAlignment.CENTER, VerticalAlignment.TOP),
    TOP_RIGHT(HorizontalAlignment.RIGHT, VerticalAlignment.TOP),
    MIDDLE_LEFT(HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE),
    CENTER(HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE),
    MIDDLE_RIGHT(HorizontalAlignment.RIGHT, VerticalAlignment.MIDDLE),
    BOTTOM_LEFT(HorizontalAlignment.LEFT, VerticalAlignment.BOTTOM),
    BOTTOM_CENTER(HorizontalAlignment.CENTER, VerticalAlignment.BOTTOM),
    BOTTOM_RIGHT(HorizontalAlignment.RIGHT, VerticalAlignment.BOTTOM),
    FILL(HorizontalAlignment.STRETCH, VerticalAlignment.STRETCH);

    private final HorizontalAlignment horizontal;
    private final VerticalAlignment vertical;

    Anchor(final HorizontalAlignment horizontal, final VerticalAlignment vertical) {
        this.horizontal = horizontal;
        this.vertical = vertical;
    }

    public HorizontalAlignment horizontal() {
        return this.horizontal;
    }

    public VerticalAlignment vertical() {
        return this.vertical;
    }
}
