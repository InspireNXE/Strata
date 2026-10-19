package org.inspirenxe.strata.api.event.input;

import org.inspirenxe.strata.api.event.Cancelable;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.ModifierPolicy;
import org.inspirenxe.strata.api.input.Modifiers;

/**
 * A physical key going down, repeating or coming up. Used by {@link InputEvents#KEY}.
 *
 * <p>Key events say which key, not which character: for typed text use {@link CharEvent}. A key event goes to the
 * focused component, or to the screen's root when nothing has focus, and bubbles.</p>
 *
 * <p>Only Strata creates events. Do not implement this interface.</p>
 */
public interface KeyEvent extends Cancelable {

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
     * Returns the modifiers held at the time of the event.
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
}
