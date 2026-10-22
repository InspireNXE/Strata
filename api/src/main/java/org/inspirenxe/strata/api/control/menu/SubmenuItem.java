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
 * A menu entry that opens another menu: beside its own in a {@link Menu}, or below its title in a {@link MenuBar}.
 *
 * <p>A menu opens from at most one place. While the item is in a menu or a bar, its menu may not be in another
 * one, and in a menu it may not be that menu or one it opens from.</p>
 *
 * <pre>{@code
 * bar.addMenu(factory.submenuItem("View").setMenu(viewMenu));
 * }</pre>
 *
 * <p>Its caption marks a mnemonic with an underscore, as {@link org.inspirenxe.strata.api.control.button.Button}
 * describes: Alt with the letter opens its menu while the menu holding it is open, as the letter alone does, or opens a
 * menu bar's menu from anywhere in the bar's layer.</p>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#submenuItem(String)} or make one with
 * {@link Menu#addSubmenu} or {@link MenuBar#addMenu(String, Menu)}. Do not implement this interface.</p>
 */
public non-sealed interface SubmenuItem extends MenuEntry, HasText<SubmenuItem>, Enableable<SubmenuItem> {

    /**
     * Returns the menu it opens.
     *
     * @return the menu, or empty until one is set
     */
    Optional<Menu> menu();

    /**
     * Sets the menu it opens, closing the one before if it is open. An item without a menu opens nothing. The menu
     * before is free to open from somewhere else afterwards.
     *
     * @param menu the menu, or {@code null} for none
     * @return this item
     * @throws IllegalArgumentException if {@code menu} was not made by Strata, the item is in a menu or bar and
     *     {@code menu} already opens from somewhere else, or the item is in a menu and {@code menu} is that menu or one
     *     it opens from
     */
    SubmenuItem setMenu(Menu menu);

    /**
     * Returns whether the item can open its menu.
     *
     * @return true by default
     */
    @Override
    boolean isEnabled();

    /**
     * Enables or disables the item. A disabled item draws dimmed and does not open its menu.
     *
     * @param enabled true to enable it
     * @return this item
     */
    @Override
    SubmenuItem setEnabled(boolean enabled);
}
