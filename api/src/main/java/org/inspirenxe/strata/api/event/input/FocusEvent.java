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

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.event.SnapshotEvent;

import java.util.Optional;

/**
 * Keyboard focus moving. Used by {@link org.inspirenxe.strata.api.component.Focusable#FOCUS_GAINED} and
 * {@link org.inspirenxe.strata.api.component.Focusable#FOCUS_LOST}. When focus moves from one component to another,
 * the old one gets {@code FOCUS_LOST} first, then the new one gets {@code FOCUS_GAINED}; both run after the focus has
 * already changed. Canceling the event cannot undo the move; it skips the component's default behavior, such as a
 * text box moving its caret.
 *
 * <p>Only Strata creates events. Do not implement this interface.</p>
 */
public interface FocusEvent extends SnapshotEvent {

    /**
     * Returns the other side of the move.
     *
     * @return for a gain, the component that lost focus; for a loss, the component that gained it; empty when focus
     *         came from or went to nothing
     */
    Optional<Component<?>> other();

    /**
     * Returns why focus moved. A loss and the gain that follows it report the same reason.
     *
     * @return the reason
     */
    FocusReason reason();
}
