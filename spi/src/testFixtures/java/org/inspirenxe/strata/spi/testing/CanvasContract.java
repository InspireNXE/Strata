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
package org.inspirenxe.strata.spi.testing;

import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.extension.Gradient;
import org.inspirenxe.strata.api.extension.Shape;
import org.inspirenxe.strata.api.extension.Stroke;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.visual.PixelBuffer;
import org.inspirenxe.strata.api.visual.Visual;
import org.inspirenxe.strata.spi.Canvas;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Checks a {@link Canvas} against the requirements in its documentation. Extend it in an adapter's tests and
 * implement {@link #surface(int, int)}.
 *
 * <p>Every canvas runs the checks that need no pixels: each call is accepted with ordinary and edge-case arguments,
 * and deep clip and transform stacks, scales among them, unwind. A canvas that draws to readable pixels (a Swing
 * image, an offscreen buffer) also returns a {@linkplain Surface#readable() readable} surface; then the checks also
 * verify what was drawn: fills cover their rectangle and nothing more, alpha 0 draws nothing and partial alpha blends,
 * clips, translations and scales apply and restore, gradients run top to bottom, missing sprites and parts of them
 * stay visible and inside their rectangle, a visual the adapter did not make draws a placeholder inside its
 * rectangle, and text stays within its measured width.</p>
 *
 * <p>A canvas whose platform reports {@link org.inspirenxe.strata.spi.Capabilities#drawsShapes() drawsShapes}
 * returns true from {@link #drawsShapes()}, and then also runs the shape checks: every shape call is accepted, and on
 * a readable surface lines cover their length and nothing beside it, rounded rectangles and round clips hide their
 * corners, outlines stay inside their rectangle or oval, arcs turn clockwise from the right, shapes fill by the
 * nonzero rule, gradients run along their angle, shadows fade out within their blur, and hard edges have no partial
 * alpha. A canvas that draws {@link Canvas#pixels} itself returns true from {@link #drawsPixels()}, and then checks
 * that a buffer's pixels land where they should and that a changed buffer draws its new pixels. Every canvas checks
 * that the calls with defaults (round clips, smoothing and pixels) are accepted and unwind.</p>
 *
 * <p>Surfaces use one UI unit per pixel, with the background transparent black ({@code 0x00000000}).</p>
 */
public abstract class CanvasContract {
    /** An opaque red. */
    protected static final int RED = 0xFFFF0000;
    /** An opaque green. */
    protected static final int GREEN = 0xFF00FF00;
    /** An opaque blue. */
    protected static final int BLUE = 0xFF0000FF;
    /** The background every surface starts with. */
    protected static final int CLEAR = 0x00000000;
    /** A sprite no adapter can find. */
    protected static final SpriteId MISSING_SPRITE = SpriteId.of("strata_contract", "does/not/exist");
    /** A visual no adapter made, 8 by 8 units. */
    protected static final Visual FOREIGN_VISUAL = () -> new Size(8, 8);
    /** An opaque white, which leaves sprites and pixels unchanged as a tint. */
    protected static final int WHITE = 0xFFFFFFFF;

    /**
     * A blank area to draw on, with the canvas that draws to it.
     */
    public interface Surface {

        /**
         * Returns the canvas that draws on this surface, ready for a frame.
         *
         * @return the canvas
         */
        Canvas canvas();

        /**
         * Finishes the frame, flushing any recorded draws. The default does nothing.
         */
        default void finish() {
        }

        /**
         * Returns whether {@link #argb} can read back what was drawn.
         *
         * @return true for a canvas that draws to readable pixels
         */
        default boolean readable() {
            return false;
        }

        /**
         * Returns the color of a pixel after {@link #finish()}.
         *
         * @param x the pixel column
         * @param y the pixel row
         * @return the pixel as ARGB
         */
        default int argb(final int x, final int y) {
            throw new UnsupportedOperationException("This surface cannot be read");
        }
    }

    /**
     * Creates a blank surface. Called once per check.
     *
     * @param width the width in pixels, equal to UI units
     * @param height the height in pixels
     * @return the surface
     */
    protected abstract Surface surface(int width, int height);

    /**
     * Returns the text metrics that match the canvas's text, for the text check. The default returns {@code null},
     * which skips that check.
     *
     * @return the metrics, or {@code null}
     */
    protected TextMetrics metrics() {
        return null;
    }

    /**
     * Returns whether the canvas implements the shape calls, as the platform's
     * {@link org.inspirenxe.strata.spi.Capabilities#drawsShapes()} says. The default returns false, which skips the
     * shape checks.
     *
     * @return true to run the shape checks
     */
    protected boolean drawsShapes() {
        return false;
    }

    /**
     * Returns whether the canvas draws {@link Canvas#pixels} itself instead of the default placeholder. The default
     * returns false, which skips the pixel buffer checks.
     *
     * @return true to run the pixel buffer checks
     */
    protected boolean drawsPixels() {
        return false;
    }

    private Surface shapes(final int width, final int height, final java.util.function.Consumer<Canvas> draw) {
        assumeTrue(this.drawsShapes(), "this canvas draws no shapes");
        return this.readable(width, height, draw);
    }

    private Surface drawn(final int width, final int height, final java.util.function.Consumer<Canvas> draw) {
        final Surface surface = this.surface(width, height);
        draw.accept(surface.canvas());
        surface.finish();
        return surface;
    }

    private Surface readable(final int width, final int height, final java.util.function.Consumer<Canvas> draw) {
        final Surface surface = this.surface(width, height);
        assumeTrue(surface.readable(), "this canvas cannot be read back");
        draw.accept(surface.canvas());
        surface.finish();
        return surface;
    }

    // ------------------------------------------------------------------
    // Checks for every canvas
    // ------------------------------------------------------------------

    @Test
    void acceptsEveryCall() {
        assertDoesNotThrow(() -> this.drawn(64, 64, canvas -> {
            canvas.fill(new Rect(1, 1, 10, 10), RED);
            canvas.fillGradient(new Rect(1, 1, 10, 10), RED, BLUE);
            canvas.sprite(MISSING_SPRITE, new Rect(1, 1, 10, 10), 0xFFFFFFFF);
            canvas.spriteRegion(MISSING_SPRITE, new Rect(1, 1, 10, 10), 0.25f, 0.25f, 0.75f, 0.75f, 0xFFFFFFFF);
            canvas.nineSlice(MISSING_SPRITE, new Rect(1, 1, 30, 20), Insets.of(3), 0xFFFFFFFF);
            canvas.visual(FOREIGN_VISUAL, new Rect(1, 1, 8, 8), 0xFFFFFFFF);
            canvas.visual(FOREIGN_VISUAL, new Rect(1, 1, 8, 8), 0x80FF0000);
            canvas.pushScale(2f);
            canvas.fill(new Rect(1, 1, 4, 4), RED);
            canvas.text(TextSpan.of("Scaled"), 2, 2);
            canvas.popTransform();
            canvas.text(TextSpan.of("Strata"), 2, 2);
            canvas.text(new TextSpan("Styled", TextStyle.DEFAULT.withBold(true).withItalic(true).withUnderline(true)
                    .withStrikethrough(true).withShadow(true)), 2, 20);
            canvas.nextLayer();
            for (final CursorKind cursor : CursorKind.values()) {
                canvas.requestCursor(cursor);
            }
        }));
    }

    @Test
    void acceptsTheCallsWithDefaults() {
        final PixelBuffer buffer = PixelBuffer.of(4, 4).fill(RED).changed();
        assertDoesNotThrow(() -> this.drawn(32, 32, canvas -> {
            canvas.pixels(buffer, new Rect(1, 1, 8, 8), WHITE);
            canvas.pixels(buffer, new Rect(1, 1, 8, 8), 0x80FFFFFF);
            buffer.set(0, 0, BLUE).changed();
            canvas.pixels(buffer, new Rect(10, 10, 4, 4), WHITE);
            canvas.pixels(buffer, Rect.ZERO, WHITE);
            for (int i = 0; i < 16; i++) {
                canvas.pushRoundClip(new Rect(i, i, 20, 20), 4f);
                canvas.pushSmoothing(i % 2 == 0);
            }
            canvas.fill(new Rect(0, 0, 4, 4), RED);
            for (int i = 0; i < 16; i++) {
                canvas.popSmoothing();
                canvas.popClip();
            }
            canvas.fill(new Rect(0, 0, 4, 4), GREEN);
        }));
    }

    @Test
    void acceptsEveryShapeCall() {
        assumeTrue(this.drawsShapes(), "this canvas draws no shapes");
        final Shape shape = Shape.builder().moveTo(2, 2).lineTo(20, 4).quadTo(30, 10, 20, 20)
                .cubicTo(15, 25, 5, 25, 2, 20).close().arc(new Rect(30, 30, 20, 20), 0f, 3f).build();
        final float[] points = {1, 1, 10, 20, 20, 5, 30, 25};
        assertDoesNotThrow(() -> this.drawn(64, 64, canvas -> {
            for (final Stroke.Cap cap : Stroke.Cap.values()) {
                for (final Stroke.Join join : Stroke.Join.values()) {
                    final Stroke stroke = new Stroke(3f, cap, join);
                    canvas.line(1, 1, 40, 30, stroke, RED);
                    canvas.polyline(points, 4, stroke, RED);
                    canvas.strokeShape(shape, stroke, RED);
                    canvas.strokeArc(new Rect(4, 4, 40, 30), 1f, -2f, stroke, RED);
                }
            }
            canvas.fillGradient(new Rect(1, 1, 30, 20), Gradient.of(0.7f, RED, BLUE));
            canvas.fillRoundRect(new Rect(1, 1, 30, 20), 5f, RED);
            canvas.strokeRect(new Rect(1, 1, 30, 20), 0f, Stroke.of(1f), RED);
            canvas.strokeRect(new Rect(1, 1, 30, 20), 5f, Stroke.of(2f), 0x80FF0000);
            canvas.fillEllipse(new Rect(1, 1, 30, 20), RED);
            canvas.strokeEllipse(new Rect(1, 1, 30, 20), Stroke.of(2f), RED);
            canvas.fillShape(shape, 0x8000FF00);
            canvas.shadow(new Rect(10, 10, 20, 20), 4f, 6f, 0x80000000);
            canvas.pushScale(1.5f);
            canvas.pushSmoothing(false);
            canvas.fillEllipse(new Rect(1, 1, 30, 20), RED);
            canvas.line(1, 1, 30, 20, Stroke.of(0.5f), BLUE);
            canvas.popSmoothing();
            canvas.popTransform();
        }));
    }

    @Test
    void acceptsEdgeCaseShapeArguments() {
        assumeTrue(this.drawsShapes(), "this canvas draws no shapes");
        final Shape lone = Shape.builder().moveTo(4, 4).build();
        final Shape far = Shape.builder().moveTo(-100_000, -100_000).lineTo(100_000, 5).lineTo(5, 100_000).build();
        assertDoesNotThrow(() -> this.drawn(16, 16, canvas -> {
            canvas.line(5, 5, 5, 5, new Stroke(2f, Stroke.Cap.ROUND, Stroke.Join.ROUND), RED);
            canvas.line(-1000, -1000, 1000, 1000, Stroke.of(50f), RED);
            canvas.polyline(new float[]{1, 1, 1, 1, 1, 1}, 3, Stroke.of(1f), RED);
            canvas.fillRoundRect(new Rect(1, 1, 0.5f, 0.5f), 0.25f, RED);
            canvas.fillRoundRect(new Rect(1, 1, 10, 10), 5f, RED);
            canvas.strokeRect(new Rect(1, 1, 4, 4), 2f, Stroke.of(10f), RED);
            canvas.fillEllipse(new Rect(0, 0, 100_000, 100_000), RED);
            canvas.strokeEllipse(new Rect(1, 1, 4, 4), Stroke.of(10f), RED);
            canvas.strokeArc(new Rect(1, 1, 10, 10), 0f, 1e-4f, Stroke.of(1f), RED);
            canvas.fillShape(lone, RED);
            canvas.strokeShape(lone, Stroke.of(1f), RED);
            canvas.fillShape(far, RED);
            canvas.fillGradient(new Rect(1, 1, 0.5f, 10), new Gradient(-7f,
                    List.of(new Gradient.Stop(0.5f, RED), new Gradient.Stop(0.5f, BLUE))));
            canvas.shadow(new Rect(-5, -5, 1, 1), 0f, 100f, RED);
            canvas.pushRoundClip(new Rect(0, 0, 10, 10), 100f);
            canvas.fill(new Rect(0, 0, 10, 10), RED);
            canvas.popClip();
        }));
    }

    @Test
    void acceptsEdgeCaseArguments() {
        assertDoesNotThrow(() -> this.drawn(16, 16, canvas -> {
            canvas.fill(Rect.ZERO, RED);
            canvas.fill(new Rect(-50, -50, 10, 10), RED);
            canvas.fill(new Rect(0, 0, 100_000, 100_000), RED);
            canvas.fill(new Rect(0.25f, 0.75f, 3.5f, 2.25f), RED);
            canvas.fillGradient(Rect.ZERO, RED, BLUE);
            canvas.nineSlice(MISSING_SPRITE, new Rect(0, 0, 2, 2), Insets.of(8), 0xFFFFFFFF);
            canvas.text(TextSpan.of(""), 0, 0);
            canvas.text(TextSpan.of("off screen"), -1000, -1000);
            canvas.pushClip(Rect.ZERO);
            canvas.fill(new Rect(0, 0, 10, 10), RED);
            canvas.popClip();
        }));
    }

    @Test
    void deepClipAndTransformStacksUnwind() {
        assertDoesNotThrow(() -> this.drawn(32, 32, canvas -> {
            for (int i = 0; i < 32; i++) {
                canvas.pushClip(new Rect(i * 0.5f, i * 0.5f, 32, 32));
                canvas.pushTranslation(0.5f, 0.5f);
                canvas.pushScale(i % 2 == 0 ? 1.5f : 0.75f);
            }
            canvas.fill(new Rect(0, 0, 4, 4), RED);
            for (int i = 0; i < 32; i++) {
                canvas.popTransform();
                canvas.popTransform();
                canvas.popClip();
            }
            canvas.fill(new Rect(0, 0, 4, 4), GREEN);
        }));
    }

    // ------------------------------------------------------------------
    // Checks for canvases that can be read back
    // ------------------------------------------------------------------

    @Test
    void fillCoversExactlyItsRectangle() {
        final Surface surface = this.readable(20, 20, canvas -> canvas.fill(new Rect(5, 5, 10, 10), RED));
        assertEquals(RED, surface.argb(5, 5), "top-left corner is inside");
        assertEquals(RED, surface.argb(14, 14), "bottom-right corner is inside");
        assertEquals(CLEAR, surface.argb(4, 10), "left of the rectangle");
        assertEquals(CLEAR, surface.argb(15, 10), "the right edge is outside");
        assertEquals(CLEAR, surface.argb(10, 15), "the bottom edge is outside");
    }

    @Test
    void transparentColorDrawsNothing() {
        final Surface surface = this.readable(10, 10, canvas -> {
            canvas.fill(new Rect(0, 0, 10, 10), GREEN);
            canvas.fill(new Rect(0, 0, 10, 10), 0x00FF0000);
        });
        assertEquals(GREEN, surface.argb(5, 5));
    }

    @Test
    void laterDrawsCoverEarlierOnes() {
        final Surface surface = this.readable(10, 10, canvas -> {
            canvas.fill(new Rect(0, 0, 10, 10), RED);
            canvas.fill(new Rect(0, 0, 10, 10), BLUE);
        });
        assertEquals(BLUE, surface.argb(5, 5));
    }

    @Test
    void clipLimitsDrawingAndRestores() {
        final Surface surface = this.readable(20, 20, canvas -> {
            canvas.pushClip(new Rect(5, 5, 5, 5));
            canvas.fill(new Rect(0, 0, 20, 20), RED);
            canvas.popClip();
            canvas.fill(new Rect(15, 15, 5, 5), BLUE);
        });
        assertEquals(RED, surface.argb(7, 7), "inside the clip");
        assertEquals(CLEAR, surface.argb(2, 2), "outside the clip");
        assertEquals(CLEAR, surface.argb(12, 7), "outside the clip");
        assertEquals(BLUE, surface.argb(17, 17), "after popClip, drawing is no longer clipped");
    }

    @Test
    void nestedClipsIntersect() {
        final Surface surface = this.readable(20, 20, canvas -> {
            canvas.pushClip(new Rect(0, 0, 10, 10));
            canvas.pushClip(new Rect(5, 5, 10, 10));
            canvas.fill(new Rect(0, 0, 20, 20), RED);
            canvas.popClip();
            canvas.popClip();
        });
        assertEquals(RED, surface.argb(7, 7), "inside both clips");
        assertEquals(CLEAR, surface.argb(2, 2), "inside the outer clip only");
        assertEquals(CLEAR, surface.argb(12, 12), "inside the inner clip only");
    }

    @Test
    void translationShiftsDrawingClipsAndRestores() {
        final Surface surface = this.readable(30, 30, canvas -> {
            canvas.pushTranslation(10, 10);
            canvas.pushTranslation(5, 0);
            canvas.fill(new Rect(0, 0, 2, 2), RED);
            canvas.popTransform();
            canvas.pushClip(new Rect(0, 0, 2, 2));
            canvas.fill(new Rect(0, 0, 10, 10), GREEN);
            canvas.popClip();
            canvas.popTransform();
            canvas.fill(new Rect(0, 0, 2, 2), BLUE);
        });
        assertEquals(RED, surface.argb(15, 10), "nested translations add up");
        assertEquals(GREEN, surface.argb(11, 11), "a clip pushed under a translation is translated");
        assertEquals(CLEAR, surface.argb(13, 13), "and still clips");
        assertEquals(BLUE, surface.argb(0, 0), "popTransform restores the origin");
    }

    @Test
    void scaleMultipliesCoordinatesAndRestores() {
        final Surface surface = this.readable(30, 30, canvas -> {
            canvas.pushTranslation(10, 0);
            canvas.pushScale(2f);
            canvas.fill(new Rect(1, 1, 2, 2), RED);
            canvas.popTransform();
            canvas.popTransform();
            canvas.fill(new Rect(20, 20, 2, 2), BLUE);
        });
        assertEquals(RED, surface.argb(12, 2), "the scaled rectangle's top-left corner");
        assertEquals(RED, surface.argb(15, 5), "the scaled rectangle's bottom-right corner");
        assertEquals(CLEAR, surface.argb(11, 1), "outside the scaled rectangle");
        assertEquals(CLEAR, surface.argb(16, 6), "outside the scaled rectangle");
        assertEquals(BLUE, surface.argb(21, 21), "popTransform restores the scale");
        assertEquals(CLEAR, surface.argb(22, 22), "popTransform restores the scale");
    }

    @Test
    void partialAlphaBlendsOverWhatIsDrawn() {
        final Surface surface = this.readable(10, 10, canvas -> {
            canvas.fill(new Rect(0, 0, 10, 10), BLUE);
            canvas.fill(new Rect(0, 0, 10, 10), 0x80FF0000);
        });
        final int blended = surface.argb(5, 5);
        final String hex = Integer.toHexString(blended);
        assertEquals(0xFF, blended >>> 24, "blending over an opaque color stays opaque: " + hex);
        assertTrue(red(blended) > 0x50 && red(blended) < 0xB0, "about half the red: " + hex);
        assertTrue(blue(blended) > 0x50 && blue(blended) < 0xB0, "about half the blue below: " + hex);
    }

    @Test
    void missingSpriteRegionStaysVisibleAndInside() {
        final Surface surface = this.readable(20, 20, canvas -> canvas.spriteRegion(MISSING_SPRITE,
                new Rect(5, 5, 6, 6), 0.5f, 0.5f, 1f, 1f, 0xFFFFFFFF));
        assertNotEquals(CLEAR, surface.argb(7, 7), "a part of a missing sprite must still show up");
        assertEquals(CLEAR, surface.argb(3, 3), "and only inside its rectangle");
        assertEquals(CLEAR, surface.argb(12, 12), "and only inside its rectangle");
    }

    @Test
    void foreignVisualDrawsAPlaceholderInside() {
        final Surface surface = this.readable(20, 20, canvas -> canvas.visual(FOREIGN_VISUAL, new Rect(5, 5, 6, 6),
                0xFFFFFFFF));
        assertNotEquals(CLEAR, surface.argb(7, 7), "a visual the adapter did not make draws a placeholder");
        assertEquals(CLEAR, surface.argb(3, 3), "and only inside its rectangle");
        assertEquals(CLEAR, surface.argb(12, 12), "and only inside its rectangle");
    }

    @Test
    void gradientRunsTopToBottom() {
        final Surface surface = this.readable(10, 40, canvas -> canvas.fillGradient(new Rect(0, 0, 10, 40), RED, BLUE));
        final int top = surface.argb(5, 0);
        final int bottom = surface.argb(5, 39);
        assertTrue(red(top) > blue(top), "the top is mostly the top color: " + Integer.toHexString(top));
        assertTrue(blue(bottom) > red(bottom), "the bottom is mostly the bottom color: " + Integer.toHexString(bottom));
    }

    @Test
    void missingSpriteStaysVisibleAndInside() {
        final Surface surface = this.readable(20, 20, canvas -> canvas.sprite(MISSING_SPRITE, new Rect(5, 5, 6, 6), 0xFFFFFFFF));
        assertNotEquals(CLEAR, surface.argb(7, 7), "a missing sprite must still show up");
        assertEquals(CLEAR, surface.argb(3, 3), "and only inside its rectangle");
        assertEquals(CLEAR, surface.argb(12, 12), "and only inside its rectangle");
    }

    @Test
    void nineSliceStaysInsideASmallRectangle() {
        final Surface surface = this.readable(20, 20, canvas -> canvas.nineSlice(MISSING_SPRITE, new Rect(8, 8, 3, 3), Insets.of(6), 0xFFFFFFFF));
        assertEquals(CLEAR, surface.argb(6, 6), "borders larger than the rectangle must not spill out");
        assertEquals(CLEAR, surface.argb(12, 12), "borders larger than the rectangle must not spill out");
    }

    @Test
    void textStaysWithinItsMeasuredWidth() {
        final TextMetrics metrics = this.metrics();
        assumeTrue(metrics != null, "no metrics given for the text check");
        final TextSpan span = new TextSpan("Wide WWWW text", TextStyle.DEFAULT);
        final float width = metrics.width(span);
        final int height = (int) Math.ceil(metrics.lineHeight(span.style())) + 4;
        final int imageWidth = (int) Math.ceil(width) + 20;
        final Surface surface = this.readable(imageWidth, height, canvas -> canvas.text(span, 2, 0));

        int rightmost = -1;
        for (int x = 0; x < imageWidth; x++) {
            for (int y = 0; y < height; y++) {
                if ((surface.argb(x, y) >>> 24) != 0) {
                    rightmost = x;
                }
            }
        }
        assertTrue(rightmost >= 0, "the text drew something");
        assertTrue(rightmost <= 2 + width + 1.5f, "text ends at " + rightmost + ", past its measured width " + width);
    }

    // ------------------------------------------------------------------
    // Shape checks for canvases that can be read back
    // ------------------------------------------------------------------

    @Test
    void lineCoversItsLengthAndNothingBeside() {
        final Surface surface = this.shapes(32, 20, canvas -> canvas.line(4, 10, 26, 10, Stroke.of(2f), RED));
        assertNotEquals(CLEAR, surface.argb(5, 9), "near the start, above the middle");
        assertNotEquals(CLEAR, surface.argb(15, 10), "the middle");
        assertNotEquals(CLEAR, surface.argb(24, 10), "near the end");
        assertEquals(CLEAR, surface.argb(15, 6), "above the line");
        assertEquals(CLEAR, surface.argb(15, 13), "below the line");
        assertEquals(CLEAR, surface.argb(1, 10), "before the start, with butt caps");
        assertEquals(CLEAR, surface.argb(29, 10), "past the end, with butt caps");
    }

    @Test
    void diagonalLineFollowsItsSegment() {
        final Surface surface = this.shapes(32, 32, canvas -> canvas.line(2, 2, 28, 28, Stroke.of(3f), RED));
        assertNotEquals(CLEAR, surface.argb(15, 15), "on the segment");
        assertEquals(CLEAR, surface.argb(15, 5), "beside it");
        assertEquals(CLEAR, surface.argb(5, 15), "beside it on the other side");
    }

    @Test
    void polylineRunsThroughItsPoints() {
        final float[] points = {4, 4, 24, 4, 24, 24, 99, 99};
        final Surface surface = this.shapes(32, 32, canvas -> canvas.polyline(points, 3, Stroke.of(2f), RED));
        assertNotEquals(CLEAR, surface.argb(14, 4), "the first segment");
        assertNotEquals(CLEAR, surface.argb(24, 14), "the second segment");
        assertEquals(CLEAR, surface.argb(14, 14), "inside the corner");
        assertEquals(CLEAR, surface.argb(24, 28), "the point past the count is not drawn");
    }

    @Test
    void roundRectangleHidesItsCorners() {
        final Surface surface = this.shapes(32, 32, canvas -> canvas.fillRoundRect(new Rect(0, 0, 30, 30), 10f, RED));
        assertEquals(RED, surface.argb(15, 15), "the middle");
        assertNotEquals(CLEAR, surface.argb(15, 0), "the middle of the top edge");
        assertNotEquals(CLEAR, surface.argb(0, 15), "the middle of the left edge");
        assertEquals(CLEAR, surface.argb(1, 1), "the top left corner is rounded off");
        assertEquals(CLEAR, surface.argb(28, 28), "the bottom right corner is rounded off");
        assertEquals(CLEAR, surface.argb(30, 15), "right of the rectangle");
    }

    @Test
    void outlineStaysInsideItsRectangle() {
        final Surface surface = this.shapes(32, 32, canvas -> canvas.strokeRect(new Rect(5, 5, 20, 20), 0f,
                Stroke.of(2f), RED));
        assertNotEquals(CLEAR, surface.argb(5, 15), "the left edge's outer pixel");
        assertNotEquals(CLEAR, surface.argb(6, 15), "the left edge's inner pixel");
        assertNotEquals(CLEAR, surface.argb(24, 15), "the right edge");
        assertNotEquals(CLEAR, surface.argb(15, 5), "the top edge");
        assertNotEquals(CLEAR, surface.argb(5, 5), "a square corner");
        assertEquals(CLEAR, surface.argb(4, 15), "outside the rectangle");
        assertEquals(CLEAR, surface.argb(25, 15), "outside the rectangle");
        assertEquals(CLEAR, surface.argb(8, 15), "inside the outline");
        assertEquals(CLEAR, surface.argb(15, 15), "the middle");
    }

    @Test
    void roundOutlineRoundsItsCorners() {
        final Surface surface = this.shapes(32, 32, canvas -> canvas.strokeRect(new Rect(0, 0, 30, 30), 10f,
                Stroke.of(2f), RED));
        assertNotEquals(CLEAR, surface.argb(15, 0), "the top edge");
        assertEquals(CLEAR, surface.argb(0, 0), "the corner is rounded off");
        assertEquals(CLEAR, surface.argb(15, 15), "the middle");
    }

    @Test
    void ellipseStaysInItsOval() {
        final Surface surface = this.shapes(32, 32, canvas -> canvas.fillEllipse(new Rect(5, 5, 20, 20), RED));
        assertEquals(RED, surface.argb(15, 15), "the middle");
        assertNotEquals(CLEAR, surface.argb(6, 15), "near the left edge");
        assertEquals(CLEAR, surface.argb(6, 6), "the oval's corner");
        assertEquals(CLEAR, surface.argb(3, 15), "outside the oval");
    }

    @Test
    void ellipseOutlineIsARingInsideItsOval() {
        final Surface surface = this.shapes(32, 32, canvas -> canvas.strokeEllipse(new Rect(0, 0, 30, 30),
                Stroke.of(3f), RED));
        assertNotEquals(CLEAR, surface.argb(15, 1), "the top of the ring");
        assertEquals(CLEAR, surface.argb(15, 15), "the middle");
        assertEquals(CLEAR, surface.argb(1, 1), "the oval's corner");
        assertEquals(CLEAR, surface.argb(31, 15), "outside the oval");
    }

    @Test
    void arcTurnsClockwiseFromTheRight() {
        final Surface surface = this.shapes(32, 32, canvas -> canvas.strokeArc(new Rect(0, 0, 30, 30), 0f,
                (float) (Math.PI / 2), Stroke.of(3f), RED));
        assertNotEquals(CLEAR, surface.argb(24, 24), "the bottom right quarter, between 0 and a quarter turn");
        assertEquals(CLEAR, surface.argb(24, 5), "the top right quarter is not part of it");
        assertEquals(CLEAR, surface.argb(5, 5), "the top left quarter is not part of it");
        assertEquals(CLEAR, surface.argb(15, 15), "the middle");
    }

    @Test
    void shapeFillsInsideItsBounds() {
        final Shape triangle = Shape.builder().moveTo(5, 5).lineTo(25, 5).lineTo(15, 25).close().build();
        final Surface surface = this.shapes(32, 32, canvas -> canvas.fillShape(triangle, RED));
        assertEquals(RED, surface.argb(15, 11), "inside the triangle");
        assertEquals(CLEAR, surface.argb(5, 24), "inside its bounds, outside the triangle");
        assertEquals(CLEAR, surface.argb(27, 5), "outside its bounds");
    }

    @Test
    void shapeFillsByTheNonzeroRule() {
        final Shape hole = Shape.builder().moveTo(0, 0).lineTo(30, 0).lineTo(30, 30).lineTo(0, 30).close()
                .moveTo(10, 10).lineTo(10, 20).lineTo(20, 20).lineTo(20, 10).close().build();
        final Shape same = Shape.builder().moveTo(0, 0).lineTo(30, 0).lineTo(30, 30).lineTo(0, 30).close()
                .moveTo(10, 10).lineTo(20, 10).lineTo(20, 20).lineTo(10, 20).close().build();
        final Surface holed = this.shapes(32, 32, canvas -> canvas.fillShape(hole, RED));
        assertEquals(RED, holed.argb(5, 5), "the outer square");
        assertEquals(CLEAR, holed.argb(15, 15), "an inner square that runs the other way is a hole");
        final Surface filled = this.shapes(32, 32, canvas -> canvas.fillShape(same, RED));
        assertEquals(RED, filled.argb(15, 15), "an inner square that runs the same way is filled");
    }

    @Test
    void shapeOutlineFollowsItsPath() {
        final Shape square = Shape.builder().moveTo(5, 5).lineTo(25, 5).lineTo(25, 25).lineTo(5, 25).close().build();
        final Surface surface = this.shapes(32, 32, canvas -> canvas.strokeShape(square, Stroke.of(2f), RED));
        assertNotEquals(CLEAR, surface.argb(15, 4), "the top side, centered on the path");
        assertNotEquals(CLEAR, surface.argb(15, 5), "the top side, centered on the path");
        assertNotEquals(CLEAR, surface.argb(5, 15), "the closing side");
        assertEquals(CLEAR, surface.argb(15, 15), "the middle");
    }

    @Test
    void roundClipHidesCornersAndRestores() {
        final Surface surface = this.shapes(32, 32, canvas -> {
            canvas.pushRoundClip(new Rect(0, 0, 30, 30), 10f);
            canvas.fill(new Rect(0, 0, 32, 32), RED);
            canvas.popClip();
            canvas.fill(new Rect(28, 28, 4, 4), BLUE);
        });
        assertEquals(RED, surface.argb(15, 15), "the middle");
        assertEquals(RED, surface.argb(15, 0), "the middle of the top edge");
        assertEquals(CLEAR, surface.argb(1, 1), "the rounded corner hides drawing");
        assertEquals(CLEAR, surface.argb(31, 15), "outside the clip");
        assertEquals(BLUE, surface.argb(29, 29), "after popClip, drawing is no longer clipped");
    }

    @Test
    void gradientRunsAlongItsAngle() {
        final Surface across = this.shapes(40, 10, canvas -> canvas.fillGradient(new Rect(0, 0, 40, 10),
                Gradient.of(0f, RED, BLUE)));
        assertTrue(red(across.argb(0, 5)) > blue(across.argb(0, 5)), "angle 0 starts at the left");
        assertTrue(blue(across.argb(39, 5)) > red(across.argb(39, 5)), "and ends at the right");
        final Surface down = this.shapes(10, 40, canvas -> canvas.fillGradient(new Rect(0, 0, 10, 40),
                Gradient.of((float) (Math.PI / 2), RED, BLUE)));
        assertTrue(red(down.argb(5, 0)) > blue(down.argb(5, 0)), "a quarter turn starts at the top");
        assertTrue(blue(down.argb(5, 39)) > red(down.argb(5, 39)), "and ends at the bottom");
        final Surface back = this.shapes(40, 10, canvas -> canvas.fillGradient(new Rect(0, 0, 40, 10),
                Gradient.of((float) Math.PI, RED, BLUE)));
        assertTrue(blue(back.argb(0, 5)) > red(back.argb(0, 5)), "a half turn runs right to left");
        final Surface stops = this.shapes(40, 10, canvas -> canvas.fillGradient(new Rect(0, 0, 40, 10),
                new Gradient(0f, List.of(new Gradient.Stop(0f, RED), new Gradient.Stop(0.5f, GREEN),
                        new Gradient.Stop(1f, BLUE)))));
        final int middle = stops.argb(20, 5);
        assertTrue(green(middle) > red(middle) && green(middle) > blue(middle), "the middle stop shows in the middle: "
                + Integer.toHexString(middle));
    }

    @Test
    void shadowFadesOutWithinItsBlur() {
        final Surface surface = this.shapes(40, 40, canvas -> canvas.shadow(new Rect(10, 10, 20, 20), 0f, 8f,
                0xFF000000));
        final int middle = surface.argb(20, 20) >>> 24;
        final int fading = surface.argb(6, 20) >>> 24;
        assertTrue(middle > 0xC0, "the rectangle has nearly the full color: " + middle);
        assertTrue(fading > 0 && fading < middle, "a few units outside, the shadow is fading: " + fading);
        assertEquals(CLEAR, surface.argb(0, 20), "past the blur, nothing");
    }

    @Test
    void hardEdgesHaveNoPartialAlpha() {
        final Surface surface = this.shapes(32, 32, canvas -> {
            canvas.pushSmoothing(false);
            canvas.fillEllipse(new Rect(2, 2, 26, 26), RED);
            canvas.line(0, 31, 31, 0, Stroke.of(1.5f), BLUE);
            canvas.popSmoothing();
        });
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 32; x++) {
                final int alpha = surface.argb(x, y) >>> 24;
                assertTrue(alpha == 0 || alpha == 0xFF, "pixel " + x + ", " + y + " has alpha " + alpha);
            }
        }
    }

    // ------------------------------------------------------------------
    // Pixel buffer checks for canvases that can be read back
    // ------------------------------------------------------------------

    @Test
    void pixelBufferLandsWhereItShould() {
        assumeTrue(this.drawsPixels(), "this canvas draws no pixel buffers");
        final PixelBuffer buffer = PixelBuffer.of(2, 2).set(0, 0, RED).set(1, 0, GREEN).set(0, 1, BLUE)
                .set(1, 1, WHITE).changed();
        final Surface surface = this.readable(16, 16, canvas -> canvas.pixels(buffer, new Rect(4, 4, 8, 8), WHITE));
        assertEquals(RED, surface.argb(5, 5), "the top left pixel");
        assertEquals(GREEN, surface.argb(10, 5), "the top right pixel");
        assertEquals(BLUE, surface.argb(5, 10), "the bottom left pixel");
        assertEquals(WHITE, surface.argb(10, 10), "the bottom right pixel");
        assertEquals(CLEAR, surface.argb(2, 2), "outside the rectangle");
        assertEquals(CLEAR, surface.argb(13, 13), "outside the rectangle");
    }

    @Test
    void changedPixelBufferDrawsItsNewPixels() {
        assumeTrue(this.drawsPixels(), "this canvas draws no pixel buffers");
        final PixelBuffer buffer = PixelBuffer.of(2, 2).fill(RED).changed();
        final Surface surface = this.readable(16, 16, canvas -> {
            canvas.pixels(buffer, new Rect(0, 0, 8, 8), WHITE);
            buffer.fill(BLUE).changed();
            canvas.pixels(buffer, new Rect(0, 0, 8, 8), WHITE);
        });
        assertEquals(BLUE, surface.argb(4, 4), "a buffer whose version moved is copied again");
    }

    private static int green(final int argb) {
        return (argb >> 8) & 0xFF;
    }

    private static int red(final int argb) {
        return (argb >> 16) & 0xFF;
    }

    private static int blue(final int argb) {
        return argb & 0xFF;
    }
}
