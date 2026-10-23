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

import org.inspirenxe.strata.api.component.ComponentSnapshot;
import org.inspirenxe.strata.api.control.attribute.text.TextEntry;
import org.inspirenxe.strata.api.control.attribute.text.TextInput;
import org.inspirenxe.strata.api.control.field.PasswordTextBox;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.event.Event;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.SnapshotEvent;
import org.inspirenxe.strata.api.event.control.ValueEvent;
import org.inspirenxe.strata.api.event.control.ValueRequestEvent;
import org.inspirenxe.strata.api.event.input.InputEvents;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifier;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.input.TextInputKind;
import org.inspirenxe.strata.api.layout.AbsolutePanel;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.spi.ScreenHost;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The password box: what it keeps from events, snapshots, the clipboard and {@code toString}, and how it edits and
 * reveals. With the test platform the box is 200 by 20 at the top left, its text starts 4 units in, every code point
 * of the mask is 6 units wide, and the eye is a 9-unit square from x 187 to 196.
 */
class PasswordTextBoxTest {
    private static final Modifiers CONTROL = Modifiers.of(Modifier.CONTROL);
    private static final Modifiers ALT = Modifiers.of(Modifier.ALT);
    private static final String SECRET = "open sesame";

    private final TestHost test = new TestHost(300, 100);
    private final ScreenHost host = this.test.host();
    private final PasswordTextBox box = this.test.factory().passwordTextBox().setWidth(200);
    private final TextBox name = this.test.factory().textBox().setWidth(200);
    private final AbsolutePanel form = this.test.factory().absolutePanel().add(this.box).add(this.name, 0, 40);
    private final List<String> heard = new ArrayList<>();

    PasswordTextBoxTest() {
        this.test.show(this.form);
    }

    private void type(final String text) {
        text.codePoints().forEach(codePoint -> this.host.charTyped(codePoint, Modifiers.NONE));
    }

    private boolean press(final Key key, final Modifiers modifiers) {
        final boolean handled = this.host.key(key, KeyAction.PRESS, modifiers);
        this.host.key(key, KeyAction.RELEASE, modifiers);
        return handled;
    }

    /**
     * Clicks a number of times in a row, after long enough that the first click starts a new run.
     */
    private void clickAt(final float x, final int clicks) {
        this.test.platform().advanceMillis(1000L);
        for (int i = 0; i < clicks; i++) {
            this.host.pointerButton(x, 10, MouseButton.LEFT, true, Modifiers.NONE);
            this.host.pointerButton(x, 10, MouseButton.LEFT, false, Modifiers.NONE);
        }
    }

    private void clickEye() {
        // The eye shows or hides on the next layout, as on the next frame
        this.host.layout();
        this.host.pointerMoved(191, 10, Modifiers.NONE);
        this.clickAt(191, 1);
    }

    private String text() {
        return this.box.withPassword(String::new);
    }

    /**
     * Records every event of a type that reaches the form, by its type's name and its values.
     */
    private <E extends Event> void listen(final EventType<E> type) {
        this.form.onEvent(type, event -> {
            final StringBuilder line = new StringBuilder(type.name());
            if (event instanceof final ValueRequestEvent<?, ?> request) {
                line.append(' ').append(request.value());
            } else if (event instanceof final ValueEvent<?, ?> report) {
                line.append(' ').append(report.previous()).append("->").append(report.value());
            }
            this.heard.add(line.toString());
        });
    }

    private List<TextSpan> drawn() {
        final NullCanvas canvas = new NullCanvas();
        this.host.render(canvas);
        return canvas.texts();
    }

    // ------------------------------------------------------------------
    // What the box keeps to itself
    // ------------------------------------------------------------------

    @Test
    void typingReportsLengthsAndNoTextInputEvents() {
        this.listen(TextEntry.MODIFY);
        this.listen(TextEntry.MODIFIED);
        this.listen(TextInput.EDIT);
        this.listen(TextInput.EDITED);
        this.box.requestFocus();

        this.type("ab😀");
        this.press(Key.BACKSPACE, Modifiers.NONE);

        assertEquals(List.of("text_entry.modify", "text_entry.modified 0->1", "text_entry.modify",
                "text_entry.modified 1->2", "text_entry.modify", "text_entry.modified 2->3", "text_entry.modify",
                "text_entry.modified 3->2"), this.heard);
        assertEquals(2, this.box.length());
        assertEquals("ab", this.text());
    }

    @Test
    void aCanceledModifyLeavesTheTextAlone() {
        this.box.setPassword("ab".toCharArray());
        this.form.onEvent(TextEntry.MODIFY, event -> event.cancel());
        this.box.requestFocus();

        this.type("c");
        this.press(Key.BACKSPACE, Modifiers.NONE);

        assertEquals("ab", this.text());
        assertEquals(2, this.box.caret());
    }

    @Test
    void noEventValueSnapshotOrStringHoldsTheText() {
        final List<String> seen = new ArrayList<>();
        for (final EventType<?> type : List.of(TextEntry.MODIFY, TextEntry.MODIFIED, PasswordTextBox.REVEAL,
                PasswordTextBox.REVEALED, PasswordTextBox.SUBMIT, PasswordTextBox.SUBMITTED, InputEvents.KEY,
                InputEvents.POINTER_PRESS)) {
            this.form.onEvent(type, event -> {
                seen.add(String.valueOf(event));
                if (event instanceof final SnapshotEvent request) {
                    request.snapshotOf(this.box).map(ComponentSnapshot::toString).ifPresent(seen::add);
                }
                if (event instanceof final ValueEvent<?, ?> report) {
                    seen.add(String.valueOf(report.value()));
                    seen.add(String.valueOf(report.previous()));
                }
            });
        }
        this.box.onEvent(PasswordTextBox.SUBMITTED, event -> { });
        this.box.requestFocus();
        this.type(SECRET);
        this.press(Key.HOME, Modifiers.NONE);
        this.press(Key.ENTER, Modifiers.NONE);
        this.clickEye();
        this.press(Key.F8, ALT);
        this.clickAt(10, 1);

        assertFalse(seen.isEmpty());
        for (final String line : seen) {
            assertFalse(line.contains("sesame"), line);
        }
        assertFalse(this.box.toString().contains("sesame"));
        final ComponentSnapshot snapshot = this.snapshotOnKey();
        // The click at x 10 put the caret after the first mask glyph
        assertEquals(new PasswordTextBox.State(SECRET.length(), 1, 1, Integer.MAX_VALUE, false),
                snapshot.state(PasswordTextBox.State.class).orElseThrow());
    }

    private ComponentSnapshot snapshotOnKey() {
        final List<ComponentSnapshot> taken = new ArrayList<>();
        this.box.onEvent(InputEvents.KEY, event -> taken.add(event.snapshot()));
        this.press(Key.HOME, Modifiers.NONE);
        return taken.get(0);
    }

    @Test
    void copyAndCutLeaveTheClipboardAlone() {
        this.box.setPassword(SECRET.toCharArray());
        this.box.requestFocus();
        this.test.platform().clipboard().set("untouched");

        this.press(Key.A, CONTROL);
        this.press(Key.C, CONTROL);
        this.press(Key.X, CONTROL);
        this.box.setRevealed(true);
        this.press(Key.A, CONTROL);
        this.press(Key.C, CONTROL);
        this.press(Key.X, CONTROL);
        this.box.setReadOnly(true);
        this.press(Key.C, CONTROL);

        assertEquals("untouched", this.test.platform().clipboard().get().orElseThrow());
        assertEquals(SECRET, this.text());
    }

    @Test
    void typedCharactersStopAtTheBoxEvenWhenReadOnly() {
        final List<Integer> reached = new ArrayList<>();
        this.form.onEvent(InputEvents.CHAR, event -> reached.add(event.codePoint()));
        this.box.requestFocus();
        this.type("a");
        this.box.setReadOnly(true);
        this.type("b");
        this.host.charTyped('\t', Modifiers.NONE);

        assertEquals(List.of(), reached);
        assertEquals("a", this.text());
    }

    // ------------------------------------------------------------------
    // Reading and setting
    // ------------------------------------------------------------------

    @Test
    void copiesAreNewArraysAndWithPasswordZeroesItsCopy() {
        this.box.setPassword("abc".toCharArray());
        final char[] first = this.box.copyPassword();
        final char[] second = this.box.copyPassword();
        assertNotSame(first, second);
        assertArrayEquals("abc".toCharArray(), first);

        final char[][] kept = new char[1][];
        final int length = this.box.withPassword(chars -> {
            kept[0] = chars;
            return chars.length;
        });
        assertEquals(3, length);
        assertArrayEquals(new char[3], kept[0]);
        assertThrows(IllegalStateException.class, () -> this.box.withPassword(chars -> {
            kept[0] = chars;
            throw new IllegalStateException("reader failed");
        }));
        assertArrayEquals(new char[3], kept[0]);
    }

    @Test
    void setPasswordCopiesDropsLineBreaksAndCutsToTheLimit() {
        this.listen(TextEntry.MODIFY);
        this.listen(TextEntry.MODIFIED);
        final char[] given = "ab\r\ncdef".toCharArray();
        this.box.setMaxLength(4).setPassword(given);
        given[0] = 'x';

        assertEquals("abcd", this.text());
        assertEquals(4, this.box.caret());
        assertEquals(List.of("text_entry.modified 0->4"), this.heard);

        this.box.setPassword("abcd".toCharArray());
        this.box.clear();
        this.box.clear();
        assertTrue(this.box.isEmpty());
        assertEquals(List.of("text_entry.modified 0->4", "text_entry.modified 4->0"), this.heard);
    }

    @Test
    void matchesComparesTwoBoxes() {
        final PasswordTextBox confirm = this.test.factory().passwordTextBox().setPassword("abc".toCharArray());
        this.box.setPassword("abc".toCharArray());
        assertTrue(this.box.matches(confirm));
        confirm.setPassword("abd".toCharArray());
        assertFalse(this.box.matches(confirm));
        confirm.setPassword("abcd".toCharArray());
        assertFalse(this.box.matches(confirm));
        confirm.clear();
        this.box.clear();
        assertTrue(this.box.matches(confirm));
    }

    @Test
    void theMaskIsSettableAndChecked() {
        this.box.setPassword("abc".toCharArray()).setMaskCodePoint('•');
        assertEquals(List.of("•••"), this.drawn().stream().map(TextSpan::text).toList());
        assertThrows(IllegalArgumentException.class, () -> this.box.setMaskCodePoint(-1));
    }

    @Test
    void thePlaceholderIsNotMasked() {
        this.box.setPlaceholder("Password");
        assertEquals(List.of("Password"), this.drawn().stream().map(TextSpan::text).toList());
    }

    // ------------------------------------------------------------------
    // Editing
    // ------------------------------------------------------------------

    @Test
    void wordMovementTreatsTheTextAsOneWord() {
        this.box.setPassword(SECRET.toCharArray());
        this.box.requestFocus();
        this.press(Key.LEFT, CONTROL);
        assertEquals(0, this.box.caret());
        this.press(Key.RIGHT, CONTROL);
        assertEquals(SECRET.length(), this.box.caret());

        this.clickAt(4 + 6 * 2, 2);
        assertEquals(0, this.box.selectionStart());
        assertEquals(SECRET.length(), this.box.selectionEnd());

        this.press(Key.END, Modifiers.NONE);
        this.press(Key.BACKSPACE, CONTROL);
        assertTrue(this.box.isEmpty());
    }

    @Test
    void aClickPlacesTheCaretOnTheNearestMaskBoundary() {
        this.box.setPassword("abcd".toCharArray());
        this.clickAt(4 + 6 * 2 + 2, 1);
        assertEquals(2, this.box.caret());
        this.clickAt(4 + 6 * 3 - 1, 1);
        assertEquals(3, this.box.caret());
        this.clickAt(150, 1);
        assertEquals(4, this.box.caret());
    }

    @Test
    void theFilterAndTheLengthLimitApplyToTyping() {
        this.box.setMaxLength(3).setCodePointFilter(Character::isDigit);
        this.box.requestFocus();
        this.type("1a23");
        this.type("4");
        assertEquals("123", this.text());
    }

    @Test
    void aPasteTheLimitOrTheFilterWouldChangeIsRefusedWhole() {
        this.listen(TextEntry.MODIFY);
        this.box.setMaxLength(6).setCodePointFilter(Character::isDigit);
        this.box.requestFocus();

        this.test.platform().clipboard().set("12\r\n34");
        this.press(Key.V, CONTROL);
        assertEquals("1234", this.text());

        this.test.platform().clipboard().set("567");
        this.press(Key.V, CONTROL);
        assertEquals("1234", this.text());

        this.test.platform().clipboard().set("5a");
        this.press(Key.V, CONTROL);
        assertEquals("1234", this.text());

        this.press(Key.A, CONTROL);
        this.test.platform().clipboard().set("567890");
        this.press(Key.V, CONTROL);
        assertEquals("567890", this.text());
        assertEquals(List.of("text_entry.modify", "text_entry.modify"), this.heard);
    }

    @Test
    void focusAsksTheEngineForSecretTextInput() {
        this.box.requestFocus();
        assertEquals(TextInputKind.SECRET, this.test.platform().textInputKind());
        this.name.requestFocus();
        assertEquals(TextInputKind.TEXT, this.test.platform().textInputKind());
        this.box.requestFocus();
        assertEquals(TextInputKind.SECRET, this.test.platform().textInputKind());
    }

    @Test
    void enterSubmitsWithoutAValue() {
        this.listen(PasswordTextBox.SUBMIT);
        this.listen(PasswordTextBox.SUBMITTED);
        this.box.setPassword("abc".toCharArray());
        this.box.requestFocus();
        assertTrue(this.press(Key.ENTER, Modifiers.NONE));
        assertEquals(List.of("password_text_box.submit", "password_text_box.submitted"), this.heard);
    }

    // ------------------------------------------------------------------
    // Peeking
    // ------------------------------------------------------------------

    @Test
    void theEyeAndAltF8RevealAndHide() {
        this.listen(PasswordTextBox.REVEAL);
        this.listen(PasswordTextBox.REVEALED);
        this.box.requestFocus();
        this.type("abc");

        this.host.layout();
        this.host.pointerMoved(191, 10, Modifiers.NONE);
        assertEquals(List.of("***", "Show password"), this.drawn().stream().map(TextSpan::text).toList());

        this.clickEye();
        assertTrue(this.box.isRevealed());
        assertSame(this.box, this.test.focused(), "a press on the eye leaves focus in the box");
        assertEquals(3, this.box.caret(), "a press on the eye leaves the caret alone");
        assertEquals(List.of("abc", "Hide password"), this.drawn().stream().map(TextSpan::text).toList());

        this.host.pointerMoved(100, 80, Modifiers.NONE);
        this.press(Key.F8, ALT);
        assertFalse(this.box.isRevealed());
        assertEquals(List.of("***"), this.drawn().stream().map(TextSpan::text).toList());
        assertEquals(List.of("password_text_box.reveal true", "password_text_box.revealed false->true",
                "password_text_box.reveal false", "password_text_box.revealed true->false"), this.heard);
    }

    @Test
    void aCanceledOrRewrittenRevealKeepsTheState() {
        this.box.requestFocus();
        this.type("abc");
        this.box.onEvent(PasswordTextBox.REVEAL, event -> event.cancel());
        this.clickEye();
        this.press(Key.F8, ALT);
        assertFalse(this.box.isRevealed());
    }

    @Test
    void theBoxHidesOnFocusLossDisablingAndBecomingUnrevealable() {
        this.listen(PasswordTextBox.REVEALED);
        this.box.requestFocus();
        this.type("abc");

        this.press(Key.F8, ALT);
        this.name.requestFocus();
        assertFalse(this.box.isRevealed());

        this.box.setRevealed(true);
        this.box.setEnabled(false);
        assertFalse(this.box.isRevealed());

        this.box.setEnabled(true).setRevealed(true);
        this.box.setRevealable(false);
        assertFalse(this.box.isRevealed());
        assertEquals(List.of("password_text_box.revealed false->true", "password_text_box.revealed true->false",
                "password_text_box.revealed false->true", "password_text_box.revealed true->false",
                "password_text_box.revealed false->true", "password_text_box.revealed true->false"), this.heard);
    }

    @Test
    void anUnrevealableBoxIgnoresAltF8ButCodeCanStillReveal() {
        this.box.setRevealable(false);
        this.box.requestFocus();
        this.type("abc");
        this.press(Key.F8, ALT);
        assertFalse(this.box.isRevealed());
        this.box.setRevealed(true);
        assertTrue(this.box.isRevealed());
    }

    @Test
    void textSetFromCodeCannotBeRevealedUntilTheUserReplacesIt() {
        this.box.setPassword("remembered".toCharArray());
        this.box.requestFocus();

        this.press(Key.F8, ALT);
        this.clickEye();
        assertFalse(this.box.isRevealed());

        this.type("x");
        this.press(Key.F8, ALT);
        assertFalse(this.box.isRevealed(), "adding to the text keeps it guarded");

        this.press(Key.A, CONTROL);
        this.type("n");
        this.press(Key.F8, ALT);
        assertTrue(this.box.isRevealed(), "replacing all of it lifts the guard");

        this.box.setPassword("again".toCharArray());
        assertFalse(this.box.isRevealed(), "text set from code hides");
        this.box.setRevealed(true);
        assertTrue(this.box.isRevealed(), "code may still reveal it");
    }

    @Test
    void anEmptyBoxCannotBeRevealedByTheUser() {
        this.box.requestFocus();
        this.clickEye();
        assertFalse(this.box.isRevealed());
        this.press(Key.F8, ALT);
        assertFalse(this.box.isRevealed());
        assertSame(this.box, this.test.focused(), "the click landed on the box, which has no eye while empty");
    }
}
