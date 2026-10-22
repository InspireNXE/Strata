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
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.core.control.RectCache;
import org.inspirenxe.strata.core.control.Rings;
import org.inspirenxe.strata.core.text.TextLine;

/**
 * The shared state of the core menu entries: the menu or bar that holds the entry, the text spans its menu caches
 * during measure, and the rectangles its menu draws for it, kept between frames. A change to an entry tells its
 * holder, which lays out again.
 */
public abstract class MenuEntryImpl {
    private Object owner;
    TextLine span;
    TextLine disabledSpan;
    TextLine shortcutSpan;
    float shortcutWidth;
    final RectCache rowRect = new RectCache();
    final RectCache markRect = new RectCache();
    final Rings.Arrow arrow = new Rings.Arrow();

    /**
     * Creates an entry that no menu holds yet.
     */
    MenuEntryImpl() {
        this.span = TextLine.EMPTY;
        this.disabledSpan = this.span;
        this.shortcutSpan = this.span;
    }

    /**
     * Returns this entry as its API type.
     */
    abstract MenuEntry entry();

    /**
     * Returns the text drawn for the entry.
     */
    Text label() {
        return Text.empty();
    }

    /**
     * Returns the shortcut hint drawn at the entry's right.
     */
    Text hint() {
        return Text.empty();
    }

    /**
     * Returns whether the entry can be highlighted and chosen.
     */
    boolean usable() {
        return false;
    }

    /**
     * Returns whether a check mark shows.
     */
    boolean checked() {
        return false;
    }

    /**
     * Returns whether the entry is a separator line.
     */
    boolean separator() {
        return false;
    }

    /**
     * Returns the menu the entry opens, or null.
     */
    MenuImpl submenu() {
        return null;
    }

    /**
     * Returns the menu or bar that holds the entry, or null.
     */
    Object owner() {
        return this.owner;
    }

    /**
     * Gives the entry to a holder, or takes it back with null.
     *
     * @throws IllegalArgumentException if the entry is given while a holder already has it
     */
    void setOwner(final Object owner) {
        if (owner != null && this.owner != null) {
            throw new IllegalArgumentException(this.entry() + " is already in a menu or a menu bar");
        }
        this.owner = owner;
    }

    /**
     * Tells the holder that the entry changed.
     */
    void changed() {
        if (this.owner instanceof MenuImpl menu) {
            menu.entryChanged(this);
        } else if (this.owner instanceof MenuBarImpl bar) {
            bar.entryChanged(this);
        }
    }

    /**
     * Casts an API entry to its core class.
     *
     * @throws IllegalArgumentException if the entry was not made by Strata
     */
    static MenuEntryImpl of(final MenuEntry entry) {
        if (!(entry instanceof MenuEntryImpl impl)) {
            throw new IllegalArgumentException(entry + " is not a Strata menu entry");
        }
        return impl;
    }
}
