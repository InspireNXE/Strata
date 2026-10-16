package org.inspirenxe.strata.core.event;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.event.KeyEvent;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;

public final class KeyEventImpl extends EventImpl implements KeyEvent {
    private final Key key;
    private final KeyAction action;
    private final Modifiers modifiers;

    public KeyEventImpl(final Component<?> target, final Key key, final KeyAction action, final Modifiers modifiers) {
        super(target);
        this.key = key;
        this.action = action;
        this.modifiers = modifiers;
    }

    @Override
    public Key key() {
        return this.key;
    }

    @Override
    public KeyAction action() {
        return this.action;
    }

    @Override
    public Modifiers modifiers() {
        return this.modifiers;
    }
}
