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
import org.inspirenxe.strata.api.control.attribute.value.HasTime;
import org.inspirenxe.strata.api.control.attribute.value.HasTime.ClockFormat;
import org.inspirenxe.strata.api.control.collection.ListView;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.control.field.TimePicker;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.api.layout.ScrollPanel;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.ThemeException;
import org.inspirenxe.strata.core.text.Translations;
import org.inspirenxe.strata.spi.Translator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The time picker in American English, which counts hours to 12, on the test platform.
 */
class TimePickerTest {
    private TestHost test;
    private TimePicker picker;
    private TextBox other;
    private final List<String> events = new ArrayList<>();

    @BeforeEach
    void setUp() {
        Translations.setTranslator(new LocaleTranslator(Locale.US));
        this.test = new TestHost(400, 300);
        this.picker = this.test.factory().timePicker();
        this.other = this.test.factory().textBox();
        this.picker.onEvent(HasTime.PICK_TIME, event -> this.events.add("pick " + event.value()));
        this.picker.onEvent(HasTime.TIME_PICKED, event -> this.events.add("picked " + event.value()));
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
        return canvas.texts().stream().map(TextSpan::text).filter(text -> !text.isBlank()).toList();
    }

    private void type(final String text) {
        text.codePoints().forEach(codePoint -> this.test.host().charTyped(codePoint, Modifiers.NONE));
    }

    private boolean press(final Key key) {
        final boolean handled = this.test.host().key(key, KeyAction.PRESS, Modifiers.NONE);
        this.test.host().key(key, KeyAction.RELEASE, Modifiers.NONE);
        return handled;
    }

    @SuppressWarnings("unchecked")
    private ListView<Integer> column(final int index) {
        final Component<?> content = this.test.factory().overlays().open().get(0).content();
        return (ListView<Integer>) ((Container<?>) content).children().get(index);
    }

    private void pressRow(final ListView<Integer> list, final int row) {
        final Rect bounds = list.bounds();
        final float y = bounds.y() + row * 13f + 6f - list.scrollY();
        this.test.host().pointerButton(bounds.x() + 10f, y, MouseButton.LEFT, true, Modifiers.NONE);
        this.test.host().pointerButton(bounds.x() + 10f, y, MouseButton.LEFT, false, Modifiers.NONE);
    }

    @Test
    void aTwelveHourFieldTakesTheHalfOfTheDayByItsFirstLetter() {
        this.picker.requestFocus();
        this.type("0930p");
        assertEquals(List.of("09", ":", "30", "PM"), this.texts());
        this.press(Key.ENTER);
        assertEquals(LocalTime.of(21, 30), this.picker.time().orElseThrow());
        assertEquals(List.of("pick 21:30", "picked 21:30"), this.events);
        this.press(Key.UP);
        this.press(Key.ENTER);
        assertEquals(LocalTime.of(9, 30), this.picker.time().orElseThrow(), "Up flips the half of the day");
    }

    @Test
    void aTwentyFourHourClockCanBeSetInCode() {
        this.picker.setClockFormat(ClockFormat.H24);
        this.picker.requestFocus();
        this.type("2130");
        this.press(Key.ENTER);
        assertEquals(LocalTime.of(21, 30), this.picker.time().orElseThrow());
        assertEquals(List.of("21", ":", "30"), this.texts());
        this.picker.setClockFormat(null);
        assertEquals(List.of("09", ":", "30", "PM"), this.texts());
    }

    @Test
    void setTimeDropsWhatItCannotShow() {
        this.picker.setTime(LocalTime.of(8, 15, 42, 5));
        assertEquals(LocalTime.of(8, 15), this.picker.time().orElseThrow());
        this.picker.setSecondsShown(true);
        this.picker.setTime(LocalTime.of(8, 15, 42, 5));
        assertEquals(LocalTime.of(8, 15, 42), this.picker.time().orElseThrow());
        assertEquals(List.of("08", ":", "15", ":", "42", "AM"), this.texts());
        this.events.clear();
        this.picker.setSecondsShown(false);
        assertEquals(LocalTime.of(8, 15), this.picker.time().orElseThrow());
        assertEquals(List.of("picked 08:15"), this.events);
    }

    @Test
    void theMinuteStepSetsHowUpAndDownMoveTheMinutes() {
        this.picker.setMinuteStep(5);
        this.picker.setTime(LocalTime.of(7, 7));
        assertEquals(LocalTime.of(7, 7), this.picker.time().orElseThrow(), "minutes from code are kept");
        this.picker.requestFocus();
        this.press(Key.RIGHT);
        this.press(Key.UP);
        this.press(Key.ENTER);
        assertEquals(LocalTime.of(7, 10), this.picker.time().orElseThrow());
        this.press(Key.DOWN);
        this.press(Key.DOWN);
        this.press(Key.ENTER);
        assertEquals(LocalTime.of(7, 0), this.picker.time().orElseThrow());
        this.press(Key.DOWN);
        this.press(Key.ENTER);
        assertEquals(LocalTime.of(7, 55), this.picker.time().orElseThrow(), "the minutes wrap without carrying");
        assertThrows(IllegalArgumentException.class, () -> this.picker.setMinuteStep(7));
        assertThrows(IllegalArgumentException.class, () -> this.picker.setMinuteStep(0));
    }

    @Test
    void anInvalidHourIsNotTaken() {
        this.picker.requestFocus();
        this.type("00");
        this.type("15a");
        this.press(Key.ENTER);
        assertTrue(this.picker.time().isEmpty(), "a 12 hour clock has no hour 0");
        this.other.requestFocus();
        assertEquals(List.of("Pick a time"), this.texts());
    }

    @Test
    void thePopupHasAColumnForEachPartAndPicksAtOnce() {
        this.picker.requestFocus();
        this.press(Key.F4);
        assertTrue(this.picker.isOpen());
        this.test.host().layout();
        final ListView<Integer> hours = this.column(0);
        final ListView<Integer> minutes = this.column(1);
        final ListView<Integer> halves = this.column(2);
        assertEquals(12, hours.items().size());
        assertEquals(60, minutes.items().size());
        assertEquals(List.of(0, 1), halves.items());
        assertEquals(13f * TimePicker.VISIBLE_ROWS, hours.bounds().height());

        this.pressRow(hours, 3);
        assertEquals(LocalTime.of(3, 0), this.picker.time().orElseThrow(), "the other parts start at zero");
        assertSame(this.picker, this.test.focused(), "focus comes back to the field");
        assertTrue(this.picker.isOpen());
        this.pressRow(halves, 1);
        assertEquals(LocalTime.of(15, 0), this.picker.time().orElseThrow());
        assertEquals(List.of("pick 03:00", "picked 03:00", "pick 15:00", "picked 15:00"), this.events);
        assertEquals(3, hours.selectedIndex());
        assertEquals(1, halves.selectedIndex());
    }

    @Test
    void upAndDownSetTheValueAtOnceWhileOpen() {
        this.picker.setTime(LocalTime.of(10, 0));
        this.events.clear();
        this.picker.requestFocus();
        this.press(Key.F4);
        this.press(Key.RIGHT);
        this.press(Key.UP);
        assertEquals(LocalTime.of(10, 1), this.picker.time().orElseThrow());
        this.press(Key.LEFT);
        this.press(Key.DOWN);
        assertEquals(LocalTime.of(9, 1), this.picker.time().orElseThrow());
        assertTrue(this.press(Key.ENTER));
        assertFalse(this.picker.isOpen());
        assertEquals(List.of("pick 10:01", "picked 10:01", "pick 09:01", "picked 09:01"), this.events);
    }

    @Test
    void escapeClosesThePopup() {
        this.picker.requestFocus();
        this.press(Key.F4);
        assertTrue(this.press(Key.ESCAPE));
        assertFalse(this.picker.isOpen());
        assertTrue(this.picker.time().isEmpty());
    }

    @Test
    void secondsGetAColumnWhileShown() {
        this.picker.setSecondsShown(true).setMinuteStep(15).setClockFormat(ClockFormat.H24);
        this.picker.requestFocus();
        this.press(Key.F4);
        this.test.host().layout();
        assertEquals(24, this.column(0).items().size());
        assertEquals(List.of(0, 15, 30, 45), this.column(1).items());
        assertEquals(60, this.column(2).items().size());
        assertEquals(3, ((Container<?>) this.test.factory().overlays().open().get(0).content()).children().size());
    }

    @Test
    void aThemeNeedsTheListKeys() {
        final TestHost host = new TestHost(new TestPlatform(), Theme.builder("bare").build(), 200, 200);
        final ThemeException error = assertThrows(ThemeException.class, () -> host.show(host.factory().timePicker()));
        assertEquals(Set.of(TextBox.STYLE_NORMAL, TextBox.STYLE_FOCUSED, TextBox.STYLE_DISABLED,
                ListView.STYLE_ROW_SELECTED, ScrollPanel.STYLE_THUMB), error.missingKeys());
    }
}
