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

import java.time.LocalTime;
import java.util.Optional;

/**
 * A control that holds a time of day the user can pick, such as a time picker.
 *
 * <p>The time may be empty. It holds whole minutes, or whole seconds while seconds are shown: a time set from code
 * loses what it cannot show. A time of day has no range, since a range that wraps past midnight would need rules of
 * its own.</p>
 *
 * <pre>{@code
 * form.onEvent(HasTime.TIME_PICKED, event -> this.save());
 * }</pre>
 *
 * <p>Do not implement this interface.</p>
 *
 * @param <S> the control's own type, which the setters return
 */
public interface HasTime<S extends HasTime<S>> extends EventTarget<S> {
    /**
     * Asked before the user picks a time or clears it. The value is the time about to be set, or null when clearing.
     * A handler may rewrite it, null included; the new time then applies as {@link #setTime} does. Fires only for the
     * user's actions, and only when the time would change.
     */
    EventType<ValueRequestEvent<HasTime<?>, LocalTime>> PICK_TIME = EventType.valueRequest("has_time.pick");
    /**
     * Told after the time changed. The value is the new time and the previous value the old one; either is null when
     * empty. Fires for changes made from code too, including hiding seconds, which drops them.
     */
    EventType<ValueEvent<HasTime<?>, LocalTime>> TIME_PICKED = EventType.valueReport("has_time.picked");
    /**
     * The time, as {@link #time()} and {@link #setTime} say, null for none. Binds two ways through
     * {@link #TIME_PICKED}. A null value clears the time, and a control with no time writes null.
     */
    PropertyKey<HasTime<?>, LocalTime> TIME = PropertyKey.of("has_time.time", control -> control.time().orElse(null),
            HasTime::setTime, TIME_PICKED);

    /**
     * Returns the time.
     *
     * @return the time, or empty when none is picked
     */
    Optional<LocalTime> time();

    /**
     * Sets the time or clears it. The seconds are dropped unless {@linkplain #isSecondsShown() shown}, and the
     * fraction of a second always. Fires {@link #TIME_PICKED} if the time changed, and never {@link #PICK_TIME}. Works
     * while disabled too.
     *
     * @param time the time, or null to clear it
     * @return this control
     */
    S setTime(LocalTime time);

    /**
     * Clears the time, as {@code setTime(null)} does.
     *
     * @return this control
     */
    S clearTime();

    /**
     * Returns the clock format, when code set one.
     *
     * @return the format, or empty to follow the language
     */
    Optional<ClockFormat> clockFormat();

    /**
     * Sets whether hours count to 12 or 24, or follows the language again.
     *
     * @param format the format, or null to follow the language
     * @return this control
     */
    S setClockFormat(ClockFormat format);

    /**
     * Returns whether seconds show.
     *
     * @return false by default
     */
    boolean isSecondsShown();

    /**
     * Shows or hides seconds. Hiding them drops the seconds of the time, which fires {@link #TIME_PICKED} if they were
     * not zero.
     *
     * @param shown true to show seconds
     * @return this control
     */
    S setSecondsShown(boolean shown);

    /**
     * Returns how far apart the minutes offered to the user are.
     *
     * @return the step in minutes; 1 by default
     */
    int minuteStep();

    /**
     * Sets how far apart the minutes offered to the user are: the rows of a minute list and the step of Up and Down
     * on the minutes. Minutes the user types and minutes set from code are kept as they are.
     *
     * @param minutes the step, a divisor of 60
     * @return this control
     * @throws IllegalArgumentException if {@code minutes} does not divide 60
     */
    S setMinuteStep(int minutes);

    /**
     * Whether hours count to 12, with a half of the day, or to 24.
     */
    enum ClockFormat {
        /**
         * Hours from 1 to 12, with the first or second half of the day.
         */
        H12,
        /**
         * Hours from 0 to 23.
         */
        H24
    }
}
