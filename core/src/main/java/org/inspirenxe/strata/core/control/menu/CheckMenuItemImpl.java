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
package org.inspirenxe.strata.core.control.menu;

import org.inspirenxe.strata.api.command.Command;
import org.inspirenxe.strata.api.control.menu.CheckMenuItem;
import org.inspirenxe.strata.api.control.menu.MenuEntry;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.core.control.Commands;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * The core {@link CheckMenuItem}: enabled and unchecked, with no shortcut and an action that does nothing until set.
 * With a toggle command its check mark reads and writes the command's state, and it is usable while its own flag is
 * set and the command's enabled source gives true, each read when asked.
 */
public final class CheckMenuItemImpl extends MenuEntryImpl implements CheckMenuItem {
    private Text text;
    private Text shortcut;
    private boolean selected;
    private boolean enabled;
    private Consumer<Boolean> action;
    private Command.Toggle command;

    /**
     * Creates an unchecked item.
     *
     * @param text the item's text
     */
    public CheckMenuItemImpl(final Text text) {
        this.text = Objects.requireNonNull(text, "text");
        this.shortcut = Text.empty();
        this.enabled = true;
        this.action = selected -> { };
    }

    @Override
    MenuEntry entry() {
        return this;
    }

    @Override
    Text label() {
        return this.text;
    }

    @Override
    Text hint() {
        return this.shortcut;
    }

    @Override
    Command shownCommand() {
        return this.command;
    }

    @Override
    boolean usable() {
        return this.enabled && (this.command == null || Commands.isEnabled(this.command));
    }

    @Override
    boolean checked() {
        return this.isSelected();
    }

    @Override
    public Text text() {
        return this.text;
    }

    @Override
    public CheckMenuItem setText(final Text text) {
        this.text = Objects.requireNonNull(text, "text");
        this.changed();
        return this;
    }

    @Override
    public Text shortcut() {
        return this.shortcut;
    }

    @Override
    public CheckMenuItem setShortcut(final Text shortcut) {
        this.shortcut = Objects.requireNonNull(shortcut, "shortcut");
        this.changed();
        return this;
    }

    @Override
    public boolean isSelected() {
        return this.command != null ? Boolean.TRUE.equals(this.command.selected().get()) : this.selected;
    }

    @Override
    public CheckMenuItem setSelected(final boolean selected) {
        if (this.command != null) {
            this.command.selected().set(selected);
        } else {
            this.selected = selected;
        }
        return this;
    }

    @Override
    public boolean isEnabled() {
        return this.usable();
    }

    @Override
    public CheckMenuItem setEnabled(final boolean enabled) {
        this.enabled = enabled;
        this.changed();
        return this;
    }

    @Override
    public Consumer<Boolean> action() {
        return this.action;
    }

    @Override
    public CheckMenuItem setAction(final Consumer<Boolean> action) {
        this.action = Objects.requireNonNull(action, "action");
        return this;
    }

    @Override
    public Optional<Command.Toggle> command() {
        return Optional.ofNullable(this.command);
    }

    /**
     * Shows a toggle and takes its text, or clears it, keeping the text and the check mark the toggle's state gave.
     */
    @Override
    public CheckMenuItem setCommand(final Command.Toggle command) {
        if (command == null && this.command != null) {
            this.selected = this.isSelected();
        }
        this.command = command;
        if (command != null) {
            this.text = command.text();
        }
        this.changed();
        return this;
    }

    @Override
    public String toString() {
        return "CheckMenuItem[" + this.text.plain() + "]";
    }
}
