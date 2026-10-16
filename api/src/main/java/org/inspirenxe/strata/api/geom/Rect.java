package org.inspirenxe.strata.api.geom;

/**
 * An axis-aligned rectangle in UI units. Width and height are never negative.
 */
public record Rect(float x, float y, float width, float height) {
    public static final Rect ZERO = new Rect(0, 0, 0, 0);

    public Rect {
        width = Math.max(0f, width);
        height = Math.max(0f, height);
    }

    public static Rect ofEdges(final float left, final float top, final float right, final float bottom) {
        return new Rect(left, top, right - left, bottom - top);
    }

    public float right() {
        return this.x + this.width;
    }

    public float bottom() {
        return this.y + this.height;
    }

    public boolean isEmpty() {
        return this.width <= 0f || this.height <= 0f;
    }

    public boolean contains(final float px, final float py) {
        return px >= this.x && py >= this.y && px < this.right() && py < this.bottom();
    }

    public Rect shrink(final Insets insets) {
        return new Rect(
                this.x + insets.left(),
                this.y + insets.top(),
                this.width - insets.horizontal(),
                this.height - insets.vertical()
        );
    }

    public Rect grow(final Insets insets) {
        return new Rect(
                this.x - insets.left(),
                this.y - insets.top(),
                this.width + insets.horizontal(),
                this.height + insets.vertical()
        );
    }

    public Rect translate(final float dx, final float dy) {
        return new Rect(this.x + dx, this.y + dy, this.width, this.height);
    }

    public Rect intersect(final Rect other) {
        final float left = Math.max(this.x, other.x);
        final float top = Math.max(this.y, other.y);
        final float right = Math.min(this.right(), other.right());
        final float bottom = Math.min(this.bottom(), other.bottom());
        return Rect.ofEdges(left, top, Math.max(left, right), Math.max(top, bottom));
    }
}
