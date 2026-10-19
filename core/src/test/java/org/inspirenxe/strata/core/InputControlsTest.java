package org.inspirenxe.strata.core;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.control.attribute.item.SingleSelection;
import org.inspirenxe.strata.api.control.attribute.text.TextInput;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.collection.DualList.Move;
import org.inspirenxe.strata.api.control.collection.DualList.Side;
import org.inspirenxe.strata.api.control.collection.DualList;
import org.inspirenxe.strata.api.control.field.ColorPicker;
import org.inspirenxe.strata.api.control.field.TextBox.Suggestion;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.event.control.ValueEvent;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifier;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.layout.AbsolutePanel;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;
import org.inspirenxe.strata.core.node.ContainerNode;
import org.inspirenxe.strata.core.node.Node;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Text box suggestions, the dual list and the color picker: what the user can do with each, and their Pre requests
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
        void readOnlyAndPasswordBoxesShowNone() {
            this.box.setText("a").setPassword(true);
            this.box.showSuggestions();
            assertEquals(List.of(), this.shown());
            this.box.setPassword(false).setReadOnly(true);
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
    class DualLists {
        private final List<String> moves = new ArrayList<>();
        private final List<String> moved = new ArrayList<>();
        private final List<String> selected = new ArrayList<>();
        private final DualList<String> dual = InputControlsTest.this.ui.<String>dualList()
                .setItems(Side.LEFT, List.of("Ana", "Ben", "Cleo"))
                .setItems(Side.RIGHT, List.of("Dev"))
                .onEvent(DualList.MOVE, event -> this.moves.add(describe(event.value())))
                .onEvent(DualList.MOVED, event -> this.moved.add(describe(event.previous()) + "->" + describe(event.value())))
                .onEvent(SingleSelection.SELECTED, event -> this.selected.add(change(event)));

        DualLists() {
            InputControlsTest.this.test.show(InputControlsTest.this.panel.add(this.dual, 0, 0));
        }

        private static String describe(final Move<?> move) {
            return move.item() + "@" + move.side() + move.index();
        }

        /**
         * The buttons in the order they are added: move right, move all right, move left, move all left.
         */
        private Component<?> button(final int index) {
            final List<Component<?>> buttons = new ArrayList<>();
            for (final Node child : ((ContainerNode) ((ComponentFactoryImpl) InputControlsTest.this.ui).nodeOf(this.dual)).childNodes()) {
                final Object facade = child.facade();
                if (facade instanceof Button button) {
                    buttons.add(button);
                }
            }
            return buttons.get(index);
        }

        private void clickButton(final int index) {
            final Rect bounds = this.button(index).bounds();
            InputControlsTest.this.test.click(bounds.x() + bounds.width() / 2f, bounds.y() + bounds.height() / 2f);
        }

        private float rowY(final int row) {
            return 20f * row + 10f;
        }

        private float rightX() {
            return this.dual.bounds().right() - 10f;
        }

        @Test
        void selectionSpansBothListsWithLeftItemsFirst() {
            final List<Integer> asked = new ArrayList<>();
            this.dual.onEvent(SingleSelection.SELECT, event -> asked.add(event.value()));
            InputControlsTest.this.test.click(10, this.rowY(1));
            assertEquals(1, this.dual.selectedIndex());
            assertEquals(Optional.of("Ben"), this.dual.selected());
            InputControlsTest.this.test.click(this.rightX(), this.rowY(0));
            assertEquals(3, this.dual.selectedIndex());
            assertEquals(Optional.of(Side.RIGHT), this.dual.selectedSide());
            assertEquals(List.of(1, 3), asked);
            assertEquals(List.of("-1->1", "1->3"), this.selected);
        }

        @Test
        void buttonsMoveTheSelectionAndItFollowsToTheNextItem() {
            assertFalse(this.button(0).isEnabled());
            assertTrue(this.button(1).isEnabled());
            assertFalse(this.button(2).isEnabled());
            InputControlsTest.this.test.click(10, this.rowY(1));
            assertTrue(this.button(0).isEnabled());
            this.clickButton(0);
            assertEquals(List.of("Ana", "Cleo"), this.dual.items(Side.LEFT));
            assertEquals(List.of("Dev", "Ben"), this.dual.items(Side.RIGHT));
            assertEquals(List.of("Ben@RIGHT1"), this.moves);
            assertEquals(List.of("Ben@LEFT1->Ben@RIGHT1"), this.moved);
            assertEquals(Optional.of("Cleo"), this.dual.selected());
            this.clickButton(0);
            assertEquals(List.of("Ana"), this.dual.items(Side.LEFT));
            assertEquals(Optional.of("Ana"), this.dual.selected());
        }

        @Test
        void aDoubleClickOrEnterMovesARow() {
            InputControlsTest.this.doubleClick(this.rightX(), this.rowY(0));
            assertEquals(List.of("Ana", "Ben", "Cleo", "Dev"), this.dual.items(Side.LEFT));
            assertEquals(List.of(), this.dual.items(Side.RIGHT));
            InputControlsTest.this.test.click(10, this.rowY(0));
            InputControlsTest.this.key(Key.ENTER);
            assertEquals(List.of("Ana"), this.dual.items(Side.RIGHT));
            assertEquals(List.of("Dev@LEFT3", "Ana@RIGHT0"), this.moves);
        }

        @Test
        void aMoveCanBeCanceledOrRewritten() {
            final boolean[] cancel = {true};
            this.dual.onEvent(DualList.MOVE, event -> {
                if (cancel[0]) {
                    event.cancel();
                } else {
                    event.setValue(new Move<>(event.value().item(), Side.RIGHT, 0));
                }
            });
            InputControlsTest.this.test.click(10, this.rowY(1));
            this.clickButton(0);
            assertEquals(List.of("Ana", "Ben", "Cleo"), this.dual.items(Side.LEFT));
            assertEquals(List.of(), this.moved);
            cancel[0] = false;
            this.clickButton(0);
            assertEquals(List.of("Ben", "Dev"), this.dual.items(Side.RIGHT));
            assertEquals(List.of("Ben@LEFT1->Ben@RIGHT0"), this.moved);
        }

        @Test
        void moveAllAsksOncePerItem() {
            this.dual.onEvent(DualList.MOVE, event -> {
                if (event.value().item().equals("Ben")) {
                    event.cancel();
                }
            });
            this.clickButton(1);
            assertEquals(List.of("Ben"), this.dual.items(Side.LEFT));
            assertEquals(List.of("Dev", "Ana", "Cleo"), this.dual.items(Side.RIGHT));
            assertEquals(List.of("Ana@RIGHT1", "Ben@RIGHT2", "Cleo@RIGHT2"), this.moves);
        }

        @Test
        void codeMovesReportWithoutAsking() {
            this.dual.move("Ana", Side.RIGHT);
            this.dual.move("Ana", Side.RIGHT);
            assertEquals(List.of("Ana@LEFT0->Ana@RIGHT1"), this.moved);
            this.dual.moveAll(Side.LEFT);
            assertEquals(List.of("Ben", "Cleo", "Dev", "Ana"), this.dual.items(Side.LEFT));
            assertEquals(List.of(), this.moves);
            assertEquals(3, this.moved.size());
        }

        @Test
        void setItemsKeepsTheSelectionAndReportsShiftedIndexes() {
            this.dual.selectItem("Dev");
            assertEquals(3, this.dual.selectedIndex());
            this.dual.setItems(Side.LEFT, List.of("Ana"));
            assertEquals(1, this.dual.selectedIndex());
            this.dual.setItems(Side.RIGHT, List.of("Eve"));
            assertEquals(-1, this.dual.selectedIndex());
            assertEquals(List.of("-1->3", "3->1", "1->-1"), this.selected);
            assertThrows(IndexOutOfBoundsException.class, () -> this.dual.select(2));
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
            return (TextBox) InputControlsTest.this.test.tabOrder().getFirst();
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
