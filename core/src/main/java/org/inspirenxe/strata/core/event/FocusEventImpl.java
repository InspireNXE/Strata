package org.inspirenxe.strata.core.event;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.event.FocusEvent;

import java.util.Optional;

public final class FocusEventImpl extends EventImpl implements FocusEvent {
    private final Component<?> other;

    public FocusEventImpl(final Component<?> target, final Component<?> other) {
        super(target);
        this.other = other;
    }

    @Override
    public Optional<Component<?>> other() {
        return Optional.ofNullable(this.other);
    }
}
