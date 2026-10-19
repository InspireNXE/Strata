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
import org.inspirenxe.strata.api.event.input.PointerButtonEvent;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;

/**
 * The core {@link PointerButtonEvent}, used for presses, releases and clicks.
 */
public final class PointerButtonEventImpl extends PointerEventImpl implements PointerButtonEvent {
    private final MouseButton button;
    private final int clicks;

    /**
     * Creates the event.
     *
     * @param target the component under the pointer for a press, or the pressed component for a release or click
     * @param x the pointer's x, in the host's coordinates
     * @param y the pointer's y, in the host's coordinates
     * @param modifiers the modifiers held
     * @param button the button
     * @param clicks the press's place in a run of quick presses (1, 2 or 3); a release and its click carry the
     *     count of their press
     */
    public PointerButtonEventImpl(final Component<?> target, final float x, final float y, final Modifiers modifiers,
                                  final MouseButton button, final int clicks) {
        super(target, x, y, modifiers);
        this.button = button;
        this.clicks = clicks;
    }

    @Override
    public MouseButton button() {
        return this.button;
    }

    @Override
    public int clicks() {
        return this.clicks;
    }
}
