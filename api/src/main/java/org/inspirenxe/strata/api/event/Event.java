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
import org.inspirenxe.strata.api.event.cause.Cause;
import org.inspirenxe.strata.api.event.cause.Origin;

/**
 * Base of every UI event. See the {@linkplain org.inspirenxe.strata.api.event package description} for the order
 * in which handlers and default behavior run.
 *
 * <p>An event object is valid only while it is being delivered. Do not keep it and read it later: Strata may reuse
 * objects or make {@link #current()} change. To keep the state a component had when the event fired, take its
 * snapshot from a {@link SnapshotEvent}, which stays valid.</p>
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
     * Returns what led to the event: the objects the change passed through and the context, such as who caused it
     * and with which device. Strata makes the cause when this is called, from the frames open at that moment. During
     * delivery those are the frames that were open when the event fired, so call it before opening a frame of your
     * own.
     *
     * @return the cause; never null
     */
    Cause cause();

    /**
     * Returns who caused the event, without making the whole {@linkplain #cause() cause}.
     *
     * @return the origin the innermost open frame put, or {@link Origin#CODE} when none did
     */
    Origin origin();

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
