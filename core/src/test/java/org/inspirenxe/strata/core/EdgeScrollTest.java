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
import org.inspirenxe.strata.api.control.collection.ListView;
import org.inspirenxe.strata.api.event.input.InputEvents;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.layout.AbsolutePanel;
import org.inspirenxe.strata.api.layout.ScrollPanel;
import org.inspirenxe.strata.api.transfer.DropEffect;
import org.inspirenxe.strata.api.transfer.Transfer;
import org.inspirenxe.strata.core.host.ScreenHostImpl;
import org.inspirenxe.strata.core.layout.EdgeScroll;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Scrolling while a drag that carries a transfer holds the pointer near an edge (design 4.48).
 */
class EdgeScrollTest {
    private final TestPlatform platform = new TestPlatform();
    private final TestHost test = new TestHost(this.platform, org.inspirenxe.strata.api.theme.Themes.flat(), 300, 300);
    private final ComponentFactory ui = this.test.factory();
    private final ScreenHostImpl host = this.test.host();
    // Drags start here and carry text
    private final Box source = new Box(this.ui, 20, 20);
    private final ScrollPanel scroll = this.ui.scrollPanel().add(new Box(this.ui, 100, 1000));
    private final AbsolutePanel panel = this.ui.absolutePanel().add(this.source, 0, 0).add(this.scroll, 100, 100);

    EdgeScrollTest() {
        this.scroll.setSize(100, 100);
        this.panel.setSize(300, 300);
        this.source.onEvent(InputEvents.DRAG_START, event -> event.startTransfer(Transfer.text("x"),
                EnumSet.of(DropEffect.MOVE)));
        this.test.show(this.panel);
    }

    private void dragTo(final float x, final float y) {
        this.host.pointerMoved(5, 5, Modifiers.NONE);
        this.host.pointerButton(5, 5, MouseButton.LEFT, true, Modifiers.NONE);
        this.host.pointerMoved(x, y, Modifiers.NONE);
    }

    private void frame() {
        this.platform.advanceMillis(16);
        this.host.render(new NullCanvas());
    }

    @Test
    void theStepGrowsTowardTheEdgeUpToALine() {
        assertEquals(10f, EdgeScroll.step(199.5f, 100, 200, 10, 0, 50), 0.5f);
        assertEquals(5f, EdgeScroll.step(192, 100, 200, 10, 0, 50), 0.01f);
        assertEquals(1.25f, EdgeScroll.step(185f, 100, 200, 10, 0, 50), 0.01f);
        assertEquals(0f, EdgeScroll.step(150, 100, 200, 10, 0, 50));
        assertEquals(-5f, EdgeScroll.step(108, 100, 200, 10, 20, 50), 0.01f);
        assertEquals(0f, EdgeScroll.step(108, 100, 200, 10, 0, 50), "nothing to scroll above");
        assertEquals(0f, EdgeScroll.step(192, 100, 200, 10, 50, 50), "nothing to scroll below");
        assertEquals(0f, EdgeScroll.step(201, 100, 200, 10, 0, 50), "outside the viewport");
    }

    @Test
    void aPanelScrollsEachFrameWhileThePointerStaysNearItsEdge() {
        this.dragTo(150, 196);
        this.frame();
        final float first = this.scroll.scrollY();
        assertEquals(7.5f, first, 0.01f);
        final long due = this.host.nextFrameNanos();
        assertTrue(due <= this.platform.clock().nanoTime() + 16_000_000L, "a frame is asked for while still");
        this.frame();
        this.frame();
        assertEquals(first * 3, this.scroll.scrollY(), 0.01f);

        this.host.pointerMoved(150, 150, Modifiers.NONE);
        this.frame();
        assertEquals(first * 3, this.scroll.scrollY(), 0.01f);
        assertEquals(Long.MAX_VALUE, this.host.nextFrameNanos());
    }

    @Test
    void theTopEdgeScrollsBack() {
        this.scroll.setScroll(0, 50);
        this.host.layout();
        this.dragTo(150, 101);
        this.frame();
        assertTrue(this.scroll.scrollY() < 50f);
    }

    @Test
    void aPlainDragOrAPanelThatSaysNoDoesNotScroll() {
        final Box plain = new Box(this.ui, 20, 20);
        this.panel.add(plain, 40, 0);
        this.host.layout();
        this.host.pointerMoved(45, 5, Modifiers.NONE);
        this.host.pointerButton(45, 5, MouseButton.LEFT, true, Modifiers.NONE);
        this.host.pointerMoved(150, 196, Modifiers.NONE);
        this.frame();
        assertEquals(0f, this.scroll.scrollY());
        this.host.pointerButton(150, 196, MouseButton.LEFT, false, Modifiers.NONE);

        this.scroll.setDragScroll(false);
        this.dragTo(150, 196);
        this.frame();
        assertEquals(0f, this.scroll.scrollY());
    }

    @Test
    void anInnerPanelAtItsEndLetsTheOuterOneScroll() {
        final ScrollPanel inner = this.ui.scrollPanel().add(new Box(this.ui, 50, 60));
        inner.setSize(50, 50);
        final AbsolutePanel content = this.ui.absolutePanel().add(inner, 0, 40);
        content.setSize(100, 1000);
        final ScrollPanel outer = this.ui.scrollPanel().add(content);
        outer.setSize(100, 100);
        this.panel.remove(this.scroll);
        this.panel.add(outer, 100, 100);
        this.host.layout();

        this.dragTo(120, 188);
        this.frame();
        assertTrue(inner.scrollY() > 0f);
        assertEquals(0f, outer.scrollY());
        for (int i = 0; i < 10; i++) {
            this.frame();
        }
        assertEquals(inner.maxScrollY(), inner.scrollY());
        assertTrue(outer.scrollY() > 0f, "the outer panel takes over once the inner one is at its end");
    }

    @Test
    void aListScrollsByRows() {
        final ListView<String> list = this.ui.<String>listView()
                .setItems(IntStream.range(0, 100).mapToObj(i -> "row " + i).toList());
        list.setSize(100, 100);
        this.panel.remove(this.scroll);
        this.panel.add(list, 100, 100);
        this.host.layout();

        this.dragTo(150, 199);
        this.frame();
        assertTrue(list.scrollY() > 0f);
        assertTrue(list.scrollY() <= list.rowHeight() + list.rowSpacing());

        list.setDragScroll(false);
        final float at = list.scrollY();
        this.frame();
        assertEquals(at, list.scrollY());
    }
}
