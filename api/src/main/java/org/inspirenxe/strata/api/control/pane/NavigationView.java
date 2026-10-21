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
import org.inspirenxe.strata.api.component.Focusable;
import org.inspirenxe.strata.api.component.HasCursor;
import org.inspirenxe.strata.api.component.HasTooltip;
import org.inspirenxe.strata.api.control.attribute.item.Activatable;
import org.inspirenxe.strata.api.control.attribute.item.Selection;
import org.inspirenxe.strata.api.control.attribute.item.SingleSelection;
import org.inspirenxe.strata.api.control.attribute.text.Titled;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.control.ValueEvent;
import org.inspirenxe.strata.api.event.control.ValueRequestEvent;
import org.inspirenxe.strata.api.state.PropertyKey;
import org.inspirenxe.strata.api.text.Text;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * A pane of navigation items at the side of a screen, with the content of the selected item beside it, for screens
 * with several sections such as a settings screen or a market with Buy, Sell and History.
 *
 * <h2>Display modes</h2>
 * <ul>
 *     <li>{@link DisplayMode#EXPANDED}: the pane is open at its {@linkplain #setPaneWidth width} beside the content,
 *     with each item's icon and text.</li>
 *     <li>{@link DisplayMode#COMPACT}: the pane is a column of icons, as wide as an item is tall plus the pane's
 *     padding, beside the content. An item's tooltip is its text unless one is set. The toggle opens the full pane
 *     over the content.</li>
 *     <li>{@link DisplayMode#MINIMAL}: the pane is hidden, and a bar across the top holds the toggle and the title.
 *     The toggle opens the pane over the content.</li>
 *     <li>{@link DisplayMode#AUTO}, the default: {@code EXPANDED} at {@link #compactBelow()} units wide and more,
 *     {@code COMPACT} from {@link #minimalBelow()}, and {@code MINIMAL} below, worked out from the view's width each
 *     time it is arranged. {@link #shownMode()} reports the result.</li>
 * </ul>
 *
 * <p>The pane opened over the content is a popup laid out at the view's left edge, the pane's width wide and as tall
 * as the view, through {@link org.inspirenxe.strata.api.overlay.Overlays#popupIn}. A press outside it, Escape, and a
 * pick close it, as does a change of the shown mode. While it is open the pane is in that popup, outside the view's
 * tree, so control events of the pane header and footer reach only handlers on them until it closes.</p>
 *
 * <h2>The pane</h2>
 * <p>From the top: the toggle, drawn as three lines, and the title, then the
 * {@linkplain #setPaneHeader pane header}, the items in a scrolling list, the {@linkplain #addFooterItem footer items},
 * and the {@linkplain #setPaneFooter pane footer}. The toggle shows while the pane can be opened over the content,
 * so not in {@code EXPANDED}. Headers are captions, hidden in the column of icons along with the pane header and
 * footer, and separators are lines. An item with children shows an arrow at its right end; its children show below
 * it, indented, while it is expanded. The selected item draws its selected background and an indicator
 * {@value #INDICATOR_WIDTH} units wide down its left edge, and so does an item whose selected descendant is hidden.
 * Disabled items draw in the disabled text and cannot be picked; their tooltips still show.</p>
 *
 * <h2>Picking</h2>
 * <p>A click on an item asks {@link Activatable#ACTIVATE}, then, if the item is selectable and not selected,
 * {@link Selection#SELECT}, and reports {@link Selection#SELECTED} and {@link Activatable#ACTIVATED}. Activation
 * fires on every click, the selected item included, so an item that only acts, such as Sign out, is made
 * {@linkplain NavigationItem#setSelectable not selectable} and handled with {@code ACTIVATED}. An item with
 * children that is not selectable expands or collapses instead of being selected. A click on an item's arrow asks
 * {@link #EXPAND_ITEM} and nothing else. In the column of icons, a click on an item with children opens its
 * children in a menu beside it, made of menu items that mirror them; choosing one is a click on that child. With
 * {@linkplain #setActivateOnClick activate on click} turned off, a single click selects or expands without
 * activating, and a double click activates.</p>
 *
 * <p>The indexes of {@code SELECT}, {@code SELECTED}, {@code ACTIVATE} and {@code ACTIVATED} count
 * {@link #allItems()}: every item depth first, the children of collapsed items included, then the footer items in the
 * same way. Adding or removing items before the selected one changes its index, which reports {@code SELECTED}.</p>
 *
 * <h2>Keyboard</h2>
 * <p>The view is one tab stop. While it has focus, a cursor marks one of the items the pane shows, starting on the
 * selected item: Up and Down move it, skipping headers, separators and disabled items, Home and End go to the first
 * and last, Enter and Space pick its item as a click does, Right expands an item with children, or opens its menu in
 * the column of icons, and Left collapses an expanded item or moves to its parent. With the pane hidden in
 * {@code MINIMAL}, Enter and Space open it as the toggle does. Tab moves on to the pane header, the pane footer and
 * the content. The cursor's item draws {@value #STYLE_FOCUS} while the view has focus.</p>
 *
 * <h2>Pages and content</h2>
 * <p>An item's {@linkplain NavigationItem#setPage page} is built with the view's factory the first time the item is
 * selected, and kept while the item stays in the view. Selecting another item takes the old page out of the tree,
 * as a collapsed expander takes out its content, so a hidden page holds no focus and costs no layout, and coming
 * back finds its text, scroll and selection as they were. Removing the item or setting its page again discards the
 * kept page. While the selected item has no page, or nothing is selected, the view shows its
 * {@linkplain #setContent content}, so code that manages its own content sets it from a {@code SELECTED} handler.
 * Pages, the content, the pane header and the pane footer are content, so their controls' events go on past the
 * view.</p>
 *
 * <p>The view stretches over its slot by default. Disabled, it closes the pane over the content and its menu, and it
 * and everything in it take no focus or input.</p>
 *
 * <h2>Events</h2>
 * <ul>
 *     <li>{@link Selection#SELECT} and {@link Selection#SELECTED}: asked when the user picks a selectable item that
 *     is not selected, and reported after every change, code included.</li>
 *     <li>{@link Activatable#ACTIVATE} and {@link Activatable#ACTIVATED}: asked and reported for every pick of an
 *     enabled item.</li>
 *     <li>{@link #EXPAND} and {@link #EXPANDED}: asked when the user opens or closes the pane with the toggle, and
 *     reported after every change, code, a press outside, Escape, a pick and a change of mode included.</li>
 *     <li>{@link #EXPAND_ITEM} and {@link #ITEM_EXPANDED}: asked when the user expands or collapses an item, and
 *     reported after every change of an item in the view.</li>
 * </ul>
 *
 * <h2>Theme keys</h2>
 * <ul>
 *     <li>{@value #STYLE_PANE} (paint, optional): behind the pane beside the content and the bar of
 *     {@code MINIMAL}. Nothing is drawn if it is missing.</li>
 *     <li>{@value #STYLE_PANE_OVERLAY} (paint, optional): behind the pane opened over the content. Falls back to
 *     {@value #STYLE_PANE}, and to the combo box's {@code combobox.popup} when that is missing too.</li>
 *     <li>{@value #STYLE_ITEM_HOVERED} (paint, optional): behind the hovered item. Falls back to the list's
 *     {@code list.row.hovered}.</li>
 *     <li>{@value #STYLE_ITEM_SELECTED} (paint, required): behind the selected item.</li>
 *     <li>{@value #STYLE_INDICATOR} (paint, optional): the strip down the selected item's left edge. Nothing is
 *     drawn if it is missing.</li>
 *     <li>{@value #STYLE_ITEM_TEXT} (text style, optional): an item's text, and the color of arrows and of the
 *     toggle's lines. Falls back to the default text style.</li>
 *     <li>{@value #STYLE_ITEM_TEXT_SELECTED} (text style, optional): the selected item's text. Falls back to
 *     {@value #STYLE_ITEM_TEXT}.</li>
 *     <li>{@value #STYLE_ITEM_TEXT_DISABLED} (text style, optional): a disabled item's text. Falls back to the text
 *     box's {@code textbox.text.disabled}, then to {@value #STYLE_ITEM_TEXT}.</li>
 *     <li>{@value #STYLE_HEADER_TEXT} (text style, optional): a header. Falls back to
 *     {@value #STYLE_ITEM_TEXT_DISABLED}.</li>
 *     <li>{@value #STYLE_TITLE_TEXT} (text style, optional): the title. Falls back to the default text style.</li>
 *     <li>{@value #STYLE_SEPARATOR} (paint, optional): a separator's line. Falls back to the separator control's
 *     {@code separator}.</li>
 *     <li>{@value #STYLE_TOGGLE} and {@value #STYLE_TOGGLE_HOVERED} (paint, optional): behind the toggle, and behind
 *     it while the pointer is over it. Without the first nothing is drawn, and the second falls back to
 *     {@value #STYLE_ITEM_HOVERED}. The three lines are drawn over them in the item text's color.</li>
 *     <li>{@value #STYLE_FOCUS} (paint, optional): a one-unit ring around the keyboard cursor's item. Falls back to
 *     the button's {@code button.focus}.</li>
 *     <li>{@value #STYLE_ITEM_PADDING} (insets, optional): around an item's icon and text. Defaults to 6 at the
 *     sides and 5 at the top and bottom, so an item is a line plus 10 tall.</li>
 *     <li>{@value #STYLE_PANE_PADDING} (insets, optional): between the pane's edges and what it holds. Defaults to
 *     2 on every side.</li>
 * </ul>
 *
 * <p>Badges use the shared {@code badge.*} keys of {@link org.inspirenxe.strata.api.control.display.Avatar}, and the
 * scrolling list the scroll panel's keys.</p>
 *
 * <pre>{@code
 * NavigationView settings = factory.navigationView()
 *         .setTitle("Settings")
 *         .addItem("General", GeneralPage::new)
 *         .addItem("Sound", SoundPage::new)
 *         .addSeparator()
 *         .addFooterItem(factory.navigationItem("Sign out").setSelectable(false));
 * settings.onEvent(Activatable.ACTIVATED, event -> { ... });
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#navigationView()}. Do not implement this
 * interface.</p>
 */
public interface NavigationView extends Component<NavigationView>, Focusable<NavigationView>,
        HasCursor<NavigationView>, HasTooltip<NavigationView>, Titled<NavigationView>,
        SingleSelection<NavigationItem, NavigationView>, Activatable<NavigationView> {
    /**
     * Style key for behind the pane beside the content and the bar of {@code MINIMAL} (paint, optional).
     */
    String STYLE_PANE = "nav.pane";
    /**
     * Style key for behind the pane opened over the content (paint, optional).
     */
    String STYLE_PANE_OVERLAY = "nav.pane.overlay";
    /**
     * Style key for behind the hovered item (paint, optional).
     */
    String STYLE_ITEM_HOVERED = "nav.item.hovered";
    /**
     * Style key for behind the selected item (paint, required).
     */
    String STYLE_ITEM_SELECTED = "nav.item.selected";
    /**
     * Style key for the strip down the selected item's left edge (paint, optional).
     */
    String STYLE_INDICATOR = "nav.indicator";
    /**
     * Style key for an item's text (text style, optional).
     */
    String STYLE_ITEM_TEXT = "nav.item.text";
    /**
     * Style key for the selected item's text (text style, optional).
     */
    String STYLE_ITEM_TEXT_SELECTED = "nav.item.text.selected";
    /**
     * Style key for a disabled item's text (text style, optional).
     */
    String STYLE_ITEM_TEXT_DISABLED = "nav.item.text.disabled";
    /**
     * Style key for a header (text style, optional).
     */
    String STYLE_HEADER_TEXT = "nav.header.text";
    /**
     * Style key for the title (text style, optional).
     */
    String STYLE_TITLE_TEXT = "nav.title.text";
    /**
     * Style key for a separator's line (paint, optional).
     */
    String STYLE_SEPARATOR = "nav.separator";
    /**
     * Style key for behind the toggle (paint, optional).
     */
    String STYLE_TOGGLE = "nav.toggle";
    /**
     * Style key for behind the toggle while the pointer is over it (paint, optional).
     */
    String STYLE_TOGGLE_HOVERED = "nav.toggle.hovered";
    /**
     * Style key for the ring around the keyboard cursor's item (paint, optional).
     */
    String STYLE_FOCUS = "nav.focus";
    /**
     * Style key for the space around an item's icon and text (insets, optional). Defaults to 6 at the sides and 5 at
     * the top and bottom.
     */
    String STYLE_ITEM_PADDING = "nav.item.padding";
    /**
     * Style key for the space between the pane's edges and what it holds (insets, optional). Defaults to 2 on every
     * side.
     */
    String STYLE_PANE_PADDING = "nav.pane.padding";
    /**
     * The pane's width unless {@link #setPaneWidth} says otherwise, in UI units.
     */
    float DEFAULT_PANE_WIDTH = 120f;
    /**
     * The width below which {@link DisplayMode#AUTO} shows {@code COMPACT} unless {@link #setAutoWidths} says
     * otherwise, in UI units.
     */
    float DEFAULT_COMPACT_BELOW = 400f;
    /**
     * The width below which {@link DisplayMode#AUTO} shows {@code MINIMAL} unless {@link #setAutoWidths} says
     * otherwise, in UI units.
     */
    float DEFAULT_MINIMAL_BELOW = 240f;
    /**
     * The width of the strip down the selected item's left edge, in UI units.
     */
    float INDICATOR_WIDTH = 3f;

    /**
     * Asked before the user opens or closes the pane over the content with the toggle, with whether it would be
     * open. Cancel it to leave the pane as it is. Fires only for the toggle, and for Enter or Space on a view with a
     * hidden pane.
     */
    EventType<ValueRequestEvent<NavigationView, Boolean>> EXPAND = EventType.valueRequest("navigation_view.expand");
    /**
     * Told after the pane over the content opened or closed, by the user, from code, or because a press outside,
     * Escape, a pick or a change of the shown mode closed it.
     */
    EventType<ValueEvent<NavigationView, Boolean>> EXPANDED = EventType.valueReport("navigation_view.expanded");
    /**
     * Asked before the user expands or collapses an item's children, with the item and whether it would be
     * expanded. A handler may rewrite it to another item of the view; the view ignores null and items it does not
     * hold.
     */
    EventType<ValueRequestEvent<NavigationView, Expansion>> EXPAND_ITEM =
            EventType.valueRequest("navigation_view.expand_item");
    /**
     * Told after an item of the view was expanded or collapsed, by the user or with
     * {@link NavigationItem#setExpanded}. The previous value names the same item with the other state.
     */
    EventType<ValueEvent<NavigationView, Expansion>> ITEM_EXPANDED =
            EventType.valueReport("navigation_view.item_expanded");
    /**
     * Whether the pane is open over the content, as {@link #isExpanded()} and {@link #setExpanded} say. Binds two ways
     * through {@link #EXPANDED}, so the pane closing on a press outside or a pick writes false. A true value does
     * nothing where {@code setExpanded} cannot open the pane, and is not written back. It is named after the getter
     * because the report has the property's name.
     */
    PropertyKey<NavigationView, Boolean> IS_EXPANDED = PropertyKey.of("navigation_view.is_expanded",
            NavigationView::isExpanded, NavigationView::setExpanded, EXPANDED);

    /**
     * Returns the entries of the pane above the footer.
     *
     * @return a copy, in order
     */
    List<NavigationEntry> items();

    /**
     * Adds an entry at the end of the pane, above the footer items.
     *
     * @param entry the entry
     * @return this view
     * @throws IllegalArgumentException if the entry belongs to a view or an item already, or was not made by Strata
     */
    NavigationView addItem(NavigationEntry entry);

    /**
     * Adds an entry at a place among the entries above the footer.
     *
     * @param index where it goes: 0 for the first, the number of entries for the last
     * @param entry the entry
     * @return this view
     * @throws IndexOutOfBoundsException if the index is negative or greater than the number of entries
     * @throws IllegalArgumentException if the entry belongs to a view or an item already, or was not made by Strata
     */
    NavigationView addItem(int index, NavigationEntry entry);

    /**
     * Removes an entry from above the footer, and with it the pages kept for it and its children. Removing the
     * selected item clears the selection. Does nothing if the view does not hold the entry there.
     *
     * @param entry the entry
     * @return this view
     */
    NavigationView removeItem(NavigationEntry entry);

    /**
     * Removes every entry above the footer, as {@link #removeItem} does. The footer items stay.
     *
     * @return this view
     */
    NavigationView clearItems();

    /**
     * Adds an item with a text and a page at the end of the pane.
     *
     * @param text the item's text
     * @param page builds the item's page; see {@link NavigationItem#setPage}
     * @return this view
     */
    NavigationView addItem(Text text, Function<ComponentFactory, ? extends Component<?>> page);

    /**
     * Does the same as {@link #addItem(Text, Function)} with literal text.
     *
     * @param text the item's text
     * @param page builds the item's page
     * @return this view
     */
    default NavigationView addItem(final String text, final Function<ComponentFactory, ? extends Component<?>> page) {
        return this.addItem(Text.of(text), page);
    }

    /**
     * Adds a header with a text at the end of the pane.
     *
     * @param text the header's text
     * @return this view
     */
    NavigationView addHeader(Text text);

    /**
     * Does the same as {@link #addHeader(Text)} with literal text.
     *
     * @param text the header's text
     * @return this view
     */
    default NavigationView addHeader(final String text) {
        return this.addHeader(Text.of(text));
    }

    /**
     * Adds a separator at the end of the pane.
     *
     * @return this view
     */
    NavigationView addSeparator();

    /**
     * Returns the entries at the foot of the pane, below the scrolling items and above the pane footer.
     *
     * @return a copy, in order
     */
    List<NavigationEntry> footerItems();

    /**
     * Adds an entry at the end of the footer, such as Settings or Sign out. Footer entries do not scroll.
     *
     * @param entry the entry
     * @return this view
     * @throws IllegalArgumentException if the entry belongs to a view or an item already, or was not made by Strata
     */
    NavigationView addFooterItem(NavigationEntry entry);

    /**
     * Removes an entry from the footer, as {@link #removeItem} does. Does nothing if the footer does not hold it.
     *
     * @param entry the entry
     * @return this view
     */
    NavigationView removeFooterItem(NavigationEntry entry);

    /**
     * Returns every item of the view in the order selection indexes count them: the items above the footer depth
     * first, the children of collapsed items included, then the footer items in the same way. Headers and separators
     * are left out.
     *
     * @return a copy, in order
     */
    List<NavigationItem> allItems();

    /**
     * Returns the selected item.
     *
     * @return the item at {@link #selectedIndex()} in {@link #allItems()}, or empty when none is selected
     */
    @Override
    Optional<NavigationItem> selected();

    /**
     * Returns the display mode set.
     *
     * @return the mode; {@link DisplayMode#AUTO} by default
     */
    DisplayMode displayMode();

    /**
     * Sets how the pane shows. A mode other than {@code AUTO} shows at once; {@code AUTO} is worked out at the next
     * layout. A change of the shown mode closes the pane over the content and the menu of children.
     *
     * @param mode the mode
     * @return this view
     */
    NavigationView setDisplayMode(DisplayMode mode);

    /**
     * Returns the mode the pane shows in: the display mode set, or what {@link DisplayMode#AUTO} chose from the
     * view's width when the view was last arranged.
     *
     * @return {@code EXPANDED}, {@code COMPACT} or {@code MINIMAL}, never {@code AUTO}; {@code EXPANDED} for a view
     *     set to {@code AUTO} that was never arranged
     */
    DisplayMode shownMode();

    /**
     * Returns the width below which {@code AUTO} shows {@code COMPACT}.
     *
     * @return the width in UI units; {@link #DEFAULT_COMPACT_BELOW} by default
     */
    float compactBelow();

    /**
     * Returns the width below which {@code AUTO} shows {@code MINIMAL}.
     *
     * @return the width in UI units; {@link #DEFAULT_MINIMAL_BELOW} by default
     */
    float minimalBelow();

    /**
     * Sets the widths at which {@code AUTO} changes mode. The view lays out again.
     *
     * @param compactBelow the width below which the pane shows {@code COMPACT}
     * @param minimalBelow the width below which it shows {@code MINIMAL}
     * @return this view
     * @throws IllegalArgumentException if a width is negative or not finite, or {@code minimalBelow} is greater than
     *     {@code compactBelow}
     */
    NavigationView setAutoWidths(float compactBelow, float minimalBelow);

    /**
     * Returns whether the pane is open over the content.
     *
     * @return true while it shows over the content in {@code COMPACT} or {@code MINIMAL}
     */
    boolean isExpanded();

    /**
     * Opens or closes the pane over the content and reports {@link #EXPANDED} if that changed. Never asks
     * {@link #EXPAND}. Opening does nothing in {@code EXPANDED}, or while the view is disabled, hidden or not shown on
     * a screen.
     *
     * @param expanded true to open the pane over the content
     * @return this view
     */
    NavigationView setExpanded(boolean expanded);

    /**
     * Returns the width of the pane, beside the content in {@code EXPANDED} and over it in the other modes.
     *
     * @return the width in UI units; {@link #DEFAULT_PANE_WIDTH} by default
     */
    float paneWidth();

    /**
     * Sets the width of the pane. The view lays out again.
     *
     * @param width the width in UI units
     * @return this view
     * @throws IllegalArgumentException if the width is negative or not finite
     */
    NavigationView setPaneWidth(float width);

    /**
     * Returns whether the toggle shows.
     *
     * @return true by default
     */
    boolean isToggleShown();

    /**
     * Sets whether the toggle shows where the pane can be opened over the content. Without it, only code and, in
     * {@code MINIMAL}, Enter and Space open the pane.
     *
     * @param shown false to leave the toggle out
     * @return this view
     */
    NavigationView setToggleShown(boolean shown);

    /**
     * Returns the pane header.
     *
     * @return the component below the title, or empty when there is none
     */
    Optional<Component<?>> paneHeader();

    /**
     * Shows a component below the title, across the pane, such as a search box. It is content, so its control
     * events go on past the view, and it is out of the tree while the pane shows as a column of icons.
     *
     * @param header the component, or {@code null} to remove it
     * @return this view
     * @throws IllegalArgumentException if the component is already in a tree or was made by another factory
     */
    NavigationView setPaneHeader(Component<?> header);

    /**
     * Returns the pane footer.
     *
     * @return the component at the foot of the pane, or empty when there is none
     */
    Optional<Component<?>> paneFooter();

    /**
     * Shows a component at the foot of the pane, below the footer items, such as the user's
     * {@link org.inspirenxe.strata.api.control.display.Avatar}. It is content, and it is out of the tree while the
     * pane shows as a column of icons.
     *
     * @param footer the component, or {@code null} to remove it
     * @return this view
     * @throws IllegalArgumentException if the component is already in a tree or was made by another factory
     */
    NavigationView setPaneFooter(Component<?> footer);

    /**
     * Returns the content shown while the selected item has no page.
     *
     * @return the content, or empty when there is none
     */
    Optional<Component<?>> content();

    /**
     * Sets what the view shows beside the pane while the selected item has no page or nothing is selected. It is
     * content, so its control events go on past the view.
     *
     * @param content the component, or {@code null} for none
     * @return this view
     * @throws IllegalArgumentException if the component is already in a tree or was made by another factory
     */
    NavigationView setContent(Component<?> content);

    /**
     * Sets whether a single click activates an item. With it on, a click asks {@link Activatable#ACTIVATE} before it
     * selects or expands; with it off, a single click selects or expands without activating, and a double click
     * activates. Enter and Space always activate.
     *
     * @param onClick false to activate on double clicks only; true by default
     * @return this view
     */
    @Override
    NavigationView setActivateOnClick(boolean onClick);

    /**
     * How the pane of a {@link NavigationView} shows.
     */
    enum DisplayMode {
        /** Pick one of the others from the view's width. */
        AUTO,
        /** The pane open beside the content. */
        EXPANDED,
        /** A column of icons beside the content, which the toggle opens over it. */
        COMPACT,
        /** A bar with the toggle and the title across the top, which opens the pane over the content. */
        MINIMAL
    }

    /**
     * An item and whether its children show, as {@link #EXPAND_ITEM} asks and {@link #ITEM_EXPANDED} reports.
     *
     * @param item the item; never null
     * @param expanded whether its children show
     */
    record Expansion(NavigationItem item, boolean expanded) {

        /**
         * Checks the item.
         */
        public Expansion {
            Objects.requireNonNull(item, "item");
        }
    }

    /**
     * A navigation view's own state in a {@link org.inspirenxe.strata.api.component.ComponentSnapshot}.
     *
     * @param selectedIndex the {@linkplain #selectedIndex() selected index}, or -1
     * @param shownMode the {@linkplain #shownMode() shown mode}; never null
     * @param expanded whether the pane {@linkplain #isExpanded() was open over the content}
     */
    record State(int selectedIndex, DisplayMode shownMode, boolean expanded) {

        /**
         * Checks the shown mode.
         */
        public State {
            Objects.requireNonNull(shownMode, "shownMode");
        }
    }
}
