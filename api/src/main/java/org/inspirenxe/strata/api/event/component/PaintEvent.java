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
package org.inspirenxe.strata.api.event.component;

import org.inspirenxe.strata.api.event.Cancelable;
import org.inspirenxe.strata.api.event.Event;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Rect;

/**
 * Drawing a component: {@link org.inspirenxe.strata.api.component.Component#PAINTED} after the component and its
 * children have drawn, so handlers draw on top, and {@link org.inspirenxe.strata.api.component.Component#PAINT}
 * before, as a {@link Pre} event that can skip the component's own drawing.
 *
 * <p>Paint events reach only their component and never take snapshots. A component fires them only when it has a
 * handler for them, and reuses one event object for every frame, so keep handlers free of allocation where you can
 * and never keep the event.</p>
 *
 * <p>Only Strata creates events. Do not implement this interface.</p>
 */
public interface PaintEvent extends Event {

    /**
     * Returns what to draw with, valid only during this frame.
     *
     * @return the painter
     */
    Painter painter();

    /**
     * Returns the component's bounds, in the host's coordinates.
     *
     * @return the bounds
     */
    Rect bounds();

    /**
     * The paint event fired before a component draws its background. Canceling it skips the component's own
     * background, border and content; its children still draw.
     *
     * <p>Only Strata creates events. Do not implement this interface.</p>
     */
    interface Pre extends PaintEvent, Cancelable {
    }
}
