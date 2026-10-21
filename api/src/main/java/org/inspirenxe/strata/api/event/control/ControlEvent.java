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
package org.inspirenxe.strata.api.event.control;

import org.inspirenxe.strata.api.event.Event;

/**
 * A control's Post event: something happened, such as a press or a close. It fires after the fact, whether the user
 * or code caused it, and cannot be canceled.
 *
 * <p>Only Strata creates events. Do not implement this interface.</p>
 *
 * @param <C> the control type
 */
public interface ControlEvent<C> extends Event {

    /**
     * Returns the control that fired the event. For a control that is a component this is also the event's
     * {@linkplain #target() target}. For one that is not, the target is a component that stands for it: a radio
     * group's {@code SELECTED} is aimed at the button newly selected, or at the one that lost the selection when it
     * was cleared, and an overlay's {@code CLOSED} at the overlay's content.
     *
     * @return the control, typed as the attribute or control that declares the event type
     */
    C control();
}
