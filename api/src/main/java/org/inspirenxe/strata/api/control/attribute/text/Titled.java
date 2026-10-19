package org.inspirenxe.strata.api.control.attribute.text;

import org.inspirenxe.strata.api.event.EventTarget;

/**
 * A control with a title over its content, such as a window or an expander.
 *
 * <p>Only Strata implements this interface.</p>
 *
 * @param <S> the control's own type, which the setter returns
 */
public interface Titled<S extends Titled<S>> extends EventTarget<S> {

    /**
     * Returns the title.
     *
     * @return the title, never null; empty when there is none
     */
    String title();

    /**
     * Replaces the title.
     *
     * @param title the title, or an empty string for none
     * @return this control
     */
    S setTitle(String title);
}
