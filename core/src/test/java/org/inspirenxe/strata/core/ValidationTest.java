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
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.Tooltip;
import org.inspirenxe.strata.api.component.Validation;
import org.inspirenxe.strata.api.control.attribute.text.HasText;
import org.inspirenxe.strata.api.control.attribute.text.TextInput;
import org.inspirenxe.strata.api.control.attribute.value.Validatable;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.button.CheckBox;
import org.inspirenxe.strata.api.control.button.CheckState;
import org.inspirenxe.strata.api.control.collection.ComboBox;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.control.field.ColorPicker;
import org.inspirenxe.strata.api.control.field.DatePicker;
import org.inspirenxe.strata.api.control.field.DateTimePicker;
import org.inspirenxe.strata.api.control.field.PasswordTextBox;
import org.inspirenxe.strata.api.control.field.Slider;
import org.inspirenxe.strata.api.control.field.Spinner;
import org.inspirenxe.strata.api.control.field.TextArea;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.control.field.TimePicker;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.state.Converters;
import org.inspirenxe.strata.api.state.State;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Themes;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;
import org.inspirenxe.strata.core.text.Translations;
import org.inspirenxe.strata.spi.Translator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code Validation}, {@code Validatable} on Strata's controls, conversion errors from bindings, and how an invalid
 * control looks (design 4.43).
 */
class ValidationTest {
    private static final Validation NAME_MISSING = Validation.error(Text.of("Enter a name"));

    private final TestHost test = new TestHost(300, 200);
    private final ComponentFactory ui = this.test.factory();

    @BeforeEach
    void english() {
        Translations.setTranslator(new LocaleTranslator(Locale.US));
    }

    @AfterEach
    void resetTranslator() {
        Translations.setTranslator(Translator.NONE);
    }

    private static List<String> validated(final Validatable<?> control) {
        final List<String> seen = new ArrayList<>();
        control.onEvent(Validatable.VALIDATED, event -> seen.add(event.previous().severity() + " > "
                + event.value().severity() + " " + event.value().message().plain()));
        return seen;
    }

    private void type(final String text) {
        text.codePoints().forEach(codePoint -> this.test.host().charTyped(codePoint, Modifiers.NONE));
    }

    @Test
    void validationsSayWhetherTheValueMayBeUsed() {
        assertTrue(Validation.VALID.isValid());
        assertEquals(Validation.Severity.NONE, Validation.VALID.severity());
        assertTrue(Validation.warning(Text.of("Weak")).isValid());
        assertFalse(NAME_MISSING.isValid());
        assertEquals(Validation.Severity.ERROR, NAME_MISSING.severity());
        assertThrows(NullPointerException.class, () -> new Validation(null, Text.empty()));
    }

    @Nested
    class Sources {
        @Test
        void setValidationReportsEachChange() {
            final TextBox box = ValidationTest.this.ui.textBox();
            final List<String> seen = validated(box);
            assertSame(box, box.setValidation(NAME_MISSING));
            box.setValidation(Validation.error(Text.of("Enter a name")));
            box.setValidation(Validation.VALID);
            assertEquals(List.of("NONE > ERROR Enter a name", "ERROR > NONE "), seen);
            assertEquals(Validation.VALID, box.validation());
        }

        @Test
        void aValidatorRunsAtOnceAndAfterEachChange() {
            final TextBox box = ValidationTest.this.ui.textBox();
            ValidationTest.this.test.show(box);
            box.setValidator(field -> field.isEmpty() ? NAME_MISSING : Validation.VALID);
            assertEquals(NAME_MISSING, box.validation());
            box.requestFocus();
            ValidationTest.this.type("Ana");
            assertEquals(Validation.VALID, box.validation());
            box.clear();
            assertEquals(NAME_MISSING, box.validation());

            box.setValidator(null);
            box.setText("Ben");
            assertEquals(NAME_MISSING, box.validation());
        }

        @Test
        void aValidatorReturningNullThrows() {
            final TextBox box = ValidationTest.this.ui.textBox();
            assertThrows(NullPointerException.class, () -> box.setValidator(field -> null));
        }

        @Test
        void everyValidatableControlRunsItsValidatorAfterItsValueChanges() {
            final ComponentFactory ui = ValidationTest.this.ui;
            check(ui.textArea(), area -> area.setText("x"));
            check(ui.passwordTextBox(), box -> box.setPassword("x".toCharArray()));
            check(ui.spinner(), spinner -> spinner.setValue(3));
            check(ui.slider(), slider -> slider.setValue(0.5));
            check(ui.checkBox(), box -> box.setState(CheckState.CHECKED));
            final ComboBox<String> combo = ui.<String>comboBox().setItems(List.of("a", "b"));
            check(combo, box -> box.select(1));
            check(ui.datePicker(), picker -> picker.setDate(LocalDate.of(2026, 10, 22)));
            check(ui.timePicker(), picker -> picker.setTime(LocalTime.of(9, 30)));
            check(ui.dateTimePicker(), picker -> picker.setDateTime(LocalDateTime.of(2026, 10, 22, 9, 30)));
            check(ui.colorPicker(), picker -> picker.setColor(0xFF336699));
        }

        private static <C extends Validatable<C>> void check(final C control, final Consumer<C> change) {
            final int[] asked = new int[1];
            control.setValidator(c -> {
                asked[0]++;
                return asked[0] > 1 ? NAME_MISSING : Validation.VALID;
            });
            assertEquals(1, asked[0], control.getClass().getSimpleName());
            change.accept(control);
            assertEquals(2, asked[0], control.getClass().getSimpleName());
            assertEquals(NAME_MISSING, control.validation(), control.getClass().getSimpleName());
        }
    }

    @Nested
    class Conversions {
        private final State<Integer> quantity = State.of(5);
        private TextBox box;

        @BeforeEach
        void bind() {
            this.box = ValidationTest.this.ui.textBox();
            this.box.bind(TextInput.TEXT.convert(Converters.integer(1, 64)), this.quantity);
            ValidationTest.this.test.show(this.box);
            this.box.requestFocus();
        }

        private void retype(final String text) {
            this.box.selectAll();
            ValidationTest.this.type(text);
        }

        @Test
        void aFailedConversionMarksTheFieldUntilItConvertsAgain() {
            final List<String> seen = validated(this.box);
            this.retype("x");
            assertEquals(Validation.error(Text.translatable("strata.validation.integer_range", "1", "64")),
                    this.box.validation());
            assertEquals("Enter a whole number from 1 to 64", this.box.validation().message().plain());
            this.retype("7");
            assertEquals(Validation.VALID, this.box.validation());
            assertEquals(7, this.quantity.get());
            assertEquals(List.of("NONE > ERROR Enter a whole number from 1 to 64", "ERROR > NONE "), seen);
        }

        @Test
        void winsOverTheValidatorWhileItLasts() {
            this.box.setValidator(field -> Validation.warning(Text.of("Odd")));
            this.retype("abc");
            assertEquals(Validation.Severity.ERROR, this.box.validation().severity());
            this.retype("9");
            assertEquals(Validation.warning(Text.of("Odd")), this.box.validation());
        }

        @Test
        void aChangeOfTheStateMakesTheFieldValidAgain() {
            this.retype("abc");
            this.quantity.set(12);
            ValidationTest.this.test.host().flush();
            assertEquals("12", this.box.text());
            assertEquals(Validation.VALID, this.box.validation());
        }

        @Test
        void unbindingTakesTheErrorAway() {
            this.retype("abc");
            this.box.unbind(TextInput.TEXT);
            assertEquals(Validation.VALID, this.box.validation());
        }

        @Test
        void aFormCombinesItsFieldsThroughAState() {
            ValidationTest.this.test.host().close();
            final State<Validation> quantityCheck = State.of(Validation.VALID);
            final Button save = ValidationTest.this.ui.button("Save");
            final Label hint = ValidationTest.this.ui.label("");
            this.box.bind(Validatable.VALIDATION, quantityCheck);
            save.bind(Component.ENABLED, quantityCheck.map(Validation::isValid));
            hint.bind(HasText.TEXT, quantityCheck.map(Validation::message));
            ValidationTest.this.test.show(ValidationTest.this.ui.stackPanel().add(this.box).add(save).add(hint));
            this.box.requestFocus();

            this.retype("100");
            ValidationTest.this.test.host().flush();
            assertFalse(quantityCheck.get().isValid());
            assertFalse(save.isEnabled());
            assertEquals("Enter a whole number from 1 to 64", hint.text().plain());

            this.retype("10");
            ValidationTest.this.test.host().flush();
            assertTrue(save.isEnabled());
            assertEquals("", hint.text().plain());
        }
    }

    @Nested
    class Looks {
        @Test
        void anErrorDrawsTheInvalidFaceWhenTheThemeHasOne() {
            final TestHost host = new TestHost(new TestPlatform(),
                    Themes.flat().toBuilder().paint(TextBox.STYLE_INVALID, Paint.solid(0xFF800000)).build(), 300, 200);
            final TextBox box = host.factory().textBox().setAnchor(Anchor.TOP_LEFT).setWidth(100)
                    .setValidation(NAME_MISSING);
            host.show(box);
            final DrawLog log = new DrawLog();
            host.host().render(log);
            assertEquals("fill 0,0 100x20 #FF800000", log.lines("fill").get(0));
            assertTrue(log.lines("fill").stream().noneMatch(line -> line.endsWith("#FFE04848")));
        }

        @Test
        void otherwiseAnOutlineInTheErrorOrWarningColor() {
            final TestHost host = new TestHost(300, 200);
            final TextBox box = host.factory().textBox().setAnchor(Anchor.TOP_LEFT).setWidth(100)
                    .setValidation(NAME_MISSING);
            host.show(box);
            DrawLog log = new DrawLog();
            host.host().render(log);
            assertEquals(List.of("fill 0,0 100x1 #FFE04848", "fill 0,19 100x1 #FFE04848", "fill 0,1 1x18 #FFE04848",
                    "fill 99,1 1x18 #FFE04848"), log.lines("fill").stream().filter(l -> l.endsWith("#FFE04848")).toList());

            box.setValidation(Validation.warning(Text.of("Weak")));
            log = new DrawLog();
            host.host().render(log);
            assertEquals(4, log.lines("fill").stream().filter(l -> l.endsWith("#FFE0A030")).count());

            box.setEnabled(false);
            log = new DrawLog();
            host.host().render(log);
            assertEquals(0, log.lines("fill").stream().filter(l -> l.endsWith("#FFE0A030")).count());
        }

        @Test
        void theTooltipShowsTheMessageFirst() {
            final TextBox box = ValidationTest.this.ui.textBox();
            final var node = ((ComponentFactoryImpl) ValidationTest.this.ui).nodeOf(box);
            assertNull(node.shownTooltip());
            box.setValidation(NAME_MISSING);
            assertEquals(Tooltip.text(Text.of("Enter a name")), node.shownTooltip());
            box.setTooltip(Tooltip.text("Your full name"));
            assertEquals(Tooltip.lines(List.of(Text.of("Enter a name"), Text.of("Your full name"))), node.shownTooltip());
            assertEquals(Optional.of(Tooltip.text("Your full name")), box.tooltip());
            assertSame(node.shownTooltip(), node.shownTooltip());
            box.setValidation(Validation.VALID);
            assertEquals(Tooltip.text("Your full name"), node.shownTooltip());

            box.setTooltip(Tooltip.of(factory -> factory.label("custom")));
            box.setValidation(NAME_MISSING);
            assertTrue(node.shownTooltip() instanceof Tooltip.Custom);
        }
    }
}
