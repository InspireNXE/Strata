package org.inspirenxe.strata.core.control.menu;

import org.inspirenxe.strata.api.control.menu.CheckMenuItem;
import org.inspirenxe.strata.api.control.menu.MenuEntry;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * The core {@link CheckMenuItem}: enabled and unchecked, with an action that does nothing until set.
 */
public final class CheckMenuItemImpl extends MenuEntryImpl implements CheckMenuItem {
    private String text;
    private boolean selected;
    private boolean enabled;
    private Consumer<Boolean> action;

    /**
     * Creates an unchecked item.
     *
     * @param text the item's text
     */
    public CheckMenuItemImpl(final String text) {
        this.text = Objects.requireNonNull(text, "text");
        this.enabled = true;
        this.action = selected -> { };
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
    boolean usable() {
        return this.enabled;
    }

    @Override
    boolean checked() {
        return this.selected;
    }

    @Override
    public String text() {
        return this.text;
    }

    @Override
    public CheckMenuItem setText(final String text) {
        this.text = Objects.requireNonNull(text, "text");
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
        return "CheckMenuItem[" + this.text + "]";
    }
}
