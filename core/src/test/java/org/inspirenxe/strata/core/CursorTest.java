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

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.inspirenxe.strata.spi.Canvas;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cursor kinds and their fallbacks (design 4.54).
 */
class CursorTest {
    private final TestHost test = new TestHost(200, 100);
    private final ComponentFactory ui = this.test.factory();

    @Test
    void everyFallbackChainEndsAtTheArrow() {
        for (final CursorKind kind : CursorKind.values()) {
            final Set<CursorKind> seen = new HashSet<>();
            CursorKind next = kind;
            while (next != CursorKind.ARROW) {
                assertTrue(seen.add(next), "loop from " + kind);
                next = next.fallback();
            }
        }
        assertEquals(CursorKind.ARROW, CursorKind.ARROW.fallback());
        assertEquals(CursorKind.RESIZE_ALL, CursorKind.RESIZE_DIAGONAL_DOWN.fallback());
        assertEquals(CursorKind.RESIZE_ALL, CursorKind.RESIZE_DIAGONAL_UP.fallback());
        assertEquals(CursorKind.HAND, CursorKind.GRAB.fallback());
        assertEquals(CursorKind.HAND, CursorKind.GRABBING.fallback());
        assertEquals(CursorKind.NOT_ALLOWED, CursorKind.NO_DROP.fallback());
        assertEquals(CursorKind.ARROW, CursorKind.COPY.fallback());
        assertEquals(CursorKind.ARROW, CursorKind.WAIT.fallback());
    }

    @Test
    void aCanvasShowsTheEightOldKindsByDefault() {
        final Canvas canvas = new NullCanvas();
        final Set<CursorKind> old = EnumSet.of(CursorKind.ARROW, CursorKind.TEXT, CursorKind.HAND,
                CursorKind.CROSSHAIR, CursorKind.RESIZE_HORIZONTAL, CursorKind.RESIZE_VERTICAL, CursorKind.RESIZE_ALL,
                CursorKind.NOT_ALLOWED);
        for (final CursorKind kind : CursorKind.values()) {
            assertEquals(old.contains(kind), canvas.showsCursor(kind), kind.name());
        }
    }

    private CursorKind shown(final CursorKind set, final NullCanvas canvas) {
        final StackPanel panel = this.ui.stackPanel();
        panel.setCursor(set).setSize(200, 100);
        this.test.show(panel);
        this.test.host().pointerMoved(50, 50, Modifiers.NONE);
        this.test.host().render(canvas);
        return canvas.cursor();
    }

    @Test
    void theHostAsksForTheFallbackOfAKindTheCanvasCannotShow() {
        assertEquals(CursorKind.HAND, this.shown(CursorKind.GRABBING, new NullCanvas()));
    }

    @Test
    void theHostWalksTheChainUntilTheCanvasShowsAKind() {
        final NullCanvas canvas = new NullCanvas().showCursors(EnumSet.of(CursorKind.ARROW));
        assertEquals(CursorKind.ARROW, this.shown(CursorKind.NO_DROP, canvas));
    }

    @Test
    void aCanvasThatShowsAKindGetsItAsItIs() {
        final NullCanvas canvas = new NullCanvas().showCursors(EnumSet.allOf(CursorKind.class));
        assertEquals(CursorKind.COPY, this.shown(CursorKind.COPY, canvas));
        assertFalse(new NullCanvas().showsCursor(CursorKind.COPY));
    }
}
