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
import org.inspirenxe.strata.api.event.input.PointerEvent;
import org.inspirenxe.strata.api.input.Modifiers;

/**
 * The core {@link PointerEvent}. The host uses it directly for pointer enter, leave and move; the button, drag and
 * scroll events extend it.
 */
public class PointerEventImpl extends EventImpl implements PointerEvent {
    private final float x;
    private final float y;
    private final Modifiers modifiers;

    /**
     * Creates the event.
     *
     * @param target the component the event is for
     * @param x the pointer's x, in the host's coordinates
     * @param y the pointer's y, in the host's coordinates
     * @param modifiers the modifiers held, or {@link Modifiers#NONE} when the engine does not report them
     */
    public PointerEventImpl(final Component<?> target, final float x, final float y, final Modifiers modifiers) {
        super(target);
        this.x = x;
        this.y = y;
        this.modifiers = modifiers;
    }

    @Override
    public final float x() {
        return this.x;
    }

    @Override
    public final float y() {
        return this.y;
    }

    @Override
    public final Modifiers modifiers() {
        return this.modifiers;
    }
}
