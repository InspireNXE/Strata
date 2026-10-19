package org.inspirenxe.strata.api.event;

/**
 * A control's Pre event that carries the value a change is about to use: the text an edit would produce, the
 * slider value a drag reached, the row a press would select. Cancel it to refuse the change, or set another value
 * to rewrite it. The control checks the final value as it checks one set from code, so a rewritten value may still
 * be clamped, limited or ignored; each event type says how.
 *
 * <p>Only Strata creates events. Do not implement this interface.</p>
 *
 * @param <C> the control type
 * @param <V> the value type
 */
public interface ValueRequestEvent<C, V> extends RequestEvent<C> {

    /**
     * Returns the value the change will use: the one the user's action produced, or the last one a handler set.
     *
     * @return the value
     */
    V value();

    /**
     * Replaces the value the change will use. Handlers after this one see the new value.
     *
     * @param value the value to use instead; each event type says whether null is allowed
     */
    void setValue(V value);
}
