package org.inspirenxe.strata.api.layout;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.Container;

import java.util.List;

/**
 * Rows and columns sized {@link GridLength.Auto auto}, {@link GridLength.Fixed fixed} or
 * {@link GridLength.Weighted weighted}. Each child sits in a cell and may span several rows or columns.
 *
 * <p>Fixed tracks keep their size; auto tracks grow to their largest single-span child; weighted tracks share
 * whatever space is left by weight. A spanning child that does not fit grows the auto tracks it spans. With no
 * definitions the grid is a single auto cell. {@link #add(Component)} places a child in the top-left cell.</p>
 */
public interface Grid extends Container<Grid> {

    Grid columns(GridLength... lengths);

    Grid rows(GridLength... lengths);

    Grid addColumn(GridLength length);

    Grid addRow(GridLength length);

    List<GridLength> columnDefinitions();

    List<GridLength> rowDefinitions();

    Grid setSpacing(float columnSpacing, float rowSpacing);

    default Grid add(final Component<?> child, final int row, final int column) {
        return this.add(child, row, column, 1, 1);
    }

    Grid add(Component<?> child, int row, int column, int rowSpan, int columnSpan);

    /**
     * The cell a child sits in; children added without a cell sit at {@link Cell#ORIGIN}.
     */
    Cell cellOf(Component<?> child);

    /**
     * Where a child sits. Spans are at least 1.
     */
    record Cell(int row, int column, int rowSpan, int columnSpan) {
        public static final Cell ORIGIN = new Cell(0, 0, 1, 1);

        public Cell {
            if (row < 0 || column < 0) {
                throw new IllegalArgumentException("Row and column must not be negative");
            }
            if (rowSpan < 1 || columnSpan < 1) {
                throw new IllegalArgumentException("Spans must be at least 1");
            }
        }
    }
}
