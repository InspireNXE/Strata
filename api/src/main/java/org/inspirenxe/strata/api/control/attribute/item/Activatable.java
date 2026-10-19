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
package org.inspirenxe.strata.api.control.attribute.item;

import org.inspirenxe.strata.api.event.EventTarget;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.control.ValueEvent;
import org.inspirenxe.strata.api.event.control.ValueRequestEvent;

/**
 * A control whose entries the user can activate, to open or use one, such as a list, a table, a tree or a grid.
 *
 * <p>A double click on an entry activates it, and so does Enter while the control has focus and an entry is
 * selected. With {@link #setActivateOnClick} a single click does instead, as in a drop-down.</p>
 *
 * <p>Only Strata implements this interface.</p>
 *
 * @param <S> the control's own type, which the setter returns
 */
public interface Activatable<S extends Activatable<S>> extends EventTarget<S> {
    /**
     * Asked before the user activates an entry. The value is the entry's index, as for
     * {@link SingleSelection}. A handler may rewrite it to another entry; the control ignores an index it does not
     * show. Fires only for the user's actions.
     */
    EventType<ValueRequestEvent<Activatable<?>, Integer>> ACTIVATE = EventType.valueRequest("activatable.activate");
    /**
     * Told after the user activated an entry. The value is the entry's index, and the previous value the same
     * index. There is no method that activates an entry from code.
     */
    EventType<ValueEvent<Activatable<?>, Integer>> ACTIVATED = EventType.valueReport("activatable.activated");

    /**
     * Sets whether a single click activates an entry, instead of a double click.
     *
     * @param onClick true to activate on every click; false by default
     * @return this control
     */
    S setActivateOnClick(boolean onClick);
}
