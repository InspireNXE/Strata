package org.inspirenxe.strata.api.theme;

import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;

import java.util.Objects;

/**
 * Solid lines along the inside of a rectangle's edges, each side with its own width. A side of width 0 draws
 * nothing, so a tab can have a border on every side but the bottom.
 *
 * @param argb the color of every side, ARGB
 * @param widths the width of each side, in UI units
 */
public record Border(int argb, Insets widths) {
    /**
     * No border. The default of every component.
     */
    public static final Border NONE = new Border(0x00000000, Insets.NONE);

    /**
     * Checks the widths.
     */
    public Border {
        Objects.requireNonNull(widths, "widths");
    }

    /**
     * Returns a border of the same width on every side.
     *
     * @param argb the color
     * @param width the width of every side
     * @return the border
     */
    public static Border of(final int argb, final float width) {
        return new Border(argb, Insets.of(width));
    }

    /**
     * Returns whether the border draws anything.
     *
     * @return false if the color is fully transparent or every side has width 0
     */
    public boolean isVisible() {
        return (this.argb >>> 24) != 0 && (this.widths.horizontal() > 0f || this.widths.vertical() > 0f);
    }

    /**
     * Draws the border inside a rectangle. The top and bottom sides span the whole width, and the left and right
     * sides fill the height between them, so corners are drawn once.
     *
     * @param painter what to draw with
     * @param rect the rectangle whose edges get the border
     */
    public void paint(final Painter painter, final Rect rect) {
        if (!this.isVisible()) {
            return;
        }
        final float top = Math.min(this.widths.top(), rect.height());
        final float bottom = Math.min(this.widths.bottom(), rect.height() - top);
        final float middle = rect.height() - top - bottom;
        if (top > 0f) {
            painter.fill(new Rect(rect.x(), rect.y(), rect.width(), top), this.argb);
        }
        if (bottom > 0f) {
            painter.fill(new Rect(rect.x(), rect.bottom() - bottom, rect.width(), bottom), this.argb);
        }
        if (middle > 0f) {
            final float left = Math.min(this.widths.left(), rect.width());
            final float right = Math.min(this.widths.right(), rect.width() - left);
            if (left > 0f) {
                painter.fill(new Rect(rect.x(), rect.y() + top, left, middle), this.argb);
            }
            if (right > 0f) {
                painter.fill(new Rect(rect.right() - right, rect.y() + top, right, middle), this.argb);
            }
        }
    }
}
