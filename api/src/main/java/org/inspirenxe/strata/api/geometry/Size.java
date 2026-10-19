package org.inspirenxe.strata.api.geometry;

/**
 * A width and height in UI units. Measuring returns sizes; Strata treats a negative value as zero.
 *
 * @param width the width
 * @param height the height
 */
public record Size(float width, float height) {
    /**
     * Zero width and zero height.
     */
    public static final Size ZERO = new Size(0, 0);
}
