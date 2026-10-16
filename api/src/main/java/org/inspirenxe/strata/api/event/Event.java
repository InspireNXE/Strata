package org.inspirenxe.strata.api.event;

import org.inspirenxe.strata.api.component.Component;

/**
 * Base of every UI event. Strata creates events; developers only receive them.
 *
 * <p>An event starts at its {@link #target()} and, if its type bubbles, travels up through the ancestors. At each
 * component, {@link Phase#BEFORE} handlers run, then the component's own default behavior unless the event was
 * {@linkplain #cancel() canceled}, then {@link Phase#AFTER} handlers. {@link #stopPropagation()} stops the event
 * from reaching the next ancestor.</p>
 */
public interface Event {

    /**
     * The component the event was aimed at.
     */
    Component<?> target();

    /**
     * The component whose handlers are running now. Differs from {@link #target()} while the event bubbles.
     */
    Component<?> current();

    /**
     * Skips the default behavior of every component the event still reaches.
     */
    void cancel();

    boolean isCancelled();

    /**
     * Stops the event after the current component.
     */
    void stopPropagation();

    boolean isPropagationStopped();

    /**
     * Cancels the event and stops it, meaning "handled here".
     */
    default void consume() {
        this.cancel();
        this.stopPropagation();
    }

    /**
     * True if something along the way canceled or stopped the event, which the host reports back to the engine as
     * "consumed".
     */
    default boolean isHandled() {
        return this.isCancelled() || this.isPropagationStopped();
    }
}
