package org.inspirenxe.strata.core.control.menu;

import org.inspirenxe.strata.api.control.menu.MenuEntry;
import org.inspirenxe.strata.api.control.menu.MenuSeparator;

/**
 * The core {@link MenuSeparator}.
 */
public final class MenuSeparatorImpl extends MenuEntryImpl implements MenuSeparator {

    /**
     * Creates a separator.
     */
    public MenuSeparatorImpl() {
        super();
    }

    @Override
    MenuEntry entry() {
        return this;
    }

    @Override
    boolean separator() {
        return true;
    }

    @Override
    public String toString() {
        return "MenuSeparator";
    }
}
