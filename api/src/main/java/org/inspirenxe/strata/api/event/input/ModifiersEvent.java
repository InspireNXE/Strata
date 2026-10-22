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
package org.inspirenxe.strata.api.event.input;

import org.inspirenxe.strata.api.event.SnapshotEvent;
import org.inspirenxe.strata.api.input.Modifiers;

/**
 * The set of held modifier keys changed. Used by {@link InputEvents#MODIFIERS_CHANGED}, so a screen can relabel a
 * button while Control is held.
 *
 * <p>Only Strata creates events. Do not implement this interface.</p>
 */
public interface ModifiersEvent extends SnapshotEvent {

    /**
     * Returns the modifiers now held.
     *
     * @return the modifiers after the change
     */
    Modifiers modifiers();
}
