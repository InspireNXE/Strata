package org.inspirenxe.strata.api.event;

/**
 * When a handler runs relative to a component's own default behavior.
 */
public enum Phase {
    /**
     * Before the default behavior; can cancel it.
     */
    BEFORE,
    /**
     * After the default behavior.
     */
    AFTER
}
