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

import org.inspirenxe.strata.api.component.Enableable;
import org.inspirenxe.strata.api.control.attribute.text.HasText;

import java.util.Optional;

/**
 * A menu entry that is one choice of several, such as Name, Date or Size under "Sort by". The entries of one menu
 * with the same {@linkplain #setGroup group} form a set of which at most one is selected.
 *
 * <p>Choosing it selects it, clears the other entries of its menu in the same group, runs its action and reports
 * {@link Menu#CHOSEN} with it. It draws the same mark a checked {@link CheckMenuItem} does while selected. Like a
 * {@link MenuItem}, it can show a {@linkplain #setShortcut(org.inspirenxe.strata.api.text.Text) shortcut hint} at
 * its right.</p>
 *
 * <pre>{@code
 * Object sortBy = new Object();
 * menu.addItem(factory.radioMenuItem("Name").setGroup(sortBy).setSelected(true).setAction(() -> sort(NAME)))
 *         .addItem(factory.radioMenuItem("Date").setGroup(sortBy).setAction(() -> sort(DATE)));
 * }</pre>
 *
 * <p>Its caption marks a mnemonic with an underscore, as {@link org.inspirenxe.strata.api.control.button.Button}
 * describes: Alt with the letter chooses the item while its menu is open, as the letter alone does.</p>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#radioMenuItem(String)}. Do not implement this
 * interface.</p>
 */
public non-sealed interface RadioMenuItem extends MenuEntry, HasText<RadioMenuItem>, HasShortcut<RadioMenuItem>,
        Enableable<RadioMenuItem> {

    /**
     * Returns whether the item is the selected one of its group.
     *
     * @return true if selected; false for a new item
     */
    boolean isSelected();

    /**
     * Selects or clears the item without running its action. Selecting it clears the other items of its menu in the
     * same group. An item in no menu keeps its state until a menu holds it; adding a selected item to a menu leaves
     * the others as they are.
     *
     * @param selected true to select it
     * @return this item
     */
    RadioMenuItem setSelected(boolean selected);

    /**
     * Returns the group the item belongs to.
     *
     * @return the group, or empty for the group of a menu's items that have none, as for a new item
     */
    Optional<Object> group();

    /**
     * Puts the item in a group, compared with {@link Object#equals}. The items of a menu that have no group form one
     * group of their own.
     *
     * @param group any object that names the group, or null for none
     * @return this item
     */
    RadioMenuItem setGroup(Object group);

    /**
     * Enables or disables the item. A disabled item draws dimmed and cannot be highlighted or chosen.
     *
     * @param enabled true to enable it
     * @return this item
     */
    @Override
    RadioMenuItem setEnabled(boolean enabled);

    /**
     * Returns the action.
     *
     * @return what runs when the item is chosen; does nothing by default
     */
    Runnable action();

    /**
     * Sets what runs when the item is chosen, after it is selected and the menus close and before
     * {@link Menu#CHOSEN}.
     *
     * @param action the action
     * @return this item
     */
    RadioMenuItem setAction(Runnable action);
}
