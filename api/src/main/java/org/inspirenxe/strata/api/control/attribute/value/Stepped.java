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

/**
 * A {@link Ranged} control whose value moves in steps, such as a slider or a spinner.
 *
 * <p>Do not implement this interface.</p>
 *
 * @param <S> the control's own type, which the setters return
 */
public interface Stepped<S extends Stepped<S>> extends Ranged<S> {

    /**
     * Returns the step.
     *
     * @return the step, or 0 when the control allows none and the value may take any number in the range
     */
    double step();

    /**
     * Sets the step. A control that keeps its value on its steps, such as a slider, rounds the value to them again; a
     * control that only moves its value by them, such as a spinner, leaves the value as it is. Each control says
     * which it does.
     *
     * @param step the step, or 0 for none where the control allows that
     * @return this control
     * @throws IllegalArgumentException if the step is negative or not finite, or 0 where the control needs a step
     */
    S setStep(double step);
}
