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
 * A character the user typed, after the keyboard layout and any input method have done their work. Used by
 * {@link InputEvents#CHAR}. Goes only to the focused component, and bubbles.
 *
 * <p>Characters are Unicode code points, so characters outside the Basic Multilingual Plane, such as emoji, arrive
 * as one event. Some engines, SDL-based ones among them, deliver characters only while a component that accepts
 * text input has focus.</p>
 *
 * <p>Only Strata creates events. Do not implement this interface.</p>
 */
public interface CharEvent extends SnapshotEvent {

    /**
     * Returns the typed character.
     *
     * @return a Unicode code point
     */
    int codePoint();

    /**
     * Returns the typed character as a string of one or two chars.
     *
     * @return the character
     */
    default String text() {
        return Character.toString(this.codePoint());
    }

    /**
     * Returns the modifiers held when the character was typed. Engines that do not report them give
     * {@link Modifiers#NONE}.
     *
     * @return the held modifiers
     */
    Modifiers modifiers();
}
