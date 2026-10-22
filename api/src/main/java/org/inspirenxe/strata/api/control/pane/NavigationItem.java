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
package org.inspirenxe.strata.api.control.pane;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.Enableable;
import org.inspirenxe.strata.api.component.Tooltip;
import org.inspirenxe.strata.api.control.attribute.text.HasIcon;
import org.inspirenxe.strata.api.control.attribute.text.HasText;
import org.inspirenxe.strata.api.theme.SpriteId;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * An entry of a {@link NavigationView}'s pane that the user can pick: a section of the screen, with a text, an
 * optional icon, an optional badge, an optional page, and optionally items of its own below it.
 *
 * <p>A pick selects the item unless it is not {@linkplain #setSelectable selectable}, in which case it only
 * activates, as a Sign out item does. The item's page is built with the view's factory the first time the item is
 * selected, and kept while the item stays in the view; see {@link NavigationView}. An item with children shows an
 * arrow and can be {@linkplain #setExpanded expanded} to show them below it, indented.</p>
 *
 * <pre>{@code
 * NavigationItem market = factory.navigationItem("Market")
 *         .setIcon(SpriteId.of("example", "icon/market"))
 *         .setBadge(3)
 *         .setPage(MarketPage::new);
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#navigationItem(String)}. Do not implement this
 * interface.</p>
 */
public non-sealed interface NavigationItem extends NavigationEntry, HasText<NavigationItem>, HasIcon<NavigationItem>,
        Enableable<NavigationItem> {

    /**
     * Shows a sprite as the icon, replacing the icon set before. The icon is all a column of icons shows. The size
     * stays what it was, 16 by 16 by default.
     *
     * @param sprite the sprite; see {@link SpriteId} for where engines look for it
     * @return this item
     */
    @Override
    NavigationItem setIcon(SpriteId sprite);

    /**
     * Removes the icon. The space for it stays, so the texts of items line up.
     *
     * @return this item
     */
    @Override
    NavigationItem clearIcon();

    /**
     * Sets the size the icon is drawn at. The icon sits in a square one line tall before the text, so the texts of
     * items line up: an icon that fits is drawn at this size in the middle of the square, and a larger one is
     * scaled down to fit, keeping its shape.
     *
     * @param width the width in UI units, finite and not negative
     * @param height the height in UI units, finite and not negative
     * @return this item
     * @throws IllegalArgumentException if either size is negative, infinite or not a number
     */
    @Override
    NavigationItem setIconSize(float width, float height);

    /**
     * Returns whether the user can pick the item.
     *
     * @return true by default
     */
    @Override
    boolean isEnabled();

    /**
     * Sets whether the user can pick the item. A disabled item draws in the disabled text style, cannot be picked or
     * reached with the keyboard, and still shows its tooltip. Disabling the selected item keeps it selected.
     *
     * @param enabled false to keep the user from picking it
     * @return this item
     */
    @Override
    NavigationItem setEnabled(boolean enabled);

    /**
     * Returns the tooltip.
     *
     * @return the tooltip, or empty when none is set
     */
    Optional<Tooltip> tooltip();

    /**
     * Sets the tooltip shown while the pointer rests on the item. While the pane shows as a column of icons, an item
     * with no tooltip shows its text as one.
     *
     * @param tooltip the tooltip, or {@code null} for none
     * @return this item
     */
    NavigationItem setTooltip(Tooltip tooltip);

    /**
     * Returns whether a pick selects the item.
     *
     * @return true by default
     */
    boolean isSelectable();

    /**
     * Sets whether a pick selects the item. An item that is not selectable, such as Sign out, only activates when
     * picked, which {@link org.inspirenxe.strata.api.control.attribute.item.Activatable#ACTIVATED} reports; one with
     * children expands or collapses instead. Code can still select it.
     *
     * @param selectable false to only activate it
     * @return this item
     */
    NavigationItem setSelectable(boolean selectable);

    /**
     * Returns the count the item's badge shows.
     *
     * @return the count; 0 by default
     */
    int badge();

    /**
     * Sets the count the item's badge shows, drawn with the shared {@code badge.*} keys of
     * {@link org.inspirenxe.strata.api.control.display.Avatar} at the item's right end, or at the corner of its icon
     * in a column of icons. 0 hides it, and a count above {@code 99} shows as {@code 99+}.
     *
     * @param count the count
     * @return this item
     * @throws IllegalArgumentException if the count is negative
     */
    NavigationItem setBadge(int count);

    /**
     * Sets how the item's page is built. The view builds it with its factory the first time the item is selected,
     * shows it in its content area while the item is selected, and keeps it while the item stays in the view, so
     * coming back finds it as it was. Setting a page again, or {@code null}, discards the page kept so far; if the
     * item is selected, the new page is built at once.
     *
     * @param page builds the page, or {@code null} for none, in which case the view shows its own content while the
     *     item is selected
     * @return this item
     */
    NavigationItem setPage(Function<ComponentFactory, ? extends Component<?>> page);

    /**
     * Returns the item's children.
     *
     * @return a copy, in order
     */
    List<NavigationItem> items();

    /**
     * Adds a child at the end.
     *
     * @param child the child
     * @return this item
     * @throws IllegalArgumentException if the child belongs to a view or another item, or is this item or one of its
     *     ancestors, or was not made by Strata
     */
    NavigationItem addItem(NavigationItem child);

    /**
     * Removes a child, and with it the pages kept for it and its children. Does nothing if it is not a child of this
     * item.
     *
     * @param child the child
     * @return this item
     */
    NavigationItem removeItem(NavigationItem child);

    /**
     * Returns whether the item's children show below it.
     *
     * @return false by default
     */
    boolean isExpanded();

    /**
     * Shows or hides the item's children below it. In a view, this reports
     * {@link NavigationView#ITEM_EXPANDED} when it changes. While the pane shows as a column of icons, children open
     * in a menu instead, whatever this says.
     *
     * @param expanded true to show the children
     * @return this item
     */
    NavigationItem setExpanded(boolean expanded);
}
