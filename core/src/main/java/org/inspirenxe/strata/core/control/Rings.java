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
package org.inspirenxe.strata.core.control;

import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.theme.Paint;

/**
 * Small shapes several controls draw: one-unit rings and the 7 by 4 arrows built from shrinking lines. Each call
 * allocates a few {@link Rect}s, one per line drawn.
 */
public final class Rings {
    /**
     * The long side of an arrow, in UI units.
     */
    public static final float ARROW_LONG = 7f;
    /**
     * The short side of an arrow, in UI units.
     */
    public static final float ARROW_SHORT = 4f;

    private Rings() {
    }

    /**
     * Paints a one-unit ring just inside a rectangle.
     *
     * @param painter what to draw with
     * @param paint the ring's paint
     * @param box the rectangle the ring lines
     */
    public static void paint(final Painter painter, final Paint paint, final Rect box) {
        paint.paint(painter, new Rect(box.x(), box.y(), box.width(), 1f));
        paint.paint(painter, new Rect(box.x(), box.bottom() - 1f, box.width(), 1f));
        paint.paint(painter, new Rect(box.x(), box.y() + 1f, 1f, box.height() - 2f));
        paint.paint(painter, new Rect(box.right() - 1f, box.y() + 1f, 1f, box.height() - 2f));
    }

    /**
     * Fills an arrow 7 wide and 4 tall pointing down, its top-left corner at a point.
     *
     * @param painter what to draw with
     * @param color the fill color, as ARGB
     * @param x the left edge
     * @param y the top edge
     */
    public static void down(final Painter painter, final int color, final float x, final float y) {
        for (int i = 0; i < 4; i++) {
            painter.fill(new Rect(x + i, y + i, ARROW_LONG - i * 2f, 1f), color);
        }
    }

    /**
     * Fills an arrow 7 wide and 4 tall pointing up, its top-left corner at a point.
     *
     * @param painter what to draw with
     * @param color the fill color, as ARGB
     * @param x the left edge
     * @param y the top edge
     */
    public static void up(final Painter painter, final int color, final float x, final float y) {
        for (int i = 0; i < 4; i++) {
            painter.fill(new Rect(x + i, y + 3f - i, ARROW_LONG - i * 2f, 1f), color);
        }
    }

    /**
     * Fills an arrow 4 wide and 7 tall pointing right, its top-left corner at a point.
     *
     * @param painter what to draw with
     * @param color the fill color, as ARGB
     * @param x the left edge
     * @param y the top edge
     */
    public static void right(final Painter painter, final int color, final float x, final float y) {
        for (int i = 0; i < 4; i++) {
            painter.fill(new Rect(x + i, y + i, 1f, ARROW_LONG - i * 2f), color);
        }
    }

    /**
     * Fills an arrow 4 wide and 7 tall pointing left, its top-left corner at a point.
     *
     * @param painter what to draw with
     * @param color the fill color, as ARGB
     * @param x the left edge
     * @param y the top edge
     */
    public static void left(final Painter painter, final int color, final float x, final float y) {
        for (int i = 0; i < 4; i++) {
            painter.fill(new Rect(x + 3f - i, y + i, 1f, ARROW_LONG - i * 2f), color);
        }
    }

    /**
     * Paints an arrow 4 wide and 7 tall pointing left with a paint, its top-left corner at a point.
     *
     * @param painter what to draw with
     * @param paint the arrow's paint
     * @param x the left edge
     * @param y the top edge
     */
    public static void left(final Painter painter, final Paint paint, final float x, final float y) {
        for (int i = 0; i < 4; i++) {
            paint.paint(painter, new Rect(x + 3f - i, y + i, 1f, ARROW_LONG - i * 2f));
        }
    }

    /**
     * Paints an arrow 4 wide and 7 tall pointing right with a paint, its top-left corner at a point.
     *
     * @param painter what to draw with
     * @param paint the arrow's paint
     * @param x the left edge
     * @param y the top edge
     */
    public static void right(final Painter painter, final Paint paint, final float x, final float y) {
        for (int i = 0; i < 4; i++) {
            paint.paint(painter, new Rect(x + i, y + i, 1f, ARROW_LONG - i * 2f));
        }
    }

    /**
     * Paints an arrow 7 wide and 4 tall pointing down with a paint, its top-left corner at a point.
     *
     * @param painter what to draw with
     * @param paint the arrow's paint
     * @param x the left edge
     * @param y the top edge
     */
    public static void down(final Painter painter, final Paint paint, final float x, final float y) {
        for (int i = 0; i < 4; i++) {
            paint.paint(painter, new Rect(x + i, y + i, ARROW_LONG - i * 2f, 1f));
        }
    }
}
