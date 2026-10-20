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

import org.inspirenxe.strata.api.control.menu.Menu;
import org.inspirenxe.strata.api.control.menu.MenuEntry;
import org.inspirenxe.strata.api.control.menu.SubmenuItem;
import org.inspirenxe.strata.api.text.Text;

import java.util.Objects;
import java.util.Optional;

/**
 * The core {@link SubmenuItem}. While a menu or bar holds it, its menu is linked to that holder, so the menu knows
 * where it opens from; setting another menu unlinks the old one, closing it first.
 */
public final class SubmenuItemImpl extends MenuEntryImpl implements SubmenuItem {
    private Text text;
    private boolean enabled;
    private MenuImpl menu;

    /**
     * Creates an item with no menu.
     *
     * @param text the item's text
     */
    public SubmenuItemImpl(final Text text) {
        this.text = Objects.requireNonNull(text, "text");
        this.enabled = true;
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
    boolean usable() {
        return this.enabled;
    }

    @Override
    MenuImpl submenu() {
        return this.menu;
    }

    @Override
    public Text text() {
        return this.text;
    }

    @Override
    public SubmenuItem setText(final Text text) {
        this.text = Objects.requireNonNull(text, "text");
        this.changed();
        return this;
    }

    @Override
    public Optional<Menu> menu() {
        return Optional.ofNullable(this.menu);
    }

    @Override
    public SubmenuItem setMenu(final Menu menu) {
        final MenuImpl next = MenuImpl.of(menu);
        if (next == this.menu) {
            return this;
        }
        this.check(next, this.owner());
        this.unlink();
        this.menu = next;
        this.link(this.owner());
        this.changed();
        return this;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    @Override
    public SubmenuItem setEnabled(final boolean enabled) {
        this.enabled = enabled;
        if (!enabled && this.menu != null) {
            this.menu.close();
        }
        this.changed();
        return this;
    }

    /**
     * Checks that a menu may open from a holder.
     *
     * @throws IllegalArgumentException if it opens from somewhere already, or would open from itself
     */
    void check(final MenuImpl next, final Object holder) {
        if (next == null || holder == null) {
            return;
        }
        if (!next.isFree()) {
            throw new IllegalArgumentException(next + " is already a submenu or in a menu bar");
        }
        if (holder instanceof MenuImpl parent) {
            parent.checkNotAbove(next);
        }
    }

    /**
     * Links the menu to the holder it now opens from.
     */
    void link(final Object holder) {
        if (this.menu == null) {
            return;
        }
        if (holder instanceof MenuImpl parent) {
            this.menu.setParent(parent);
        } else if (holder instanceof MenuBarImpl bar) {
            this.menu.setBar(bar);
        }
    }

    /**
     * Closes the menu and unlinks it from its holder.
     */
    void unlink() {
        if (this.menu == null) {
            return;
        }
        this.menu.close();
        this.menu.setParent(null);
        this.menu.setBar(null);
    }

    @Override
    public String toString() {
        return "SubmenuItem[" + this.text.plain() + "]";
    }
}
