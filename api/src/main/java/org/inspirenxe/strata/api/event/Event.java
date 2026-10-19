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
 * Base of every UI event. See the {@linkplain org.inspirenxe.strata.api.event package description} for the order
 * in which handlers and default behavior run.
 *
 * <p>An event object is valid only while it is being delivered. Do not keep it and read it later: Strata may reuse
 * objects or make {@link #current()} change. To keep the state a component had when the event fired, take its
 * {@linkplain #snapshotOf(Component) snapshot}, which stays valid.</p>
 *
 * <p>Only Strata creates events. Do not implement this interface.</p>
 */
public interface Event {

    /**
     * Returns the component the event was fired at.
     *
     * @return the target; never null
     */
    Component<?> target();

    /**
     * Returns the component whose handlers or default behavior are running now. Equal to {@link #target()} at
     * first; while a bubbling event travels up, it is each ancestor in turn.
     *
     * @return the current component; never null
     */
    Component<?> current();

    /**
     * Returns the snapshot taken of a component when the event fired, before any handler or default behavior ran.
     *
     * <p>Snapshots are lazy: Strata takes them only when a handler for the event's type is on its path. The path
     * is the target and, for a bubbling type, every ancestor the target had when the event fired. Strata then
     * snapshots the target and each component on the path that has a handler for the type. Nothing else is
     * captured. Only cancelable events take snapshots: a control's report fires after its change, so it carries the
     * value from before the change instead, and paint and measure events take none.</p>
     *
     * <p>Each snapshot covers only its own component, not the component's children. A component that joins the
     * path while the event is being delivered, for example because a handler moved the target to another
     * container, has no snapshot.</p>
     *
     * @param component the component to look up
     * @return the component's snapshot, or empty if none was taken of it for this event
     */
    Optional<ComponentSnapshot> snapshotOf(Component<?> component);

    /**
     * Returns the snapshot of the {@linkplain #target() target}. Present for every cancelable event that reaches a
     * handler, so such a handler can always call this.
     *
     * @return the target's snapshot; never null
     * @throws IllegalStateException if no snapshot was taken for this event; see {@link #snapshotOf(Component)}
     */
    default ComponentSnapshot snapshot() {
        return this.snapshotOf(this.target()).orElseThrow(() -> new IllegalStateException(
                "No snapshot was taken for this event; only cancelable events take snapshots"));
    }

    /**
     * Returns the snapshot of the {@linkplain #current() current} component. Present for a cancelable event whose
     * handler is running on the current component.
     *
     * @return the current component's snapshot; never null
     * @throws IllegalStateException if no snapshot was taken of the current component; see
     *                               {@link #snapshotOf(Component)}
     */
    default ComponentSnapshot currentSnapshot() {
        return this.snapshotOf(this.current()).orElseThrow(() -> new IllegalStateException(
                "No snapshot was taken of " + this.current() + " for this event"));
    }

    /**
     * Keeps the event from reaching any further ancestor. The current component's remaining handlers and default
     * behavior still run. Works on every event, including those that cannot be canceled.
     */
    void stopPropagation();

    /**
     * Returns whether propagation was stopped.
     *
     * @return true after {@link #stopPropagation()} or {@link Cancelable#consume()}
     */
    boolean isPropagationStopped();
}
