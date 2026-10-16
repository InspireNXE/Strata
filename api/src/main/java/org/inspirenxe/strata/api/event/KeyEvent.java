package org.inspirenxe.strata.api.event;

import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.ModifierPolicy;
import org.inspirenxe.strata.api.input.Modifiers;

public interface KeyEvent extends Event {

    Key key();

    KeyAction action();

    Modifiers modifiers();

    /**
     * True when this is a press or repeat of {@code key} with modifiers that satisfy {@code policy}.
     */
    default boolean is(final Key key, final Modifiers required, final ModifierPolicy policy) {
        return this.action().isDown() && this.key() == key && policy.matches(required, this.modifiers());
    }
}
