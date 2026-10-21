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
import org.inspirenxe.strata.api.control.field.ColorPicker;
import org.inspirenxe.strata.api.control.field.TextBox.Suggestion;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.event.control.ValueEvent;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifier;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.layout.AbsolutePanel;
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
 * Text box suggestions and the color picker: what the user can do with each, and their Pre requests
 * and Post reports.
 */
class InputControlsTest {
    private static final Modifiers CONTROL = Modifiers.of(Modifier.CONTROL);

    private final TestHost test = new TestHost(400, 300);
    private final ComponentFactory ui = this.test.factory();
    private final AbsolutePanel panel = this.ui.absolutePanel();

    private boolean key(final Key key) {
        return this.key(key, Modifiers.NONE);
    }

    private boolean key(final Key key, final Modifiers modifiers) {
        final boolean handled = this.test.host().key(key, KeyAction.PRESS, modifiers);
        this.test.host().key(key, KeyAction.RELEASE, modifiers);
        return handled;
    }

    private void type(final String text) {
        text.codePoints().forEach(codePoint -> this.test.host().charTyped(codePoint, Modifiers.NONE));
    }

    private void doubleClick(final float x, final float y) {
        this.test.click(x, y);
        this.test.click(x, y);
    }

    private static String change(final ValueEvent<?, ?> event) {
        return event.previous() + "->" + event.value();
    }

    @Nested
    class Suggestions {
        private final List<String> asked = new ArrayList<>();
        private final List<String> completed = new ArrayList<>();
        private final TextBox box = InputControlsTest.this.ui.textBox()
                .setWidth(200)
                .setSuggestionProvider(TextBox.SuggestionProvider.words(List.of("Ana", "Anders", "Ben", "Cleo", "Dev")))
                .onEvent(TextBox.COMPLETE, event -> this.asked.add(event.value().text()))
                .onEvent(TextBox.COMPLETED, event -> this.completed.add(event.value().text()));

        Suggestions() {
            InputControlsTest.this.test.show(InputControlsTest.this.panel.add(this.box, 0, 0));
            this.box.requestFocus();
        }

        private List<String> shown() {
            return this.box.shownSuggestions().stream().map(Suggestion::text).toList();
        }

        /**
         * Clicks a row of the open list: rows are a line plus 4 tall, starting at the bottom of the box.
         */
        private void clickRow(final int row) {
            InputControlsTest.this.test.host().layout();
            InputControlsTest.this.test.click(20, this.box.bounds().bottom() + 13 * row + 6);
        }

        @Test
        void typingShowsSuggestionsAndTabAcceptsTheFirst() {
            InputControlsTest.this.type("a");
            assertEquals(List.of("Ana", "Anders"), this.shown());
            assertTrue(InputControlsTest.this.key(Key.TAB));
            assertEquals("Ana", this.box.text());
            assertEquals(3, this.box.caret());
            assertEquals(List.of(), this.shown());
            assertEquals(List.of("Ana"), this.asked);
            assertEquals(List.of("Ana"), this.completed);
            assertSame(this.box, InputControlsTest.this.test.focused());
        }

        @Test
        void upAndDownWrapAndEnterAcceptsWithoutSubmitting() {
            final List<String> submitted = new ArrayList<>();
            this.box.onEvent(TextBox.SUBMITTED, event -> submitted.add(event.value()));
            InputControlsTest.this.type("a");
            InputControlsTest.this.key(Key.UP);
            InputControlsTest.this.key(Key.DOWN);
            InputControlsTest.this.key(Key.DOWN);
            assertTrue(InputControlsTest.this.key(Key.ENTER));
            assertEquals("Anders", this.box.text());
            assertEquals(List.of(), submitted);
            InputControlsTest.this.key(Key.ENTER);
            assertEquals(List.of("Anders"), submitted);
        }

        @Test
        void escapeClosesTheListAndIsConsumed() {
            InputControlsTest.this.type("b");
            assertEquals(List.of("Ben"), this.shown());
            assertTrue(InputControlsTest.this.key(Key.ESCAPE));
            assertEquals(List.of(), this.shown());
            assertTrue(InputControlsTest.this.ui.overlays().open().isEmpty());
            assertEquals("b", this.box.text());
        }

        @Test
        void aClickAcceptsTheRowAndKeepsFocusAndCaret() {
            InputControlsTest.this.type("an");
            this.clickRow(1);
            assertEquals("Anders", this.box.text());
            assertEquals(6, this.box.caret());
            assertSame(this.box, InputControlsTest.this.test.focused());
            assertEquals(List.of("Anders"), this.completed);
        }

        @Test
        void completeCanBeCanceledOrRewritten() {
            final boolean[] cancel = {true};
            this.box.onEvent(TextBox.COMPLETE, event -> {
                if (cancel[0]) {
                    event.cancel();
                } else {
                    event.setValue(new Suggestion("Dev", 0, 1));
                }
            });
            InputControlsTest.this.type("c");
            InputControlsTest.this.key(Key.TAB);
            assertEquals("c", this.box.text());
            assertEquals(List.of(), this.shown());
            assertEquals(List.of(), this.completed);

            cancel[0] = false;
            InputControlsTest.this.key(Key.SPACE, CONTROL);
            InputControlsTest.this.key(Key.TAB);
            assertEquals("Dev", this.box.text());
            assertEquals(List.of("Dev"), this.completed);
        }

        @Test
        void acceptingGoesThroughTheEditRequest() {
            final List<String> edits = new ArrayList<>();
            this.box.onEvent(TextInput.EDIT, event -> {
                edits.add(event.value());
                if (event.value().equals("Ben")) {
                    event.cancel();
                }
            });
            InputControlsTest.this.type("b");
            InputControlsTest.this.key(Key.TAB);
            assertEquals(List.of("b", "Ben"), edits);
            assertEquals("b", this.box.text());
            assertEquals(List.of("Ben"), this.asked);
            assertEquals(List.of(), this.completed);
        }

        @Test
        void movingTheCaretAndSetTextClose() {
            InputControlsTest.this.type("a");
            InputControlsTest.this.key(Key.LEFT);
            assertEquals(List.of(), this.shown());
            InputControlsTest.this.key(Key.END);
            InputControlsTest.this.key(Key.SPACE, CONTROL);
            assertEquals(List.of("Ana", "Anders"), this.shown());
            this.box.setText("Cleo");
            assertEquals(List.of(), this.shown());
        }

        @Test
        void controlSpaceShowsSuggestionsAndTypesNothing() {
            this.box.setText("De");
            InputControlsTest.this.key(Key.SPACE, CONTROL);
            InputControlsTest.this.test.host().charTyped(' ', CONTROL);
            assertEquals(List.of("Dev"), this.shown());
            assertEquals("De", this.box.text());
        }

        @Test
        void readOnlyBoxesShowNone() {
            this.box.setText("a").setReadOnly(true);
            this.box.showSuggestions();
            assertEquals(List.of(), this.shown());
        }

        @Test
        void wordsReplaceTheWordAroundTheCaret() {
            final TextBox.SuggestionProvider words = TextBox.SuggestionProvider.words(List.of("Ana", "Ben", "Benny"));
            assertEquals(List.of(new Suggestion("Ben", 4, 7), new Suggestion("Benny", 4, 7)), words.suggest("Ana bex", 5));
            assertEquals(List.of(new Suggestion("Benny", 0, 3)), words.suggest("Ben", 3));
            assertEquals(List.of(), words.suggest("Ana ", 4));
            assertThrows(IllegalArgumentException.class, () -> new Suggestion("x", 2, 1));
        }
    }

    @Nested
    class ColorPickers {
        private final List<Integer> asked = new ArrayList<>();
        private final List<String> picked = new ArrayList<>();
        private final ColorPicker picker = InputControlsTest.this.ui.colorPicker()
                .onEvent(ColorPicker.PICK, event -> this.asked.add(event.value()))
                .onEvent(ColorPicker.PICKED, event -> this.picked.add(hex(event.previous()) + "->" + hex(event.value())));

        ColorPickers() {
            InputControlsTest.this.test.show(InputControlsTest.this.panel.add(this.picker, 0, 0));
        }

        private static String hex(final int argb) {
            return Integer.toHexString(argb).toUpperCase();
        }

        private TextBox field() {
            return (TextBox) InputControlsTest.this.test.tabOrder().get(0);
        }

        private void press(final float x, final float y) {
            InputControlsTest.this.test.click(x, y);
        }

        @Test
        void setColorWorksOutHueAndKeepsItForGrays() {
            assertEquals(0xFFFFFFFF, this.picker.color());
            assertEquals("#FFFFFF", this.field().text());
            this.picker.setColor(0xFF00FF00);
            assertEquals(120f, this.picker.hue(), 1e-3);
            assertEquals(1f, this.picker.saturation(), 1e-6);
            this.picker.setColor(0xFF808080);
            assertEquals(120f, this.picker.hue(), 1e-3);
            assertEquals(0f, this.picker.saturation(), 1e-6);
            this.picker.setColor(0xFF000000);
            assertEquals(0f, this.picker.brightness(), 1e-6);
            this.picker.setColor(0xFF000000);
            assertEquals(List.of("FFFFFFFF->FF00FF00", "FF00FF00->FF808080", "FF808080->FF000000"), this.picked);
            assertEquals(List.of(), this.asked);
            assertEquals("#000000", this.field().text());
        }

        @Test
        void pressesInTheSquareAskToPick() {
            this.picker.setColor(0xFFFF0000);
            this.press(95.9f, 0);
            assertEquals(List.of(), this.asked);
            this.press(48, 48);
            assertEquals(List.of(0xFF804040), this.asked);
            assertEquals(0xFF804040, this.picker.color());
            assertEquals(0f, this.picker.hue(), 1e-6);
            assertEquals(0.5f, this.picker.saturation(), 1e-6);
            assertEquals(0.5f, this.picker.brightness(), 1e-6);
        }

        @Test
        void dragsFollowThePointerOutsideTheSquare() {
            this.picker.setColor(0xFFFF0000);
            InputControlsTest.this.test.host().pointerButton(48, 48, MouseButton.LEFT, true, Modifiers.NONE);
            InputControlsTest.this.test.host().pointerMoved(200, -50, Modifiers.NONE);
            InputControlsTest.this.test.host().pointerButton(200, -50, MouseButton.LEFT, false, Modifiers.NONE);
            assertEquals(0xFFFF0000, this.picker.color());
            assertEquals(List.of(0xFF804040, 0xFFFF0000), this.asked);
        }

        @Test
        void theHueOfAGrayMovesWithoutEvents() {
            this.picker.setColor(0xFF808080);
            this.picked.clear();
            this.press(105, 48);
            assertEquals(180f, this.picker.hue(), 1e-3);
            assertEquals(0xFF808080, this.picker.color());
            assertEquals(List.of(), this.asked);
            assertEquals(List.of(), this.picked);
        }

        @Test
        void pickCanBeCanceledOrRewritten() {
            final boolean[] cancel = {true};
            this.picker.onEvent(ColorPicker.PICK, event -> {
                if (cancel[0]) {
                    event.cancel();
                } else {
                    event.setValue(0xFF0000FF);
                }
            });
            this.press(48, 48);
            assertEquals(0xFFFFFFFF, this.picker.color());
            cancel[0] = false;
            this.press(48, 48);
            assertEquals(0xFF0000FF, this.picker.color());
            assertEquals(240f, this.picker.hue(), 1e-3);
            assertEquals(List.of("FFFFFFFF->FF0000FF"), this.picked);
        }

        @Test
        void theHexFieldTakesSixOrEightDigits() {
            final TextBox field = this.field();
            field.requestFocus();
            field.selectAll();
            InputControlsTest.this.type("zz00ff00");
            assertEquals("00ff00", field.text());
            InputControlsTest.this.key(Key.ENTER);
            assertEquals(0xFF00FF00, this.picker.color());
            assertEquals(List.of(0xFF00FF00), this.asked);
            assertEquals("#00FF00", field.text());

            field.selectAll();
            InputControlsTest.this.type("12");
            InputControlsTest.this.key(Key.ENTER);
            assertEquals("#00FF00", field.text());

            field.selectAll();
            InputControlsTest.this.type("#8000ff00");
            InputControlsTest.this.key(Key.ENTER);
            assertEquals(0x8000FF00, this.picker.color());
            this.picker.setAlphaShown(true);
            assertEquals("#8000FF00", field.text());
        }

        @Test
        void theAlphaStripSetsTheAlpha() {
            this.picker.setAlphaShown(true);
            InputControlsTest.this.test.host().layout();
            this.press(119, 95.9f);
            assertEquals(0x00FFFFFF, this.picker.color());
            this.picker.setAlphaShown(false);
            InputControlsTest.this.test.host().layout();
            this.press(48, 48);
            assertEquals(0x00804040, this.picker.color());
        }
    }
}
