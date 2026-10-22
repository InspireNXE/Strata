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
import org.inspirenxe.strata.api.control.window.Window;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.core.host.ScreenHostImpl;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Resizing a window by its grip and edges. The window starts 100 by 80, centered in a 300 by 300 host at (100, 110),
 * with a stretched panel inside, so presses on the grip land on the panel first. Its title bar is 15 tall and its
 * empty title asks for 34 units of width.
 */
class WindowResizeTest {
    private final TestHost test = new TestHost(300, 300);
    private final ComponentFactory ui = this.test.factory();
    private final List<Size> requests = new ArrayList<>();
    private final List<String> heard = new ArrayList<>();
    private final Window window = this.ui.window()
            .setResizable(true)
            .add(this.ui.anchorPanel().setAnchor(Anchor.FILL))
            .setSize(100, 80);

    WindowResizeTest() {
        this.test.show(this.ui.anchorPanel().add(this.window).setAnchor(Anchor.FILL));
        // Registered on the screen's root, so the events are heard around the window
        this.window.parent().orElseThrow()
                .onEvent(Window.RESIZE, event -> this.requests.add(event.value()))
                .onEvent(Window.RESIZED, event -> this.heard.add(text(event.previous()) + "->" + text(event.value())));
    }

    private static String text(final Size size) {
        return (int) size.width() + "x" + (int) size.height();
    }

    private void move(final float x, final float y) {
        this.test.host().pointerMoved(x, y, Modifiers.NONE);
    }

    private void drag(final float fromX, final float fromY, final float toX, final float toY) {
        final ScreenHostImpl host = this.test.host();
        host.pointerMoved(fromX, fromY, Modifiers.NONE);
        host.pointerButton(fromX, fromY, MouseButton.LEFT, true, Modifiers.NONE);
        host.pointerMoved(toX, toY, Modifiers.NONE);
        host.pointerButton(toX, toY, MouseButton.LEFT, false, Modifiers.NONE);
        host.layout();
    }

    @Test
    void draggingTheGripResizesWithTheTopLeftCornerInPlace() {
        assertEquals(new Rect(100, 110, 100, 80), this.window.bounds());
        this.drag(195, 185, 215, 195);

        assertEquals(new Rect(100, 110, 120, 90), this.window.bounds());
        assertEquals(List.of(new Size(120, 90)), this.requests);
        assertEquals(List.of("100x80->120x90"), this.heard);
    }

    @Test
    void eachMoveOfOneDragAsksAgainFromThePress() {
        final ScreenHostImpl host = this.test.host();
        host.pointerMoved(195, 185, Modifiers.NONE);
        host.pointerButton(195, 185, MouseButton.LEFT, true, Modifiers.NONE);
        host.pointerMoved(205, 185, Modifiers.NONE);
        host.layout();
        host.pointerMoved(225, 175, Modifiers.NONE);
        host.pointerButton(225, 175, MouseButton.LEFT, false, Modifiers.NONE);
        host.layout();

        assertEquals(new Rect(100, 110, 130, 70), this.window.bounds());
        assertEquals(List.of("100x80->110x80", "110x80->130x70"), this.heard);
    }

    @Test
    void cancelingKeepsTheSize() {
        this.window.onEvent(Window.RESIZE, event -> event.cancel());
        this.drag(195, 185, 215, 195);

        assertEquals(new Rect(100, 110, 100, 80), this.window.bounds());
        assertEquals(List.of(new Size(120, 90)), this.requests);
        assertEquals(List.of(), this.heard);
    }

    @Test
    void aRewrittenSizeIsUsedWithinTheLimits() {
        this.window.onEvent(Window.RESIZE, event -> event.setValue(new Size(150, 5)));
        this.drag(195, 185, 215, 195);

        // The height stops at the title bar plus the grip
        assertEquals(new Rect(100, 110, 150, 25), this.window.bounds());
        assertEquals(List.of("100x80->150x25"), this.heard);
    }

    @Test
    void aNullSizeIsIgnored() {
        this.window.onEvent(Window.RESIZE, event -> event.setValue(null));
        this.drag(195, 185, 215, 195);

        assertEquals(new Rect(100, 110, 100, 80), this.window.bounds());
        assertEquals(List.of(), this.heard);
    }

    @Test
    void theMinimumAndMaximumSizesHold() {
        this.window.setMinSize(90, 70).setMaxSize(130, 200);
        this.test.host().layout();
        this.drag(195, 185, 145, 135);
        assertEquals(new Rect(100, 110, 90, 70), this.window.bounds());

        this.drag(185, 175, 285, 275);
        assertEquals(new Rect(100, 110, 130, 170), this.window.bounds());
    }

    @Test
    void theWindowStaysInsideItsSlotAndAboveItsFloor() {
        this.drag(195, 185, 299, 299);
        assertEquals(new Rect(100, 110, 200, 190), this.window.bounds());

        this.drag(295, 295, 0, 0);
        assertEquals(new Rect(100, 110, 34, 25), this.window.bounds());
    }

    @Test
    void aWindowThatIsNotResizableIgnoresTheGrip() {
        this.window.setResizable(false);
        this.drag(195, 185, 215, 195);

        assertEquals(new Rect(100, 110, 100, 80), this.window.bounds());
        assertEquals(List.of(), this.requests);
    }

    @Test
    void edgesResizeOnlyWithEdgeResizing() {
        assertFalse(this.window.isEdgeResizable());
        this.drag(101, 150, 81, 150);
        assertEquals(new Rect(100, 110, 100, 80), this.window.bounds());

        this.window.setEdgeResizable(true);
        this.drag(101, 150, 81, 150);
        assertEquals(new Rect(80, 110, 120, 80), this.window.bounds());
        assertEquals(List.of("100x80->120x80"), this.heard);
    }

    @Test
    void theTopEdgeWinsOverMovingAndACornerResizesBothWays() {
        this.window.setEdgeResizable(true);
        this.drag(150, 111, 150, 101);
        assertEquals(new Rect(100, 100, 100, 90), this.window.bounds());

        this.drag(101, 101, 91, 91);
        assertEquals(new Rect(90, 90, 110, 100), this.window.bounds());
    }

    @Test
    void theCursorShowsWhatAPressWouldResize() {
        this.move(195, 185);
        assertEquals(CursorKind.RESIZE_ALL, this.window.cursor().orElseThrow());
        this.move(150, 150);
        assertEquals(Optional.empty(), this.window.cursor());

        this.window.setEdgeResizable(true);
        this.move(101, 150);
        assertEquals(CursorKind.RESIZE_HORIZONTAL, this.window.cursor().orElseThrow());
        this.move(150, 189);
        assertEquals(CursorKind.RESIZE_VERTICAL, this.window.cursor().orElseThrow());
        this.move(10, 10);
        this.move(150, 150);
        assertEquals(Optional.empty(), this.window.cursor());
    }

    @Test
    void codeSizesFireNothing() {
        this.window.setSize(200, 100);
        this.test.host().layout();

        assertTrue(this.window.isResizable());
        assertEquals(List.of(), this.requests);
        assertEquals(List.of(), this.heard);
    }

    @Test
    void aMovedWindowIsKeptInsideItsSlotWhenTheHostShrinks() {
        this.drag(150, 113, 350, 113);
        assertEquals(new Rect(200, 110, 100, 80), this.window.bounds());

        this.test.host().resize(220, 300);
        this.test.host().layout();

        assertEquals(new Rect(120, 110, 100, 80), this.window.bounds());
    }

    @Test
    void anOffsetSetFromCodeIsNotKeptInside() {
        this.window.setOffset(150, 0);
        this.test.host().layout();

        assertEquals(new Rect(250, 110, 100, 80), this.window.bounds());
    }
}
