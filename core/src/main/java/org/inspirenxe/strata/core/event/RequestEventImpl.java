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
import org.inspirenxe.strata.api.event.control.RequestEvent;

/**
 * A control's request without a value. It has no default behavior on the components it passes; the control that
 * fired it acts once delivery is over, unless the event was canceled.
 *
 * @param <C> the control type
 */
public class RequestEventImpl<C> extends EventImpl implements RequestEvent<C> {

    /**
     * Creates a request fired by a control.
     *
     * @param target the control
     */
    public RequestEventImpl(final Component<?> target) {
        super(target);
    }

    @Override
    @SuppressWarnings("unchecked")
    public final C control() {
        return (C) this.target();
    }
}
