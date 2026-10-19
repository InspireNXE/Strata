package org.inspirenxe.strata.core.control.menu;

import org.inspirenxe.strata.api.control.menu.MenuEntry;
import org.inspirenxe.strata.api.text.TextSpan;

/**
 * The shared state of the core menu entries: the menu or bar that holds the entry, and the text spans its menu
 * caches during measure. A change to an entry tells its holder, which lays out again.
 */
public abstract class MenuEntryImpl {
    private Object owner;
    TextSpan span;
    TextSpan disabledSpan;
    TextSpan shortcutSpan;
    float shortcutWidth;

    /**
     * Creates an entry that no menu holds yet.
     */
    MenuEntryImpl() {
        this.span = TextSpan.of("");
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
    String label() {
        return "";
    }

    /**
     * Returns the shortcut hint drawn at the entry's right.
     */
    String hint() {
        return "";
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
