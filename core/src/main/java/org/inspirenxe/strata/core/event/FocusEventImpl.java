package org.inspirenxe.strata.core.event;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.event.input.FocusEvent;

import java.util.Optional;

/**
 * The core {@link FocusEvent}. When focus moves, the host sends the component losing it an event first, then the
 * component gaining it.
 */
public final class FocusEventImpl extends EventImpl implements FocusEvent {
    private final Component<?> other;

    /**
     * Creates the event.
     *
     * @param target the component gaining or losing focus
     * @param other the component on the other side of the move, or {@code null} when focus comes from nothing or
     *     goes to nothing
     */
    public FocusEventImpl(final Component<?> target, final Component<?> other) {
        super(target);
        this.other = other;
    }

    @Override
    public Optional<Component<?>> other() {
        return Optional.ofNullable(this.other);
    }
}
