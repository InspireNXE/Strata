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

import org.inspirenxe.strata.api.event.EventTarget;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.control.ControlEvent;
import org.inspirenxe.strata.api.event.control.RequestEvent;

/**
 * Something that can be closed, such as a window, an overlay, a combo box's list or a menu.
 *
 * <p>Only Strata implements this interface.</p>
 *
 * @param <S> the control's own type, which {@link #close()} returns
 */
public interface Closable<S extends Closable<S>> extends EventTarget<S> {
    /**
     * Asked before the user closes it, as each implementation describes, such as a press on a window's close button
     * or Escape. Cancel it to keep it open. Fires only for the user's actions, and only where the implementation says
     * so.
     */
    EventType<RequestEvent<Closable<?>>> CLOSE = EventType.request("closable.close");
    /**
     * Told after it closed, by the user or from code.
     */
    EventType<ControlEvent<Closable<?>>> CLOSED = EventType.report("closable.closed");

    /**
     * Closes it and fires {@link #CLOSED}, never {@link #CLOSE}. Each implementation says what closing does and what a
     * second call does.
     *
     * @return this
     */
    S close();
}
