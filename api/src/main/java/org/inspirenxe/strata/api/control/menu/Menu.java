package org.inspirenxe.strata.api.control.menu;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.control.attribute.popup.Closable;
import org.inspirenxe.strata.api.control.attribute.popup.Openable;
import org.inspirenxe.strata.api.control.display.Separator;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.control.ValueEvent;
import org.inspirenxe.strata.api.event.control.ValueRequestEvent;
import org.inspirenxe.strata.api.geometry.Rect;

import java.util.List;
import java.util.function.Consumer;

/**
 * A list of commands that opens in a popup, such as a right-click menu or a menu in a {@link MenuBar}.
 *
 * <p>Its entries are {@link MenuEntry}s: {@link MenuItem} commands, {@link CheckMenuItem}s that flip a setting,
 * {@link SubmenuItem}s that open another menu, and {@link MenuSeparator}s. Add them with {@link #addItem(MenuEntry)},
 * or let the shorthands such as {@link #addItem(String, Runnable)} make them. A shortcut is text drawn at the right of
 * an item; it is only a hint and binds no key.</p>
 *
 * <p>An open menu takes focus. Hovering an item highlights it, and hovering a submenu item opens the submenu beside
 * it. Up and Down move the highlight past separators and disabled items, wrapping at the ends; Right and Left open
 * and close a submenu; Enter, keypad Enter and Space choose the highlighted item; Escape closes the menu it is
 * pressed in. Choosing an item, by click or key, closes the whole chain of menus and then runs the item's action.
 * Clicking outside closes the menus. When the first menu of a chain closes, focus goes back to where it was before
 * the chain opened.</p>
 *
 * <p>A menu is a component only so it can sit in a popup. Open it with {@link #open} or {@link #openAt} rather than
 * adding it to a container. Items are one line plus 6 units tall, with 14 units at the left for the check mark and 14
 * at the right for the submenu arrow, and separators 5 units tall.</p>
 *
 * <p>A menu is the whole content of its popup and has no parent, so its events ({@link #CHOOSE}, {@link #CHOSEN},
 * {@link Openable#OPENED} and {@link Closable#CLOSED}) reach only the handlers registered on the menu itself. A menu
 * opens only from code or from its bar, so it never fires {@link Openable#OPEN}, and it never fires
 * {@link Closable#CLOSE}.</p>
 *
 * <h2>Events</h2>
 * <ul>
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
public interface Menu extends Component<Menu>, Openable<Menu> {
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
     * Requested when the user chooses an entry: a click on it, or Enter, keypad Enter or Space while it is
     * highlighted. The value is the {@link MenuItem} or {@link CheckMenuItem} chosen. Unless a handler cancels the
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
    Menu addItem(String text, Runnable action);

    /**
     * Adds a command with a shortcut hint.
     *
     * @param text the item's text
     * @param shortcut the hint drawn at the right, such as {@code "Ctrl+C"}
     * @param action run when the item is chosen, after the menus close
     * @return this menu
     */
    Menu addItem(String text, String shortcut, Runnable action);

    /**
     * Adds a {@link CheckMenuItem}, an item with a check mark that flips each time it is chosen.
     *
     * @param text the item's text
     * @param checked whether it starts checked
     * @param action told the new state when the item is chosen, after the menus close
     * @return this menu
     */
    Menu addCheckItem(String text, boolean checked, Consumer<Boolean> action);

    /**
     * Adds a {@link SubmenuItem}, an item that opens another menu beside this one.
     *
     * @param text the item's text
     * @param submenu the menu to open beside this one
     * @return this menu
     * @throws IllegalArgumentException if {@code submenu} is this menu or one this menu opens from, is already a
     *     submenu, or is in a {@link MenuBar}
     */
    Menu addSubmenu(String text, Menu submenu);

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
