package org.inspirenxe.strata.api.event.component;

import org.inspirenxe.strata.api.event.Event;
import org.inspirenxe.strata.api.extension.MeasureContext;

/**
 * Fired as {@link org.inspirenxe.strata.api.component.Component#MEASURED} after a component measured its content,
 * so a handler can change the size it asks for. A fixed width or height set on the component, and its minimum and
 * maximum sizes, still apply to the result.
 *
 * <p>Measure events reach only their component and never take snapshots. A component fires them only when it has
 * a handler for them and reuses one event object, so never keep the event. Do not change layout from a handler.</p>
 *
 * <p>Only Strata creates events. Do not implement this interface.</p>
 */
public interface MeasureEvent extends Event {

    /**
     * Returns text measurement and child measuring for the pass in progress.
     *
     * @return the measure context
     */
    MeasureContext context();

    /**
     * Returns the width offered for the content, after the margin and any fixed width. May be infinite.
     *
     * @return the available width
     */
    float availableWidth();

    /**
     * Returns the height offered for the content, after the margin and any fixed height. May be infinite.
     *
     * @return the available height
     */
    float availableHeight();

    /**
     * Returns the content width the component asked for, or the last width a handler set.
     *
     * @return the width
     */
    float width();

    /**
     * Returns the content height the component asked for, or the last height a handler set.
     *
     * @return the height
     */
    float height();

    /**
     * Replaces the content width.
     *
     * @param width the width; negative values count as 0
     */
    void setWidth(float width);

    /**
     * Replaces the content height.
     *
     * @param height the height; negative values count as 0
     */
    void setHeight(float height);
}
