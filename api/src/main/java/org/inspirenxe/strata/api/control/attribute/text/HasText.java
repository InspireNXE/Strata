package org.inspirenxe.strata.api.control.attribute.text;

import org.inspirenxe.strata.api.event.EventTarget;

/**
 * A control that shows one string of text as its caption, such as a button, a check box or a label.
 *
 * <p>Only Strata implements this interface.</p>
 *
 * @param <S> the control's own type, which the setter returns
 */
public interface HasText<S extends HasText<S>> extends EventTarget<S> {

    /**
     * Returns the text.
     *
     * @return the text, never null; empty when there is none
     */
    String text();

    /**
     * Replaces the text. The control lays out again if its size depends on it.
     *
     * @param text the text, or an empty string for none
     * @return this control
     */
    S setText(String text);
}
