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
import org.inspirenxe.strata.api.control.attribute.value.HasDate;
import org.inspirenxe.strata.api.control.field.CalendarView;
import org.inspirenxe.strata.api.control.field.CalendarView.Zoom;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifier;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.ThemeException;
import org.inspirenxe.strata.core.control.field.CalendarViewImpl;
import org.inspirenxe.strata.core.text.Translations;
import org.inspirenxe.strata.spi.Translator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The calendar in American English on the test platform: cells are 24 by 13 units ("Mon" plus the cell padding wide,
 * a line plus the padding tall), the grid starts 4 units in, the header is the first row, the weekday names the
 * second, and days start 30 units down. Today is Tuesday 20 October 2026, and weeks start on Sunday, so the grid of
 * October starts on 27 September.
 */
class CalendarViewTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 20);

    private TestHost test;
    private CalendarView calendar;
    private final List<String> events = new ArrayList<>();

    @BeforeEach
    void setUp() {
        Translations.setTranslator(new LocaleTranslator(Locale.US));
        this.test = new TestHost(400, 300);
        this.calendar = this.test.factory().calendarView();
        this.calendar.onEvent(HasDate.PICK_DATE, event -> this.events.add("pick " + event.value()));
        this.calendar.onEvent(HasDate.DATE_PICKED, event -> this.events.add("picked " + event.value()));
        this.calendar.onEvent(CalendarView.BROWSE, event -> this.events.add("browse " + event.value()));
        this.calendar.onEvent(CalendarView.BROWSED, event -> this.events.add("browsed " + event.value()));
        this.test.show(this.test.factory().absolutePanel().add(this.calendar));
    }

    @AfterEach
    void resetTranslator() {
        Translations.setTranslator(Translator.NONE);
    }

    private NullCanvas render() {
        final NullCanvas canvas = new NullCanvas();
        this.test.host().layout();
        this.test.host().render(canvas);
        return canvas;
    }

    /**
     * The center of a day's cell in the grid of the month in view.
     */
    private float[] center(final LocalDate day) {
        final YearMonth shown = this.calendar.shownMonth();
        final LocalDate first = shown.atDay(1);
        final int offset = Math.floorMod(first.getDayOfWeek().getValue() - DayOfWeek.SUNDAY.getValue(), 7);
        final int index = (int) (day.toEpochDay() - first.toEpochDay()) + offset;
        return new float[] {4f + (index % 7) * 24f + 12f, 30f + (index / 7) * 13f + 6f};
    }

    private void click(final float x, final float y) {
        this.test.host().pointerMoved(x, y, Modifiers.NONE);
        this.test.click(x, y);
    }

    private void clickDay(final LocalDate day) {
        final float[] at = this.center(day);
        this.click(at[0], at[1]);
    }

    private boolean press(final Key key, final Modifiers modifiers) {
        final boolean handled = this.test.host().key(key, KeyAction.PRESS, modifiers);
        this.test.host().key(key, KeyAction.RELEASE, modifiers);
        return handled;
    }

    @Test
    void aNewCalendarShowsTodaysMonthWithNothingPicked() {
        assertEquals(YearMonth.of(2026, 10), this.calendar.shownMonth());
        assertTrue(this.calendar.date().isEmpty());
        assertEquals(Zoom.MONTH, this.calendar.zoom());
        assertTrue(this.calendar.isOutsideDaysShown());
        assertEquals(176f, this.calendar.desiredWidth());
        assertEquals(112f, this.calendar.desiredHeight());
    }

    @Test
    void itDrawsTheTitleTheWeekdaysAndSixRows() {
        final List<String> texts = this.render().texts().stream().map(TextSpan::text).toList();
        assertTrue(texts.contains("October 2026"), texts.toString());
        assertEquals(List.of("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"), texts.subList(1, 8));
        // 27 September to 7 November
        assertEquals(42, texts.size() - 8);
        assertEquals("27", texts.get(8));
        assertEquals("7", texts.get(texts.size() - 1));
    }

    @Test
    void aClickPicksADay() {
        this.render();
        this.clickDay(LocalDate.of(2026, 10, 14));
        assertEquals(LocalDate.of(2026, 10, 14), this.calendar.date().orElseThrow());
        assertEquals(List.of("pick 2026-10-14", "picked 2026-10-14"), this.events);
    }

    @Test
    void aCanceledPickChangesNothing() {
        this.calendar.onEvent(HasDate.PICK_DATE, event -> event.cancel());
        this.render();
        this.clickDay(LocalDate.of(2026, 10, 14));
        assertTrue(this.calendar.date().isEmpty());
        assertEquals(List.of("pick 2026-10-14"), this.events);
    }

    @Test
    void aRewrittenPickAppliesAsSetDateDoes() {
        this.calendar.setDateRange(null, LocalDate.of(2026, 10, 25));
        this.calendar.onEvent(HasDate.PICK_DATE, event -> event.setValue(LocalDate.of(2026, 12, 1)));
        this.render();
        this.clickDay(LocalDate.of(2026, 10, 14));
        assertEquals(LocalDate.of(2026, 10, 25), this.calendar.date().orElseThrow(), "kept inside the range");
    }

    @Test
    void daysTheFilterRefusesCannotBePickedButCodeMaySetThem() {
        this.calendar.setDateFilter(day -> day.getDayOfWeek() != DayOfWeek.SATURDAY);
        this.render();
        this.clickDay(LocalDate.of(2026, 10, 17));
        assertTrue(this.calendar.date().isEmpty());
        this.calendar.setDate(LocalDate.of(2026, 10, 17));
        assertEquals(LocalDate.of(2026, 10, 17), this.calendar.date().orElseThrow());
    }

    @Test
    void aClickOnADayOfTheNextMonthBrowsesThereAndPicksIt() {
        this.render();
        this.clickDay(LocalDate.of(2026, 11, 3));
        assertEquals(YearMonth.of(2026, 11), this.calendar.shownMonth());
        assertEquals(LocalDate.of(2026, 11, 3), this.calendar.date().orElseThrow());
        assertEquals(List.of("browse 2026-11", "browsed 2026-11", "pick 2026-11-03", "picked 2026-11-03"), this.events);
    }

    @Test
    void hiddenOutsideDaysCannotBeClicked() {
        this.calendar.setOutsideDaysShown(false);
        final List<String> texts = this.render().texts().stream().map(TextSpan::text).toList();
        assertEquals(31, texts.size() - 8);
        this.clickDay(LocalDate.of(2026, 11, 3));
        assertTrue(this.calendar.date().isEmpty());
        assertEquals(YearMonth.of(2026, 10), this.calendar.shownMonth());
    }

    @Test
    void setDateShowsItsMonthAndKeepsItInsideTheRange() {
        this.calendar.setDate(LocalDate.of(2027, 3, 9));
        assertEquals(YearMonth.of(2027, 3), this.calendar.shownMonth());
        assertEquals(List.of("picked 2027-03-09", "browsed 2027-03"), this.events);
        this.events.clear();
        this.calendar.setDateRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        assertEquals(LocalDate.of(2026, 12, 31), this.calendar.date().orElseThrow());
        assertEquals(YearMonth.of(2026, 12), this.calendar.shownMonth());
        assertThrows(IllegalArgumentException.class,
                () -> this.calendar.setDateRange(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1)));
        this.calendar.clearDate();
        assertTrue(this.calendar.date().isEmpty());
    }

    @Test
    void theArrowsBrowseAndStopAtTheRange() {
        this.calendar.setDateRange(LocalDate.of(2026, 9, 15), LocalDate.of(2026, 11, 15));
        this.render();
        this.click(16f, 10f);
        assertEquals(YearMonth.of(2026, 9), this.calendar.shownMonth());
        this.click(16f, 10f);
        assertEquals(YearMonth.of(2026, 9), this.calendar.shownMonth(), "the range starts in September");
        this.click(4f + 6 * 24f + 12f, 10f);
        this.click(4f + 6 * 24f + 12f, 10f);
        this.click(4f + 6 * 24f + 12f, 10f);
        assertEquals(YearMonth.of(2026, 11), this.calendar.shownMonth());
        assertEquals(List.of("browse 2026-09", "browsed 2026-09", "browse 2026-10", "browsed 2026-10", "browse 2026-11",
                "browsed 2026-11"), this.events);
    }

    @Test
    void aCanceledBrowseStaysAndARewrittenOneGoesElsewhere() {
        final List<Boolean> cancel = new ArrayList<>(List.of(true));
        this.calendar.onEvent(CalendarView.BROWSE, event -> {
            if (cancel.get(0)) {
                event.cancel();
            } else {
                event.setValue(YearMonth.of(2030, 1));
            }
        });
        this.render();
        this.click(16f, 10f);
        assertEquals(YearMonth.of(2026, 10), this.calendar.shownMonth());
        cancel.set(0, false);
        this.click(16f, 10f);
        assertEquals(YearMonth.of(2030, 1), this.calendar.shownMonth());
    }

    @Test
    void theTitleZoomsOutAndACellZoomsIn() {
        this.render();
        this.click(88f, 10f);
        assertEquals(Zoom.YEAR, this.calendar.zoom());
        assertTrue(this.render().texts().stream().map(TextSpan::text).toList().containsAll(List.of("2026", "Jan", "Dec")));
        this.click(88f, 10f);
        assertEquals(Zoom.DECADE, this.calendar.zoom());
        final List<String> years = this.render().texts().stream().map(TextSpan::text).toList();
        assertTrue(years.containsAll(List.of("2020 - 2029", "2019", "2030")), years.toString());
        // The decade grid: four across of 42 units, three down of 30 units, below the header
        this.click(4f + 42f * 2 + 21f, 17f + 15f);
        assertEquals(Zoom.YEAR, this.calendar.zoom());
        assertEquals(2021, this.calendar.shownMonth().getYear());
        this.click(4f + 42f + 21f, 17f + 30f + 15f);
        assertEquals(Zoom.MONTH, this.calendar.zoom());
        assertEquals(YearMonth.of(2021, 6), this.calendar.shownMonth());
    }

    @Test
    void theKeyboardMovesTheCursorBrowsesAndPicks() {
        final CalendarViewImpl impl = (CalendarViewImpl) this.calendar;
        this.calendar.requestFocus();
        assertEquals(TODAY, impl.cursorDate().orElseThrow(), "the cursor starts on today");
        this.press(Key.RIGHT, Modifiers.NONE);
        this.press(Key.DOWN, Modifiers.NONE);
        assertEquals(LocalDate.of(2026, 10, 28), impl.cursorDate().orElseThrow());
        this.press(Key.END, Modifiers.NONE);
        this.press(Key.RIGHT, Modifiers.NONE);
        assertEquals(LocalDate.of(2026, 11, 1), impl.cursorDate().orElseThrow());
        assertEquals(YearMonth.of(2026, 11), this.calendar.shownMonth());
        this.press(Key.PAGE_DOWN, Modifiers.of(Modifier.SHIFT));
        assertEquals(LocalDate.of(2027, 11, 1), impl.cursorDate().orElseThrow());
        this.press(Key.HOME, Modifiers.NONE);
        this.press(Key.ENTER, Modifiers.NONE);
        assertEquals(LocalDate.of(2027, 11, 1), this.calendar.date().orElseThrow());
    }

    @Test
    void controlUpZoomsOutAndEnterZoomsBackIn() {
        final CalendarViewImpl impl = (CalendarViewImpl) this.calendar;
        this.calendar.requestFocus();
        this.press(Key.UP, Modifiers.of(Modifier.CONTROL));
        assertEquals(Zoom.YEAR, this.calendar.zoom());
        this.press(Key.RIGHT, Modifiers.NONE);
        this.press(Key.ENTER, Modifiers.NONE);
        assertEquals(Zoom.MONTH, this.calendar.zoom());
        assertEquals(YearMonth.of(2026, 11), this.calendar.shownMonth());
        assertEquals(YearMonth.of(2026, 11), YearMonth.from(impl.cursorDate().orElseThrow()));
    }

    @Test
    void theCursorStaysInsideTheRange() {
        final CalendarViewImpl impl = (CalendarViewImpl) this.calendar;
        this.calendar.setDateRange(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 22));
        this.calendar.requestFocus();
        this.press(Key.DOWN, Modifiers.NONE);
        assertEquals(LocalDate.of(2026, 10, 22), impl.cursorDate().orElseThrow());
        this.press(Key.PAGE_UP, Modifiers.NONE);
        assertEquals(LocalDate.of(2026, 10, 10), impl.cursorDate().orElseThrow());
    }

    @Test
    void theWheelIsLeftForThePanelAround() {
        this.render();
        final float[] at = this.center(TODAY);
        assertFalse(this.test.host().scroll(at[0], at[1], 0f, 1f, org.inspirenxe.strata.api.input.ScrollKind.WHEEL,
                Modifiers.NONE));
    }

    @Test
    void aDisabledCalendarIgnoresClicks() {
        this.calendar.setEnabled(false);
        this.render();
        this.clickDay(LocalDate.of(2026, 10, 14));
        assertTrue(this.calendar.date().isEmpty());
        assertFalse(this.calendar.requestFocus());
    }

    @Test
    void todayIsReadAgainOnceAMinuteHasPassed() {
        final CalendarViewImpl stale = (CalendarViewImpl) this.calendar;
        this.render();
        this.test.platform().setToday(LocalDateTime.of(2026, 10, 21, 0, 0));
        this.render();
        stale.requestFocus();
        assertEquals(TODAY, stale.cursorDate().orElseThrow(), "less than a minute passed");

        final CalendarViewImpl fresh = (CalendarViewImpl) this.test.factory().calendarView();
        this.test.show(this.test.factory().absolutePanel().add(fresh));
        this.render();
        this.test.platform().setToday(LocalDateTime.of(2026, 10, 22, 0, 0));
        this.test.platform().advanceMillis(CalendarViewImpl.TODAY_CHECK_MILLIS);
        this.render();
        fresh.requestFocus();
        assertEquals(LocalDate.of(2026, 10, 22), fresh.cursorDate().orElseThrow());
    }

    @Test
    void theFirstDayOfWeekCanBeSetInCode() {
        this.calendar.setFirstDayOfWeek(DayOfWeek.MONDAY);
        final List<String> texts = this.render().texts().stream().map(TextSpan::text).toList();
        assertEquals(List.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"), texts.subList(1, 8));
        assertEquals("28", texts.get(8), "October 2026 starts on a Thursday, three days after Monday the 28th");
        assertEquals(DayOfWeek.MONDAY, this.calendar.firstDayOfWeek().orElseThrow());
        this.calendar.setFirstDayOfWeek(null);
        assertTrue(this.calendar.firstDayOfWeek().isEmpty());
    }

    @Test
    void theLanguageChangesTheNamesAndTheFirstDay() {
        final LocaleTranslator german = new LocaleTranslator(Locale.GERMANY);
        Translations.setTranslator(german);
        final List<String> texts = this.render().texts().stream().map(TextSpan::text).toList();
        assertTrue(texts.contains("Oktober 2026"), texts.toString());
        assertEquals("Mo", texts.get(1).substring(0, 2));
    }

    @Test
    void snapshotsHoldTheDateTheMonthAndTheZoom() {
        final List<ComponentSnapshot> seen = new ArrayList<>();
        this.calendar.onEvent(HasDate.PICK_DATE, event -> seen.add(event.snapshot()));
        this.calendar.setDate(LocalDate.of(2026, 10, 2));
        this.render();
        this.clickDay(LocalDate.of(2026, 10, 14));
        assertEquals(new CalendarView.State(LocalDate.of(2026, 10, 2), YearMonth.of(2026, 10), Zoom.MONTH),
                seen.get(0).state());
    }

    @Test
    void aThemeWithoutTheSelectedDayIsRefused() {
        final TestHost host = new TestHost(new TestPlatform(), Theme.builder("bare").build(), 200, 200);
        final ThemeException error = assertThrows(ThemeException.class, () -> host.show(host.factory().calendarView()));
        assertEquals(Set.of(CalendarView.STYLE_DAY_SELECTED), error.missingKeys());
    }

    @Test
    void mouseButtonsOtherThanTheLeftDoNothing() {
        this.render();
        final float[] at = this.center(LocalDate.of(2026, 10, 14));
        this.test.host().pointerButton(at[0], at[1], MouseButton.RIGHT, true, Modifiers.NONE);
        this.test.host().pointerButton(at[0], at[1], MouseButton.RIGHT, false, Modifiers.NONE);
        assertTrue(this.calendar.date().isEmpty());
    }
}
