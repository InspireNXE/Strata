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
import org.inspirenxe.strata.api.control.menu.RadioMenuItem;
import org.inspirenxe.strata.api.text.Text;

import java.util.Objects;
import java.util.Optional;

/**
 * The core {@link RadioMenuItem}: enabled and not selected, in no group, with no shortcut and an action that does
 * nothing until set. Selecting it asks the menu that holds it to clear the others of its group.
 */
public final class RadioMenuItemImpl extends MenuEntryImpl implements RadioMenuItem {
    private Text text;
    private Text shortcut;
    private boolean selected;
    private boolean enabled;
    private Object group;
    private Runnable action;

    /**
     * Creates an item that is not selected.
     *
     * @param text the item's text
     */
    public RadioMenuItemImpl(final Text text) {
        this.text = Objects.requireNonNull(text, "text");
        this.shortcut = Text.empty();
        this.enabled = true;
        this.action = () -> { };
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
    public RadioMenuItem setText(final Text text) {
        this.text = Objects.requireNonNull(text, "text");
        this.changed();
        return this;
    }

    @Override
    public Text shortcut() {
        return this.shortcut;
    }

    @Override
    public RadioMenuItem setShortcut(final Text shortcut) {
        this.shortcut = Objects.requireNonNull(shortcut, "shortcut");
        this.changed();
        return this;
    }

    @Override
    public boolean isSelected() {
        return this.selected;
    }

    @Override
    public RadioMenuItem setSelected(final boolean selected) {
        this.selected = selected;
        if (selected && this.owner() instanceof final MenuImpl menu) {
            menu.clearGroupBut(this);
        }
        this.changed();
        return this;
    }

    /**
     * Clears the item for its menu, when another of its group was selected.
     */
    void clear() {
        if (this.selected) {
            this.selected = false;
            this.changed();
        }
    }

    @Override
    public Optional<Object> group() {
        return Optional.ofNullable(this.group);
    }

    @Override
    public RadioMenuItem setGroup(final Object group) {
        this.group = group;
        return this;
    }

    /**
     * Returns whether another item is in the same group.
     *
     * @param other the other item
     * @return true when their groups are equal, or both have none
     */
    boolean sameGroup(final RadioMenuItemImpl other) {
        return Objects.equals(this.group, other.group);
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    @Override
    public RadioMenuItem setEnabled(final boolean enabled) {
        this.enabled = enabled;
        this.changed();
        return this;
    }

    @Override
    public Runnable action() {
        return this.action;
    }

    @Override
    public RadioMenuItem setAction(final Runnable action) {
        this.action = Objects.requireNonNull(action, "action");
        return this;
    }

    @Override
    public String toString() {
        return "RadioMenuItem[" + this.text.plain() + "]";
    }
}
