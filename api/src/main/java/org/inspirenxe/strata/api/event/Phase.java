package org.inspirenxe.strata.api.event;

/**
 * When a handler runs, relative to the component's own default behavior for the event.
 */
public enum Phase {
    /**
     * Before the default behavior. The place to veto: a handler that calls {@link Event#cancel()} here skips the
     * default behavior.
     */
    BEFORE,
    /**
     * After the default behavior, when it has already taken effect (for example, after a text box inserted a
     * character).
     */
    AFTER
}
