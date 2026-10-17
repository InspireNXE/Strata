package org.inspirenxe.strata.core.event;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.event.ModifiersEvent;
import org.inspirenxe.strata.api.input.Modifiers;

import java.util.Objects;

/**
 * The core {@link ModifiersEvent}. The host fires one at the screen's content and at each open overlay's content
 * whenever the held modifiers change.
 */
public final class ModifiersEventImpl extends EventImpl implements ModifiersEvent {
    private final Modifiers modifiers;

    /**
     * Creates the event.
     *
     * @param target the component it is fired at
     * @param modifiers the modifiers now held
     */
    public ModifiersEventImpl(final Component<?> target, final Modifiers modifiers) {
        super(target);
        this.modifiers = Objects.requireNonNull(modifiers, "modifiers");
    }

    @Override
    public Modifiers modifiers() {
        return this.modifiers;
    }
}
