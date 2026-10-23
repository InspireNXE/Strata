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
package org.inspirenxe.strata.api.event;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.ComponentSnapshot;

import java.util.Optional;

/**
 * A Pre event that records snapshots of the components on its path: every input event and every control's request.
 * A handler can compare the state a component had when the event fired with the live component.
 *
 * <p>Snapshots are lazy: Strata takes them only when a handler for the event's type is on its path. The path is the
 * target and, for a bubbling type, every ancestor the target had when the event fired, as far as the event goes.
 * Strata then snapshots the target and each component on the path that has a handler for the type. Nothing else is
 * captured. So a handler of a snapshot event always finds its target's snapshot and its own component's.</p>
 *
 * <p>Each snapshot covers only its own component, not the component's children. A component that joins the path
 * while the event is being delivered, for example because a handler moved the target to another container, has no
 * snapshot. A snapshot stays valid after delivery, unlike the event.</p>
 *
 * <p>Other events take none. A control's report fires after its change, so it carries the value from before the
 * change as {@code previous()} instead, and paint and measure events, {@link
 * org.inspirenxe.strata.api.event.component.PaintEvent.Pre} included, run every frame.</p>
 *
 * <p>Only Strata creates events. Do not implement this interface.</p>
 */
public interface SnapshotEvent extends Cancelable {

    /**
     * Returns the snapshot taken of a component when the event fired, before any handler or default behavior ran.
     *
     * @param component the component to look up
     * @return the component's snapshot, or empty if none was taken of it for this event
     */
    Optional<ComponentSnapshot> snapshotOf(Component<?> component);

    /**
     * Returns the snapshot of the {@linkplain #target() target}, present whenever a handler runs.
     *
     * @return the target's snapshot
     * @throws IllegalStateException if no snapshot was taken of the target, which happens only when the event is
     *     read outside a handler
     */
    default ComponentSnapshot snapshot() {
        return this.snapshotOf(this.target()).orElseThrow(() -> new IllegalStateException(
                "No snapshot was taken of " + this.target() + " for this event"));
    }

    /**
     * Returns the snapshot of the {@linkplain #current() current} component, present while one of its own handlers
     * runs.
     *
     * @return the current component's snapshot
     * @throws IllegalStateException if no snapshot was taken of the current component, as when it has no handler
     *     for the type and its default behavior is running, or it joined the path during delivery
     */
    default ComponentSnapshot currentSnapshot() {
        return this.snapshotOf(this.current()).orElseThrow(() -> new IllegalStateException(
                "No snapshot was taken of " + this.current() + " for this event"));
    }
}
