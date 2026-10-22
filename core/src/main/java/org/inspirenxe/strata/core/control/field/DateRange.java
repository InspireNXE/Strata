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

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * The dates a calendar or a date picker holds: both ends included, either end open, and never outside the dates the
 * date controls support, {@link #FIRST} to {@link #LAST}. The calendar, the date picker and the date and time picker
 * check and clamp their dates through it, so they agree on what the range allows, and a date never reaches a year the
 * field cannot show in four digits or a month whose grid would leave what {@link LocalDate} holds.
 *
 * @param min the earliest date, or null for no limit but {@link #FIRST}; an earlier one is moved to it
 * @param max the latest date, or null for no limit but {@link #LAST}; a later one is moved to it
 */
record DateRange(LocalDate min, LocalDate max) {
    /**
     * The first date the date controls support: 1 January of year 1.
     */
    static final LocalDate FIRST = LocalDate.of(1, 1, 1);
    /**
     * The last date the date controls support: 31 December 9999.
     */
    static final LocalDate LAST = LocalDate.of(9999, 12, 31);
    /**
     * The range with no limit at either end but the supported dates.
     */
    static final DateRange ALL = new DateRange(null, null);

    /**
     * Moves the ends inside the supported dates.
     */
    DateRange {
        min = min == null ? null : limit(min);
        max = max == null ? null : limit(max);
    }

    /**
     * Makes a range, refusing one that ends before it starts.
     *
     * @throws IllegalArgumentException if both ends are given and {@code min} is after {@code max}
     */
    static DateRange of(final LocalDate min, final LocalDate max) {
        if (min != null && max != null && min.isAfter(max)) {
            throw new IllegalArgumentException("The range must not end before it starts: " + min + " to " + max);
        }
        return new DateRange(min, max);
    }

    private static LocalDate limit(final LocalDate day) {
        if (day.isBefore(FIRST)) {
            return FIRST;
        }
        return day.isAfter(LAST) ? LAST : day;
    }

    /**
     * Returns the earliest date the range allows.
     */
    LocalDate first() {
        return this.min == null ? FIRST : this.min;
    }

    /**
     * Returns the latest date the range allows.
     */
    LocalDate last() {
        return this.max == null ? LAST : this.max;
    }

    /**
     * Returns whether a date lies inside the range.
     */
    boolean contains(final LocalDate day) {
        return !day.isBefore(this.first()) && !day.isAfter(this.last());
    }

    /**
     * Returns a date moved to the nearer end if it lies outside the range.
     *
     * @param day the date, or null
     * @return the date inside the range, or null for null
     */
    LocalDate clamp(final LocalDate day) {
        if (day == null) {
            return null;
        }
        if (day.isBefore(this.first())) {
            return this.first();
        }
        return day.isAfter(this.last()) ? this.last() : day;
    }

    /**
     * Returns whether any day of a month lies inside the range.
     */
    boolean contains(final YearMonth month) {
        return !month.isBefore(YearMonth.from(this.first())) && !month.isAfter(YearMonth.from(this.last()));
    }

    /**
     * Returns a month moved to the nearer end's month if no day of it lies inside the range.
     */
    YearMonth clamp(final YearMonth month) {
        if (month.isBefore(YearMonth.from(this.first()))) {
            return YearMonth.from(this.first());
        }
        return month.isAfter(YearMonth.from(this.last())) ? YearMonth.from(this.last()) : month;
    }

    /**
     * Returns whether any day of a year lies inside the range.
     */
    boolean containsYear(final int year) {
        return year >= this.first().getYear() && year <= this.last().getYear();
    }

    /**
     * Returns whether any day of the ten years from {@code start} lies inside the range.
     */
    boolean containsDecade(final int start) {
        return start + 9 >= this.first().getYear() && start <= this.last().getYear();
    }
}
