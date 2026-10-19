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
import org.inspirenxe.strata.api.component.Tooltip;
import org.inspirenxe.strata.api.control.attribute.popup.Closable;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.event.input.InputEvents;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifier;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.layout.AbsolutePanel;
import org.inspirenxe.strata.api.overlay.Overlay;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Popups, dialogs, tooltips and held modifiers.
 */
class OverlayTest {
    private final TestHost test = new TestHost(300, 300);
    private final ComponentFactory ui = this.test.factory();

    @Nested
    class Popups {
        @Test
        void aPopupSitsBelowItsAnchorAndInsideTheHost() {
            OverlayTest.this.test.show(OverlayTest.this.ui.absolutePanel());
            final Box content = new Box(OverlayTest.this.ui, 80, 40);
            OverlayTest.this.ui.overlays().popup(content, new Rect(250, 20, 30, 10));
            OverlayTest.this.test.host().layout();
            assertEquals(new Rect(220, 30, 80, 40), content.bounds());
        }

        @Test
        void aPopupFlipsAboveWhenThereIsMoreRoom() {
            OverlayTest.this.test.show(OverlayTest.this.ui.absolutePanel());
            final Box content = new Box(OverlayTest.this.ui, 80, 100);
            OverlayTest.this.ui.overlays().popup(content, new Rect(0, 250, 30, 10));
            OverlayTest.this.test.host().layout();
            assertEquals(new Rect(0, 150, 80, 100), content.bounds());
        }

        @Test
        void aPressOutsideClosesThePopupAndGoesNoFurther() {
            final List<String> clicks = new ArrayList<>();
            final Button button = OverlayTest.this.ui.button("Under").onEvent(Button.PRESSED, event -> clicks.add("button"));
            OverlayTest.this.test.show(OverlayTest.this.ui.absolutePanel().add(button, 100, 100));
            final Overlay popup = OverlayTest.this.ui.overlays().popup(new Box(OverlayTest.this.ui, 20, 20), new Rect(0, 0, 10, 10));
            OverlayTest.this.test.click(110, 105);
            assertFalse(popup.isOpen());
            assertEquals(List.of(), clicks);
            OverlayTest.this.test.click(110, 105);
            assertEquals(List.of("button"), clicks);
        }

        @Test
        void escapeClosesTheTopOverlay() {
            OverlayTest.this.test.show(OverlayTest.this.ui.absolutePanel());
            final List<String> closed = new ArrayList<>();
            OverlayTest.this.ui.overlays().popup(new Box(OverlayTest.this.ui, 20, 20), Rect.ZERO).onEvent(Closable.CLOSED, event -> closed.add("first"));
            OverlayTest.this.ui.overlays().popup(new Box(OverlayTest.this.ui, 20, 20), Rect.ZERO).onEvent(Closable.CLOSED, event -> closed.add("second"));
            assertTrue(OverlayTest.this.test.host().key(Key.ESCAPE, KeyAction.PRESS, Modifiers.NONE));
            assertEquals(List.of("second"), closed);
            assertEquals(1, OverlayTest.this.ui.overlays().open().size());
        }

        @Test
        void contentAlreadyInATreeIsRefused() {
            final Box box = new Box(OverlayTest.this.ui, 10, 10);
            OverlayTest.this.test.show(OverlayTest.this.ui.absolutePanel().add(box, 0, 0));
            assertThrows(IllegalArgumentException.class, () -> OverlayTest.this.ui.overlays().popup(box, Rect.ZERO));
        }

        @Test
        void closingTheScreenClosesEveryOverlay() {
            OverlayTest.this.test.show(OverlayTest.this.ui.absolutePanel());
            final Overlay popup = OverlayTest.this.ui.overlays().popup(new Box(OverlayTest.this.ui, 20, 20), Rect.ZERO);
            OverlayTest.this.test.host().close();
            assertFalse(popup.isOpen());
        }
    }

    @Nested
    class Dialogs {
        private TextBox below;
        private Button inside;
        private Overlay dialog;

        private void open() {
            this.below = OverlayTest.this.ui.textBox();
            final AbsolutePanel root = OverlayTest.this.ui.absolutePanel().add(this.below, 0, 0);
            OverlayTest.this.test.show(root);
            this.below.requestFocus();
            this.inside = OverlayTest.this.ui.button("OK");
            final AbsolutePanel content = OverlayTest.this.ui.absolutePanel().add(this.inside, 100, 100)
                    .add(OverlayTest.this.ui.button("Cancel"), 160, 100);
            this.dialog = OverlayTest.this.ui.overlays().dialog(content);
        }

        @Test
        void openingMovesFocusInAndClosingPutsItBack() {
            this.open();
            assertSame(this.inside, OverlayTest.this.test.focused());
            this.dialog.close();
            assertSame(this.below, OverlayTest.this.test.focused());
        }

        @Test
        void nothingBelowCanBeFocusedOrPressed() {
            this.open();
            assertFalse(this.below.requestFocus());
            // Outside the dialog's content, where the screen below would get it
            OverlayTest.this.test.click(290, 290);
            assertSame(this.inside, OverlayTest.this.test.focused());
            assertTrue(this.dialog.isOpen());
        }

        @Test
        void tabStaysInsideTheDialog() {
            this.open();
            assertEquals(2, OverlayTest.this.test.tabOrder().size());
            OverlayTest.this.test.host().key(Key.TAB, KeyAction.PRESS, Modifiers.NONE);
            OverlayTest.this.test.host().key(Key.TAB, KeyAction.PRESS, Modifiers.NONE);
            assertSame(this.inside, OverlayTest.this.test.focused());
        }

        @Test
        void ofFindsTheOverlayOfADescendant() {
            this.open();
            assertSame(this.dialog, OverlayTest.this.ui.overlays().of(this.inside).orElseThrow());
            assertTrue(OverlayTest.this.ui.overlays().of(this.below).isEmpty());
        }
    }

    @Nested
    class Tooltips {
        @Test
        void hoveringShowsTheNearestTooltipAndLeavingHidesIt() {
            final Box outer = new Box(OverlayTest.this.ui, 50, 50);
            outer.setTooltip("Hello");
            OverlayTest.this.test.show(OverlayTest.this.ui.absolutePanel().add(outer, 10, 10));
            final var root = OverlayTest.this.test.host().root();
            OverlayTest.this.test.host().pointerMoved(20, 30, Modifiers.NONE);
            assertTrue(root.tooltip().isPresent());
            OverlayTest.this.test.host().layout();
            // Width: 5 characters at 6 units plus 4 units of padding on each side
            assertEquals(new Rect(32, 18, 38, 17), root.tooltip().orElseThrow().bounds());
            OverlayTest.this.test.host().pointerMoved(200, 200, Modifiers.NONE);
            assertTrue(root.tooltip().isEmpty());
        }

        @Test
        void aTooltipFlipsLeftAtTheRightEdge() {
            final Box box = new Box(OverlayTest.this.ui, 300, 300);
            box.setTooltip(Tooltip.text("Wide tooltip"));
            OverlayTest.this.test.show(OverlayTest.this.ui.absolutePanel().add(box, 0, 0));
            OverlayTest.this.test.host().pointerMoved(290, 100, Modifiers.NONE);
            OverlayTest.this.test.host().layout();
            final Rect bounds = OverlayTest.this.test.host().root().tooltip().orElseThrow().bounds();
            assertEquals(290 - 12 - bounds.width(), bounds.x());
        }

        @Test
        void aCustomTooltipIsBuiltEachTimeItShows() {
            final Box box = new Box(OverlayTest.this.ui, 50, 50);
            final List<Box> built = new ArrayList<>();
            box.setTooltip(Tooltip.of(factory -> {
                final Box content = new Box(factory, 10, 10);
                built.add(content);
                return content;
            }));
            OverlayTest.this.test.show(OverlayTest.this.ui.absolutePanel().add(box, 0, 0));
            OverlayTest.this.test.host().pointerMoved(10, 10, Modifiers.NONE);
            OverlayTest.this.test.host().pointerMoved(100, 100, Modifiers.NONE);
            OverlayTest.this.test.host().pointerMoved(10, 10, Modifiers.NONE);
            assertEquals(2, built.size());
        }
    }

    @Test
    void modifierKeysAreTrackedAndReported() {
        final AbsolutePanel root = this.ui.absolutePanel();
        final List<Boolean> control = new ArrayList<>();
        root.onEvent(InputEvents.MODIFIERS_CHANGED, event -> control.add(event.modifiers().control()));
        this.test.show(root);
        this.test.host().key(Key.LEFT_CONTROL, KeyAction.PRESS, Modifiers.NONE);
        this.test.host().key(Key.LEFT_CONTROL, KeyAction.RELEASE, Modifiers.of(Modifier.CONTROL));
        assertEquals(List.of(true, false), control);
        this.test.host().pointerButton(1, 1, MouseButton.LEFT, true, Modifiers.of(Modifier.SHIFT));
        assertTrue(this.test.host().root().modifiers().shift());
    }
}
