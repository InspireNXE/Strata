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
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.visual.Visual;
import org.inspirenxe.strata.core.paint.CanvasPainter;
import org.inspirenxe.strata.spi.Canvas;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlicedPaintTest {
    private static final SpriteId SPRITE = SpriteId.of("a", "b");

    @Nested
    class Records {
        @Test
        void tiledNeedsASize() {
            assertThrows(IllegalArgumentException.class, () -> new Paint.NineSlice(SPRITE, Insets.of(1), -1, null, true));
        }

        @Test
        void bordersMustFit() {
            assertThrows(IllegalArgumentException.class, () -> Paint.nineSlice(SPRITE, new Size(4, 20), Insets.of(3)));
            assertThrows(IllegalArgumentException.class, () -> Paint.tiled(SPRITE, new Size(0, 2)));
        }

        @Test
        void theOldConstructorHasNoSize() {
            final Paint.NineSlice slice = new Paint.NineSlice(SPRITE, Insets.of(3), -1);
            assertEquals(null, slice.size());
            assertEquals(false, slice.tiled());
        }
    }

    @Nested
    class OnACanvasWithoutSlicing {
        @Test
        void anUnsizedNineSliceGoesToTheCanvas() {
            final Recording canvas = new Recording();
            Paint.nineSlice(SPRITE, Insets.of(3)).paint(new CanvasPainter().begin(canvas), new Rect(0, 0, 50, 20));
            assertEquals(List.of("nineSlice " + new Rect(0, 0, 50, 20)), canvas.calls);
        }

        @Test
        void aStretchedNineSliceDrawsNineClippedParts() {
            final Recording canvas = new Recording();
            // A 10 by 10 image with 2-unit borders over 30 by 20
            Paint.nineSlice(SPRITE, new Size(10, 10), Insets.of(2))
                    .paint(new CanvasPainter().begin(canvas), new Rect(0, 0, 30, 20));
            assertEquals(9, canvas.sprites.size());
            // The top left corner keeps its size: the whole image at its own size, clipped to 2 by 2
            assertEquals(new Drawn(new Rect(0, 0, 10, 10), new Rect(0, 0, 2, 2)), canvas.sprites.get(0));
            // The top edge stretches 6 units of image over 26: the image is drawn 26 / 6 times as wide
            final Drawn top = canvas.sprites.get(1);
            assertEquals(new Rect(2, 0, 26, 2), top.clip());
            assertEquals(2 - 2 * 26f / 6f, top.image().x(), 1e-4);
            assertEquals(10 * 26f / 6f, top.image().width(), 1e-4);
            // The bottom right corner sits at the far corner
            assertEquals(new Drawn(new Rect(20, 10, 10, 10), new Rect(28, 18, 2, 2)), canvas.sprites.get(8));
            assertEquals(0, canvas.depth);
        }

        @Test
        void aLargerImageDrawsTheSameBorders() {
            final Recording canvas = new Recording();
            Paint.nineSlice(SPRITE, new Size(200, 20), Insets.of(3))
                    .paint(new CanvasPainter().begin(canvas), new Rect(0, 0, 100, 20));
            // The corner is 3 units, whatever the image's pixels
            assertEquals(new Rect(0, 0, 3, 3), canvas.sprites.get(0).clip());
            assertEquals(new Rect(97, 17, 3, 3), canvas.sprites.get(8).clip());
        }

        @Test
        void aTiledNineSliceRepeatsItsEdgesAndCropsTheLast() {
            final Recording canvas = new Recording();
            // 4 by 4 image, 1-unit borders: edges and center are 2 units long; the rectangle's middle is 5 by 2
            Paint.tiledNineSlice(SPRITE, new Size(4, 4), Insets.of(1))
                    .paint(new CanvasPainter().begin(canvas), new Rect(0, 0, 7, 4));
            final List<Rect> topEdge = canvas.sprites.stream()
                    .map(Drawn::clip)
                    .filter(clip -> clip.y() == 0 && clip.x() >= 1 && clip.right() <= 6)
                    .toList();
            assertEquals(List.of(new Rect(1, 0, 2, 1), new Rect(3, 0, 2, 1), new Rect(5, 0, 1, 1)), topEdge);
            // Each copy is the image at its own size, shifted so its middle lands on the copy
            assertTrue(canvas.sprites.contains(new Drawn(new Rect(2, 0, 4, 4), new Rect(3, 0, 2, 1))));
            // 4 corners, 3 copies on each of the top and bottom edges, 1 on each side and 3 in the center
            assertEquals(4 + 3 + 3 + 1 + 1 + 3, canvas.sprites.size());
        }

        @Test
        void bordersShrinkInASmallRectangle() {
            final Recording canvas = new Recording();
            Paint.nineSlice(SPRITE, new Size(10, 10), Insets.of(4))
                    .paint(new CanvasPainter().begin(canvas), new Rect(0, 0, 4, 4));
            assertEquals(new Rect(0, 0, 2, 2), canvas.sprites.get(0).clip());
            assertEquals(new Rect(0, 0, 5, 5), canvas.sprites.get(0).image());
        }

        @Test
        void aTiledSpriteRepeatsUnderOneClip() {
            final Recording canvas = new Recording();
            Paint.tiled(SPRITE, new Size(16, 16)).paint(new CanvasPainter().begin(canvas), new Rect(10, 10, 40, 20));
            assertEquals(List.of(
                    new Drawn(new Rect(10, 10, 16, 16), new Rect(10, 10, 40, 20)),
                    new Drawn(new Rect(26, 10, 16, 16), new Rect(10, 10, 40, 20)),
                    new Drawn(new Rect(42, 10, 16, 16), new Rect(10, 10, 40, 20)),
                    new Drawn(new Rect(10, 26, 16, 16), new Rect(10, 10, 40, 20)),
                    new Drawn(new Rect(26, 26, 16, 16), new Rect(10, 10, 40, 20)),
                    new Drawn(new Rect(42, 26, 16, 16), new Rect(10, 10, 40, 20))), canvas.sprites);
            assertEquals(0, canvas.depth);
        }

        @Test
        void tooManyCopiesStretchInstead() {
            final Recording canvas = new Recording();
            Paint.tiled(SPRITE, new Size(0.001f, 0.001f)).paint(new CanvasPainter().begin(canvas), new Rect(0, 0, 10, 10));
            assertEquals(List.of("sprite " + new Rect(0, 0, 10, 10)), canvas.calls);
        }
    }

    @Nested
    class OnACanvasThatSlices {
        @Test
        void sizedAndTiledPaintsPassThrough() {
            final Recording canvas = new Recording();
            final CanvasPainter painter = new CanvasPainter().begin(canvas, true);
            Paint.tiledNineSlice(SPRITE, new Size(200, 20), Insets.of(3)).paint(painter, new Rect(0, 0, 50, 20));
            Paint.tiled(SPRITE, new Size(16, 16)).paint(painter, new Rect(0, 0, 50, 20));
            assertEquals(List.of("nineSlice " + new Rect(0, 0, 50, 20), "sprite " + new Rect(0, 0, 50, 20)),
                    canvas.calls);
        }
    }

    /**
     * A sprite drawn over {@code image}, seen through {@code clip}, the innermost clip at the time.
     */
    private record Drawn(Rect image, Rect clip) {
    }

    private static final class Recording implements Canvas {
        private final List<String> calls = new ArrayList<>();
        private final List<Drawn> sprites = new ArrayList<>();
        private final Deque<Rect> clips = new ArrayDeque<>();
        private int depth;

        @Override
        public void fill(final Rect rect, final int argb) {
        }

        @Override
        public void fillGradient(final Rect rect, final int topArgb, final int bottomArgb) {
        }

        @Override
        public void sprite(final SpriteId sprite, final Rect rect, final int tint) {
            if (this.clips.isEmpty()) {
                this.calls.add("sprite " + rect);
            } else {
                this.sprites.add(new Drawn(rect, this.clips.peek()));
            }
        }

        @Override
        public void nineSlice(final SpriteId sprite, final Rect rect, final Insets border, final int tint) {
            this.calls.add("nineSlice " + rect);
        }

        @Override
        public void text(final TextSpan span, final float x, final float y) {
        }

        @Override
        public void pushClip(final Rect rect) {
            this.clips.push(rect);
            this.depth++;
        }

        @Override
        public void popClip() {
            this.clips.pop();
            this.depth--;
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
        }

        @Override
        public void nextLayer() {
        }

        @Override
        public void requestCursor(final CursorKind cursor) {
        }
    }
}
