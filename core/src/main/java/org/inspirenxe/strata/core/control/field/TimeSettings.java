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

import org.inspirenxe.strata.api.control.attribute.value.HasTime.ClockFormat;
import org.inspirenxe.strata.core.text.DateFormats;

import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * How a time picker or a date and time picker shows the time: the clock, whether seconds show, and the minute step,
 * with the checks both pickers make.
 */
final class TimeSettings {
    private ClockFormat clockFormat;
    private boolean seconds;
    private int minuteStep;

    TimeSettings() {
        this.minuteStep = 1;
    }

    /**
     * Returns the clock code set, or empty to follow the language.
     */
    Optional<ClockFormat> clockFormat() {
        return Optional.ofNullable(this.clockFormat);
    }

    /**
     * Sets the clock, or follows the language again for null.
     */
    void setClockFormat(final ClockFormat format) {
        this.clockFormat = format;
    }

    /**
     * Returns whether hours count to 12 in a language: the clock code set, else the language's.
     */
    boolean twelveHour(final DateFormats formats) {
        return this.clockFormat == null ? formats.isTwelveHour() : this.clockFormat == ClockFormat.H12;
    }

    boolean seconds() {
        return this.seconds;
    }

    /**
     * Shows or hides the seconds.
     *
     * @return true if that changed anything
     */
    boolean setSeconds(final boolean shown) {
        if (shown == this.seconds) {
            return false;
        }
        this.seconds = shown;
        return true;
    }

    /**
     * Returns what a value is cut to: whole seconds while they show, whole minutes otherwise.
     */
    ChronoUnit unit() {
        return this.seconds ? ChronoUnit.SECONDS : ChronoUnit.MINUTES;
    }

    int minuteStep() {
        return this.minuteStep;
    }

    /**
     * Sets how far Up and Down move the minutes, and which minutes the popup lists.
     *
     * @throws IllegalArgumentException if the step does not divide 60
     */
    void setMinuteStep(final int minutes) {
        if (minutes < 1 || minutes > 60 || 60 % minutes != 0) {
            throw new IllegalArgumentException("The minute step must divide 60: " + minutes);
        }
        this.minuteStep = minutes;
    }
}
