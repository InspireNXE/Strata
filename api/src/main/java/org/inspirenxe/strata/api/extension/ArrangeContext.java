package org.inspirenxe.strata.api.extension;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.text.TextMetrics;

/**
 * What a container may use while it places its children. Valid only during the arrange call.
 *
 * <p>Strata provides this object. Do not implement this interface.</p>
 */
public interface ArrangeContext {

    /**
     * Returns the engine's text measurement.
     *
     * @return the text metrics
     */
    TextMetrics text();

    /**
     * Returns the space a child asked for in the last measure: its desired size plus margin.
     *
     * @param child a child of the container being arranged
     * @return the child's outer size, or zero if it is hidden
     */
    Size outerSize(Component<?> child);

    /**
     * Measures a child, for one added during arrange, such as a row that scrolled into view. Measure such a child
     * before giving it a slot.
     *
     * @param child a child of the component being arranged
     * @param availableWidth the width offered to the child, margin included; may be infinite
     * @param availableHeight the height offered to the child, margin included; may be infinite
     * @return the space the child needs: its desired size plus its margin, or zero if it is hidden
     * @throws IllegalArgumentException if {@code child} was made by another factory or has no Strata implementation
     */
    Size measure(Component<?> child, float availableWidth, float availableHeight);

    /**
     * Gives a child its slot. The child shrinks the slot by its margin and places itself in what remains by its
     * alignment and size limits, which sets its bounds. A slot smaller than the child's desired size is allowed;
     * the child then overflows it unless it stretches.
     *
     * @param child a child of the container being arranged
     * @param slot the area the child may use, margin included, in the host's coordinates
     */
    void arrange(Component<?> child, Rect slot);
}
