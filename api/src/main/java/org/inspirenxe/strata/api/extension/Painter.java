package org.inspirenxe.strata.api.extension;

import org.inspirenxe.strata.api.geom.Insets;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.theme.SpriteId;

/**
 * What a component draws with during {@link CustomComponent#paint}. Coordinates are in the host's coordinates, in
 * UI units; draw within the bounds you were given.
 *
 * <p>Colors are ARGB ({@code 0xAARRGGBB}); a color with alpha 0 draws nothing. Later calls draw over earlier ones.
 * The engine may round coordinates to whole pixels: Minecraft draws in whole GUI pixels.</p>
 *
 * <p>Clipping and translation are scoped to a callback, so the engine's state is always restored, even if the
 * callback throws.</p>
 *
 * <p>Strata provides the painter, which is valid only during the paint call. Do not implement this
 * interface.</p>
 */
public interface Painter {

    /**
     * Fills a rectangle with a color.
     *
     * @param rect the area
     * @param argb the color
     */
    void fill(Rect rect, int argb);

    /**
     * Fills a rectangle with a vertical gradient.
     *
     * @param rect the area
     * @param topArgb the color at the top edge
     * @param bottomArgb the color at the bottom edge
     */
    void fillGradient(Rect rect, int topArgb, int bottomArgb);

    /**
     * Draws a sprite stretched over a rectangle.
     *
     * @param sprite the sprite; see {@link SpriteId} for where engines look for it
     * @param rect the area
     * @param tint an ARGB color multiplied with the sprite; opaque white leaves it unchanged
     */
    void sprite(SpriteId sprite, Rect rect, int tint);

    /**
     * Draws a sprite as a nine-slice: corners at their size, edges stretched along one axis, the center stretched
     * both ways. See {@link org.inspirenxe.strata.api.theme.Paint.NineSlice}.
     *
     * @param sprite the sprite
     * @param rect the area
     * @param border the width of each fixed border, in the sprite's pixels; Minecraft takes it from the sprite's
     *               {@code .png.mcmeta} instead
     * @param tint an ARGB color multiplied with the sprite
     */
    void nineSlice(SpriteId sprite, Rect rect, Insets border, int tint);

    /**
     * Draws one line of text with its top-left corner at the given point. Use
     * {@link org.inspirenxe.strata.api.text.TextMetrics} to measure it first.
     *
     * @param span the text and its style
     * @param x the left edge
     * @param y the top edge (not the baseline)
     */
    void text(TextSpan span, float x, float y);

    /**
     * Runs {@code draw} with drawing limited to a rectangle, intersected with any clip already in place.
     *
     * @param rect the area drawing is limited to
     * @param draw the drawing to clip
     */
    void clipped(Rect rect, Runnable draw);

    /**
     * Runs {@code draw} with every coordinate shifted by an offset. Clips set inside are shifted too.
     *
     * @param dx the horizontal offset
     * @param dy the vertical offset
     * @param draw the drawing to shift
     */
    void translated(float dx, float dy, Runnable draw);
}
