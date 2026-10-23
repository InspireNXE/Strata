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
package org.inspirenxe.strata.spi;

/**
 * A monotonic time source, and the wall clock that dates shown to the user come from. Strata never reads the system
 * clock itself, so tests can drive time.
 *
 * <h2>Requirements for implementors</h2>
 * <ul>
 *     <li>Never go backward. Only differences between readings matter; the zero point is arbitrary.</li>
 *     <li>Advance in real time while the UI runs, or click counting, caret blinking and key repeat stall.</li>
 *     <li>{@link #wall()} returns the clock and zone that today's date is read from, such as the day a calendar
 *     marks. The default is the system clock in the default zone. An adapter may return the engine's own clock or
 *     zone instead, and a test a fixed clock. Core reads it on the UI thread: a calendar about once a minute while it
 *     shows, and a date or time picker each time the user steps a field, opens its popup or fills in today. Keep it
 *     cheap, as the system clock is. It must not return null.</li>
 * </ul>
 */
@FunctionalInterface
public interface Clock {
    /**
     * The JVM's monotonic clock, {@link System#nanoTime()}.
     */
    Clock SYSTEM = System::nanoTime;

    /**
     * Reads the clock.
     *
     * @return nanoseconds since an arbitrary fixed point
     */
    long nanoTime();

    /**
     * Reads the clock in milliseconds.
     *
     * @return {@link #nanoTime()} divided by one million
     */
    default long millis() {
        return this.nanoTime() / 1_000_000L;
    }

    /**
     * Returns the wall clock and zone that dates shown to the user come from, such as a calendar's today. Unlike
     * {@link #nanoTime()}, it may jump when the system time or the zone changes.
     *
     * @return the system clock in the default zone, unless the platform returns another
     */
    default java.time.Clock wall() {
        return java.time.Clock.systemDefaultZone();
    }
}
