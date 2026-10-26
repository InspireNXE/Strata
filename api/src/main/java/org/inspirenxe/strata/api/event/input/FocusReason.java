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

/**
 * Why keyboard focus moved, as {@link FocusEvent#reason()} reports it. Strata works it out from the input being
 * handled when focus moves, so focus that a key handler moves with
 * {@link org.inspirenxe.strata.api.component.Focusable#requestFocus()}, as arrow keys in a group do, counts as
 * {@link #KEYBOARD}, and focus a click handler moves counts as {@link #POINTER}.
 */
public enum FocusReason {
    /** A pointer press: a mouse button, a touch or a pen, or code run while one is handled. */
    POINTER,
    /**
     * Navigation without a pointer: Tab, arrow keys, a mnemonic or a controller's buttons sent as keys, or code run
     * while one of them is handled.
     */
    KEYBOARD,
    /** Code, with no input being handled, such as a screen giving its first field focus when it opens. */
    CODE,
    /**
     * The host became active or inactive, as when its window gains or loses the keyboard: focus leaves when the host
     * becomes inactive and comes back to the same component when it is active again.
     */
    ACTIVATION
}
