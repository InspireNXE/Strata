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
import org.inspirenxe.strata.api.event.control.ValueRequestEvent;

/**
 * A control's request that carries a value handlers may replace.
 *
 * @param <C> the control type
 * @param <V> the value type
 */
public final class ValueRequestEventImpl<C, V> extends RequestEventImpl<C> implements ValueRequestEvent<C, V> {
    private V value;

    /**
     * Creates a request fired by a control.
     *
     * @param target the control
     * @param value the value the user's action produced
     */
    public ValueRequestEventImpl(final Component<?> target, final V value) {
        super(target);
        this.value = value;
    }

    @Override
    public V value() {
        return this.value;
    }

    @Override
    public void setValue(final V value) {
        this.value = value;
    }
}
