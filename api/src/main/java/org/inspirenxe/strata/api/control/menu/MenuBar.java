package org.inspirenxe.strata.api.control.menu;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.ValueEvent;
import org.inspirenxe.strata.api.event.ValueRequestEvent;

import java.util.Optional;

/**
 * A row of menu titles, each opening its {@link Menu} below it.
 *
 * <p>Clicking a title opens its menu, and clicking it again closes it. While one of the bar's menus is open, moving
 * the pointer onto another title opens that one instead, and Left and Right in a menu with no submenu open move to
 * the neighboring title's menu. Titles are their text plus 8 units on each side, and the bar is one line plus 6 units
 * tall. The bar is not focusable.</p>
 *
 * <h2>Theme keys</h2>
 * <ul>
 *     <li>{@value #STYLE_BACKGROUND} (paint, optional): behind the whole bar. Nothing if missing.</li>
 *     <li>{@value #STYLE_ITEM_HOVERED} (paint, optional): behind the title under the pointer or with its menu
 *     open. Falls back to {@value Menu#STYLE_ITEM_HOVERED}.</li>
 *     <li>{@value #STYLE_TEXT} (text style, optional): the titles. Falls back to {@value Menu#STYLE_TEXT}.</li>
 * </ul>
 *
 * <pre>{@code
 * MenuBar bar = factory.menuBar()
 *         .addMenu("File", factory.menu().addItem("Quit", handle::close))
 *         .addMenu("View", viewMenu);
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#menuBar()}. Do not implement this
 * interface.</p>
 */
public interface MenuBar extends Component<MenuBar> {
    /**
     * Style key for the bar's background (paint, optional).
     */
    String STYLE_BACKGROUND = "menubar.background";
    /**
     * Style key for the title under the pointer or with its menu open (paint, optional).
     */
    String STYLE_ITEM_HOVERED = "menubar.item.hovered";
    /**
     * Style key for the titles (text style, optional).
     */
    String STYLE_TEXT = "menubar.text";

    /**
     * Requested when the user opens one of the bar's menus: a click on its title, moving the pointer onto another
     * title while a menu is open, or Left and Right in an open menu. The value is the title index. Unless a handler
     * cancels the request, the bar closes the open menu and opens the requested one; a handler can also change the
     * menu's items first. A rewritten index that is out of range or names the menu already open is ignored, which
     * leaves the bar as it is. A click on the title of the open menu closes it without this event. While a handler
     * cancels, each pointer move over another title fires it again. Fires only for the user's actions.
     */
    EventType<ValueRequestEvent<MenuBar, Integer>> OPEN = EventType.valueRequest("menu_bar.open");
    /**
     * Reported after one of the bar's menus opened from its title. The value is the title index, and
     * {@link ValueEvent#previous()} is the index of the menu open before, or -1 when none was. Opening a menu with
     * {@link Menu#open} does not make it the bar's open menu and reports nothing; closing a menu is reported by the
     * menu's {@link Menu#CLOSED}.
     */
    EventType<ValueEvent<MenuBar, Integer>> OPENED = EventType.valueReport("menu_bar.opened");

    /**
     * Adds a title at the right end.
     *
     * @param title the title
     * @param menu the menu it opens; it must not belong to another bar or be a submenu
     * @return this bar
     * @throws IllegalArgumentException if {@code menu} already belongs to a bar or is a submenu
     */
    MenuBar addMenu(String title, Menu menu);

    /**
     * Returns how many titles there are.
     *
     * @return the number of menus
     */
    int menuCount();

    /**
     * Returns the menu under a title.
     *
     * @param index the title index
     * @return the menu
     * @throws IndexOutOfBoundsException if there is no such title
     */
    Menu menu(int index);

    /**
     * Returns the index of the title whose menu is open.
     *
     * @return the index, or empty when none is open
     */
    Optional<Integer> openIndex();
}
