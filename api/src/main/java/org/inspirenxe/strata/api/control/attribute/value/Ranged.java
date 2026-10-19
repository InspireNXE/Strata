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

/**
 * A control that shows a number within a range, such as a slider, a spinner or a progress bar.
 *
 * <p>The value always lies within the range. Setting a value outside it clamps the value, and setting a range
 * that leaves the value outside it moves the value to the nearest end.</p>
 *
 * <p>Only Strata implements this interface.</p>
 *
 * @param <S> the control's own type, which the setters return
 */
public interface Ranged<S extends Ranged<S>> extends EventTarget<S> {
    /**
     * Asked before the user changes the value. The value is the one about to be set, already within the range and
     * on a step where the control has one. A handler may rewrite it; the control clamps the new value again, and
     * does nothing if it equals the current one. Fires only for the user's actions, and only when the value would
     * change. A control the user cannot change, such as a progress bar, never fires it.
     */
    EventType<ValueRequestEvent<Ranged<?>, Double>> CHANGE = EventType.valueRequest("ranged.change");
    /**
     * Told after the value changed. The value is the new one and the previous value the old one. Fires for changes
     * made from code too, including a change of range that moved the value.
     */
    EventType<ValueEvent<Ranged<?>, Double>> CHANGED = EventType.valueReport("ranged.changed");

    /**
     * Returns the value.
     *
     * @return the value, within the range
     */
    double value();

    /**
     * Sets the value, clamped to the range. Fires {@link #CHANGED} if it changed, and never {@link #CHANGE}.
     *
     * @param value the value
     * @return this control
     * @throws IllegalArgumentException if the value is NaN
     */
    S setValue(double value);

    /**
     * Returns the lower end of the range.
     *
     * @return the least value
     */
    double min();

    /**
     * Returns the upper end of the range.
     *
     * @return the greatest value
     */
    double max();

    /**
     * Sets the range and clamps the value to it.
     *
     * @param min the least value
     * @param max the greatest value
     * @return this control
     * @throws IllegalArgumentException if either end is not finite or {@code min} is greater than {@code max}, or
     *                                  where the control needs a span, if they are equal
     */
    S setRange(double min, double max);
}
