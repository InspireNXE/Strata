package org.inspirenxe.strata.core.layout;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.extension.ArrangeContext;
import org.inspirenxe.strata.api.extension.CustomContainer;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.geom.Insets;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.geom.Size;
import org.inspirenxe.strata.api.layout.GridPanel;
import org.inspirenxe.strata.api.layout.GridLength;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The core {@link GridPanel}.
 *
 * <p>Measuring sizes tracks in two steps. A fixed track takes its size and an auto track the largest desired size
 * of its single-span children. Weighted tracks share one unit, the smallest that gives every weighted track's
 * single-span children their desired size. Then each spanning child that still does not fit grows the auto tracks
 * it crosses by equal shares; a span with no auto track is not grown.</p>
 *
 * <p>Arranging gives the weighted tracks whatever the fixed and auto tracks leave, in proportion to their weights.
 * A child's slot covers every track it spans plus the spacing between them. A grid with no column or row
 * definitions has one track on that axis, which fills the content box. A cell or span past the last track is
 * clamped to fit.</p>
 */
public final class GridPanelImpl extends CustomContainer<GridPanel> implements GridPanel {
    private final List<GridLength> columns;
    private final List<GridLength> rows;
    private final Map<Component<?>, Cell> cells;

    private float columnSpacing;
    private float rowSpacing;

    // Reused between layouts so a relayout does not allocate per track
    private float[] columnSizes;
    private float[] rowSizes;
    private float[] columnOffsets;
    private float[] rowOffsets;

    /**
     * Creates an empty grid with no track definitions and no spacing.
     *
     * @param ui the factory that owns the grid
     */
    public GridPanelImpl(final ComponentFactory ui) {
        super(ui);
        this.columns = new ArrayList<>();
        this.rows = new ArrayList<>();
        this.cells = new IdentityHashMap<>();
        this.columnSizes = new float[0];
        this.rowSizes = new float[0];
        this.columnOffsets = new float[0];
        this.rowOffsets = new float[0];
    }

    @Override
    public GridPanel columns(final GridLength... lengths) {
        this.columns.clear();
        this.columns.addAll(Arrays.asList(lengths));
        this.invalidateLayout();
        return this;
    }

    @Override
    public GridPanel rows(final GridLength... lengths) {
        this.rows.clear();
        this.rows.addAll(Arrays.asList(lengths));
        this.invalidateLayout();
        return this;
    }

    @Override
    public GridPanel addColumn(final GridLength length) {
        this.columns.add(Objects.requireNonNull(length, "length"));
        this.invalidateLayout();
        return this;
    }

    @Override
    public GridPanel addRow(final GridLength length) {
        this.rows.add(Objects.requireNonNull(length, "length"));
        this.invalidateLayout();
        return this;
    }

    @Override
    public List<GridLength> columnDefinitions() {
        return List.copyOf(this.columns);
    }

    @Override
    public List<GridLength> rowDefinitions() {
        return List.copyOf(this.rows);
    }

    @Override
    public GridPanel setSpacing(final float columnSpacing, final float rowSpacing) {
        this.columnSpacing = columnSpacing;
        this.rowSpacing = rowSpacing;
        this.invalidateLayout();
        return this;
    }

    @Override
    public GridPanel add(final Component<?> child, final int row, final int column, final int rowSpan, final int columnSpan) {
        final Cell cell = new Cell(row, column, rowSpan, columnSpan);
        this.add(child);
        this.cells.put(child, cell);
        return this;
    }

    @Override
    public Cell cellOf(final Component<?> child) {
        return this.cells.getOrDefault(child, Cell.ORIGIN);
    }

    @Override
    protected void onChildRemoved(final Component<?> child) {
        this.cells.remove(child);
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        final int columnCount = Math.max(1, this.columns.size());
        final int rowCount = Math.max(1, this.rows.size());
        this.columnSizes = ensure(this.columnSizes, columnCount);
        this.rowSizes = ensure(this.rowSizes, rowCount);

        final Insets padding = this.padding();
        final float innerWidth = availableWidth - padding.horizontal();
        final float innerHeight = availableHeight - padding.vertical();

        // Single-span needs per track
        final float[] columnNeeds = new float[columnCount];
        final float[] rowNeeds = new float[rowCount];
        final List<Component<?>> children = this.children();
        final Size[] outers = new Size[children.size()];

        for (int i = 0; i < children.size(); i++) {
            final Component<?> child = children.get(i);
            final Cell cell = this.clampedCell(child, columnCount, rowCount);
            final float width = fixedSpan(this.columns, cell.column(), cell.columnSpan(), this.columnSpacing, innerWidth);
            final float height = fixedSpan(this.rows, cell.row(), cell.rowSpan(), this.rowSpacing, innerHeight);
            outers[i] = context.measure(child, width, height);

            if (cell.columnSpan() == 1) {
                columnNeeds[cell.column()] = Math.max(columnNeeds[cell.column()], outers[i].width());
            }
            if (cell.rowSpan() == 1) {
                rowNeeds[cell.row()] = Math.max(rowNeeds[cell.row()], outers[i].height());
            }
        }

        resolveNatural(this.columns, columnNeeds, this.columnSizes);
        resolveNatural(this.rows, rowNeeds, this.rowSizes);

        // Spanning children grow the auto tracks they cross
        for (int i = 0; i < children.size(); i++) {
            final Cell cell = this.clampedCell(children.get(i), columnCount, rowCount);
            if (cell.columnSpan() > 1) {
                growAuto(this.columns, this.columnSizes, cell.column(), cell.columnSpan(), this.columnSpacing, outers[i].width());
            }
            if (cell.rowSpan() > 1) {
                growAuto(this.rows, this.rowSizes, cell.row(), cell.rowSpan(), this.rowSpacing, outers[i].height());
            }
        }

        final float width = sum(this.columnSizes, columnCount) + this.columnSpacing * (columnCount - 1);
        final float height = sum(this.rowSizes, rowCount) + this.rowSpacing * (rowCount - 1);
        return new Size(width + padding.horizontal(), height + padding.vertical());
    }

    @Override
    protected void arrange(final ArrangeContext context, final Rect bounds) {
        final int columnCount = Math.max(1, this.columns.size());
        final int rowCount = Math.max(1, this.rows.size());
        final Rect content = this.contentBox();

        distribute(this.columns, this.columnSizes, columnCount, content.width() - this.columnSpacing * (columnCount - 1));
        distribute(this.rows, this.rowSizes, rowCount, content.height() - this.rowSpacing * (rowCount - 1));

        this.columnOffsets = ensure(this.columnOffsets, columnCount);
        this.rowOffsets = ensure(this.rowOffsets, rowCount);
        offsets(this.columnSizes, this.columnOffsets, columnCount, content.x(), this.columnSpacing);
        offsets(this.rowSizes, this.rowOffsets, rowCount, content.y(), this.rowSpacing);

        for (final Component<?> child : this.children()) {
            final Cell cell = this.clampedCell(child, columnCount, rowCount);
            final int lastColumn = cell.column() + cell.columnSpan() - 1;
            final int lastRow = cell.row() + cell.rowSpan() - 1;
            final float x = this.columnOffsets[cell.column()];
            final float y = this.rowOffsets[cell.row()];
            final float width = this.columnOffsets[lastColumn] + this.columnSizes[lastColumn] - x;
            final float height = this.rowOffsets[lastRow] + this.rowSizes[lastRow] - y;
            context.arrange(child, new Rect(x, y, width, height));
        }
    }

    private Cell clampedCell(final Component<?> child, final int columnCount, final int rowCount) {
        final Cell cell = this.cellOf(child);
        final int column = Math.min(cell.column(), columnCount - 1);
        final int row = Math.min(cell.row(), rowCount - 1);
        return new Cell(row, column, Math.min(cell.rowSpan(), rowCount - row), Math.min(cell.columnSpan(), columnCount - column));
    }

    /**
     * The size available to a child: the sum of its tracks if they are all fixed, otherwise the whole inner size.
     */
    private static float fixedSpan(final List<GridLength> tracks, final int start, final int span, final float spacing,
                                   final float inner) {
        if (tracks.isEmpty()) {
            return inner;
        }
        float total = spacing * (span - 1);
        for (int i = start; i < start + span; i++) {
            if (!(tracks.get(i) instanceof GridLength.Fixed fixed)) {
                return inner;
            }
            total += fixed.size();
        }
        return total;
    }

    private static void resolveNatural(final List<GridLength> tracks, final float[] needs, final float[] sizes) {
        if (tracks.isEmpty()) {
            sizes[0] = needs[0];
            return;
        }
        float unit = 0f;
        for (int i = 0; i < tracks.size(); i++) {
            if (tracks.get(i) instanceof GridLength.Weighted weighted) {
                unit = Math.max(unit, needs[i] / weighted.weight());
            }
        }
        for (int i = 0; i < tracks.size(); i++) {
            sizes[i] = switch (tracks.get(i)) {
                case GridLength.Fixed fixed -> fixed.size();
                case GridLength.Auto ignored -> needs[i];
                case GridLength.Weighted weighted -> unit * weighted.weight();
            };
        }
    }

    private static void growAuto(final List<GridLength> tracks, final float[] sizes, final int start, final int span,
                                 final float spacing, final float need) {
        float current = spacing * (span - 1);
        int autos = 0;
        for (int i = start; i < start + span; i++) {
            current += sizes[i];
            if (tracks.isEmpty() || tracks.get(i) instanceof GridLength.Auto) {
                autos++;
            }
        }
        if (need <= current || autos == 0) {
            return;
        }
        final float extra = (need - current) / autos;
        for (int i = start; i < start + span; i++) {
            if (tracks.isEmpty() || tracks.get(i) instanceof GridLength.Auto) {
                sizes[i] += extra;
            }
        }
    }

    /**
     * Shares the space left after fixed and auto tracks among weighted tracks. Without weighted tracks the
     * measured sizes stand.
     */
    private static void distribute(final List<GridLength> tracks, final float[] sizes, final int count, final float space) {
        if (tracks.isEmpty()) {
            sizes[0] = Math.max(sizes[0], space);
            return;
        }
        float used = 0f;
        float totalWeight = 0f;
        for (int i = 0; i < count; i++) {
            if (tracks.get(i) instanceof GridLength.Weighted weighted) {
                totalWeight += weighted.weight();
            } else {
                used += sizes[i];
            }
        }
        if (totalWeight <= 0f) {
            return;
        }
        final float remaining = Math.max(0f, space - used);
        for (int i = 0; i < count; i++) {
            if (tracks.get(i) instanceof GridLength.Weighted weighted) {
                sizes[i] = remaining * weighted.weight() / totalWeight;
            }
        }
    }

    private static void offsets(final float[] sizes, final float[] offsets, final int count, final float start,
                                final float spacing) {
        float position = start;
        for (int i = 0; i < count; i++) {
            offsets[i] = position;
            position += sizes[i] + spacing;
        }
    }

    private static float sum(final float[] values, final int count) {
        float total = 0f;
        for (int i = 0; i < count; i++) {
            total += values[i];
        }
        return total;
    }

    private static float[] ensure(final float[] array, final int length) {
        if (array.length == length) {
            Arrays.fill(array, 0f);
            return array;
        }
        return new float[length];
    }

    @Override
    protected Object snapshotState() {
        return new GridPanel.State(this.columns, this.rows, this.columnSpacing, this.rowSpacing);
    }
}
