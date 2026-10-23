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
package org.inspirenxe.strata.api.control.attribute.popup;

import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.control.ControlEvent;
import org.inspirenxe.strata.api.event.control.RequestEvent;

/**
 * A control that opens a popup and closes it again, such as a combo box or a menu. Code opens it with
 * {@link #open()}; where and when it can open is particular to each control.
 *
 * <p>Do not implement this interface.</p>
 *
 * @param <S> the control's own type
 */
public interface Openable<S extends Openable<S>> extends Closable<S> {
    /**
     * Asked before the user opens it. Cancel it to keep it closed. Fires only for the user's actions, so a control
     * opened only from code or by another control never fires it.
     */
    EventType<RequestEvent<Openable<?>>> OPEN = EventType.request("openable.open");
    /**
     * Told after it opened, by the user or from code.
     */
    EventType<ControlEvent<Openable<?>>> OPENED = EventType.report("openable.opened");

    /**
     * Returns whether it is open.
     *
     * @return true while open; false for a new control
     */
    boolean isOpen();

    /**
     * Opens it from code and reports {@link #OPENED}, without asking {@link #OPEN}. Does nothing if it is already
     * open or cannot open now, which each control defines, such as a disabled combo box or a menu with no items.
     *
     * @return this control
     */
    S open();
}
