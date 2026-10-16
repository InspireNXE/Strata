package org.inspirenxe.strata.api.extension;

import org.inspirenxe.strata.api.geom.Size;

/**
 * Runs the measurement a {@link ComponentBehavior#measure} hook wraps. Valid only during that hook call; call it at
 * most once.
 */
@FunctionalInterface
public interface MeasureNext {

    /**
     * Runs the component's own measurement with the same arguments the hook received.
     *
     * @return the size the component measured
     */
    Size proceed();
}
