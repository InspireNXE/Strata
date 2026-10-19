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
import org.inspirenxe.strata.api.event.input.CharEvent;
import org.inspirenxe.strata.api.input.Modifiers;

/**
 * The core {@link CharEvent}. The host creates one for each code point the engine reports as typed.
 */
public final class CharEventImpl extends EventImpl implements CharEvent {
    private final int codePoint;
    private final Modifiers modifiers;

    /**
     * Creates the event.
     *
     * @param target the focused component, or the screen's root when nothing has focus
     * @param codePoint the typed Unicode code point
     * @param modifiers the modifiers held while typing, or {@link Modifiers#NONE} when the engine does not report
     *     them
     */
    public CharEventImpl(final Component<?> target, final int codePoint, final Modifiers modifiers) {
        super(target);
        this.codePoint = codePoint;
        this.modifiers = modifiers;
    }

    @Override
    public int codePoint() {
        return this.codePoint;
    }

    @Override
    public Modifiers modifiers() {
        return this.modifiers;
    }
}
