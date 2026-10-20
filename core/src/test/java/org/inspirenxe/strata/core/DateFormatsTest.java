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

import org.inspirenxe.strata.core.text.DateFormats;
import org.inspirenxe.strata.core.text.DateFormats.Part;
import org.inspirenxe.strata.core.text.Translations;
import org.inspirenxe.strata.spi.Translator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Month;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DateFormatsTest {

    @AfterEach
    void resetTranslator() {
        Translations.setTranslator(Translator.NONE);
    }

    @Test
    void americanEnglishPutsTheMonthFirstOnATwelveHourClock() {
        final DateFormats formats = DateFormats.of(new LocaleTranslator(Locale.US));
        assertEquals(List.of(Part.MONTH, Part.DAY, Part.YEAR), formats.order());
        assertEquals("/", formats.separator());
        assertEquals(":", formats.timeSeparator());
        assertTrue(formats.isTwelveHour());
        assertEquals(DayOfWeek.SUNDAY, formats.firstDayOfWeek());
        assertEquals("AM", formats.am());
        assertEquals("PM", formats.pm());
        assertEquals("October", formats.monthName(Month.OCTOBER));
        assertEquals("Oct", formats.shortMonthName(Month.OCTOBER));
        assertEquals("Tue", formats.shortWeekdayName(DayOfWeek.TUESDAY));
    }

    @Test
    void germanPutsTheDayFirstOnATwentyFourHourClock() {
        final DateFormats formats = DateFormats.of(new LocaleTranslator(Locale.GERMANY));
        assertEquals(List.of(Part.DAY, Part.MONTH, Part.YEAR), formats.order());
        assertEquals(".", formats.separator());
        assertFalse(formats.isTwelveHour());
        assertEquals(DayOfWeek.MONDAY, formats.firstDayOfWeek());
        assertEquals("Oktober", formats.monthName(Month.OCTOBER));
    }

    @Test
    void japanesePutsTheYearFirst() {
        final DateFormats formats = DateFormats.of(new LocaleTranslator(Locale.JAPAN));
        assertEquals(List.of(Part.YEAR, Part.MONTH, Part.DAY), formats.order());
        assertEquals("/", formats.separator());
        assertFalse(formats.isTwelveHour());
    }

    @Test
    void translationKeysWinOverTheLocale() {
        final DateFormats formats = DateFormats.of(new LocaleTranslator(Locale.US, Map.of(
                "strata.date.order", "ymd",
                "strata.date.separator", "-",
                "strata.date.first_weekday", "1",
                "strata.time.clock", "24",
                "strata.time.am", "vorm.",
                "strata.time.pm", "nachm.",
                "strata.date.month.10", "Tenth",
                "strata.date.month.short.10", "Ten",
                "strata.date.weekday.short.2", "Tu")));
        assertEquals(List.of(Part.YEAR, Part.MONTH, Part.DAY), formats.order());
        assertEquals("-", formats.separator());
        assertEquals(DayOfWeek.MONDAY, formats.firstDayOfWeek());
        assertFalse(formats.isTwelveHour());
        assertEquals("vorm.", formats.am());
        assertEquals("nachm.", formats.pm());
        assertEquals("Tenth", formats.monthName(Month.OCTOBER));
        assertEquals("Ten", formats.shortMonthName(Month.OCTOBER));
        assertEquals("Tu", formats.shortWeekdayName(DayOfWeek.TUESDAY));
        assertEquals("November", formats.monthName(Month.NOVEMBER), "keys the translator lacks come from the locale");
    }

    @Test
    void valuesThatDoNotParseFallBackToTheLocale() {
        final DateFormats formats = DateFormats.of(new LocaleTranslator(Locale.US, Map.of(
                "strata.date.order", "dmx",
                "strata.date.first_weekday", "9",
                "strata.time.clock", "36")));
        assertEquals(List.of(Part.MONTH, Part.DAY, Part.YEAR), formats.order());
        assertEquals(DayOfWeek.SUNDAY, formats.firstDayOfWeek());
        assertTrue(formats.isTwelveHour());
    }

    @Test
    void theKeysSkipCoresEnglish() {
        // Strata's English table must never answer for these keys, or it would hide the locale's data
        for (final String key : Translations.DEFAULTS.keySet()) {
            assertFalse(key.startsWith("strata.date.") && !key.equals("strata.date.placeholder"), key);
            assertFalse(key.startsWith("strata.time.") && !key.equals("strata.time.placeholder"), key);
        }
        final DateFormats formats = DateFormats.of(new LocaleTranslator(Locale.GERMANY));
        assertTrue(formats.shortMonthName(Month.OCTOBER).startsWith("Okt"), formats.shortMonthName(Month.OCTOBER));
    }

    @Test
    void currentIsCachedPerLocaleAndVersion() {
        final LocaleTranslator translator = new LocaleTranslator(Locale.US);
        Translations.setTranslator(translator);
        final DateFormats first = DateFormats.current();
        assertSame(first, DateFormats.current());
        translator.set(Locale.GERMANY, Map.of());
        final DateFormats second = DateFormats.current();
        assertNotSame(first, second);
        assertEquals(Locale.GERMANY, second.locale());
        assertEquals(List.of(Part.DAY, Part.MONTH, Part.YEAR), second.order());
    }

    @Test
    void aTranslatorFollowsTheDefaultFormatLocaleUnlessItSaysOtherwise() {
        final Translator plain = new Translator() {
            @Override
            public Optional<String> translate(final String key) {
                return Optional.empty();
            }

            @Override
            public long version() {
                return 0L;
            }
        };
        assertEquals(Locale.getDefault(Locale.Category.FORMAT), plain.locale());
        assertEquals(Locale.getDefault(Locale.Category.FORMAT), Translator.NONE.locale());
    }
}
