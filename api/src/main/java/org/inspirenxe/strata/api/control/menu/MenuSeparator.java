package org.inspirenxe.strata.api.control.menu;

/**
 * A line between groups of menu entries. It cannot be highlighted or chosen.
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#menuSeparator()} or make one with
 * {@link Menu#addSeparator()}. Do not implement this interface.</p>
 */
public non-sealed interface MenuSeparator extends MenuEntry {
}
