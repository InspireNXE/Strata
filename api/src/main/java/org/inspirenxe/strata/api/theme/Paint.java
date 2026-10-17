package org.inspirenxe.strata.api.theme;

import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geom.Insets;
import org.inspirenxe.strata.api.geom.Rect;

import java.util.Objects;

/**
 * How to fill a rectangle: a solid color, a vertical gradient, a stretched sprite or a nine-slice sprite. Themes
 * map style keys to paints, and components draw them with {@link #paint} without caring which kind they hold.
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
     * Returns a solid color.
     *
     * @param argb the color
     * @return the paint
     */
    static Paint solid(final int argb) {
        return new Solid(argb);
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
     * Draws this paint over a rectangle.
     *
     * @param painter what to draw with
     * @param rect the area to cover
     */
    default void paint(final Painter painter, final Rect rect) {
        switch (this) {
            case Solid solid -> {
                if ((solid.argb >>> 24) != 0) {
                    painter.fill(rect, solid.argb);
                }
            }
            case Gradient gradient -> painter.fillGradient(rect, gradient.topArgb, gradient.bottomArgb);
            case Sprite sprite -> painter.sprite(sprite.sprite, rect, sprite.tint);
            case NineSlice slice -> painter.nineSlice(slice.sprite, rect, slice.border, slice.tint);
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
     * A sprite cut into a 3 by 3 grid. Corners keep their size, edges stretch along one axis, and the center
     * stretches both ways, so one small image can frame a rectangle of any size.
     *
     * <p>An engine that stores the slicing with the image may use that and ignore {@code border}; keep the two in
     * agreement so every engine draws the same.</p>
     *
     * @param sprite the sprite
     * @param border the width of each fixed border, in the sprite's pixels
     * @param tint an ARGB color multiplied with the sprite; opaque white leaves it unchanged
     */
    record NineSlice(SpriteId sprite, Insets border, int tint) implements Paint {
        /**
         * Checks the arguments.
         */
        public NineSlice {
            Objects.requireNonNull(sprite, "sprite");
            Objects.requireNonNull(border, "border");
        }
    }
}
