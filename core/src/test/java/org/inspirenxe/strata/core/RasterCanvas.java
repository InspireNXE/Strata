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
package org.inspirenxe.strata.core;

import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.extension.Gradient;
import org.inspirenxe.strata.api.extension.Shape;
import org.inspirenxe.strata.api.extension.Stroke;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.visual.PixelBuffer;
import org.inspirenxe.strata.api.visual.Visual;
import org.inspirenxe.strata.core.paint.CanvasPainter;
import org.inspirenxe.strata.spi.Canvas;
import org.inspirenxe.strata.spi.Capabilities;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * A canvas that draws into an array of ARGB pixels, so tests can read back what was drawn. A pixel is covered when its
 * middle is inside what is drawn; there is no anti-aliasing. Fills blend over what is there; every sprite and visual
 * is missing and draws magenta; text draws nothing.
 *
 * <p>It clips to rectangles and to rounded rectangles itself, and draws pixel buffers by nearest pixel. Every other
 * shape it draws through core's {@link CanvasPainter} as an engine without shapes would, so with
 * {@link #CAPABILITIES} it stands for an engine that draws shapes, and the shape contract checks exercise core's
 * fallback.</p>
 */
public final class RasterCanvas implements Canvas {
    /** What an engine with this canvas would report: it draws shapes. */
    public static final Capabilities CAPABILITIES = new Capabilities(false, false, false, true);
    /** The color drawn for missing sprites and unknown visuals. */
    public static final int MAGENTA = 0xFFFF00FF;
    private static final Capabilities FILLS_ONLY = new Capabilities(false, false, false, false);

    private final int width;
    private final int height;
    private final int[] argb;
    private final Deque<float[]> transforms = new ArrayDeque<>();
    private final Deque<float[]> clips = new ArrayDeque<>();
    private final CanvasPainter shapes = new CanvasPainter();
    private float tx;
    private float ty;
    private float scale = 1f;
    // The clip as device rectangles with radii: left, top, right, bottom, radius for each
    private float[] clip;
    private int smoothingDepth;
    private int fills;

    /**
     * Creates a transparent canvas.
     *
     * @param width the width in pixels
     * @param height the height in pixels
     */
    public RasterCanvas(final int width, final int height) {
        this.width = width;
        this.height = height;
        this.argb = new int[width * height];
        this.clip = new float[]{0f, 0f, width, height, 0f};
    }

    public int argb(final int x, final int y) {
        return this.argb[y * this.width + x];
    }

    /**
     * Returns how many pixels have any alpha.
     */
    public int covered() {
        int count = 0;
        for (final int pixel : this.argb) {
            if ((pixel >>> 24) != 0) {
                count++;
            }
        }
        return count;
    }

    /**
     * Returns how many fills were drawn.
     */
    public int fills() {
        return this.fills;
    }

    public boolean balanced() {
        return this.transforms.isEmpty() && this.clips.isEmpty() && this.smoothingDepth == 0;
    }

    private CanvasPainter shapes() {
        return this.shapes.begin(this, FILLS_ONLY, this.scale);
    }

    @Override
    public void fill(final Rect rect, final int argb) {
        this.fills++;
        if ((argb >>> 24) == 0) {
            return;
        }
        final float left = rect.x() * this.scale + this.tx;
        final float top = rect.y() * this.scale + this.ty;
        final float right = rect.right() * this.scale + this.tx;
        final float bottom = rect.bottom() * this.scale + this.ty;
        final int x0 = Math.max(0, (int) Math.floor(left - 0.5f));
        final int y0 = Math.max(0, (int) Math.floor(top - 0.5f));
        final int x1 = Math.min(this.width, (int) Math.ceil(right + 0.5f));
        final int y1 = Math.min(this.height, (int) Math.ceil(bottom + 0.5f));
        for (int y = y0; y < y1; y++) {
            final float cy = y + 0.5f;
            if (cy < top || cy >= bottom) {
                continue;
            }
            for (int x = x0; x < x1; x++) {
                final float cx = x + 0.5f;
                if (cx >= left && cx < right && this.inClip(cx, cy)) {
                    this.blend(x, y, argb);
                }
            }
        }
    }

    private boolean inClip(final float x, final float y) {
        final float[] c = this.clip;
        for (int i = 0; i < c.length; i += 5) {
            if (!RasterCanvas.inRound(x, y, c[i], c[i + 1], c[i + 2], c[i + 3], c[i + 4])) {
                return false;
            }
        }
        return true;
    }

    static boolean inRound(final float x, final float y, final float left, final float top, final float right,
                           final float bottom, final float radius) {
        if (x < left || x >= right || y < top || y >= bottom) {
            return false;
        }
        final float r = Math.min(radius, Math.min(right - left, bottom - top) / 2f);
        if (!(r > 0f)) {
            return true;
        }
        final float cx = Math.max(left + r, Math.min(x, right - r));
        final float cy = Math.max(top + r, Math.min(y, bottom - r));
        final float dx = x - cx;
        final float dy = y - cy;
        return dx * dx + dy * dy <= r * r;
    }

    private void blend(final int x, final int y, final int src) {
        final int index = y * this.width + x;
        final int dst = this.argb[index];
        final float sa = (src >>> 24) / 255f;
        final float da = (dst >>> 24) / 255f;
        final float oa = sa + da * (1f - sa);
        int result = Math.round(oa * 255f) << 24;
        for (int shift = 0; shift < 24; shift += 8) {
            final float sc = (src >>> shift) & 0xFF;
            final float dc = (dst >>> shift) & 0xFF;
            result |= Math.round((sc * sa + dc * da * (1f - sa)) / oa) << shift;
        }
        this.argb[index] = result;
    }

    @Override
    public void fillGradient(final Rect rect, final int topArgb, final int bottomArgb) {
        // One fill per device row, colored at the row's middle
        final int rows = Math.max(1, (int) Math.ceil(rect.height() * this.scale));
        final float rowHeight = rect.height() / rows;
        for (int row = 0; row < rows; row++) {
            final float t = (row + 0.5f) / rows;
            this.fill(new Rect(rect.x(), rect.y() + row * rowHeight, rect.width(), rowHeight),
                    Gradient.blend(topArgb, bottomArgb, t));
        }
    }

    @Override
    public void sprite(final SpriteId sprite, final Rect rect, final int tint) {
        this.fill(rect, MAGENTA);
    }

    @Override
    public void nineSlice(final SpriteId sprite, final Rect rect, final Insets border, final int tint) {
        this.fill(rect, MAGENTA);
    }

    @Override
    public void text(final TextSpan span, final float x, final float y) {
    }

    @Override
    public void pushClip(final Rect rect) {
        this.pushRoundClip(rect, 0f);
    }

    @Override
    public void pushRoundClip(final Rect rect, final float radius) {
        this.clips.push(this.clip);
        final float[] next = java.util.Arrays.copyOf(this.clip, this.clip.length + 5);
        final int at = this.clip.length;
        next[at] = rect.x() * this.scale + this.tx;
        next[at + 1] = rect.y() * this.scale + this.ty;
        next[at + 2] = rect.right() * this.scale + this.tx;
        next[at + 3] = rect.bottom() * this.scale + this.ty;
        next[at + 4] = Math.max(0f, radius) * this.scale;
        this.clip = next;
    }

    @Override
    public void popClip() {
        this.clip = this.clips.pop();
    }

    @Override
    public void pushTranslation(final float dx, final float dy) {
        this.transforms.push(new float[]{this.tx, this.ty, this.scale});
        this.tx += dx * this.scale;
        this.ty += dy * this.scale;
    }

    @Override
    public void pushScale(final float scale) {
        this.transforms.push(new float[]{this.tx, this.ty, this.scale});
        this.scale *= scale;
    }

    @Override
    public void popTransform() {
        final float[] previous = this.transforms.pop();
        this.tx = previous[0];
        this.ty = previous[1];
        this.scale = previous[2];
    }

    @Override
    public void visual(final Visual visual, final Rect rect, final int tint) {
        this.fill(rect, MAGENTA);
    }

    @Override
    public void nextLayer() {
    }

    @Override
    public void requestCursor(final CursorKind cursor) {
    }

    @Override
    public void pushSmoothing(final boolean smooth) {
        this.smoothingDepth++;
    }

    @Override
    public void popSmoothing() {
        if (--this.smoothingDepth < 0) {
            throw new IllegalStateException("popSmoothing without pushSmoothing");
        }
    }

    @Override
    public void pixels(final PixelBuffer buffer, final Rect rect, final int tint) {
        final float cellWidth = rect.width() / buffer.width();
        final float cellHeight = rect.height() / buffer.height();
        for (int y = 0; y < buffer.height(); y++) {
            for (int x = 0; x < buffer.width(); x++) {
                this.fill(new Rect(rect.x() + x * cellWidth, rect.y() + y * cellHeight, cellWidth, cellHeight),
                        RasterCanvas.multiply(buffer.get(x, y), tint));
            }
        }
    }

    private static int multiply(final int argb, final int tint) {
        int result = 0;
        for (int shift = 0; shift < 32; shift += 8) {
            result |= (((argb >>> shift) & 0xFF) * ((tint >>> shift) & 0xFF) / 255) << shift;
        }
        return result;
    }

    @Override
    public void fillGradient(final Rect rect, final Gradient gradient) {
        this.shapes().fillGradient(rect, gradient);
    }

    @Override
    public void line(final float x1, final float y1, final float x2, final float y2, final Stroke stroke,
                     final int argb) {
        this.shapes().line(x1, y1, x2, y2, stroke, argb);
    }

    @Override
    public void polyline(final float[] points, final int count, final Stroke stroke, final int argb) {
        this.shapes().polyline(points, count, stroke, argb);
    }

    @Override
    public void fillRoundRect(final Rect rect, final float radius, final int argb) {
        this.shapes().fillRoundRect(rect, radius, argb);
    }

    @Override
    public void strokeRect(final Rect rect, final float radius, final Stroke stroke, final int argb) {
        this.shapes().strokeRect(rect, radius, stroke, argb);
    }

    @Override
    public void fillEllipse(final Rect oval, final int argb) {
        this.shapes().fillEllipse(oval, argb);
    }

    @Override
    public void strokeEllipse(final Rect oval, final Stroke stroke, final int argb) {
        this.shapes().strokeEllipse(oval, stroke, argb);
    }

    @Override
    public void strokeArc(final Rect oval, final float startRadians, final float sweepRadians, final Stroke stroke,
                          final int argb) {
        this.shapes().strokeArc(oval, startRadians, sweepRadians, stroke, argb);
    }

    @Override
    public void fillShape(final Shape shape, final int argb) {
        this.shapes().fillShape(shape, argb);
    }

    @Override
    public void strokeShape(final Shape shape, final Stroke stroke, final int argb) {
        this.shapes().strokeShape(shape, stroke, argb);
    }

    @Override
    public void shadow(final Rect rect, final float radius, final float blur, final int argb) {
        this.shapes().shadow(rect, radius, blur, argb);
    }
}
