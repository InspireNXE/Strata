package org.inspirenxe.strata.api.widget;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.geom.Rect;

import java.util.function.Consumer;

/**
 * A list of commands that opens in a popup, such as a right-click menu or a menu in a {@link MenuBar}.
 *
 * <p>Items are plain commands, check items that flip a setting, submenus, and separators. A shortcut is text drawn at
 * the right of an item; it is only a hint and binds no key. Item indexes count separators.</p>
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
 * slot.on(EventTypes.CLICK, event -> {
 *     if (event.button() == MouseButton.RIGHT) {
 *         menu.openAt(event.x(), event.y());
 *     }
 * });
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#menu()}. Do not implement this interface.</p>
 */
public interface Menu extends Component<Menu> {
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
     * Adds a command.
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
     * Adds an item with a check mark that flips each time it is chosen.
     *
     * @param text the item's text
     * @param checked whether it starts checked
     * @param action told the new state when the item is chosen, after the menus close
     * @return this menu
     */
    Menu addCheckItem(String text, boolean checked, Consumer<Boolean> action);

    /**
     * Adds an item that opens another menu beside this one.
     *
     * @param text the item's text
     * @param submenu the menu to open beside this one
     * @return this menu
     * @throws IllegalArgumentException if {@code submenu} is this menu or one this menu opens from, is already a
     *     submenu, or is in a {@link MenuBar}
     */
    Menu addSubmenu(String text, Menu submenu);

    /**
     * Adds a separator line.
     *
     * @return this menu
     */
    Menu addSeparator();

    /**
     * Removes every item, closing the menu first if it is open.
     *
     * @return this menu
     */
    Menu clearItems();

    /**
     * Returns how many items there are, separators included.
     *
     * @return the number of items
     */
    int itemCount();

    /**
     * Returns an item's text.
     *
     * @param index the item index
     * @return the text, or an empty string for a separator
     * @throws IndexOutOfBoundsException if there is no such item
     */
    String itemText(int index);

    /**
     * Returns whether an item can be chosen.
     *
     * @param index the item index
     * @return true if enabled; false for a separator
     * @throws IndexOutOfBoundsException if there is no such item
     */
    boolean isItemEnabled(int index);

    /**
     * Enables or disables an item. A disabled item draws dimmed and cannot be highlighted or chosen.
     *
     * @param index the item index
     * @param enabled true to enable it
     * @return this menu
     * @throws IndexOutOfBoundsException if there is no such item
     */
    Menu setItemEnabled(int index, boolean enabled);

    /**
     * Returns whether a check item is checked.
     *
     * @param index the item index
     * @return true if it is a checked check item
     * @throws IndexOutOfBoundsException if there is no such item
     */
    boolean isItemChecked(int index);

    /**
     * Checks or unchecks a check item without running its action.
     *
     * @param index the item index
     * @param checked true to check it
     * @return this menu
     * @throws IndexOutOfBoundsException if there is no such item
     * @throws IllegalArgumentException if the item is not a check item
     */
    Menu setItemChecked(int index, boolean checked);

    /**
     * Returns whether the menu is open.
     *
     * @return true while it shows in a popup
     */
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
     * Closes the menu and any submenu open from it. Does nothing if it is not open.
     *
     * @return this menu
     */
    Menu close();

    /**
     * Adds a listener told each time the menu closes, for any reason.
     *
     * @param listener the listener
     * @return this menu
     */
    Menu onClosed(Runnable listener);
}
