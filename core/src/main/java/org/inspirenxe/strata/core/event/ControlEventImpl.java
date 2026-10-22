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
import org.inspirenxe.strata.api.event.control.ControlEvent;

/**
 * A control's report without a value. It has no default behavior on the components it passes.
 *
 * @param <C> the control type
 */
public class ControlEventImpl<C> extends EventImpl implements ControlEvent<C> {
    private final C control;

    /**
     * Creates a report fired by a control, which is also the target.
     *
     * @param target the control
     */
    @SuppressWarnings("unchecked")
    public ControlEventImpl(final Component<?> target) {
        this(target, (C) target);
    }

    /**
     * Creates a report fired for something that is not a component, such as a radio group or an overlay, aimed at
     * a component that stands for it.
     *
     * @param target the component the event is aimed at
     * @param control what {@link #control()} returns
     */
    public ControlEventImpl(final Component<?> target, final C control) {
        super(target);
        this.control = control;
    }

    @Override
    public final C control() {
        return this.control;
    }
}
