package org.inspirenxe.strata.api.control.attribute.value;

import org.inspirenxe.strata.api.event.EventTarget;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.control.ValueEvent;
import org.inspirenxe.strata.api.event.control.ValueRequestEvent;

/**
 * A control that is on or off, such as a check box, a switch or a radio button.
 *
 * <p>Handle {@link #TOGGLE} to refuse or change what the user does, and {@link #TOGGLED} to react to every change.
 * One handler on a container hears every toggle inside it:</p>
 *
 * <pre>{@code
 * settings.onEvent(Toggle.TOGGLED, event -> save());
 * }</pre>
 *
 * <p>Only Strata implements this interface.</p>
 *
 * @param <S> the control's own type, which the setters return
 */
public interface Toggle<S extends Toggle<S>> extends EventTarget<S> {
    /**
     * Asked before the user turns the control on or off. The value is the state the control is about to have. A
     * handler may rewrite it; the control then takes that state, and does nothing if it is the current one. Fires
     * only for the user's actions, and only when the state would change.
     */
    EventType<ValueRequestEvent<Toggle<?>, Boolean>> TOGGLE = EventType.valueRequest("toggle.toggle");
    /**
     * Told after the control was turned on or off. The value is the new state and the previous value the old one.
     * Fires for changes made from code too.
     */
    EventType<ValueEvent<Toggle<?>, Boolean>> TOGGLED = EventType.valueReport("toggle.toggled");

    /**
     * Returns whether the control is on.
     *
     * @return true when on; false for a new control
     */
    boolean isSelected();

    /**
     * Turns the control on or off. Fires {@link #TOGGLED} if the state changed, and never {@link #TOGGLE}. Works
     * while disabled too, so code can keep a disabled control in step with its setting.
     *
     * @param selected true for on
     * @return this control
     */
    S setSelected(boolean selected);

    /**
     * Flips the state, firing {@link #TOGGLED} but not {@link #TOGGLE}. Does nothing while the control is disabled.
     *
     * @return this control
     */
    S toggle();
}
