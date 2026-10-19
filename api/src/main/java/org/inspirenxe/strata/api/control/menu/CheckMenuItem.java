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
package org.inspirenxe.strata.api.control.menu;

import org.inspirenxe.strata.api.control.attribute.text.HasText;

import java.util.function.Consumer;

/**
 * A menu entry with a check mark that flips each time it is chosen, for a setting that is on or off.
 *
 * <p>It names its state as {@link org.inspirenxe.strata.api.control.attribute.value.Toggle} does, but is not one:
 * an entry has no events of its own. Choosing it flips the state and then runs its action with the new state.</p>
 *
 * <pre>{@code
 * menu.addItem(factory.checkMenuItem("Show prices")
 *         .setSelected(settings.prices())
 *         .setAction(settings::setPrices));
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#checkMenuItem(String)} or make one with
 * {@link Menu#addCheckItem}. Do not implement this interface.</p>
 */
public non-sealed interface CheckMenuItem extends MenuEntry, HasText<CheckMenuItem> {

    /**
     * Returns whether the item is checked.
     *
     * @return true if checked; false by default
     */
    boolean isSelected();

    /**
     * Checks or unchecks the item without running its action.
     *
     * @param selected true to check it
     * @return this item
     */
    CheckMenuItem setSelected(boolean selected);

    /**
     * Returns whether the item can be chosen.
     *
     * @return true by default
     */
    boolean isEnabled();

    /**
     * Enables or disables the item. A disabled item draws dimmed and cannot be highlighted or chosen.
     *
     * @param enabled true to enable it
     * @return this item
     */
    CheckMenuItem setEnabled(boolean enabled);

    /**
     * Returns the action.
     *
     * @return what runs with the new state when the item is chosen; does nothing by default
     */
    Consumer<Boolean> action();

    /**
     * Sets what runs with the new state when the item is chosen, after the menus close and before
     * {@link Menu#CHOSEN}.
     *
     * @param action the action
     * @return this item
     */
    CheckMenuItem setAction(Consumer<Boolean> action);
}
