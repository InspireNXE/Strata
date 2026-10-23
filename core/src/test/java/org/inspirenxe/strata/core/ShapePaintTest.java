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
import org.inspirenxe.strata.api.extension.Painter;
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lines, outlines, shapes, gradients at an angle, shadows, round clips and pixel buffers, drawn through core's
 * painter on an engine that draws shapes and, with core's fallback, on one that draws only rectangles.
 */
class ShapePaintTest {
    private static final Capabilities FILLS_ONLY = new Capabilities(false, false);
    private static final Capabilities SHAPES = new Capabilities(false, false, false, true);
    private static final int RED = 0xFFFF0000;
    private static final int BLUE = 0xFF0000FF;
    private static final Shape TRIANGLE = Shape.builder().moveTo(5, 5).lineTo(25, 5).lineTo(15, 25).close().build();

    private static Painter onFills(final Canvas canvas, final float pixelsPerUnit) {
        return new CanvasPainter().begin(canvas, FILLS_ONLY, pixelsPerUnit);
    }

    /**
     * Draws every kind of shape once.
     */
    private static void drawEverything(final Painter painter) {
        final Stroke round = new Stroke(3f, Stroke.Cap.ROUND, Stroke.Join.ROUND);
        final Stroke square = new Stroke(3f, Stroke.Cap.SQUARE, Stroke.Join.BEVEL);
        painter.line(1, 1, 30, 20, round, RED);
        painter.polyline(new float[]{1, 1, 20, 5, 10, 25}, 3, square, RED);
        painter.fillRoundRect(new Rect(1, 1, 30, 20), 6f, RED);
        painter.strokeRect(new Rect(1, 1, 30, 20), 6f, Stroke.of(2f), RED);
        painter.strokeRect(new Rect(1, 1, 30, 20), 0f, Stroke.of(2f), RED);
        painter.fillEllipse(new Rect(1, 1, 30, 20), RED);
        painter.strokeEllipse(new Rect(1, 1, 30, 20), Stroke.of(2f), RED);
        painter.strokeArc(new Rect(1, 1, 30, 20), 0.5f, 2f, round, RED);
        painter.strokeArc(new Rect(1, 1, 30, 20), 0.5f, -2f, square, RED);
        painter.fillShape(TRIANGLE, RED);
        painter.strokeShape(TRIANGLE, round, RED);
        painter.fillGradient(new Rect(1, 1, 30, 20), Gradient.of(0.6f, RED, BLUE));
        painter.shadow(new Rect(5, 5, 20, 10), 3f, 6f, 0x80000000);
        painter.smoothed(false, () -> painter.fillEllipse(new Rect(1, 1, 10, 10), RED));
        painter.clippedRound(new Rect(1, 1, 20, 20), 5f, () -> painter.fill(new Rect(0, 0, 30, 30), RED));
    }

    @Nested
    class WithoutShapes {
        @Test
        void noShapeCallReachesTheCanvas() {
            // NullCanvas keeps the defaults, which throw
            final NullCanvas canvas = new NullCanvas();
            assertDoesNotThrow(() -> ShapePaintTest.drawEverything(ShapePaintTest.onFills(canvas, 1f)));
            assertFalse(canvas.fills().isEmpty());
            assertEquals(0, canvas.clipDepth());
        }

        @Test
        void aRoundRectangleOfRadiusZeroIsOneFill() {
            final NullCanvas canvas = new NullCanvas();
            ShapePaintTest.onFills(canvas, 1f).fillRoundRect(new Rect(2, 3, 10, 10), -4f, RED);
            assertEquals(List.of(new Rect(2, 3, 10, 10)), canvas.fills());
        }

        @Test
        void aSquareOutlineIsFourFillsThatDoNotOverlap() {
            final NullCanvas canvas = new NullCanvas();
            ShapePaintTest.onFills(canvas, 1f).strokeRect(new Rect(0, 0, 20, 20), 0f, Stroke.of(2f), RED);
            assertEquals(4, canvas.fills().size());
            float area = 0f;
            for (final Rect fill : canvas.fills()) {
                area += fill.width() * fill.height();
            }
            assertEquals(20f * 20f - 16f * 16f, area, 0.001f);
        }

        @Test
        void rowsWithTheSameRunsMergeIntoOneFill() {
            final NullCanvas canvas = new NullCanvas();
            ShapePaintTest.onFills(canvas, 1f).fillRoundRect(new Rect(0, 0, 100, 40), 4f, RED);
            // Four rows for each rounded edge, and one fill for the straight middle
            assertTrue(canvas.fills().size() <= 9, "fills: " + canvas.fills().size());
            boolean tall = false;
            for (final Rect fill : canvas.fills()) {
                tall |= fill.height() >= 32f && fill.width() == 100f;
            }
            assertTrue(tall, "the middle is one fill: " + canvas.fills());
        }

        @Test
        void anEllipseCoversAboutItsAreaAndStaysInItsOval() {
            final RasterCanvas canvas = new RasterCanvas(100, 100);
            ShapePaintTest.onFills(canvas, 1f).fillEllipse(new Rect(10, 10, 80, 80), RED);
            final double area = Math.PI * 40 * 40;
            assertEquals(area, canvas.covered(), area * 0.02);
            for (int i = 0; i < 100; i++) {
                assertEquals(0, canvas.argb(i, 9) >>> 24, "above the oval");
                assertEquals(0, canvas.argb(9, i) >>> 24, "left of the oval");
                assertEquals(0, canvas.argb(i, 90) >>> 24, "below the oval");
            }
        }

        @Test
        void aDenserDisplayCutsFinerRows() {
            final NullCanvas coarse = new NullCanvas();
            ShapePaintTest.onFills(coarse, 1f).fillEllipse(new Rect(0, 0, 10, 10), RED);
            final NullCanvas fine = new NullCanvas();
            ShapePaintTest.onFills(fine, 2f).fillEllipse(new Rect(0, 0, 10, 10), RED);
            assertEquals(1f, ShapePaintTest.thinnest(coarse.fills()));
            assertEquals(0.5f, ShapePaintTest.thinnest(fine.fills()));
            assertTrue(fine.fills().size() > coarse.fills().size());
        }

        @Test
        void aScaleCutsFinerRowsUntilItEnds() {
            final NullCanvas canvas = new NullCanvas();
            final Painter painter = ShapePaintTest.onFills(canvas, 1f);
            painter.scaled(4f, () -> painter.fillEllipse(new Rect(0, 0, 10, 10), RED));
            assertEquals(0.25f, ShapePaintTest.thinnest(canvas.fills()));
            canvas.fills().clear();
            painter.fillEllipse(new Rect(0, 0, 10, 10), RED);
            assertEquals(1f, ShapePaintTest.thinnest(canvas.fills()));
        }

        @Test
        void aVeryTallShapeIsScannedInAtMost2048Rows() {
            final NullCanvas canvas = new NullCanvas();
            final Shape tall = Shape.builder().moveTo(0, 0).lineTo(10, 1_000_000).lineTo(-10, 1_000_000).close()
                    .build();
            ShapePaintTest.onFills(canvas, 1f).fillShape(tall, RED);
            assertTrue(canvas.fills().size() <= 2048, "fills: " + canvas.fills().size());
            assertFalse(canvas.fills().isEmpty());
        }

        @Test
        void aThinDiagonalLineHasNoGaps() {
            final RasterCanvas canvas = new RasterCanvas(60, 60);
            ShapePaintTest.onFills(canvas, 1f).line(5, 5, 55, 55, Stroke.of(1f), RED);
            for (int y = 6; y < 54; y++) {
                boolean any = false;
                for (int x = 0; x < 60; x++) {
                    any |= (canvas.argb(x, y) >>> 24) != 0;
                }
                assertTrue(any, "row " + y + " is empty");
            }
        }

        @Test
        void capsReachPastTheEndsAsTheirKindSays() {
            final RasterCanvas butt = new RasterCanvas(30, 20);
            ShapePaintTest.onFills(butt, 1f).line(10, 10, 20, 10, Stroke.of(4f), RED);
            final RasterCanvas round = new RasterCanvas(30, 20);
            ShapePaintTest.onFills(round, 1f).line(10, 10, 20, 10, new Stroke(4f, Stroke.Cap.ROUND,
                    Stroke.Join.MITER), RED);
            final RasterCanvas square = new RasterCanvas(30, 20);
            ShapePaintTest.onFills(square, 1f).line(10, 10, 20, 10, new Stroke(4f, Stroke.Cap.SQUARE,
                    Stroke.Join.MITER), RED);
            assertEquals(0, butt.argb(8, 10) >>> 24, "a butt cap stops at the end");
            assertEquals(RED, round.argb(8, 10), "a round cap reaches past it");
            assertEquals(0, round.argb(8, 8) >>> 24, "but only as a half circle");
            assertEquals(RED, square.argb(8, 10), "a square cap reaches past it");
            assertEquals(RED, square.argb(8, 8), "as a square");
        }

        @Test
        void partialAlphaBlendsOnceWhereStrokeSegmentsMeet() {
            final RasterCanvas canvas = new RasterCanvas(40, 40);
            ShapePaintTest.onFills(canvas, 1f).polyline(new float[]{5, 20, 20, 20, 20, 35}, 3,
                    new Stroke(4f, Stroke.Cap.BUTT, Stroke.Join.ROUND), 0x80FF0000);
            final int corner = canvas.argb(20, 20) >>> 24;
            final int segment = canvas.argb(10, 20) >>> 24;
            assertEquals(segment, corner, "the corner is as strong as a segment");
        }

        @Test
        void aGradientAtAnAngleIsColumnsOfVerticalGradients() {
            final Recording canvas = new Recording();
            ShapePaintTest.onFills(canvas, 1f).fillGradient(new Rect(0, 0, 10, 10),
                    Gradient.of((float) (Math.PI / 4), RED, BLUE));
            assertEquals(10, canvas.calls.size());
            assertTrue(canvas.calls.stream().allMatch(call -> call.startsWith("fillGradient")), canvas.calls.toString());
        }

        @Test
        void aVerticalGradientIsOneColumnCutAtItsStops() {
            final Recording canvas = new Recording();
            ShapePaintTest.onFills(canvas, 1f).fillGradient(new Rect(0, 0, 10, 40), new Gradient((float) (Math.PI / 2),
                    List.of(new Gradient.Stop(0f, RED), new Gradient.Stop(0.5f, 0xFF00FF00), new Gradient.Stop(1f,
                            BLUE))));
            assertEquals(List.of(
                    "fillGradient " + new Rect(0, 0, 10, 20) + " ffff0000 ff00ff00",
                    "fillGradient " + new Rect(0, 20, 10, 20) + " ff00ff00 ff0000ff"), canvas.calls);
        }

        @Test
        void aGradientRunningUpIsCutInTheOrderItMeetsItsStops() {
            final Recording canvas = new Recording();
            ShapePaintTest.onFills(canvas, 1f).fillGradient(new Rect(0, 0, 10, 40), new Gradient((float) (-Math.PI / 2),
                    List.of(new Gradient.Stop(0f, RED), new Gradient.Stop(0.5f, 0xFF00FF00), new Gradient.Stop(1f,
                            BLUE))));
            assertEquals(List.of(
                    "fillGradient " + new Rect(0, 0, 10, 20) + " ff0000ff ff00ff00",
                    "fillGradient " + new Rect(0, 20, 10, 20) + " ff00ff00 ffff0000"), canvas.calls);
        }

        @Test
        void aHorizontalGradientIsColumnsOfFills() {
            final Recording canvas = new Recording();
            ShapePaintTest.onFills(canvas, 1f).fillGradient(new Rect(0, 0, 4, 10), Gradient.of(0f, 0xFF000000,
                    0xFFFFFFFF));
            assertEquals(List.of(
                    "fill " + new Rect(0, 0, 1, 10) + " ff202020",
                    "fill " + new Rect(1, 0, 1, 10) + " ff606060",
                    "fill " + new Rect(2, 0, 1, 10) + " ff9f9f9f",
                    "fill " + new Rect(3, 0, 1, 10) + " ffdfdfdf"), canvas.calls);
        }

        @Test
        void aShadowIsTheRectangleInsideRingsThatFade() {
            final RasterCanvas canvas = new RasterCanvas(50, 50);
            ShapePaintTest.onFills(canvas, 1f).shadow(new Rect(15, 15, 20, 20), 0f, 8f, 0xFF000000);
            assertEquals(0xFF, canvas.argb(25, 25) >>> 24, "the rectangle has the full color");
            int previous = 0xFF;
            for (int x = 14; x >= 7; x--) {
                final int alpha = canvas.argb(x, 25) >>> 24;
                assertTrue(alpha <= previous && alpha > 0, "fading outward at " + x + ": " + alpha);
                previous = alpha;
            }
            assertTrue(previous < 0x40, "nearly gone near the end of the blur: " + previous);
            assertEquals(0, canvas.argb(6, 25) >>> 24, "nothing past the blur");
        }

        @Test
        void smoothedOnlyDrawsAndARoundClipClipsToItsRectangle() {
            final NullCanvas canvas = new NullCanvas();
            final Painter painter = ShapePaintTest.onFills(canvas, 1f);
            final int[] depth = new int[1];
            painter.smoothed(false, () -> painter.clippedRound(new Rect(0, 0, 10, 10), 4f,
                    () -> depth[0] = canvas.clipDepth()));
            assertEquals(1, depth[0]);
            assertEquals(0, canvas.clipDepth());
        }
    }

    @Nested
    class WithShapes {
        private final Recording canvas = new Recording();
        private final Painter painter = new CanvasPainter().begin(this.canvas, SHAPES, 1f);

        @Test
        void shapesGoToTheCanvasAsTheyAre() {
            final Stroke stroke = Stroke.of(2f);
            final float[] points = {1, 2, 3, 4};
            this.painter.line(1, 2, 3, 4, stroke, RED);
            this.painter.polyline(points, 2, stroke, RED);
            this.painter.fillShape(TRIANGLE, RED);
            this.painter.strokeShape(TRIANGLE, stroke, RED);
            this.painter.strokeArc(new Rect(0, 0, 10, 10), 1f, 2f, stroke, RED);
            assertEquals(List.of("line", "polyline 2", "fillShape", "strokeShape", "strokeArc 1.0 2.0"),
                    this.canvas.calls);
            assertSame(points, this.canvas.points);
        }

        @Test
        void radiiAreLimitedToHalfTheShorterSide() {
            this.painter.fillRoundRect(new Rect(0, 0, 10, 20), 50f, RED);
            this.painter.strokeRect(new Rect(0, 0, 10, 20), -3f, Stroke.of(1f), RED);
            this.painter.shadow(new Rect(0, 0, 10, 20), 8f, 4f, RED);
            assertEquals(List.of("fillRoundRect 5.0", "strokeRect 0.0", "shadow 5.0 4.0"), this.canvas.calls);
        }

        @Test
        void simplerCallsStandInWhereTheyDrawTheSame() {
            this.painter.fillRoundRect(new Rect(0, 0, 10, 20), 0f, RED);
            this.painter.strokeArc(new Rect(0, 0, 10, 10), 1f, 7f, Stroke.of(1f), RED);
            this.painter.shadow(new Rect(0, 0, 10, 20), 2f, 0f, RED);
            this.painter.clippedRound(new Rect(0, 0, 10, 10), 0f, () -> { });
            assertEquals(List.of("fill " + new Rect(0, 0, 10, 20) + " ffff0000", "strokeEllipse",
                    "fillRoundRect 2.0", "pushClip", "popClip"), this.canvas.calls);
        }

        @Test
        void nothingDrawsNothing() {
            final Stroke none = Stroke.of(0f);
            this.painter.line(1, 2, 3, 4, none, RED);
            this.painter.line(1, 2, 3, 4, Stroke.of(1f), 0x00FF0000);
            this.painter.polyline(new float[]{1, 2}, 1, Stroke.of(1f), RED);
            this.painter.fillEllipse(new Rect(0, 0, 0, 10), RED);
            this.painter.strokeEllipse(new Rect(0, 0, 10, 10), none, RED);
            this.painter.strokeArc(new Rect(0, 0, 10, 10), 1f, 0f, Stroke.of(1f), RED);
            this.painter.fillShape(Shape.builder().build(), RED);
            this.painter.fillGradient(Rect.ZERO, Gradient.of(0f, RED, BLUE));
            assertEquals(List.of(), this.canvas.calls);
        }

        @Test
        void aPolylineChecksItsArray() {
            assertThrows(IllegalArgumentException.class, () -> this.painter.polyline(new float[4], 3, Stroke.of(1f), RED));
            assertThrows(IllegalArgumentException.class, () -> this.painter.polyline(new float[4], -1, Stroke.of(1f),
                    RED));
        }

        @Test
        void smoothingAndRoundClipsArePoppedEvenWhenDrawingThrows() {
            assertThrows(IllegalStateException.class, () -> this.painter.smoothed(false,
                    () -> this.painter.clippedRound(new Rect(0, 0, 10, 10), 3f, () -> {
                        throw new IllegalStateException("broken");
                    })));
            assertEquals(List.of("pushSmoothing false", "pushRoundClip 3.0", "popClip", "popSmoothing"),
                    this.canvas.calls);
        }

        @Test
        void aRoundedPaintFillsARoundedRectangle() {
            org.inspirenxe.strata.api.theme.Paint.rounded(RED, 4f).paint(this.painter, new Rect(0, 0, 20, 20));
            org.inspirenxe.strata.api.theme.Paint.rounded(0x00FF0000, 4f).paint(this.painter, new Rect(0, 0, 20, 20));
            assertEquals(List.of("fillRoundRect 4.0"), this.canvas.calls);
            assertThrows(IllegalArgumentException.class, () -> org.inspirenxe.strata.api.theme.Paint.rounded(RED, -1f));
            assertThrows(IllegalArgumentException.class,
                    () -> org.inspirenxe.strata.api.theme.Paint.rounded(RED, Float.NaN));
        }

        @Test
        void pixelsGoToTheCanvas() {
            final PixelBuffer buffer = PixelBuffer.of(2, 2);
            this.painter.pixels(buffer, new Rect(0, 0, 4, 4), RED);
            assertEquals(List.of("pixels"), this.canvas.calls);
        }
    }

    @Test
    void theDefaultForPixelsDrawsTheVisualPlaceholder() {
        final Recording canvas = new Recording();
        canvas.pixelsByDefault = true;
        ShapePaintTest.onFills(canvas, 1f).pixels(PixelBuffer.of(1, 1), new Rect(1, 2, 3, 4), RED);
        assertEquals(List.of("visual " + new Rect(1, 2, 3, 4)), canvas.calls);
    }

    @Test
    void theDefaultForARoundClipIsARectangularClip() {
        final Recording canvas = new Recording();
        canvas.roundClipByDefault = true;
        new CanvasPainter().begin(canvas, SHAPES, 1f).clippedRound(new Rect(0, 0, 10, 10), 3f, () -> { });
        assertEquals(List.of("pushClip", "popClip"), canvas.calls);
    }

    private static float thinnest(final List<Rect> fills) {
        float thinnest = Float.MAX_VALUE;
        for (final Rect fill : fills) {
            thinnest = Math.min(thinnest, fill.height());
        }
        return thinnest;
    }

    /**
     * Records each call by name and the arguments the tests check, and keeps the defaults it is told to.
     */
    private static final class Recording implements Canvas {
        final List<String> calls = new ArrayList<>();
        float[] points;
        boolean pixelsByDefault;
        boolean roundClipByDefault;

        @Override
        public void fill(final Rect rect, final int argb) {
            this.calls.add("fill " + rect + " " + Integer.toHexString(argb));
        }

        @Override
        public void fillGradient(final Rect rect, final int topArgb, final int bottomArgb) {
            this.calls.add("fillGradient " + rect + " " + Integer.toHexString(topArgb) + " "
                    + Integer.toHexString(bottomArgb));
        }

        @Override
        public void sprite(final SpriteId sprite, final Rect rect, final int tint) {
            this.calls.add("sprite");
        }

        @Override
        public void nineSlice(final SpriteId sprite, final Rect rect, final Insets border, final int tint) {
            this.calls.add("nineSlice");
        }

        @Override
        public void text(final TextSpan span, final float x, final float y) {
            this.calls.add("text");
        }

        @Override
        public void pushClip(final Rect rect) {
            this.calls.add("pushClip");
        }

        @Override
        public void pushRoundClip(final Rect rect, final float radius) {
            if (this.roundClipByDefault) {
                Canvas.super.pushRoundClip(rect, radius);
            } else {
                this.calls.add("pushRoundClip " + radius);
            }
        }

        @Override
        public void popClip() {
            this.calls.add("popClip");
        }

        @Override
        public void pushTranslation(final float dx, final float dy) {
        }

        @Override
        public void pushScale(final float scale) {
        }

        @Override
        public void popTransform() {
        }

        @Override
        public void visual(final Visual visual, final Rect rect, final int tint) {
            this.calls.add("visual " + rect);
        }

        @Override
        public void nextLayer() {
        }

        @Override
        public void requestCursor(final CursorKind cursor) {
        }

        @Override
        public void pushSmoothing(final boolean smooth) {
            this.calls.add("pushSmoothing " + smooth);
        }

        @Override
        public void popSmoothing() {
            this.calls.add("popSmoothing");
        }

        @Override
        public void line(final float x1, final float y1, final float x2, final float y2, final Stroke stroke,
                         final int argb) {
            this.calls.add("line");
        }

        @Override
        public void polyline(final float[] points, final int count, final Stroke stroke, final int argb) {
            this.points = points;
            this.calls.add("polyline " + count);
        }

        @Override
        public void fillRoundRect(final Rect rect, final float radius, final int argb) {
            this.calls.add("fillRoundRect " + radius);
        }

        @Override
        public void strokeRect(final Rect rect, final float radius, final Stroke stroke, final int argb) {
            this.calls.add("strokeRect " + radius);
        }

        @Override
        public void fillEllipse(final Rect oval, final int argb) {
            this.calls.add("fillEllipse");
        }

        @Override
        public void strokeEllipse(final Rect oval, final Stroke stroke, final int argb) {
            this.calls.add("strokeEllipse");
        }

        @Override
        public void strokeArc(final Rect oval, final float startRadians, final float sweepRadians,
                              final Stroke stroke, final int argb) {
            this.calls.add("strokeArc " + startRadians + " " + sweepRadians);
        }

        @Override
        public void fillShape(final Shape shape, final int argb) {
            this.calls.add("fillShape");
        }

        @Override
        public void strokeShape(final Shape shape, final Stroke stroke, final int argb) {
            this.calls.add("strokeShape");
        }

        @Override
        public void shadow(final Rect rect, final float radius, final float blur, final int argb) {
            this.calls.add("shadow " + radius + " " + blur);
        }

        @Override
        public void pixels(final PixelBuffer buffer, final Rect rect, final int tint) {
            if (this.pixelsByDefault) {
                Canvas.super.pixels(buffer, rect, tint);
            } else {
                this.calls.add("pixels");
            }
        }
    }
}
