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
 * The dates a calendar or a date picker holds: both ends included, either end open. The calendar, the date picker and
 * the date and time picker check and clamp their dates through it, so they agree on what the range allows.
 *
 * @param min the earliest date, or null for no limit
 * @param max the latest date, or null for no limit
 */
record DateRange(LocalDate min, LocalDate max) {
    /**
     * The range with no limit at either end.
     */
    static final DateRange ALL = new DateRange(null, null);

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

    /**
     * Returns whether a date lies inside the range.
     */
    boolean contains(final LocalDate day) {
        return (this.min == null || !day.isBefore(this.min)) && (this.max == null || !day.isAfter(this.max));
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
        if (this.min != null && day.isBefore(this.min)) {
            return this.min;
        }
        return this.max != null && day.isAfter(this.max) ? this.max : day;
    }

    /**
     * Returns whether any day of a month lies inside the range.
     */
    boolean contains(final YearMonth month) {
        return (this.min == null || !month.isBefore(YearMonth.from(this.min)))
                && (this.max == null || !month.isAfter(YearMonth.from(this.max)));
    }

    /**
     * Returns a month moved to the nearer end's month if no day of it lies inside the range.
     */
    YearMonth clamp(final YearMonth month) {
        if (this.min != null && month.isBefore(YearMonth.from(this.min))) {
            return YearMonth.from(this.min);
        }
        return this.max != null && month.isAfter(YearMonth.from(this.max)) ? YearMonth.from(this.max) : month;
    }

    /**
     * Returns whether any day of a year lies inside the range.
     */
    boolean containsYear(final int year) {
        return (this.min == null || year >= this.min.getYear()) && (this.max == null || year <= this.max.getYear());
    }
}
