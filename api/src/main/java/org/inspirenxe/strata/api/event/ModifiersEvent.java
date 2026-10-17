package org.inspirenxe.strata.api.event;

import org.inspirenxe.strata.api.input.Modifiers;

/**
 * The set of held modifier keys changed. Used by {@link EventTypes#MODIFIERS_CHANGED}, so a screen can relabel a
 * button while Control is held.
 *
 * <p>Only Strata creates events. Do not implement this interface.</p>
 */
public interface ModifiersEvent extends Event {

    /**
     * Returns the modifiers now held.
     *
     * @return the modifiers after the change
     */
    Modifiers modifiers();
}
