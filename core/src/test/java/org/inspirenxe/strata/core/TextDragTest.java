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
import org.inspirenxe.strata.api.control.attribute.text.TextInput;
import org.inspirenxe.strata.api.control.field.PasswordTextBox;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.event.input.InputEvents;
import org.inspirenxe.strata.api.input.Modifier;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.layout.AbsolutePanel;
import org.inspirenxe.strata.api.transfer.DropEffect;
import org.inspirenxe.strata.api.transfer.Transfer;
import org.inspirenxe.strata.core.host.ScreenHostImpl;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Dragging selected text out of a text box and dropping text into one (design 4.48). Every code point is 6 units
 * wide and a box's text starts 4 units in, so the boundary before index {@code i} of the box at the top sits at
 * x = 4 + 6i; the second box sits 40 units lower.
 */
class TextDragTest {
    private final TestHost test = new TestHost(300, 200);
    private final ComponentFactory ui = this.test.factory();
    private final ScreenHostImpl host = this.test.host();
    private final TextBox first = this.ui.textBox().setWidth(200);
    private final TextBox second = this.ui.textBox().setWidth(200);
    private final AbsolutePanel panel = this.ui.absolutePanel().add(this.first, 0, 0).add(this.second, 0, 40);

    TextDragTest() {
        this.panel.setSize(300, 200);
        this.first.setText("ana ben cleo");
        this.second.setText("dev");
        this.test.show(this.panel);
    }

    private static float x(final int index) {
        return 4f + 6f * index;
    }

    private void drag(final float fromX, final float fromY, final float toX, final float toY, final Modifiers modifiers) {
        this.host.pointerMoved(fromX, fromY, modifiers);
        this.host.pointerButton(fromX, fromY, MouseButton.LEFT, true, modifiers);
        this.host.pointerMoved(fromX + 6, fromY + 6, modifiers);
        this.host.pointerMoved(toX, toY, modifiers);
        this.host.pointerButton(toX, toY, MouseButton.LEFT, false, modifiers);
    }

    private void selectBen() {
        this.first.requestFocus();
        this.first.select(4, 7);
        this.test.platform().advanceMillis(1000);
    }

    @Test
    void selectedTextMovesToAnotherBox() {
        this.selectBen();
        this.drag(x(5), 10, x(3), 50, Modifiers.NONE);

        assertEquals("devben", this.second.text());
        assertEquals("ana  cleo", this.first.text());
    }

    @Test
    void controlCopiesInstead() {
        this.selectBen();
        this.drag(x(5), 10, x(0), 50, Modifiers.of(Modifier.CONTROL));

        assertEquals("bendev", this.second.text());
        assertEquals("ana ben cleo", this.first.text());
    }

    @Test
    void textMovesInsideItsOwnBoxAsOneEdit() {
        this.selectBen();
        this.drag(x(5), 10, x(12), 10, Modifiers.NONE);
        assertEquals("ana  cleoben", this.first.text());

        this.first.undo();
        assertEquals("ana ben cleo", this.first.text());

        this.selectBen();
        this.drag(x(5), 10, x(0), 10, Modifiers.NONE);
        assertEquals("benana  cleo", this.first.text());
    }

    @Test
    void aDropInsideTheDraggedTextChangesNothing() {
        this.selectBen();
        this.drag(x(5), 10, x(6), 12, Modifiers.NONE);
        assertEquals("ana ben cleo", this.first.text());
    }

    @Test
    void aPressInsideTheSelectionWithoutADragPlacesTheCaretOnRelease() {
        this.selectBen();
        this.host.pointerButton(x(5), 10, MouseButton.LEFT, true, Modifiers.NONE);
        assertEquals(4, this.first.selectionStart());
        assertEquals(7, this.first.selectionEnd());
        this.host.pointerButton(x(5), 10, MouseButton.LEFT, false, Modifiers.NONE);
        assertEquals(5, this.first.caret());
        assertEquals(5, this.first.selectionStart());
    }

    @Test
    void aDropAsksEditAndARefusedEditLeavesTheBox() {
        final List<String> asked = new ArrayList<>();
        this.second.onEvent(TextInput.EDIT, event -> {
            asked.add(event.value());
            event.cancel();
        });
        this.selectBen();
        this.drag(x(5), 10, x(3), 50, Modifiers.NONE);

        assertEquals(List.of("devben"), asked);
        assertEquals("dev", this.second.text());
    }

    @Test
    void readOnlyBoxesTakeNoDropsAndOnlyLendCopies() {
        this.second.setReadOnly(true);
        this.selectBen();
        this.drag(x(5), 10, x(3), 50, Modifiers.NONE);
        assertEquals("dev", this.second.text());
        assertEquals("ana ben cleo", this.first.text());

        final List<Object> allowed = new ArrayList<>();
        this.first.setReadOnly(true);
        this.panel.onEvent(InputEvents.DRAG_OVER, event -> allowed.add(event.allowed()));
        this.selectBen();
        this.drag(x(5), 10, 250, 150, Modifiers.NONE);
        assertEquals(List.of(EnumSet.of(DropEffect.COPY)), allowed.subList(0, 1));
    }

    @Test
    void textFromAnotherApplicationDropsWhereThePointerIs() {
        assertEquals(Optional.of(DropEffect.COPY), this.host.dragEntered(x(1), 50, Transfer.text("x"),
                EnumSet.of(DropEffect.COPY), Modifiers.NONE));
        assertEquals(Optional.of(DropEffect.COPY), this.host.dropped(x(1), 50, Transfer.text("line\nbreak"),
                Modifiers.NONE));
        assertEquals("dlinebreakev", this.second.text());
    }

    @Test
    void aPasswordBoxNeitherLendsNorTakesText() {
        final PasswordTextBox secret = this.ui.passwordTextBox().setWidth(200);
        this.panel.add(secret, 0, 80);
        this.host.layout();
        secret.requestFocus();
        "hunter".codePoints().forEach(codePoint -> this.host.charTyped(codePoint, Modifiers.NONE));
        assertEquals(Optional.empty(), this.host.dropped(x(1), 90, Transfer.text("x"), Modifiers.NONE));

        // Dragging over its text selects, as before, and carries nothing out
        final List<Object> over = new ArrayList<>();
        this.panel.onEvent(InputEvents.DRAG_OVER, event -> over.add(event));
        secret.requestFocus();
        secret.selectAll();
        this.test.platform().advanceMillis(1000);
        this.drag(x(2), 90, 250, 150, Modifiers.NONE);
        assertTrue(over.isEmpty());
    }
}
