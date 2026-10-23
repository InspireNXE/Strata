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

import org.inspirenxe.strata.api.command.Command;
import org.inspirenxe.strata.api.component.Enableable;
import org.inspirenxe.strata.api.control.attribute.command.HasCommand;
import org.inspirenxe.strata.api.control.attribute.text.HasText;

/**
 * A menu entry that runs a command when chosen, with an optional shortcut hint drawn at its right.
 *
 * <pre>{@code
 * MenuItem copy = factory.menuItem("Copy")
 *         .setShortcut("Ctrl+C")
 *         .setAction(this::copy);
 * menu.addItem(copy);
 * copy.setEnabled(selection.isPresent());
 * }</pre>
 *
 * <p>An item can show a {@linkplain #setCommand command} instead: it takes the command's text, is enabled while
 * the command is and its own flag is set, shows the command's stroke as its hint while no hint text is set, and
 * runs the command after {@link Menu#CHOSEN} when chosen. A menu bar runs the commands of its menus from their
 * shortcuts.</p>
 *
 * <p>Its caption marks a mnemonic with an underscore, as {@link org.inspirenxe.strata.api.control.button.Button}
 * describes: Alt with the letter chooses the item while its menu is open, as the letter alone does.</p>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#menuItem(String)} or make one with
 * {@link Menu#addItem(String, Runnable)}. Do not implement this interface.</p>
 */
public non-sealed interface MenuItem extends MenuEntry, HasText<MenuItem>, HasShortcut<MenuItem>,
        Enableable<MenuItem>, HasCommand<Command.Action, MenuItem> {

    /**
     * Returns whether the item can be chosen: it was not disabled with {@link #setEnabled}, and its command, if it has
     * one, is enabled now.
     *
     * @return true by default
     */
    @Override
    boolean isEnabled();

    /**
     * Enables or disables the item. A disabled item draws dimmed and cannot be highlighted or chosen.
     *
     * @param enabled true to enable it
     * @return this item
     */
    @Override
    MenuItem setEnabled(boolean enabled);

    /**
     * Returns the action.
     *
     * @return what runs when the item is chosen; does nothing by default
     */
    Runnable action();

    /**
     * Sets what runs when the item is chosen, after the menus close and before {@link Menu#CHOSEN}.
     *
     * @param action the action
     * @return this item
     */
    MenuItem setAction(Runnable action);
}
