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
package org.inspirenxe.strata.core.control.collection;

import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.core.control.Rings;

/**
 * The rectangles of the rings and arrows the collection controls draw, the same ones {@link Rings} draws, made once
 * so a control can keep them between frames and paint without allocating.
 */
final class Shapes {
    private Shapes() {
    }

    /**
     * The four one-unit sides of a ring just inside a box, as {@link Rings#paint} draws it.
     */
    static Rect[] ring(final Rect box) {
        return new Rect[] {
                new Rect(box.x(), box.y(), box.width(), 1f),
                new Rect(box.x(), box.bottom() - 1f, box.width(), 1f),
                new Rect(box.x(), box.y() + 1f, 1f, box.height() - 2f),
                new Rect(box.right() - 1f, box.y() + 1f, 1f, box.height() - 2f)
        };
    }

    /**
     * The lines of an arrow 7 wide and 4 tall pointing down, its top-left corner at a point, as {@link Rings#down}
     * draws it.
     */
    static Rect[] down(final float x, final float y) {
        final Rect[] lines = new Rect[4];
        for (int i = 0; i < 4; i++) {
            lines[i] = new Rect(x + i, y + i, Rings.ARROW_LONG - i * 2f, 1f);
        }
        return lines;
    }

    /**
     * The lines of an arrow 7 wide and 4 tall pointing up, its top-left corner at a point, as {@link Rings#up} draws
     * it.
     */
    static Rect[] up(final float x, final float y) {
        final Rect[] lines = new Rect[4];
        for (int i = 0; i < 4; i++) {
            lines[i] = new Rect(x + i, y + 3f - i, Rings.ARROW_LONG - i * 2f, 1f);
        }
        return lines;
    }

    /**
     * The lines of an arrow 4 wide and 7 tall pointing right, its top-left corner at a point, as {@link Rings#right}
     * draws it.
     */
    static Rect[] right(final float x, final float y) {
        final Rect[] lines = new Rect[4];
        for (int i = 0; i < 4; i++) {
            lines[i] = new Rect(x + i, y + i, 1f, Rings.ARROW_LONG - i * 2f);
        }
        return lines;
    }

    /**
     * Paints each rectangle with a paint.
     */
    static void paint(final Painter painter, final Paint paint, final Rect[] rects) {
        for (final Rect rect : rects) {
            paint.paint(painter, rect);
        }
    }

    /**
     * Fills each rectangle with a color.
     */
    static void fill(final Painter painter, final int color, final Rect[] rects) {
        for (final Rect rect : rects) {
            painter.fill(rect, color);
        }
    }
}
