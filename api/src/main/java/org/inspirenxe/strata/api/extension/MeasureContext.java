package org.inspirenxe.strata.api.extension;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.text.TextMetrics;

/**
 * What a component may use while it measures. Valid only during the measure call.
 *
 * <p>Strata provides this object. Do not implement this interface.</p>
 */
public interface MeasureContext {

    /**
     * Returns the engine's text measurement.
     *
     * @return the text metrics
     */
    TextMetrics text();

    /**
     * Measures a child within the given space. Containers must call this for every child during their own
     * measure; the result is also available later from {@link ArrangeContext#outerSize}.
     *
     * @param child a child of the container being measured
     * @param availableWidth the width offered to the child, margin included; may be infinite
     * @param availableHeight the height offered to the child, margin included; may be infinite
     * @return the space the child needs: its desired size plus its margin, or zero if it is hidden
     * @throws IllegalArgumentException if {@code child} was made by another factory or has no Strata implementation
     */
    Size measure(Component<?> child, float availableWidth, float availableHeight);
}
