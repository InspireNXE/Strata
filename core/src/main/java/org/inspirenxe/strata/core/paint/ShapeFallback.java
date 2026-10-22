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
package org.inspirenxe.strata.core.paint;

import org.inspirenxe.strata.api.extension.Gradient;
import org.inspirenxe.strata.api.extension.Shape;
import org.inspirenxe.strata.api.extension.Stroke;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.spi.Canvas;

import java.util.Arrays;
import java.util.List;

/**
 * Draws shapes on a canvas that draws only rectangles, for {@link CanvasPainter} when the engine does not
 * {@linkplain org.inspirenxe.strata.spi.Capabilities#drawsShapes() draw shapes}.
 *
 * <p>Every shape becomes polygons: curves and arcs are cut into straight pieces about two device pixels long, and a
 * stroke becomes one quadrilateral per segment, with a triangle on each side of every corner (so miter joins look
 * beveled) or a polygonal circle for round joins and caps. The polygons are scanned in rows one device pixel high,
 * sampled at each row's middle by the nonzero winding rule, and each run of covered pixels in a row is one
 * {@link Canvas#fill}. Rows whose runs match the row above merge into one taller fill, so a rectangle's straight
 * middle is a single call. Run edges snap to device pixels. A shape taller than {@value #MAX_ROWS} rows is scanned in
 * {@value #MAX_ROWS} taller rows instead. There is no anti-aliasing.</p>
 *
 * <p>A gradient at an angle is drawn in columns a device pixel wide, at most {@value #MAX_COLUMNS}, each a vertical
 * {@link Canvas#fillGradient(Rect, int, int)} cut where it crosses a stop; a gradient that runs straight up or down
 * is one column. A shadow is the rounded rectangle in the shadow's color inside up to {@value #MAX_SHADOW_LAYERS}
 * rings of falling alpha, about two device pixels wide each, that do not overlap, so each pixel blends once.</p>
 *
 * <p>The edge, polygon and run arrays grow as needed and are kept, so after the first frames a shape allocates
 * only the rectangle of each fill.</p>
 */
final class ShapeFallback {
    /** The most rows one shape is scanned in. */
    static final int MAX_ROWS = 2048;
    /** The most columns one gradient is drawn in. */
    static final int MAX_COLUMNS = 1024;
    /** The most rings of falling alpha one shadow is drawn in. */
    static final int MAX_SHADOW_LAYERS = 6;
    private static final double TURN = 2.0 * Math.PI;
    private static final int MAX_SEGMENTS = 256;

    private final Flattener flattener = new Flattener();
    private Canvas canvas;
    // One device pixel, in the painter's units
    private float step = 1f;

    // The edges of every polygon added since the last fill, as x0, y0, x1, y1 with y0 < y1, and their windings
    private float[] edges = new float[256];
    private byte[] windings = new byte[64];
    private int edgeCount;
    private float minY = Float.POSITIVE_INFINITY;
    private float maxY = Float.NEGATIVE_INFINITY;

    // The polygon being built, as x, y pairs
    private float[] polygon = new float[128];
    private int polygonCount;

    // The crossings of one row, sorted by x, with their windings
    private float[] crossings = new float[32];
    private byte[] crossingWindings = new byte[32];

    // The runs of the row being scanned, and the runs waiting to be filled, as start, end pairs
    private float[] runs = new float[32];
    private int runCount;
    private float[] pending = new float[32];
    private int pendingCount;

    // The points of one subpath of a stroked shape, as x, y pairs
    private float[] path = new float[128];
    private int pathCount;

    /**
     * Points the fallback at a canvas.
     *
     * @param canvas the canvas to fill on
     */
    void begin(final Canvas canvas) {
        this.canvas = canvas;
    }

    /**
     * Sets how many device pixels one unit is where the next shapes draw.
     *
     * @param pixelsPerUnit device pixels per unit, greater than 0
     */
    void pixelsPerUnit(final float pixelsPerUnit) {
        this.step = 1f / pixelsPerUnit;
    }

    // ------------------------------------------------------------------
    // Shapes
    // ------------------------------------------------------------------

    void line(final float x1, final float y1, final float x2, final float y2, final Stroke stroke, final int argb) {
        this.pathCount = 0;
        this.pathPoint(x1, y1);
        this.pathPoint(x2, y2);
        this.strokePath(this.path, 2, false, stroke);
        this.fillEdges(argb);
    }

    void polyline(final float[] points, final int count, final Stroke stroke, final int argb) {
        this.strokePath(points, count, false, stroke);
        this.fillEdges(argb);
    }

    /**
     * Fills a rounded rectangle whose radius is already limited to half its shorter side.
     */
    void fillRoundRect(final Rect rect, final float radius, final int argb) {
        if (!(radius > 0f)) {
            this.canvas.fill(rect, argb);
            return;
        }
        this.roundRect(rect.x(), rect.y(), rect.right(), rect.bottom(), radius);
        this.endPolygon(0);
        this.fillEdges(argb);
    }

    void strokeRect(final Rect rect, final float radius, final float width, final int argb) {
        if (width * 2f >= rect.width() || width * 2f >= rect.height()) {
            this.fillRoundRect(rect, radius, argb);
            return;
        }
        if (!(radius > 0f)) {
            // Four rectangles that do not overlap, so partial alpha blends once
            final Canvas canvas = this.canvas;
            canvas.fill(new Rect(rect.x(), rect.y(), rect.width(), width), argb);
            canvas.fill(new Rect(rect.x(), rect.bottom() - width, rect.width(), width), argb);
            canvas.fill(new Rect(rect.x(), rect.y() + width, width, rect.height() - 2f * width), argb);
            canvas.fill(new Rect(rect.right() - width, rect.y() + width, width, rect.height() - 2f * width), argb);
            return;
        }
        this.roundRect(rect.x(), rect.y(), rect.right(), rect.bottom(), radius);
        this.endPolygon(1);
        this.roundRect(rect.x() + width, rect.y() + width, rect.right() - width, rect.bottom() - width,
                Math.max(0f, radius - width));
        this.endPolygon(-1);
        this.fillEdges(argb);
    }

    void fillEllipse(final Rect oval, final int argb) {
        final float rx = oval.width() / 2f;
        final float ry = oval.height() / 2f;
        this.arcPoints(oval.x() + rx, oval.y() + ry, rx, ry, 0.0, TURN);
        this.endPolygon(0);
        this.fillEdges(argb);
    }

    void strokeEllipse(final Rect oval, final float width, final int argb) {
        final float rx = oval.width() / 2f;
        final float ry = oval.height() / 2f;
        if (width >= Math.min(rx, ry)) {
            this.fillEllipse(oval, argb);
            return;
        }
        final float cx = oval.x() + rx;
        final float cy = oval.y() + ry;
        this.arcPoints(cx, cy, rx, ry, 0.0, TURN);
        this.endPolygon(1);
        this.arcPoints(cx, cy, rx - width, ry - width, 0.0, TURN);
        this.endPolygon(-1);
        this.fillEdges(argb);
    }

    /**
     * Strokes an arc of less than a full turn, inside its oval.
     */
    void strokeArc(final Rect oval, final float startRadians, final float sweepRadians, final Stroke stroke,
                   final int argb) {
        final float rx = oval.width() / 2f;
        final float ry = oval.height() / 2f;
        final float cx = oval.x() + rx;
        final float cy = oval.y() + ry;
        final float width = Math.min(stroke.width(), Math.min(rx, ry));
        final float half = width / 2f;
        final float midX = rx - half;
        final float midY = ry - half;
        double start = startRadians;
        double sweep = sweepRadians;
        if (stroke.cap() == Stroke.Cap.SQUARE) {
            // Square caps reach half the width past each end, measured along the middle of the band
            final double extra = half / Math.max(1e-3, (midX + midY) / 2.0) * Math.signum(sweep);
            start -= extra;
            sweep = Math.max(-TURN, Math.min(TURN, sweep + 2.0 * extra));
        }
        this.arcPoints(cx, cy, rx, ry, start, sweep);
        this.arcPointsMore(cx, cy, rx - width, ry - width, start + sweep, -sweep);
        this.endPolygon(1);
        if (stroke.cap() == Stroke.Cap.ROUND) {
            this.circle(cx + midX * (float) Math.cos(start), cy + midY * (float) Math.sin(start), half);
            final double end = start + sweep;
            this.circle(cx + midX * (float) Math.cos(end), cy + midY * (float) Math.sin(end), half);
        }
        this.fillEdges(argb);
    }

    void fillShape(final Shape shape, final int argb) {
        this.flattener.walk(shape, true, null);
        this.fillEdges(argb);
    }

    void strokeShape(final Shape shape, final Stroke stroke, final int argb) {
        this.flattener.walk(shape, false, stroke);
        this.fillEdges(argb);
    }

    /**
     * Draws a soft shadow as the rectangle in the shadow's color inside rings of falling alpha that do not overlap,
     * each as wide as the blur divided by the number of rings. The radius is already limited.
     */
    void shadow(final Rect rect, final float radius, final float blur, final int argb) {
        final int alpha = argb >>> 24;
        final int rings = Math.max(1, Math.min(MAX_SHADOW_LAYERS, Math.round(blur / (2f * this.step))));
        for (int ring = 0; ring < rings; ring++) {
            // Ring 0 is the outermost and faintest; each ring has the alpha its middle would have on a straight fade
            final float outer = blur * (rings - ring) / rings;
            final float inner = blur * (rings - ring - 1) / rings;
            final int ringAlpha = Math.round(alpha * (ring + 0.5f) / rings);
            if (ringAlpha == 0) {
                continue;
            }
            this.roundRect(rect.x() - outer, rect.y() - outer, rect.right() + outer, rect.bottom() + outer,
                    radius + outer);
            this.endPolygon(1);
            this.roundRect(rect.x() - inner, rect.y() - inner, rect.right() + inner, rect.bottom() + inner,
                    radius + inner);
            this.endPolygon(-1);
            this.fillEdges(ringAlpha << 24 | (argb & 0xFFFFFF));
        }
        this.fillRoundRect(rect, radius, argb);
    }

    void fillGradient(final Rect rect, final Gradient gradient) {
        final float width = rect.width();
        final float height = rect.height();
        if (!(width > 0f) || !(height > 0f)) {
            return;
        }
        float dx = (float) Math.cos(gradient.angleRadians());
        float dy = (float) Math.sin(gradient.angleRadians());
        dx = Math.abs(dx) < 1e-6f ? 0f : dx;
        dy = Math.abs(dy) < 1e-6f ? 0f : dy;
        // The length of the gradient line across the rectangle, which offsets 0 to 1 span
        final float length = Math.abs(width * dx) + Math.abs(height * dy);
        final float cx = rect.x() + width / 2f;
        final float cy = rect.y() + height / 2f;
        final int columns = dx == 0f ? 1 : Math.max(1, Math.min(MAX_COLUMNS, (int) Math.ceil(width / this.step)));
        final float columnWidth = width / columns;
        final List<Gradient.Stop> stops = gradient.stops();
        for (int column = 0; column < columns; column++) {
            final float x0 = rect.x() + column * columnWidth;
            final float x1 = column == columns - 1 ? rect.right() : x0 + columnWidth;
            final float middle = 0.5f + ((x0 + x1) / 2f - cx) * dx / length;
            if (dy == 0f) {
                this.canvas.fill(Rect.ofEdges(x0, rect.y(), x1, rect.bottom()), gradient.colorAt(middle));
                continue;
            }
            final float top = middle + (rect.y() - cy) * dy / length;
            final float bottom = middle + (rect.bottom() - cy) * dy / length;
            float y = rect.y();
            float offset = top;
            // Cut the column where it crosses a stop, in the order the column meets them
            for (int i = 0; i < stops.size(); i++) {
                final float stop = stops.get(dy > 0f ? i : stops.size() - 1 - i).offset();
                if (dy > 0f ? stop > offset && stop < bottom : stop < offset && stop > bottom) {
                    final float cut = rect.y() + (stop - top) / (bottom - top) * height;
                    this.gradientPiece(gradient, x0, x1, y, cut, offset, stop);
                    y = cut;
                    offset = stop;
                }
            }
            this.gradientPiece(gradient, x0, x1, y, rect.bottom(), offset, bottom);
        }
    }

    private void gradientPiece(final Gradient gradient, final float x0, final float x1, final float y0, final float y1,
                               final float from, final float to) {
        if (y1 > y0) {
            this.canvas.fillGradient(Rect.ofEdges(x0, y0, x1, y1), gradient.colorAt(from), gradient.colorAt(to));
        }
    }

    // ------------------------------------------------------------------
    // Building polygons
    // ------------------------------------------------------------------

    /**
     * Starts a polygon with the outline of a rounded rectangle, clockwise on screen.
     */
    private void roundRect(final float left, final float top, final float right, final float bottom,
                           final float radius) {
        this.polygonCount = 0;
        final float r = Math.min(radius, Math.min(right - left, bottom - top) / 2f);
        if (!(r > 0f)) {
            this.point(left, top);
            this.point(right, top);
            this.point(right, bottom);
            this.point(left, bottom);
            return;
        }
        final double quarter = Math.PI / 2.0;
        this.arcPointsMore(left + r, top + r, r, r, Math.PI, quarter);
        this.arcPointsMore(right - r, top + r, r, r, -quarter, quarter);
        this.arcPointsMore(right - r, bottom - r, r, r, 0.0, quarter);
        this.arcPointsMore(left + r, bottom - r, r, r, quarter, quarter);
    }

    /**
     * Starts a polygon with points along an arc of an ellipse.
     */
    private void arcPoints(final float cx, final float cy, final float rx, final float ry, final double start,
                           final double sweep) {
        this.polygonCount = 0;
        this.arcPointsMore(cx, cy, rx, ry, start, sweep);
    }

    /**
     * Adds points along an arc of an ellipse to the polygon being built, both ends included.
     */
    private void arcPointsMore(final float cx, final float cy, final float rx, final float ry, final double start,
                               final double sweep) {
        final double length = Math.abs(sweep) * Math.max(rx, ry);
        // At least one piece every 45 degrees, and pieces about two device pixels long
        final int pieces = Math.max((int) Math.ceil(Math.abs(sweep) / (Math.PI / 4.0) - 1e-6),
                this.segments((float) length));
        for (int i = 0; i <= pieces; i++) {
            final double angle = start + sweep * i / pieces;
            this.point(cx + rx * (float) Math.cos(angle), cy + ry * (float) Math.sin(angle));
        }
    }

    private void circle(final float cx, final float cy, final float radius) {
        if (radius > 0f) {
            this.arcPoints(cx, cy, radius, radius, 0.0, TURN);
            this.endPolygon(1);
        }
    }

    /**
     * Returns how many straight pieces a curve of a length is cut into.
     */
    private int segments(final float length) {
        return Math.max(1, Math.min(MAX_SEGMENTS, (int) Math.ceil(length / (2f * this.step))));
    }

    private void point(final float x, final float y) {
        if (this.polygonCount * 2 + 2 > this.polygon.length) {
            this.polygon = Arrays.copyOf(this.polygon, this.polygon.length * 2);
        }
        this.polygon[this.polygonCount * 2] = x;
        this.polygon[this.polygonCount * 2 + 1] = y;
        this.polygonCount++;
    }

    /**
     * Closes the polygon being built and adds its edges.
     *
     * @param orientation 1 to turn the polygon clockwise on screen, -1 counterclockwise, 0 to keep it as built
     */
    private void endPolygon(final int orientation) {
        final int count = this.polygonCount;
        this.polygonCount = 0;
        if (count < 3) {
            return;
        }
        final float[] p = this.polygon;
        boolean reverse = false;
        if (orientation != 0) {
            double area = 0.0;
            for (int i = 0; i < count; i++) {
                final int j = (i + 1) % count;
                area += (double) p[i * 2] * p[j * 2 + 1] - (double) p[j * 2] * p[i * 2 + 1];
            }
            if (area == 0.0) {
                return;
            }
            // With y down, a positive area runs clockwise on screen
            reverse = area > 0.0 != orientation > 0;
        }
        for (int i = 0; i < count; i++) {
            final int j = (i + 1) % count;
            if (reverse) {
                this.edge(p[j * 2], p[j * 2 + 1], p[i * 2], p[i * 2 + 1]);
            } else {
                this.edge(p[i * 2], p[i * 2 + 1], p[j * 2], p[j * 2 + 1]);
            }
        }
    }

    private void edge(final float x0, final float y0, final float x1, final float y1) {
        if (y0 == y1 || !Float.isFinite(x0 + y0 + x1 + y1)) {
            return;
        }
        if (this.edgeCount == this.windings.length) {
            this.windings = Arrays.copyOf(this.windings, this.windings.length * 2);
            this.edges = Arrays.copyOf(this.edges, this.edges.length * 2);
        }
        final int at = this.edgeCount * 4;
        if (y0 < y1) {
            this.edges[at] = x0;
            this.edges[at + 1] = y0;
            this.edges[at + 2] = x1;
            this.edges[at + 3] = y1;
            this.windings[this.edgeCount] = 1;
        } else {
            this.edges[at] = x1;
            this.edges[at + 1] = y1;
            this.edges[at + 2] = x0;
            this.edges[at + 3] = y0;
            this.windings[this.edgeCount] = -1;
        }
        this.edgeCount++;
        this.minY = Math.min(this.minY, Math.min(y0, y1));
        this.maxY = Math.max(this.maxY, Math.max(y0, y1));
    }

    // ------------------------------------------------------------------
    // Strokes
    // ------------------------------------------------------------------

    private void pathPoint(final float x, final float y) {
        if (this.pathCount * 2 + 2 > this.path.length) {
            this.path = Arrays.copyOf(this.path, this.path.length * 2);
        }
        this.path[this.pathCount * 2] = x;
        this.path[this.pathCount * 2 + 1] = y;
        this.pathCount++;
    }

    /**
     * Adds the polygons of a stroke along a path: one quadrilateral per segment of nonzero length, the joins between
     * them and, on an open path, its caps. Every polygon turns the same way, so the nonzero rule fills their union
     * once.
     */
    private void strokePath(final float[] points, final int count, final boolean closed, final Stroke stroke) {
        final float half = stroke.width() / 2f;
        final int segments = closed ? count : count - 1;
        boolean any = false;
        float firstNx = 0f;
        float firstNy = 0f;
        float previousNx = 0f;
        float previousNy = 0f;
        float firstX = 0f;
        float firstY = 0f;
        float lastX = 0f;
        float lastY = 0f;
        for (int i = 0; i < segments; i++) {
            final int j = (i + 1) % count;
            final float ax = points[i * 2];
            final float ay = points[i * 2 + 1];
            final float bx = points[j * 2];
            final float by = points[j * 2 + 1];
            final float length = (float) Math.hypot(bx - ax, by - ay);
            if (!(length > 0f)) {
                continue;
            }
            // The normal, half the width long
            final float nx = -(by - ay) / length * half;
            final float ny = (bx - ax) / length * half;
            this.polygonCount = 0;
            this.point(ax + nx, ay + ny);
            this.point(bx + nx, by + ny);
            this.point(bx - nx, by - ny);
            this.point(ax - nx, ay - ny);
            this.endPolygon(1);
            if (any) {
                this.join(ax, ay, previousNx, previousNy, nx, ny, stroke.join(), half);
            } else {
                any = true;
                firstNx = nx;
                firstNy = ny;
                firstX = ax;
                firstY = ay;
            }
            previousNx = nx;
            previousNy = ny;
            lastX = bx;
            lastY = by;
        }
        if (!any) {
            return;
        }
        if (closed) {
            this.join(firstX, firstY, previousNx, previousNy, firstNx, firstNy, stroke.join(), half);
            return;
        }
        switch (stroke.cap()) {
            case ROUND -> {
                this.circle(firstX, firstY, half);
                this.circle(lastX, lastY, half);
            }
            // The normal turned a quarter gives the direction, half the width long
            case SQUARE -> {
                this.squareCap(firstX, firstY, firstNx, firstNy, -firstNy, firstNx);
                this.squareCap(lastX, lastY, previousNx, previousNy, previousNy, -previousNx);
            }
            case BUTT -> {
            }
        }
    }

    /**
     * Adds a square cap at an end point, reaching half the width outward.
     */
    private void squareCap(final float x, final float y, final float nx, final float ny, final float ox,
                           final float oy) {
        this.polygonCount = 0;
        this.point(x + nx, y + ny);
        this.point(x + nx + ox, y + ny + oy);
        this.point(x - nx + ox, y - ny + oy);
        this.point(x - nx, y - ny);
        this.endPolygon(1);
    }

    private void join(final float x, final float y, final float n1x, final float n1y, final float n2x,
                      final float n2y, final Stroke.Join join, final float half) {
        if (join == Stroke.Join.ROUND) {
            this.circle(x, y, half);
            return;
        }
        // A triangle on each side of the corner; the one on the inside lies within the segments and adds nothing
        for (int side = -1; side <= 1; side += 2) {
            this.polygonCount = 0;
            this.point(x, y);
            this.point(x + side * n1x, y + side * n1y);
            this.point(x + side * n2x, y + side * n2y);
            this.endPolygon(1);
        }
    }

    // ------------------------------------------------------------------
    // Scanning
    // ------------------------------------------------------------------

    /**
     * Fills the polygons added since the last fill and forgets them.
     */
    private void fillEdges(final int argb) {
        if (this.edgeCount == 0) {
            return;
        }
        try {
            final float s = this.step;
            final double first = Math.floor(this.minY / s);
            int rows = (int) Math.min(Integer.MAX_VALUE, Math.ceil(this.maxY / s - first));
            double origin = first * s;
            double rowHeight = s;
            if (rows > MAX_ROWS) {
                rows = MAX_ROWS;
                origin = this.minY;
                rowHeight = ((double) this.maxY - this.minY) / MAX_ROWS;
            }
            this.pendingCount = 0;
            float pendingTop = (float) origin;
            for (int row = 0; row < rows; row++) {
                final float top = (float) (origin + row * rowHeight);
                this.scanRow((float) (origin + (row + 0.5) * rowHeight));
                if (!this.sameRuns()) {
                    this.flush(pendingTop, top, argb);
                    final float[] swap = this.pending;
                    this.pending = this.runs;
                    this.runs = swap;
                    this.pendingCount = this.runCount;
                    pendingTop = top;
                }
            }
            this.flush(pendingTop, (float) (origin + rows * rowHeight), argb);
        } finally {
            this.edgeCount = 0;
            this.pendingCount = 0;
            this.minY = Float.POSITIVE_INFINITY;
            this.maxY = Float.NEGATIVE_INFINITY;
        }
    }

    /**
     * Works out the covered runs of the row whose middle is at {@code y}, their ends snapped to device pixels.
     */
    private void scanRow(final float y) {
        int count = 0;
        final float[] e = this.edges;
        for (int i = 0; i < this.edgeCount; i++) {
            final int at = i * 4;
            final float y0 = e[at + 1];
            final float y1 = e[at + 3];
            if (y < y0 || y >= y1) {
                continue;
            }
            final float x = e[at] + (y - y0) * (e[at + 2] - e[at]) / (y1 - y0);
            if (count == this.crossings.length) {
                this.crossings = Arrays.copyOf(this.crossings, count * 2);
                this.crossingWindings = Arrays.copyOf(this.crossingWindings, count * 2);
            }
            // Insertion keeps the crossings sorted; a row crosses few edges
            int slot = count;
            while (slot > 0 && this.crossings[slot - 1] > x) {
                this.crossings[slot] = this.crossings[slot - 1];
                this.crossingWindings[slot] = this.crossingWindings[slot - 1];
                slot--;
            }
            this.crossings[slot] = x;
            this.crossingWindings[slot] = this.windings[i];
            count++;
        }
        this.runCount = 0;
        int winding = 0;
        float start = 0f;
        for (int i = 0; i < count; i++) {
            final int before = winding;
            winding += this.crossingWindings[i];
            if (before == 0 && winding != 0) {
                start = this.crossings[i];
            } else if (before != 0 && winding == 0) {
                this.run(this.snap(start), this.snap(this.crossings[i]));
            }
        }
    }

    private float snap(final float x) {
        return (float) (Math.rint(x / this.step) * this.step);
    }

    private void run(final float start, final float end) {
        if (!(end > start)) {
            return;
        }
        final int count = this.runCount;
        if (count > 0 && start <= this.runs[count * 2 - 1]) {
            this.runs[count * 2 - 1] = Math.max(end, this.runs[count * 2 - 1]);
            return;
        }
        if (count * 2 + 2 > this.runs.length) {
            this.runs = Arrays.copyOf(this.runs, this.runs.length * 2);
        }
        this.runs[count * 2] = start;
        this.runs[count * 2 + 1] = end;
        this.runCount++;
    }

    private boolean sameRuns() {
        if (this.runCount != this.pendingCount) {
            return false;
        }
        for (int i = 0; i < this.runCount * 2; i++) {
            if (this.runs[i] != this.pending[i]) {
                return false;
            }
        }
        return true;
    }

    private void flush(final float top, final float bottom, final int argb) {
        if (!(bottom > top)) {
            return;
        }
        for (int i = 0; i < this.pendingCount; i++) {
            final float start = this.pending[i * 2];
            this.canvas.fill(new Rect(start, top, this.pending[i * 2 + 1] - start, bottom - top), argb);
        }
    }

    /**
     * Turns the segments of a shape into polygons: for a fill, each subpath is closed and added as it is; for a
     * stroke, each subpath's points are stroked. Curves are cut into pieces about two device pixels long.
     */
    private final class Flattener implements Shape.Sink {
        private boolean fill;
        private Stroke stroke;
        private float lastX;
        private float lastY;

        void walk(final Shape shape, final boolean fill, final Stroke stroke) {
            this.fill = fill;
            this.stroke = stroke;
            ShapeFallback.this.polygonCount = 0;
            ShapeFallback.this.pathCount = 0;
            try {
                shape.walk(this);
                this.finish(false);
            } finally {
                this.stroke = null;
            }
        }

        private void finish(final boolean closed) {
            final ShapeFallback fallback = ShapeFallback.this;
            if (this.fill) {
                fallback.endPolygon(0);
            } else if (fallback.pathCount >= 2) {
                final int count = fallback.pathCount;
                fallback.pathCount = 0;
                // A closed path that ends at its start has a closing segment of zero length, which is skipped
                fallback.strokePath(fallback.path, count, closed, this.stroke);
            }
            fallback.pathCount = 0;
        }

        private void add(final float x, final float y) {
            if (this.fill) {
                ShapeFallback.this.point(x, y);
            } else {
                ShapeFallback.this.pathPoint(x, y);
            }
            this.lastX = x;
            this.lastY = y;
        }

        @Override
        public void moveTo(final float x, final float y) {
            this.finish(false);
            this.add(x, y);
        }

        @Override
        public void lineTo(final float x, final float y) {
            this.add(x, y);
        }

        @Override
        public void quadTo(final float cx, final float cy, final float x, final float y) {
            final float x0 = this.lastX;
            final float y0 = this.lastY;
            final int pieces = ShapeFallback.this.segments(
                    (float) (Math.hypot(cx - x0, cy - y0) + Math.hypot(x - cx, y - cy)));
            for (int i = 1; i <= pieces; i++) {
                final float t = (float) i / pieces;
                final float u = 1f - t;
                this.add(u * u * x0 + 2f * u * t * cx + t * t * x, u * u * y0 + 2f * u * t * cy + t * t * y);
            }
        }

        @Override
        public void cubicTo(final float c1x, final float c1y, final float c2x, final float c2y, final float x,
                            final float y) {
            final float x0 = this.lastX;
            final float y0 = this.lastY;
            final int pieces = ShapeFallback.this.segments((float) (Math.hypot(c1x - x0, c1y - y0)
                    + Math.hypot(c2x - c1x, c2y - c1y) + Math.hypot(x - c2x, y - c2y)));
            for (int i = 1; i <= pieces; i++) {
                final float t = (float) i / pieces;
                final float u = 1f - t;
                final float a = u * u * u;
                final float b = 3f * u * u * t;
                final float c = 3f * u * t * t;
                final float d = t * t * t;
                this.add(a * x0 + b * c1x + c * c2x + d * x, a * y0 + b * c1y + c * c2y + d * y);
            }
        }

        @Override
        public void close() {
            this.finish(true);
        }
    }
}
