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
package org.inspirenxe.strata.spi;

import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.extension.Gradient;
import org.inspirenxe.strata.api.extension.Shape;
import org.inspirenxe.strata.api.extension.Stroke;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.visual.PixelBuffer;
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
 *     <li>Calls may be recorded and replayed later; nothing requires immediate drawing.</li>
 *     <li>Clips and transforms form stacks. Strata balances every push with a pop within one frame, and pops in
 *     reverse order, even when a component throws. A clip applies in the transform current when it was pushed and
 *     intersects any clip already in place.</li>
 *     <li>Never throw for missing resources: a sprite that cannot be found should draw something visible (the Swing
 *     adapter draws magenta) so the problem shows on screen.</li>
 *     <li>A sprite id names one sprite, which an adapter may hold in several densities. Draw the densest image the
 *     device pixels under the rectangle can show, so a sprite stays sharp on a display of 1.5 or 2 device pixels per
 *     unit. Rectangles are in UI units whatever the image's pixel size, so a denser image draws at the same size, and
 *     a sized nine-slice's border, given in units, stays the same.</li>
 *     <li>Text drawn with {@link #text} must line up with the widths and line height the platform's
 *     {@link org.inspirenxe.strata.api.text.TextMetrics} reports.</li>
 * </ul>
 *
 * <h2>Shapes</h2>
 * <p>Lines, outlines, ellipses, arcs, rounded rectangles, shapes, gradients at an angle and shadows are optional.
 * Core calls them only when the platform's {@link Capabilities#drawsShapes()} is true; otherwise it draws each of them
 * from {@link #fill} and {@link #fillGradient(Rect, int, int)} calls, one device pixel high, and never calls them.
 * Their defaults throw {@link UnsupportedOperationException}. An adapter that reports {@code drawsShapes} must
 * implement every one of them, and should:</p>
 * <ul>
 *     <li>anti-alias them by default, and draw hard edges between {@link #pushSmoothing pushSmoothing(false)} and
 *     its {@link #popSmoothing()};</li>
 *     <li>count a radius below 0 as 0 and one above half the shorter side of its rectangle as half of it, and draw
 *     nothing for a stroke of width 0;</li>
 *     <li>measure angles in radians, with 0 at the right of the oval and angles turning clockwise on screen, as
 *     {@link Shape.Builder#arc} describes;</li>
 *     <li>keep its own copy of a {@link Shape} by identity, since shapes never change.</li>
 * </ul>
 * <p>{@link #pushRoundClip}, {@link #pushSmoothing} and {@link #pixels} have defaults that work on every canvas: a
 * clip to the whole rectangle, nothing, and the placeholder of a visual the canvas does not know.</p>
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
     * Draws part of a sprite stretched over a rectangle. The part is given in fractions of the whole image, from 0
     * at its left or top edge to 1 at its right or bottom edge, so callers need not know its pixel size. The host
     * draws sized nine-slices and tiled sprites from these calls when the engine stores no slicing with its
     * images.
     *
     * <p>The default draws the whole sprite with {@link #sprite}, scaled and moved so the part lands on the
     * rectangle, inside a clip of the rectangle. Override it when the engine can draw part of an image directly:
     * at scales that are not whole numbers, pixels next to the part can show at the clip's edge, and pieces drawn
     * side by side then show seams.</p>
     *
     * @param sprite the sprite
     * @param rect the area
     * @param u0 the left edge of the part, from 0 to 1
     * @param v0 the top edge of the part, from 0 to 1
     * @param u1 the right edge of the part, greater than {@code u0}
     * @param v1 the bottom edge of the part, greater than {@code v0}
     * @param tint an ARGB color multiplied with the sprite's pixels
     */
    default void spriteRegion(final SpriteId sprite, final Rect rect, final float u0, final float v0,
                              final float u1, final float v1, final int tint) {
        final float scaleX = rect.width() / (u1 - u0);
        final float scaleY = rect.height() / (v1 - v0);
        this.pushClip(rect);
        try {
            this.sprite(sprite, new Rect(rect.x() - u0 * scaleX, rect.y() - v0 * scaleY, scaleX, scaleY), tint);
        } finally {
            this.popClip();
        }
    }

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
     * breaks, and never holds an {@linkplain TextSpan#element() inline element}: core draws sprites and visuals in
     * text itself, through {@link #sprite} and {@link #visual}. Apply every part of the span's style the engine
     * supports: color, bold, italic, underline, strikethrough and shadow, in the monospaced font when the style asks
     * for it and the platform's {@link Capabilities#monospaceFont()} is true.
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
     * Limits drawing to a rectangle with rounded corners, intersected with the current clip, until the matching
     * {@link #popClip()}. Core calls it only when {@link Capabilities#drawsShapes()} is true. The default clips to
     * the whole rectangle with {@link #pushClip}, corners included.
     *
     * @param rect the area drawing is limited to, in the current transform
     * @param radius the radius of each corner, in UI units
     */
    default void pushRoundClip(final Rect rect, final float radius) {
        this.pushClip(rect);
    }

    /**
     * Restores the clip in place before the matching {@link #pushClip(Rect)} or {@link #pushRoundClip}.
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
     * Multiplies the alpha of every later color and tint by a factor, until the matching {@link #popOpacity()}.
     * Factors multiply when nested. Core pushes one around each component whose opacity is between 0 and 1, so a
     * fade covers everything the component and its children draw.
     *
     * <p>The default does nothing, so an adapter that does not override both methods draws such components opaque
     * and fades show as jumps at their end. An engine that can fade only some kinds of draw, such as text and fills
     * but not its own visuals, says which in its canvas Javadoc.</p>
     *
     * @param opacity the factor, from 0 exclusive to 1 exclusive
     */
    default void pushOpacity(final float opacity) {
    }

    /**
     * Restores the opacity in place before the matching {@link #pushOpacity(float)}. The default does nothing.
     */
    default void popOpacity() {
    }

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
     * the engine sorts draws within a layer. Core calls it between the screens of a screen stack, before each
     * overlay and before the tooltip. Engines that always draw in call order may do nothing.
     */
    void nextLayer();

    /**
     * Asks for a mouse cursor. A host calls it at most once per frame, after drawing, while the pointer is over a
     * component or a drag carries a transfer; the adapter shows the cursor (or the closest it has) until asked for
     * another. In a frame without a request, show the default cursor. The host asks only for kinds
     * {@link #showsCursor} accepts, walking {@link CursorKind#fallback()} down to {@link CursorKind#ARROW} for the
     * others, but the canvas must accept every kind without throwing.
     *
     * @param cursor the cursor to show
     */
    void requestCursor(CursorKind cursor);

    /**
     * Returns whether the canvas can show a cursor kind. The host asks before {@link #requestCursor}, and asks for
     * the kind's {@linkplain CursorKind#fallback() fallback} instead when the answer is false. It must answer true
     * for {@link CursorKind#ARROW}, from any frame, without drawing anything.
     *
     * <p>The default accepts the eight kinds Strata had before the diagonal, wait, progress, grab, copy, no-drop and
     * help cursors were added: {@code ARROW}, {@code TEXT}, {@code HAND}, {@code CROSSHAIR},
     * {@code RESIZE_HORIZONTAL}, {@code RESIZE_VERTICAL}, {@code RESIZE_ALL} and {@code NOT_ALLOWED}. A canvas that
     * shows more overrides it.</p>
     *
     * @param cursor the kind
     * @return true if {@link #requestCursor} shows it as itself
     */
    default boolean showsCursor(final CursorKind cursor) {
        return switch (cursor) {
            case ARROW, TEXT, HAND, CROSSHAIR, RESIZE_HORIZONTAL, RESIZE_VERTICAL, RESIZE_ALL, NOT_ALLOWED -> true;
            default -> false;
        };
    }

    /**
     * Turns anti-aliasing on or off for what is drawn until the matching {@link #popSmoothing()}. Settings nest, and
     * shapes are smooth outside any push. Core calls it only when {@link Capabilities#drawsShapes()} is true, and
     * balances every push with a pop as it does clips. The canvas must apply it to shapes and may apply it to
     * sprites and text. The default does nothing.
     *
     * @param smooth true for anti-aliased edges, false for hard ones
     */
    default void pushSmoothing(final boolean smooth) {
    }

    /**
     * Restores the smoothing in place before the matching {@link #pushSmoothing}. The default does nothing.
     */
    default void popSmoothing() {
    }

    /**
     * Fills a rectangle with a linear gradient at an angle, as {@link Gradient} describes. Called only when
     * {@link Capabilities#drawsShapes()} is true. The default throws.
     *
     * @param rect the area
     * @param gradient the gradient
     * @throws UnsupportedOperationException by default
     */
    default void fillGradient(final Rect rect, final Gradient gradient) {
        throw new UnsupportedOperationException("This canvas draws no gradients at an angle");
    }

    /**
     * Draws a straight line centered on the segment between two points, with the stroke's cap at both ends. Called
     * only when {@link Capabilities#drawsShapes()} is true. The default throws.
     *
     * @param x1 the start's x coordinate
     * @param y1 the start's y coordinate
     * @param x2 the end's x coordinate
     * @param y2 the end's y coordinate
     * @param stroke the width and caps
     * @param argb the color
     * @throws UnsupportedOperationException by default
     */
    default void line(final float x1, final float y1, final float x2, final float y2, final Stroke stroke,
                      final int argb) {
        throw new UnsupportedOperationException("This canvas draws no lines");
    }

    /**
     * Draws connected straight lines through points, centered on them, with the stroke's joins between segments and
     * its caps at the two ends. The array is valid only during the call; copy what you keep. Core checks that the
     * array holds {@code count} points and passes a count of at least 2. Called only when
     * {@link Capabilities#drawsShapes()} is true. The default throws.
     *
     * @param points the points as x, y pairs
     * @param count how many points to draw from the start of the array
     * @param stroke the width, caps and joins
     * @param argb the color
     * @throws UnsupportedOperationException by default
     */
    default void polyline(final float[] points, final int count, final Stroke stroke, final int argb) {
        throw new UnsupportedOperationException("This canvas draws no polylines");
    }

    /**
     * Fills a rectangle with rounded corners. Called only when {@link Capabilities#drawsShapes()} is true. The
     * default throws.
     *
     * @param rect the area
     * @param radius the radius of each corner
     * @param argb the color
     * @throws UnsupportedOperationException by default
     */
    default void fillRoundRect(final Rect rect, final float radius, final int argb) {
        throw new UnsupportedOperationException("This canvas draws no rounded rectangles");
    }

    /**
     * Draws the outline of a rectangle inside it, so the stroke's outer edge is the rectangle's edge and its outer
     * corners have the radius. The stroke's caps and joins do not apply, and a stroke wider than half the rectangle
     * fills it. Called only when {@link Capabilities#drawsShapes()} is true. The default throws.
     *
     * @param rect the area
     * @param radius the radius of each outer corner
     * @param stroke the width
     * @param argb the color
     * @throws UnsupportedOperationException by default
     */
    default void strokeRect(final Rect rect, final float radius, final Stroke stroke, final int argb) {
        throw new UnsupportedOperationException("This canvas draws no outlines");
    }

    /**
     * Fills the ellipse that fits a rectangle. Called only when {@link Capabilities#drawsShapes()} is true. The
     * default throws.
     *
     * @param oval the rectangle the ellipse fills
     * @param argb the color
     * @throws UnsupportedOperationException by default
     */
    default void fillEllipse(final Rect oval, final int argb) {
        throw new UnsupportedOperationException("This canvas draws no ellipses");
    }

    /**
     * Draws the outline of the ellipse that fits a rectangle inside it, so the stroke's outer edge is the ellipse.
     * The stroke's caps and joins do not apply. Called only when {@link Capabilities#drawsShapes()} is true. The
     * default throws.
     *
     * @param oval the rectangle the ellipse fills
     * @param stroke the width
     * @param argb the color
     * @throws UnsupportedOperationException by default
     */
    default void strokeEllipse(final Rect oval, final Stroke stroke, final int argb) {
        throw new UnsupportedOperationException("This canvas draws no ellipses");
    }

    /**
     * Draws part of the outline of the ellipse that fits a rectangle, inside it as {@link #strokeEllipse} does, with
     * the stroke's caps at both ends. Core passes a sweep of less than a full turn either way, and draws a full turn
     * with {@code strokeEllipse}. Called only when {@link Capabilities#drawsShapes()} is true. The default throws.
     *
     * @param oval the rectangle the ellipse fills
     * @param startRadians the angle the arc starts at: 0 at the right, turning clockwise on screen
     * @param sweepRadians how far the arc turns; negative turns counterclockwise
     * @param stroke the width and caps
     * @param argb the color
     * @throws UnsupportedOperationException by default
     */
    default void strokeArc(final Rect oval, final float startRadians, final float sweepRadians, final Stroke stroke,
                           final int argb) {
        throw new UnsupportedOperationException("This canvas draws no arcs");
    }

    /**
     * Fills a shape, closing every subpath, by the nonzero winding rule. Called only when
     * {@link Capabilities#drawsShapes()} is true. The default throws.
     *
     * @param shape the shape
     * @param argb the color
     * @throws UnsupportedOperationException by default
     */
    default void fillShape(final Shape shape, final int argb) {
        throw new UnsupportedOperationException("This canvas draws no shapes");
    }

    /**
     * Draws the outline of a shape centered on its path, with the stroke's joins at corners and its caps on the ends
     * of subpaths that are not closed. Called only when {@link Capabilities#drawsShapes()} is true. The default
     * throws.
     *
     * @param shape the shape
     * @param stroke the width, caps and joins
     * @param argb the color
     * @throws UnsupportedOperationException by default
     */
    default void strokeShape(final Shape shape, final Stroke stroke, final int argb) {
        throw new UnsupportedOperationException("This canvas draws no shapes");
    }

    /**
     * Draws the soft shadow of a rounded rectangle: the color over the rectangle, fading to nothing over
     * {@code blur} units outside its edges. Core passes a blur greater than 0, and draws a hard shadow with
     * {@link #fillRoundRect}. An adapter may keep a blurred image for each size and blur it draws. Called only when
     * {@link Capabilities#drawsShapes()} is true. The default throws.
     *
     * @param rect the rectangle that casts the shadow
     * @param radius the radius of its corners
     * @param blur how far outside the rectangle the shadow fades out, in UI units
     * @param argb the color at full strength
     * @throws UnsupportedOperationException by default
     */
    default void shadow(final Rect rect, final float radius, final float blur, final int argb) {
        throw new UnsupportedOperationException("This canvas draws no shadows");
    }

    /**
     * Draws a pixel buffer stretched over a rectangle, buffer pixel {@code (0, 0)} at its top left corner. Keep one
     * engine copy of each buffer drawn, such as a texture, and copy the pixels again when the buffer's
     * {@linkplain PixelBuffer#version() version} differs from the one copied; drop copies of buffers no longer drawn.
     * Core calls it on every canvas. The default draws the placeholder of a visual the canvas did not make, through
     * {@link #visual}.
     *
     * @param buffer the pixels
     * @param rect the area
     * @param tint an ARGB color multiplied with the pixels; opaque white leaves them unchanged
     */
    default void pixels(final PixelBuffer buffer, final Rect rect, final int tint) {
        this.visual(PixelsPlaceholder.INSTANCE, rect, tint);
    }
}
