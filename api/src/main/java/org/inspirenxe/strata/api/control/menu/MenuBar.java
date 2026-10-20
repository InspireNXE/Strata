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
import org.inspirenxe.strata.api.component.HasCursor;
import org.inspirenxe.strata.api.component.HasTooltip;
import org.inspirenxe.strata.api.control.attribute.item.SingleSelection;
import org.inspirenxe.strata.api.text.Text;

import java.util.List;
import java.util.Optional;

/**
 * A row of menu titles, each opening its {@link Menu} below it.
 *
 * <p>Clicking a title opens its menu, and clicking it again closes it. While one of the bar's menus is open, moving
 * the pointer onto another title opens that one instead, and Left and Right in a menu with no submenu open move to
 * the neighboring title's menu. Titles are their text plus 8 units on each side, and the bar is one line plus 6 units
 * tall. The bar is not focusable.</p>
 *
 * <p>The bar's selection is its open menu: {@link #selectedIndex()} is the index of the title whose menu is open, and
 * {@link #select(int)} opens a menu from code.</p>
 *
 * <h2>Events</h2>
 * <ul>
 *     <li>{@link SingleSelection#SELECT}: Requested when the user opens one of the bar's menus: a click on its title,
 *     moving the pointer onto another title while a menu is open, or Left and Right in an open menu. The value is the
 *     title index. Unless a handler cancels the request, the bar closes the open menu and opens the requested one; a
 *     handler can also change the menu's items first. A rewritten index that is out of range or names the menu already
 *     open is ignored, which leaves the bar as it is. A click on the title of the open menu closes it without this
 *     event. While a handler cancels, each pointer move over another title fires it again. Fires only for the user's
 *     actions.</li>
 *     <li>{@link SingleSelection#SELECTED}: Reported after the bar's open menu changed: a menu opened from its title or
 *     from {@link #select(int)}, or the open menu closed, however it closed. The value is the index of the title whose
 *     menu is open now, or -1 when none is, and the previous value the index before. Moving from one menu to another
 *     reports once. Opening a menu with {@link Menu#open} does not make it the bar's open menu and reports
 *     nothing.</li>
 * </ul>
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
public interface MenuBar extends Component<MenuBar>, HasCursor<MenuBar>, HasTooltip<MenuBar>, SingleSelection<Menu,
        MenuBar> {
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
     * Returns the titles, left to right.
     *
     * @return an unmodifiable copy of the titles
     */
    List<SubmenuItem> menus();

    /**
     * Adds a title at the right end. Its menu starts opening from this bar.
     *
     * @param entry the title and the menu it opens
     * @return this bar
     * @throws IllegalArgumentException if the entry is already in a menu or bar, or its menu already opens from
     *     somewhere else
     */
    MenuBar addMenu(SubmenuItem entry);

    /**
     * Adds a title at an index, moving the titles from there to the right.
     *
     * @param index where the title goes, from 0 to the number of titles
     * @param entry the title and the menu it opens
     * @return this bar
     * @throws IndexOutOfBoundsException if the index is outside 0 to the number of titles
     * @throws IllegalArgumentException for the reasons {@link #addMenu(SubmenuItem)} gives
     */
    MenuBar addMenu(int index, SubmenuItem entry);

    /**
     * Removes a title, closing its menu if it is open. Does nothing if the title is not in this bar.
     *
     * @param entry the title
     * @return this bar
     */
    MenuBar removeMenu(SubmenuItem entry);

    /**
     * Adds a title at the right end, as {@code addMenu(factory.submenuItem(title).setMenu(menu))} would.
     *
     * @param title the title
     * @param menu the menu it opens; it must not belong to another bar or be a submenu
     * @return this bar
     * @throws IllegalArgumentException if {@code menu} already belongs to a bar or is a submenu
     */
    MenuBar addMenu(Text title, Menu menu);

    /**
     * Does the same as {@link #addMenu(Text, Menu)} with literal text.
     *
     * @param title the title
     * @param menu the menu it opens; it must not belong to another bar or be a submenu
     * @return this bar
     * @throws IllegalArgumentException if {@code menu} already belongs to a bar or is a submenu
     */
    default MenuBar addMenu(final String title, final Menu menu) {
        return this.addMenu(Text.of(title), menu);
    }

    /**
     * Returns the index of the title whose menu is open.
     *
     * @return the index, or -1 when none is open
     */
    @Override
    int selectedIndex();

    /**
     * Returns the open menu.
     *
     * @return the menu, or empty when none is open
     */
    @Override
    Optional<Menu> selected();

    /**
     * Opens the menu under a title, closing the one open before, or closes the open menu for -1. Fires
     * {@link SingleSelection#SELECTED} if that changed which menu is open, and never {@link SingleSelection#SELECT}.
     * A menu with no items, a disabled title, or a bar that is not attached, stays closed.
     *
     * @param index the title index, or -1
     * @return this bar
     * @throws IndexOutOfBoundsException if the index is not -1 and there is no such title
     */
    @Override
    MenuBar select(int index);

    /**
     * Opens a menu of this bar, or closes the open menu if the menu is not one of the bar's.
     *
     * @param menu the menu
     * @return this bar
     */
    @Override
    MenuBar selectItem(Menu menu);
}
