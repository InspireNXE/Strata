/*
 * Copyright (c) InspireNXE <https://github.com/InspireNXE>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
     * suits parts of a control that should leave focus where it is, such as the list in a combo box's popup.
     * Turning it off on the focused component clears focus at once, and the component hears
     * {@link #FOCUS_LOST}.
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
     * Returns whether this component has keyboard focus that should be shown, as a focus ring or a lit face. Focus
     * is shown when it came from the keyboard, or from code while the last press was a key's rather than a pointer's,
     * and also once a key is pressed while it has focus; a pointer press hides it again. So a button clicked with the
     * mouse keeps focus without looking focused, and Tab shows where focus is. When the host becomes active again,
     * focus comes back shown as it was. Strata's controls draw their focus rings, lit faces and keyboard cursors only
     * while this is true, and text boxes show their focused look whenever they have focus. Themes can style it
     * through the {@code focus_visible} style state.
     *
     * @return true while focused and focus is shown
     */
    boolean isFocusVisible();

    /**
     * Asks the host to give this component keyboard focus. Fails, changing nothing, unless the component is
     * attached, focusable, visible and enabled, with every ancestor visible and enabled. While a dialog is open, it
     * also fails for a component below the top dialog, so only the dialog's content and overlays opened above it can
     * take focus.
     *
     * @return true if the component now has focus
     */
    boolean requestFocus();
}
