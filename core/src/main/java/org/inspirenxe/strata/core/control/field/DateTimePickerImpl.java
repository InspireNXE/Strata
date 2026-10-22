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
 * <p>The value is kept to whole minutes, or seconds while they show, and inside the range, which never reaches past
 * the dates {@link DateRange} supports. A value is cut to the unit and then clamped to the first and last whole unit
 * inside the range, so a value cut to before a start that carries seconds moves up to the next whole minute; typed
 * drafts are allowed by the same two ends. A range too short to hold a whole unit keeps the value at its start. The calendar in the
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
    private final TimeSettings settings;
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
        this.settings = new TimeSettings();
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
        this.min = min == null ? null : limit(min);
        this.max = max == null ? null : limit(max);
        return this.setDateTime(this.value);
    }

    /**
     * Moves a range end inside the supported dates.
     */
    private static LocalDateTime limit(final LocalDateTime end) {
        final LocalDateTime first = DateRange.FIRST.atStartOfDay();
        final LocalDateTime last = DateRange.LAST.atTime(LocalTime.MAX);
        if (end.isBefore(first)) {
            return first;
        }
        return end.isAfter(last) ? last : end;
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
        return this.settings.unit();
    }

    /**
     * The dates of the range.
     */
    private DateRange dates() {
        return new DateRange(this.min == null ? null : this.min.toLocalDate(), this.max == null ? null : this.max.toLocalDate());
    }

    /**
     * The start of the range, or of the supported dates when it has none.
     */
    private LocalDateTime start() {
        return this.min == null ? DateRange.FIRST.atStartOfDay() : this.min;
    }

    /**
     * The first whole unit at or after the range's start.
     */
    private LocalDateTime firstWhole() {
        final LocalDateTime start = this.start();
        final LocalDateTime cut = start.truncatedTo(this.unit());
        return cut.equals(start) ? cut : cut.plus(1L, this.unit());
    }

    /**
     * The last whole unit at or before the range's end, or the end of the supported dates when it has none.
     */
    private LocalDateTime lastWhole() {
        return (this.max == null ? DateRange.LAST.atTime(LocalTime.MAX) : this.max).truncatedTo(this.unit());
    }

    /**
     * Cuts a value to the shown unit and clamps it to the first and last whole unit inside the range. A range too
     * short to hold a whole unit gives its start.
     */
    private LocalDateTime fit(final LocalDateTime value) {
        if (value == null) {
            return null;
        }
        final LocalDateTime first = this.firstWhole();
        final LocalDateTime last = this.lastWhole();
        if (first.isAfter(last)) {
            return this.start();
        }
        final LocalDateTime cut = value.truncatedTo(this.unit());
        if (cut.isBefore(first)) {
            return first;
        }
        return cut.isAfter(last) ? last : cut;
    }

    private LocalDate clampDate(final LocalDate day) {
        return this.dates().clamp(day);
    }

    /**
     * Returns whether the user may take a draft: a whole unit from the first to the last whole one inside the range,
     * on a date the filter allows.
     */
    private boolean isAllowed(final LocalDateTime value) {
        return !value.isBefore(this.firstWhole()) && !value.isAfter(this.lastWhole())
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
        return this.settings.clockFormat();
    }

    @Override
    public DateTimePicker setClockFormat(final ClockFormat format) {
        this.settings.setClockFormat(format);
        this.relayoutField();
        return this;
    }

    @Override
    public boolean isSecondsShown() {
        return this.settings.seconds();
    }

    @Override
    public DateTimePicker setSecondsShown(final boolean shown) {
        if (this.settings.setSeconds(shown)) {
            this.relayoutField();
            this.setDateTime(this.value);
        }
        return this;
    }

    @Override
    public int minuteStep() {
        return this.settings.minuteStep();
    }

    @Override
    public DateTimePicker setMinuteStep(final int minutes) {
        this.settings.setMinuteStep(minutes);
        this.field.setMinuteStep(minutes);
        return this;
    }

    private boolean twelveHour(final DateFormats formats) {
        return this.settings.twelveHour(formats);
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
        this.field.layout(formats, true, true, this.twelveHour(formats), this.settings.seconds());
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
        view.setDateRange(this.dates().min(), this.dates().max());
        view.setDateFilter(this.filter);
        view.setFirstDayOfWeek(this.firstDay);
        view.setDate(this.value == null ? null : this.value.toLocalDate());
        view.setZoom(CalendarView.Zoom.MONTH);
        view.setShownMonth(YearMonth.from(this.value != null ? this.value.toLocalDate() : this.today()));
        this.columns.build(this.twelveHour(formats), this.settings.seconds(), this.settings.minuteStep(), formats,
                this.rowStyle,
                this.environment().text().lineHeight(this.rowStyle));
        this.columns.show(this.value == null ? null : this.value.toLocalTime(), this.twelveHour(formats));
        return this.popupRow;
    }

    @Override
    void syncPopup() {
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
     * The draft's date and time without the range or the filter, which the requests and {@link #setDateTime} apply.
     */
    @Override
    LocalDateTime pickedValue() {
        final LocalDate day = this.field.date(this.today());
        final LocalTime time = this.field.time();
        return day == null || time == null ? null : day.atTime(time);
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
