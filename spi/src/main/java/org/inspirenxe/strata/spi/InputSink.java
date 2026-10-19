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
package org.inspirenxe.strata.spi;

import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.input.ScrollKind;

/**
 * Where an adapter sends the engine's input. Implemented by Strata's hosts and called by adapters.
 *
 * <p>Each method returns true when Strata consumed the input: a component acted on it, or the pointer is over
 * Strata's UI. Pass input that returns false on to the engine's own UI or game controls.</p>
 *
 * <h2>Requirements for callers (adapters)</h2>
 * <ul>
 *     <li>Positions are in the host's UI units, with the origin at the top left of its area.</li>
 *     <li>Send {@link #pointerMoved} for every pointer movement, during drags too: drags are recognized from
 *     moves.</li>
 *     <li>Send each button's press and release; a release with no matching press is ignored.</li>
 *     <li>Send key presses, repeats and releases with the right {@link KeyAction}. If the engine reports repeats as
 *     more presses, track held keys and turn the second and later presses into {@code REPEAT}.</li>
 *     <li>Send typed characters separately through {@link #charTyped}, as code points, after the keyboard layout and
 *     input method. Do not send control characters (below U+0020, or U+007F to U+009F).</li>
 *     <li>Call on the UI thread only.</li>
 * </ul>
 */
public interface InputSink {

    /**
     * The pointer moved.
     *
     * @param x the new x position
     * @param y the new y position
     * @param modifiers the held modifiers, or {@link Modifiers#NONE} if the engine does not report them
     * @return true if the pointer is over Strata's UI or dragging in it
     */
    boolean pointerMoved(float x, float y, Modifiers modifiers);

    /**
     * A mouse button went down or came up.
     *
     * @param x the pointer's x position
     * @param y the pointer's y position
     * @param button the button
     * @param pressed true for down, false for up
     * @param modifiers the held modifiers
     * @return true if a component received the press, or the release ended a press that started on one
     */
    boolean pointerButton(float x, float y, MouseButton button, boolean pressed, Modifiers modifiers);

    /**
     * A wheel or trackpad scrolled.
     *
     * @param x the pointer's x position
     * @param y the pointer's y position
     * @param scrollX horizontal amount in wheel notches; positive toward the left
     * @param scrollY vertical amount in wheel notches; positive when the wheel turns away from the user
     * @param kind the device kind
     * @param modifiers the held modifiers
     * @return true if a component consumed the scroll
     */
    boolean scroll(float x, float y, float scrollX, float scrollY, ScrollKind kind, Modifiers modifiers);

    /**
     * The pointer left the host's area. Clears hover.
     */
    void pointerLeft();

    /**
     * A key went down, repeated or came up.
     *
     * @param key the key, mapped from the engine's code; {@link Key#UNKNOWN} if unmapped
     * @param action press, repeat or release
     * @param modifiers the held modifiers
     * @return true if a component consumed the key, or Tab moved focus
     */
    boolean key(Key key, KeyAction action, Modifiers modifiers);

    /**
     * A character was typed.
     *
     * @param codePoint the character as a Unicode code point
     * @param modifiers the held modifiers, or {@link Modifiers#NONE} if the engine does not report them
     * @return true if the focused component consumed it
     */
    boolean charTyped(int codePoint, Modifiers modifiers);
}
