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
import org.inspirenxe.strata.api.transfer.DropEffect;
import org.inspirenxe.strata.api.transfer.Transfer;

import java.util.Optional;
import java.util.Set;

/**
 * Where an adapter sends the engine's input. {@link ScreenHost} and {@link ScreenStack} extend it, and adapters call
 * it. Do not implement this interface.
 *
 * <p>Each method returns true when Strata consumed the input: a component acted on it, or the pointer is over
 * Strata's UI. Pass input that returns false on to the engine's own UI or controls.</p>
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
 *     <li>Call {@link #pointerLeft()} when the pointer leaves the host's area, so nothing stays hovered.</li>
 *     <li>Use Strata's scroll directions whatever the engine's: vertical is positive when the wheel turns away from
 *     the user, horizontal positive toward the left.</li>
 *     <li>Send a drag from another application with {@link #dragEntered}, {@link #dragMoved} for each move,
 *     and {@link #dropped} or {@link #dragExited} at its end. A drop the engine reports without a drag before it,
 *     such as files dropped on a game window, goes to {@link #dropped} alone.</li>
 *     <li>An engine that tells devices apart, such as touches, pens and controllers, sends input through the forms
 *     that take an {@link InputContext}, the same pointer id from a touch's press to its release. The forms without
 *     one send {@link InputContext#MOUSE} for pointer input and {@link InputContext#KEYBOARD} for keys and typed
 *     characters.</li>
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
     * The window or area the host sits in became active or inactive, as when its window gains or loses the
     * keyboard. Becoming inactive takes focus away from the focused component, which hears
     * {@link org.inspirenxe.strata.api.component.Focusable#FOCUS_LOST} with
     * {@link org.inspirenxe.strata.api.event.input.FocusReason#ACTIVATION}, and remembers it; becoming active gives
     * focus back to it, if it can still take focus, with the same reason and shown as it was. Calls that repeat the
     * current state do nothing. A host is active until told otherwise. The default does nothing; Strata's hosts
     * implement it, and an adapter calls it when the engine reports the change.
     *
     * @param active true when the host became active
     */
    default void activeChanged(final boolean active) {
    }

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
     * A character was typed. An engine that reports a character outside the Basic Multilingual Plane as two UTF-16
     * units must join them and send one code point, and must not send its value for "no character", such as AWT's
     * {@code CHAR_UNDEFINED}. Strata ignores a value that is not a code point, and a lone surrogate.
     *
     * @param codePoint the character as a Unicode code point
     * @param modifiers the held modifiers, or {@link Modifiers#NONE} if the engine does not report them
     * @return true if the focused component consumed it; false if nothing has focus or the value was ignored
     */
    boolean charTyped(int codePoint, Modifiers modifiers);

    /**
     * A pointer moved, from a device the context names. The default calls {@link #pointerMoved(float, float,
     * Modifiers)}, for an input sink that tells no devices apart; core's hosts handle the context.
     *
     * @param x the new x position
     * @param y the new y position
     * @param modifiers the held modifiers, or {@link Modifiers#NONE} if the engine does not report them
     * @param context the device, pointer and pressure
     * @return true if the pointer is over Strata's UI or dragging in it
     */
    default boolean pointerMoved(final float x, final float y, final Modifiers modifiers, final InputContext context) {
        return this.pointerMoved(x, y, modifiers);
    }

    /**
     * A pointer's button went down or came up, from a device the context names. A touch going down is a
     * {@link MouseButton#LEFT} press. The default calls the form without a context.
     *
     * @param x the pointer's x position
     * @param y the pointer's y position
     * @param button the button
     * @param pressed true for down, false for up
     * @param modifiers the held modifiers
     * @param context the device, pointer and pressure
     * @return true if a component received the press, or the release ended a press that started on one
     */
    default boolean pointerButton(final float x, final float y, final MouseButton button, final boolean pressed,
                                  final Modifiers modifiers, final InputContext context) {
        return this.pointerButton(x, y, button, pressed, modifiers);
    }

    /**
     * A device the context names scrolled. The default calls the form without a context.
     *
     * @param x the pointer's x position
     * @param y the pointer's y position
     * @param scrollX horizontal amount in wheel notches; positive toward the left
     * @param scrollY vertical amount in wheel notches; positive when the wheel turns away from the user
     * @param kind the device kind
     * @param modifiers the held modifiers
     * @param context the device, pointer and pressure
     * @return true if a component consumed the scroll
     */
    default boolean scroll(final float x, final float y, final float scrollX, final float scrollY, final ScrollKind kind,
                           final Modifiers modifiers, final InputContext context) {
        return this.scroll(x, y, scrollX, scrollY, kind, modifiers);
    }

    /**
     * A key went down, repeated or came up on a device the context names, such as a controller's confirm button sent
     * as {@link Key#ENTER} with {@code Device.CONTROLLER}. The default calls the form without a context.
     *
     * @param key the key, mapped from the engine's code; {@link Key#UNKNOWN} if unmapped
     * @param action press, repeat or release
     * @param modifiers the held modifiers
     * @param context the device
     * @return true if a component consumed the key, or Tab moved focus
     */
    default boolean key(final Key key, final KeyAction action, final Modifiers modifiers, final InputContext context) {
        return this.key(key, action, modifiers);
    }

    /**
     * A character was typed on a device the context names. The default calls the form without a context.
     *
     * @param codePoint the character as a Unicode code point
     * @param modifiers the held modifiers, or {@link Modifiers#NONE} if the engine does not report them
     * @param context the device
     * @return true if the focused component consumed it; false if nothing has focus or the value was ignored
     */
    default boolean charTyped(final int codePoint, final Modifiers modifiers, final InputContext context) {
        return this.charTyped(codePoint, modifiers);
    }

    /**
     * A drag from another application entered the host's area. Strata fires {@code DRAG_ENTER} and
     * {@code DRAG_OVER} at the components under the pointer. An engine that can read only the formats until the
     * drop passes a transfer whose values are {@linkplain Transfer.Builder#supply supplied} and read as empty until
     * then. The default does nothing, for an input sink that takes no drags.
     *
     * @param x the pointer's x position
     * @param y the pointer's y position
     * @param transfer what the drag carries, in the formats the engine can name
     * @param allowed the effects the other application allows, not empty
     * @param modifiers the held modifiers
     * @return the effect a component would take, which the adapter shows and reports to the other application, or
     *     empty when nothing would take it
     */
    default Optional<DropEffect> dragEntered(final float x, final float y, final Transfer transfer,
                                             final Set<DropEffect> allowed, final Modifiers modifiers) {
        return Optional.empty();
    }

    /**
     * A drag from another application moved over the host's area. The default does nothing.
     *
     * @param x the pointer's x position
     * @param y the pointer's y position
     * @param modifiers the held modifiers
     * @return the effect a component would take, or empty when nothing would
     */
    default Optional<DropEffect> dragMoved(final float x, final float y, final Modifiers modifiers) {
        return Optional.empty();
    }

    /**
     * A drag from another application left the host's area or was given up. The default does nothing.
     */
    default void dragExited() {
    }

    /**
     * Something from another application was dropped on the host, such as files from the desktop, which a game
     * adapter sends from its engine's file drop callback as {@link Transfer#files}. The component that accepts the
     * drop at the point hears {@code DROP}. The default does nothing.
     *
     * @param x the pointer's x position
     * @param y the pointer's y position
     * @param transfer what was dropped, with its values readable
     * @param modifiers the held modifiers, or {@link Modifiers#NONE} if the engine does not report them
     * @return the effect the target took, which the adapter reports to the other application, or empty when nothing
     *     took the drop
     */
    default Optional<DropEffect> dropped(final float x, final float y, final Transfer transfer,
                                         final Modifiers modifiers) {
        return Optional.empty();
    }
}
