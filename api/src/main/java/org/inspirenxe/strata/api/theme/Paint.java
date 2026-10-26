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
package org.inspirenxe.strata.api.theme;

import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;

import java.util.Objects;

/**
 * How to fill a rectangle: a solid color, a color with rounded corners, a vertical gradient, a stretched sprite, a
 * tiled sprite or a nine-slice sprite. Themes map style keys to paints, and components draw them with {@link #paint} without caring which kind
 * they hold.
 *
 * <p>Colors are ARGB ints ({@code 0xAARRGGBB}). A color with alpha 0 draws nothing; {@link #NONE} is such a
 * color.</p>
 */
public sealed interface Paint {
    /**
     * Draws nothing. The default background of every component.
     */
    Paint NONE = new Solid(0x00000000);

    /**
     * Returns frosted glass with square corners: what lies behind blurred, with a tint over it. See
     * {@link #backdrop(float, int, float)}.
     *
     * @param blur how far the blur reaches, in UI units
     * @param tint the ARGB color over the blurred backdrop
     * @return the paint
     * @throws IllegalArgumentException if the blur is negative or not finite
     */
    static Paint backdrop(final float blur, final int tint) {
        return new Backdrop(blur, tint, 0f);
    }

    /**
     * Returns frosted glass: what lies behind the area blurred, with a tint over it and rounded corners, as
     * {@link Painter#backdrop} draws it. Give it to a panel with {@code setBackground} or to a theme key. An engine
     * that cannot blur draws the tint alone, so pick a tint that reads on its own too, such as a dark color at three
     * quarters alpha.
     *
     * @param blur how far the blur reaches, in UI units; 0 tints without blurring
     * @param tint the ARGB color over the blurred backdrop
     * @param radius the radius of each corner, in UI units
     * @return the paint
     * @throws IllegalArgumentException if the blur or the radius is negative or not finite
     */
    static Paint backdrop(final float blur, final int tint, final float radius) {
        return new Backdrop(blur, tint, radius);
    }

    /**
     * Returns a solid color.
     *
     * @param argb the color
     * @return the paint
     */
    static Paint solid(final int argb) {
        return new Solid(argb);
    }

    /**
     * Returns a solid color with rounded corners. See {@link Rounded}.
     *
     * @param argb the color
     * @param radius the radius of each corner, in UI units
     * @return the paint
     * @throws IllegalArgumentException if the radius is negative or not finite
     */
    static Paint rounded(final int argb, final float radius) {
        return new Rounded(argb, radius);
    }

    /**
     * Returns a vertical gradient.
     *
     * @param topArgb the color at the top edge
     * @param bottomArgb the color at the bottom edge
     * @return the paint
     */
    static Paint gradient(final int topArgb, final int bottomArgb) {
        return new Gradient(topArgb, bottomArgb);
    }

    /**
     * Returns a sprite stretched over the whole rectangle, untinted.
     *
     * @param sprite the sprite
     * @return the paint
     */
    static Paint sprite(final SpriteId sprite) {
        return new Sprite(sprite, 0xFFFFFFFF);
    }

    /**
     * Returns a nine-slice sprite, untinted: corners keep their size, edges stretch along one axis, and the center
     * stretches both ways. See {@link NineSlice}.
     *
     * @param sprite the sprite
     * @param border the width of each fixed border, in the sprite's pixels
     * @return the paint
     */
    static Paint nineSlice(final SpriteId sprite, final Insets border) {
        return new NineSlice(sprite, border, 0xFFFFFFFF);
    }

    /**
     * Returns a nine-slice sprite that stands for an image of a given size, untinted. Edges and the center
     * stretch. See {@link NineSlice}.
     *
     * @param sprite the sprite
     * @param size the size in UI units that the whole image stands for, whatever its pixel size
     * @param border the width of each fixed border, in units of {@code size}
     * @return the paint
     * @throws IllegalArgumentException if the size is not positive or the borders do not fit in it
     */
    static Paint nineSlice(final SpriteId sprite, final Size size, final Insets border) {
        return new NineSlice(sprite, border, 0xFFFFFFFF, size, false);
    }

    /**
     * Returns a nine-slice sprite whose edges and center repeat at their size instead of stretching, untinted.
     * See {@link NineSlice}.
     *
     * @param sprite the sprite
     * @param size the size in UI units that the whole image stands for, whatever its pixel size
     * @param border the width of each fixed border, in units of {@code size}
     * @return the paint
     * @throws IllegalArgumentException if the size is not positive or the borders do not fit in it
     */
    static Paint tiledNineSlice(final SpriteId sprite, final Size size, final Insets border) {
        return new NineSlice(sprite, border, 0xFFFFFFFF, size, true);
    }

    /**
     * Returns a sprite repeated over the rectangle at a given size, untinted. See {@link Tiled}.
     *
     * @param sprite the sprite
     * @param size the size in UI units of one copy of the image
     * @return the paint
     * @throws IllegalArgumentException if the size is not positive
     */
    static Paint tiled(final SpriteId sprite, final Size size) {
        return new Tiled(sprite, size, 0xFFFFFFFF);
    }

    /**
     * Draws this paint over a rectangle.
     *
     * @param painter what to draw with
     * @param rect the area to cover
     */
    default void paint(final Painter painter, final Rect rect) {
        switch (this) {
            case Backdrop backdrop -> painter.backdrop(rect, backdrop.radius, backdrop.blur, backdrop.tint);
            case Solid solid -> {
                if ((solid.argb >>> 24) != 0) {
                    painter.fill(rect, solid.argb);
                }
            }
            case Rounded rounded -> {
                if ((rounded.argb >>> 24) != 0) {
                    painter.fillRoundRect(rect, rounded.radius, rounded.argb);
                }
            }
            case Gradient gradient -> painter.fillGradient(rect, gradient.topArgb, gradient.bottomArgb);
            case Sprite sprite -> painter.sprite(sprite.sprite, rect, sprite.tint);
            case Tiled tiled -> painter.tiled(tiled.sprite, rect, tiled.size, tiled.tint);
            case NineSlice slice -> {
                if (slice.size == null) {
                    painter.nineSlice(slice.sprite, rect, slice.border, slice.tint);
                } else {
                    painter.nineSlice(slice.sprite, rect, slice.size, slice.border, slice.tiled, slice.tint);
                }
            }
        }
    }

    /**
     * A blurred backdrop with a tint over it, as {@link #backdrop(float, int, float)} describes.
     *
     * @param blur how far the blur reaches, in UI units; 0 tints without blurring
     * @param tint the ARGB color over the blurred backdrop
     * @param radius the radius of each corner, in UI units
     */
    record Backdrop(float blur, int tint, float radius) implements Paint {
        /**
         * Checks the blur and the radius.
         *
         * @throws IllegalArgumentException if either is negative or not finite
         */
        public Backdrop {
            if (!(blur >= 0f) || Float.isInfinite(blur)) {
                throw new IllegalArgumentException("Blur must be finite and not negative: " + blur);
            }
            if (!(radius >= 0f) || Float.isInfinite(radius)) {
                throw new IllegalArgumentException("Corner radius must be finite and not negative: " + radius);
            }
        }
    }

    /**
     * A solid color.
     *
     * @param argb the color; alpha 0 draws nothing
     */
    record Solid(int argb) implements Paint {
    }

    /**
     * A solid color with rounded corners, drawn with {@link Painter#fillRoundRect}. A radius larger than half the
     * shorter side of the rectangle counts as half of it. An engine that draws only rectangles draws the corners in
     * steps of a device pixel.
     *
     * @param argb the color; alpha 0 draws nothing
     * @param radius the radius of each corner, in UI units; 0 gives square corners
     */
    record Rounded(int argb, float radius) implements Paint {
        /**
         * Checks the radius.
         *
         * @throws IllegalArgumentException if the radius is negative or not finite
         */
        public Rounded {
            if (!(radius >= 0f) || Float.isInfinite(radius)) {
                throw new IllegalArgumentException("Corner radius must be finite and not negative: " + radius);
            }
        }
    }

    /**
     * A gradient from top to bottom.
     *
     * @param topArgb the color at the top edge
     * @param bottomArgb the color at the bottom edge
     */
    record Gradient(int topArgb, int bottomArgb) implements Paint {
    }

    /**
     * A sprite stretched over the whole rectangle.
     *
     * @param sprite the sprite
     * @param tint an ARGB color multiplied with the sprite; opaque white leaves it unchanged
     */
    record Sprite(SpriteId sprite, int tint) implements Paint {
        /**
         * Checks the sprite.
         */
        public Sprite {
            Objects.requireNonNull(sprite, "sprite");
        }
    }

    /**
     * A sprite repeated from the rectangle's top left corner, one copy per {@code size}. The copies in the last
     * column and row are cropped to the rectangle.
     *
     * <p>An engine that stores the tiling with the image may use that instead; keep the two in agreement so
     * every engine draws the same.</p>
     *
     * @param sprite the sprite
     * @param size the size in UI units of one copy of the image, whatever its pixel size
     * @param tint an ARGB color multiplied with the sprite; opaque white leaves it unchanged
     */
    record Tiled(SpriteId sprite, Size size, int tint) implements Paint {
        /**
         * Checks the arguments.
         *
         * @throws IllegalArgumentException if the size is not positive
         */
        public Tiled {
            Objects.requireNonNull(sprite, "sprite");
            Objects.requireNonNull(size, "size");
            if (!(size.width() > 0f) || !(size.height() > 0f)) {
                throw new IllegalArgumentException("Tile size must be positive: " + size);
            }
        }
    }

    /**
     * A sprite cut into a 3 by 3 grid. Corners keep their size, edges stretch or repeat along one axis, and the
     * center stretches or repeats both ways, so one small image can frame a rectangle of any size. When the
     * rectangle is smaller than the borders, they shrink in proportion.
     *
     * <p>Without a {@code size}, {@code border} is in the sprite's pixels and the image is drawn at one pixel per
     * UI unit. With one, the whole image stands for {@code size} UI units and {@code border} is in those units, so
     * an image with twice the pixels draws the same borders, only sharper. {@code tiled} repeats the edges and the
     * center at their size instead of stretching them, cropping the last copy, and needs a size.</p>
     *
     * <p>An engine that stores the slicing with the image may use that and ignore {@code border}, {@code size}
     * and {@code tiled}; keep them in agreement so every engine draws the same.</p>
     *
     * @param sprite the sprite
     * @param border the width of each fixed border, in the sprite's pixels, or in units of {@code size} when there
     *               is one
     * @param tint an ARGB color multiplied with the sprite; opaque white leaves it unchanged
     * @param size the size in UI units that the whole image stands for, or {@code null} for its pixel size
     * @param tiled true to repeat the edges and the center instead of stretching them
     */
    record NineSlice(SpriteId sprite, Insets border, int tint, Size size, boolean tiled) implements Paint {
        /**
         * Checks the arguments.
         *
         * @throws IllegalArgumentException if {@code tiled} is set without a size, the size is not positive, or
         *     the borders do not fit in it
         */
        public NineSlice {
            Objects.requireNonNull(sprite, "sprite");
            Objects.requireNonNull(border, "border");
            if (size == null) {
                if (tiled) {
                    throw new IllegalArgumentException("A tiled nine-slice needs a size");
                }
            } else {
                if (!(size.width() > 0f) || !(size.height() > 0f)) {
                    throw new IllegalArgumentException("Nine-slice size must be positive: " + size);
                }
                if (border.left() + border.right() > size.width() || border.top() + border.bottom() > size.height()) {
                    throw new IllegalArgumentException("Borders " + border + " do not fit in " + size);
                }
            }
        }

        /**
         * Creates a nine-slice whose border is in the sprite's pixels, with stretched edges and center.
         *
         * @param sprite the sprite
         * @param border the width of each fixed border, in the sprite's pixels
         * @param tint an ARGB color multiplied with the sprite; opaque white leaves it unchanged
         */
        public NineSlice(final SpriteId sprite, final Insets border, final int tint) {
            this(sprite, border, tint, null, false);
        }
    }
}
