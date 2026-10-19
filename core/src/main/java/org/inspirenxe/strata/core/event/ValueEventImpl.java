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
package org.inspirenxe.strata.core.event;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.event.control.ValueEvent;

/**
 * A control's report that carries the value after a change and the one before it.
 *
 * @param <C> the control type
 * @param <V> the value type
 */
public final class ValueEventImpl<C, V> extends ControlEventImpl<C> implements ValueEvent<C, V> {
    private final V value;
    private final V previous;

    /**
     * Creates a report fired by a control.
     *
     * @param target the control
     * @param value the value after the change
     * @param previous the value before the change
     */
    public ValueEventImpl(final Component<?> target, final V value, final V previous) {
        super(target);
        this.value = value;
        this.previous = previous;
    }

    /**
     * Creates a report fired for something that is not a component, aimed at a component that stands for it.
     *
     * @param target the component the event is aimed at
     * @param control what {@link #control()} returns
     * @param value the value after the change
     * @param previous the value before the change
     */
    public ValueEventImpl(final Component<?> target, final C control, final V value, final V previous) {
        super(target, control);
        this.value = value;
        this.previous = previous;
    }

    @Override
    public V value() {
        return this.value;
    }

    @Override
    public V previous() {
        return this.previous;
    }
}
