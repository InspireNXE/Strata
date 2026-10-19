package org.inspirenxe.strata.api.control.menu;

/**
 * One entry of a {@link Menu}: a {@link MenuItem} that runs a command, a {@link CheckMenuItem} that flips a setting,
 * a {@link SubmenuItem} that opens another menu, or a {@link MenuSeparator} line. A {@link MenuBar}'s titles are
 * {@link SubmenuItem}s too.
 *
 * <p>An entry is made by the {@link org.inspirenxe.strata.api.ComponentFactory}, or by a menu's shorthands such as
 * {@link Menu#addItem(String, Runnable)}. It belongs to at most one menu or bar at a time. It is mutable: a change
 * to it lays out or redraws the menu or bar it is in. Entries are not components and have no events of their own;
 * the menu's {@link Menu#CHOOSE} and {@link Menu#CHOSEN} carry the entry chosen.</p>
 *
 * <p>Call entries on the UI thread. Do not implement this interface.</p>
 */
public sealed interface MenuEntry permits MenuItem, CheckMenuItem, SubmenuItem, MenuSeparator {
}
