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
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.theme.SpriteId;
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
