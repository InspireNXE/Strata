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
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.layout.Anchor;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Editing that the text box and the text area share through one editing core, and the fixes made to it. With the
 * test platform every code point is 6 units wide.
 */
class TextEditingTest {
    private static final Modifiers ALT = Modifiers.of(Modifier.ALT);
    private static final Modifiers CONTROL = Modifiers.of(Modifier.CONTROL);

    private final TestHost test = new TestHost(300, 200);
    private final ComponentFactory ui = this.test.factory();

    private boolean press(final Key key, final Modifiers modifiers) {
        final boolean handled = this.test.host().key(key, KeyAction.PRESS, modifiers);
        this.test.host().key(key, KeyAction.RELEASE, modifiers);
        return handled;
    }

    private void paste(final String text) {
        this.test.platform().clipboard().set(text);
        this.press(Key.V, CONTROL);
    }

    private void doubleClickBoxAt(final int index) {
        final float x = 4f + 6f * index + 1f;
        for (int i = 0; i < 2; i++) {
            this.test.host().pointerButton(x, 10, MouseButton.LEFT, true, Modifiers.NONE);
            this.test.host().pointerButton(x, 10, MouseButton.LEFT, false, Modifiers.NONE);
        }
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

    @Nested
    class GoalColumn {

        @Test
        void selectingFromCodeForgetsTheColumnUpAndDownKept() {
            final TextArea area = TextEditingTest.this.showArea("abcdef\nab\nabcdef");
            area.select(5, 5);
            TextEditingTest.this.press(Key.DOWN, Modifiers.NONE);
            assertEquals(9, area.caret(), "the short row stops the caret at its end");

            area.select(1, 1);
            TextEditingTest.this.press(Key.DOWN, Modifiers.NONE);

            assertEquals(8, area.caret(), "Down starts again from where code put the caret");
        }

        @Test
        void settingTheTextForgetsTheColumnUpAndDownKept() {
            final TextArea area = TextEditingTest.this.showArea("abcdef\nab\nabcdef");
            area.select(5, 5);
            TextEditingTest.this.press(Key.DOWN, Modifiers.NONE);

            area.setText("abcdef\nabcdef");
            TextEditingTest.this.press(Key.UP, Modifiers.NONE);

            assertEquals(6, area.caret(), "Up starts again from the end of the new text");
        }

        @Test
        void selectingAllForgetsTheColumnUpAndDownKept() {
            final TextArea area = TextEditingTest.this.showArea("abcdef\nab\nabcdefgh");
            area.select(5, 5);
            TextEditingTest.this.press(Key.DOWN, Modifiers.NONE);

            area.selectAll();
            TextEditingTest.this.press(Key.UP, Modifiers.NONE);

            assertEquals(9, area.caret(), "Up starts again from the end of the text, past the short row");
            TextEditingTest.this.press(Key.UP, Modifiers.NONE);
            assertEquals(6, area.caret(), "and keeps that column, not the one from before");
        }
    }

    @Nested
    class PastedText {

        @Test
        void aTextBoxDropsControlCharactersAndEveryLineBreakFromAPaste() {
            final TextBox box = TextEditingTest.this.showBox("");

            TextEditingTest.this.paste("a\tb\u0000c\u001Bd\ne\u2028f\u0085g");

            assertEquals("abcdefg", box.text());
        }

        @Test
        void aTextBoxDropsControlCharactersFromTextSetByCode() {
            final TextBox box = TextEditingTest.this.showBox("");

            box.setText("one\ttwo\u2029three");

            assertEquals("onetwothree", box.text());
        }

        @Test
        void aTextAreaBreaksLinesAtEveryLineSeparatorItIsGiven() {
            final TextArea area = TextEditingTest.this.showArea("");

            TextEditingTest.this.paste("a\u2028b\u2029c\u0085d\r\ne");

            assertEquals("a\nb\nc\nd\ne", area.text());
        }

        @Test
        void aTextAreaKeepsTabsButDropsOtherControlCharacters() {
            final TextArea area = TextEditingTest.this.showArea("");

            TextEditingTest.this.paste("a\tb\u0000c\u001Bd");

            assertEquals("a\tbcd", area.text());
        }

        @Test
        void aPasswordBoxRefusesAPasteWithAControlCharacterWhole() {
            final PasswordTextBox box = TextEditingTest.this.ui.passwordTextBox().setAnchor(Anchor.TOP_LEFT);
            TextEditingTest.this.test.show(box);
            box.requestFocus();

            TextEditingTest.this.paste("pa\tss");
            assertEquals(0, box.length(), "a tab is never typed, so a password with one is refused");

            TextEditingTest.this.paste("pass\r\n");
            assertEquals(4, box.length(), "line breaks are still removed");
        }
    }

    @Nested
    class PasswordEditsThatChangeNothing {
        private final List<String> heard = new ArrayList<>();
        private PasswordTextBox box;

        private void show() {
            this.box = TextEditingTest.this.ui.passwordTextBox().setAnchor(Anchor.TOP_LEFT);
            TextEditingTest.this.test.show(this.box);
            this.box.setPassword("abc".toCharArray());
            this.box.requestFocus();
            this.box.onEvent(TextEntry.MODIFY, event -> this.heard.add("modify"));
            this.box.onEvent(TextEntry.MODIFIED, event -> this.heard.add("modified"));
        }

        @Test
        void typingTheSelectedCharacterAgainFiresNothing() {
            this.show();
            this.box.select(1, 2);

            TextEditingTest.this.type("b");

            assertEquals(List.of(), this.heard);
        }

        @Test
        void pastingTheSelectedTextAgainFiresNothing() {
            this.show();
            this.box.select(1, 3);

            TextEditingTest.this.paste("bc");

            assertEquals(List.of(), this.heard);
        }

        @Test
        void typingAnotherCharacterOverTheSelectionStillFires() {
            this.show();
            this.box.select(1, 2);

            TextEditingTest.this.type("x");

            assertEquals(List.of("modify", "modified"), this.heard);
        }
    }

    @Nested
    class SettingAreaText {

        private void render() {
            TextEditingTest.this.test.host().render(new NullCanvas());
        }

        @Test
        void newTextShowsItsStartUntilTheCaretMoves() {
            final TextArea area = TextEditingTest.this.showArea("");
            final String lines = "one\ntwo\nthree\nfour\nfive\nsix\nseven\neight\nnine\nten";

            area.setText(lines);
            this.render();

            assertEquals(0f, area.scrollY(), "the view stays at the top");
            assertEquals(lines.length(), area.caret(), "with the caret at the end");

            TextEditingTest.this.press(Key.LEFT, Modifiers.NONE);
            this.render();
            assertTrue(area.scrollY() > 0f, "moving the caret brings it into view");
        }
    }

    @Nested
    class Characters {
        // A flag is two regional indicators, four chars
        private static final String FLAG = "\uD83C\uDDFA\uD83C\uDDF8";
        // A thumbs-up with a skin tone, four chars
        private static final String THUMB = "\uD83D\uDC4D\uD83C\uDFFD";

        @Test
        void leftAndRightStepOverAFlagWhole() {
            final TextBox box = TextEditingTest.this.showBox("a" + FLAG + "b");

            TextEditingTest.this.press(Key.LEFT, Modifiers.NONE);
            TextEditingTest.this.press(Key.LEFT, Modifiers.NONE);
            assertEquals(1, box.caret());

            TextEditingTest.this.press(Key.RIGHT, Modifiers.NONE);
            assertEquals(5, box.caret());
        }

        @Test
        void backspaceRemovesAFlagOrASkinToneWhole() {
            final TextBox box = TextEditingTest.this.showBox("a" + FLAG + THUMB);

            TextEditingTest.this.press(Key.BACKSPACE, Modifiers.NONE);
            assertEquals("a" + FLAG, box.text());
            TextEditingTest.this.press(Key.BACKSPACE, Modifiers.NONE);
            assertEquals("a", box.text());
        }

        @Test
        void anAccentTypedAfterItsLetterMovesAndDeletesWithIt() {
            final TextArea area = TextEditingTest.this.showArea("e\u0301x");

            TextEditingTest.this.press(Key.LEFT, Modifiers.NONE);
            TextEditingTest.this.press(Key.LEFT, Modifiers.NONE);
            assertEquals(0, area.caret());

            TextEditingTest.this.press(Key.DELETE, Modifiers.NONE);
            assertEquals("x", area.text());
        }

        @Test
        void aPasswordBoxStillStepsByCodePointLikeItsMask() {
            final PasswordTextBox box = TextEditingTest.this.ui.passwordTextBox().setAnchor(Anchor.TOP_LEFT);
            TextEditingTest.this.test.show(box);
            box.setPassword(("a" + FLAG).toCharArray());
            box.requestFocus();

            TextEditingTest.this.press(Key.BACKSPACE, Modifiers.NONE);

            assertEquals(2, box.length(), "one mask glyph, one code point, goes");
        }
    }

    @Nested
    class Words {

        @Test
        void controlArrowsStopAtPunctuation() {
            final TextBox box = TextEditingTest.this.showBox("hello, world");

            TextEditingTest.this.press(Key.LEFT, CONTROL);
            assertEquals(7, box.caret());
            TextEditingTest.this.press(Key.LEFT, CONTROL);
            assertEquals(5, box.caret());
            TextEditingTest.this.press(Key.LEFT, CONTROL);
            assertEquals(0, box.caret());

            TextEditingTest.this.press(Key.RIGHT, CONTROL);
            assertEquals(5, box.caret());
            TextEditingTest.this.press(Key.RIGHT, CONTROL);
            assertEquals(6, box.caret());
        }

        @Test
        void controlBackspaceDeletesTheWordAndLeavesItsPunctuation() {
            final TextBox box = TextEditingTest.this.showBox("path/to/file");

            TextEditingTest.this.press(Key.BACKSPACE, CONTROL);

            assertEquals("path/to/", box.text());
        }

        @Test
        void aDoubleClickSelectsAWordWithItsApostropheOrARunOfPunctuation() {
            final TextBox box = TextEditingTest.this.showBox("don't stop...now");

            TextEditingTest.this.doubleClickBoxAt(1);
            assertEquals("don't", box.selectedText());

            TextEditingTest.this.test.platform().advanceMillis(1000);
            TextEditingTest.this.doubleClickBoxAt(11);
            assertEquals("...", box.selectedText());
        }
    }

    @Nested
    class Presses {
        private static final String LONG = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJ";

        private void pressAt(final float x, final float y) {
            TextEditingTest.this.test.platform().advanceMillis(1000);
            TextEditingTest.this.test.host().pointerButton(x, y, MouseButton.LEFT, true, Modifiers.NONE);
            TextEditingTest.this.test.host().pointerButton(x, y, MouseButton.LEFT, false, Modifiers.NONE);
        }

        @Test
        void aPressOnLongTextLandsOnTheNearestBoundary() {
            final TextBox box = TextEditingTest.this.ui.textBox().setAnchor(Anchor.TOP_LEFT).setWidth(300);
            TextEditingTest.this.test.show(box);
            box.setText(LONG);

            this.pressAt(4f + 6f * 23f + 2f, 10f);
            assertEquals(23, box.caret());
            this.pressAt(4f + 6f * 23f + 3f, 10f);
            assertEquals(24, box.caret(), "halfway goes to the later boundary");
            this.pressAt(4f + 6f * 45f + 1f, 10f);
            assertEquals(45, box.caret());
            this.pressAt(290f, 10f);
            assertEquals(LONG.length(), box.caret());
        }

        @Test
        void aPressOnATextAreaRowLandsOnTheNearestBoundary() {
            final TextArea area = TextEditingTest.this.ui.textArea().setAnchor(Anchor.TOP_LEFT).setWidth(300);
            TextEditingTest.this.test.show(area);
            area.setText("one\n" + LONG);

            this.pressAt(4f + 6f * 30f + 2f, 4f + 9f + 4f);

            assertEquals(4 + 30, area.caret());
        }
    }
}
