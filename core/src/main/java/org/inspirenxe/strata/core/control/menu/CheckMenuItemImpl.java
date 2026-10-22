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

import org.inspirenxe.strata.api.control.menu.CheckMenuItem;
import org.inspirenxe.strata.api.control.menu.MenuEntry;
import org.inspirenxe.strata.api.text.Text;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * The core {@link CheckMenuItem}: enabled and unchecked, with no shortcut and an action that does nothing until set.
 */
public final class CheckMenuItemImpl extends MenuEntryImpl implements CheckMenuItem {
    private Text text;
    private Text shortcut;
    private boolean selected;
    private boolean enabled;
    private Consumer<Boolean> action;

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
    boolean usable() {
        return this.enabled;
    }

    @Override
    boolean checked() {
        return this.selected;
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
        return this.selected;
    }

    @Override
    public CheckMenuItem setSelected(final boolean selected) {
        this.selected = selected;
        return this;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
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
    public String toString() {
        return "CheckMenuItem[" + this.text.plain() + "]";
    }
}
