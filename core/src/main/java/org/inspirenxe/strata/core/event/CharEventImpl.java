package org.inspirenxe.strata.core.event;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.event.CharEvent;
import org.inspirenxe.strata.api.input.Modifiers;

public final class CharEventImpl extends EventImpl implements CharEvent {
    private final int codePoint;
    private final Modifiers modifiers;

    public CharEventImpl(final Component<?> target, final int codePoint, final Modifiers modifiers) {
        super(target);
        this.codePoint = codePoint;
        this.modifiers = modifiers;
    }

    @Override
    public int codePoint() {
        return this.codePoint;
    }

    @Override
    public Modifiers modifiers() {
        return this.modifiers;
    }
}
