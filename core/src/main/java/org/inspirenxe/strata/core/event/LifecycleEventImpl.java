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
import org.inspirenxe.strata.api.event.component.LifecycleEvent;

/**
 * The event of {@link Component#ATTACHED} and {@link Component#DETACHED}. A node makes one only when it has a
 * handler for the type it fires.
 */
public final class LifecycleEventImpl extends EventImpl implements LifecycleEvent {

    /**
     * Creates the event of a component joining or leaving a shown tree.
     *
     * @param target the component
     */
    public LifecycleEventImpl(final Component<?> target) {
        super(target);
    }
}
