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
package org.inspirenxe.strata.api.control.menu;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.Focusable;
import org.inspirenxe.strata.api.component.HasContextMenu;
import org.inspirenxe.strata.api.component.HasCursor;
import org.inspirenxe.strata.api.component.HasTooltip;
import org.inspirenxe.strata.api.control.attribute.popup.Closable;
import org.inspirenxe.strata.api.control.attribute.popup.Openable;
import org.inspirenxe.strata.api.control.attribute.popup.Popup;
import org.inspirenxe.strata.api.control.display.Separator;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.control.ValueEvent;
import org.inspirenxe.strata.api.event.control.ValueRequestEvent;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.text.Text;

import java.util.List;
import java.util.function.Consumer;

/**
 * A list of commands that opens in a popup, such as a right-click menu or a menu in a {@link MenuBar}.
 *
 * <p>Its entries are {@link MenuEntry}s: {@link MenuItem} commands, {@link CheckMenuItem}s that flip a setting,
 * {@link RadioMenuItem}s that pick one of several, {@link SubmenuItem}s that open another menu, and {@link MenuSeparator}s. Add them with {@link #addItem(MenuEntry)},
 * or let the shorthands such as {@link #addItem(String, Runnable)} make them. A shortcut is text drawn at the right of
 * an item; it is only a hint and binds no key. An item showing a {@linkplain MenuItem#setCommand command} with a
 * stroke and no hint text shows the stroke, and a menu bar runs that command from it.</p>
 *
 * <p>An open menu takes focus. Hovering an item highlights it, and hovering a submenu item opens the submenu beside
 * it. Up and Down move the highlight past separators and disabled items, wrapping at the ends, and repeat while held.
 * Right on a highlighted submenu item opens its submenu with the first item highlighted and focused. When hovering
 * already opened the submenu, Right moves focus into it and highlights its first item unless the pointer highlighted
 * one there. Left in a submenu closes it and gives focus back to the menu it opened from. Enter, keypad Enter and Space choose the
 * highlighted item, or open the submenu of a submenu item as Right does. Escape closes the menu it is pressed in,
 * with its submenus, and focus goes back to the menu it opened from. Home and End highlight the first and last items
 * that can be chosen. These keys work with any modifiers held, and the menu consumes them. A letter or digit, alone
 * or with Alt, chooses the one item whose mnemonic it is, or highlights the items that share it in turn; a letter no
 * item's mnemonic claims highlights the next item whose text starts with it, and goes on to the host when none
 * does. An item's mnemonics are underlined while Alt is held and once the keyboard opened or moved through the
 * menu. A left click on an item chooses it. Choosing an item, by click or key, closes the whole chain of menus and then runs the item's action.
 * Clicking outside closes the menus. When the first menu of a chain closes, focus goes back to where it was before
 * the chain opened. Items are chosen on the click even on a platform whose conventions say buttons act on the press,
 * since those conventions cover buttons, checkboxes, radio buttons, toggles and tabs.</p>
 *
 * <p>A menu is a component only so it can sit in a popup. Open it with {@link #open} or {@link #openAt} rather than
 * adding it to a container, or give it to a {@link org.inspirenxe.strata.api.control.button.DropDownButton}, which
 * opens it below itself at least as wide as the button. Items are one line plus 6 units tall, with 14 units at the
 * left for the check mark and 14 at the right for the submenu arrow, and separators 5 units tall.</p>
 *
 * <p>A menu is the whole content of its popup and has no parent, so its events ({@link #CHOOSE}, {@link #CHOSEN},
 * {@link Openable#OPENED} and {@link Closable#CLOSED}) reach only the handlers registered on the menu itself. A menu
 * opens only from code, its bar or a drop-down button, and the bar and the button ask on their own behalf, so it
 * never fires {@link Openable#OPEN}. It never fires {@link Closable#CLOSE} either.</p>
 *
 * <h2>Events</h2>
 * <ul>
 *     <li>{@link #CHOOSE}: Requested when the user chooses an item, by click or key. Cancel it to leave the menu open
 *     and run nothing.</li>
 *     <li>{@link #CHOSEN}: Reported after the menus closed and the chosen item's action ran.</li>
 *     <li>{@link Openable#OPENED}: Reported each time the menu opens, as a submenu too.</li>
 *     <li>{@link Closable#CLOSED}: Reported each time the menu closes, for any reason: a choice, Escape, a press
 *     outside, a parent menu closing, or {@link #close()} called from code.</li>
 * </ul>
 *
 * <h2>Theme keys</h2>
 * <ul>
 *     <li>{@value #STYLE_BACKGROUND} (paint, required): behind the whole menu.</li>
 *     <li>{@value #STYLE_ITEM_HOVERED} (paint, required): behind the highlighted item.</li>
 *     <li>{@value #STYLE_TEXT} (text style, optional): item text, check marks and arrows. Falls back to the default
 *     text style.</li>
 *     <li>{@value #STYLE_TEXT_DISABLED} (text style, optional): disabled items. Falls back to
 *     {@value #STYLE_TEXT}.</li>
 *     <li>{@value #STYLE_SHORTCUT} (text style, optional): shortcut hints. Falls back to
 *     {@value #STYLE_TEXT_DISABLED}.</li>
 *     <li>{@value #STYLE_SEPARATOR} (paint, optional): the line of a separator. Falls back to
 *     {@value Separator#STYLE_LINE}.</li>
 *     <li>{@value #STYLE_ITEM_PADDING} (insets, optional): the space around an item's text. The left side also
 *     holds the check mark and the right side the submenu arrow. Defaults to 14 at the sides and 3 at the top and
 *     bottom.</li>
 *     <li>{@value #STYLE_PADDING} (insets, optional): the space between the menu's edges and its items. Defaults
 *     to 2 on every side.</li>
 * </ul>
 *
 * <pre>{@code
 * Menu menu = factory.menu()
 *         .addItem("Copy", "Ctrl+C", this::copy)
 *         .addSeparator()
 *         .addCheckItem("Show prices", settings.prices(), settings::setPrices);
 * cell.onEvent(InputEvents.CLICK, event -> {
 *     if (event.button() == MouseButton.RIGHT) {
 *         menu.openAt(event.x(), event.y());
 *     }
 * });
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#menu()}. Do not implement this interface.</p>
 */
public interface Menu extends Component<Menu>, Focusable<Menu>, HasCursor<Menu>, HasTooltip<Menu>, HasContextMenu<Menu>,
        Popup<Menu> {
    /**
     * Style key for the menu's background (paint, required).
     */
    String STYLE_BACKGROUND = "menu.background";
    /**
     * Style key for the highlighted item (paint, required).
     */
    String STYLE_ITEM_HOVERED = "menu.item.hovered";
    /**
     * Style key for item text (text style, optional).
     */
    String STYLE_TEXT = "menu.text";
    /**
     * Style key for disabled item text (text style, optional).
     */
    String STYLE_TEXT_DISABLED = "menu.text.disabled";
    /**
     * Style key for shortcut hints (text style, optional).
     */
    String STYLE_SHORTCUT = "menu.shortcut";
    /**
     * Style key for separator lines (paint, optional).
     */
    String STYLE_SEPARATOR = "menu.separator";
    /**
     * Style key for the space around a menu item's text (insets, optional). The left side also holds the check
     * mark and the right side the submenu arrow. Defaults to 14 at the sides and 3 at the top and bottom.
     */
    String STYLE_ITEM_PADDING = "menu.item.padding";
    /**
     * Style key for the space between the menu's edges and its items (insets, optional). Defaults to 2 on every
     * side.
     */
    String STYLE_PADDING = "menu.padding";

    /**
     * Requested when the user chooses an entry: a click on it, or Enter, keypad Enter or Space while it is
     * highlighted. The value is the {@link MenuItem}, {@link CheckMenuItem} or {@link RadioMenuItem} chosen. Unless a handler cancels the
     * request, the menu closes its whole chain, flips a check item and runs the entry's action. A rewritten value
     * that is not one of this menu's items, is disabled, or is a separator or a submenu item is ignored, which leaves
     * the menu open. Submenu items do not fire this event, and a choice in a submenu fires the submenu's event, not
     * this menu's. Fires only for the user's actions.
     */
    EventType<ValueRequestEvent<Menu, MenuEntry>> CHOOSE = EventType.valueRequest("menu.choose");
    /**
     * Reported after the user chose an entry, once the menus have closed and the entry's action has run. The value
     * is the entry, and {@link ValueEvent#previous()} is the same entry. Code cannot choose an entry, so only the
     * user's choices report it.
     */
    EventType<ValueEvent<Menu, MenuEntry>> CHOSEN = EventType.valueReport("menu.chosen");

    /**
     * Returns the entries, separators included, in the order they show.
     *
     * @return an unmodifiable copy of the entries
     */
    List<MenuEntry> items();

    /**
     * Adds an entry at the end.
     *
     * @param entry the entry; a submenu item's menu starts opening from this menu
     * @return this menu
     * @throws IllegalArgumentException if the entry is already in a menu or bar, or is a submenu item whose menu
     *     already opens from somewhere else, is this menu, or is one this menu opens from
     */
    Menu addItem(MenuEntry entry);

    /**
     * Adds an entry at an index, moving the entries from there down.
     *
     * @param index where the entry goes, from 0 to the number of entries
     * @param entry the entry
     * @return this menu
     * @throws IndexOutOfBoundsException if the index is outside 0 to the number of entries
     * @throws IllegalArgumentException for the reasons {@link #addItem(MenuEntry)} gives
     */
    Menu addItem(int index, MenuEntry entry);

    /**
     * Removes an entry, closing its submenu if it is open. Does nothing if the entry is not in this menu.
     *
     * @param entry the entry
     * @return this menu
     */
    Menu removeItem(MenuEntry entry);

    /**
     * Adds a command, as {@code addItem(factory.menuItem(text).setAction(action))} would.
     *
     * @param text the item's text
     * @param action run when the item is chosen, after the menus close
     * @return this menu
     */
    Menu addItem(Text text, Runnable action);

    /**
     * Does the same as {@link #addItem(Text, Runnable)} with literal text.
     *
     * @param text the item's text
     * @param action run when the item is chosen, after the menus close
     * @return this menu
     */
    default Menu addItem(final String text, final Runnable action) {
        return this.addItem(Text.of(text), action);
    }

    /**
     * Adds a command with a shortcut hint.
     *
     * @param text the item's text
     * @param shortcut the hint drawn at the right, such as {@code "Ctrl+C"}
     * @param action run when the item is chosen, after the menus close
     * @return this menu
     */
    Menu addItem(Text text, Text shortcut, Runnable action);

    /**
     * Does the same as {@link #addItem(Text, Text, Runnable)} with literal text.
     *
     * @param text the item's text
     * @param shortcut the hint drawn at the right, such as {@code "Ctrl+C"}
     * @param action run when the item is chosen, after the menus close
     * @return this menu
     */
    default Menu addItem(final String text, final String shortcut, final Runnable action) {
        return this.addItem(Text.of(text), Text.of(shortcut), action);
    }

    /**
     * Adds a {@link CheckMenuItem}, an item with a check mark that flips each time it is chosen.
     *
     * @param text the item's text
     * @param checked whether it starts checked
     * @param action told the new state when the item is chosen, after the menus close
     * @return this menu
     */
    Menu addCheckItem(Text text, boolean checked, Consumer<Boolean> action);

    /**
     * Does the same as {@link #addCheckItem(Text, boolean, Consumer)} with literal text.
     *
     * @param text the item's text
     * @param checked whether it starts checked
     * @param action told the new state when the item is chosen, after the menus close
     * @return this menu
     */
    default Menu addCheckItem(final String text, final boolean checked, final Consumer<Boolean> action) {
        return this.addCheckItem(Text.of(text), checked, action);
    }

    /**
     * Adds a {@link SubmenuItem}, an item that opens another menu beside this one.
     *
     * @param text the item's text
     * @param submenu the menu to open beside this one
     * @return this menu
     * @throws IllegalArgumentException if {@code submenu} is this menu or one this menu opens from, is already a
     *     submenu, is in a {@link MenuBar}, or is held by a
     *     {@link org.inspirenxe.strata.api.control.button.DropDownButton}
     */
    Menu addSubmenu(Text text, Menu submenu);

    /**
     * Does the same as {@link #addSubmenu(Text, Menu)} with literal text.
     *
     * @param text the item's text
     * @param submenu the menu to open beside this one
     * @return this menu
     * @throws IllegalArgumentException if {@code submenu} is this menu or one this menu opens from, is already a
     *     submenu, is in a {@link MenuBar}, or is held by a
     *     {@link org.inspirenxe.strata.api.control.button.DropDownButton}
     */
    default Menu addSubmenu(final String text, final Menu submenu) {
        return this.addSubmenu(Text.of(text), submenu);
    }

    /**
     * Adds a {@link MenuSeparator}.
     *
     * @return this menu
     */
    Menu addSeparator();

    /**
     * Removes every entry, closing the menu first if it is open.
     *
     * @return this menu
     */
    Menu clearItems();

    /**
     * Sets the least width the menu is laid out at, keeping its minimum height, as
     * {@code setMinSize(width, minHeight())} does. A menu is also never narrower than 60 units or than its widest
     * item, and a {@link org.inspirenxe.strata.api.control.button.DropDownButton} opens it at least as wide as the
     * button without changing this setting. {@link #minWidth()} returns it.
     *
     * @param width the least width in UI units; 0 by default
     * @return this menu
     */
    Menu setMinWidth(float width);

    /**
     * Returns whether the menu is open.
     *
     * @return true while it shows in a popup
     */
    @Override
    boolean isOpen();

    /**
     * Opens the menu below a rectangle, or above it when there is more room there, as
     * {@link org.inspirenxe.strata.api.overlay.Overlays#popup} places popups. Does nothing if it is already open or
     * has no items.
     *
     * @param anchor the rectangle in root coordinates, such as a button's bounds
     * @return this menu
     * @throws IllegalStateException if the factory's host has no screen yet
     */
    @Override
    Menu open(Rect anchor);

    /**
     * Opens the menu with its top-left corner at a point, moved to stay inside the host. Does nothing if it is
     * already open or has no items.
     *
     * @param x the point's x in root coordinates
     * @param y the point's y in root coordinates
     * @return this menu
     * @throws IllegalStateException if the factory's host has no screen yet
     */
    @Override
    Menu openAt(float x, float y);

    /**
     * Closes the menu and any submenu open from it, and reports {@link Closable#CLOSED}. Does nothing if it is not
     * open.
     *
     * @return this menu
     */
    @Override
    Menu close();
}
