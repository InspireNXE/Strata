package org.inspirenxe.strata.api.extension;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.geom.Size;
import org.inspirenxe.strata.api.text.TextMetrics;

/**
 * What a container may use while placing its children.
 */
public interface ArrangeContext {

    TextMetrics text();

    /**
     * The space the child asked for in the last measure, margin included.
     */
    Size outerSize(Component<?> child);

    /**
     * Gives a child its slot. The child places itself inside it by its margin, alignment and size limits.
     */
    void arrange(Component<?> child, Rect slot);
}
