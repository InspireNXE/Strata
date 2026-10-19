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
import org.inspirenxe.strata.api.event.input.FocusEvent;

import java.util.Optional;

/**
 * The core {@link FocusEvent}. When focus moves, the host sends the component losing it an event first, then the
 * component gaining it.
 */
public final class FocusEventImpl extends EventImpl implements FocusEvent {
    private final Component<?> other;

    /**
     * Creates the event.
     *
     * @param target the component gaining or losing focus
     * @param other the component on the other side of the move, or {@code null} when focus comes from nothing or
     *     goes to nothing
     */
    public FocusEventImpl(final Component<?> target, final Component<?> other) {
        super(target);
        this.other = other;
    }

    @Override
    public Optional<Component<?>> other() {
        return Optional.ofNullable(this.other);
    }
}
