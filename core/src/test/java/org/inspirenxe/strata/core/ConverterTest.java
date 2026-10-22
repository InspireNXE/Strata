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
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.state.Conversion;
import org.inspirenxe.strata.api.state.Converter;
import org.inspirenxe.strata.api.state.Converters;
import org.inspirenxe.strata.api.state.PropertyKey;
import org.inspirenxe.strata.api.state.State;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.core.state.StateImpl;
import org.inspirenxe.strata.core.text.Translations;
import org.inspirenxe.strata.spi.Translator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Converters, converted keys and bindings through them (design 4.43).
 */
class ConverterTest {
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

    private static <M> M valid(final Conversion<M> conversion) {
        return ((Conversion.Valid<M>) conversion).value();
    }

    private static String invalid(final Conversion<?> conversion) {
        return ((Conversion.Invalid<?>) conversion).message().plain();
    }

    @Nested
    class Numbers {
        @Test
        void integersParseWholeNumbersInTheirRange() {
            final Converter<Integer, String> quantity = Converters.integer(1, 64);
            assertEquals(5, valid(quantity.toModel("5")));
            assertEquals(7, valid(quantity.toModel(" 7 ")));
            assertEquals(2, valid(quantity.toModel("2.0")));
            assertEquals(64, valid(quantity.toModel("64")));
            for (final String bad : new String[] {"", "abc", "100", "0", "2.5", "5x", "-1"}) {
                assertEquals("Enter a whole number from 1 to 64", invalid(quantity.toModel(bad)), bad);
            }
        }

        @Test
        void integersFormatWithoutGroupingAndParseWithIt() {
            final Converter<Integer, String> any = Converters.integer();
            assertEquals("12000", any.toView(12000));
            assertEquals("", any.toView(null));
            assertEquals(1000, valid(any.toModel("1,000")));
            assertEquals(-3, valid(any.toModel("-3")));
            assertEquals("Enter a whole number", invalid(any.toModel("3000000000")));
            assertEquals("Enter a whole number", invalid(any.toModel("Infinity")));
        }

        @Test
        void decimalsShowTheirDigitsAndRoundHalfUp() {
            final Converter<Double, String> price = Converters.decimal(2);
            assertEquals("2.50", price.toView(2.5));
            assertEquals("0.13", price.toView(0.125));
            assertEquals(2.57, valid(price.toModel("2.565")));
            assertEquals(0.0, valid(price.toModel("-0")));
            assertEquals("Enter a number", invalid(price.toModel("two")));
        }

        @Test
        void decimalRangesCheckAfterRounding() {
            final Converter<Double, String> rating = Converters.decimal(0, 10, 1);
            assertEquals(10.0, valid(rating.toModel("10.04")));
            assertEquals("Enter a number from 0 to 10", invalid(rating.toModel("10.06")));
            assertEquals("Enter a number from 0 to 10", invalid(rating.toModel("")));
        }

        @Test
        void followTheTranslatorsLocale() {
            final Converter<Double, String> price = Converters.decimal(2);
            Translations.setTranslator(new LocaleTranslator(Locale.GERMANY));
            assertEquals(2.5, valid(price.toModel("2,5")));
            assertEquals("2,50", price.toView(2.5));
            assertEquals(1234.5, valid(price.toModel("1.234,5")));
            assertEquals("Enter a number from 0,5 to 9,5", invalid(Converters.decimal(0.5, 9.5, 1).toModel("x")));
        }

        @Test
        void refuseBadRanges() {
            assertThrows(IllegalArgumentException.class, () -> Converters.integer(5, 1));
            assertThrows(IllegalArgumentException.class, () -> Converters.decimal(1, 0, 2));
            assertThrows(IllegalArgumentException.class, () -> Converters.decimal(Double.NaN, 1, 2));
            assertThrows(IllegalArgumentException.class, () -> Converters.decimal(-1));
            assertThrows(IllegalArgumentException.class, () -> Converters.decimal(16));
        }

        @Test
        void functionsMakeAConverterThatIsNeverInvalid() {
            final Converter<String, Text> name = Converters.of(Text::of, Text::plain);
            assertEquals("Ana", name.toView("Ana").plain());
            assertEquals("Ben", valid(name.toModel(Text.of("Ben"))));
            assertTrue(name.toModel(Text.of("")).isValid());
        }
    }

    @Nested
    class Keys {
        @Test
        void aConvertedKeyKeepsTheNameAndReports() {
            final PropertyKey<TextInput<?>, Integer> key = TextInput.TEXT.convert(Converters.integer());
            assertEquals(TextInput.TEXT.name(), key.name());
            assertEquals(TextInput.TEXT.reports(), key.reports());
        }

        @Test
        void readsNullAndChecksInvalidWhileTheTextDoesNotConvert() {
            final PropertyKey<TextInput<?>, Integer> key = TextInput.TEXT.convert(Converters.integer(1, 64));
            final TextBox box = ConverterTest.this.ui.textBox().setText("12");
            assertEquals(12, key.reader().apply(box));
            key.writer().accept(box, 30);
            assertEquals("30", box.text());
            box.setText("lots");
            assertNull(key.reader().apply(box));
            assertFalse(key.check().apply(box).isValid());
        }

        @Test
        void plainKeysAlwaysCheckValid() {
            final TextBox box = ConverterTest.this.ui.textBox().setText("abc");
            assertEquals(Conversion.valid("abc"), TextInput.TEXT.check().apply(box));
        }

        @Test
        void convertedKeysChain() {
            final PropertyKey<TextInput<?>, String> key = TextInput.TEXT.convert(Converters.integer())
                    .convert(Converters.of(Integer::parseInt, String::valueOf));
            final TextBox box = ConverterTest.this.ui.textBox();
            key.writer().accept(box, "41");
            assertEquals("41", box.text());
            assertEquals("41", key.reader().apply(box));
            box.setText("x");
            assertFalse(key.check().apply(box).isValid());
        }
    }

    @Nested
    class Binding {
        private TextBox box;
        private State<Integer> quantity;

        @BeforeEach
        void bind() {
            this.quantity = State.of(5);
            this.box = ConverterTest.this.ui.textBox();
            this.box.bind(TextInput.TEXT.convert(Converters.integer(1, 64)), this.quantity);
            ConverterTest.this.test.show(this.box);
            this.box.requestFocus();
        }

        private void retype(final String text) {
            this.box.selectAll();
            text.codePoints().forEach(c -> ConverterTest.this.test.host().charTyped(c, Modifiers.NONE));
        }

        @Test
        void showsTheConvertedState() {
            assertEquals("5", this.box.text());
        }

        @Test
        void writesConvertedEditsAndKeepsTheLastValidValue() {
            this.retype("12");
            assertEquals(12, this.quantity.get());
            this.retype("12x");
            assertEquals("12x", this.box.text());
            assertEquals(12, this.quantity.get());
            // Each key is an edit: "1" and "10" convert, "100" does not
            this.retype("100");
            assertEquals(10, this.quantity.get());
            // The state changed to 10 from this field, so the frame leaves the field's "100" alone
            ConverterTest.this.test.host().flush();
            assertEquals("100", this.box.text());
            ConverterTest.this.test.host().key(Key.BACKSPACE, KeyAction.PRESS, Modifiers.NONE);
            ConverterTest.this.test.host().key(Key.BACKSPACE, KeyAction.PRESS, Modifiers.NONE);
            assertEquals("1", this.box.text());
            assertEquals(1, this.quantity.get());
        }

        @Test
        void leavesTextThatConvertsToTheStatesValue() {
            this.retype("05");
            ConverterTest.this.test.host().flush();
            assertEquals("05", this.box.text());
            assertEquals(5, this.quantity.get());
        }

        @Test
        void aChangeFromCodeReplacesTextThatDidNotConvert() {
            this.retype("abc");
            this.quantity.set(7);
            ConverterTest.this.test.host().flush();
            assertEquals("7", this.box.text());
        }

        @Test
        void replacesAndIsRemovedByThePlainKey() {
            final State<String> raw = State.of("plain");
            this.box.bind(TextInput.TEXT, raw);
            assertEquals("plain", this.box.text());
            assertEquals(0, ((StateImpl<?>) this.quantity).dependentCount());
            this.box.unbind(TextInput.TEXT.convert(Converters.integer()));
            assertEquals(0, ((StateImpl<?>) raw).dependentCount());
            assertSame(this.box, this.box.unbind(TextInput.TEXT));
        }
    }
}
