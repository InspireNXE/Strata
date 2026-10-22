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
import org.inspirenxe.strata.api.control.field.TextArea;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifier;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.layout.Anchor;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Editing that the text box and the text area share through one editing core, and the fixes made to it. With the
 * test platform every code point is 6 units wide.
 */
class TextEditingTest {
    private static final Modifiers ALT = Modifiers.of(Modifier.ALT);

    private final TestHost test = new TestHost(300, 200);
    private final ComponentFactory ui = this.test.factory();

    private boolean press(final Key key, final Modifiers modifiers) {
        final boolean handled = this.test.host().key(key, KeyAction.PRESS, modifiers);
        this.test.host().key(key, KeyAction.RELEASE, modifiers);
        return handled;
    }

    private void type(final String text) {
        text.codePoints().forEach(codePoint -> this.test.host().charTyped(codePoint, Modifiers.NONE));
    }

    /**
     * Records each {@code EDITED} as "previous > value".
     */
    private static List<String> edits(final TextInput<?> field) {
        final List<String> seen = new ArrayList<>();
        field.onEvent(TextInput.EDITED, event -> seen.add(event.previous() + " > " + event.value()));
        return seen;
    }

    private TextBox showBox(final String text) {
        final TextBox box = this.ui.textBox().setAnchor(Anchor.TOP_LEFT).setWidth(200);
        this.test.show(box);
        box.setText(text);
        box.requestFocus();
        return box;
    }

    private TextArea showArea(final String text) {
        final TextArea area = this.ui.textArea().setAnchor(Anchor.TOP_LEFT).setWidth(200);
        this.test.show(area);
        area.setText(text);
        area.requestFocus();
        return area;
    }

    @Nested
    class SharedKeys {

        @Test
        void altKeysAreLeftToTheEngineInATextBox() {
            final TextBox box = TextEditingTest.this.showBox("hello");

            assertFalse(TextEditingTest.this.press(Key.LEFT, ALT));
            assertFalse(TextEditingTest.this.press(Key.BACKSPACE, ALT));

            assertEquals(5, box.caret());
            assertEquals("hello", box.text());
        }

        @Test
        void altKeysAreLeftToTheEngineInATextArea() {
            final TextArea area = TextEditingTest.this.showArea("hello");

            assertFalse(TextEditingTest.this.press(Key.LEFT, ALT));
            assertFalse(TextEditingTest.this.press(Key.BACKSPACE, ALT));

            assertEquals(5, area.caret());
            assertEquals("hello", area.text());
        }
    }

    @Nested
    class HandlersThatSetTheText {

        @Test
        void anEditAfterAHandlerSetTheTextReportsTheTextTheHandlerSet() {
            final TextBox box = TextEditingTest.this.showBox("ab");
            final List<String> seen = edits(box);
            box.onEvent(TextInput.EDIT, event -> box.setText("X"));

            TextEditingTest.this.type("c");

            assertEquals(List.of("ab > X", "X > abc"), seen);
            assertEquals("abc", box.text());
        }

        @Test
        void aHandlerThatSetsTheEditsOwnTextLeavesNothingMoreToReport() {
            final TextBox box = TextEditingTest.this.showBox("ab");
            final List<String> seen = edits(box);
            box.onEvent(TextInput.EDIT, event -> box.setText(event.value()));

            TextEditingTest.this.type("c");

            assertEquals(List.of("ab > abc"), seen);
        }

        @Test
        void anEditRewrittenBackToTheOldTextStillReportsLeavingTheHandlersText() {
            final TextBox box = TextEditingTest.this.showBox("ab");
            final List<String> seen = edits(box);
            box.onEvent(TextInput.EDIT, event -> {
                box.setText("zz");
                event.setValue("ab");
            });

            TextEditingTest.this.type("c");

            assertEquals(List.of("ab > zz", "zz > ab"), seen);
            assertEquals("ab", box.text());
        }

        @Test
        void aTextAreaReportsFromTheTextAModifyHandlerSet() {
            final TextArea area = TextEditingTest.this.showArea("ab");
            final List<String> seen = edits(area);
            area.onEvent(TextInput.MODIFY, event -> area.setText("X"));

            TextEditingTest.this.type("c");

            assertEquals(List.of("ab > X", "X > abc"), seen);
        }
    }
}
