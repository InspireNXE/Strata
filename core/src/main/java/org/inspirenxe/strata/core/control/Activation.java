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
package org.inspirenxe.strata.core.control;

import org.inspirenxe.strata.api.event.input.KeyEvent;
import org.inspirenxe.strata.api.event.input.PointerButtonEvent;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.MouseButton;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * When a button, checkbox, radio button, toggle switch, cycle button or tab acts: on the pointer press where the
 * platform's {@code Conventions.activateOnPress} says so, and on the click otherwise, and on a press of Enter, keypad
 * Enter or Space. Each control keeps one and calls {@link #press} and {@link #click} from its pointer hooks.
 */
public final class Activation {
    private final BooleanSupplier onPress;
    private final Consumer<PointerButtonEvent> pointer;

    /**
     * Creates an activation that hands the acting pointer event to the control, which checks the button and
     * consumes the event itself.
     *
     * @param onPress whether the control acts on press, asked at each event
     * @param pointer what to do with the press or click the control acts on
     */
    public Activation(final BooleanSupplier onPress, final Consumer<PointerButtonEvent> pointer) {
        this.onPress = Objects.requireNonNull(onPress, "onPress");
        this.pointer = Objects.requireNonNull(pointer, "pointer");
    }

    /**
     * Creates an activation that runs an action for the left button and consumes the event.
     *
     * @param onPress whether the control acts on press, asked at each event
     * @param action what the control does
     * @return the activation
     */
    public static Activation leftButton(final BooleanSupplier onPress, final Runnable action) {
        Objects.requireNonNull(action, "action");
        return new Activation(onPress, event -> {
            if (event.button() == MouseButton.LEFT) {
                action.run();
                event.consume();
            }
        });
    }

    /**
     * Acts on a pointer press while the control acts on press.
     *
     * @param event the press
     */
    public void press(final PointerButtonEvent event) {
        if (this.onPress.getAsBoolean()) {
            this.pointer.accept(event);
        }
    }

    /**
     * Acts on a click while the control acts on click.
     *
     * @param event the click
     */
    public void click(final PointerButtonEvent event) {
        if (!this.onPress.getAsBoolean()) {
            this.pointer.accept(event);
        }
    }

    /**
     * Returns whether a key event activates a control: a press, not a repeat, of Enter, keypad Enter or Space.
     *
     * @param event the key event
     * @return true if it activates
     */
    public static boolean isActivationKey(final KeyEvent event) {
        return event.action() == KeyAction.PRESS && (event.key().isEnter() || event.key() == Key.SPACE);
    }

    /**
     * Runs an action and consumes the event if the event activates a control, as {@link #isActivationKey} says.
     *
     * @param event the key event
     * @param action what the control does
     * @return true if the event activated
     */
    public static boolean key(final KeyEvent event, final Runnable action) {
        if (!Activation.isActivationKey(event)) {
            return false;
        }
        action.run();
        event.consume();
        return true;
    }
}
