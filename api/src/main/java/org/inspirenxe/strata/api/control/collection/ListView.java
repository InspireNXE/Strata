package org.inspirenxe.strata.api.control.collection;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.ValueEvent;
import org.inspirenxe.strata.api.event.ValueRequestEvent;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * A scrolling list of items, each shown as a row built by a cell factory, with single selection.
 *
 * <h2>Rows</h2>
 * <p>Every row is {@linkplain #setRowHeight the row height} tall, with {@linkplain #setRowSpacing the row
 * spacing} between rows, and as wide as the list less its scroll bar gutter. The list builds a component only for
 * the rows in view, from the {@linkplain #setCellFactory cell factory}, and drops it when the row scrolls out, so a
 * list of thousands of items costs no more than a screenful. A cell is laid out in its row like a child of an
 * {@code AnchorPanel}. Keep nothing in a cell that must survive scrolling; build it from the item.</p>
 *
 * <p>The list draws each row's background under its cell: the selected face, the hovered face while the pointer is
 * over the row or anything in it, or the plain face.</p>
 *
 * <h2>Selection and input</h2>
 * <ul>
 *     <li>At most one item is selected. A press on a row selects it; a press on the selected row deselects it only
 *     when the list {@linkplain #setDeselectable is deselectable}. A cell that consumes the press, such as a button,
 *     keeps the row from being selected.</li>
 *     <li>A double click, or Enter while the list has focus, {@linkplain #ACTIVATE activates} the selected item.
 *     With {@link #setActivateOnClick} a single click does, as in a drop-down.</li>
 *     <li>While focused: Up and Down move the selection, Page Up and Page Down by a viewport, Home and End to the
 *     ends; the list scrolls to keep the selection in view.</li>
 *     <li>The wheel and the scroll bar scroll as in {@link org.inspirenxe.strata.api.layout.ScrollPanel}, and the
 *     list uses its theme keys for the bar.</li>
 * </ul>
 *
 * <p>{@link #setItems} keeps the selection on the item with the same {@linkplain #setKeyFunction key}, so a list
 * refreshed from the server keeps the row the user picked. If no item has that key, nothing is selected.</p>
 *
 * <p>The events carry row indexes, so read the item from the list: {@code list.items().get(e.value())}. Handle
 * {@link #SELECT} or {@link #ACTIVATE} to refuse or change what the user does, and {@link #SELECTED} or
 * {@link #ACTIVATED} to react.</p>
 *
 * <p>The list is focusable and tabbable. It asks for the width of its widest built row, at least 40 units, and the
 * height of all its rows, but no more height than it is offered; give it a height or stretch it to scroll.</p>
 *
 * <h2>Theme keys</h2>
 * <ul>
 *     <li>{@value #STYLE_ROW_SELECTED} (paint, required): the selected row.</li>
 *     <li>{@value #STYLE_ROW} (paint, optional): every other row. Nothing is drawn if it is missing.</li>
 *     <li>{@value #STYLE_ROW_HOVERED} (paint, optional): a row under the pointer. Falls back to
 *     {@value #STYLE_ROW}.</li>
 *     <li>{@value #STYLE_PLACEHOLDER} (text style, optional): the {@linkplain #setPlaceholder placeholder}. Falls
 *     back to the default style.</li>
 *     <li>The scroll bar keys of {@link org.inspirenxe.strata.api.layout.ScrollPanel}.</li>
 * </ul>
 *
 * <pre>{@code
 * ListView<Listing> listings = factory.listView((Listing listing) -> factory.label(listing.name()))
 *         .setRowHeight(24)
 *         .setKeyFunction(Listing::record);
 * listings.onEvent(ListView.ACTIVATED, e -> buy(listings.items().get(e.value()), 1));
 * listings.setItems(fromServer);
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#listView(Function)}. Do not implement this
 * interface.</p>
 *
 * @param <T> the type of the items
 */
public interface ListView<T> extends Component<ListView<T>> {
    /**
     * Style key for an unselected row (paint, optional).
     */
    String STYLE_ROW = "list.row";
    /**
     * Style key for an unselected row under the pointer (paint, optional).
     */
    String STYLE_ROW_HOVERED = "list.row.hovered";
    /**
     * Style key for the selected row (paint, required).
     */
    String STYLE_ROW_SELECTED = "list.row.selected";
    /**
     * Style key for the placeholder text (text style, optional).
     */
    String STYLE_PLACEHOLDER = "list.placeholder";
    /**
     * The row height of a new list, in UI units.
     */
    float DEFAULT_ROW_HEIGHT = 20f;
    /**
     * Asked before the user changes the selection: a press on a row, a press on the selected row of a
     * {@linkplain #setDeselectable deselectable} list, which clears it, or the Up, Down, Page Up, Page Down, Home and
     * End keys while the list has focus. It fires only when the selection would change. The value is the index of
     * the row about to be selected, or -1 when the user clears the selection. A handler may rewrite it to another
     * index or to -1; the list then selects that row and scrolls it into view, as {@link #select} does, and ignores
     * any other index outside the items. Fires only for the user's actions.
     */
    EventType<ValueRequestEvent<ListView<?>, Integer>> SELECT = EventType.valueRequest("list_view.select");
    /**
     * Told after the selection changed. The value is the selected index, or -1 when nothing is selected, and the
     * previous value is the index before. It also fires when {@link #setItems} keeps the selection at the same index
     * but on an item that is no longer equal to the one before, so the value can equal the previous value. Fires for
     * changes made from code too, including {@link #setItems}.
     */
    EventType<ValueEvent<ListView<?>, Integer>> SELECTED = EventType.valueReport("list_view.selected");
    /**
     * Asked before the user activates a row: a double press on a row, or Enter while the list has focus and a row is
     * selected. With {@link #setActivateOnClick} a click on a row activates it and a double press does not. The value
     * is the row's index. A handler may rewrite it to another row; an index outside the items is ignored. Fires only
     * for the user's actions.
     */
    EventType<ValueRequestEvent<ListView<?>, Integer>> ACTIVATE = EventType.valueRequest("list_view.activate");
    /**
     * Told after the user activated a row. The value is the row's index, and the previous value is the same index.
     * The list has no method that activates a row from code.
     */
    EventType<ValueEvent<ListView<?>, Integer>> ACTIVATED = EventType.valueReport("list_view.activated");

    /**
     * Returns the items.
     *
     * @return an unmodifiable copy of the items
     */
    List<T> items();

    /**
     * Replaces the items and rebuilds the rows in view. The selection moves to the item with the selected item's
     * key, or clears; {@link #SELECTED} fires if the selected item or its index changed. The scroll offset is kept,
     * clamped to the new length.
     *
     * @param items the items; the list keeps a copy
     * @return this list
     */
    ListView<T> setItems(List<? extends T> items);

    /**
     * Rebuilds the rows in view from the same items, for when the items changed inside.
     *
     * @return this list
     */
    ListView<T> refresh();

    /**
     * Sets what builds the component for an item's row, and rebuilds the rows in view.
     *
     * @param cellFactory makes a new component for an item each time its row comes into view
     * @return this list
     */
    ListView<T> setCellFactory(Function<? super T, ? extends Component<?>> cellFactory);

    /**
     * Sets how items are told apart when {@link #setItems} keeps the selection. By default an item is its own key,
     * compared with {@code equals}.
     *
     * @param keyFunction returns the key of an item; keys are compared with {@code equals}
     * @return this list
     */
    ListView<T> setKeyFunction(Function<? super T, ?> keyFunction);

    /**
     * Returns the row height.
     *
     * @return the height of every row; {@value #DEFAULT_ROW_HEIGHT} by default
     */
    float rowHeight();

    /**
     * Sets the height of every row.
     *
     * @param height the height in UI units
     * @return this list
     * @throws IllegalArgumentException if the height is not greater than 0 or not finite
     */
    ListView<T> setRowHeight(float height);

    /**
     * Returns the row spacing.
     *
     * @return the space between rows; 0 by default
     */
    float rowSpacing();

    /**
     * Sets the space between rows. No background is drawn in it.
     *
     * @param spacing the space in UI units
     * @return this list
     * @throws IllegalArgumentException if the spacing is negative or not finite
     */
    ListView<T> setRowSpacing(float spacing);

    /**
     * Returns the selected index.
     *
     * @return the index of the selected item, or -1 when nothing is selected
     */
    int selectedIndex();

    /**
     * Returns the selected item.
     *
     * @return the selected item, or empty when nothing is selected
     */
    Optional<T> selected();

    /**
     * Selects the item at an index and scrolls it into view.
     *
     * @param index the index, or -1 to clear the selection
     * @return this list
     * @throws IndexOutOfBoundsException if the index is not -1 and not an item's index
     */
    ListView<T> select(int index);

    /**
     * Selects the first item with the same key as the given one, or clears the selection if there is none.
     *
     * @param item the item to find by its key
     * @return this list
     */
    ListView<T> selectItem(T item);

    /**
     * Returns whether a press on the selected row deselects it.
     *
     * @return false by default
     */
    boolean isDeselectable();

    /**
     * Sets whether a press on the selected row deselects it.
     *
     * @param deselectable true to allow deselecting by pressing
     * @return this list
     */
    ListView<T> setDeselectable(boolean deselectable);

    /**
     * Sets whether a single click activates a row, instead of a double click.
     *
     * @param onClick true to activate on every click
     * @return this list
     */
    ListView<T> setActivateOnClick(boolean onClick);

    /**
     * Sets the text shown in the middle of the list while it has no items.
     *
     * @param placeholder the text, or {@code null} for none
     * @return this list
     */
    ListView<T> setPlaceholder(String placeholder);

    /**
     * Returns the vertical offset.
     *
     * @return how far the rows are scrolled down, in UI units
     */
    float scrollY();

    /**
     * Scrolls to an offset, clamped to the rows.
     *
     * @param scrollY the vertical offset
     * @return this list
     */
    ListView<T> setScrollY(float scrollY);

    /**
     * Scrolls the least distance that shows a row.
     *
     * @param index the row's index
     * @return this list
     * @throws IndexOutOfBoundsException if the index is not an item's index
     */
    ListView<T> scrollToIndex(int index);
}
