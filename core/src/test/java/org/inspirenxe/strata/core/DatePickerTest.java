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
import org.inspirenxe.strata.api.control.attribute.popup.Closable;
import org.inspirenxe.strata.api.control.attribute.popup.Openable;
import org.inspirenxe.strata.api.control.attribute.value.HasDate;
import org.inspirenxe.strata.api.control.field.CalendarView;
import org.inspirenxe.strata.api.control.field.DatePicker;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifier;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.ThemeException;
import org.inspirenxe.strata.core.text.Translations;
import org.inspirenxe.strata.spi.Translator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The date picker in American English on the test platform: the field reads month, day and year, every character is
 * 6 units wide, the text starts 4 units in, and the 12 unit button ends the field. Today is 20 October 2026.
 */
class DatePickerTest {
    private TestHost test;
    private DatePicker picker;
    private TextBox other;
    private final List<String> events = new ArrayList<>();

    @BeforeEach
    void setUp() {
        Translations.setTranslator(new LocaleTranslator(Locale.US));
        this.test = new TestHost(400, 300);
        this.picker = this.test.factory().datePicker();
        this.other = this.test.factory().textBox();
        this.picker.onEvent(HasDate.PICK_DATE, event -> this.events.add("pick " + event.value()));
        this.picker.onEvent(HasDate.DATE_PICKED, event -> this.events.add("picked " + event.value()));
        this.picker.onEvent(Openable.OPEN, event -> this.events.add("open"));
        this.picker.onEvent(Openable.OPENED, event -> this.events.add("opened"));
        this.picker.onEvent(Closable.CLOSED, event -> this.events.add("closed"));
        this.test.show(this.test.factory().stackPanel(Orientation.VERTICAL).add(this.picker).add(this.other));
    }

    @AfterEach
    void resetTranslator() {
        Translations.setTranslator(Translator.NONE);
    }

    private List<String> texts() {
        final NullCanvas canvas = new NullCanvas();
        this.test.host().layout();
        this.test.host().render(canvas);
        return canvas.texts().stream().map(TextSpan::text).filter(text -> !text.isEmpty()).toList();
    }

    private void type(final String text) {
        text.codePoints().forEach(codePoint -> this.test.host().charTyped(codePoint, Modifiers.NONE));
    }

    private boolean press(final Key key) {
        return this.press(key, Modifiers.NONE);
    }

    private boolean press(final Key key, final Modifiers modifiers) {
        final boolean handled = this.test.host().key(key, KeyAction.PRESS, modifiers);
        this.test.host().key(key, KeyAction.RELEASE, modifiers);
        return handled;
    }

    @Test
    void anEmptyPickerShowsItsPlaceholderAndAsksForItsWidestText() {
        assertEquals("Pick a date", this.texts().get(0));
        assertTrue(this.picker.date().isEmpty());
        // "Pick a date" is wider than "MM/DD/YYYY": 4 + 66 + 4, then the button
        assertEquals(86f, this.picker.desiredWidth());
        assertEquals(19f, this.picker.desiredHeight());
        this.picker.setDate(LocalDate.of(2026, 10, 20));
        this.test.host().layout();
        assertEquals(86f, this.picker.desiredWidth(), "the width does not follow the value");
        assertEquals(List.of("10", "/", "20", "/", "2026"), this.texts());
    }

    @Test
    void focusedAndEmptyEachPartShowsDashes() {
        this.picker.requestFocus();
        assertEquals(List.of("--", "/", "--", "/", "----"), this.texts());
    }

    @Test
    void typingFillsThePartsAndEnterTakesTheDraft() {
        this.picker.requestFocus();
        this.type("10202026");
        assertTrue(this.picker.date().isEmpty(), "a draft is not the date yet");
        assertEquals(List.of("10", "/", "20", "/", "2026"), this.texts());
        assertTrue(this.press(Key.ENTER));
        assertEquals(LocalDate.of(2026, 10, 20), this.picker.date().orElseThrow());
        assertEquals(List.of("pick 2026-10-20", "picked 2026-10-20"), this.events);
    }

    @Test
    void aDigitThatCannotGrowMovesOn() {
        this.picker.requestFocus();
        this.type("342027");
        this.press(Key.ENTER);
        assertEquals(LocalDate.of(2027, 3, 4), this.picker.date().orElseThrow());
    }

    @Test
    void typingTheSeparatorMovesOn() {
        this.picker.requestFocus();
        this.type("1/5/2026");
        this.press(Key.ENTER);
        assertEquals(LocalDate.of(2026, 1, 5), this.picker.date().orElseThrow());
    }

    @Test
    void focusLossTakesACompleteDraft() {
        this.picker.requestFocus();
        this.type("10202026");
        this.other.requestFocus();
        assertEquals(LocalDate.of(2026, 10, 20), this.picker.date().orElseThrow());
    }

    @Test
    void focusLossDropsAnIncompleteDraft() {
        this.picker.setDate(LocalDate.of(2026, 1, 2));
        this.picker.requestFocus();
        this.type("11");
        this.press(Key.BACKSPACE);
        this.press(Key.ENTER);
        assertEquals(List.of("11", "/", "--", "/", "2026"), this.texts(), "Enter keeps an incomplete draft");
        this.other.requestFocus();
        assertEquals(LocalDate.of(2026, 1, 2), this.picker.date().orElseThrow());
        assertEquals(List.of("01", "/", "02", "/", "2026"), this.texts());
    }

    @Test
    void twoDigitYearsLandInTheHundredYearsAroundToday() {
        this.picker.requestFocus();
        this.type("102026");
        this.press(Key.ENTER);
        assertEquals(LocalDate.of(2026, 10, 20), this.picker.date().orElseThrow());
        this.picker.requestFocus();
        this.press(Key.END);
        this.type("99");
        this.press(Key.ENTER);
        assertEquals(LocalDate.of(1999, 10, 20), this.picker.date().orElseThrow());
    }

    @Test
    void aDayPastTheMonthsEndIsCut() {
        this.picker.requestFocus();
        this.type("02312026");
        this.press(Key.ENTER);
        assertEquals(LocalDate.of(2026, 2, 28), this.picker.date().orElseThrow());
    }

    @Test
    void upAndDownWrapWithinAPartWithoutCarrying() {
        this.picker.setDate(LocalDate.of(2026, 10, 31));
        this.picker.requestFocus();
        this.press(Key.RIGHT);
        this.press(Key.UP);
        this.press(Key.ENTER);
        assertEquals(LocalDate.of(2026, 10, 1), this.picker.date().orElseThrow());
        this.press(Key.HOME);
        this.press(Key.DOWN);
        this.press(Key.ENTER);
        assertEquals(LocalDate.of(2026, 9, 1), this.picker.date().orElseThrow());
    }

    @Test
    void anEmptyPartStepsFromToday() {
        this.picker.requestFocus();
        this.press(Key.UP);
        assertEquals(List.of("10", "/", "--", "/", "----"), this.texts());
    }

    @Test
    void backspaceEmptiesThePartAndEscapeDropsTheDraft() {
        this.picker.setDate(LocalDate.of(2026, 10, 20));
        this.picker.requestFocus();
        assertFalse(this.press(Key.ESCAPE), "nothing to drop, so Escape goes on");
        this.press(Key.BACKSPACE);
        assertEquals(List.of("--", "/", "20", "/", "2026"), this.texts());
        assertTrue(this.press(Key.ESCAPE));
        assertEquals(List.of("10", "/", "20", "/", "2026"), this.texts());
    }

    @Test
    void draftsOutsideTheRangeOrRefusedByTheFilterAreNotTaken() {
        this.picker.setDateRange(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));
        this.picker.setDateFilter(day -> day.getDayOfWeek() != DayOfWeek.SUNDAY);
        this.picker.requestFocus();
        this.type("11052026");
        this.press(Key.ENTER);
        assertTrue(this.picker.date().isEmpty());
        assertEquals(List.of("11", "/", "05", "/", "2026"), this.texts(), "Enter keeps a draft it cannot take");
        this.press(Key.ESCAPE);
        this.type("10182026");
        this.press(Key.ENTER);
        assertTrue(this.picker.date().isEmpty(), "18 October 2026 is a Sunday");
        this.other.requestFocus();
        assertTrue(this.picker.date().isEmpty());
        assertEquals("Pick a date", this.texts().get(0));
    }

    @Test
    void codeKeepsTheDateInsideTheRangeButIgnoresTheFilter() {
        this.picker.setDateFilter(day -> false);
        this.picker.setDate(LocalDate.of(2026, 10, 18));
        assertEquals(LocalDate.of(2026, 10, 18), this.picker.date().orElseThrow());
        this.picker.setDateRange(LocalDate.of(2026, 11, 1), null);
        assertEquals(LocalDate.of(2026, 11, 1), this.picker.date().orElseThrow());
        assertEquals(LocalDate.of(2026, 11, 1), this.picker.minDate().orElseThrow());
        assertTrue(this.picker.maxDate().isEmpty());
    }

    @Test
    void deleteClearsWhileClearable() {
        this.picker.setDate(LocalDate.of(2026, 10, 20));
        this.events.clear();
        this.picker.requestFocus();
        assertTrue(this.press(Key.DELETE));
        assertTrue(this.picker.date().isEmpty());
        assertEquals(List.of("pick null", "picked null"), this.events);
        this.picker.setDate(LocalDate.of(2026, 10, 20));
        this.picker.setClearable(false);
        assertFalse(this.press(Key.DELETE));
        assertEquals(LocalDate.of(2026, 10, 20), this.picker.date().orElseThrow());
    }

    @Test
    void altDownOpensTheCalendarAndAPickClosesIt() {
        this.picker.setDateRange(null, LocalDate.of(2026, 12, 31));
        this.picker.requestFocus();
        this.press(Key.DOWN, Modifiers.of(Modifier.ALT));
        assertTrue(this.picker.isOpen());
        assertEquals(List.of("open", "opened"), this.events);
        final CalendarView calendar = assertInstanceOf(CalendarView.class, this.test.focused());
        assertEquals(LocalDate.of(2026, 12, 31), calendar.maxDate().orElseThrow(), "the range goes to the calendar");
        this.test.host().layout();
        this.press(Key.RIGHT);
        this.press(Key.ENTER);
        assertFalse(this.picker.isOpen());
        assertEquals(LocalDate.of(2026, 10, 21), this.picker.date().orElseThrow());
        assertSame(this.picker, this.test.focused(), "focus comes back to the field");
        assertEquals(List.of("open", "opened", "closed", "pick 2026-10-21", "picked 2026-10-21"), this.events);
    }

    @Test
    void altF4IsLeftForTheWindow() {
        this.picker.requestFocus();
        assertFalse(this.press(Key.F4, Modifiers.of(Modifier.ALT)), "Alt+F4 is not consumed");
        assertFalse(this.picker.isOpen());
        this.press(Key.F4);
        assertTrue(this.picker.isOpen());
        assertFalse(this.press(Key.F4, Modifiers.of(Modifier.ALT)), "nor while the calendar is open");
        assertTrue(this.picker.isOpen());
    }

    @Test
    void escapeClosesTheCalendarWithNoChange() {
        this.picker.setDate(LocalDate.of(2026, 3, 3));
        this.events.clear();
        this.picker.requestFocus();
        this.press(Key.F4);
        this.test.host().layout();
        assertEquals(java.time.YearMonth.of(2026, 3),
                ((CalendarView) this.test.focused()).shownMonth(), "the calendar shows the date's month");
        this.press(Key.ESCAPE);
        assertFalse(this.picker.isOpen());
        assertEquals(LocalDate.of(2026, 3, 3), this.picker.date().orElseThrow());
        assertSame(this.picker, this.test.focused());
        assertEquals(List.of("open", "opened", "closed"), this.events);
    }

    @Test
    void theButtonOpensTheCalendarUnlessOpenIsCanceled() {
        this.picker.onEvent(Openable.OPEN, event -> event.cancel());
        this.test.host().layout();
        this.test.click(80f, 9f);
        assertFalse(this.picker.isOpen());
        assertEquals(List.of("open"), this.events);
    }

    @Test
    void theButtonOpensTheCalendar() {
        this.test.host().layout();
        this.test.click(80f, 9f);
        assertTrue(this.picker.isOpen());
        assertEquals(1, this.test.factory().overlays().open().size());
        this.picker.close();
        assertFalse(this.picker.isOpen());
        assertTrue(this.test.factory().overlays().open().isEmpty());
    }

    @Test
    void aClickOnAPartFocusesIt() {
        this.picker.setDate(LocalDate.of(2026, 10, 20));
        this.test.host().layout();
        // The year starts at 4 + 6 * 6 = 40
        this.test.click(50f, 9f);
        assertSame(this.picker, this.test.focused());
        this.press(Key.UP);
        this.press(Key.ENTER);
        assertEquals(LocalDate.of(2027, 10, 20), this.picker.date().orElseThrow());
    }

    @Test
    void aDisabledPickerDoesNotOpen() {
        this.picker.setEnabled(false);
        this.picker.open();
        assertFalse(this.picker.isOpen());
        assertFalse(this.picker.requestFocus());
    }

    @Test
    void theFieldFollowsTheLanguage() {
        this.picker.setDate(LocalDate.of(2026, 10, 20));
        Translations.setTranslator(new LocaleTranslator(Locale.GERMANY));
        assertEquals(List.of("20", ".", "10", ".", "2026"), this.texts());
        Translations.setTranslator(new LocaleTranslator(Locale.JAPAN));
        assertEquals(List.of("2026", "/", "10", "/", "20"), this.texts());
    }

    @Test
    void thePlaceholderCanBeReplaced() {
        this.picker.setPlaceholder("Due");
        assertEquals("Due", this.texts().get(0));
        assertEquals("Due", this.picker.placeholder().plain());
    }

    @Test
    void snapshotsHoldTheDateAndWhetherItIsOpen() {
        final List<ComponentSnapshot> seen = new ArrayList<>();
        this.picker.onEvent(HasDate.PICK_DATE, event -> seen.add(event.snapshot()));
        this.picker.setDate(LocalDate.of(2026, 1, 1));
        this.picker.requestFocus();
        this.press(Key.DELETE);
        assertEquals(new DatePicker.State(LocalDate.of(2026, 1, 1), false), seen.get(0).state());
    }

    @Test
    void aThemeNeedsTheTextBoxKeysAndTheSelectedDay() {
        final TestHost host = new TestHost(new TestPlatform(), Theme.builder("bare").build(), 200, 200);
        final ThemeException error = assertThrows(ThemeException.class, () -> host.show(host.factory().datePicker()));
        assertEquals(java.util.Set.of(TextBox.STYLE_NORMAL, TextBox.STYLE_FOCUSED, TextBox.STYLE_DISABLED,
                CalendarView.STYLE_DAY_SELECTED), error.missingKeys());
    }
}
