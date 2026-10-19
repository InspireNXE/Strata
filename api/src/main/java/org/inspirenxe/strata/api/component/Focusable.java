package org.inspirenxe.strata.api.component;

import org.inspirenxe.strata.api.event.EventTarget;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.input.FocusEvent;

/**
 * A component that can take keyboard focus and be reached with Tab, such as a button, a text box or a list.
 *
 * <p>Keyboard input goes to the focused component and bubbles up from it, so containers hear
 * {@link org.inspirenxe.strata.api.event.input.InputEvents#KEY} without being focusable themselves. Components that
 * do not implement this interface never take focus.</p>
 *
 * <p>A component that implements it starts out focusable and tabbable, with tab index 0. To make a custom component
 * focusable, extend {@link org.inspirenxe.strata.api.extension.CustomComponent} and declare
 * {@code implements Focusable<Self>}: the class already has every method below. Do not implement this interface any
 * other way.</p>
 *
 * @param <S> the component's own type, which the setters return
 */
public interface Focusable<S extends Focusable<S>> extends EventTarget<S> {
    /**
     * Told after the component gained keyboard focus. Direct. Default behavior: a text box moves its caret to the
     * end.
     */
    EventType<FocusEvent> FOCUS_GAINED = EventType.direct("focus_gained", FocusEvent.class);
    /**
     * Told after the component lost keyboard focus. Direct.
     */
    EventType<FocusEvent> FOCUS_LOST = EventType.direct("focus_lost", FocusEvent.class);

    /**
     * Returns whether the component can take keyboard focus now.
     *
     * @return true unless turned off with {@code setFocusable(false)}
     */
    boolean isFocusable();

    /**
     * Sets whether the component can take keyboard focus, by a click or by {@link #requestFocus()}. Turning it off
     * suits parts of a control that should leave focus where it is, such as the list in a combo box's popup. It
     * does not take focus away from a component that already has it.
     *
     * @param focusable false to refuse focus
     * @return this component
     */
    S setFocusable(boolean focusable);

    /**
     * Returns whether Tab and Shift+Tab stop at this component.
     *
     * @return true unless turned off with {@code setTabbable(false)}
     */
    boolean isTabbable();

    /**
     * Sets whether Tab and Shift+Tab stop at this component. Only matters while it is focusable.
     *
     * @param tabbable false to leave the component out of tab order
     * @return this component
     */
    S setTabbable(boolean tabbable);

    /**
     * Returns the tab index.
     *
     * @return the tab index; 0 by default
     */
    int tabIndex();

    /**
     * Sets the tab index. Tab visits components in ascending tab index, and components with equal indexes in tree
     * order (depth first, children in the order they were added). Negative values are allowed.
     *
     * @param tabIndex the tab index
     * @return this component
     */
    S setTabIndex(int tabIndex);

    /**
     * Returns whether this component has keyboard focus. At most one component per host has focus.
     *
     * @return true while focused
     */
    boolean isFocused();

    /**
     * Asks the host to give this component keyboard focus. Fails, changing nothing, unless the component is
     * attached, focusable, visible and enabled, with every ancestor visible and enabled.
     *
     * @return true if the component now has focus
     */
    boolean requestFocus();
}
