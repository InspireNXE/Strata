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

import org.inspirenxe.strata.api.control.menu.MenuEntry;
import org.inspirenxe.strata.api.control.menu.MenuItem;

import java.util.Objects;

/**
 * The core {@link MenuItem}: enabled, with no shortcut and an action that does nothing until set.
 */
public final class MenuItemImpl extends MenuEntryImpl implements MenuItem {
    private String text;
    private String shortcut;
    private boolean enabled;
    private Runnable action;

    /**
     * Creates an item.
     *
     * @param text the item's text
     */
    public MenuItemImpl(final String text) {
        this.text = Objects.requireNonNull(text, "text");
        this.shortcut = "";
        this.enabled = true;
        this.action = () -> { };
    }

    @Override
    MenuEntry entry() {
        return this;
    }

    @Override
    String label() {
        return this.text;
    }

    @Override
    String hint() {
        return this.shortcut;
    }

    @Override
    boolean usable() {
        return this.enabled;
    }

    @Override
    public String text() {
        return this.text;
    }

    @Override
    public MenuItem setText(final String text) {
        this.text = Objects.requireNonNull(text, "text");
        this.changed();
        return this;
    }

    @Override
    public String shortcut() {
        return this.shortcut;
    }

    @Override
    public MenuItem setShortcut(final String shortcut) {
        this.shortcut = Objects.requireNonNull(shortcut, "shortcut");
        this.changed();
        return this;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    @Override
    public MenuItem setEnabled(final boolean enabled) {
        this.enabled = enabled;
        this.changed();
        return this;
    }

    @Override
    public Runnable action() {
        return this.action;
    }

    @Override
    public MenuItem setAction(final Runnable action) {
        this.action = Objects.requireNonNull(action, "action");
        return this;
    }

    @Override
    public String toString() {
        return "MenuItem[" + this.text + "]";
    }
}
