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
import org.inspirenxe.strata.api.event.input.ModifiersEvent;
import org.inspirenxe.strata.api.input.Modifiers;

import java.util.Objects;

/**
 * The core {@link ModifiersEvent}. The host fires one at the screen's content and at each open overlay's content
 * whenever the held modifiers change.
 */
public final class ModifiersEventImpl extends EventImpl implements ModifiersEvent {
    private final Modifiers modifiers;

    /**
     * Creates the event.
     *
     * @param target the component it is fired at
     * @param modifiers the modifiers now held
     */
    public ModifiersEventImpl(final Component<?> target, final Modifiers modifiers) {
        super(target);
        this.modifiers = Objects.requireNonNull(modifiers, "modifiers");
    }

    @Override
    public Modifiers modifiers() {
        return this.modifiers;
    }
}
