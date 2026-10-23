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
package org.inspirenxe.strata.core.layout;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.extension.ArrangeContext;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.layout.GridLength;
import org.inspirenxe.strata.api.layout.GridPanel;
import org.inspirenxe.strata.core.node.BuiltInContainer;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The core {@link GridPanel}.
 *
 * <p>Measuring sizes tracks in two steps. A fixed track takes its size and an auto track the largest desired size
 * of its single-span children. Children are measured in four groups, so the fixed and auto tracks are known before
 * the weighted tracks share what they leave: first those in no weighted track, then those in a weighted row only,
 * then those in a weighted column only, and last those in both. A child in a weighted column is offered its
 * columns' share of the width the fixed and auto columns leave, so wrapping text wraps at the column it sits in, and
 * a child in a weighted row its rows' share of the height in the same way. Auto rows are known in full only after
 * the third group, so a child in a weighted row and no weighted column is offered a share of what the auto rows
 * measured before it leave. With unbounded space on an axis, or no definitions on it, a child is offered all of it.
 * Weighted tracks share one unit, the smallest that gives every weighted track's single-span children their desired
 * size. Then each spanning child that still does not fit grows the auto tracks it crosses by equal shares; a span
 * with no auto track is not grown.</p>
 *
 * <p>Arranging starts again from the measured sizes and gives the weighted tracks whatever the fixed and auto tracks
 * leave, in proportion to their weights. A child's slot covers every track it spans plus the spacing between them. A
 * grid with no column or row definitions has one track on that axis, which fills the content box, or keeps its
 * measured size when that is larger. A cell or span past the last track is clamped to fit.</p>
 *
 * <p>Every weighted share, measured or arranged, keeps a weighted track within the limits of the visible children
 * that sit only in it: at least the largest of their minimum sizes and at most the largest of their maximum sizes,
 * margins included, as {@link WeightedShares} resolves them. A track with no such child has no limits.</p>
 *
 * <p>In each row, the children that sit in that row alone and are aligned
 * {@link org.inspirenxe.strata.api.layout.VerticalAlignment#BASELINE} are lined up as {@link BaselineRow} describes:
 * measuring makes the row need at least the height they take lined up, and arranging starts each one's slot where
 * its baseline meets theirs, as tall as the child asked for and as wide as its cells.</p>
 *
 * <p>The per-track arrays are kept between layouts, so a layout allocates them only when the number of tracks
 * grows.</p>
 */
public final class GridPanelImpl extends BuiltInContainer<GridPanel> implements GridPanel {
    private final List<GridLength> columns;
    private final List<GridLength> rows;
    private final Map<Component<?>, Cell> cells;
    private final Axis columnAxis;
    private final Axis rowAxis;

    private float columnSpacing;
    private float rowSpacing;

    // Reused between measures; grows with the children
    private Size[] outers;
    // Track floors while working out the grid's own minimum
    private float[] floors;
    // Lines up the baselines of one row at a time, and the line of each row while arranging
    private final BaselineRow baselines;
    private float[] rowLines;

    /**
     * Creates an empty grid with no track definitions and no spacing.
     *
     * @param ui the factory that owns the grid
     */
    public GridPanelImpl(final ComponentFactoryImpl ui) {
        super(ui);
        this.columns = new ArrayList<>();
        this.rows = new ArrayList<>();
        this.cells = new IdentityHashMap<>();
        this.columnAxis = new Axis(this.columns);
        this.rowAxis = new Axis(this.rows);
        this.outers = new Size[0];
        this.floors = new float[0];
        this.baselines = new BaselineRow();
        this.rowLines = new float[0];
    }

    @Override
    public GridPanel columns(final GridLength... lengths) {
        // Checked before the old definitions go, so a bad argument changes nothing
        final List<GridLength> checked = List.of(lengths);
        this.columns.clear();
        this.columns.addAll(checked);
        this.invalidateLayout();
        return this;
    }

    @Override
    public GridPanel rows(final GridLength... lengths) {
        final List<GridLength> checked = List.of(lengths);
        this.rows.clear();
        this.rows.addAll(checked);
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
        if (!Float.isFinite(columnSpacing) || !Float.isFinite(rowSpacing)) {
            throw new IllegalArgumentException("Spacing must be finite: " + columnSpacing + ", " + rowSpacing);
        }
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
        final Axis columns = this.columnAxis;
        final Axis rows = this.rowAxis;
        columns.begin();
        rows.begin();

        final Insets padding = this.padding();
        final float innerWidth = availableWidth - padding.horizontal();
        final float innerHeight = availableHeight - padding.vertical();

        final List<Component<?>> children = this.children();
        if (this.outers.length < children.size()) {
            this.outers = new Size[children.size()];
        }

        // Weighted tracks keep within the limits of the visible children that sit only in them
        for (final Component<?> child : children) {
            if (!child.isVisible()) {
                continue;
            }
            final Cell cell = this.cellOf(child);
            final Insets margin = child.margin();
            final Size minimum = context.minimumSize(child);
            if (columns.span(cell.column(), cell.columnSpan()) == 1) {
                columns.limit(columns.first(cell.column()), minimum.width(), child.maxWidth() + margin.horizontal());
            }
            if (rows.span(cell.row(), cell.rowSpan()) == 1) {
                rows.limit(rows.first(cell.row()), minimum.height(), child.maxHeight() + margin.vertical());
            }
        }

        // Children in no weighted track first, then those in weighted rows only, weighted columns only, and both
        for (int group = 0; group < 4; group++) {
            if (group == 1 || group == 3) {
                rows.share(innerHeight, this.rowSpacing);
            } else if (group == 2) {
                columns.share(innerWidth, this.columnSpacing);
            }
            for (int i = 0; i < children.size(); i++) {
                final Cell cell = this.cellOf(children.get(i));
                final int column = columns.first(cell.column());
                final int columnSpan = columns.span(cell.column(), cell.columnSpan());
                final int row = rows.first(cell.row());
                final int rowSpan = rows.span(cell.row(), cell.rowSpan());
                final boolean weightedColumn = columns.crossesWeighted(column, columnSpan);
                final boolean weightedRow = rows.crossesWeighted(row, rowSpan);
                if ((weightedRow ? 1 : 0) + (weightedColumn ? 2 : 0) != group) {
                    continue;
                }
                final float width = weightedColumn ? columns.offered(column, columnSpan, this.columnSpacing, innerWidth)
                        : columns.fixedSpan(column, columnSpan, this.columnSpacing, innerWidth);
                final float height = weightedRow ? rows.offered(row, rowSpan, this.rowSpacing, innerHeight)
                        : rows.fixedSpan(row, rowSpan, this.rowSpacing, innerHeight);
                final Size outer = context.measure(children.get(i), width, height);
                this.outers[i] = outer;
                if (columnSpan == 1) {
                    columns.need(column, outer.width());
                }
                if (rowSpan == 1) {
                    rows.need(row, outer.height());
                }
            }
        }

        this.needBaselines(context, children, rows);
        columns.natural();
        rows.natural();

        // Spanning children grow the auto tracks they cross
        for (int i = 0; i < children.size(); i++) {
            final Cell cell = this.cellOf(children.get(i));
            final int columnSpan = columns.span(cell.column(), cell.columnSpan());
            final int rowSpan = rows.span(cell.row(), cell.rowSpan());
            if (columnSpan > 1) {
                columns.growAuto(columns.first(cell.column()), columnSpan, this.columnSpacing, this.outers[i].width());
            }
            if (rowSpan > 1) {
                rows.growAuto(rows.first(cell.row()), rowSpan, this.rowSpacing, this.outers[i].height());
            }
        }
        // Drop the references so removed children are not kept alive
        for (int i = 0; i < children.size(); i++) {
            this.outers[i] = null;
        }

        final float width = columns.measuredLength(this.columnSpacing);
        final float height = rows.measuredLength(this.rowSpacing);
        return new Size(width + padding.horizontal(), height + padding.vertical());
    }

    /**
     * Makes each row need the height its single-row children aligned to the baseline take lined up.
     */
    private void needBaselines(final MeasureContext context, final List<Component<?>> children, final Axis rows) {
        if (!this.anyAligned(children, rows)) {
            return;
        }
        for (int row = 0; row < rows.count; row++) {
            final BaselineRow baselines = this.baselines;
            baselines.reset();
            for (int i = 0; i < children.size(); i++) {
                final Component<?> child = children.get(i);
                if (this.alignsIn(child, rows, row)) {
                    baselines.add(context.baseline(child), this.outers[i].height());
                }
            }
            rows.need(row, baselines.height());
        }
    }

    private boolean anyAligned(final List<Component<?>> children, final Axis rows) {
        for (final Component<?> child : children) {
            final Cell cell = this.cellOf(child);
            if (BaselineRow.aligns(child) && rows.span(cell.row(), cell.rowSpan()) == 1) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns whether a child is lined up by its baseline in a row: it is aligned to the baseline and sits in that
     * row alone.
     */
    private boolean alignsIn(final Component<?> child, final Axis rows, final int row) {
        if (!BaselineRow.aligns(child)) {
            return false;
        }
        final Cell cell = this.cellOf(child);
        return rows.first(cell.row()) == row && rows.span(cell.row(), cell.rowSpan()) == 1;
    }

    @Override
    protected Size measureMinimum(final MeasureContext context) {
        final Insets padding = this.padding();
        return new Size(this.minimumLength(context, this.columns, true, this.columnSpacing) + padding.horizontal(),
                this.minimumLength(context, this.rows, false, this.rowSpacing) + padding.vertical());
    }

    /**
     * Returns the smallest length of one axis: each fixed track its size, every other track the largest outer
     * minimum of the visible children that sit only in it, and the spacing between tracks. Children that span
     * several tracks add nothing.
     */
    private float minimumLength(final MeasureContext context, final List<GridLength> tracks, final boolean horizontal,
                                final float spacing) {
        final int count = Math.max(1, tracks.size());
        if (this.floors.length < count) {
            this.floors = new float[count];
        }
        for (int i = 0; i < count; i++) {
            this.floors[i] = i < tracks.size() && tracks.get(i) instanceof GridLength.Fixed fixed ? fixed.size() : 0f;
        }
        for (final Component<?> child : this.children()) {
            if (!child.isVisible()) {
                continue;
            }
            final Cell cell = this.cellOf(child);
            final int track = Math.min(horizontal ? cell.column() : cell.row(), count - 1);
            final int span = Math.min(horizontal ? cell.columnSpan() : cell.rowSpan(), count - track);
            if (span == 1 && !(track < tracks.size() && tracks.get(track) instanceof GridLength.Fixed)) {
                final Size minimum = context.minimumSize(child);
                this.floors[track] = Math.max(this.floors[track], horizontal ? minimum.width() : minimum.height());
            }
        }
        float length = spacing * (count - 1);
        for (int i = 0; i < count; i++) {
            length += this.floors[i];
        }
        return length;
    }

    @Override
    protected void arrange(final ArrangeContext context, final Rect bounds) {
        final Axis columns = this.columnAxis;
        final Axis rows = this.rowAxis;
        final Rect content = this.contentBox();

        columns.distribute(content.width() - this.columnSpacing * (columns.count - 1));
        rows.distribute(content.height() - this.rowSpacing * (rows.count - 1));
        columns.offsets(content.x(), this.columnSpacing);
        rows.offsets(content.y(), this.rowSpacing);
        final List<Component<?>> children = this.children();
        final boolean aligned = this.anyAligned(children, rows);
        if (aligned) {
            this.lineUpRows(context, children, rows);
        }

        for (final Component<?> child : children) {
            final Cell cell = this.cellOf(child);
            final int column = columns.first(cell.column());
            final int row = rows.first(cell.row());
            final int lastColumn = column + columns.span(cell.column(), cell.columnSpan()) - 1;
            final int lastRow = row + rows.span(cell.row(), cell.rowSpan()) - 1;
            final float x = columns.offsets[column];
            final float y = rows.offsets[row];
            final float width = columns.offsets[lastColumn] + columns.sizes[lastColumn] - x;
            final float height = rows.offsets[lastRow] + rows.sizes[lastRow] - y;
            if (aligned && this.alignsIn(child, rows, row)) {
                final float baseline = context.baseline(child);
                final float outer = context.outerSize(child).height();
                final float offset = Float.isNaN(baseline) ? outer : baseline;
                context.arrange(child, new Rect(x, y + this.rowLines[row] - offset, width, outer));
            } else {
                context.arrange(child, new Rect(x, y, width, height));
            }
        }
    }

    /**
     * Works out the shared baseline of each row, from the top of the row, into {@link #rowLines}.
     */
    private void lineUpRows(final ArrangeContext context, final List<Component<?>> children, final Axis rows) {
        if (this.rowLines.length < rows.count) {
            this.rowLines = new float[rows.count];
        }
        for (int row = 0; row < rows.count; row++) {
            final BaselineRow baselines = this.baselines;
            baselines.reset();
            for (final Component<?> child : children) {
                if (this.alignsIn(child, rows, row)) {
                    baselines.add(context.baseline(child), context.outerSize(child).height());
                }
            }
            this.rowLines[row] = baselines.line();
        }
    }

    @Override
    protected Object snapshotState() {
        return new GridPanel.State(this.columns, this.rows, this.columnSpacing, this.rowSpacing);
    }

    /**
     * The tracks of one axis and what layout works out for them. A grid without definitions on the axis has one
     * track there that acts as neither fixed, auto nor weighted.
     */
    private static final class Axis {
        private final List<GridLength> tracks;
        private final WeightedShares shares;
        private int count;
        // The largest single-span need, and the size measure settled on
        private float[] needs;
        private float[] measured;
        // Shares offered during measure, then the arranged sizes
        private float[] sizes;
        private float[] offsets;
        // Outer limits of the children that sit only in the track; a max of negative infinity means no child
        private float[] mins;
        private float[] maxes;
        private boolean bounded;

        private Axis(final List<GridLength> tracks) {
            this.tracks = tracks;
            this.shares = new WeightedShares();
            this.needs = new float[0];
            this.measured = new float[0];
            this.sizes = new float[0];
            this.offsets = new float[0];
            this.mins = new float[0];
            this.maxes = new float[0];
        }

        /**
         * Starts a measure: sizes the arrays for the definitions and clears the needs and limits.
         */
        private void begin() {
            this.count = Math.max(1, this.tracks.size());
            if (this.needs.length < this.count) {
                this.needs = new float[this.count];
                this.measured = new float[this.count];
                this.sizes = new float[this.count];
                this.offsets = new float[this.count];
                this.mins = new float[this.count];
                this.maxes = new float[this.count];
            }
            for (int i = 0; i < this.count; i++) {
                this.needs[i] = 0f;
                this.measured[i] = 0f;
                this.mins[i] = 0f;
                this.maxes[i] = Float.NEGATIVE_INFINITY;
            }
            this.bounded = false;
        }

        private int first(final int index) {
            return Math.min(index, this.count - 1);
        }

        private int span(final int index, final int span) {
            return Math.min(span, this.count - this.first(index));
        }

        private void limit(final int track, final float min, final float max) {
            this.mins[track] = Math.max(this.mins[track], min);
            this.maxes[track] = Math.max(this.maxes[track], max);
        }

        private float max(final int track) {
            return this.maxes[track] == Float.NEGATIVE_INFINITY ? Float.POSITIVE_INFINITY : this.maxes[track];
        }

        private void need(final int track, final float need) {
            this.needs[track] = Math.max(this.needs[track], need);
        }

        private boolean crossesWeighted(final int start, final int span) {
            for (int i = start; i < start + span && i < this.tracks.size(); i++) {
                if (this.tracks.get(i) instanceof GridLength.Weighted) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Works out the size of every track as a child in a weighted track is offered it: fixed tracks their size,
         * auto tracks the needs found so far, and weighted tracks their share of what those leave. Nothing is shared
         * when the space is unbounded or the axis has no definitions.
         */
        private void share(final float inner, final float spacing) {
            this.bounded = !this.tracks.isEmpty() && Float.isFinite(inner);
            if (!this.bounded) {
                return;
            }
            float used = spacing * (this.count - 1);
            this.shares.reset(this.count);
            for (int i = 0; i < this.count; i++) {
                final GridLength track = this.tracks.get(i);
                if (track instanceof GridLength.Fixed fixed) {
                    this.sizes[i] = fixed.size();
                    used += fixed.size();
                } else if (track instanceof GridLength.Auto) {
                    this.sizes[i] = this.needs[i];
                    used += this.needs[i];
                } else if (track instanceof GridLength.Weighted weighted) {
                    this.shares.set(i, weighted.weight(), this.mins[i], this.max(i));
                }
            }
            this.shares.resolve(inner - used);
            for (int i = 0; i < this.count; i++) {
                if (this.tracks.get(i) instanceof GridLength.Weighted) {
                    this.sizes[i] = this.shares.size(i);
                }
            }
        }

        /**
         * The space offered to a child that crosses a weighted track: its tracks' shares and the spacing between
         * them, or the whole inner size when nothing was shared.
         */
        private float offered(final int start, final int span, final float spacing, final float inner) {
            if (!this.bounded) {
                return inner;
            }
            float total = spacing * (span - 1);
            for (int i = start; i < start + span; i++) {
                total += this.sizes[i];
            }
            return total;
        }

        /**
         * The space offered to a child in no weighted track: the sum of its tracks if they are all fixed, otherwise
         * the whole inner size.
         */
        private float fixedSpan(final int start, final int span, final float spacing, final float inner) {
            if (this.tracks.isEmpty()) {
                return inner;
            }
            float total = spacing * (span - 1);
            for (int i = start; i < start + span; i++) {
                if (!(this.tracks.get(i) instanceof GridLength.Fixed fixed)) {
                    return inner;
                }
                total += fixed.size();
            }
            return total;
        }

        /**
         * Settles the measured sizes from the needs: fixed tracks their size, auto tracks their need, and weighted
         * tracks one shared unit within their limits.
         */
        private void natural() {
            if (this.tracks.isEmpty()) {
                this.measured[0] = this.needs[0];
                return;
            }
            this.shares.reset(this.count);
            for (int i = 0; i < this.count; i++) {
                if (this.tracks.get(i) instanceof GridLength.Weighted weighted) {
                    this.shares.set(i, weighted.weight(), this.mins[i], this.max(i));
                    this.shares.need(i, this.needs[i]);
                }
            }
            this.shares.fit();
            for (int i = 0; i < this.count; i++) {
                final GridLength track = this.tracks.get(i);
                if (track instanceof GridLength.Fixed fixed) {
                    this.measured[i] = fixed.size();
                } else if (track instanceof GridLength.Auto) {
                    this.measured[i] = this.needs[i];
                } else {
                    this.measured[i] = this.shares.size(i);
                }
            }
        }

        private void growAuto(final int start, final int span, final float spacing, final float need) {
            float current = spacing * (span - 1);
            int autos = 0;
            for (int i = start; i < start + span; i++) {
                current += this.measured[i];
                if (this.tracks.isEmpty() || this.tracks.get(i) instanceof GridLength.Auto) {
                    autos++;
                }
            }
            if (need <= current || autos == 0) {
                return;
            }
            final float extra = (need - current) / autos;
            for (int i = start; i < start + span; i++) {
                if (this.tracks.isEmpty() || this.tracks.get(i) instanceof GridLength.Auto) {
                    this.measured[i] += extra;
                }
            }
        }

        private float measuredLength(final float spacing) {
            float total = spacing * (this.count - 1);
            for (int i = 0; i < this.count; i++) {
                total += this.measured[i];
            }
            return total;
        }

        /**
         * Works out the arranged sizes from the measured ones: the weighted tracks share the space the others leave,
         * and the single track of an axis without definitions fills the space.
         */
        private void distribute(final float space) {
            System.arraycopy(this.measured, 0, this.sizes, 0, this.count);
            if (this.tracks.isEmpty()) {
                this.sizes[0] = Math.max(this.measured[0], space);
                return;
            }
            float used = 0f;
            boolean weighted = false;
            this.shares.reset(this.count);
            for (int i = 0; i < this.count; i++) {
                if (this.tracks.get(i) instanceof GridLength.Weighted track) {
                    this.shares.set(i, track.weight(), this.mins[i], this.max(i));
                    weighted = true;
                } else {
                    used += this.sizes[i];
                }
            }
            if (!weighted) {
                return;
            }
            this.shares.resolve(space - used);
            for (int i = 0; i < this.count; i++) {
                if (this.tracks.get(i) instanceof GridLength.Weighted) {
                    this.sizes[i] = this.shares.size(i);
                }
            }
        }

        private void offsets(final float start, final float spacing) {
            float position = start;
            for (int i = 0; i < this.count; i++) {
                this.offsets[i] = position;
                position += this.sizes[i] + spacing;
            }
        }
    }
}
