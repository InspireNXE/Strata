package org.inspirenxe.strata.api.control.menu;

import org.inspirenxe.strata.api.control.attribute.text.HasText;

/**
 * A menu entry that runs a command when chosen, with an optional shortcut hint drawn at its right.
 *
 * <pre>{@code
 * MenuItem copy = factory.menuItem("Copy")
 *         .setShortcut("Ctrl+C")
 *         .setAction(this::copy);
 * menu.addItem(copy);
 * copy.setEnabled(selection.isPresent());
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#menuItem(String)} or make one with
 * {@link Menu#addItem(String, Runnable)}. Do not implement this interface.</p>
 */
public non-sealed interface MenuItem extends MenuEntry, HasText<MenuItem> {

    /**
     * Returns the shortcut hint.
     *
     * @return the hint, never null; empty when there is none
     */
    String shortcut();

    /**
     * Sets the hint drawn at the right of the item, such as {@code "Ctrl+C"}. It is only text and binds no key.
     *
     * @param shortcut the hint, or an empty string for none
     * @return this item
     */
    MenuItem setShortcut(String shortcut);

    /**
     * Returns whether the item can be chosen.
     *
     * @return true by default
     */
    boolean isEnabled();

    /**
     * Enables or disables the item. A disabled item draws dimmed and cannot be highlighted or chosen.
     *
     * @param enabled true to enable it
     * @return this item
     */
    MenuItem setEnabled(boolean enabled);

    /**
     * Returns the action.
     *
     * @return what runs when the item is chosen; does nothing by default
     */
    Runnable action();

    /**
     * Sets what runs when the item is chosen, after the menus close and before {@link Menu#CHOSEN}.
     *
     * @param action the action
     * @return this item
     */
    MenuItem setAction(Runnable action);
}
