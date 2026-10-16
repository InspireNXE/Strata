package org.inspirenxe.strata.api.geom;

/**
 * Space around the four sides of a rectangle, used for margins, padding and nine-slice borders.
 */
public record Insets(float left, float top, float right, float bottom) {
    public static final Insets NONE = new Insets(0, 0, 0, 0);

    public static Insets of(final float all) {
        return new Insets(all, all, all, all);
    }

    public static Insets of(final float horizontal, final float vertical) {
        return new Insets(horizontal, vertical, horizontal, vertical);
    }

    public float horizontal() {
        return this.left + this.right;
    }

    public float vertical() {
        return this.top + this.bottom;
    }
}
