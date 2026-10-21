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
package org.inspirenxe.strata.core.control.field;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.control.collection.ListView;
import org.inspirenxe.strata.api.control.field.CalendarView;
import org.inspirenxe.strata.api.control.field.DateTimePicker;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.api.layout.ScrollPanel;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;
import org.inspirenxe.strata.core.text.DateFormats;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * The core {@link DateTimePicker}: one field of date parts, a space and time parts over a {@link SegmentedField},
 * with a private {@link CalendarViewImpl} beside {@link TimeColumns} in its popup.
 *
 * <p>The value is kept to whole minutes, or seconds while they show, and inside the range. When cutting it to the
 * minute or second would leave it before the range's start, it moves up to the next whole one. The calendar in the
 * popup is not focusable, so focus stays in the field; a press on the calendar or a column takes it back. The popup's
 * parts are made the first time it opens and kept.</p>
 */
public final class DateTimePickerImpl extends PickerBase<DateTimePicker, LocalDateTime> implements DateTimePicker {
    private static final Set<String> EXTRA = Set.of(CalendarView.STYLE_DAY_SELECTED, ListView.STYLE_ROW_SELECTED,
            ScrollPanel.STYLE_THUMB);

    private LocalDateTime value;
    private LocalDateTime min;
    private LocalDateTime max;
    private Predicate<? super LocalDate> filter;
    private DayOfWeek firstDay;
    private ClockFormat clockFormat;
    private boolean seconds;
    private int minuteStep;
    private LocalTime defaultTime;
    private CalendarViewImpl calendar;
    private TimeColumns columns;
    private StackPanel popupRow;
    private TextStyle rowStyle;

    /**
     * Creates an empty picker.
     *
     * @param ui the factory that owns the picker
     */
    public DateTimePickerImpl(final ComponentFactoryImpl ui) {
        super(ui, Text.translatable("strata.datetime.placeholder"));
        this.filter = day -> true;
        this.minuteStep = 1;
        this.defaultTime = LocalTime.MIDNIGHT;
        this.rowStyle = TextStyle.DEFAULT;
    }

    // ------------------------------------------------------------------
    // The value
    // ------------------------------------------------------------------

    @Override
    public Optional<LocalDateTime> dateTime() {
        return Optional.ofNullable(this.value);
    }

    @Override
    public DateTimePicker setDateTime(final LocalDateTime value) {
        final LocalDateTime next = this.fit(value);
        final LocalDateTime previous = this.value;
        if (Objects.equals(next, previous)) {
            return this;
        }
        this.value = next;
        this.valueChanged();
        this.syncPopup();
        this.report(PICKED, next, previous);
        final LocalDate nextDate = next == null ? null : next.toLocalDate();
        final LocalDate previousDate = previous == null ? null : previous.toLocalDate();
        if (!Objects.equals(nextDate, previousDate)) {
            this.report(DATE_PICKED, nextDate, previousDate);
        }
        final LocalTime nextTime = next == null ? null : next.toLocalTime();
        final LocalTime previousTime = previous == null ? null : previous.toLocalTime();
        if (!Objects.equals(nextTime, previousTime)) {
            this.report(TIME_PICKED, nextTime, previousTime);
        }
        return this;
    }

    @Override
    public Optional<LocalDateTime> minDateTime() {
        return Optional.ofNullable(this.min);
    }

    @Override
    public Optional<LocalDateTime> maxDateTime() {
        return Optional.ofNullable(this.max);
    }

    @Override
    public DateTimePicker setRange(final LocalDateTime min, final LocalDateTime max) {
        if (min != null && max != null && min.isAfter(max)) {
            throw new IllegalArgumentException("The range must not end before it starts: " + min + " to " + max);
        }
        this.min = min;
        this.max = max;
        return this.setDateTime(this.value);
    }

    @Override
    public LocalTime defaultTime() {
        return this.defaultTime;
    }

    @Override
    public DateTimePicker setDefaultTime(final LocalTime time) {
        this.defaultTime = Objects.requireNonNull(time, "time");
        return this;
    }

    private ChronoUnit unit() {
        return this.seconds ? ChronoUnit.SECONDS : ChronoUnit.MINUTES;
    }

    /**
     * Cuts a value to the shown unit and keeps it inside the range.
     */
    private LocalDateTime fit(final LocalDateTime value) {
        if (value == null) {
            return null;
        }
        LocalDateTime fitted = value.truncatedTo(this.unit());
        if (this.max != null && fitted.isAfter(this.max)) {
            fitted = this.max.truncatedTo(this.unit());
        }
        if (this.min != null && fitted.isBefore(this.min)) {
            final LocalDateTime start = this.min.truncatedTo(this.unit());
            fitted = start.equals(this.min) ? start : start.plus(1L, this.unit());
        }
        return fitted;
    }

    private LocalDate clampDate(final LocalDate day) {
        final LocalDate first = this.min == null ? null : this.min.toLocalDate();
        final LocalDate last = this.max == null ? null : this.max.toLocalDate();
        if (first != null && day.isBefore(first)) {
            return first;
        }
        return last != null && day.isAfter(last) ? last : day;
    }

    private boolean isAllowed(final LocalDateTime value) {
        return (this.min == null || !value.isBefore(this.min)) && (this.max == null || !value.isAfter(this.max))
                && this.filter.test(value.toLocalDate());
    }

    // ------------------------------------------------------------------
    // HasDate
    // ------------------------------------------------------------------

    @Override
    public Optional<LocalDate> date() {
        return this.value == null ? Optional.empty() : Optional.of(this.value.toLocalDate());
    }

    @Override
    public DateTimePicker setDate(final LocalDate date) {
        if (date == null) {
            return this.setDateTime(null);
        }
        return this.setDateTime(date.atTime(this.value != null ? this.value.toLocalTime() : this.defaultTime));
    }

    @Override
    public DateTimePicker clearDate() {
        return this.setDateTime(null);
    }

    @Override
    public Optional<LocalDate> minDate() {
        return this.min == null ? Optional.empty() : Optional.of(this.min.toLocalDate());
    }

    @Override
    public Optional<LocalDate> maxDate() {
        return this.max == null ? Optional.empty() : Optional.of(this.max.toLocalDate());
    }

    @Override
    public DateTimePicker setDateRange(final LocalDate min, final LocalDate max) {
        if (min != null && max != null && min.isAfter(max)) {
            throw new IllegalArgumentException("The range must not end before it starts: " + min + " to " + max);
        }
        return this.setRange(min == null ? null : min.atStartOfDay(), max == null ? null : max.atTime(LocalTime.MAX));
    }

    @Override
    public DateTimePicker setDateFilter(final Predicate<? super LocalDate> allowed) {
        this.filter = Objects.requireNonNull(allowed, "allowed");
        return this;
    }

    @Override
    public Optional<DayOfWeek> firstDayOfWeek() {
        return Optional.ofNullable(this.firstDay);
    }

    @Override
    public DateTimePicker setFirstDayOfWeek(final DayOfWeek day) {
        this.firstDay = day;
        return this;
    }

    // ------------------------------------------------------------------
    // HasTime
    // ------------------------------------------------------------------

    @Override
    public Optional<LocalTime> time() {
        return this.value == null ? Optional.empty() : Optional.of(this.value.toLocalTime());
    }

    @Override
    public DateTimePicker setTime(final LocalTime time) {
        if (time == null) {
            return this.setDateTime(null);
        }
        return this.setDateTime((this.value != null ? this.value.toLocalDate() : this.clampDate(this.today())).atTime(time));
    }

    @Override
    public DateTimePicker clearTime() {
        return this.setDateTime(null);
    }

    @Override
    public Optional<ClockFormat> clockFormat() {
        return Optional.ofNullable(this.clockFormat);
    }

    @Override
    public DateTimePicker setClockFormat(final ClockFormat format) {
        this.clockFormat = format;
        this.relayoutField();
        return this;
    }

    @Override
    public boolean isSecondsShown() {
        return this.seconds;
    }

    @Override
    public DateTimePicker setSecondsShown(final boolean shown) {
        if (shown != this.seconds) {
            this.seconds = shown;
            this.relayoutField();
            this.setDateTime(this.value);
        }
        return this;
    }

    @Override
    public int minuteStep() {
        return this.minuteStep;
    }

    @Override
    public DateTimePicker setMinuteStep(final int minutes) {
        if (minutes < 1 || minutes > 60 || 60 % minutes != 0) {
            throw new IllegalArgumentException("The minute step must divide 60: " + minutes);
        }
        this.minuteStep = minutes;
        this.field.setMinuteStep(minutes);
        return this;
    }

    private boolean twelveHour(final DateFormats formats) {
        return this.clockFormat == null ? formats.isTwelveHour() : this.clockFormat == ClockFormat.H12;
    }

    // ------------------------------------------------------------------
    // PickerBase
    // ------------------------------------------------------------------

    @Override
    LocalDateTime value() {
        return this.value;
    }

    @Override
    LocalDateTime draftValue() {
        final LocalDate day = this.field.date(this.today());
        final LocalTime time = this.field.time();
        if (day == null || time == null) {
            return null;
        }
        final LocalDateTime draft = day.atTime(time);
        return this.isAllowed(draft) ? draft : null;
    }

    /**
     * Asks {@link #PICK}, then {@code PICK_DATE} and {@code PICK_TIME} for the parts that change, and sets the value
     * they end with.
     */
    @Override
    void userPick(final LocalDateTime candidate) {
        if (Objects.equals(candidate, this.value)) {
            return;
        }
        this.request(PICK, candidate, requested -> {
            final LocalDateTime fitted = this.fit(requested);
            final LocalDate oldDate = this.value == null ? null : this.value.toLocalDate();
            final LocalTime oldTime = this.value == null ? null : this.value.toLocalTime();
            final LocalDate newDate = fitted == null ? null : fitted.toLocalDate();
            final LocalTime newTime = fitted == null ? null : fitted.toLocalTime();
            final Consumer<LocalDate> afterDate = day -> {
                if (!Objects.equals(newTime, oldTime)) {
                    this.request(PICK_TIME, newTime, time -> this.finish(day, time));
                } else {
                    this.finish(day, newTime);
                }
            };
            if (!Objects.equals(newDate, oldDate)) {
                this.request(PICK_DATE, newDate, afterDate);
            } else {
                afterDate.accept(newDate);
            }
        });
    }

    /**
     * Sets the value from the parts the requests ended with, filling a missing part when the other is set.
     */
    private void finish(final LocalDate day, final LocalTime time) {
        if (day == null && time == null) {
            this.setDateTime(null);
            return;
        }
        final LocalDate date = day != null ? day : this.clampDate(this.today());
        this.setDateTime(date.atTime(time != null ? time : this.defaultTime));
    }

    @Override
    void layoutField(final DateFormats formats) {
        this.field.layout(formats, true, true, this.twelveHour(formats), this.seconds);
    }

    @Override
    void loadField() {
        this.field.load(this.value == null ? null : this.value.toLocalDate(),
                this.value == null ? null : this.value.toLocalTime());
    }

    @Override
    void fillForPopup() {
        this.field.fill(this.clampDate(this.today()), this.defaultTime);
    }

    @Override
    protected void onApplyTheme(final Theme theme) {
        super.onApplyTheme(theme);
        this.rowStyle = theme.textStyleOr(TextBox.STYLE_TEXT, TextStyle.DEFAULT);
    }

    @Override
    Component<?> popupContent() {
        if (this.popupRow == null) {
            this.calendar = new CalendarViewImpl(this.ui);
            this.calendar.setFocusable(false);
            this.calendar.setTabbable(false);
            this.calendar.route(this::pickDate);
            this.calendar.setAnchor(Anchor.TOP_LEFT);
            this.columns = new TimeColumns(this.ui, this::pickPart);
            this.columns.panel().setAnchor(Anchor.TOP_LEFT);
            this.popupRow = this.ui.stackPanel(Orientation.HORIZONTAL).setSpacing(4f)
                    .add(this.calendar).add(this.columns.panel());
        }
        final DateFormats formats = this.field.formats() != null ? this.field.formats() : DateFormats.current();
        final CalendarViewImpl view = this.calendar;
        view.setDateRange(this.min == null ? null : this.min.toLocalDate(),
                this.max == null ? null : this.max.toLocalDate());
        view.setDateFilter(this.filter);
        view.setFirstDayOfWeek(this.firstDay);
        view.setDate(this.value == null ? null : this.value.toLocalDate());
        view.setZoom(CalendarView.Zoom.MONTH);
        view.setShownMonth(YearMonth.from(this.value != null ? this.value.toLocalDate() : this.today()));
        this.columns.build(this.twelveHour(formats), this.seconds, this.minuteStep, formats, this.rowStyle,
                this.environment().text().lineHeight(this.rowStyle));
        this.columns.show(this.value == null ? null : this.value.toLocalTime(), this.twelveHour(formats));
        return this.popupRow;
    }

    private void syncPopup() {
        if (this.popupRow == null || !this.isOpen() || this.field.formats() == null) {
            return;
        }
        this.calendar.setDate(this.value == null ? null : this.value.toLocalDate());
        this.columns.show(this.value == null ? null : this.value.toLocalTime(), this.twelveHour(this.field.formats()));
    }

    /**
     * Sets the date from a day picked in the calendar, keeping the time or taking the default, and leaves the popup
     * open.
     */
    private void pickDate(final LocalDate day) {
        this.userPick(day.atTime(this.value != null ? this.value.toLocalTime() : this.defaultTime));
        this.loadField();
        if (!this.isFocused()) {
            this.requestFocus();
        }
    }

    /**
     * Sets one part of the time from a row the user pressed, filling an empty value first, and takes focus back.
     */
    private void pickPart(final SegmentedField.Kind kind, final int number) {
        this.loadField();
        if (this.value == null) {
            this.fillForPopup();
        }
        this.field.setPart(kind, number);
        final int index = this.field.indexOf(kind);
        final LocalDate day = this.field.date(this.today());
        final LocalTime time = this.field.time();
        if (day != null && time != null) {
            this.userPick(day.atTime(time));
        }
        this.loadField();
        if (!this.isFocused()) {
            this.requestFocus();
        }
        if (index >= 0) {
            this.field.setFocus(index);
        }
    }

    @Override
    String iconKey() {
        return STYLE_ICON;
    }

    @Override
    Set<String> extraRequiredKeys() {
        return EXTRA;
    }

    @Override
    protected Object snapshotState() {
        return new DateTimePicker.State(this.value, this.isOpen());
    }
}
