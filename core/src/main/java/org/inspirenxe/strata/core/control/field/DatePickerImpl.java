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
import org.inspirenxe.strata.api.control.field.CalendarView;
import org.inspirenxe.strata.api.control.field.DatePicker;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;
import org.inspirenxe.strata.core.text.DateFormats;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The core {@link DatePicker}: a field of day, month and year parts over a {@link SegmentedField}, with a private
 * {@link CalendarViewImpl} in its popup.
 *
 * <p>The calendar is made the first time the popup opens and kept. Each time it opens it gets the picker's range,
 * filter and first day of the week, the date, and the date's month or today's, in the month view, and then takes
 * focus. It routes the user's picks to the picker, which closes the popup and asks {@link #PICK_DATE} itself, so the
 * calendar fires no requests. When the popup closes with nothing focused, as after Escape or a pick, the picker takes
 * focus back.</p>
 */
public final class DatePickerImpl extends PickerBase<DatePicker, LocalDate> implements DatePicker {
    private static final Set<String> EXTRA = Set.of(CalendarView.STYLE_DAY_SELECTED);

    private LocalDate date;
    private DateRange range;
    private Predicate<? super LocalDate> filter;
    private DayOfWeek firstDay;
    private CalendarViewImpl calendar;

    /**
     * Creates an empty picker.
     *
     * @param ui the factory that owns the picker
     */
    public DatePickerImpl(final ComponentFactoryImpl ui) {
        super(ui, Text.translatable("strata.date.placeholder"));
        this.range = DateRange.ALL;
        this.filter = day -> true;
    }

    // ------------------------------------------------------------------
    // HasDate
    // ------------------------------------------------------------------

    @Override
    public Optional<LocalDate> date() {
        return Optional.ofNullable(this.date);
    }

    @Override
    public DatePicker setDate(final LocalDate date) {
        final LocalDate next = this.range.clamp(date);
        final LocalDate previous = this.date;
        if (!Objects.equals(next, previous)) {
            this.date = next;
            this.valueChanged();
            if (this.calendar != null && this.isOpen()) {
                this.calendar.setDate(next);
            }
            this.report(DATE_PICKED, next, previous);
            this.validate();
        }
        return this;
    }

    @Override
    public DatePicker clearDate() {
        return this.setDate(null);
    }

    @Override
    public Optional<LocalDate> minDate() {
        return Optional.ofNullable(this.range.min());
    }

    @Override
    public Optional<LocalDate> maxDate() {
        return Optional.ofNullable(this.range.max());
    }

    @Override
    public DatePicker setDateRange(final LocalDate min, final LocalDate max) {
        this.range = DateRange.of(min, max);
        return this.setDate(this.date);
    }

    @Override
    public DatePicker setDateFilter(final Predicate<? super LocalDate> allowed) {
        this.filter = Objects.requireNonNull(allowed, "allowed");
        return this;
    }

    @Override
    public Optional<DayOfWeek> firstDayOfWeek() {
        return Optional.ofNullable(this.firstDay);
    }

    @Override
    public DatePicker setFirstDayOfWeek(final DayOfWeek day) {
        this.firstDay = day;
        return this;
    }

    private boolean isAllowed(final LocalDate day) {
        return this.range.contains(day) && this.filter.test(day);
    }

    // ------------------------------------------------------------------
    // PickerBase
    // ------------------------------------------------------------------

    @Override
    LocalDate value() {
        return this.date;
    }

    @Override
    LocalDate draftValue() {
        final LocalDate draft = this.field.date(this.today());
        return draft != null && this.isAllowed(draft) ? draft : null;
    }

    @Override
    void userPick(final LocalDate value) {
        if (!Objects.equals(value, this.date)) {
            this.request(PICK_DATE, value, this::setDate);
        }
    }

    @Override
    void layoutField(final DateFormats formats) {
        this.field.layout(formats, true, false, false, false);
    }

    @Override
    void loadField() {
        this.field.load(this.date, null);
    }

    @Override
    void fillForPopup() {
        this.field.fill(this.range.clamp(this.today()), null);
    }

    @Override
    Component<?> popupContent() {
        if (this.calendar == null) {
            this.calendar = new CalendarViewImpl(this.ui);
            this.calendar.route(this::pickFromCalendar);
        }
        final CalendarViewImpl view = this.calendar;
        view.setDateRange(this.range.min(), this.range.max());
        view.setDateFilter(this.filter);
        view.setFirstDayOfWeek(this.firstDay);
        view.setDate(this.date);
        view.setZoom(CalendarView.Zoom.MONTH);
        view.setShownMonth(YearMonth.from(this.date != null ? this.date : this.today()));
        return view;
    }

    @Override
    void popupOpened() {
        this.calendar.requestFocus();
    }

    private void pickFromCalendar(final LocalDate day) {
        this.close();
        this.userPick(day);
        if (!this.isFocused()) {
            this.requestFocus();
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
        return new DatePicker.State(this.date, this.isOpen());
    }
}
