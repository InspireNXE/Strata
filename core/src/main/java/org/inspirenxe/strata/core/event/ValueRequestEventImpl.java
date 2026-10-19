package org.inspirenxe.strata.core.event;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.event.control.ValueRequestEvent;

/**
 * A control's request that carries a value handlers may replace.
 *
 * @param <C> the control type
 * @param <V> the value type
 */
public final class ValueRequestEventImpl<C, V> extends RequestEventImpl<C> implements ValueRequestEvent<C, V> {
    private V value;

    /**
     * Creates a request fired by a control.
     *
     * @param target the control
     * @param value the value the user's action produced
     */
    public ValueRequestEventImpl(final Component<?> target, final V value) {
        super(target);
        this.value = value;
    }

    @Override
    public V value() {
        return this.value;
    }

    @Override
    public void setValue(final V value) {
        this.value = value;
    }
}
