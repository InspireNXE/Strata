package org.inspirenxe.strata.api.control.pane;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.Tooltip;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.ValueEvent;
import org.inspirenxe.strata.api.event.ValueRequestEvent;

/**
 * A row of tabs, one of them selected. The bar only picks a tab; show the matching content yourself when
 * {@link #SELECTED} fires.
 *
 * <p>Tabs share the bar's width equally, with {@linkplain #setSpacing the spacing} between them, and are one line
 * plus 7 units tall. A click on an enabled tab selects it. While the bar has focus, Left and Right select the
 * previous and next enabled tab. Each tab can have its own tooltip, such as one that says why it is disabled. The
 * first tab added is selected; a bar with no tabs has none.</p>
 *
 * <p>The bar is focusable and tabbable, and each tab shows the hand cursor while enabled.</p>
 *
 * <h2>Theme keys</h2>
 * <ul>
 *     <li>{@value #STYLE_NORMAL} (paint, required): an unselected tab.</li>
 *     <li>{@value #STYLE_SELECTED} (paint, required): the selected tab.</li>
 *     <li>{@value #STYLE_HOVERED} (paint, optional): an unselected tab under the pointer. Falls back to
 *     {@value #STYLE_NORMAL}.</li>
 *     <li>{@value #STYLE_DISABLED} (paint, optional): a disabled tab. Falls back to {@value #STYLE_NORMAL}.</li>
 *     <li>{@value #STYLE_TEXT} (text style, optional): an unselected tab's text. Falls back to the default
 *     style.</li>
 *     <li>{@value #STYLE_TEXT_SELECTED} (text style, optional): the selected tab's text. Falls back to
 *     {@value #STYLE_TEXT}.</li>
 *     <li>{@value #STYLE_TEXT_DISABLED} (text style, optional): a disabled tab's text. Falls back to
 *     {@value #STYLE_TEXT}.</li>
 * </ul>
 *
 * <pre>{@code
 * TabBar sides = factory.tabBar().addTab("Buy").addTab("Sell")
 *         .onEvent(TabBar.SELECTED, event -> showSide(event.value() == 0 ? Side.BUY : Side.SELL));
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#tabBar()}. Do not implement this
 * interface.</p>
 */
public interface TabBar extends Component<TabBar> {
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
     * Fired when the user selects a tab, by a left click on an enabled tab or by Left or Right, which move to the
     * nearest enabled tab in that direction, before the selection changes. The value is the index of the tab about
     * to be selected. Not fired for a click on the selected tab. Canceling it keeps the current tab. A handler may
     * set another index; an index out of range, a disabled tab or null is ignored. Fires only for the user's
     * actions.
     */
    EventType<ValueRequestEvent<TabBar, Integer>> SELECT = EventType.valueRequest("tab_bar.select");
    /**
     * Fired after another tab was selected. The value is the new index and the previous value the old one. The
     * first tab becoming selected when it is added fires nothing. Fires for changes made from code too.
     */
    EventType<ValueEvent<TabBar, Integer>> SELECTED = EventType.valueReport("tab_bar.selected");

    /**
     * Adds a tab at the end. The first tab added is selected.
     *
     * @param text the tab's text
     * @return this bar
     */
    TabBar addTab(String text);

    /**
     * Returns how many tabs there are.
     *
     * @return the number of tabs
     */
    int tabCount();

    /**
     * Returns a tab's text.
     *
     * @param index the tab's index
     * @return the text
     * @throws IndexOutOfBoundsException if there is no such tab
     */
    String tabText(int index);

    /**
     * Replaces a tab's text.
     *
     * @param index the tab's index
     * @param text the text
     * @return this bar
     * @throws IndexOutOfBoundsException if there is no such tab
     */
    TabBar setTabText(int index, String text);

    /**
     * Returns whether a tab can be selected.
     *
     * @param index the tab's index
     * @return true by default
     * @throws IndexOutOfBoundsException if there is no such tab
     */
    boolean isTabEnabled(int index);

    /**
     * Sets whether a tab can be selected by input. Disabling the selected tab keeps it selected.
     *
     * @param index the tab's index
     * @param enabled false to keep clicks and keys from selecting it
     * @return this bar
     * @throws IndexOutOfBoundsException if there is no such tab
     */
    TabBar setTabEnabled(int index, boolean enabled);

    /**
     * Sets a tab's tooltip.
     *
     * @param index the tab's index
     * @param tooltip the tooltip, or {@code null} for none
     * @return this bar
     * @throws IndexOutOfBoundsException if there is no such tab
     */
    TabBar setTabTooltip(int index, Tooltip tooltip);

    /**
     * Returns the selected tab.
     *
     * @return the selected index, or -1 when there are no tabs
     */
    int selectedIndex();

    /**
     * Selects a tab, enabled or not. Fires {@link #SELECTED} if the selection changed.
     *
     * @param index the tab's index
     * @return this bar
     * @throws IndexOutOfBoundsException if there is no such tab
     */
    TabBar select(int index);

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
