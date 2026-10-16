package org.inspirenxe.strata.api.extension;

/**
 * Runs the behavior a {@link ComponentBehavior} hook wraps, much like calling {@code super}. Valid only during the
 * hook call that received it; call it at most once.
 */
@FunctionalInterface
public interface Next {

    /**
     * Runs the wrapped behavior.
     */
    void proceed();
}
