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
package org.inspirenxe.strata.api.extension;

import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.layout.VerticalAlignment;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.visual.PixelBuffer;
import org.inspirenxe.strata.api.visual.Visual;

/**
 * What a component draws with during {@link CustomComponent#paint}. Coordinates are in the host's coordinates, in
 * UI units; draw within the bounds you were given.
 *
 * <p>Colors are ARGB ({@code 0xAARRGGBB}); a color with alpha 0 draws nothing. Later calls draw over earlier ones.
 * The engine may round coordinates to whole pixels.</p>
 *
 * <p>Clipping, translation and scaling are scoped to a callback, so the engine's state is always restored, even if
 * the callback throws.</p>
 *
 * <p>Lines, outlines, ellipses, arcs, rounded rectangles, shapes, gradients at an angle and shadows are anti-aliased
 * where the engine can, and {@link #smoothed} asks for hard edges. An engine that draws only rectangles still draws
 * them, built from rectangles one device pixel high, so they look stepped there. Radii below 0 count as 0, and radii
 * larger than half the shorter side of their rectangle as half of it. A {@link Stroke}, a {@link Gradient} and a
 * {@link Shape} are values: make them once and keep them in fields, so a frame allocates nothing.</p>
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
     * Fills a rectangle with a gradient at any angle, with any number of stops.
     *
     * @param rect the area
     * @param gradient the gradient
     */
    void fillGradient(Rect rect, Gradient gradient);

    /**
     * Draws a straight line, centered on the segment between two points, with caps at both ends.
     *
     * @param x1 the start's x coordinate
     * @param y1 the start's y coordinate
     * @param x2 the end's x coordinate
     * @param y2 the end's y coordinate
     * @param stroke the width, caps and joins
     * @param argb the color
     */
    void line(float x1, float y1, float x2, float y2, Stroke stroke, int argb);

    /**
     * Draws connected straight lines through points, centered on them, with joins at the points between and caps
     * at the two ends. The array is read during the call and not kept, so a chart can keep one array and refill it
     * for every frame.
     *
     * @param points the points as x, y pairs: point {@code i} is {@code points[2 * i]}, {@code points[2 * i + 1]}
     * @param count how many points to draw from the start of the array; fewer than 2 draw nothing
     * @param stroke the width, caps and joins
     * @param argb the color
     * @throws IllegalArgumentException if {@code count} is negative or the array holds fewer than {@code count}
     *                                  points
     */
    void polyline(float[] points, int count, Stroke stroke, int argb);

    /**
     * Fills a rectangle with rounded corners.
     *
     * @param rect the area
     * @param radius the radius of each corner; 0 for square corners
     * @param argb the color
     */
    void fillRoundRect(Rect rect, float radius, int argb);

    /**
     * Draws the outline of a rectangle, inside it: the stroke's outer edge is the rectangle's edge, so a border drawn
     * over a component's bounds stays inside them. The corners follow the radius; the stroke's caps and joins do not
     * apply. A stroke wider than half the rectangle fills it.
     *
     * @param rect the area
     * @param radius the radius of each outer corner; 0 for square corners
     * @param stroke the width
     * @param argb the color
     */
    void strokeRect(Rect rect, float radius, Stroke stroke, int argb);

    /**
     * Fills the ellipse that fits a rectangle.
     *
     * @param oval the rectangle the ellipse fills
     * @param argb the color
     */
    void fillEllipse(Rect oval, int argb);

    /**
     * Draws the outline of the ellipse that fits a rectangle, inside it: the stroke's outer edge is the ellipse. The
     * stroke's caps and joins do not apply.
     *
     * @param oval the rectangle the ellipse fills
     * @param stroke the width
     * @param argb the color
     */
    void strokeEllipse(Rect oval, Stroke stroke, int argb);

    /**
     * Draws part of the outline of the ellipse that fits a rectangle, inside it, as for {@link #strokeEllipse}, with
     * the stroke's caps at both ends. Angles are as for {@link Shape.Builder#arc}: 0 is at the right and angles turn
     * clockwise on screen. A sweep of a full turn or more either way draws the whole outline.
     *
     * @param oval the rectangle the ellipse fills
     * @param startRadians the angle the arc starts at, in radians
     * @param sweepRadians how far the arc turns, in radians; negative turns counterclockwise
     * @param stroke the width and caps
     * @param argb the color
     */
    void strokeArc(Rect oval, float startRadians, float sweepRadians, Stroke stroke, int argb);

    /**
     * Fills a shape, closing every subpath, by the nonzero winding rule.
     *
     * @param shape the shape
     * @param argb the color
     */
    void fillShape(Shape shape, int argb);

    /**
     * Draws the outline of a shape, centered on its path, with the stroke's joins at its corners and caps on the
     * ends of subpaths that are not closed.
     *
     * @param shape the shape
     * @param stroke the width, caps and joins
     * @param argb the color
     */
    void strokeShape(Shape shape, Stroke stroke, int argb);

    /**
     * Draws the soft shadow of a rounded rectangle: the color over the rectangle, fading to nothing over
     * {@code blur} units outside its edges. Draw it before what casts it, at an offset if the light comes from one
     * side.
     *
     * @param rect the rectangle that casts the shadow
     * @param radius the radius of its corners
     * @param blur how far outside the rectangle the shadow fades out, in UI units; 0 or less gives a hard edge
     * @param argb the color at full strength
     */
    void shadow(Rect rect, float radius, float blur, int argb);

    /**
     * Runs {@code draw} with anti-aliasing turned on or off for what it draws. Shapes are smooth by default; pixel
     * art and bitmap fonts want hard edges. Engines that cannot anti-alias draw hard edges either way, and an engine
     * may apply the setting to sprites and text too.
     *
     * @param smooth true for anti-aliased edges, false for hard ones
     * @param draw the drawing
     */
    void smoothed(boolean smooth, Runnable draw);

    /**
     * Runs {@code draw} with drawing limited to a rectangle with rounded corners, intersected with any clip already
     * in place. It clips drawing only; hit testing stays rectangular. An engine that draws only rectangles clips to
     * the whole rectangle, corners included.
     *
     * @param rect the area drawing is limited to
     * @param radius the radius of each corner
     * @param draw the drawing to clip
     */
    void clippedRound(Rect rect, float radius, Runnable draw);

    /**
     * Draws a pixel buffer stretched over a rectangle. An engine copies the pixels again only when the buffer's
     * {@linkplain PixelBuffer#version() version} moved since it last drew it. An engine that cannot draw pixels
     * shows the placeholder it shows for a visual it does not know.
     *
     * @param buffer the pixels
     * @param rect the area
     * @param tint an ARGB color multiplied with the pixels; opaque white leaves them unchanged
     */
    void pixels(PixelBuffer buffer, Rect rect, int tint);

    /**
     * Returns how many device pixels one unit covers where the painter draws now: the host's
     * {@linkplain Environment#pixelsPerUnit() density} times every {@link #scaled} in progress. A line
     * {@code 1 / pixelsPerUnit()} units wide is one device pixel wide.
     *
     * @return device pixels per unit, greater than 0
     */
    float pixelsPerUnit();

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
     * @param border the width of each fixed border, in the sprite's pixels; an engine that stores the slicing
     *               with the image may use that instead
     * @param tint an ARGB color multiplied with the sprite
     */
    void nineSlice(SpriteId sprite, Rect rect, Insets border, int tint);

    /**
     * Draws a sprite as a nine-slice whose image stands for a given size, with its edges and center stretched or
     * repeated. See {@link org.inspirenxe.strata.api.theme.Paint.NineSlice}.
     *
     * @param sprite the sprite
     * @param rect the area
     * @param size the size in UI units that the whole image stands for
     * @param border the width of each fixed border, in units of {@code size}
     * @param tiled true to repeat the edges and the center at their size, false to stretch them
     * @param tint an ARGB color multiplied with the sprite
     * @throws IllegalArgumentException if the size is not positive or the borders do not fit in it
     */
    void nineSlice(SpriteId sprite, Rect rect, Size size, Insets border, boolean tiled, int tint);

    /**
     * Draws a sprite repeated over a rectangle from its top left corner, cropping the last column and row. See
     * {@link org.inspirenxe.strata.api.theme.Paint.Tiled}.
     *
     * @param sprite the sprite
     * @param rect the area
     * @param size the size in UI units of one copy of the image
     * @param tint an ARGB color multiplied with the sprite
     * @throws IllegalArgumentException if the size is not positive
     */
    void tiled(SpriteId sprite, Rect rect, Size size, int tint);

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
     * Draws one line of text aligned in a rectangle, measured with the host's
     * {@link org.inspirenxe.strata.api.text.TextMetrics}. Across, {@link HorizontalAlignment#LEFT} and
     * {@link HorizontalAlignment#STRETCH} put it at the left edge, {@link HorizontalAlignment#CENTER} in the middle
     * and {@link HorizontalAlignment#RIGHT} against the right edge. Down, {@link VerticalAlignment#TOP},
     * {@link VerticalAlignment#STRETCH} and {@link VerticalAlignment#BASELINE} put the line's top at the top edge,
     * {@link VerticalAlignment#MIDDLE} centers its line height and {@link VerticalAlignment#BOTTOM} puts its bottom
     * at the bottom edge. Centering keeps whole numbers whole: with a whole rectangle and a whole text size, the
     * spare unit of an odd difference goes right or below. The alignment does not mirror right to left, and text
     * wider or taller than the rectangle is not cut; draw inside {@link #clipped} for that. The line's baseline is
     * {@link org.inspirenxe.strata.api.text.TextMetrics#ascent} below where its top lands.
     *
     * @param span the text and its style
     * @param rect the rectangle to align in
     * @param horizontal where the text sits across
     * @param vertical where the text sits down
     */
    void text(TextSpan span, Rect rect, HorizontalAlignment horizontal, VerticalAlignment vertical);

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

    /**
     * Runs {@code draw} with every coordinate and size multiplied by a factor, about the origin. To draw text at
     * 0.8 size at a point, draw it at the point divided by 0.8 inside this call. Clips set inside are scaled too.
     *
     * @param scale the factor; 1 changes nothing
     * @param draw the drawing to scale
     */
    void scaled(float scale, Runnable draw);

    /**
     * Draws an engine visual stretched over a rectangle. A visual the engine cannot draw shows a placeholder.
     *
     * @param visual the visual
     * @param rect the area
     * @param tint an ARGB color multiplied with the visual where the engine supports it; opaque white leaves it
     *             unchanged
     */
    void visual(Visual visual, Rect rect, int tint);
}
