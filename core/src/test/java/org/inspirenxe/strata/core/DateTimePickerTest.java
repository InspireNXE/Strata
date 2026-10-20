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
import org.inspirenxe.strata.api.control.attribute.value.HasDate;
import org.inspirenxe.strata.api.control.attribute.value.HasTime;
import org.inspirenxe.strata.api.control.field.CalendarView;
import org.inspirenxe.strata.api.control.field.DateTimePicker;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.core.text.Translations;
import org.inspirenxe.strata.spi.Translator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The date and time picker in American English: month, day, year, then hour, minute and the half of the day.
 */
class DateTimePickerTest {
    private TestHost test;
    private DateTimePicker picker;
    private final List<String> events = new ArrayList<>();

    @BeforeEach
    void setUp() {
        Translations.setTranslator(new LocaleTranslator(Locale.US));
        this.test = new TestHost(500, 400);
        this.picker = this.test.factory().dateTimePicker();
        this.picker.onEvent(DateTimePicker.PICK, event -> this.events.add("pick " + event.value()));
        this.picker.onEvent(HasDate.PICK_DATE, event -> this.events.add("pick date " + event.value()));
        this.picker.onEvent(HasTime.PICK_TIME, event -> this.events.add("pick time " + event.value()));
        this.picker.onEvent(DateTimePicker.PICKED, event -> this.events.add("picked " + event.value()));
        this.picker.onEvent(HasDate.DATE_PICKED, event -> this.events.add("date picked " + event.value()));
        this.picker.onEvent(HasTime.TIME_PICKED, event -> this.events.add("time picked " + event.value()));
        this.test.show(this.test.factory().stackPanel(Orientation.VERTICAL).add(this.picker)
                .add(this.test.factory().textBox()));
    }

    @AfterEach
    void resetTranslator() {
        Translations.setTranslator(Translator.NONE);
    }

    private List<String> texts() {
        final NullCanvas canvas = new NullCanvas();
        this.test.host().layout();
        this.test.host().render(canvas);
        return canvas.texts().stream().map(TextSpan::text).filter(text -> !text.isBlank()).toList();
    }

    private void type(final String text) {
        text.codePoints().forEach(codePoint -> this.test.host().charTyped(codePoint, Modifiers.NONE));
    }

    private void press(final Key key) {
        this.test.host().key(key, KeyAction.PRESS, Modifiers.NONE);
        this.test.host().key(key, KeyAction.RELEASE, Modifiers.NONE);
    }

    @Test
    void aUserChangeAsksForTheWholeThenEachPartAndReportsTheSameWay() {
        this.picker.requestFocus();
        this.type("102020260930p");
        assertEquals(List.of("10", "/", "20", "/", "2026", "09", ":", "30", "PM"), this.texts());
        this.press(Key.ENTER);
        assertEquals(LocalDateTime.of(2026, 10, 20, 21, 30), this.picker.dateTime().orElseThrow());
        assertEquals(List.of("pick 2026-10-20T21:30", "pick date 2026-10-20", "pick time 21:30",
                "picked 2026-10-20T21:30", "date picked 2026-10-20", "time picked 21:30"), this.events);
    }

    @Test
    void onlyTheChangedPartIsAskedAndReported() {
        this.picker.setDateTime(LocalDateTime.of(2026, 10, 20, 9, 0));
        this.events.clear();
        this.picker.requestFocus();
        this.press(Key.RIGHT);
        this.press(Key.UP);
        this.press(Key.ENTER);
        assertEquals(List.of("pick 2026-10-21T09:00", "pick date 2026-10-21", "picked 2026-10-21T09:00",
                "date picked 2026-10-21"), this.events);
    }

    @Test
    void rewritingTheDateChangesOnlyTheDate() {
        this.picker.onEvent(HasDate.PICK_DATE, event -> event.setValue(LocalDate.of(2026, 10, 25)));
        this.picker.requestFocus();
        this.type("102020260930p");
        this.press(Key.ENTER);
        assertEquals(LocalDateTime.of(2026, 10, 25, 21, 30), this.picker.dateTime().orElseThrow());
    }

    @Test
    void cancelingThePartStopsTheChange() {
        this.picker.onEvent(HasTime.PICK_TIME, event -> event.cancel());
        this.picker.requestFocus();
        this.type("102020260930p");
        this.press(Key.ENTER);
        assertTrue(this.picker.dateTime().isEmpty());
    }

    @Test
    void settingOnePartOfAnEmptyValueFillsTheOther() {
        this.picker.setDefaultTime(LocalTime.of(9, 0));
        this.picker.setDate(LocalDate.of(2026, 12, 24));
        assertEquals(LocalDateTime.of(2026, 12, 24, 9, 0), this.picker.dateTime().orElseThrow());
        this.picker.clearTime();
        assertTrue(this.picker.dateTime().isEmpty(), "clearing either part clears the value");
        this.picker.setTime(LocalTime.of(18, 45));
        assertEquals(LocalDateTime.of(2026, 10, 20, 18, 45), this.picker.dateTime().orElseThrow(), "today");
        assertEquals(LocalDate.of(2026, 10, 20), this.picker.date().orElseThrow());
        assertEquals(LocalTime.of(18, 45), this.picker.time().orElseThrow());
    }

    @Test
    void theDateRangeRunsFromTheStartOfTheFirstDayToTheEndOfTheLast() {
        this.picker.setDateRange(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));
        assertEquals(LocalDateTime.of(2026, 10, 1, 0, 0), this.picker.minDateTime().orElseThrow());
        assertEquals(LocalDate.of(2026, 10, 31), this.picker.maxDate().orElseThrow());
        this.picker.setDateTime(LocalDateTime.of(2026, 11, 5, 10, 0));
        assertEquals(LocalDateTime.of(2026, 10, 31, 23, 59), this.picker.dateTime().orElseThrow());
        this.picker.setRange(LocalDateTime.of(2026, 10, 31, 23, 30, 30), null);
        assertEquals(LocalDateTime.of(2026, 10, 31, 23, 59), this.picker.dateTime().orElseThrow());
        this.picker.setDateTime(LocalDateTime.of(2026, 10, 1, 0, 0));
        assertEquals(LocalDateTime.of(2026, 10, 31, 23, 31), this.picker.dateTime().orElseThrow(),
                "moved up to the next whole minute inside the range");
        assertThrows(IllegalArgumentException.class,
                () -> this.picker.setRange(LocalDateTime.of(2026, 2, 1, 0, 0), LocalDateTime.of(2026, 1, 1, 0, 0)));
    }

    @Test
    void aDayPickedInThePopupKeepsItOpenForTheTime() {
        this.picker.setDefaultTime(LocalTime.of(8, 0));
        this.picker.requestFocus();
        this.press(Key.F4);
        this.test.host().layout();
        final Component<?> content = this.test.factory().overlays().open().getFirst().content();
        final CalendarView calendar = (CalendarView) ((Container<?>) content).children().getFirst();
        final Rect bounds = calendar.bounds();
        // 14 October 2026 sits in the third row, fourth column, of a grid that starts on Sunday 27 September
        this.test.click(bounds.x() + 4f + 3 * 24f + 12f, bounds.y() + 30f + 2 * 13f + 6f);
        assertEquals(LocalDateTime.of(2026, 10, 14, 8, 0), this.picker.dateTime().orElseThrow());
        assertTrue(this.picker.isOpen());
        assertSame(this.picker, this.test.focused());
        assertEquals(LocalDate.of(2026, 10, 14), calendar.date().orElseThrow());
    }

    @Test
    void aHalfDraftIsDroppedOnFocusLoss() {
        this.picker.requestFocus();
        this.type("10202026");
        this.press(Key.TAB);
        assertTrue(this.picker.dateTime().isEmpty());
        assertEquals("Pick a date and time", this.texts().getFirst());
    }
}
