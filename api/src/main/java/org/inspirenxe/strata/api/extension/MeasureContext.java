package org.inspirenxe.strata.api.extension;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.geom.Size;
import org.inspirenxe.strata.api.text.TextMetrics;

/**
 * What a component may consult while measuring.
 */
public interface MeasureContext {

    TextMetrics text();

    /**
     * Measures a child within the given space and returns the space it needs, margin included. A hidden child needs
     * none. Containers must measure every child here before they are arranged.
     */
    Size measure(Component<?> child, float availableWidth, float availableHeight);
}
