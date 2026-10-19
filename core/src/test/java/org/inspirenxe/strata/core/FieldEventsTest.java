package org.inspirenxe.strata.core;

import org.inspirenxe.strata.api.control.attribute.text.TextInput;
import org.inspirenxe.strata.api.control.attribute.value.Ranged;
import org.inspirenxe.strata.api.control.field.Slider;
import org.inspirenxe.strata.api.control.field.Spinner;
import org.inspirenxe.strata.api.control.field.TextArea;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.event.control.ValueRequestEvent;
import org.inspirenxe.strata.api.extension.CustomComposite;
import org.inspirenxe.strata.api.extension.CustomContainer;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifier;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.input.ScrollKind;
import org.inspirenxe.strata.api.layout.AbsolutePanel;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleUnaryOperator;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The events of the field controls: each Pre event accepts, cancels and rewrites the user's change and never fires
 * for code, and each Post event reports changes from the user and from code with the value before. Text in the test
 * platform is 6 units per code point.
 */
class FieldEventsTest {
    private static final Modifiers CONTROL = Modifiers.of(Modifier.CONTROL);

    /**
     * Records what a text edit request sees and rewrites it with {@code rewrite}, or cancels it when that is null.
     */
    private static final class Edits {
        private final List<String> seen = new ArrayList<>();
        private final List<String> heldDuring = new ArrayList<>();
        private UnaryOperator<String> rewrite = UnaryOperator.identity();

        private void handle(final String held, final ValueRequestEvent<?, String> event) {
            this.seen.add(event.value());
            this.heldDuring.add(held);
            if (this.rewrite == null) {
                event.cancel();
            } else {
                event.setValue(this.rewrite.apply(event.value()));
            }
        }
    }

    /**
     * Records what a value change request sees and rewrites it with {@code rewrite}, or cancels it when that is null.
     */
    private static final class Changes {
        private final List<Double> seen = new ArrayList<>();
        private DoubleUnaryOperator rewrite = DoubleUnaryOperator.identity();

        private void handle(final ValueRequestEvent<?, Double> event) {
            this.seen.add(event.value());
            if (this.rewrite == null) {
                event.cancel();
            } else {
                event.setValue(this.rewrite.applyAsDouble(event.value()));
            }
        }
    }

    @Nested
    class TextBoxes {
        private final TestHost test = new TestHost(300, 100);
        private final Edits edits = new Edits();
        private final List<String> heard = new ArrayList<>();
        private final List<String> submitted = new ArrayList<>();
        private final TextBox box = this.test.factory().textBox().setWidth(200);
        private final AbsolutePanel form = this.test.factory().absolutePanel().add(this.box);

        TextBoxes() {
            this.box.onEvent(TextInput.EDIT, event -> this.edits.handle(event.control().text(), event))
                    .onEvent(TextInput.EDITED, event -> this.heard.add(event.previous() + "->" + event.value()));
            this.test.show(this.form);
            this.box.requestFocus();
        }

        private void type(final String text) {
            text.codePoints().forEach(code -> this.test.host().charTyped(code, Modifiers.NONE));
        }

        private boolean press(final Key key, final Modifiers modifiers) {
            final boolean handled = this.test.host().key(key, KeyAction.PRESS, modifiers);
            this.test.host().key(key, KeyAction.RELEASE, modifiers);
            return handled;
        }

        @Test
        void acceptedEditsApplyAndHandlersSeeTheOldText() {
            this.type("ab");
            assertEquals("ab", this.box.text());
            assertEquals(List.of("a", "ab"), this.edits.seen);
            assertEquals(List.of("", "a"), this.edits.heldDuring);
            assertEquals(List.of("->a", "a->ab"), this.heard);
        }

        @Test
        void deletingCuttingAndPastingFireEdit() {
            this.box.setText("hello");
            this.press(Key.BACKSPACE, Modifiers.NONE);
            this.box.select(0, 2);
            this.press(Key.X, CONTROL);
            this.test.platform().clipboard().set("XY");
            this.press(Key.V, CONTROL);
            assertEquals(List.of("hell", "ll", "XYll"), this.edits.seen);
            assertEquals("XYll", this.box.text());
        }

        @Test
        void canceledEditsChangeNothing() {
            this.box.setText("hello");
            this.heard.clear();
            this.box.select(1, 3);
            this.edits.rewrite = null;

            this.type("x");
            this.press(Key.DELETE, Modifiers.NONE);

            assertEquals(List.of("hxlo", "hlo"), this.edits.seen);
            assertEquals("hello", this.box.text());
            assertEquals(1, this.box.selectionStart());
            assertEquals(3, this.box.selectionEnd());
            assertEquals(List.of(), this.heard);
        }

        @Test
        void rewrittenTextIsCleanedLimitedAndKeepsTheCaretFromTheEnd() {
            this.box.setText("hello");
            this.box.select(2, 2);
            this.edits.rewrite = text -> "[" + text + "]";

            this.type("x");
            // "hexllo" with the caret 3 from the end becomes "[hexllo]" with the caret still 3 from the end
            assertEquals("[hexllo]", this.box.text());
            assertEquals(5, this.box.caret());
            assertEquals(5, this.box.selectionStart());

            this.box.setText("");
            this.box.setMaxLength(4);
            this.edits.rewrite = text -> text + "\nbcdef";
            this.type("a");
            assertEquals("abcd", this.box.text());
            assertEquals(List.of("hello->[hexllo]", "[hexllo]->", "->abcd"), this.heard.subList(1, this.heard.size()));
        }

        @Test
        void codeFiresEditedButNotEdit() {
            this.edits.rewrite = null;
            this.box.setText("from code");
            this.box.setMaxLength(4);
            assertEquals("from", this.box.text());
            assertEquals(List.of(), this.edits.seen);
            assertEquals(List.of("->from code", "from code->from"), this.heard);
        }

        @Test
        void editsThatChangeNothingFireNothing() {
            this.box.setMaxLength(2);
            this.type("ab");
            this.type("c");
            this.press(Key.BACKSPACE, Modifiers.NONE);
            this.box.select(0, 0);
            this.press(Key.BACKSPACE, Modifiers.NONE);
            assertEquals(List.of("a", "ab", "a"), this.edits.seen);
            this.box.setText("a");
            assertEquals(List.of("->a", "a->ab", "ab->a"), this.heard);
        }

        @Test
        void aFormCanPoliceEveryBoxInIt() {
            final List<TextInput<?>> targets = new ArrayList<>();
            this.form.onEvent(TextInput.EDIT, event -> {
                targets.add(event.control());
                if (!event.value().matches("\\d*")) {
                    event.cancel();
                }
            });
            this.type("1a2");
            assertEquals("12", this.box.text());
            assertEquals(List.of(this.box, this.box, this.box), targets);
        }

        @Test
        void enterSubmitsTheText() {
            this.box.setText("go");
            this.box.onEvent(TextBox.SUBMITTED, event -> this.submitted.add(event.previous() + "=" + event.value()));
            assertTrue(this.press(Key.ENTER, Modifiers.NONE));
            assertEquals(List.of("go=go"), this.submitted);
        }

        @Test
        void aCanceledSubmitStillConsumesEnter() {
            this.box.onEvent(TextBox.SUBMIT, event -> event.cancel())
                    .onEvent(TextBox.SUBMITTED, event -> this.submitted.add(event.value()));
            assertTrue(this.press(Key.ENTER, Modifiers.NONE));
            assertEquals(List.of(), this.submitted);
        }

        @Test
        void submitFiresWithoutAReportHandlerButEnterPassesOnWhenNothingListens() {
            final int[] submits = {0};
            assertFalse(this.press(Key.ENTER, Modifiers.NONE), "unheard Enter reaches the engine");

            this.form.onEvent(TextBox.SUBMIT, event -> submits[0]++);
            assertTrue(this.press(Key.ENTER, Modifiers.NONE));
            assertEquals(1, submits[0]);
        }
    }

    @Nested
    class TextAreas {
        private final TestHost test = new TestHost(300, 300);
        private final Edits edits = new Edits();
        private final List<String> heard = new ArrayList<>();
        private final TextArea area = this.test.factory().textArea().setWidth(100);
        private final AbsolutePanel form = this.test.factory().absolutePanel().add(this.area);

        TextAreas() {
            this.area.onEvent(TextInput.EDIT, event -> this.edits.handle(event.control().text(), event))
                    .onEvent(TextInput.EDITED, event -> this.heard.add(event.previous() + "->" + event.value()));
            this.test.show(this.form);
            this.area.requestFocus();
        }

        private void type(final String text) {
            text.codePoints().forEach(code -> this.test.host().charTyped(code, Modifiers.NONE));
        }

        private void press(final Key key) {
            this.test.host().key(key, KeyAction.PRESS, Modifiers.NONE);
            this.test.host().key(key, KeyAction.RELEASE, Modifiers.NONE);
        }

        @Test
        void typingAndEnterFireEdit() {
            this.type("a");
            this.press(Key.ENTER);
            this.press(Key.BACKSPACE);
            assertEquals(List.of("a", "a\n", "a"), this.edits.seen);
            assertEquals(List.of("", "a", "a\n"), this.edits.heldDuring);
            assertEquals("a", this.area.text());
            assertEquals(List.of("->a", "a->a\n", "a\n->a"), this.heard);
        }

        @Test
        void canceledEditsChangeNothing() {
            this.area.setText("ab\ncd");
            this.heard.clear();
            this.area.select(1, 4);
            this.edits.rewrite = null;

            this.type("x");
            this.press(Key.ENTER);

            assertEquals("ab\ncd", this.area.text());
            assertEquals(1, this.area.selectionStart());
            assertEquals(4, this.area.selectionEnd());
            assertEquals(List.of(), this.heard);
        }

        @Test
        void rewrittenTextIsNormalizedLimitedAndKeepsTheCaretFromTheEnd() {
            this.area.setText("ab");
            this.area.select(1, 1);
            this.edits.rewrite = text -> text.toUpperCase() + "\r\n";

            this.type("x");
            // "axb" with the caret 1 from the end becomes "AXB\n" with the caret still 1 from the end
            assertEquals("AXB\n", this.area.text());
            assertEquals(3, this.area.caret());

            this.area.setText("");
            this.area.setMaxLength(3);
            this.edits.rewrite = text -> text + "!!!";
            this.type("a");
            assertEquals("a!!", this.area.text());
        }

        @Test
        void codeFiresEditedButNotEdit() {
            this.edits.rewrite = null;
            this.area.setText("one\ntwo");
            this.area.setMaxLength(3);
            assertEquals("one", this.area.text());
            assertEquals(List.of(), this.edits.seen);
            assertEquals(List.of("->one\ntwo", "one\ntwo->one"), this.heard);
        }

        @Test
        void aFormHearsEdits() {
            final List<String> formHeard = new ArrayList<>();
            this.form.onEvent(TextInput.EDITED, event -> formHeard.add(event.value()));
            this.type("hi");
            assertEquals(List.of("h", "hi"), formHeard);
        }
    }

    @Nested
    class Sliders {
        // 150 wide: the thumb's center travels from x = 4 to x = 146, 142 units for the whole range
        private final TestHost test = new TestHost(200, 100);
        private final Changes changes = new Changes();
        private final List<String> heard = new ArrayList<>();
        private final Slider slider = this.test.factory().slider().setRange(0, 142);
        private final AbsolutePanel form = this.test.factory().absolutePanel().add(this.slider);

        Sliders() {
            this.slider.onEvent(Ranged.CHANGE, this.changes::handle)
                    .onEvent(Ranged.CHANGED, event -> this.heard.add(event.previous() + "->" + event.value()));
            this.test.show(this.form);
        }

        private void press(final float x) {
            this.test.host().pointerButton(x, 10, MouseButton.LEFT, true, Modifiers.NONE);
            this.test.host().pointerButton(x, 10, MouseButton.LEFT, false, Modifiers.NONE);
        }

        private void key(final Key key) {
            this.test.host().key(key, KeyAction.PRESS, Modifiers.NONE);
        }

        @Test
        void pressesDragsAndKeysFireChange() {
            this.press(54);
            assertEquals(50d, this.slider.value(), 1e-9);
            this.test.host().pointerButton(54, 10, MouseButton.LEFT, true, Modifiers.NONE);
            this.test.host().pointerMoved(64, 10, Modifiers.NONE);
            this.test.host().pointerButton(64, 10, MouseButton.LEFT, false, Modifiers.NONE);
            assertEquals(60d, this.slider.value(), 1e-9);
            this.slider.requestFocus();
            this.key(Key.HOME);
            assertEquals(0d, this.slider.value());
            assertEquals(3, this.changes.seen.size());
            assertEquals(3, this.heard.size());
            assertEquals(this.changes.seen.get(1) + "->0.0", this.heard.get(2));
        }

        @Test
        void aPressThatKeepsTheValueFiresNothing() {
            this.press(54);
            this.press(54);
            assertEquals(1, this.changes.seen.size());
            assertEquals(1, this.heard.size());
        }

        @Test
        void canceledChangesKeepTheValue() {
            this.changes.rewrite = null;
            this.press(54);
            this.slider.requestFocus();
            this.key(Key.END);
            assertEquals(0d, this.slider.value());
            assertEquals(List.of(), this.heard);
            assertEquals(List.of(142d), this.changes.seen.subList(1, 2));
        }

        @Test
        void rewrittenValuesAreClampedAndSnapped() {
            this.slider.setRange(0, 100).setStep(5);
            this.changes.rewrite = value -> value + 1000;
            this.slider.requestFocus();
            this.key(Key.RIGHT);
            assertEquals(5d, this.changes.seen.getFirst());
            assertEquals(100d, this.slider.value());

            this.changes.rewrite = value -> 41;
            this.key(Key.HOME);
            assertEquals(40d, this.slider.value());
            assertEquals(List.of("0.0->100.0", "100.0->40.0"), this.heard);
        }

        @Test
        void codeFiresChangedButNotChange() {
            this.changes.rewrite = null;
            this.slider.setValue(20);
            this.slider.setRange(0, 10);
            this.slider.setStep(4);
            assertEquals(10d, this.slider.value());
            assertEquals(List.of(), this.changes.seen);
            assertEquals(List.of("0.0->20.0", "20.0->10.0"), this.heard);
        }

        @Test
        void aFormCanRefuseChanges() {
            this.form.onEvent(Ranged.CHANGE, event -> event.cancel());
            this.press(54);
            assertEquals(0d, this.slider.value());
            assertEquals(1, this.changes.seen.size());
        }
    }

    @Nested
    class Spinners {
        private final TestHost test = new TestHost(300, 300);
        private final Changes changes = new Changes();
        private final List<String> heard = new ArrayList<>();
        private final Spinner spinner = this.test.factory().spinner();
        private final AbsolutePanel form = this.test.factory().absolutePanel().add(this.spinner);

        Spinners() {
            this.spinner.onEvent(Ranged.CHANGE, this.changes::handle)
                    .onEvent(Ranged.CHANGED, event -> this.heard.add(event.previous() + "->" + event.value()));
            this.test.show(this.form);
            this.test.click(10, 9);
        }

        private TextBox field() {
            return (TextBox) this.test.focused();
        }

        private void key(final Key key) {
            this.test.host().key(key, KeyAction.PRESS, Modifiers.NONE);
        }

        private void type(final String text) {
            text.codePoints().forEach(code -> this.test.host().charTyped(code, Modifiers.NONE));
        }

        @Test
        void keysButtonsWheelAndTypedTextFireChange() {
            this.key(Key.UP);
            this.key(Key.PAGE_UP);
            // Up button, then down button: the halves of the 12-unit column at the right of the 60-unit spinner
            this.test.click(54, 4);
            this.test.click(54, 15);
            this.test.host().scroll(30, 9, 0f, 1f, ScrollKind.WHEEL, Modifiers.NONE);
            this.test.click(10, 9);
            this.test.host().key(Key.A, KeyAction.PRESS, CONTROL);
            this.type("42");
            this.key(Key.ENTER);
            assertEquals(42d, this.spinner.value());
            assertEquals(List.of(1d, 11d, 12d, 11d, 12d, 42d), this.changes.seen);
            assertEquals(List.of("0.0->1.0", "1.0->11.0", "11.0->12.0", "12.0->11.0", "11.0->12.0", "12.0->42.0"),
                    this.heard);
        }

        @Test
        void canceledChangesKeepTheValueAndShowItAgain() {
            this.changes.rewrite = null;
            this.key(Key.UP);
            this.test.click(54, 4);
            assertEquals(0d, this.spinner.value());
            this.test.click(10, 9);
            this.key(Key.BACKSPACE);
            this.type("7");
            assertEquals("7", this.field().text());
            this.key(Key.ENTER);

            assertEquals(0d, this.spinner.value());
            assertEquals("0", this.field().text());
            assertEquals(List.of(1d, 1d, 7d), this.changes.seen);
            assertEquals(List.of(), this.heard);
        }

        @Test
        void rewrittenValuesAreClampedAndRounded() {
            this.spinner.setDecimals(1);
            this.changes.rewrite = value -> value * 1000;
            this.key(Key.UP);
            assertEquals(100d, this.spinner.value());
            assertEquals("100.0", this.field().text());

            this.changes.rewrite = value -> 2.26;
            this.key(Key.DOWN);
            assertEquals(2.3d, this.spinner.value());
            assertEquals(List.of("0.0->100.0", "100.0->2.3"), this.heard);
        }

        @Test
        void codeFiresChangedButNotChange() {
            this.changes.rewrite = null;
            this.spinner.setValue(5);
            this.spinner.increment();
            this.spinner.decrement();
            this.spinner.decrement();
            this.spinner.setRange(0, 3);
            assertEquals(3d, this.spinner.value());
            assertEquals(List.of(), this.changes.seen);
            assertEquals(List.of("0.0->5.0", "5.0->6.0", "6.0->5.0", "5.0->4.0", "4.0->3.0"), this.heard);
        }

        @Test
        void typedTextIsStillFilteredWithoutAPublicFilter() {
            this.key(Key.BACKSPACE);
            this.type("4x2");
            assertEquals("42", this.field().text());
        }

        @Test
        void aFormHearsTheSpinnerButNotItsField() {
            final List<String> outside = new ArrayList<>();
            final List<String> composite = new ArrayList<>();
            this.form.onEvent(TextInput.EDIT, event -> outside.add("edit " + event.value()))
                    .onEvent(TextInput.EDITED, event -> outside.add("edited " + event.value()))
                    .onEvent(TextBox.SUBMIT, event -> outside.add("submit"))
                    .onEvent(TextBox.SUBMITTED, event -> outside.add("submitted"))
                    .onEvent(Ranged.CHANGED, event -> outside.add("changed " + event.value()));
            this.spinner.onEvent(TextInput.EDITED, event -> composite.add(event.value()));

            this.key(Key.BACKSPACE);
            this.type("9");
            this.key(Key.ENTER);
            this.spinner.setValue(3);

            assertEquals(3d, this.spinner.value());
            assertEquals(List.of("changed 9.0", "changed 3.0"), outside);
            assertEquals(List.of("", "9", "3"), composite);
            assertInstanceOf(CustomComposite.class, this.spinner);
            assertFalse(this.spinner instanceof CustomContainer<?>);
        }
    }
}
