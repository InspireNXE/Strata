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
import org.inspirenxe.strata.api.component.HasCursor;
import org.inspirenxe.strata.api.component.HasTooltip;
import org.inspirenxe.strata.api.component.Tooltip;
import org.inspirenxe.strata.api.control.attribute.item.Activatable;
import org.inspirenxe.strata.api.control.attribute.item.CellSource;
import org.inspirenxe.strata.api.control.attribute.item.ItemSource;
import org.inspirenxe.strata.api.control.attribute.item.SingleSelection;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.control.ValueEvent;
import org.inspirenxe.strata.api.event.control.ValueRequestEvent;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * A grid of equal square cells over a list of items, such as an inventory, an icon picker or an image gallery.
 *
 * <p>There is one cell per entry in the items, null entries being empty cells, laid out left to right in rows of
 * {@linkplain #setColumns a set number of columns}. Each filled cell shows the component its cell factory makes for
 * the item, inset by one unit, and the tooltip its tooltip function gives. Cells are rebuilt when the items, the
 * cell factory or the tooltip function change. Unlike a list, the grid is positional: {@link #setItems} keeps the
 * selection on the same index, and {@link #setKeyFunction} only affects {@link #selectItem}.</p>
 *
 * <p>Clicking a cell with any button fires {@link #CLICK} and then {@link #CLICKED}, empty cells included, and
 * consumes the click. A {@linkplain #setSelectable selectable} grid also selects the cell clicked with the left
 * button. A double click with the left button activates the cell, or a single click with
 * {@link #setActivateOnClick}. The grid is not focusable, so it has no keyboard selection. The desired size is the
 * columns and rows of cells with the spacing between them.</p>
 *
 * <h2>Events</h2>
 * <ul>
 *     <li>{@link SingleSelection#SELECT}: Asked before the user changes the selection: a left click on a cell other
 *     than the selected one, when the grid is selectable. The value is the cell about to be selected. A handler may
 *     rewrite it to another cell or to -1, which clears the selection; any other index outside the cells is ignored.
 *     Fires only for the user's actions.</li>
 *     <li>{@link SingleSelection#SELECTED}: Told after the selection changed. The value is the selected cell, or -1
 *     when none is selected, and the previous value is the cell before. Fires for changes made from code too,
 *     including {@link #setItems} clearing a selection whose cell no longer exists.</li>
 *     <li>{@link Activatable#ACTIVATE}: Asked before the user activates a cell, empty cells included. It fires after
 *     the click's {@link #CLICK}. The value is the cell. A handler may rewrite it to another cell; an index outside
 *     the cells is ignored. Fires only for the user's actions.</li>
 *     <li>{@link Activatable#ACTIVATED}: Told after the user activated a cell. The value is the cell, and the previous
 *     value is the same cell.</li>
 * </ul>
 *
 * <h2>Theme keys</h2>
 * <ul>
 *     <li>{@value #STYLE_CELL} (paint, required): each cell, behind its content.</li>
 *     <li>{@value #STYLE_CELL_HOVERED} (paint, optional): drawn over the cell under the pointer, in front of its
 *     content. Nothing if missing.</li>
 *     <li>{@value #STYLE_CELL_SELECTED} (paint, optional): drawn over the selected cell, in front of its content.
 *     Falls back to {@value #STYLE_CELL_HOVERED}.</li>
 * </ul>
 *
 * <pre>{@code
 * GridView<Item> inventory = factory.<Item>gridView(item -> factory.image(item.icon()))
 *         .setColumns(9)
 *         .setTooltips(item -> Tooltip.text(item.name()))
 *         .setItems(storage.contents())
 *         .onEvent(GridView.CLICKED, e -> this.take(e.value().index(), e.value().button()));
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#gridView}. Do not implement this
 * interface.</p>
 *
 * @param <T> the type of the items
 */
public interface GridView<T> extends Component<GridView<T>>, HasCursor<GridView<T>>, HasTooltip<GridView<T>>,
        ItemSource<T, GridView<T>>, CellSource<T, GridView<T>>,
        SingleSelection<T, GridView<T>>, Activatable<GridView<T>> {
    /**
     * Style key for each cell (paint, required).
     */
    String STYLE_CELL = "grid.cell";
    /**
     * Style key drawn over the cell under the pointer (paint, optional).
     */
    String STYLE_CELL_HOVERED = "grid.cell.hovered";
    /**
     * Style key drawn over the selected cell (paint, optional).
     */
    String STYLE_CELL_SELECTED = "grid.cell.selected";
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
     * selection it fires after {@link SingleSelection#SELECT}, whether or not that request was canceled. The value
     * is the cell, its item, the button and the modifiers. A handler may rewrite it; the grid reports the final click
     * with {@link #CLICKED} as given, unless its index is not a cell's, which is ignored. Canceling it skips
     * {@link #CLICKED}. Fires only for the user's actions.
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
     * Replaces the items. The selection is kept if its index still exists, and cleared otherwise, which fires
     * {@link SingleSelection#SELECTED}.
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
     * Selects a cell, whether or not the grid is selectable. {@link SingleSelection#SELECTED} fires if the selection
     * changed.
     *
     * @param index the cell index, or -1 to select none
     * @return this grid
     * @throws IndexOutOfBoundsException if {@code index} is neither -1 nor a cell index
     */
    @Override
    GridView<T> select(int index);

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
