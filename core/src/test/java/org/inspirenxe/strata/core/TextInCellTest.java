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
import org.inspirenxe.strata.api.component.Validation;
import org.inspirenxe.strata.api.control.attribute.item.CellEditing;
import org.inspirenxe.strata.api.control.collection.ListView;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifier;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.text.Text;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Undo and validation in a text box that edits a list's cell (designs 4.43 and 4.44 beside 4.56's cell editing).
 */
class TextInCellTest {
    private final TestHost test = new TestHost(300, 200);
    private final ComponentFactory ui = this.test.factory();
    private final List<TextBox> editors = new ArrayList<>();
    private final List<String> committed = new ArrayList<>();

    private void press(final Key key, final Modifiers modifiers) {
        this.test.host().key(key, KeyAction.PRESS, modifiers);
        this.test.host().key(key, KeyAction.RELEASE, modifiers);
    }

    @Test
    void anEditingCellUndoesAndValidatesLikeAnyBox() {
        final ListView<String> list = this.ui.<String>listView().setItems(List.of("Ana", "Ben"))
                .setCellEditor((factory, item, commit, cancel) -> {
                    final TextBox box = factory.textBox().setText(item)
                            .setValidator(field -> field.isEmpty() ? Validation.error(Text.of("Enter a name"))
                                    : Validation.VALID);
                    box.onEvent(TextBox.SUBMITTED, event -> {
                        if (box.validation().isValid()) {
                            commit.accept(event.value());
                        }
                    });
                    this.editors.add(box);
                    return box;
                })
                .onEvent(CellEditing.CELL_EDITED, event -> this.committed.add(String.valueOf(event.value())));
        list.setSize(150, 100);
        this.test.show(this.ui.absolutePanel().add(list, 0, 0));
        list.select(0).requestFocus();
        this.press(Key.F2, Modifiers.NONE);
        final TextBox box = this.editors.get(0);
        assertTrue(box.isFocused());

        // An item set from code is no edit to undo, and Ctrl+Z with nothing to undo leaves the edit open
        this.press(Key.Z, Modifiers.of(Modifier.CONTROL));
        assertTrue(list.isEditing());
        assertEquals("Ana", box.text());

        box.selectAll();
        this.press(Key.BACKSPACE, Modifiers.NONE);
        assertEquals(Validation.Severity.ERROR, box.validation().severity());
        this.press(Key.ENTER, Modifiers.NONE);
        assertTrue(list.isEditing(), "an invalid name is not committed");

        this.press(Key.Z, Modifiers.of(Modifier.CONTROL));
        assertEquals("Ana", box.text());
        assertTrue(box.validation().isValid());
        // The undo put the selection back too
        assertEquals(3, box.selectionEnd() - box.selectionStart());
        this.press(Key.END, Modifiers.NONE);
        "!".codePoints().forEach(c -> this.test.host().charTyped(c, Modifiers.NONE));
        this.press(Key.ENTER, Modifiers.NONE);
        assertFalse(list.isEditing());
        assertEquals(List.of("Ana!"), this.committed);
    }
}
