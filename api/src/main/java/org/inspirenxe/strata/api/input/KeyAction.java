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
package org.inspirenxe.strata.api.input;

/**
 * What happened to a key.
 *
 * <p>A held key produces one {@link #PRESS}, then {@link #REPEAT}s for keys that repeat, then one {@link #RELEASE}.
 * An engine that sends repeats sends them at its own rate for the keys it chooses. When an engine does not, Strata
 * makes them for the arrow keys, Home, End, Page Up, Page Down, Backspace and Delete only, the first 400 ms after
 * the press and then one every 33 ms, for the most recently pressed of those keys.</p>
 */
public enum KeyAction {
    /** The key went down. */
    PRESS,
    /** The key is still held and repeating. */
    REPEAT,
    /** The key came up. */
    RELEASE;

    /**
     * Returns whether this is a press or a repeat: the moments when a held key should act, such as moving a caret
     * again.
     *
     * @return true for {@link #PRESS} and {@link #REPEAT}
     */
    public boolean isDown() {
        return this != RELEASE;
    }
}
