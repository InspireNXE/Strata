package org.inspirenxe.strata.api.event;

/**
 * A handler registered with {@link EventTarget#subscribe}, which can be removed before its target is discarded.
 * Removing a component from its tree does not remove its handlers; close the subscription.
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
