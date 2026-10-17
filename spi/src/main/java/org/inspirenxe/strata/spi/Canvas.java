package org.inspirenxe.strata.spi;

import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.geom.Insets;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.visual.Visual;

/**
 * What an engine draws for Strata. A host calls it during {@code render}; components never see it (they get a
 * {@link org.inspirenxe.strata.api.extension.Painter} built on it).
 *
 * <h2>Requirements for implementors</h2>
 * <ul>
 *     <li>Coordinates are UI units, floats, in the current transform. Round them to the engine's pixel grid if it
 *     needs whole pixels, consistently between drawing and clipping.</li>
 *     <li>Colors are ARGB ({@code 0xAARRGGBB}). A color with alpha 0 draws nothing; partial alpha blends over what
 *     is already drawn.</li>
 *     <li>Later calls draw over earlier ones. An engine that batches or reorders draws must keep this order, at least
 *     across {@link #nextLayer()}.</li>
 *     <li>Calls may be recorded and replayed later; nothing requires immediate
 *     drawing.</li>
 *     <li>Clips and transforms form stacks. Strata balances every push with a pop within one frame, and pops in
 *     reverse order, even when a component throws. A clip applies in the transform current when it was pushed and
 *     intersects any clip already in place.</li>
 *     <li>Never throw for missing resources: a sprite that cannot be found should draw something visible (the Swing
 *     adapter draws magenta) so the problem shows on screen.</li>
 *     <li>Text drawn with {@link #text} must line up with the widths and line height the platform's
 *     {@link org.inspirenxe.strata.api.text.TextMetrics} reports.</li>
 * </ul>
 */
public interface Canvas {

    /**
     * Fills a rectangle with a color.
     *
     * @param rect the area
     * @param argb the color; alpha 0 draws nothing
     */
    void fill(Rect rect, int argb);

    /**
     * Fills a rectangle with a vertical gradient: {@code topArgb} at the top edge blending to {@code bottomArgb}
     * at the bottom edge.
     *
     * @param rect the area
     * @param topArgb the color at the top
     * @param bottomArgb the color at the bottom
     */
    void fillGradient(Rect rect, int topArgb, int bottomArgb);

    /**
     * Draws a whole sprite stretched over a rectangle.
     *
     * @param sprite the sprite; see {@link SpriteId} for where adapters look for it
     * @param rect the area
     * @param tint an ARGB color multiplied with the sprite's pixels; opaque white ({@code 0xFFFFFFFF}) leaves them
     *             unchanged
     */
    void sprite(SpriteId sprite, Rect rect, int tint);

    /**
     * Draws a sprite as a nine-slice over a rectangle: the corners at their native size, the edges stretched along
     * their length, and the center stretched both ways. An engine that knows the sprite's slicing natively
     * (stored with the image, for example) may use that and ignore {@code border}. If the rectangle is smaller than the
     * borders, shrink the borders rather than drawing outside it.
     *
     * @param sprite the sprite
     * @param rect the area
     * @param border the width of each fixed border, in the sprite's pixels
     * @param tint an ARGB color multiplied with the sprite's pixels
     */
    void nineSlice(SpriteId sprite, Rect rect, Insets border, int tint);

    /**
     * Draws one line of text with the top-left corner of its line box at the given point. The span holds no line
     * breaks. Apply every part of the span's style the engine supports: color, bold, italic, underline,
     * strikethrough and shadow.
     *
     * @param span the text and its style
     * @param x the left edge
     * @param y the top of the line box (not the baseline)
     */
    void text(TextSpan span, float x, float y);

    /**
     * Limits drawing to a rectangle, intersected with the current clip, until the matching {@link #popClip()}.
     *
     * @param rect the area drawing is limited to, in the current transform
     */
    void pushClip(Rect rect);

    /**
     * Restores the clip in place before the matching {@link #pushClip(Rect)}.
     */
    void popClip();

    /**
     * Shifts every later coordinate by an offset, until the matching {@link #popTransform()}. Offsets add up when
     * nested.
     *
     * @param dx the horizontal offset
     * @param dy the vertical offset
     */
    void pushTranslation(float dx, float dy);

    /**
     * Multiplies every later coordinate and size by a factor, about the current origin, until the matching
     * {@link #popTransform()}. Factors multiply when nested, and translations pushed inside are scaled too. Text
     * drawn while scaled must come out at the scaled size, matching the platform's text widths times the factor.
     *
     * @param scale the factor, greater than 0
     */
    void pushScale(float scale);

    /**
     * Restores the transform in place before the matching {@link #pushTranslation(float, float)} or
     * {@link #pushScale(float)}.
     */
    void popTransform();

    /**
     * Draws an engine visual stretched over a rectangle. Draw the visuals this adapter makes; for any other visual
     * draw a visible placeholder instead of throwing, as for a missing sprite.
     *
     * @param visual the visual
     * @param rect the area
     * @param tint an ARGB color multiplied with the visual where the engine supports tinting it; opaque white leaves
     *             it unchanged
     */
    void visual(Visual visual, Rect rect, int tint);

    /**
     * Starts a new layer: everything drawn after this call must appear above everything drawn before it, even if
     * the engine sorts draws within a layer. A screen stack calls it between screens. Engines that always draw in
     * call order may do nothing.
     */
    void nextLayer();

    /**
     * Asks for a mouse cursor. A host calls it at most once per frame, after drawing, while the pointer is over a
     * component; the adapter shows the cursor (or the closest it has) until asked for another. In a frame without
     * a request, show the default cursor.
     *
     * @param cursor the cursor to show
     */
    void requestCursor(CursorKind cursor);
}
