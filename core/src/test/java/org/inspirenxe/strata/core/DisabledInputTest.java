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
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.event.input.InputEvents;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.input.ScrollKind;
import org.inspirenxe.strata.api.layout.AbsolutePanel;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.inspirenxe.strata.api.overlay.Overlay;
import org.inspirenxe.strata.spi.Canvas;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A disabled component takes the pointer so nothing behind it acts, shows its tooltip, and hears nothing itself.
 */
class DisabledInputTest {
    private final TestHost test = new TestHost(300, 300);
    private final ComponentFactory ui = this.test.factory();

    private void listen(final List<String> heard, final String name, final Component<?> component) {
        component.onEvent(InputEvents.POINTER_ENTER, event -> heard.add(name + " enter"));
        component.onEvent(InputEvents.POINTER_EXIT, event -> heard.add(name + " exit"));
        component.onEvent(InputEvents.POINTER_MOVE, event -> heard.add(name + " move"));
        component.onEvent(InputEvents.POINTER_PRESS, event -> heard.add(name + " press"));
        component.onEvent(InputEvents.POINTER_RELEASE, event -> heard.add(name + " release"));
        component.onEvent(InputEvents.CLICK, event -> heard.add(name + " click"));
    }

    @Nested
    class Presses {
        @Test
        void aClickOnADisabledChildGoesNoFurther() {
            final Box box = new Box(DisabledInputTest.this.ui, 50, 50);
            final AbsolutePanel panel = DisabledInputTest.this.ui.absolutePanel().add(box, 0, 0);
            DisabledInputTest.this.test.show(panel);
            box.setEnabled(false);
            final List<String> heard = new ArrayList<>();
            DisabledInputTest.this.listen(heard, "box", box);
            DisabledInputTest.this.listen(heard, "panel", panel);

            final boolean pressed = DisabledInputTest.this.test.host().pointerButton(10, 10, MouseButton.LEFT, true,
                    Modifiers.NONE);
            final boolean released = DisabledInputTest.this.test.host().pointerButton(10, 10, MouseButton.LEFT, false,
                    Modifiers.NONE);

            assertTrue(pressed, "the press was Strata's");
            assertTrue(released, "and so was its release");
            assertEquals(List.of(), heard);
            assertFalse(box.pressed());
        }

        @Test
        void aClickInsideADisabledContainerReachesNeitherTheChildNorTheContainer() {
            final Box box = new Box(DisabledInputTest.this.ui, 50, 50);
            final AbsolutePanel inner = DisabledInputTest.this.ui.absolutePanel().add(box, 0, 0);
            final AbsolutePanel outer = DisabledInputTest.this.ui.absolutePanel().add(inner, 0, 0);
            DisabledInputTest.this.test.show(outer);
            inner.setEnabled(false);
            final List<String> heard = new ArrayList<>();
            DisabledInputTest.this.listen(heard, "box", box);
            DisabledInputTest.this.listen(heard, "inner", inner);
            DisabledInputTest.this.listen(heard, "outer", outer);

            DisabledInputTest.this.test.host().pointerMoved(10, 10, Modifiers.NONE);
            DisabledInputTest.this.test.click(10, 10);

            assertEquals(List.of(), heard);
            assertSame(box, DisabledInputTest.this.test.host().hovered().orElse(null));
        }

        @Test
        void aPressOnADisabledComponentLeavesFocusWhereItIs() {
            final TextBox field = DisabledInputTest.this.ui.textBox();
            final Box box = new Box(DisabledInputTest.this.ui, 50, 50);
            DisabledInputTest.this.test.show(DisabledInputTest.this.ui.absolutePanel().add(field, 0, 100)
                    .add(box, 0, 0));
            box.setEnabled(false);
            assertTrue(field.requestFocus());

            DisabledInputTest.this.test.click(10, 10);

            assertSame(field, DisabledInputTest.this.test.focused());
        }

        @Test
        void aPressOnADisabledComponentStillClosesAPopupAbove() {
            final Box box = new Box(DisabledInputTest.this.ui, 50, 50);
            DisabledInputTest.this.test.show(DisabledInputTest.this.ui.absolutePanel().add(box, 0, 0));
            box.setEnabled(false);
            final Overlay popup = DisabledInputTest.this.ui.overlays().popup(new Box(DisabledInputTest.this.ui, 20, 20),
                    new Rect(200, 200, 10, 10));

            DisabledInputTest.this.test.click(10, 10);

            assertFalse(popup.isOpen());
        }
    }

    @Nested
    class Hover {
        @Test
        void aDisabledComponentShowsItsTooltip() {
            final Box box = new Box(DisabledInputTest.this.ui, 50, 50).setTooltip("Needs a name first");
            DisabledInputTest.this.test.show(DisabledInputTest.this.ui.absolutePanel().add(box, 0, 0));
            box.setEnabled(false);

            DisabledInputTest.this.test.host().pointerMoved(10, 10, Modifiers.NONE);
            DisabledInputTest.this.test.host().render(new NullCanvas());

            assertTrue(DisabledInputTest.this.test.host().root().tooltip().isPresent());
        }

        @Test
        void aDisabledComponentIsNotMarkedHoveredAndHearsNoMoves() {
            final Box box = new Box(DisabledInputTest.this.ui, 50, 50);
            DisabledInputTest.this.test.show(DisabledInputTest.this.ui.absolutePanel().add(box, 0, 0));
            box.setEnabled(false);
            final List<String> heard = new ArrayList<>();
            DisabledInputTest.this.listen(heard, "box", box);

            DisabledInputTest.this.test.host().pointerMoved(10, 10, Modifiers.NONE);
            DisabledInputTest.this.test.host().pointerMoved(12, 12, Modifiers.NONE);
            DisabledInputTest.this.test.host().pointerLeft();

            assertEquals(List.of(), heard);
            assertFalse(box.peer().isHovered());
        }

        @Test
        void disablingTheHoveredComponentEndsItsHoverOnTheNextFrameAndEnablingBringsItBack() {
            final Box box = new Box(DisabledInputTest.this.ui, 50, 50);
            DisabledInputTest.this.test.show(DisabledInputTest.this.ui.absolutePanel().add(box, 0, 0));
            final List<String> heard = new ArrayList<>();
            DisabledInputTest.this.test.host().pointerMoved(10, 10, Modifiers.NONE);
            DisabledInputTest.this.listen(heard, "box", box);

            box.setEnabled(false);
            DisabledInputTest.this.test.host().render(new NullCanvas());
            final boolean hoveredWhileDisabled = box.peer().isHovered();
            box.setEnabled(true);
            DisabledInputTest.this.test.host().render(new NullCanvas());

            assertEquals(List.of("box exit", "box enter"), heard);
            assertFalse(hoveredWhileDisabled);
            assertTrue(box.peer().isHovered());
        }

        @Test
        void aDisabledComponentShowsTheCursorOfItsNearestEnabledAncestor() {
            final Box box = new Box(DisabledInputTest.this.ui, 50, 50);
            final StackPanel panel = DisabledInputTest.this.ui.stackPanel().add(box);
            panel.setCursor(CursorKind.HAND);
            DisabledInputTest.this.test.show(panel);
            box.setEnabled(false);
            final List<CursorKind> cursors = new ArrayList<>();

            DisabledInputTest.this.test.host().pointerMoved(10, 10, Modifiers.NONE);
            // Every canvas call does nothing except the cursor request, which is recorded
            final Canvas canvas = (Canvas) Proxy.newProxyInstance(Canvas.class.getClassLoader(),
                    new Class<?>[] {Canvas.class}, (proxy, method, args) -> {
                        if (method.getName().equals("requestCursor")) {
                            cursors.add((CursorKind) args[0]);
                        }
                        return null;
                    });
            DisabledInputTest.this.test.host().render(canvas);

            assertEquals(List.of(CursorKind.HAND), cursors);
        }
    }

    @Nested
    class Scrolling {
        @Test
        void aScrollOverADisabledChildGoesToItsEnabledAncestor() {
            final Box box = new Box(DisabledInputTest.this.ui, 50, 50);
            final AbsolutePanel panel = DisabledInputTest.this.ui.absolutePanel().add(box, 0, 0);
            DisabledInputTest.this.test.show(panel);
            box.setEnabled(false);
            final List<String> heard = new ArrayList<>();
            box.onEvent(InputEvents.SCROLL, event -> heard.add("box"));
            panel.onEvent(InputEvents.SCROLL, event -> {
                heard.add("panel");
                event.consume();
            });

            final boolean used = DisabledInputTest.this.test.host().scroll(10, 10, 0, 1, ScrollKind.WHEEL,
                    Modifiers.NONE);

            assertTrue(used);
            assertEquals(List.of("panel"), heard);
        }
    }

    @Nested
    class Queries {
        @Test
        void aChildOfADisabledContainerIsNotEffectivelyEnabled() {
            final Box box = new Box(DisabledInputTest.this.ui, 10, 10);
            final AbsolutePanel panel = DisabledInputTest.this.ui.absolutePanel().add(box, 0, 0);

            panel.setEnabled(false);

            assertTrue(box.isEnabled());
            assertFalse(box.isEffectivelyEnabled());
            assertTrue(box.isEffectivelyVisible());
            panel.setVisible(false);
            assertFalse(box.isEffectivelyVisible());
        }
    }
}
