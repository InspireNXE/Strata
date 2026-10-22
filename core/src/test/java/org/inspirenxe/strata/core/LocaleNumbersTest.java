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
import org.inspirenxe.strata.api.control.field.Spinner;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.state.Converters;
import org.inspirenxe.strata.core.text.Numbers;
import org.inspirenxe.strata.core.text.Translations;
import org.inspirenxe.strata.spi.Translator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Numbers in the translator's language, shared by the spinner and the number converters (design 4.49).
 */
class LocaleNumbersTest {
    private static final Locale ARABIC_DIGITS = Locale.forLanguageTag("ar-EG-u-nu-arab");

    private final LocaleTranslator translator = new LocaleTranslator(Locale.GERMANY);
    private final TestHost test;
    private final ComponentFactory ui;

    LocaleNumbersTest() {
        Translations.setTranslator(this.translator);
        this.test = new TestHost(300, 100);
        this.ui = this.test.factory();
    }

    @AfterEach
    void resetTranslator() {
        Translations.setTranslator(Translator.NONE);
    }

    @Test
    void numbersAreWrittenInTheLocalesSymbols() {
        final Numbers german = Numbers.of(Locale.GERMANY);
        assertEquals("2,50", german.format(2.5, 2));
        assertEquals("-1234,5", german.format(-1234.5, 1));
        assertEquals("3", german.format(2.5, 0));
        assertEquals("0", german.format(-0.0, 0));
        assertEquals("0,0", german.format(-0.04, 1));
        assertEquals("2,5", german.format(2.5, 0, 3));
        assertEquals("2", german.format(2.0, 0, 3));
        assertEquals("1.25", Numbers.of(Locale.US).format(1.25, 2));
        final Numbers arabic = Numbers.of(ARABIC_DIGITS);
        assertEquals("١٢" + arabic.separator() + "٥", arabic.format(12.5, 1));
    }

    @Test
    void numbersAreReadInTheLocalesSymbolsAndAscii() {
        final Numbers german = Numbers.of(Locale.GERMANY);
        assertEquals(new BigDecimal("2.5"), german.parse("2,5", false));
        assertEquals(new BigDecimal("2.5"), german.parse("2.5", false));
        assertEquals(new BigDecimal("1000"), german.parse("1.000", true));
        assertEquals(new BigDecimal("-3"), german.parse(" -3 ", true));
        assertEquals(new BigDecimal("5"), german.parse("5,", false));
        assertNull(german.parse("", false));
        assertNull(german.parse("2,5x", false));
        assertNull(german.parse("-", false));
        assertNull(german.parse("1-", false));
        final Numbers arabic = Numbers.of(ARABIC_DIGITS);
        assertEquals(new BigDecimal("12.5"), arabic.parse("١٢" + arabic.separator() + "٥", false));
        assertEquals(new BigDecimal("12"), arabic.parse("12", false));
    }

    private Spinner showSpinner(final int decimals) {
        final Spinner spinner = this.ui.spinner().setAnchor(Anchor.TOP_LEFT).setRange(-100, 100).setDecimals(decimals);
        this.test.show(spinner);
        this.test.click(10, 9);
        return spinner;
    }

    private TextBox field() {
        return (TextBox) this.test.focused();
    }

    private void retype(final String text) {
        this.field().selectAll();
        text.codePoints().forEach(codePoint -> this.test.host().charTyped(codePoint, Modifiers.NONE));
        this.test.host().key(Key.ENTER, KeyAction.PRESS, Modifiers.NONE);
        this.test.host().key(Key.ENTER, KeyAction.RELEASE, Modifiers.NONE);
    }

    @Test
    void aSpinnerShowsAndTakesTheLanguagesDecimalSeparator() {
        final Spinner spinner = this.showSpinner(2);
        spinner.setValue(2.5);
        assertEquals("2,50", this.field().text());
        this.retype("3,25");
        assertEquals(3.25, spinner.value());
        assertEquals("3,25", this.field().text());
        this.retype("-1.5");
        assertEquals(-1.5, spinner.value());
        assertEquals("-1,50", this.field().text());
    }

    @Test
    void aSpinnerRefusesWhatTheLanguageDoesNotWrite() {
        final Spinner spinner = this.showSpinner(1);
        spinner.setValue(4);
        this.retype("2,5,5");
        assertEquals("2,5", this.field().text());
        assertEquals(2.5, spinner.value());
    }

    @Test
    void aSpinnerAgreesWithTheConverters() {
        final Spinner spinner = this.showSpinner(2);
        spinner.setValue(-7.125);
        assertEquals(Converters.decimal(2).toView(-7.13), this.field().text());
        this.translator.set(Locale.US, Map.of());
        this.test.host().layout();
        assertEquals("-7.13", this.field().text());
        assertEquals(Converters.decimal(2).toView(-7.13), this.field().text());
    }

    @Test
    void aSpinnerFollowsALanguageChange() {
        final Spinner spinner = this.showSpinner(1);
        spinner.setValue(1.5);
        this.translator.set(Locale.US, Map.of());
        this.test.host().layout();
        assertEquals("1.5", this.field().text());
        this.retype("2,5");
        // A comma is not a decimal separator in English, so the text is refused as it is typed
        assertEquals(25.0, spinner.value());
    }
}
