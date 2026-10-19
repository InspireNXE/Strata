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

import org.inspirenxe.strata.api.input.MouseButton;

/**
 * A mouse button press, release or click. Used by {@link InputEvents#POINTER_PRESS},
 * {@link InputEvents#POINTER_RELEASE} and {@link InputEvents#CLICK}.
 *
 * <p>Only Strata creates events. Do not implement this interface.</p>
 */
public interface PointerButtonEvent extends PointerEvent {

    /**
     * Returns the button involved.
     *
     * @return the button
     */
    MouseButton button();

    /**
     * Returns the position of this press in a run of quick presses. A press counts as the next in a run when it
     * hits the same component with the same button within 500 ms and 4 units of the previous press; the count goes
     * 1, 2, 3 and then starts again at 1. A release and its click carry the count of their press.
     *
     * <p>Strata delivers every press at once with its count, never holding a single click back to wait for a
     * double. React to the count you care about: a text box selects a word on 2.</p>
     *
     * @return 1 for a single click, 2 for a double click, 3 for a triple click
     */
    int clicks();
}
