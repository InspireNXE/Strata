package org.inspirenxe.strata.api.theme;

import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geom.Insets;
import org.inspirenxe.strata.api.geom.Rect;

import java.util.Objects;

/**
 * How to fill a rectangle. Themes map style keys to paints; components draw them without knowing which kind they
 * hold.
 */
public sealed interface Paint {
    Paint NONE = new Solid(0x00000000);

    static Paint solid(final int argb) {
        return new Solid(argb);
    }

    static Paint gradient(final int topArgb, final int bottomArgb) {
        return new Gradient(topArgb, bottomArgb);
    }

    static Paint sprite(final SpriteId sprite) {
        return new Sprite(sprite, 0xFFFFFFFF);
    }

    static Paint nineSlice(final SpriteId sprite, final Insets border) {
        return new NineSlice(sprite, border, 0xFFFFFFFF);
    }

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

    record Solid(int argb) implements Paint {
    }

    record Gradient(int topArgb, int bottomArgb) implements Paint {
    }

    record Sprite(SpriteId sprite, int tint) implements Paint {
        public Sprite {
            Objects.requireNonNull(sprite, "sprite");
        }
    }

    record NineSlice(SpriteId sprite, Insets border, int tint) implements Paint {
        public NineSlice {
            Objects.requireNonNull(sprite, "sprite");
            Objects.requireNonNull(border, "border");
        }
    }
}
