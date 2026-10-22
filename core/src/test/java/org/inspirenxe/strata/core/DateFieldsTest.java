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

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.Container;
import org.inspirenxe.strata.api.control.collection.ListView;
import org.inspirenxe.strata.api.control.field.CalendarView;
import org.inspirenxe.strata.api.control.field.DatePicker;
import org.inspirenxe.strata.api.control.field.DateTimePicker;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.core.text.Translations;
import org.inspirenxe.strata.spi.Translator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The date and time controls, in American English, on the test platform's 20 October 2026: what their ranges allow,
 * and how their fields and popups follow the value.
 */
class DateFieldsTest {
    private TestHost test;

    @BeforeEach
    void setUp() {
        Translations.setTranslator(new LocaleTranslator(Locale.US));
        this.test = new TestHost(500, 400);
    }

    @AfterEach
    void resetTranslator() {
        Translations.setTranslator(Translator.NONE);
    }

    private void type(final String text) {
        text.codePoints().forEach(codePoint -> this.test.host().charTyped(codePoint, Modifiers.NONE));
    }

    private List<String> texts() {
        final NullCanvas canvas = new NullCanvas();
        this.test.host().layout();
        this.test.host().render(canvas);
        return canvas.texts().stream().map(TextSpan::text).filter(text -> !text.isBlank()).toList();
    }

    private void press(final Key key) {
        this.test.host().key(key, KeyAction.PRESS, Modifiers.NONE);
        this.test.host().key(key, KeyAction.RELEASE, Modifiers.NONE);
    }

    @Nested
    class DateTimePopup {
        private DateTimePicker picker;

        private void show() {
            this.picker = DateFieldsTest.this.test.factory().dateTimePicker();
            DateFieldsTest.this.test.show(DateFieldsTest.this.test.factory().stackPanel(Orientation.VERTICAL)
                    .add(this.picker));
        }

        @SuppressWarnings("unchecked")
        private ListView<Integer> column(final int index) {
            final Component<?> content = DateFieldsTest.this.test.factory().overlays().open().getFirst().content();
            final Container<?> columns = (Container<?>) ((Container<?>) content).children().get(1);
            return (ListView<Integer>) columns.children().get(index);
        }

        private void pressRow(final ListView<Integer> list, final int row) {
            final Rect bounds = list.bounds();
            final float y = bounds.y() + row * 13f + 6f - list.scrollY();
            DateFieldsTest.this.test.host().pointerButton(bounds.x() + 10f, y, MouseButton.LEFT, true, Modifiers.NONE);
            DateFieldsTest.this.test.host().pointerButton(bounds.x() + 10f, y, MouseButton.LEFT, false, Modifiers.NONE);
        }

        @Test
        void aRowSetsItsPartAtOnce() {
            this.show();
            this.picker.setDateTime(LocalDateTime.of(2026, 10, 20, 9, 0));
            this.picker.requestFocus();
            DateFieldsTest.this.press(Key.F4);
            DateFieldsTest.this.test.host().layout();
            DateFieldsTest.this.test.host().render(new NullCanvas());
            DateFieldsTest.this.test.host().layout();
            final ListView<Integer> hours = this.column(0);
            this.pressRow(hours, 10);

            assertEquals(LocalDateTime.of(2026, 10, 20, 10, 0), this.picker.dateTime().orElseThrow());
            assertEquals(10, hours.selectedIndex());
        }

        @Test
        void aRowWhosePickIsCanceledGivesWayToTheValueAgain() {
            this.show();
            this.picker.setDateTime(LocalDateTime.of(2026, 10, 20, 9, 0));
            this.picker.onEvent(DateTimePicker.PICK, event -> event.cancel());
            this.picker.requestFocus();
            DateFieldsTest.this.press(Key.F4);
            DateFieldsTest.this.test.host().layout();
            DateFieldsTest.this.test.host().render(new NullCanvas());
            DateFieldsTest.this.test.host().layout();
            final ListView<Integer> hours = this.column(0);
            this.pressRow(hours, 10);

            assertEquals(LocalDateTime.of(2026, 10, 20, 9, 0), this.picker.dateTime().orElseThrow());
            assertEquals(9, hours.selectedIndex(), "the hours column shows nine again, as the time picker's does");
        }
    }

    @Nested
    class SupportedDates {

        @Test
        void aCalendarHoldsTheLastSupportedDayAndItsKeysStopThere() {
            final CalendarView calendar = DateFieldsTest.this.test.factory().calendarView();
            calendar.setAnchor(Anchor.TOP_LEFT);
            DateFieldsTest.this.test.show(calendar);

            calendar.setDate(LocalDate.MAX);
            DateFieldsTest.this.texts();
            assertEquals(LocalDate.of(9999, 12, 31), calendar.date().orElseThrow());
            assertEquals(YearMonth.of(9999, 12), calendar.shownMonth());

            calendar.requestFocus();
            DateFieldsTest.this.press(Key.RIGHT);
            DateFieldsTest.this.press(Key.PAGE_DOWN);
            DateFieldsTest.this.texts();
            assertEquals(YearMonth.of(9999, 12), calendar.shownMonth());
        }

        @Test
        void aCalendarHoldsTheFirstSupportedDayAndItsKeysStopThere() {
            final CalendarView calendar = DateFieldsTest.this.test.factory().calendarView();
            calendar.setAnchor(Anchor.TOP_LEFT);
            DateFieldsTest.this.test.show(calendar);

            calendar.setDate(LocalDate.MIN);
            calendar.requestFocus();
            DateFieldsTest.this.press(Key.LEFT);
            DateFieldsTest.this.press(Key.PAGE_UP);
            DateFieldsTest.this.texts();

            assertEquals(LocalDate.of(1, 1, 1), calendar.date().orElseThrow());
            assertEquals(YearMonth.of(1, 1), calendar.shownMonth());
        }

        @Test
        void aDatePickerKeepsCodesDatesToFourDigitYears() {
            final DatePicker picker = DateFieldsTest.this.test.factory().datePicker();
            DateFieldsTest.this.test.show(picker);

            picker.setDate(LocalDate.of(20000, 1, 1));
            assertEquals(LocalDate.of(9999, 12, 31), picker.date().orElseThrow());
            picker.setDate(LocalDate.of(-5, 6, 1));
            assertEquals(LocalDate.of(1, 1, 1), picker.date().orElseThrow());
            assertTrue(DateFieldsTest.this.texts().contains("0001"), "the year shows rather than an empty part");
        }

        @Test
        void aYearTypedWithTwoDigitsShowsThemUntilItIsTaken() {
            final DatePicker picker = DateFieldsTest.this.test.factory().datePicker();
            DateFieldsTest.this.test.show(picker);
            picker.requestFocus();

            DateFieldsTest.this.type("102025");
            DateFieldsTest.this.press(Key.LEFT);
            assertTrue(DateFieldsTest.this.texts().contains("25"));
            assertFalse(DateFieldsTest.this.texts().contains("0025"), "not a year in the first century");

            DateFieldsTest.this.press(Key.ENTER);
            assertEquals(LocalDate.of(2025, 10, 20), picker.date().orElseThrow());
            assertTrue(DateFieldsTest.this.texts().contains("2025"));
        }
    }

    @Nested
    class DateTimeRanges {

        @Test
        void aRangeTooShortForAWholeMinuteKeepsTheValueInsideIt() {
            final DateTimePicker picker = DateFieldsTest.this.test.factory().dateTimePicker();
            DateFieldsTest.this.test.show(picker);
            final LocalDateTime min = LocalDateTime.of(2026, 10, 20, 10, 0, 20);
            final LocalDateTime max = LocalDateTime.of(2026, 10, 20, 10, 0, 40);

            picker.setRange(min, max);
            picker.setDateTime(LocalDateTime.of(2026, 10, 20, 10, 0, 30));

            final LocalDateTime value = picker.dateTime().orElseThrow();
            assertFalse(value.isBefore(min) || value.isAfter(max), "the value " + value + " stays inside the range");
        }

        @Test
        void typedAndSetValuesAgreeOnTheFirstWholeMinute() {
            final DateTimePicker picker = DateFieldsTest.this.test.factory().dateTimePicker();
            DateFieldsTest.this.test.show(picker);
            picker.setRange(LocalDateTime.of(2026, 10, 20, 10, 0, 30), null);

            picker.setDateTime(LocalDateTime.of(2026, 10, 20, 10, 0));
            assertEquals(LocalDateTime.of(2026, 10, 20, 10, 1), picker.dateTime().orElseThrow());

            picker.clearDate();
            picker.requestFocus();
            DateFieldsTest.this.type("102020261000a");
            DateFieldsTest.this.press(Key.ENTER);
            assertTrue(picker.dateTime().isEmpty(), "10:00 lies before the range, so the draft is refused");
            DateFieldsTest.this.press(Key.ESCAPE);
            DateFieldsTest.this.press(Key.HOME);
            DateFieldsTest.this.type("102020261001a");
            DateFieldsTest.this.press(Key.ENTER);
            assertEquals(LocalDateTime.of(2026, 10, 20, 10, 1), picker.dateTime().orElseThrow());
        }
    }
}
