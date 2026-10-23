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
package org.inspirenxe.strata.api.control.attribute.value;

import org.inspirenxe.strata.api.event.EventTarget;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.control.ValueEvent;
import org.inspirenxe.strata.api.event.control.ValueRequestEvent;
import org.inspirenxe.strata.api.state.PropertyKey;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * A control that holds a date the user can pick, such as a calendar or a date picker.
 *
 * <p>The date may be empty. A date set from code is kept inside the range, as {@link Ranged} clamps its value. The
 * filter limits only the user: a date the filter refuses draws disabled and cannot be picked or typed, but code may
 * still set it. Clearing is always allowed from code.</p>
 *
 * <p>Dates run from 1 January of year 1 to 31 December 9999, so a year always fits four digits. A date outside those,
 * set from code or given as an end of the range, is moved to the nearer of them, and a range with an open end stops
 * there too.</p>
 *
 * <p>Dates carry no zone. A screen shows what the user picked, and the project decides what zone it means.</p>
 *
 * <pre>{@code
 * form.onEvent(HasDate.DATE_PICKED, event -> this.save());
 * }</pre>
 *
 * <p>Do not implement this interface.</p>
 *
 * @param <S> the control's own type, which the setters return
 */
public interface HasDate<S extends HasDate<S>> extends EventTarget<S> {
    /**
     * Asked before the user picks a date or clears it. The value is the date about to be set, or null when clearing.
     * A handler may rewrite it, null included; the new date then applies as {@link #setDate} does. Fires only for the
     * user's actions, and only when the date would change.
     */
    EventType<ValueRequestEvent<HasDate<?>, LocalDate>> PICK_DATE = EventType.valueRequest("has_date.pick");
    /**
     * Told after the date changed. The value is the new date and the previous value the old one; either is null when
     * empty. Fires for changes made from code too, including a change of range that moved the date.
     */
    EventType<ValueEvent<HasDate<?>, LocalDate>> DATE_PICKED = EventType.valueReport("has_date.picked");
    /**
     * The date, as {@link #date()} and {@link #setDate} say, null for none. Binds two ways through
     * {@link #DATE_PICKED}. A null value clears the date, and a control with no date writes null.
     */
    PropertyKey<HasDate<?>, LocalDate> DATE = PropertyKey.of("has_date.date", control -> control.date().orElse(null),
            HasDate::setDate, DATE_PICKED);

    /**
     * Returns the date.
     *
     * @return the date, or empty when none is picked
     */
    Optional<LocalDate> date();

    /**
     * Sets the date, kept inside the range, or clears it. Fires {@link #DATE_PICKED} if the date changed, and never
     * {@link #PICK_DATE}. The filter does not apply. Works while disabled too.
     *
     * @param date the date, or null to clear it
     * @return this control
     */
    S setDate(LocalDate date);

    /**
     * Clears the date, as {@code setDate(null)} does.
     *
     * @return this control
     */
    S clearDate();

    /**
     * Returns the earliest date the control holds.
     *
     * @return the earliest date, or empty for no limit
     */
    Optional<LocalDate> minDate();

    /**
     * Returns the latest date the control holds.
     *
     * @return the latest date, or empty for no limit
     */
    Optional<LocalDate> maxDate();

    /**
     * Sets the range of dates, both ends included, and moves a date outside it to the nearest end. An end outside the
     * supported dates is moved inside them, and the getters return it as moved.
     *
     * @param min the earliest date, or null for no limit but the first supported date
     * @param max the latest date, or null for no limit but the last supported date
     * @return this control
     * @throws IllegalArgumentException if both are given and {@code min} is after {@code max}
     */
    S setDateRange(LocalDate min, LocalDate max);

    /**
     * Sets which dates the user may pick. The control asks the filter whenever it draws or checks a date, so it may
     * read state that changes. Dates outside the range are refused whatever the filter says.
     *
     * @param allowed returns true for a date the user may pick; every date by default
     * @return this control
     */
    S setDateFilter(Predicate<? super LocalDate> allowed);

    /**
     * Returns the day weeks start on, when code set one.
     *
     * @return the day, or empty to follow the language
     */
    Optional<DayOfWeek> firstDayOfWeek();

    /**
     * Sets the day weeks start on, or follows the language again.
     *
     * @param day the day, or null to follow the language
     * @return this control
     */
    S setFirstDayOfWeek(DayOfWeek day);
}
