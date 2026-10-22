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
package org.inspirenxe.strata.api.control.collection;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.Focusable;
import org.inspirenxe.strata.api.component.HasCursor;
import org.inspirenxe.strata.api.component.HasTooltip;
import org.inspirenxe.strata.api.component.Tooltip;
import org.inspirenxe.strata.api.control.attribute.item.Activatable;
import org.inspirenxe.strata.api.control.attribute.item.CellEditing;
import org.inspirenxe.strata.api.control.attribute.item.CellSource;
import org.inspirenxe.strata.api.control.attribute.item.ItemSource;
import org.inspirenxe.strata.api.control.attribute.item.MultiSelection;
import org.inspirenxe.strata.api.control.attribute.item.Selection;
import org.inspirenxe.strata.api.control.attribute.item.SelectionMode;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.control.ValueEvent;
import org.inspirenxe.strata.api.event.control.ValueRequestEvent;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * A grid of equal square cells over a list of items, such as an icon picker, an image gallery or a palette.
 *
 * <p>There is one cell per entry in the items, null entries being empty cells, laid out left to right in rows of
 * {@linkplain #setColumns a set number of columns}. Each filled cell shows the component its cell factory makes for
 * the item, inset by the theme's {@value #STYLE_CELL_PADDING}, one unit by default, and the tooltip its tooltip
 * function gives. Unlike a list, the grid is positional: {@link #setItems} keeps the selected cells that still
 * exist, and {@link #setKeyFunction} only affects {@link #selectItem} and {@link #selectItems}, which find filled
 * cells only. {@code selectItem(null)} clears the selection; select an empty cell by its index.</p>
 *
 * <h2>Rows in view</h2>
 * <p>The grid asks for the height of all its rows, but no more than it is offered. Given less, by a fixed or maximum
 * height or by stretching, it builds cells only for the rows in view, as a {@link ListView} does, and drops a row's
 * cells when it scrolls out, so a grid of thousands of pictures costs no more than a screenful. It then scrolls with
 * the wheel and a scroll bar of its own, which reads the scroll bar keys of
 * {@link org.inspirenxe.strata.api.layout.ScrollPanel}, and {@link #scrollY}, {@link #setScrollY} and
 * {@link #scrollToIndex} work as on a list. Cells are built again when the items, the cell factory or the tooltip
 * function change. Keep nothing in a cell that must survive scrolling.</p>
 *
 * <h2>Selection and input</h2>
 * <ul>
 *     <li>Clicking a cell with any button fires {@link #CLICK} and then {@link #CLICKED}, empty cells included, and
 *     consumes the click. A {@linkplain #setSelectable selectable} grid also selects the cell clicked with the left
 *     button first. In {@link SelectionMode#MULTIPLE} mode Control and a click adds or removes a cell, Shift and a
 *     click selects the range from the anchor in cell order, and Control+A selects every cell, as in a list. A double
 *     click with the left button activates the cell, or a single click with {@link #setActivateOnClick}.</li>
 *     <li>The grid is focusable and tabbable, and has a cursor cell while focused, ringed with
 *     {@value #STYLE_FOCUS}. It starts on the selected cell, or the first. The arrows move it cell by cell, Left and
 *     Right going on across the ends of rows; Home and End move it to the ends of its row, and with Control to the
 *     first and last cell; Page Up and Page Down move it by the rows in view. The grid scrolls to keep it in view.
 *     Enter and Space ask {@link Activatable#ACTIVATE} for it. While the grid is selectable the cursor selects as it
 *     moves, and Shift extends the range in {@link SelectionMode#MULTIPLE} mode. Held keys repeat, except Enter and
 *     Space. Keys with Alt are left alone, and so are keys with Control other than Home, End and Control+A.</li>
 * </ul>
 *
 * <p>With a {@linkplain #setCellEditor cell editor}, filled cells are edited in place, as {@link CellEditing}
 * describes: F2 or a typed character edits the cursor cell, and a double click edits the cell clicked and no longer
 * activates it. Empty cells are not edited.</p>
 *
 * <p>The desired size is the columns and rows of cells with the spacing between them, and the scroll bar's gutter
 * while the rows overflow the height offered.</p>
 *
 * <h2>Events</h2>
 * <ul>
 *     <li>{@link Selection#SELECT}: Asked before the user changes the selected cell in {@link SelectionMode#SINGLE}
 *     mode: a left click on a cell other than the selected one, or a move of the cursor, when the grid is selectable.
 *     The value is the cell about to be selected. A handler may rewrite it to another cell or to -1, which clears the
 *     selection; any other index outside the cells is ignored. Fires only for the user's actions.</li>
 *     <li>{@link MultiSelection#SELECT_MANY}: Asked instead of {@code SELECT} in {@link SelectionMode#MULTIPLE}
 *     mode, with the cells about to be selected, as in a {@link ListView}.</li>
 *     <li>{@link Selection#SELECTED} and {@link MultiSelection#SELECTED_MANY}: Told after the current cell or the
 *     selected cells changed, from the user or code, including {@link #setItems} dropping cells that no longer
 *     exist.</li>
 *     <li>{@link Activatable#ACTIVATE}: Asked before the user activates a cell, empty cells included: after the
 *     click's {@link #CLICK}, or for Enter or Space on the cursor cell. The value is the cell. A handler may rewrite
 *     it to another cell; an index outside the cells is ignored. Fires only for the user's actions.</li>
 *     <li>{@link Activatable#ACTIVATED}: Told after the user activated a cell. The value is the cell, and the previous
 *     value is the same cell.</li>
 * </ul>
 *
 * <h2>Theme keys</h2>
 * <ul>
 *     <li>{@value #STYLE_CELL} (paint, required): each cell, behind its content.</li>
 *     <li>{@value #STYLE_CELL_HOVERED} (paint, optional): drawn over the cell under the pointer, in front of its
 *     content. Nothing if missing.</li>
 *     <li>{@value #STYLE_CELL_SELECTED} (paint, optional): drawn over each selected cell, in front of its content.
 *     Falls back to {@value #STYLE_CELL_HOVERED}.</li>
 *     <li>{@value #STYLE_FOCUS} (paint, optional): drawn over the cursor cell while the grid has focus. Falls back to
 *     {@value org.inspirenxe.strata.api.control.button.Button#STYLE_FOCUS}.</li>
 *     <li>{@value #STYLE_CELL_PADDING} (insets, optional): the space between a cell's edges and the component its
 *     cell factory made. Defaults to 1 on every side.</li>
 *     <li>The scroll bar keys of {@link org.inspirenxe.strata.api.layout.ScrollPanel}.</li>
 * </ul>
 *
 * <pre>{@code
 * GridView<Picture> gallery = factory.<Picture>gridView(picture -> factory.image(picture.thumbnail()))
 *         .setColumns(6)
 *         .setCellSize(48)
 *         .setTooltips(picture -> Tooltip.text(picture.title()))
 *         .setItems(album.pictures())
 *         .onEvent(Activatable.ACTIVATED, e -> this.open(e.value()));
 * gallery.setHeight(200);
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#gridView}. Do not implement this
 * interface.</p>
 *
 * @param <T> the type of the items
 */
public interface GridView<T> extends Component<GridView<T>>, Focusable<GridView<T>>, HasCursor<GridView<T>>,
        HasTooltip<GridView<T>>, ItemSource<T, GridView<T>>, CellSource<T, GridView<T>>,
        MultiSelection<T, GridView<T>>, Activatable<GridView<T>>, CellEditing<T, GridView<T>> {
    /**
     * Style key drawn over the cursor cell while the grid has focus (paint, optional). Falls back to
     * {@value org.inspirenxe.strata.api.control.button.Button#STYLE_FOCUS}.
     */
    String STYLE_FOCUS = "grid.focus";
    /**
     * Style key for each cell (paint, required).
     */
    String STYLE_CELL = "grid.cell";
    /**
     * Style key drawn over the cell under the pointer (paint, optional).
     */
    String STYLE_CELL_HOVERED = "grid.cell.hovered";
    /**
     * Style key drawn under the content of the cell under the pointer, over the cell's own paint (paint, optional).
     * Defaults to none.
     */
    String STYLE_CELL_HOVERED_BACK = "grid.cell.hovered.back";
    /**
     * Style key for where the hovered and selected paints are drawn, as insets from the cell's edges (insets,
     * optional). Defaults to none, which covers the whole cell.
     */
    String STYLE_CELL_HIGHLIGHT = "grid.cell.highlight";
    /**
     * Style key drawn over the selected cell (paint, optional).
     */
    String STYLE_CELL_SELECTED = "grid.cell.selected";
    /**
     * Style key for the space between a cell's edges and the component its cell factory made (insets, optional).
     * Defaults to 1 on every side.
     */
    String STYLE_CELL_PADDING = "grid.cell.padding";
    /**
     * The size of a cell unless {@linkplain #setCellSize set} otherwise, in UI units.
     */
    float DEFAULT_CELL_SIZE = 18f;
    /**
     * The number of columns unless {@linkplain #setColumns set} otherwise.
     */
    int DEFAULT_COLUMNS = 9;
    /**
     * Asked when the user clicks a cell with any button, empty cells included. For a left click that changes the
     * selection it fires after {@link Selection#SELECT} or {@link MultiSelection#SELECT_MANY}, whether or not that
     * request was canceled. The value
     * is the cell, its item, the button and the modifiers. A handler may rewrite it; the grid reports the final click
     * with {@link #CLICKED} as given, unless its index is not a cell's, which is ignored. Canceling it skips
     * {@link #CLICKED}. If a selection handler replaced the items so that the clicked cell is
     * gone, neither this nor {@link Activatable#ACTIVATE} fires for the click. Fires only for the user's actions.
     */
    EventType<ValueRequestEvent<GridView<?>, Click<?>>> CLICK = EventType.valueRequest("grid_view.click");
    /**
     * Told after the user clicked a cell. The value is the click as the {@link #CLICK} request ended, and the
     * previous value is the same click. The grid has no method that clicks a cell from code.
     */
    EventType<ValueEvent<GridView<?>, Click<?>>> CLICKED = EventType.valueReport("grid_view.clicked");

    /**
     * Returns the items, one per cell.
     *
     * @return an unmodifiable list of the items, null for an empty cell
     */
    @Override
    List<T> items();

    /**
     * Replaces the items. Selected cells whose index still exists stay selected and the rest leave the selection,
     * which fires {@link MultiSelection#SELECTED_MANY} and, if the current cell went, {@link Selection#SELECTED}.
     *
     * @param items the items, one per cell, copied; null entries are empty cells
     * @return this grid
     */
    @Override
    GridView<T> setItems(List<? extends T> items);

    /**
     * Returns the item in a cell.
     *
     * @param index the cell index
     * @return the item, or null for an empty cell
     * @throws IndexOutOfBoundsException if there is no such cell
     */
    T item(int index);

    /**
     * Replaces the item in one cell and rebuilds only that cell.
     *
     * @param index the cell index
     * @param item the item, or null to empty the cell
     * @return this grid
     * @throws IndexOutOfBoundsException if there is no such cell
     */
    GridView<T> setItem(int index, T item);

    /**
     * Replaces the cell factory and rebuilds the cells.
     *
     * @param cellFactory makes the component shown in a filled cell
     * @return this grid
     */
    @Override
    GridView<T> setCellFactory(Function<? super T, ? extends Component<?>> cellFactory);

    /**
     * Sets the tooltip of filled cells and rebuilds the cells.
     *
     * @param tooltips returns an item's tooltip, or null for none; a null function means no tooltips
     * @return this grid
     */
    GridView<T> setTooltips(Function<? super T, Tooltip> tooltips);

    /**
     * Returns the number of columns.
     *
     * @return the columns; {@value #DEFAULT_COLUMNS} by default
     */
    int columns();

    /**
     * Sets the number of columns. A new layout runs.
     *
     * @param columns the number of cells per row
     * @return this grid
     * @throws IllegalArgumentException if {@code columns} is less than 1
     */
    GridView<T> setColumns(int columns);

    /**
     * Returns the size of each cell.
     *
     * @return the cell size in UI units; {@value #DEFAULT_CELL_SIZE} by default
     */
    float cellSize();

    /**
     * Sets the size of each cell. A new layout runs.
     *
     * @param size the width and height of a cell in UI units
     * @return this grid
     * @throws IllegalArgumentException if {@code size} is not greater than 0 or is infinite
     */
    GridView<T> setCellSize(float size);

    /**
     * Returns the space between cells.
     *
     * @return the spacing in UI units; 0 by default
     */
    float spacing();

    /**
     * Sets the space between cells, across and down. A new layout runs.
     *
     * @param spacing the spacing in UI units
     * @return this grid
     * @throws IllegalArgumentException if {@code spacing} is negative or infinite
     */
    GridView<T> setSpacing(float spacing);

    /**
     * Returns whether a left click selects a cell.
     *
     * @return true if selectable; false by default
     */
    boolean isSelectable();

    /**
     * Makes left clicks select cells, or not.
     *
     * @param selectable true to select the cell clicked
     * @return this grid
     */
    GridView<T> setSelectable(boolean selectable);

    /**
     * Selects a cell alone, whether or not the grid is selectable, and scrolls it into view.
     * {@link MultiSelection#SELECTED_MANY} and {@link Selection#SELECTED} fire if the selection changed.
     *
     * @param index the cell index, or -1 to select none
     * @return this grid
     * @throws IndexOutOfBoundsException if {@code index} is neither -1 nor a cell index
     */
    @Override
    GridView<T> select(int index);

    /**
     * Returns the vertical offset, as {@link org.inspirenxe.strata.api.control.attribute.item.RowView#scrollY()}
     * describes for a list. It stays 0 while every row fits.
     *
     * @return how far the rows are scrolled down, in UI units
     */
    float scrollY();

    /**
     * Scrolls to an offset, as {@link org.inspirenxe.strata.api.control.attribute.item.RowView#setScrollY} describes
     * for a list.
     *
     * @param scrollY the vertical offset
     * @return this grid
     * @throws IllegalArgumentException if the offset is NaN
     */
    GridView<T> setScrollY(float scrollY);

    /**
     * Scrolls the least distance that shows a cell's row, at the next layout.
     *
     * @param index the cell index
     * @return this grid
     * @throws IndexOutOfBoundsException if there is no such cell
     */
    GridView<T> scrollToIndex(int index);

    /**
     * Returns the cell under the pointer.
     *
     * @return the cell index, or -1 when the pointer is over no cell
     */
    int hoveredIndex();

    /**
     * A click on a cell.
     *
     * @param <T> the type of the items
     * @param index the cell index
     * @param item the item in the cell, or null for an empty cell
     * @param button the button clicked
     * @param modifiers the modifiers held; never null
     */
    record Click<T>(int index, T item, MouseButton button, Modifiers modifiers) {

        /**
         * Checks the button and modifiers.
         */
        public Click {
            Objects.requireNonNull(button, "button");
            Objects.requireNonNull(modifiers, "modifiers");
        }
    }
}
