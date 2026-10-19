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
import org.inspirenxe.strata.api.event.input.ScrollEvent;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.ScrollKind;

/**
 * The core {@link ScrollEvent}, sent to the component under the pointer.
 */
public final class ScrollEventImpl extends PointerEventImpl implements ScrollEvent {
    private final float scrollX;
    private final float scrollY;
    private final ScrollKind kind;

    /**
     * Creates the event.
     *
     * @param target the component under the pointer
     * @param x the pointer's x, in the host's coordinates
     * @param y the pointer's y, in the host's coordinates
     * @param modifiers the modifiers held, or {@link Modifiers#NONE} when the engine does not report them
     * @param scrollX the horizontal amount in notches, positive toward the start of a row (the left)
     * @param scrollY the vertical amount in notches, positive when the wheel turns away from the user
     * @param kind the kind of device the scroll came from
     */
    public ScrollEventImpl(final Component<?> target, final float x, final float y, final Modifiers modifiers,
                           final float scrollX, final float scrollY, final ScrollKind kind) {
        super(target, x, y, modifiers);
        this.scrollX = scrollX;
        this.scrollY = scrollY;
        this.kind = kind;
    }

    @Override
    public float scrollX() {
        return this.scrollX;
    }

    @Override
    public float scrollY() {
        return this.scrollY;
    }

    @Override
    public ScrollKind kind() {
        return this.kind;
    }
}
