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
import org.inspirenxe.strata.api.control.field.PasswordTextBox;
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
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Undo and redo in the text fields (design 4.44).
 */
class TextUndoTest {
    private static final Modifiers CONTROL = Modifiers.of(Modifier.CONTROL);
    private static final Modifiers CONTROL_SHIFT = Modifiers.of(Modifier.CONTROL, Modifier.SHIFT);

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

    private boolean undo() {
        return this.press(Key.Z, CONTROL);
    }

    private TextBox box() {
        final TextBox box = this.ui.textBox().setAnchor(Anchor.TOP_LEFT).setWidth(200);
        this.test.show(box);
        box.requestFocus();
        return box;
    }

    @Nested
    class Merging {
        @Test
        void typingMergesUntilAWordEndsWithASpace() {
            final TextBox box = TextUndoTest.this.box();
            TextUndoTest.this.type("hello world");
            assertTrue(TextUndoTest.this.undo());
            assertEquals("hello ", box.text());
            TextUndoTest.this.undo();
            assertEquals("", box.text());
            assertFalse(box.canUndo());
            assertFalse(TextUndoTest.this.undo());

            TextUndoTest.this.press(Key.Y, CONTROL);
            assertEquals("hello ", box.text());
            TextUndoTest.this.press(Key.Z, CONTROL_SHIFT);
            assertEquals("hello world", box.text());
            assertEquals(11, box.caret());
            assertFalse(box.canRedo());
        }

        @Test
        void aPauseOfASecondStartsANewEdit() {
            final TextBox box = TextUndoTest.this.box();
            TextUndoTest.this.type("ab");
            TextUndoTest.this.test.platform().advanceMillis(1000);
            TextUndoTest.this.type("cd");
            TextUndoTest.this.undo();
            assertEquals("ab", box.text());
        }

        @Test
        void movingTheCaretStartsANewEditAndUndoPutsItBack() {
            final TextBox box = TextUndoTest.this.box();
            TextUndoTest.this.type("ab");
            TextUndoTest.this.press(Key.LEFT, Modifiers.NONE);
            TextUndoTest.this.type("c");
            assertEquals("acb", box.text());
            TextUndoTest.this.undo();
            assertEquals("ab", box.text());
            assertEquals(1, box.caret());
        }

        @Test
        void aRunOfBackspacesOrDeletesIsOneEdit() {
            final TextBox box = TextUndoTest.this.box();
            TextUndoTest.this.type("hello");
            TextUndoTest.this.test.platform().advanceMillis(2000);
            for (int i = 0; i < 3; i++) {
                TextUndoTest.this.press(Key.BACKSPACE, Modifiers.NONE);
            }
            assertEquals("he", box.text());
            TextUndoTest.this.undo();
            assertEquals("hello", box.text());
            assertEquals(5, box.caret());

            TextUndoTest.this.press(Key.HOME, Modifiers.NONE);
            TextUndoTest.this.press(Key.DELETE, Modifiers.NONE);
            TextUndoTest.this.press(Key.DELETE, Modifiers.NONE);
            assertEquals("llo", box.text());
            TextUndoTest.this.undo();
            assertEquals("hello", box.text());
            assertEquals(0, box.caret());
        }

        @Test
        void pastesCutsAndDeletedSelectionsAreAnEditEach() {
            final TextBox box = TextUndoTest.this.box();
            TextUndoTest.this.type("x");
            TextUndoTest.this.test.platform().clipboard().set("123");
            TextUndoTest.this.press(Key.V, CONTROL);
            TextUndoTest.this.press(Key.V, CONTROL);
            assertEquals("x123123", box.text());
            TextUndoTest.this.undo();
            assertEquals("x123", box.text());

            box.select(1, 4);
            TextUndoTest.this.press(Key.BACKSPACE, Modifiers.NONE);
            assertEquals("x", box.text());
            TextUndoTest.this.undo();
            assertEquals("x123", box.text());
            assertEquals(1, box.selectionStart());
            assertEquals(4, box.selectionEnd());
        }

        @Test
        void typingOverASelectionUndoesToTheSelection() {
            final TextBox box = TextUndoTest.this.box();
            TextUndoTest.this.type("hello");
            box.selectAll();
            TextUndoTest.this.type("bye");
            TextUndoTest.this.undo();
            assertEquals("hello", box.text());
            assertEquals(0, box.selectionStart());
            assertEquals(5, box.selectionEnd());
        }

        @Test
        void aLineBreakEndsAWordInATextArea() {
            final TextArea area = TextUndoTest.this.ui.textArea().setAnchor(Anchor.TOP_LEFT).setWidth(200);
            TextUndoTest.this.test.show(area);
            area.requestFocus();
            TextUndoTest.this.type("one");
            TextUndoTest.this.press(Key.ENTER, Modifiers.NONE);
            TextUndoTest.this.type("two");
            TextUndoTest.this.undo();
            assertEquals("one\n", area.text());
        }
    }

    @Nested
    class Requests {
        @Test
        void anUndoByKeyAsksWithTheTextItWouldRestore() {
            final TextBox box = TextUndoTest.this.box();
            TextUndoTest.this.type("abc");
            final List<String> seen = new ArrayList<>();
            box.onEvent(TextEntry.MODIFY, event -> seen.add("modify"));
            box.onEvent(TextInput.EDIT, event -> seen.add("edit " + event.value()));
            TextUndoTest.this.undo();
            assertEquals(List.of("modify", "edit "), seen);
            assertEquals("", box.text());
        }

        @Test
        void aHandlerThatRefusesLettersRefusesAnUndoBackToThem() {
            final TextBox box = TextUndoTest.this.box();
            TextUndoTest.this.type("abc");
            box.onEvent(TextInput.EDIT, event -> {
                if (event.value().chars().anyMatch(Character::isLetter)) {
                    event.cancel();
                }
            });
            box.selectAll();
            TextUndoTest.this.press(Key.BACKSPACE, Modifiers.NONE);
            assertEquals("", box.text());
            assertTrue(TextUndoTest.this.undo());
            assertEquals("", box.text());
            assertTrue(box.canUndo());

            // Code is trusted
            box.undo();
            assertEquals("abc", box.text());
        }

        @Test
        void aRewrittenUndoIsAnEditOfItsOwn() {
            final TextBox box = TextUndoTest.this.box();
            TextUndoTest.this.type("ab ");
            TextUndoTest.this.type("cd");
            box.onEvent(TextInput.EDIT, event -> event.setValue(event.value().toUpperCase(Locale.ROOT)));
            TextUndoTest.this.undo();
            assertEquals("AB ", box.text());
            assertEquals(3, box.caret());
            // The rewrite is the last edit now, and undoing it is asked about like any other
            TextUndoTest.this.undo();
            assertEquals("AB CD", box.text());
        }

        @Test
        void aReadOnlyFieldLeavesTheKeysAloneButCodeStillUndoes() {
            final TextBox box = TextUndoTest.this.box();
            TextUndoTest.this.type("abc");
            box.setReadOnly(true);
            assertFalse(TextUndoTest.this.undo());
            assertEquals("abc", box.text());
            box.undo();
            assertEquals("", box.text());
            box.redo();
            assertEquals("abc", box.text());
        }
    }

    @Nested
    class FromCode {
        @Test
        void setTextClearsTheHistory() {
            final TextBox box = TextUndoTest.this.box();
            TextUndoTest.this.type("abc");
            box.setText("new");
            assertFalse(box.canUndo());
            box.setText("new");
            assertFalse(box.canUndo());
        }

        @Test
        void editsFromCodeAreRecorded() {
            final TextBox box = TextUndoTest.this.box();
            TextUndoTest.this.type("hi ");
            box.insert("@Ana ");
            assertEquals("hi @Ana ", box.text());
            TextUndoTest.this.undo();
            assertEquals("hi ", box.text());
            assertEquals(3, box.caret());
            box.redo();
            assertEquals("hi @Ana ", box.text());
        }

        @Test
        void undoAndRedoFromCodeReportAndAskNothing() {
            final TextBox box = TextUndoTest.this.box();
            TextUndoTest.this.type("abc");
            final List<String> seen = new ArrayList<>();
            box.onEvent(TextEntry.MODIFY, event -> {
                seen.add("modify");
                event.cancel();
            });
            box.onEvent(TextInput.EDITED, event -> seen.add("edited " + event.value()));
            box.undo();
            box.redo();
            box.redo();
            assertEquals(List.of("edited ", "edited abc"), seen);
        }

        @Test
        void theLimitDropsTheOldestEdits() {
            final TextBox box = TextUndoTest.this.box();
            assertEquals(100, box.historyLimit());
            box.setHistoryLimit(2);
            box.append("a").append("b").append("c");
            box.undo().undo().undo();
            assertEquals("a", box.text());
            box.setHistoryLimit(0);
            assertFalse(box.canUndo());
            assertFalse(box.canRedo());
            box.append("d");
            assertFalse(box.canUndo());
            assertThrows(IllegalArgumentException.class, () -> box.setHistoryLimit(-1));
        }

        @Test
        void clearHistoryForgetsEverything() {
            final TextBox box = TextUndoTest.this.box();
            box.append("a").append("b");
            box.undo();
            box.clearHistory();
            assertFalse(box.canUndo());
            assertFalse(box.canRedo());
        }
    }

    @Test
    void aPasswordBoxKeepsNoHistory() {
        final PasswordTextBox box = this.ui.passwordTextBox().setAnchor(Anchor.TOP_LEFT).setWidth(200);
        this.test.show(box);
        box.requestFocus();
        this.type("secret");
        assertFalse(this.undo());
        assertEquals(6, box.length());
    }
}
