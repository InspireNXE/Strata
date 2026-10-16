package org.inspirenxe.strata.api.extension;

/**
 * Runs the behavior a {@link ComponentBehavior} hook wraps, much like calling {@code super}.
 */
@FunctionalInterface
public interface Next {

    void proceed();
}
