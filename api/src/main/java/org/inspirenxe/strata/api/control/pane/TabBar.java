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

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.Focusable;
import org.inspirenxe.strata.api.component.HasCursor;
import org.inspirenxe.strata.api.component.HasTooltip;
import org.inspirenxe.strata.api.control.attribute.item.SingleSelection;
import org.inspirenxe.strata.api.text.Text;

import java.util.List;
import java.util.Optional;

/**
 * A row of tabs, one of them selected. The bar only picks a tab; show the matching content yourself when
 * {@link SingleSelection#SELECTED} fires. Each tab is a {@link Tab}, which holds its text, whether it is enabled and
 * its tooltip.
 *
 * <p>Tabs share the bar's width equally, with {@linkplain #setSpacing the spacing} between them, and are one line
 * plus 7 units tall. A click on an enabled tab selects it. While the bar has focus, Left and Right select the
 * previous and next enabled tab. Each tab can have its own tooltip, such as one that says why it is disabled. The
 * first tab added is selected; a bar with no tabs has none.</p>
 *
 * <p>The bar is focusable and tabbable, and each tab shows the hand cursor while enabled.</p>
 *
 * <h2>Events</h2>
 * <ul>
 *     <li>{@link SingleSelection#SELECT}: Fired when the user selects a tab, by a left click on an enabled tab or by
 *     Left or Right, which move to the nearest enabled tab in that direction, before the selection changes. The value
 *     is the index of the tab about to be selected. Not fired for a click on the selected tab. Canceling it keeps the
 *     current tab. A handler may set another index; an index out of range, a disabled tab or null is ignored. Fires
 *     only for the user's actions.</li>
 *     <li>{@link SingleSelection#SELECTED}: Fired after another tab was selected. The value is the new index and the
 *     previous value the old one. The first tab becoming selected when it is added fires nothing. Fires for changes
 *     made from code too.</li>
 * </ul>
 *
 * <h2>Theme keys</h2>
 * <ul>
 *     <li>{@value #STYLE_NORMAL} (paint, required): an unselected tab.</li>
 *     <li>{@value #STYLE_SELECTED} (paint, required): the selected tab.</li>
 *     <li>{@value #STYLE_HOVERED} (paint, optional): an unselected tab under the pointer. Falls back to
 *     {@value #STYLE_NORMAL}.</li>
 *     <li>{@value #STYLE_DISABLED} (paint, optional): a disabled tab, and every unselected tab of a disabled bar.
 *     Falls back to {@value #STYLE_NORMAL}.</li>
 *     <li>{@value #STYLE_SELECTED_DISABLED} (paint, optional): the selected tab when it, or the whole bar, is
 *     disabled, so the selection stays visible in the disabled look. Falls back to {@value #STYLE_DISABLED}.</li>
 *     <li>{@value #STYLE_TEXT} (text style, optional): an unselected tab's text. Falls back to the default
 *     style.</li>
 *     <li>{@value #STYLE_TEXT_SELECTED} (text style, optional): the selected tab's text. Falls back to
 *     {@value #STYLE_TEXT}.</li>
 *     <li>{@value #STYLE_TEXT_DISABLED} (text style, optional): a disabled tab's text. Falls back to
 *     {@value #STYLE_TEXT}.</li>
 * </ul>
 *
 * <pre>{@code
 * TabBar sides = factory.tabBar()
 *         .addTab("Buy")
 *         .addTab("Sell")
 *         .onEvent(SingleSelection.SELECTED, event -> showSide(event.value() == 0 ? Side.BUY : Side.SELL));
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#tabBar()}. Do not implement this
 * interface.</p>
 */
public interface TabBar extends Component<TabBar>, Focusable<TabBar>, HasCursor<TabBar>, HasTooltip<TabBar>,
        SingleSelection<Tab, TabBar> {
    /**
     * Style key for an unselected tab (paint, required).
     */
    String STYLE_NORMAL = "tab.normal";
    /**
     * Style key for the selected tab (paint, required).
     */
    String STYLE_SELECTED = "tab.selected";
    /**
     * Style key for an unselected tab under the pointer (paint, optional).
     */
    String STYLE_HOVERED = "tab.hovered";
    /**
     * Style key for a disabled tab (paint, optional).
     */
    String STYLE_DISABLED = "tab.disabled";
    /**
     * Style key for the selected tab when it or the bar is disabled (paint, optional).
     */
    String STYLE_SELECTED_DISABLED = "tab.selected.disabled";
    /**
     * Style key for an unselected tab's text (text style, optional).
     */
    String STYLE_TEXT = "tab.text";
    /**
     * Style key for the selected tab's text (text style, optional).
     */
    String STYLE_TEXT_SELECTED = "tab.text.selected";
    /**
     * Style key for a disabled tab's text (text style, optional).
     */
    String STYLE_TEXT_DISABLED = "tab.text.disabled";

    /**
     * Returns the tabs, left to right.
     *
     * @return an unmodifiable copy of the tabs
     */
    List<Tab> tabs();

    /**
     * Adds a tab at the end. The first tab added to an empty bar is selected, without an event.
     *
     * @param tab the tab
     * @return this bar
     * @throws IllegalArgumentException if the tab is already in a bar
     */
    TabBar addTab(Tab tab);

    /**
     * Adds a tab at an index, moving the tabs from there to the right. The selection stays on the same tab, and
     * {@link SingleSelection#SELECTED} fires if its index moved.
     *
     * @param index where the tab goes, from 0 to the number of tabs
     * @param tab the tab
     * @return this bar
     * @throws IndexOutOfBoundsException if the index is outside 0 to the number of tabs
     * @throws IllegalArgumentException if the tab is already in a bar
     */
    TabBar addTab(int index, Tab tab);

    /**
     * Removes a tab. Removing the selected tab selects the one that takes its place, or the last one, or none when
     * no tab is left. {@link SingleSelection#SELECTED} fires if the selected tab or its index changed. Does nothing
     * if the tab is not in this bar.
     *
     * @param tab the tab
     * @return this bar
     */
    TabBar removeTab(Tab tab);

    /**
     * Adds a tab with a text at the end, as {@code addTab(factory.tab(text))} would.
     *
     * @param text the tab's text
     * @return this bar
     */
    TabBar addTab(Text text);

    /**
     * Does the same as {@link #addTab(Text)} with literal text.
     *
     * @param text the tab's text
     * @return this bar
     */
    default TabBar addTab(final String text) {
        return this.addTab(Text.of(text));
    }

    /**
     * Returns the selected tab.
     *
     * @return the selected index, or -1 when there are no tabs or the selection was cleared
     */
    @Override
    int selectedIndex();

    /**
     * Returns the selected tab.
     *
     * @return the tab, or empty when no tab is selected
     */
    @Override
    Optional<Tab> selected();

    /**
     * Selects a tab, enabled or not, or none for -1. Fires {@link SingleSelection#SELECTED} if the selection changed.
     *
     * @param index the tab's index, or -1
     * @return this bar
     * @throws IndexOutOfBoundsException if the index is not -1 and there is no such tab
     */
    @Override
    TabBar select(int index);

    /**
     * Selects a tab, or none if the tab is not in this bar.
     *
     * @param tab the tab
     * @return this bar
     */
    @Override
    TabBar selectItem(Tab tab);

    /**
     * Returns the space between tabs.
     *
     * @return the spacing; 0 by default
     */
    float spacing();

    /**
     * Sets the space between tabs.
     *
     * @param spacing the space in UI units
     * @return this bar
     */
    TabBar setSpacing(float spacing);
}
