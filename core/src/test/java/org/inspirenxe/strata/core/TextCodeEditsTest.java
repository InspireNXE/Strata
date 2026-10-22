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
import org.inspirenxe.strata.api.control.attribute.text.TextEntry;
import org.inspirenxe.strata.api.control.attribute.text.TextInput;
import org.inspirenxe.strata.api.control.field.TextArea;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.layout.Anchor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code TextInput.insert}, {@code replace}, {@code append} and {@code delete} (design 4.44).
 */
class TextCodeEditsTest {
    private final TestHost test = new TestHost(300, 200);
    private final ComponentFactory ui = this.test.factory();

    private static List<String> log(final TextInput<?> field) {
        final List<String> seen = new ArrayList<>();
        field.onEvent(TextInput.EDIT, event -> seen.add("edit"));
        field.onEvent(TextEntry.MODIFY, event -> seen.add("modify"));
        field.onEvent(TextInput.EDITED, event -> seen.add("edited " + event.previous() + " > " + event.value()));
        field.onEvent(TextEntry.MODIFIED, event -> seen.add("modified " + event.value()));
        field.onEvent(TextEntry.CARET_MOVED, event -> seen.add("caret " + event.value().anchor() + ":"
                + event.value().caret()));
        return seen;
    }

    private TextBox box(final String text) {
        final TextBox box = this.ui.textBox().setAnchor(Anchor.TOP_LEFT).setWidth(200);
        this.test.show(box);
        box.setText(text);
        return box;
    }

    @Test
    void insertGoesAtTheCaretOverTheSelectionAndReportsWithoutAsking() {
        final TextBox box = this.box("hello world");
        box.select(6, 11);
        final List<String> seen = log(box);
        box.insert("@Ana ");
        assertEquals("hello @Ana ", box.text());
        assertEquals(11, box.caret());
        assertEquals(11, box.selectionStart());
        assertEquals(List.of("edited hello world > hello @Ana ", "modified 11", "caret 11:11"), seen);
    }

    @Test
    void replaceKeepsTheCaretWithItsCharacters() {
        final TextBox box = this.box("one two three");
        box.select(4, 13);
        box.replace(0, 3, "1");
        assertEquals("1 two three", box.text());
        assertEquals(2, box.selectionStart());
        assertEquals(11, box.selectionEnd());

        // A caret inside the range goes to the end of the new text; an anchor before it stays
        box.select(0, 4);
        box.replace(2, 5, "2222");
        assertEquals("1 2222 three", box.text());
        assertEquals(0, box.selectionStart());
        assertEquals(6, box.caret());
    }

    @Test
    void appendKeepsACaretAtTheEndAtTheEnd() {
        final TextBox box = this.box("log");
        box.append(" more");
        assertEquals(8, box.caret());
        box.select(1, 1);
        box.append("!");
        assertEquals("log more!", box.text());
        assertEquals(1, box.caret());
    }

    @Test
    void deleteRemovesARangeAndMovesACaretInsideToItsStart() {
        final TextBox box = this.box("abcdef");
        box.select(3, 3);
        box.delete(1, 4);
        assertEquals("aef", box.text());
        assertEquals(1, box.caret());
    }

    @Test
    void rangesAreClampedAndSnappedAndOrdered() {
        final TextBox box = this.box("a😀b");
        // 2 falls inside the pair, so the range ends at the pair's start
        box.replace(-5, 2, "x");
        assertEquals("x😀b", box.text());
        box.delete(3, 99);
        assertEquals("x😀", box.text());
        assertThrows(IllegalArgumentException.class, () -> box.replace(3, 1, "y"));
    }

    @Test
    void textIsCleanedAndCutToTheLimit() {
        final TextBox box = this.box("ab").setMaxLength(4);
        box.insert("1\n2\t3");
        assertEquals("ab12", box.text());
        final TextArea area = this.ui.textArea();
        area.setText("x");
        area.append("\r\nline\u0007");
        assertEquals("x\nline", area.text());
    }

    @Test
    void anEditThatChangesNothingReportsNothing() {
        final TextBox box = this.box("same");
        final List<String> seen = log(box);
        box.replace(0, 2, "sa");
        box.insert("");
        box.setMaxLength(1).setMaxLength(9);
        assertEquals(List.of("edited same > s", "modified 1", "caret 1:1"), seen);
    }

    @Test
    void aTextAreaKeepsItsScrollUnlessTheCaretMoves() {
        final TextArea area = this.ui.textArea().setAnchor(Anchor.TOP_LEFT).setSize(200, 40);
        this.test.show(area);
        final StringBuilder lines = new StringBuilder();
        for (int i = 0; i < 30; i++) {
            lines.append("line ").append(i).append('\n');
        }
        area.setText(lines.toString());
        area.select(0, 0);
        this.test.host().layout();
        this.test.host().render(new NullCanvas());
        assertEquals(0f, area.scrollY());

        area.append("tail\n");
        this.test.host().render(new NullCanvas());
        assertEquals(0f, area.scrollY());

        area.insert("top\n");
        this.test.host().render(new NullCanvas());
        assertEquals(0f, area.scrollY());
        assertTrue(area.text().startsWith("top\nline 0"));
    }
}
