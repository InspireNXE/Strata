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
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.extension.Shape;
import org.inspirenxe.strata.api.extension.Stroke;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.text.InlineSprite;
import org.inspirenxe.strata.api.text.InlineVisual;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.visual.PixelBuffer;
import org.inspirenxe.strata.api.visual.Visual;
import org.inspirenxe.strata.core.text.InlineMetrics;
import org.inspirenxe.strata.spi.Canvas;
import org.inspirenxe.strata.spi.Capabilities;

/**
 * The {@link Painter} that components draw with, passing each call to the engine's {@link Canvas}. The scoped clip
 * and translation calls pop what they pushed even if the drawing code throws, so the canvas stacks stay balanced.
 * Each host reuses one painter for every frame.
 *
 * <p>Sized nine-slices and tiled sprites go to the canvas as they are when the engine
 * {@linkplain org.inspirenxe.strata.spi.Capabilities#slicesSprites() slices sprites} itself. Otherwise the painter
 * draws them from pieces, each one {@link Canvas#spriteRegion} call for the part of the image it shows. A row or
 * column holds at most {@value #MAX_COPIES} copies, and a tiled sprite or tiled part at most {@value #MAX_TILES} in
 * all; past either, the sprite or part is stretched instead.</p>
 *
 * <p>Drawing a sized nine-slice allocates only the rectangle of each of its nine parts, and drawing tiles only the
 * rectangle of each copy, since the canvas takes rectangles; the slice edges are kept in arrays the painter
 * reuses.</p>
 *
 * <p>Shapes go to the canvas when the engine {@linkplain Capabilities#drawsShapes() draws shapes}. Otherwise the
 * painter draws them from fills one device pixel high, gradients at an angle from columns of vertical gradients,
 * shadows from rings of falling alpha, and round clips as rectangular clips, and never calls the canvas's shape
 * methods; {@link #smoothed} then only runs its drawing. Either way the painter draws nothing for a color with alpha
 * 0, an empty rectangle, a stroke of width 0, an empty shape or fewer than two points, limits each radius to half the
 * shorter side of its rectangle (a negative radius counts as 0), fills a rounded rectangle of radius 0 with
 * {@link Canvas#fill}, draws an arc of a full turn or more as a whole ellipse, and a shadow without blur as a
 * rounded rectangle. {@link #pixels} always goes to the canvas.</p>
 */
public final class CanvasPainter implements Painter {
    /** The most copies of a tile in one row or column, so a tiny tile cannot flood the canvas with calls. */
    static final int MAX_COPIES = 1024;
    /** The most copies of a tile in all, so a tile that fits under {@link #MAX_COPIES} on each axis cannot either. */
    static final int MAX_TILES = 16384;
    private static final int WHITE = 0xFFFFFFFF;

    private final InlineMetrics metrics;
    private final ShapeFallback fallback = new ShapeFallback();
    // The edges of a nine-slice's parts, in the rectangle and in the image, reused by every nine-slice
    private final float[] destX = new float[4];
    private final float[] destY = new float[4];
    private final float[] sourceX = new float[4];
    private final float[] sourceY = new float[4];
    private Canvas canvas;
    private boolean slicesSprites;
    private boolean drawsShapes;
    private boolean shapesRightToLeft;
    // Device pixels per unit of the host, and the product of the scales in progress
    private float pixelsPerUnit = 1f;
    private float scale = 1f;

    /**
     * Creates a painter with no canvas and no text metrics. Call {@link #begin(Canvas)} before drawing. It draws
     * inline sprites that {@linkplain InlineSprite#fitsLine() fit the line} nowhere, since it cannot measure the line.
     */
    public CanvasPainter() {
        this.metrics = null;
    }

    /**
     * Creates a painter with no canvas that measures inline sprites that fit the line with the given metrics. Call
     * {@link #begin(Canvas)} before drawing.
     *
     * @param metrics the host's text metrics
     */
    public CanvasPainter(final TextMetrics metrics) {
        this.metrics = InlineMetrics.of(metrics);
    }

    /**
     * Points the painter at the canvas for this frame, for an engine that stores no slicing with its images.
     *
     * @param canvas the frame's canvas
     * @return this painter
     */
    public CanvasPainter begin(final Canvas canvas) {
        return this.begin(canvas, false);
    }

    /**
     * Points the painter at the canvas for this frame.
     *
     * @param canvas the frame's canvas
     * @param slicesSprites true when the engine applies slicing stored with its images, so sized nine-slices and
     *                      tiled sprites go to the canvas as they are
     * @return this painter
     */
    public CanvasPainter begin(final Canvas canvas, final boolean slicesSprites) {
        return this.begin(canvas, slicesSprites, false, 1f);
    }

    /**
     * Points the painter at the canvas for this frame, with what the engine does for itself and the host's density.
     *
     * @param canvas the frame's canvas
     * @param capabilities what the engine does for itself; the painter reads whether it slices sprites and draws
     *                     shapes
     * @param pixelsPerUnit device pixels per UI unit, greater than 0
     * @return this painter
     */
    public CanvasPainter begin(final Canvas canvas, final Capabilities capabilities, final float pixelsPerUnit) {
        this.begin(canvas, capabilities.slicesSprites(), capabilities.drawsShapes(), pixelsPerUnit);
        this.shapesRightToLeft = capabilities.shapesRightToLeft();
        return this;
    }

    private CanvasPainter begin(final Canvas canvas, final boolean slicesSprites, final boolean drawsShapes,
                                final float pixelsPerUnit) {
        this.canvas = canvas;
        this.slicesSprites = slicesSprites;
        this.drawsShapes = drawsShapes;
        this.shapesRightToLeft = false;
        this.pixelsPerUnit = pixelsPerUnit;
        this.scale = 1f;
        this.fallback.begin(canvas);
        this.fallback.pixelsPerUnit(pixelsPerUnit);
        return this;
    }

    /**
     * Returns the canvas set by the last {@link #begin(Canvas)}.
     *
     * @return the canvas, or {@code null} before the first frame
     */
    public Canvas canvas() {
        return this.canvas;
    }

    @Override
    public void fill(final Rect rect, final int argb) {
        this.canvas.fill(rect, argb);
    }

    @Override
    public void fillGradient(final Rect rect, final int topArgb, final int bottomArgb) {
        this.canvas.fillGradient(rect, topArgb, bottomArgb);
    }

    @Override
    public void fillGradient(final Rect rect, final Gradient gradient) {
        if (!CanvasPainter.isArea(rect)) {
            return;
        }
        if (this.drawsShapes) {
            this.canvas.fillGradient(rect, gradient);
        } else {
            this.fallback.fillGradient(rect, gradient);
        }
    }

    @Override
    public void line(final float x1, final float y1, final float x2, final float y2, final Stroke stroke,
                     final int argb) {
        if (!CanvasPainter.isVisible(argb) || !(stroke.width() > 0f)) {
            return;
        }
        if (this.drawsShapes) {
            this.canvas.line(x1, y1, x2, y2, stroke, argb);
        } else {
            this.fallback.line(x1, y1, x2, y2, stroke, argb);
        }
    }

    @Override
    public void polyline(final float[] points, final int count, final Stroke stroke, final int argb) {
        if (count < 0 || points.length < 2L * count) {
            throw new IllegalArgumentException("A polyline of " + count + " points needs " + 2L * count
                    + " coordinates, not " + points.length);
        }
        if (count < 2 || !CanvasPainter.isVisible(argb) || !(stroke.width() > 0f)) {
            return;
        }
        if (this.drawsShapes) {
            this.canvas.polyline(points, count, stroke, argb);
        } else {
            this.fallback.polyline(points, count, stroke, argb);
        }
    }

    @Override
    public void fillRoundRect(final Rect rect, final float radius, final int argb) {
        if (!CanvasPainter.isVisible(argb) || !CanvasPainter.isArea(rect)) {
            return;
        }
        final float r = CanvasPainter.radius(rect, radius);
        if (r == 0f) {
            this.canvas.fill(rect, argb);
        } else if (this.drawsShapes) {
            this.canvas.fillRoundRect(rect, r, argb);
        } else {
            this.fallback.fillRoundRect(rect, r, argb);
        }
    }

    @Override
    public void strokeRect(final Rect rect, final float radius, final Stroke stroke, final int argb) {
        if (!CanvasPainter.isVisible(argb) || !CanvasPainter.isArea(rect) || !(stroke.width() > 0f)) {
            return;
        }
        final float r = CanvasPainter.radius(rect, radius);
        if (this.drawsShapes) {
            this.canvas.strokeRect(rect, r, stroke, argb);
        } else {
            this.fallback.strokeRect(rect, r, stroke.width(), argb);
        }
    }

    @Override
    public void fillEllipse(final Rect oval, final int argb) {
        if (!CanvasPainter.isVisible(argb) || !CanvasPainter.isArea(oval)) {
            return;
        }
        if (this.drawsShapes) {
            this.canvas.fillEllipse(oval, argb);
        } else {
            this.fallback.fillEllipse(oval, argb);
        }
    }

    @Override
    public void strokeEllipse(final Rect oval, final Stroke stroke, final int argb) {
        if (!CanvasPainter.isVisible(argb) || !CanvasPainter.isArea(oval) || !(stroke.width() > 0f)) {
            return;
        }
        if (this.drawsShapes) {
            this.canvas.strokeEllipse(oval, stroke, argb);
        } else {
            this.fallback.strokeEllipse(oval, stroke.width(), argb);
        }
    }

    @Override
    public void strokeArc(final Rect oval, final float startRadians, final float sweepRadians, final Stroke stroke,
                          final int argb) {
        if (!(Math.abs(sweepRadians) > 0f) || !Float.isFinite(startRadians)) {
            return;
        }
        if (Math.abs(sweepRadians) >= 2.0 * Math.PI) {
            this.strokeEllipse(oval, stroke, argb);
            return;
        }
        if (!CanvasPainter.isVisible(argb) || !CanvasPainter.isArea(oval) || !(stroke.width() > 0f)) {
            return;
        }
        if (this.drawsShapes) {
            this.canvas.strokeArc(oval, startRadians, sweepRadians, stroke, argb);
        } else {
            this.fallback.strokeArc(oval, startRadians, sweepRadians, stroke, argb);
        }
    }

    @Override
    public void fillShape(final Shape shape, final int argb) {
        if (!CanvasPainter.isVisible(argb) || shape.isEmpty()) {
            return;
        }
        if (this.drawsShapes) {
            this.canvas.fillShape(shape, argb);
        } else {
            this.fallback.fillShape(shape, argb);
        }
    }

    @Override
    public void strokeShape(final Shape shape, final Stroke stroke, final int argb) {
        if (!CanvasPainter.isVisible(argb) || shape.isEmpty() || !(stroke.width() > 0f)) {
            return;
        }
        if (this.drawsShapes) {
            this.canvas.strokeShape(shape, stroke, argb);
        } else {
            this.fallback.strokeShape(shape, stroke, argb);
        }
    }

    @Override
    public void shadow(final Rect rect, final float radius, final float blur, final int argb) {
        if (!(blur > 0f) || Float.isInfinite(blur)) {
            this.fillRoundRect(rect, radius, argb);
            return;
        }
        if (!CanvasPainter.isVisible(argb) || !CanvasPainter.isArea(rect)) {
            return;
        }
        final float r = CanvasPainter.radius(rect, radius);
        if (this.drawsShapes) {
            this.canvas.shadow(rect, r, blur, argb);
        } else {
            this.fallback.shadow(rect, r, blur, argb);
        }
    }

    @Override
    public void smoothed(final boolean smooth, final Runnable draw) {
        if (!this.drawsShapes) {
            draw.run();
            return;
        }
        this.canvas.pushSmoothing(smooth);
        try {
            draw.run();
        } finally {
            this.canvas.popSmoothing();
        }
    }

    @Override
    public void clippedRound(final Rect rect, final float radius, final Runnable draw) {
        final float r = CanvasPainter.radius(rect, radius);
        if (this.drawsShapes && r > 0f) {
            this.canvas.pushRoundClip(rect, r);
        } else {
            this.canvas.pushClip(rect);
        }
        try {
            draw.run();
        } finally {
            this.canvas.popClip();
        }
    }

    @Override
    public void pixels(final PixelBuffer buffer, final Rect rect, final int tint) {
        this.canvas.pixels(buffer, rect, tint);
    }

    /**
     * Returns the density given to {@link #begin(Canvas, Capabilities, float)}, 1 after the other forms of
     * {@code begin}, times every scale in progress.
     */
    @Override
    public float pixelsPerUnit() {
        return this.pixelsPerUnit * this.scale;
    }

    private static boolean isVisible(final int argb) {
        return (argb >>> 24) != 0;
    }

    private static boolean isArea(final Rect rect) {
        return rect.width() > 0f && rect.height() > 0f;
    }

    /**
     * Limits a radius to half the shorter side of a rectangle, counting a negative or undefined one as 0.
     */
    private static float radius(final Rect rect, final float radius) {
        return radius > 0f ? Math.min(radius, Math.min(rect.width(), rect.height()) / 2f) : 0f;
    }

    @Override
    public void sprite(final SpriteId sprite, final Rect rect, final int tint) {
        this.canvas.sprite(sprite, rect, tint);
    }

    @Override
    public void nineSlice(final SpriteId sprite, final Rect rect, final Insets border, final int tint) {
        this.canvas.nineSlice(sprite, rect, border, tint);
    }

    @Override
    public void nineSlice(final SpriteId sprite, final Rect rect, final Size size, final Insets border,
                          final boolean tiled, final int tint) {
        CanvasPainter.checkSize(size);
        if (border.left() + border.right() > size.width() || border.top() + border.bottom() > size.height()) {
            throw new IllegalArgumentException("Borders " + border + " do not fit in " + size);
        }
        if (this.slicesSprites) {
            this.canvas.nineSlice(sprite, rect, border, tint);
            return;
        }
        if (!(rect.width() > 0f) || !(rect.height() > 0f)) {
            return;
        }
        // Borders wider than the rectangle shrink in proportion, as the canvas does for its own nine-slices
        final float horizontal = border.left() + border.right() > rect.width()
                ? rect.width() / (border.left() + border.right()) : 1f;
        final float vertical = border.top() + border.bottom() > rect.height()
                ? rect.height() / (border.top() + border.bottom()) : 1f;
        final float[] dx = this.destX;
        dx[0] = rect.x();
        dx[1] = rect.x() + border.left() * horizontal;
        dx[2] = rect.right() - border.right() * horizontal;
        dx[3] = rect.right();
        final float[] dy = this.destY;
        dy[0] = rect.y();
        dy[1] = rect.y() + border.top() * vertical;
        dy[2] = rect.bottom() - border.bottom() * vertical;
        dy[3] = rect.bottom();
        final float[] sx = this.sourceX;
        sx[0] = 0f;
        sx[1] = border.left();
        sx[2] = size.width() - border.right();
        sx[3] = size.width();
        final float[] sy = this.sourceY;
        sy[0] = 0f;
        sy[1] = border.top();
        sy[2] = size.height() - border.bottom();
        sy[3] = size.height();
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                this.part(sprite, size, tint,
                        sx[column], sy[row], sx[column + 1] - sx[column], sy[row + 1] - sy[row],
                        Rect.ofEdges(dx[column], dy[row], dx[column + 1], dy[row + 1]),
                        tiled && column == 1, tiled && row == 1);
            }
        }
    }

    @Override
    public void tiled(final SpriteId sprite, final Rect rect, final Size size, final int tint) {
        CanvasPainter.checkSize(size);
        if (this.slicesSprites) {
            this.canvas.sprite(sprite, rect, tint);
            return;
        }
        if (!(rect.width() > 0f) || !(rect.height() > 0f)) {
            return;
        }
        final int columns = CanvasPainter.copies(rect.width(), size.width());
        final int rows = CanvasPainter.copies(rect.height(), size.height());
        if (columns > MAX_COPIES || rows > MAX_COPIES || (long) columns * rows > MAX_TILES) {
            this.canvas.sprite(sprite, rect, tint);
            return;
        }
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                final float x = rect.x() + column * size.width();
                final float y = rect.y() + row * size.height();
                // The last row and column show only as much of the image as fits
                final float width = Math.min(size.width(), rect.right() - x);
                final float height = Math.min(size.height(), rect.bottom() - y);
                this.canvas.spriteRegion(sprite, new Rect(x, y, width, height), 0f, 0f, width / size.width(),
                        height / size.height(), tint);
            }
        }
    }

    /**
     * Draws one part of a nine-slice: the image region at {@code sx, sy} of {@code sw} by {@code sh} units of
     * {@code size}, over {@code dest}, stretched along an axis or repeated at its own length.
     */
    private void part(final SpriteId sprite, final Size size, final int tint, final float sx, final float sy,
                      final float sw, final float sh, final Rect dest, final boolean tileX, final boolean tileY) {
        if (!(sw > 0f) || !(sh > 0f) || !(dest.width() > 0f) || !(dest.height() > 0f)) {
            return;
        }
        int columns = tileX ? CanvasPainter.copies(dest.width(), sw) : 1;
        int rows = tileY ? CanvasPainter.copies(dest.height(), sh) : 1;
        // Too many copies in all stretch the part both ways
        final boolean tooMany = (long) columns * rows > MAX_TILES;
        final float stepX = columns > MAX_COPIES || tooMany || !tileX ? dest.width() : sw;
        final float stepY = rows > MAX_COPIES || tooMany || !tileY ? dest.height() : sh;
        columns = stepX == dest.width() ? 1 : columns;
        rows = stepY == dest.height() ? 1 : rows;
        final float scaleX = stepX / sw;
        final float scaleY = stepY / sh;
        final float u0 = sx / size.width();
        final float v0 = sy / size.height();
        if (columns == 1 && rows == 1) {
            // One copy covers the whole part, so the part's own rectangle will do
            this.canvas.spriteRegion(sprite, dest, u0, v0, (sx + sw) / size.width(), (sy + sh) / size.height(), tint);
            return;
        }
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                final float x = dest.x() + column * stepX;
                final float y = dest.y() + row * stepY;
                final Rect copy = Rect.ofEdges(x, y, Math.min(x + stepX, dest.right()), Math.min(y + stepY, dest.bottom()));
                // A cropped copy shows only the start of the region
                final float u1 = (sx + copy.width() / scaleX) / size.width();
                final float v1 = (sy + copy.height() / scaleY) / size.height();
                this.canvas.spriteRegion(sprite, copy, u0, v0, u1, v1, tint);
            }
        }
    }

    /**
     * Returns how many copies of a length cover a span, the last one cropped.
     */
    private static int copies(final float span, final float length) {
        final double count = Math.ceil(span / length - 1e-4);
        return count > Integer.MAX_VALUE ? Integer.MAX_VALUE : Math.max(1, (int) count);
    }

    private static void checkSize(final Size size) {
        if (!(size.width() > 0f) || !(size.height() > 0f)) {
            throw new IllegalArgumentException("Sprite size must be positive: " + size);
        }
    }

    /**
     * Draws a run the bidirectional algorithm put right to left: in logical order with its direction to an engine
     * that {@linkplain Capabilities#shapesRightToLeft() shapes right-to-left text}, or else the reversed span as plain
     * text.
     *
     * @param logical the run in logical order
     * @param reversed the run with its characters reversed and its brackets mirrored
     * @param x the run's left edge
     * @param y the top of its line box
     */
    public void rightToLeftText(final TextSpan logical, final TextSpan reversed, final float x, final float y) {
        if (this.shapesRightToLeft) {
            this.canvas.text(logical, x, y, true);
        } else {
            this.canvas.text(reversed, x, y);
        }
    }

    @Override
    public void text(final TextSpan span, final float x, final float y) {
        switch (span.element()) {
            case null -> this.canvas.text(span, x, y);
            case final InlineVisual visual -> {
                final Size size = visual.visual().size();
                this.canvas.visual(visual.visual(), new Rect(x, y, size.width(), size.height()), WHITE);
            }
            case final InlineSprite sprite -> {
                if (!sprite.fitsLine()) {
                    this.canvas.sprite(sprite.sprite(), new Rect(x, y, sprite.width(), sprite.height()), WHITE);
                } else if (this.metrics != null) {
                    final float side = this.metrics.lineHeight(span.style());
                    this.canvas.sprite(sprite.sprite(), new Rect(x, y, side, side), WHITE);
                }
            }
        }
    }

    @Override
    public void clipped(final Rect rect, final Runnable draw) {
        this.canvas.pushClip(rect);
        try {
            draw.run();
        } finally {
            this.canvas.popClip();
        }
    }

    @Override
    public void translated(final float dx, final float dy, final Runnable draw) {
        this.canvas.pushTranslation(dx, dy);
        try {
            draw.run();
        } finally {
            this.canvas.popTransform();
        }
    }

    @Override
    public void scaled(final float scale, final Runnable draw) {
        if (scale == 1f) {
            draw.run();
            return;
        }
        final float previous = this.scale;
        this.canvas.pushScale(scale);
        this.scale = previous * scale;
        this.fallback.pixelsPerUnit(this.pixelsPerUnit * this.scale);
        try {
            draw.run();
        } finally {
            this.scale = previous;
            this.fallback.pixelsPerUnit(this.pixelsPerUnit * previous);
            this.canvas.popTransform();
        }
    }

    @Override
    public void visual(final Visual visual, final Rect rect, final int tint) {
        this.canvas.visual(visual, rect, tint);
    }
}
