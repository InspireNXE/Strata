package org.inspirenxe.strata.api.event;

import org.inspirenxe.strata.api.component.Component;

import java.util.Optional;

/**
 * Keyboard focus moving. Used by {@link EventTypes#FOCUS_GAINED} and {@link EventTypes#FOCUS_LOST}. When focus
 * moves from one component to another, the old one gets {@code FOCUS_LOST} first, then the new one gets
 * {@code FOCUS_GAINED}; both run after the focus has already changed.
 *
 * <p>Only Strata creates events. Do not implement this interface.</p>
 */
public interface FocusEvent extends Event {

    /**
     * Returns the other side of the move.
     *
     * @return for a gain, the component that lost focus; for a loss, the component that gained it; empty when focus
     *         came from or went to nothing
     */
    Optional<Component<?>> other();
}
