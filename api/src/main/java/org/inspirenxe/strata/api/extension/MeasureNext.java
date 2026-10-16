package org.inspirenxe.strata.api.extension;

import org.inspirenxe.strata.api.geom.Size;

/**
 * Runs the measurement a {@link ComponentBehavior#measure} hook wraps and returns its result.
 */
@FunctionalInterface
public interface MeasureNext {

    Size proceed();
}
