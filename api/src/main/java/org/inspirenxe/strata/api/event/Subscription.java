package org.inspirenxe.strata.api.event;

/**
 * A registered handler. Closing it removes the handler; closing twice does nothing.
 */
@FunctionalInterface
public interface Subscription extends AutoCloseable {

    void unsubscribe();

    @Override
    default void close() {
        this.unsubscribe();
    }
}
