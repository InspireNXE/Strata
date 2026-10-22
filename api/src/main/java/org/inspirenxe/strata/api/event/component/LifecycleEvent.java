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

import org.inspirenxe.strata.api.event.Event;

/**
 * Fired as {@link org.inspirenxe.strata.api.component.Component#ATTACHED} when a component joins a tree a host is
 * showing, and as {@link org.inspirenxe.strata.api.component.Component#DETACHED} when it leaves it. The
 * {@linkplain #target() target} is the component that joined or left; every component of a subtree that joins or
 * leaves hears its own.
 *
 * <p>Lifecycle events reach only their component and never take snapshots. A component fires them only when it has
 * a handler for them.</p>
 *
 * <p>Only Strata creates events. Do not implement this interface.</p>
 */
public interface LifecycleEvent extends Event {
}
