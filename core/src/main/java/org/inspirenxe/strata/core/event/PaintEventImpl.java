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
import org.inspirenxe.strata.api.event.component.PaintEvent;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Rect;

/**
 * The paint event of one component, serving as both {@link Component#PAINT} and {@link Component#PAINTED}. The
 * component keeps one and resets it before each firing, so painting allocates nothing for it.
 */
public final class PaintEventImpl extends EventImpl implements PaintEvent.Pre {
    private Painter painter;
    private Rect bounds;

    /**
     * Creates the paint event of a component.
     *
     * @param target the component
     */
    public PaintEventImpl(final Component<?> target) {
        super(target);
    }

    /**
     * Readies the event for one firing: clears the canceled and stopped flags and sets what to draw with.
     *
     * @param painter the painter for this frame
     * @param bounds the component's bounds
     * @return this event
     */
    public PaintEventImpl begin(final Painter painter, final Rect bounds) {
        this.reset();
        this.painter = painter;
        this.bounds = bounds;
        return this;
    }

    @Override
    public Painter painter() {
        return this.painter;
    }

    @Override
    public Rect bounds() {
        return this.bounds;
    }
}
