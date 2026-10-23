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
package org.inspirenxe.strata.api.layout;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.Container;

import java.util.List;

/**
 * A container that places children in the cells of rows and columns.
 *
 * <p>Each row and column has a {@link GridLength}: {@linkplain GridLength#auto() auto} (as big as its largest
 * child), {@linkplain GridLength#fixed(float) fixed}, or {@linkplain GridLength#weighted(float) weighted} (a share
 * of the space left over). A child sits in one cell, or spans several rows or columns; its slot is the cell (or
 * the cells it spans, with the spacing between them) and it aligns itself there like in any container. Several
 * children may share a cell.</p>
 *
 * <p>A weighted track keeps within the size limits of the visible children that sit only in it, margins included:
 * it is never smaller than the largest minimum among them, nor larger than the largest
 * {@linkplain Component#maxWidth() maximum}. A child's minimum is the larger of its
 * {@linkplain Component#minWidth() minimum size} and what its content needs, or its fixed size where one is set. A track held at a limit leaves the other weighted tracks to
 * share the rest by their weights.</p>
 *
 * <p>The grid's own minimum, which a parent that shares space reads, is each fixed track's size plus, for every
 * other track, the largest minimum of the children that sit only in it, with the spacing and the padding.</p>
 *
 * <p>Without column definitions a grid has one column, which fills the content box, or is as wide as its widest
 * child when that is wider; rows work the same way. Cells outside the defined rows and columns are moved to the
 * last row or column, and spans are cut to fit.</p>
 *
 * <pre>{@code
 * GridPanel form = factory.gridPanel()
 *         .columns(GridLength.auto(), GridLength.weighted(1))
 *         .rows(GridLength.auto(), GridLength.auto())
 *         .setSpacing(6, 4);
 * form.add(factory.label("Name"), 0, 0).add(nameBox, 0, 1)
 *     .add(factory.label("Password"), 1, 0).add(passwordBox, 1, 1);
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#gridPanel()}. Do not implement this
 * interface.</p>
 */
public interface GridPanel extends Container<GridPanel> {

    /**
     * Replaces the column definitions.
     *
     * @param lengths one length per column, left to right
     * @return this grid
     */
    GridPanel columns(GridLength... lengths);

    /**
     * Replaces the row definitions.
     *
     * @param lengths one length per row, top to bottom
     * @return this grid
     */
    GridPanel rows(GridLength... lengths);

    /**
     * Adds a column on the right.
     *
     * @param length the column's length
     * @return this grid
     */
    GridPanel addColumn(GridLength length);

    /**
     * Adds a row at the bottom.
     *
     * @param length the row's length
     * @return this grid
     */
    GridPanel addRow(GridLength length);

    /**
     * Returns the column definitions.
     *
     * @return a copy of the column lengths, left to right
     */
    List<GridLength> columnDefinitions();

    /**
     * Returns the row definitions.
     *
     * @return a copy of the row lengths, top to bottom
     */
    List<GridLength> rowDefinitions();

    /**
     * Sets the gaps between columns and between rows. Gaps go between tracks only, not around the edge.
     *
     * @param columnSpacing the gap between columns
     * @param rowSpacing the gap between rows
     * @return this grid
     * @throws IllegalArgumentException if either gap is infinite or NaN
     */
    GridPanel setSpacing(float columnSpacing, float rowSpacing);

    /**
     * Adds a child to one cell.
     *
     * @param child the component to add
     * @param row the row, from 0
     * @param column the column, from 0
     * @return this grid
     * @throws IllegalArgumentException if {@code row} or {@code column} is negative, or as for
     *                                  {@link Container#add(Component)}
     */
    default GridPanel add(final Component<?> child, final int row, final int column) {
        return this.add(child, row, column, 1, 1);
    }

    /**
     * Adds a child spanning several cells.
     *
     * @param child the component to add
     * @param row the top row, from 0
     * @param column the left column, from 0
     * @param rowSpan the number of rows covered, at least 1
     * @param columnSpan the number of columns covered, at least 1
     * @return this grid
     * @throws IllegalArgumentException if a position is negative or a span is below 1, or as for
     *                                  {@link Container#add(Component)}
     */
    GridPanel add(Component<?> child, int row, int column, int rowSpan, int columnSpan);

    /**
     * Returns the cell a child sits in. Children added with {@link Container#add(Component)} sit at
     * {@link Cell#ORIGIN}.
     *
     * @param child a child of this grid
     * @return its cell, as given when it was added
     */
    Cell cellOf(Component<?> child);

    /**
     * The cell or cells a child covers.
     *
     * @param row the top row, from 0
     * @param column the left column, from 0
     * @param rowSpan the number of rows covered, at least 1
     * @param columnSpan the number of columns covered, at least 1
     */
    record Cell(int row, int column, int rowSpan, int columnSpan) {
        /**
         * The top-left cell, spanning one row and one column.
         */
        public static final Cell ORIGIN = new Cell(0, 0, 1, 1);

        /**
         * Checks the position and spans.
         *
         * @throws IllegalArgumentException if a position is negative or a span is below 1
         */
        public Cell {
            if (row < 0 || column < 0) {
                throw new IllegalArgumentException("Row and column must not be negative");
            }
            if (rowSpan < 1 || columnSpan < 1) {
                throw new IllegalArgumentException("Spans must be at least 1");
            }
        }
    }

    /**
     * A grid's own state in a {@link org.inspirenxe.strata.api.component.ComponentSnapshot}. It holds the row and
     * column definitions, but not the children or their cells.
     *
     * @param columns the {@linkplain #columnDefinitions() column definitions}, left to right; copied, never null
     * @param rows the {@linkplain #rowDefinitions() row definitions}, top to bottom; copied, never null
     * @param columnSpacing the gap between columns
     * @param rowSpacing the gap between rows
     */
    record State(List<GridLength> columns, List<GridLength> rows, float columnSpacing, float rowSpacing) {

        /**
         * Copies the definitions.
         */
        public State {
            columns = List.copyOf(columns);
            rows = List.copyOf(rows);
        }
    }
}
