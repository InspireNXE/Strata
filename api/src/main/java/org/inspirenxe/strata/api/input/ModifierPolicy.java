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
 * How a keyboard shortcut treats the modifiers held when its key is pressed. See
 * {@link org.inspirenxe.strata.api.event.input.KeyEvent#is}. {@link #matches} works on the modifiers of any event, so
 * pointer input can use it too, such as a click that does something else with Control held.
 */
public enum ModifierPolicy {
    /**
     * Matches whatever modifiers are held.
     */
    IGNORE,
    /**
     * Matches only when exactly the required modifiers are held: Ctrl+S does not fire for Ctrl+Shift+S.
     */
    EXACT,
    /**
     * Matches when the required modifiers are held, whatever else is held too: Ctrl+S also fires for Ctrl+Shift+S.
     */
    AT_LEAST,
    /**
     * Matches only when no modifier is held. The required modifiers are ignored.
     */
    FORBID;

    /**
     * Returns whether the held modifiers satisfy this policy.
     *
     * @param required the modifiers the shortcut asks for
     * @param held the modifiers held when the key was pressed
     * @return true if the shortcut should fire
     */
    public boolean matches(final Modifiers required, final Modifiers held) {
        return switch (this) {
            case IGNORE -> true;
            case EXACT -> held.equals(required);
            case AT_LEAST -> held.containsAll(required);
            case FORBID -> held.isEmpty();
        };
    }
}
