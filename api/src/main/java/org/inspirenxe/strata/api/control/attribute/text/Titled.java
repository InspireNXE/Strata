package org.inspirenxe.strata.api.control.attribute.text;

/**
 * A control or entry with a title over its content, such as a window, an expander or a table column.
 *
 * <p>Only Strata implements this interface.</p>
 *
 * @param <S> the control's own type, which the setter returns
 */
public interface Titled<S extends Titled<S>> {

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
