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
import org.inspirenxe.strata.api.event.cause.CauseKeys;
import org.inspirenxe.strata.api.event.input.InputEvents;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.input.Modifier;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.layout.AbsolutePanel;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.layout.VerticalAlignment;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Presses whose release went missing or was used up, drags, and hover when the tree changes under a still pointer.
 */
class PointerEdgeCaseTest {
    private final TestHost test = new TestHost(300, 300);
    private final ComponentFactory ui = this.test.factory();

    @Test
    void aSecondPressOfAHeldButtonEndsTheFirstPress() {
        final Box first = new Box(this.ui, 50, 50);
        final Box second = new Box(this.ui, 50, 50);
        this.test.show(this.ui.absolutePanel().add(first, 0, 0).add(second, 100, 0));
        final List<String> heard = new ArrayList<>();
        first.onEvent(InputEvents.POINTER_RELEASE, event -> heard.add("release"));
        first.onEvent(InputEvents.CLICK, event -> heard.add("click"));

        this.test.host().pointerButton(10, 10, MouseButton.LEFT, true, Modifiers.NONE);
        // The release went elsewhere and the engine never told us
        this.test.host().pointerButton(110, 10, MouseButton.LEFT, true, Modifiers.NONE);

        assertFalse(first.pressed());
        assertTrue(second.pressed());
        assertEquals(List.of("release"), heard);
    }

    @Test
    void disablingAnAncestorDuringAPressStopsTheClick() {
        final Box box = new Box(this.ui, 50, 50);
        final AbsolutePanel inner = this.ui.absolutePanel().add(box, 0, 0);
        this.test.show(this.ui.absolutePanel().add(inner, 0, 0));
        final List<String> heard = new ArrayList<>();
        box.onEvent(InputEvents.CLICK, event -> heard.add("click"));

        this.test.host().pointerButton(10, 10, MouseButton.LEFT, true, Modifiers.NONE);
        inner.setEnabled(false);
        this.test.host().pointerButton(10, 10, MouseButton.LEFT, false, Modifiers.NONE);

        assertEquals(List.of(), heard);
    }

    @Test
    void leavingTheHostReportsTheHeldModifiers() {
        final Box box = new Box(this.ui, 50, 50);
        this.test.show(this.ui.absolutePanel().add(box, 0, 0));
        final List<Modifiers> heard = new ArrayList<>();
        box.onEvent(InputEvents.POINTER_EXIT, event -> heard.add(event.modifiers()));
        final Modifiers shift = Modifiers.NONE.with(Modifier.SHIFT);

        this.test.host().pointerMoved(10, 10, shift);
        this.test.host().pointerLeft();

        assertEquals(List.of(shift), heard);
    }

    @Test
    void thePressThatClosesAPopupUsesUpItsReleaseToo() {
        this.test.show(this.ui.absolutePanel());
        this.ui.overlays().popup(new Box(this.ui, 20, 20), new Rect(0, 0, 10, 10));

        assertTrue(this.test.host().pointerButton(250, 250, MouseButton.LEFT, true, Modifiers.NONE));
        assertTrue(this.test.host().pointerButton(250, 250, MouseButton.LEFT, false, Modifiers.NONE));
        // Only once: a later release with no press is not Strata's
        assertFalse(this.test.host().pointerButton(250, 250, MouseButton.LEFT, false, Modifiers.NONE));
    }

    @Test
    void dragsCarryTheButtonOfTheirPress() {
        final Box box = new Box(this.ui, 50, 50);
        this.test.show(this.ui.absolutePanel().add(box, 0, 0));
        final List<Optional<MouseButton>> heard = new ArrayList<>();
        box.onEvent(InputEvents.DRAG_START, event -> heard.add(event.cause().context(CauseKeys.BUTTON)));
        box.onEvent(InputEvents.DRAG, event -> heard.add(event.cause().context(CauseKeys.BUTTON)));

        this.test.host().pointerButton(10, 10, MouseButton.RIGHT, true, Modifiers.NONE);
        this.test.host().pointerMoved(30, 30, Modifiers.NONE);

        assertEquals(List.of(Optional.of(MouseButton.RIGHT), Optional.of(MouseButton.RIGHT)), heard);
    }

    @Test
    void theNextFrameHoversWhatMovedUnderAStillPointer() {
        final Box old = new Box(this.ui, 50, 50);
        final AbsolutePanel panel = this.ui.absolutePanel().add(old, 0, 0);
        this.test.show(panel);
        this.test.host().pointerMoved(10, 10, Modifiers.NONE);
        final Box replacement = new Box(this.ui, 50, 50);
        final List<String> heard = new ArrayList<>();
        replacement.onEvent(InputEvents.POINTER_ENTER, event -> heard.add("enter"));

        panel.remove(old);
        panel.add(replacement, 0, 0);
        this.test.host().render(new NullCanvas());

        assertEquals(List.of("enter"), heard);
    }

    @Test
    void aContainerWithEmptyBoundsHidesItsChildrenFromThePointerAsFromPainting() {
        final Box child = new Box(this.ui, 20, 20);
        final AbsolutePanel squeezed = this.ui.absolutePanel().add(child, 0, 0)
                .setClipChildren(false);
        squeezed.setWidth(0).setHeight(0);
        final AbsolutePanel outer = this.ui.absolutePanel().add(squeezed, 0, 0);
        outer.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.STRETCH);
        this.test.show(outer);
        assertTrue(squeezed.bounds().isEmpty());
        assertFalse(child.bounds().isEmpty());

        this.test.host().pointerMoved(5, 5, Modifiers.NONE);

        assertSame(outer, this.test.host().hovered().orElse(null));
    }
}
