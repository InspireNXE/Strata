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
        void aStretchedNineSliceDrawsNineParts() {
            final Recording canvas = new Recording();
            // A 10 by 10 image with 2-unit borders over 30 by 20
            Paint.nineSlice(SPRITE, new Size(10, 10), Insets.of(2))
                    .paint(new CanvasPainter().begin(canvas), new Rect(0, 0, 30, 20));
            assertEquals(9, canvas.regions.size());
            // The top left corner keeps its size and shows the image's first fifth each way
            canvas.regions.get(0).check(new Rect(0, 0, 2, 2), 0, 0, 0.2f, 0.2f);
            // The top edge stretches the middle of the image's top border over 26 units
            canvas.regions.get(1).check(new Rect(2, 0, 26, 2), 0.2f, 0, 0.8f, 0.2f);
            canvas.regions.get(8).check(new Rect(28, 18, 2, 2), 0.8f, 0.8f, 1, 1);
        }

        @Test
        void aLargerImageDrawsTheSameBorders() {
            final Recording canvas = new Recording();
            Paint.nineSlice(SPRITE, new Size(200, 20), Insets.of(3))
                    .paint(new CanvasPainter().begin(canvas), new Rect(0, 0, 100, 20));
            // The corner is 3 units, whatever the image's pixels
            assertEquals(new Rect(0, 0, 3, 3), canvas.regions.get(0).rect());
            assertEquals(new Rect(97, 17, 3, 3), canvas.regions.get(8).rect());
        }

        @Test
        void aTiledNineSliceRepeatsItsEdgesAndCropsTheLast() {
            final Recording canvas = new Recording();
            // 4 by 4 image, 1-unit borders: edges and center are 2 units long; the rectangle's middle is 5 by 2
            Paint.tiledNineSlice(SPRITE, new Size(4, 4), Insets.of(1))
                    .paint(new CanvasPainter().begin(canvas), new Rect(0, 0, 7, 4));
            final List<Region> topEdge = canvas.regions.stream()
                    .filter(region -> region.rect().y() == 0 && region.rect().x() >= 1 && region.rect().right() <= 6)
                    .toList();
            assertEquals(3, topEdge.size());
            topEdge.get(0).check(new Rect(1, 0, 2, 1), 0.25f, 0, 0.75f, 0.25f);
            topEdge.get(1).check(new Rect(3, 0, 2, 1), 0.25f, 0, 0.75f, 0.25f);
            // The last copy is cropped to one unit, and shows the first half of the edge
            topEdge.get(2).check(new Rect(5, 0, 1, 1), 0.25f, 0, 0.5f, 0.25f);
            // 4 corners, 3 copies on each of the top and bottom edges, 1 on each side and 3 in the center
            assertEquals(4 + 3 + 3 + 1 + 1 + 3, canvas.regions.size());
        }

        @Test
        void bordersShrinkInASmallRectangle() {
            final Recording canvas = new Recording();
            Paint.nineSlice(SPRITE, new Size(10, 10), Insets.of(4))
                    .paint(new CanvasPainter().begin(canvas), new Rect(0, 0, 4, 4));
            canvas.regions.get(0).check(new Rect(0, 0, 2, 2), 0, 0, 0.4f, 0.4f);
        }

        @Test
        void aTiledSpriteRepeatsAndCropsTheLastRowAndColumn() {
            final Recording canvas = new Recording();
            Paint.tiled(SPRITE, new Size(16, 16)).paint(new CanvasPainter().begin(canvas), new Rect(10, 10, 40, 20));
            assertEquals(6, canvas.regions.size());
            canvas.regions.get(0).check(new Rect(10, 10, 16, 16), 0, 0, 1, 1);
            canvas.regions.get(1).check(new Rect(26, 10, 16, 16), 0, 0, 1, 1);
            canvas.regions.get(2).check(new Rect(42, 10, 8, 16), 0, 0, 0.5f, 1);
            canvas.regions.get(3).check(new Rect(10, 26, 16, 4), 0, 0, 1, 0.25f);
            canvas.regions.get(5).check(new Rect(42, 26, 8, 4), 0, 0, 0.5f, 0.25f);
        }

        @Test
        void tooManyCopiesStretchInstead() {
            final Recording canvas = new Recording();
            Paint.tiled(SPRITE, new Size(0.001f, 0.001f)).paint(new CanvasPainter().begin(canvas), new Rect(0, 0, 10, 10));
            assertEquals(List.of("sprite " + new Rect(0, 0, 10, 10)), canvas.calls);
        }

        @Test
        void theDefaultRegionClipsTheWholeSprite() {
            final Recording canvas = new Recording();
            canvas.regionsFallBack = true;
            canvas.spriteRegion(SPRITE, new Rect(2, 0, 26, 2), 0.2f, 0, 0.8f, 0.2f, -1);
            final Drawn top = canvas.sprites.get(0);
            assertEquals(new Rect(2, 0, 26, 2), top.clip());
            assertEquals(2 - 2 * 26f / 6f, top.image().x(), 1e-4);
            assertEquals(10 * 26f / 6f, top.image().width(), 1e-4);
            assertEquals(0, canvas.depth);
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

    /**
     * A part of a sprite drawn over {@code rect}.
     */
    private record Region(Rect rect, float u0, float v0, float u1, float v1) {
        void check(final Rect expected, final float eu0, final float ev0, final float eu1, final float ev1) {
            assertEquals(expected, this.rect);
            assertEquals(eu0, this.u0, 1e-4);
            assertEquals(ev0, this.v0, 1e-4);
            assertEquals(eu1, this.u1, 1e-4);
            assertEquals(ev1, this.v1, 1e-4);
        }
    }

    private static final class Recording implements Canvas {
        private final List<String> calls = new ArrayList<>();
        private final List<Drawn> sprites = new ArrayList<>();
        private final List<Region> regions = new ArrayList<>();
        private boolean regionsFallBack;
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
        public void spriteRegion(final SpriteId sprite, final Rect rect, final float u0, final float v0,
                                 final float u1, final float v1, final int tint) {
            if (this.regionsFallBack) {
                Canvas.super.spriteRegion(sprite, rect, u0, v0, u1, v1, tint);
            } else {
                this.regions.add(new Region(rect, u0, v0, u1, v1));
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
