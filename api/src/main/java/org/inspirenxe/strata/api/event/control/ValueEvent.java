package org.inspirenxe.strata.api.event.control;

/**
 * A control's Post event that carries a value: the new text after an edit, the new value of a slider, the row now
 * selected. It fires after the change, whether the user or code made it, and carries the value from before the
 * change too.
 *
 * <p>Only Strata creates events. Do not implement this interface.</p>
 *
 * @param <C> the control type
 * @param <V> the value type
 */
public interface ValueEvent<C, V> extends ControlEvent<C> {

    /**
     * Returns the value after the change.
     *
     * @return the new value; each event type says whether it can be null
     */
    V value();

    /**
     * Returns the value before the change. For an event that reports an action rather than a change of state, such
     * as a submit, it equals {@link #value()}.
     *
     * @return the previous value; each event type says whether it can be null
     */
    V previous();
}
