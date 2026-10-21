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

import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.text.InlineElement;
import org.inspirenxe.strata.api.text.InlineSprite;
import org.inspirenxe.strata.api.text.InlineVisual;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.visual.Visual;
import org.inspirenxe.strata.core.text.InlineMetrics;
import org.inspirenxe.strata.spi.Canvas;

/**
 * The {@link Painter} that components draw with, passing each call to the engine's {@link Canvas}. The scoped clip
 * and translation calls pop what they pushed even if the drawing code throws, so the canvas stacks stay balanced.
 * Each host reuses one painter for every frame.
 *
 * <p>Sized nine-slices and tiled sprites go to the canvas as they are when the engine
 * {@linkplain org.inspirenxe.strata.spi.Capabilities#slicesSprites() slices sprites} itself. Otherwise the painter
 * draws them from pieces, each one {@link Canvas#spriteRegion} call for the part of the image it shows. A row or
 * column holds at most {@value #MAX_COPIES} copies; past that, the part is stretched instead.</p>
 */
public final class CanvasPainter implements Painter {
    /** The most copies of a tile in one row or column, so a tiny tile cannot flood the canvas with calls. */
    static final int MAX_COPIES = 1024;
    private static final int WHITE = 0xFFFFFFFF;

    private final InlineMetrics metrics;
    private Canvas canvas;
    private boolean slicesSprites;

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
        this.canvas = canvas;
        this.slicesSprites = slicesSprites;
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
        final float[] dx = {rect.x(), rect.x() + border.left() * horizontal,
            rect.right() - border.right() * horizontal, rect.right()};
        final float[] dy = {rect.y(), rect.y() + border.top() * vertical,
            rect.bottom() - border.bottom() * vertical, rect.bottom()};
        final float[] sx = {0f, border.left(), size.width() - border.right(), size.width()};
        final float[] sy = {0f, border.top(), size.height() - border.bottom(), size.height()};
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
        if (columns > MAX_COPIES || rows > MAX_COPIES) {
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
        final float stepX = columns > MAX_COPIES || !tileX ? dest.width() : sw;
        final float stepY = rows > MAX_COPIES || !tileY ? dest.height() : sh;
        columns = stepX == dest.width() ? 1 : columns;
        rows = stepY == dest.height() ? 1 : rows;
        final float scaleX = stepX / sw;
        final float scaleY = stepY / sh;
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                final float x = dest.x() + column * stepX;
                final float y = dest.y() + row * stepY;
                final Rect copy = Rect.ofEdges(x, y, Math.min(x + stepX, dest.right()), Math.min(y + stepY, dest.bottom()));
                // A cropped copy shows only the start of the region
                final float u0 = sx / size.width();
                final float v0 = sy / size.height();
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

    @Override
    public void text(final TextSpan span, final float x, final float y) {
        final InlineElement element = span.element();
        if (element == null) {
            this.canvas.text(span, x, y);
        } else if (element instanceof final InlineVisual visual) {
            final Size size = visual.visual().size();
            this.canvas.visual(visual.visual(), new Rect(x, y, size.width(), size.height()), WHITE);
        } else if (element instanceof final InlineSprite sprite) {
            if (!sprite.fitsLine()) {
                this.canvas.sprite(sprite.sprite(), new Rect(x, y, sprite.width(), sprite.height()), WHITE);
            } else if (this.metrics != null) {
                final float side = this.metrics.lineHeight(span.style());
                this.canvas.sprite(sprite.sprite(), new Rect(x, y, side, side), WHITE);
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
        this.canvas.pushScale(scale);
        try {
            draw.run();
        } finally {
            this.canvas.popTransform();
        }
    }

    @Override
    public void visual(final Visual visual, final Rect rect, final int tint) {
        this.canvas.visual(visual, rect, tint);
    }
}
