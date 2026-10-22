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

import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.core.paint.CanvasPainter;
import org.inspirenxe.strata.spi.Canvas;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A tile small enough to fit under the limit on each axis must still not flood the canvas with calls.
 */
class TileLimitTest {
    private static final SpriteId SPRITE = SpriteId.of("a", "b");

    private final Map<String, Integer> calls = new HashMap<>();
    // Every canvas call does nothing and is counted by name
    private final Canvas canvas = (Canvas) Proxy.newProxyInstance(Canvas.class.getClassLoader(),
            new Class<?>[] {Canvas.class}, (proxy, method, args) -> {
                this.calls.merge(method.getName(), 1, Integer::sum);
                return null;
            });

    @Test
    void aTiledSpriteWithTooManyCopiesInAllIsStretched() {
        // 1000 by 1000 copies: under the limit on each axis, a million in all
        Paint.tiled(SPRITE, new Size(1, 1)).paint(new CanvasPainter().begin(this.canvas), new Rect(0, 0, 1000, 1000));

        assertEquals(Map.of("sprite", 1), this.calls);
    }

    @Test
    void aTiledCenterWithTooManyCopiesInAllIsStretched() {
        // A 3 by 3 image with 1-unit borders has a 1 by 1 center, tiled about a million times over this rectangle
        Paint.tiledNineSlice(SPRITE, new Size(3, 3), Insets.of(1))
                .paint(new CanvasPainter().begin(this.canvas), new Rect(0, 0, 1002, 1002));

        final int regions = this.calls.getOrDefault("spriteRegion", 0);
        assertTrue(regions < 5000, "the nine-slice made " + regions + " region calls");
    }
}
