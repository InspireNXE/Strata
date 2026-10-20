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

import org.inspirenxe.strata.api.control.attribute.text.TextEntry;
import org.inspirenxe.strata.api.control.attribute.text.TextInput;
import org.inspirenxe.strata.api.control.field.PasswordTextBox;
import org.inspirenxe.strata.api.control.field.TextArea;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.event.Event;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.control.ValueEvent;
import org.inspirenxe.strata.api.event.control.ValueRequestEvent;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.layout.AbsolutePanel;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The {@link TextEntry} pair on every field: a user's edit asks {@code MODIFY} before {@code EDIT}, and every change
 * reports {@code EDITED} before {@code MODIFIED}, so one form handler hears every field without learning any text.
 */
class TextEntryTest {
    private final TestHost test = new TestHost(300, 200);
    private final TextBox box = this.test.factory().textBox().setWidth(200);
    private final TextArea area = this.test.factory().textArea().setSize(200, 60);
    private final PasswordTextBox password = this.test.factory().passwordTextBox().setWidth(200);
    private final AbsolutePanel form = this.test.factory().absolutePanel()
            .add(this.box).add(this.area, 0, 30).add(this.password, 0, 100);
    private final List<String> heard = new ArrayList<>();

    TextEntryTest() {
        this.test.show(this.form);
    }

    private <E extends Event> void listen(final EventType<E> type) {
        this.form.onEvent(type, event -> {
            final StringBuilder line = new StringBuilder(type.name()).append(' ')
                    .append(event.target().getClass().getSimpleName());
            if (event instanceof final ValueRequestEvent<?, ?> request) {
                line.append(' ').append(request.value());
            } else if (event instanceof final ValueEvent<?, ?> report) {
                line.append(' ').append(report.previous()).append("->").append(report.value());
            }
            this.heard.add(line.toString());
        });
    }

    private void type(final String text) {
        text.codePoints().forEach(codePoint -> this.test.host().charTyped(codePoint, Modifiers.NONE));
    }

    @Test
    void anEditAsksModifyThenEditAndReportsEditedThenModified() {
        this.listen(TextEntry.MODIFY);
        this.listen(TextInput.EDIT);
        this.listen(TextInput.EDITED);
        this.listen(TextEntry.MODIFIED);
        this.box.requestFocus();
        this.type("a");
        this.area.requestFocus();
        this.test.host().key(Key.ENTER, KeyAction.PRESS, Modifiers.NONE);

        assertEquals(List.of(
                "text_entry.modify TextBoxImpl", "text_input.edit TextBoxImpl a",
                "text_input.edited TextBoxImpl ->a", "text_entry.modified TextBoxImpl 0->1",
                "text_entry.modify TextAreaImpl", "text_input.edit TextAreaImpl \n",
                "text_input.edited TextAreaImpl ->\n", "text_entry.modified TextAreaImpl 0->1"), this.heard);
    }

    @Test
    void aCanceledModifySkipsEditAndKeepsTheText() {
        final List<String> edits = new ArrayList<>();
        this.form.onEvent(TextInput.EDIT, event -> edits.add(event.value()));
        this.box.setText("ab");
        this.area.setText("ab");
        this.form.onEvent(TextEntry.MODIFY, event -> event.cancel());

        this.box.requestFocus();
        this.type("c");
        this.test.host().key(Key.BACKSPACE, KeyAction.PRESS, Modifiers.NONE);
        this.area.requestFocus();
        this.type("c");

        assertEquals("ab", this.box.text());
        assertEquals(2, this.box.caret());
        assertEquals("ab", this.area.text());
        assertEquals(List.of(), edits);
    }

    @Test
    void aModifyHandlerAloneStillLetsTheEditApply() {
        this.listen(TextEntry.MODIFY);
        this.box.requestFocus();
        this.type("ab");
        assertEquals("ab", this.box.text());
        assertEquals(2, this.box.caret());
        assertEquals(List.of("text_entry.modify TextBoxImpl", "text_entry.modify TextBoxImpl"), this.heard);
    }

    @Test
    void codeReportsModifiedButNeverAsksModify() {
        this.listen(TextEntry.MODIFY);
        this.listen(TextEntry.MODIFIED);
        this.box.setText("abc");
        this.box.setMaxLength(2);
        this.area.setText("x");
        this.password.setPassword("pw".toCharArray());
        this.box.clear();

        assertEquals(List.of("text_entry.modified TextBoxImpl 0->3", "text_entry.modified TextBoxImpl 3->2",
                "text_entry.modified TextAreaImpl 0->1", "text_entry.modified PasswordTextBoxImpl 0->2",
                "text_entry.modified TextBoxImpl 2->0"), this.heard);
    }

    @Test
    void oneFormHandlerEnablesSaveFromEveryField() {
        final boolean[] save = new boolean[1];
        this.form.onEvent(TextEntry.MODIFIED,
                event -> save[0] = !this.box.isEmpty() && !this.area.isEmpty() && !this.password.isEmpty());
        this.box.requestFocus();
        this.type("Ana");
        this.area.requestFocus();
        this.type("note");
        assertFalse(save[0]);
        this.password.requestFocus();
        this.type("pw");
        assertTrue(save[0]);
    }

    @Test
    void lengthCountsCodePoints() {
        this.box.setText("a😀");
        this.area.setText("a\n😀");
        assertEquals(2, this.box.length());
        assertEquals(3, this.area.length());
        assertEquals(3, this.box.text().length());
        assertTrue(this.box.clear().isEmpty());
        assertTrue(this.area.clear().isEmpty());
    }
}
