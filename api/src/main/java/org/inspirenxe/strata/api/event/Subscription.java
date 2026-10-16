package org.inspirenxe.strata.api.event;

/**
 * A registered event handler. Removing the component from its tree does not remove its handlers; close the
 * subscription, or use an owner token with
 * {@link org.inspirenxe.strata.api.component.Component#removeHandlers(Object)}.
 */
@FunctionalInterface
public interface Subscription extends AutoCloseable {

    /**
     * Removes the handler. Calling it again does nothing.
     */
    void unsubscribe();

    /**
     * Same as {@link #unsubscribe()}, so a subscription works in try-with-resources.
     */
    @Override
    default void close() {
        this.unsubscribe();
    }
}
