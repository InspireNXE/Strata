package org.inspirenxe.strata.api.control.menu;

import org.inspirenxe.strata.api.control.attribute.text.HasText;

import java.util.Optional;

/**
 * A menu entry that opens another menu: beside its own in a {@link Menu}, or below its title in a {@link MenuBar}.
 *
 * <p>A menu opens from at most one place. While the item is in a menu or a bar, its menu may not be in another
 * one, and in a menu it may not be that menu or one it opens from.</p>
 *
 * <pre>{@code
 * bar.addMenu(factory.submenuItem("View").setMenu(viewMenu));
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#submenuItem(String)} or make one with
 * {@link Menu#addSubmenu} or {@link MenuBar#addMenu(String, Menu)}. Do not implement this interface.</p>
 */
public non-sealed interface SubmenuItem extends MenuEntry, HasText<SubmenuItem> {

    /**
     * Returns the menu it opens.
     *
     * @return the menu, or empty until one is set
     */
    Optional<Menu> menu();

    /**
     * Sets the menu it opens, closing the one before if it is open. An item without a menu opens nothing.
     *
     * @param menu the menu
     * @return this item
     * @throws IllegalArgumentException if the item is in a menu or bar and {@code menu} already opens from somewhere
     *     else, or the item is in a menu and {@code menu} is that menu or one it opens from
     */
    SubmenuItem setMenu(Menu menu);

    /**
     * Returns whether the item can open its menu.
     *
     * @return true by default
     */
    boolean isEnabled();

    /**
     * Enables or disables the item. A disabled item draws dimmed and does not open its menu.
     *
     * @param enabled true to enable it
     * @return this item
     */
    SubmenuItem setEnabled(boolean enabled);
}
