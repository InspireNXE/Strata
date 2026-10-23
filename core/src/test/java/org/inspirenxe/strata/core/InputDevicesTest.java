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
import org.inspirenxe.strata.api.control.menu.Menu;
import org.inspirenxe.strata.api.event.cause.CauseKeys;
import org.inspirenxe.strata.api.event.cause.Device;
import org.inspirenxe.strata.api.event.input.InputEvents;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.layout.AbsolutePanel;
import org.inspirenxe.strata.api.layout.ScrollPanel;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.core.host.ScreenHostImpl;
import org.inspirenxe.strata.core.node.Root;
import org.inspirenxe.strata.spi.InputContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Input from devices other than the mouse and the keyboard: touches, pens and controllers (design 4.54).
 */
class InputDevicesTest {
    private static final InputContext FIRST = InputContext.touch(0);
    private static final InputContext SECOND = InputContext.touch(7);

    private final TestHost test = new TestHost(300, 200);
    private final ComponentFactory ui = this.test.factory();
    private final ScreenHostImpl host = this.test.host();
    private final Box left = new Box(this.ui, 40, 40);
    private final Box right = new Box(this.ui, 40, 40);
    private final AbsolutePanel panel = this.ui.absolutePanel().add(this.left, 10, 10).add(this.right, 100, 10);
    private final List<String> log = new ArrayList<>();

    InputDevicesTest() {
        this.panel.setSize(300, 200);
        this.left.onEvent(InputEvents.CLICK, event -> this.log.add("click left " + event.pointerId()));
        this.right.onEvent(InputEvents.CLICK, event -> this.log.add("click right " + event.pointerId()));
        this.test.show(this.panel);
    }

    private void touch(final InputContext touch, final float x, final float y, final boolean down) {
        this.host.pointerButton(x, y, MouseButton.LEFT, down, Modifiers.NONE, touch);
    }

    @Test
    void contextsAreChecked() {
        assertThrows(IllegalArgumentException.class, () -> new InputContext(Device.TOUCH, -1, 1f));
        assertThrows(IllegalArgumentException.class, () -> new InputContext(Device.PEN, 0, 1.5f));
        assertThrows(IllegalArgumentException.class, () -> new InputContext(Device.PEN, 0, Float.NaN));
        assertEquals(new InputContext(Device.PEN, 0, 0.25f), InputContext.pen(0.25f));
    }

    @Test
    void theDeviceGoesIntoTheCauseAndPointerEventsCarryThePointer() {
        final List<String> seen = new ArrayList<>();
        this.left.onEvent(InputEvents.POINTER_PRESS, event -> seen.add(event.cause().context(CauseKeys.DEVICE)
                .orElseThrow() + " " + event.pointerId() + " " + event.pressure()));
        this.host.pointerButton(20, 20, MouseButton.LEFT, true, Modifiers.NONE, InputContext.pen(0.5f));
        this.host.pointerButton(20, 20, MouseButton.LEFT, false, Modifiers.NONE, InputContext.pen(0.5f));
        this.test.platform().advanceMillis(1000);
        this.host.pointerButton(20, 20, MouseButton.LEFT, true, Modifiers.NONE);
        this.host.pointerButton(20, 20, MouseButton.LEFT, false, Modifiers.NONE);

        assertEquals(List.of("PEN 0 0.5", "MOUSE 0 1.0"), seen);
    }

    @Test
    void aControllersButtonsArriveAsKeysFromTheController() {
        final List<Device> seen = new ArrayList<>();
        // With nothing focused, keys go to the screen's content
        this.panel.onEvent(InputEvents.KEY, event -> seen.add(event.cause().context(CauseKeys.DEVICE).orElseThrow()));
        this.host.key(Key.ENTER, KeyAction.PRESS, Modifiers.NONE, new InputContext(Device.CONTROLLER, 0, 1f));
        this.host.key(Key.ENTER, KeyAction.PRESS, Modifiers.NONE);
        assertEquals(List.of(Device.CONTROLLER, Device.KEYBOARD), seen);
    }

    @Test
    void twoFingersPressTwoButtonsAtOnce() {
        this.touch(FIRST, 20, 20, true);
        this.touch(SECOND, 110, 20, true);
        assertTrue(this.left.pressed());
        assertTrue(this.right.pressed());
        this.touch(SECOND, 110, 20, false);
        this.touch(FIRST, 20, 20, false);

        assertEquals(List.of("click right 7", "click left 0"), this.log);
    }

    @Test
    void aTouchHoversNothing() {
        this.left.setTooltip("Left");
        this.host.pointerMoved(20, 20, Modifiers.NONE, FIRST);
        this.touch(FIRST, 20, 20, true);
        this.host.render(new NullCanvas());
        assertEquals(Optional.empty(), this.host.hovered());
        assertEquals(Optional.empty(), this.host.root().tooltip());
        this.touch(FIRST, 20, 20, false);
        assertEquals(List.of("click left 0"), this.log);

        this.host.pointerMoved(20, 20, Modifiers.NONE, InputContext.pen(1f));
        assertTrue(this.host.hovered().isPresent(), "a pen hovers");
    }

    @Test
    void aLongPressOpensTheContextMenuAndClicksNothing() {
        final Menu menu = this.ui.menu().addItem(Text.of("Copy"), () -> { });
        this.panel.setContextMenu(menu);
        // At most a millisecond between frames, so the long press's own time decides when it is due
        this.host.setFrameRate(1000);
        this.touch(FIRST, 20, 20, true);
        this.test.platform().advanceMillis(Root.LONG_PRESS_MILLIS - 1);
        this.host.render(new NullCanvas());
        assertFalse(menu.isOpen());
        assertTrue(this.host.nextFrameNanos() <= this.test.platform().clock().nanoTime() + 1_000_000L,
                "a frame is due when the long press is");
        this.test.platform().advanceMillis(1);
        this.host.render(new NullCanvas());
        assertTrue(menu.isOpen());
        this.touch(FIRST, 20, 20, false);
        assertEquals(List.of(), this.log);
    }

    @Test
    void aTouchThatMovesFirstOpensNoMenu() {
        final Menu menu = this.ui.menu().addItem(Text.of("Copy"), () -> { });
        this.panel.setContextMenu(menu);
        this.touch(FIRST, 20, 20, true);
        this.host.pointerMoved(40, 40, Modifiers.NONE, FIRST);
        this.test.platform().advanceMillis(Root.LONG_PRESS_MILLIS);
        this.host.render(new NullCanvas());
        assertFalse(menu.isOpen());
    }

    @Test
    void twoTouchesThatMoveTogetherScroll() {
        final ScrollPanel scroll = this.ui.scrollPanel().add(new Box(this.ui, 100, 1000));
        scroll.setSize(100, 100);
        this.panel.add(scroll, 150, 50);
        this.host.layout();

        this.touch(FIRST, 170, 120, true);
        this.touch(SECOND, 190, 120, true);
        this.host.pointerMoved(170, 100, Modifiers.NONE, FIRST);
        this.host.pointerMoved(190, 100, Modifiers.NONE, SECOND);
        this.host.layout();

        // The midpoint moved up 20 units, two precise notches toward the end
        assertEquals(20f, scroll.scrollY(), 0.01f);
        this.touch(FIRST, 170, 100, false);
        this.touch(SECOND, 190, 100, false);
        assertEquals(List.of(), this.log);

        // Afterwards a single touch drags again rather than scrolling
        this.touch(FIRST, 170, 120, true);
        this.host.pointerMoved(170, 60, Modifiers.NONE, FIRST);
        this.touch(FIRST, 170, 60, false);
        assertEquals(20f, scroll.scrollY(), 0.01f);
    }
}
