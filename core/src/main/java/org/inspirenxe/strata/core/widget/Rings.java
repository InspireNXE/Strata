package org.inspirenxe.strata.core.widget;

import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.theme.Paint;

/**
 * Small shapes several widgets draw: one-unit rings and the 7 by 4 arrows built from shrinking lines.
 */
final class Rings {
    /**
     * The long side of an arrow, in UI units.
     */
    static final float ARROW_LONG = 7f;
    /**
     * The short side of an arrow, in UI units.
     */
    static final float ARROW_SHORT = 4f;

    private Rings() {
    }

    /**
     * Paints a one-unit ring just inside a rectangle.
     */
    static void paint(final Painter painter, final Paint paint, final Rect box) {
        paint.paint(painter, new Rect(box.x(), box.y(), box.width(), 1f));
        paint.paint(painter, new Rect(box.x(), box.bottom() - 1f, box.width(), 1f));
        paint.paint(painter, new Rect(box.x(), box.y() + 1f, 1f, box.height() - 2f));
        paint.paint(painter, new Rect(box.right() - 1f, box.y() + 1f, 1f, box.height() - 2f));
    }

    /**
     * Fills an arrow 7 wide and 4 tall pointing down, its top-left corner at a point.
     */
    static void down(final Painter painter, final int color, final float x, final float y) {
        for (int i = 0; i < 4; i++) {
            painter.fill(new Rect(x + i, y + i, ARROW_LONG - i * 2f, 1f), color);
        }
    }

    /**
     * Fills an arrow 7 wide and 4 tall pointing up, its top-left corner at a point.
     */
    static void up(final Painter painter, final int color, final float x, final float y) {
        for (int i = 0; i < 4; i++) {
            painter.fill(new Rect(x + i, y + 3f - i, ARROW_LONG - i * 2f, 1f), color);
        }
    }

    /**
     * Fills an arrow 4 wide and 7 tall pointing right, its top-left corner at a point.
     */
    static void right(final Painter painter, final int color, final float x, final float y) {
        for (int i = 0; i < 4; i++) {
            painter.fill(new Rect(x + i, y + i, 1f, ARROW_LONG - i * 2f), color);
        }
    }

    /**
     * Paints an arrow 4 wide and 7 tall pointing right with a paint, its top-left corner at a point.
     */
    static void right(final Painter painter, final Paint paint, final float x, final float y) {
        for (int i = 0; i < 4; i++) {
            paint.paint(painter, new Rect(x + i, y + i, 1f, ARROW_LONG - i * 2f));
        }
    }

    /**
     * Paints an arrow 7 wide and 4 tall pointing down with a paint, its top-left corner at a point.
     */
    static void down(final Painter painter, final Paint paint, final float x, final float y) {
        for (int i = 0; i < 4; i++) {
            paint.paint(painter, new Rect(x + i, y + i, ARROW_LONG - i * 2f, 1f));
        }
    }
}
