package org.inspirenxe.strata.api.widget;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.layout.GridLength;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Rows of items under a header of columns, such as a list of sale offers with a name, a price and a seller.
 *
 * <p>The body is a {@link ListView}: only the rows in view are built, it scrolls the same way, and it selects and
 * activates rows the same way. Each row lays out one cell per column, made by that column's cell factory. The header
 * sits above the body, {@link ListView#DEFAULT_ROW_HEIGHT} units tall, and lines its columns up with the cells even
 * when the scroll bar shows.</p>
 *
 * <p>Column widths work like a {@link org.inspirenxe.strata.api.layout.GridPanel}'s: fixed widths first, then
 * {@link GridLength#auto() auto} columns at their title's width plus 8 units, and 11 more for the arrow when
 * sortable, and weighted columns share what is left by weight. Titles sit 4 units in from the left of their column,
 * and a sort arrow 4 units in from the right.</p>
 *
 * <p>A column with a comparator is sortable. Clicking its title sorts the rows by it, ascending first and then
 * flipping on each click, and the title shows an arrow. Text columns sort by their text, ignoring case, unless
 * given another comparator. Sorting changes only the order the rows are shown in: {@link #items()} keeps the order
 * they were set in, while {@link #shownItems()} and every row index follow the order on screen.</p>
 *
 * <h2>Theme keys</h2>
 * <p>The body reads the {@link ListView} keys. The header reads these.</p>
 * <ul>
 *     <li>{@value #STYLE_HEADER} (paint, required): behind each column title.</li>
 *     <li>{@value #STYLE_HEADER_HOVERED} (paint, optional): behind a sortable title under the pointer. Falls back
 *     to {@value #STYLE_HEADER}.</li>
 *     <li>{@value #STYLE_HEADER_TEXT} (text style, optional): the titles and sort arrows. Falls back to the default
 *     text style.</li>
 *     <li>{@value #STYLE_GRID} (paint, optional): a one-unit line between columns, in the header and the body.
 *     Nothing if missing.</li>
 * </ul>
 *
 * <pre>{@code
 * TableView<Offer> offers = factory.<Offer>tableView()
 *         .addTextColumn("Item", GridLength.weighted(1), Offer::name)
 *         .addTextColumn("Price", GridLength.fixed(60), offer -> format(offer.price()))
 *         .setColumnComparator(1, Comparator.comparingDouble(Offer::price))
 *         .setItems(market.offers());
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#tableView()}. Do not implement this
 * interface.</p>
 *
 * @param <T> the type of the items
 */
public interface TableView<T> extends Component<TableView<T>> {
    /**
     * Style key for the header behind each title (paint, required).
     */
    String STYLE_HEADER = "table.header";
    /**
     * Style key for a sortable header title under the pointer (paint, optional).
     */
    String STYLE_HEADER_HOVERED = "table.header.hovered";
    /**
     * Style key for the header titles (text style, optional).
     */
    String STYLE_HEADER_TEXT = "table.header.text";
    /**
     * Style key for the lines between columns (paint, optional).
     */
    String STYLE_GRID = "table.grid";

    /**
     * Adds a column whose cells are components. The rows are built again.
     *
     * @param title the header title
     * @param width how wide the column is
     * @param cellFactory makes the cell for an item; called when the row comes into view
     * @return this table
     */
    TableView<T> addColumn(String title, GridLength width, Function<? super T, ? extends Component<?>> cellFactory);

    /**
     * Adds a column of text, sortable by that text ignoring case. Each cell is a one-line {@link Label} 4 units in
     * from the column's edges, cut short with {@code ...} when too long. The rows are built again.
     *
     * @param title the header title
     * @param width how wide the column is
     * @param text returns the text for an item
     * @return this table
     */
    TableView<T> addTextColumn(String title, GridLength width, Function<? super T, String> text);

    /**
     * Returns how many columns there are.
     *
     * @return the number of columns
     */
    int columnCount();

    /**
     * Returns a column's title.
     *
     * @param column the column index
     * @return the title
     * @throws IndexOutOfBoundsException if there is no such column
     */
    String columnTitle(int column);

    /**
     * Makes a column sortable by a comparator, or not sortable. If the rows are sorted by that column they are sorted
     * again, or shown in their set order when the comparator is null.
     *
     * @param column the column index
     * @param comparator orders the items ascending, or null to make the column not sortable
     * @return this table
     * @throws IndexOutOfBoundsException if there is no such column
     */
    TableView<T> setColumnComparator(int column, Comparator<? super T> comparator);

    /**
     * Sorts the rows by a column, or shows them in their set order. The selection stays on the same item.
     *
     * @param column the column index, or -1 for the set order
     * @param ascending true for ascending, false for descending; ignored for -1
     * @return this table
     * @throws IndexOutOfBoundsException if {@code column} is neither -1 nor a column index
     * @throws IllegalArgumentException if the column is not sortable
     */
    TableView<T> sortBy(int column, boolean ascending);

    /**
     * Returns the column the rows are sorted by.
     *
     * @return the column index, or -1 when the rows are in their set order
     */
    int sortColumn();

    /**
     * Returns whether the sort is ascending.
     *
     * @return true for ascending; also true when the rows are not sorted
     */
    boolean isSortAscending();

    /**
     * Returns the items in the order they were set.
     *
     * @return an unmodifiable list of the items
     */
    List<T> items();

    /**
     * Returns the items in the order they are shown.
     *
     * @return an unmodifiable list of the items, sorted if a sort column is set
     */
    List<T> shownItems();

    /**
     * Replaces the items, sorting them by the sort column. The selection stays on an item with the same key, as
     * {@link ListView#setItems} does.
     *
     * @param items the new items, copied
     * @return this table
     */
    TableView<T> setItems(List<? extends T> items);

    /**
     * Builds every row again, for items whose content changed.
     *
     * @return this table
     */
    TableView<T> refresh();

    /**
     * Sets what identifies an item across {@link #setItems} calls, as {@link ListView#setKeyFunction} does.
     *
     * @param keyFunction returns an item's key
     * @return this table
     */
    TableView<T> setKeyFunction(Function<? super T, ?> keyFunction);

    /**
     * Sets the height of every body row.
     *
     * @param height the row height in UI units
     * @return this table
     * @throws IllegalArgumentException if {@code height} is not greater than 0 or is infinite
     */
    TableView<T> setRowHeight(float height);

    /**
     * Sets the text shown in the body while there are no items.
     *
     * @param placeholder the text, or null for none
     * @return this table
     */
    TableView<T> setPlaceholder(String placeholder);

    /**
     * Returns the index of the selected row in the shown order.
     *
     * @return the index, or -1 when nothing is selected
     */
    int selectedIndex();

    /**
     * Returns the selected item.
     *
     * @return the selected item, or empty when nothing is selected
     */
    Optional<T> selected();

    /**
     * Selects the row at an index in the shown order and scrolls it into view, as {@link ListView#select} does.
     *
     * @param index the index, or -1 to select nothing
     * @return this table
     * @throws IndexOutOfBoundsException if {@code index} is neither -1 nor a row index
     */
    TableView<T> select(int index);

    /**
     * Selects the row of an item with the same key, or nothing if there is none.
     *
     * @param item the item
     * @return this table
     */
    TableView<T> selectItem(T item);

    /**
     * Adds a listener told the selected item after every change.
     *
     * @param listener receives the selected item, or empty when nothing is selected
     * @return this table
     */
    TableView<T> onSelectionChanged(Consumer<Optional<T>> listener);

    /**
     * Adds a listener told when a row is activated, by a double click or Enter, as {@link ListView#onActivate}
     * describes.
     *
     * @param listener receives the activated item
     * @return this table
     */
    TableView<T> onActivate(Consumer<T> listener);
}
