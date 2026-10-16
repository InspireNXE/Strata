package org.inspirenxe.strata.api.event;

/**
 * Code that reacts to an event. Runs on the UI thread. An exception thrown by a handler propagates out of the
 * input call that caused the event, so handle your own errors.
 *
 * @param <E> the event interface
 */
@FunctionalInterface
public interface EventHandler<E extends Event> {

    /**
     * Reacts to an event.
     *
     * @param event the event; valid only during this call
     */
    void handle(E event);
}
