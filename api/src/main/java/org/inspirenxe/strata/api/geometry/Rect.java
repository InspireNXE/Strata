/*
 * Copyright (c) InspireNXE <https://github.com/InspireNXE>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.inspirenxe.strata.api.geometry;

/**
 * An axis-aligned rectangle in UI units.
 *
 * <p>Width and height are never negative: the constructor clamps a negative size to zero, so shrinking a rectangle
 * by more than its size yields an empty rectangle at the shrunk position instead of an inverted one. A rectangle
 * covers the half-open range {@code [x, x + width)} by {@code [y, y + height)}.</p>
 *
 * @param x the left edge
 * @param y the top edge
 * @param width the width; zero if negative or NaN
 * @param height the height; zero if negative or NaN
 */
public record Rect(float x, float y, float width, float height) {
    /**
     * The empty rectangle at the origin.
     */
    public static final Rect ZERO = new Rect(0, 0, 0, 0);

    /**
     * Creates a rectangle, making a negative or NaN width or height zero.
     */
    public Rect {
        width = width > 0f ? width : 0f;
        height = height > 0f ? height : 0f;
    }

    /**
     * Creates a rectangle from its edges.
     *
     * @param left the left edge
     * @param top the top edge
     * @param right the right edge; a value left of {@code left} gives zero width
     * @param bottom the bottom edge; a value above {@code top} gives zero height
     * @return the rectangle between the edges
     */
    public static Rect ofEdges(final float left, final float top, final float right, final float bottom) {
        return new Rect(left, top, right - left, bottom - top);
    }

    /**
     * Returns the right edge, {@code x + width}. The edge itself is outside the rectangle.
     *
     * @return the right edge
     */
    public float right() {
        return this.x + this.width;
    }

    /**
     * Returns the bottom edge, {@code y + height}. The edge itself is outside the rectangle.
     *
     * @return the bottom edge
     */
    public float bottom() {
        return this.y + this.height;
    }

    /**
     * Returns whether the rectangle covers no area.
     *
     * @return true if the width or the height is zero
     */
    public boolean isEmpty() {
        return this.width <= 0f || this.height <= 0f;
    }

    /**
     * Returns whether a point lies inside the rectangle. The left and top edges are inside; the right and bottom
     * edges are outside, so two rectangles that share an edge never both contain a point on it.
     *
     * @param px the point's x
     * @param py the point's y
     * @return true if the point is inside
     */
    public boolean contains(final float px, final float py) {
        return px >= this.x && py >= this.y && px < this.right() && py < this.bottom();
    }

    /**
     * Moves each edge inward by the matching inset. Insets larger than the rectangle give an empty rectangle.
     *
     * @param insets how far to move each edge
     * @return the smaller rectangle
     */
    public Rect shrink(final Insets insets) {
        return new Rect(
                this.x + insets.left(),
                this.y + insets.top(),
                this.width - insets.horizontal(),
                this.height - insets.vertical()
        );
    }

    /**
     * Moves each edge outward by the matching inset. The inverse of {@link #shrink} for rectangles large enough
     * not to be clamped.
     *
     * @param insets how far to move each edge
     * @return the larger rectangle
     */
    public Rect grow(final Insets insets) {
        return new Rect(
                this.x - insets.left(),
                this.y - insets.top(),
                this.width + insets.horizontal(),
                this.height + insets.vertical()
        );
    }

    /**
     * Returns the same rectangle moved by an offset.
     *
     * @param dx the horizontal offset
     * @param dy the vertical offset
     * @return the moved rectangle
     */
    public Rect translate(final float dx, final float dy) {
        return new Rect(this.x + dx, this.y + dy, this.width, this.height);
    }

    /**
     * Returns the overlap of two rectangles. Rectangles that do not overlap give an empty rectangle placed where
     * the overlap would start.
     *
     * @param other the other rectangle
     * @return the area both rectangles cover
     */
    public Rect intersect(final Rect other) {
        final float left = Math.max(this.x, other.x);
        final float top = Math.max(this.y, other.y);
        final float right = Math.min(this.right(), other.right());
        final float bottom = Math.min(this.bottom(), other.bottom());
        return Rect.ofEdges(left, top, Math.max(left, right), Math.max(top, bottom));
    }

    /**
     * Returns whether two rectangles overlap. Rectangles that only share an edge do not, and an empty rectangle
     * overlaps nothing, since it has no area.
     *
     * @param other the other rectangle
     * @return true if some area lies inside both
     */
    public boolean intersects(final Rect other) {
        return this.width > 0f && this.height > 0f && other.width > 0f && other.height > 0f
                && this.x < other.right() && other.x < this.right() && this.y < other.bottom() && other.y < this.bottom();
    }
}
