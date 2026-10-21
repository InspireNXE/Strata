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

import org.inspirenxe.strata.api.event.Cancelable;

/**
 * A control's Pre event: the user asked the control to do something, such as press or close, and it has not
 * happened yet. Cancel it to refuse. The control fires it through its whole path first, so an ancestor can refuse
 * too, and acts afterwards unless the event was canceled.
 *
 * <p>Request events fire only for what the user does. Changes made from code, through a control's setters and
 * methods, fire none.</p>
 *
 * <p>Only Strata creates events. Do not implement this interface.</p>
 *
 * @param <C> the control type
 */
public interface RequestEvent<C> extends Cancelable {

    /**
     * Returns the control that fired the event, which is also its {@linkplain #target() target}.
     *
     * @return the control, typed as the attribute or control that declares the event type
     */
    C control();
}
