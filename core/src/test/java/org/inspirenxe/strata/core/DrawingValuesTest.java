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

import org.inspirenxe.strata.api.extension.Gradient;
import org.inspirenxe.strata.api.extension.Shape;
import org.inspirenxe.strata.api.extension.Stroke;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.visual.PixelBuffer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The values components draw with: strokes, gradients, shapes and pixel buffers.
 */
class DrawingValuesTest {

    /**
     * Records the segments a shape walks through, one string each.
     */
    private static List<String> segments(final Shape shape) {
        final List<String> out = new ArrayList<>();
        shape.walk(new Shape.Sink() {
            @Override
            public void moveTo(final float x, final float y) {
                out.add("M " + x + " " + y);
            }

            @Override
            public void lineTo(final float x, final float y) {
                out.add("L " + x + " " + y);
            }

            @Override
            public void quadTo(final float cx, final float cy, final float x, final float y) {
                out.add("Q " + cx + " " + cy + " " + x + " " + y);
            }

            @Override
            public void cubicTo(final float c1x, final float c1y, final float c2x, final float c2y, final float x,
                                final float y) {
                out.add("C " + x + " " + y);
            }

            @Override
            public void close() {
                out.add("Z");
            }
        });
        return out;
    }

    @Nested
    class Strokes {
        @Test
        void ofHasButtCapsAndMiterJoins() {
            assertEquals(new Stroke(2f, Stroke.Cap.BUTT, Stroke.Join.MITER), Stroke.of(2f));
        }

        @Test
        void aWidthMustBeFiniteAndNotNegative() {
            assertThrows(IllegalArgumentException.class, () -> Stroke.of(-1f));
            assertThrows(IllegalArgumentException.class, () -> Stroke.of(Float.NaN));
            assertThrows(IllegalArgumentException.class, () -> Stroke.of(Float.POSITIVE_INFINITY));
            assertEquals(0f, Stroke.of(0f).width());
        }
    }

    @Nested
    class Gradients {
        private final Gradient three = new Gradient(0f, List.of(new Gradient.Stop(0f, 0xFF000000),
                new Gradient.Stop(0.5f, 0xFFFF0000), new Gradient.Stop(1f, 0xFFFFFFFF)));

        @Test
        void colorsBlendBetweenStopsAndHoldBeyondThem() {
            assertEquals(0xFF000000, this.three.colorAt(-1f));
            assertEquals(0xFF000000, this.three.colorAt(0f));
            assertEquals(0xFF800000, this.three.colorAt(0.25f));
            assertEquals(0xFFFF0000, this.three.colorAt(0.5f));
            assertEquals(0xFFFF8080, this.three.colorAt(0.75f));
            assertEquals(0xFFFFFFFF, this.three.colorAt(2f));
        }

        @Test
        void alphaBlendsToo() {
            assertEquals(0x80, Gradient.blend(0x00000000, 0xFF000000, 0.5f) >>> 24);
        }

        @Test
        void twoStopsAtOneOffsetMakeAHardStep() {
            final Gradient step = new Gradient(0f, List.of(new Gradient.Stop(0.5f, 0xFF000000),
                    new Gradient.Stop(0.5f, 0xFFFFFFFF)));
            assertEquals(0xFF000000, step.colorAt(0.4f));
            assertEquals(0xFFFFFFFF, step.colorAt(0.6f));
        }

        @Test
        void stopsAreCheckedAndCopied() {
            final List<Gradient.Stop> stops = new ArrayList<>(List.of(new Gradient.Stop(0f, 1), new Gradient.Stop(1f, 2)));
            final Gradient gradient = new Gradient(0f, stops);
            stops.clear();
            assertEquals(2, gradient.stops().size());
            assertThrows(IllegalArgumentException.class, () -> new Gradient(0f, List.of(new Gradient.Stop(0f, 1))));
            assertThrows(IllegalArgumentException.class, () -> new Gradient(0f,
                    List.of(new Gradient.Stop(0.8f, 1), new Gradient.Stop(0.2f, 2))));
            assertThrows(IllegalArgumentException.class, () -> new Gradient.Stop(1.5f, 1));
            assertThrows(IllegalArgumentException.class, () -> new Gradient.Stop(Float.NaN, 1));
            assertThrows(IllegalArgumentException.class, () -> Gradient.of(Float.NaN, 1, 2));
        }
    }

    @Nested
    class Shapes {
        @Test
        void aShapeWalksItsSegmentsInOrder() {
            final Shape shape = Shape.builder().moveTo(1, 2).lineTo(3, 4).quadTo(5, 6, 7, 8)
                    .cubicTo(1, 1, 2, 2, 9, 9).close().build();
            assertEquals(List.of("M 1.0 2.0", "L 3.0 4.0", "Q 5.0 6.0 7.0 8.0", "C 9.0 9.0", "Z"),
                    DrawingValuesTest.segments(shape));
        }

        @Test
        void boundsHoldEveryPointAndControlPoint() {
            final Shape shape = Shape.builder().moveTo(10, 10).quadTo(50, -20, 30, 30).build();
            assertEquals(Rect.ofEdges(10, -20, 50, 30), shape.bounds());
            assertEquals(Rect.ZERO, Shape.builder().build().bounds());
            assertTrue(Shape.builder().build().isEmpty());
        }

        @Test
        void aSegmentAfterCloseStartsAgainAtTheSubpathsStart() {
            final Shape shape = Shape.builder().moveTo(1, 1).lineTo(5, 1).close().lineTo(1, 5).build();
            assertEquals(List.of("M 1.0 1.0", "L 5.0 1.0", "Z", "M 1.0 1.0", "L 1.0 5.0"),
                    DrawingValuesTest.segments(shape));
        }

        @Test
        void closingTwiceClosesOnce() {
            final Shape shape = Shape.builder().close().moveTo(0, 0).lineTo(1, 0).close().close().build();
            assertEquals(List.of("M 0.0 0.0", "L 1.0 0.0", "Z"), DrawingValuesTest.segments(shape));
        }

        @Test
        void aSegmentNeedsAStartedSubpath() {
            assertThrows(IllegalStateException.class, () -> Shape.builder().lineTo(1, 1));
            assertThrows(IllegalStateException.class, () -> Shape.builder().quadTo(1, 1, 2, 2));
            assertThrows(IllegalStateException.class, () -> Shape.builder().cubicTo(1, 1, 2, 2, 3, 3));
        }

        @Test
        void aCoordinateThatIsNotFiniteThrowsAndChangesNothing() {
            final Shape.Builder builder = Shape.builder().moveTo(0, 0);
            assertThrows(IllegalArgumentException.class, () -> builder.lineTo(Float.NaN, 1));
            assertThrows(IllegalArgumentException.class, () -> builder.cubicTo(1, 1, 2, 2, 3, Float.NEGATIVE_INFINITY));
            assertThrows(IllegalArgumentException.class, () -> builder.arc(new Rect(0, 0, 4, 4), Float.NaN, 1f));
            final Shape shape = builder.lineTo(2, 2).build();
            assertEquals(List.of("M 0.0 0.0", "L 2.0 2.0"), DrawingValuesTest.segments(shape));
            assertEquals(Rect.ofEdges(0, 0, 2, 2), shape.bounds());
        }

        @Test
        void anArcIsCubicsOnItsEllipseThatTurnClockwise() {
            // A quarter turn from the right of a circle at 10, 10 with radius 10 ends at its bottom
            final Shape shape = Shape.builder().arc(new Rect(0, 0, 20, 20), 0f, (float) (Math.PI / 2)).build();
            final List<String> segments = DrawingValuesTest.segments(shape);
            assertEquals("M 20.0 10.0", segments.get(0));
            assertEquals(2, segments.size());
            assertTrue(segments.get(1).startsWith("C 10.0"), segments.get(1));
            assertEquals(Rect.ofEdges(10, 10, 20, 20), shape.bounds());
        }

        @Test
        void aFullCircleIsFourQuartersAndAnArcJoinsTheCurrentPoint() {
            final Shape circle = Shape.builder().arc(new Rect(0, 0, 20, 20), 0f, 100f).build();
            assertEquals(5, DrawingValuesTest.segments(circle).size(), "a sweep past a full turn draws one turn");
            assertEquals(0f, circle.bounds().x(), 1e-4f);
            assertEquals(0f, circle.bounds().y(), 1e-4f);
            assertEquals(20f, circle.bounds().right(), 1e-4f);
            assertEquals(20f, circle.bounds().bottom(), 1e-4f);
            final Shape joined = Shape.builder().moveTo(0, 0).arc(new Rect(0, 0, 20, 20), 0f, -1f).build();
            assertEquals("L 20.0 10.0", DrawingValuesTest.segments(joined).get(1));
        }

        @Test
        void aBuiltShapeDoesNotChangeWithItsBuilder() {
            final Shape.Builder builder = Shape.builder().moveTo(0, 0).lineTo(1, 1);
            final Shape first = builder.build();
            builder.lineTo(5, 5);
            assertEquals(2, DrawingValuesTest.segments(first).size());
            assertNotSame(first, builder.build());
            assertFalse(first.equals(builder.build()), "shapes are equal only to themselves");
        }
    }

    @Nested
    class PixelBuffers {
        @Test
        void aBufferStartsTransparentAtVersionZero() {
            final PixelBuffer buffer = PixelBuffer.of(3, 2);
            assertEquals(3, buffer.width());
            assertEquals(2, buffer.height());
            assertEquals(0L, buffer.version());
            assertArrayEquals(new int[6], buffer.array());
        }

        @Test
        void pixelsAreRowByRowInTheArray() {
            final PixelBuffer buffer = PixelBuffer.of(3, 2).set(2, 1, 0xFF112233);
            assertEquals(0xFF112233, buffer.array()[5]);
            assertEquals(0xFF112233, buffer.get(2, 1));
            buffer.array()[1] = 7;
            assertEquals(7, buffer.get(1, 0));
            assertSame(buffer.array(), buffer.array());
        }

        @Test
        void changedMovesTheVersion() {
            final PixelBuffer buffer = PixelBuffer.of(1, 1);
            assertSame(buffer, buffer.fill(1).changed());
            buffer.changed();
            assertEquals(2L, buffer.version());
            assertEquals(1, buffer.get(0, 0));
        }

        @Test
        void sizesAndPixelsAreChecked() {
            assertThrows(IllegalArgumentException.class, () -> PixelBuffer.of(0, 4));
            assertThrows(IllegalArgumentException.class, () -> PixelBuffer.of(4, -1));
            assertThrows(IllegalArgumentException.class, () -> PixelBuffer.of(1 << 15, 1 << 15));
            final PixelBuffer buffer = PixelBuffer.of(2, 2);
            assertThrows(IndexOutOfBoundsException.class, () -> buffer.get(2, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> buffer.set(0, -1, 0));
        }
    }
}
