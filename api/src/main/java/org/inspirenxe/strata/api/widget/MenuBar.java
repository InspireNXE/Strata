package org.inspirenxe.strata.api.widget;

import org.inspirenxe.strata.api.component.Component;

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
