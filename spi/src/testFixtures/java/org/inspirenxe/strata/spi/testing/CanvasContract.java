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
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.visual.Visual;
import org.inspirenxe.strata.spi.Canvas;
import org.junit.jupiter.api.Test;

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

    private static int red(final int argb) {
        return (argb >> 16) & 0xFF;
    }

    private static int blue(final int argb) {
        return argb & 0xFF;
    }
}
