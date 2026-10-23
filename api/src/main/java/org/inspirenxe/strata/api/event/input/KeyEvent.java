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
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.KeyStroke;
import org.inspirenxe.strata.api.input.Modifier;
import org.inspirenxe.strata.api.input.ModifierPolicy;
import org.inspirenxe.strata.api.input.Modifiers;

/**
 * A physical key going down, repeating or coming up. Used by {@link InputEvents#KEY}.
 *
 * <p>Key events say which key, not which character: for typed text use {@link CharEvent}. A key event goes to the
 * focused component or, when nothing has focus, to the content of the top dialog, or else to the screen's content,
 * and bubbles. See {@link InputEvents#KEY} for what the host does with keys nothing consumed.</p>
 *
 * <p>Only Strata creates events. Do not implement this interface.</p>
 */
public interface KeyEvent extends SnapshotEvent {

    /**
     * Returns the key.
     *
     * @return the key; {@link Key#UNKNOWN} if the engine's key could not be mapped
     */
    Key key();

    /**
     * Returns what happened to the key.
     *
     * @return press, repeat or release
     */
    KeyAction action();

    /**
     * Returns the modifiers held at the time of the event. For a modifier key's own event they are the modifiers as
     * the key leaves them, on every engine: a press or repeat of Shift includes {@code SHIFT}, and its release does
     * not.
     *
     * @return the held modifiers
     */
    Modifiers modifiers();

    /**
     * Returns whether this event is a press or repeat of a shortcut.
     *
     * <pre>{@code
     * if (event.is(Key.S, Modifiers.of(Modifier.CONTROL), ModifierPolicy.EXACT)) {
     *     save();
     *     event.consume();
     * }
     * }</pre>
     *
     * @param key the shortcut's key
     * @param required the modifiers the shortcut needs
     * @param policy how other held modifiers are treated
     * @return true for a press or repeat of {@code key} with modifiers that satisfy {@code policy}
     */
    default boolean is(final Key key, final Modifiers required, final ModifierPolicy policy) {
        return this.action().isDown() && this.key() == key && policy.matches(required, this.modifiers());
    }

    /**
     * Returns whether this event is a press or repeat of exactly a key stroke on this host's platform, with a
     * {@linkplain KeyStroke#shortcut shortcut} stroke holding the platform's shortcut modifier.
     *
     * <pre>{@code
     * if (event.is(KeyStroke.shortcut(Key.S))) {
     *     save();
     *     event.consume();
     * }
     * }</pre>
     *
     * @param stroke the stroke
     * @return true for a press or repeat of the stroke's key with exactly its modifiers held
     */
    boolean is(KeyStroke stroke);

    /**
     * Returns the platform's shortcut modifier, which {@link KeyStroke#shortcut} strokes hold.
     *
     * @return {@link Modifier#CONTROL} on most platforms, {@link Modifier#SUPER} where the platform's conventions
     *     name Command
     */
    Modifier shortcutModifier();
}
