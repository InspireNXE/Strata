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
package org.inspirenxe.strata.core.control.collection;

import org.inspirenxe.strata.core.layout.Flow;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.HasTooltip;
import org.inspirenxe.strata.api.component.Tooltip;
import org.inspirenxe.strata.api.control.attribute.item.CellEditor;
import org.inspirenxe.strata.api.control.attribute.item.SelectionMode;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.collection.GridView;
import org.inspirenxe.strata.api.event.input.CharEvent;
import org.inspirenxe.strata.api.event.input.DragEvent;
import org.inspirenxe.strata.api.event.input.FocusEvent;
import org.inspirenxe.strata.api.event.input.KeyEvent;
import org.inspirenxe.strata.api.event.input.PointerButtonEvent;
import org.inspirenxe.strata.api.event.input.ScrollEvent;
import org.inspirenxe.strata.api.extension.ArrangeContext;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifier;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.layout.ScrollPanel;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.core.layout.ScrollTrack;
import org.inspirenxe.strata.core.layout.ThumbDrag;
import org.inspirenxe.strata.core.node.BuiltInComposite;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;
import org.inspirenxe.strata.core.node.Root;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

/**
 * The core {@link GridView}. Each cell in view is a private child component, a slot, holding the component its cell
 * factory made, so each can have its own hover and tooltip. Slots are built during arrange for the rows the viewport
 * shows and dropped when their row scrolls out, as the list builds its rows; a grid with room for every row builds
 * every slot. The hover, selection and focus paints are drawn over that component. The scroll bar is drawn by the
 * grid in a gutter right of the viewport, with the same geometry as a list's, and a wheel notch scrolls one cell and
 * its spacing.
 *
 * @param <T> the type of the items
 */
public final class GridViewImpl<T> extends BuiltInComposite<GridView<T>> implements GridView<T> {
    /**
     * How far a slot's content sits in from its edges when the theme sets no {@value #STYLE_CELL_PADDING}: 1 on
     * every side.
     */
    public static final Insets DEFAULT_CELL_PADDING = Insets.of(1f);
    private static final Set<String> REQUIRED = Set.of(STYLE_CELL);
    private static final float GUTTER = ScrollPanel.BAR_WIDTH + ScrollPanel.BAR_GAP;

    private final ComponentFactoryImpl ui;
    // The slots built for the rows in view, by cell index
    private final Map<Integer, Slot<T>> slots;
    // The items, changed in place by setItem, and the copy items() last handed out, or null once they changed
    private final List<T> items;
    private List<T> shown;
    private Function<? super T, ? extends Component<?>> cellFactory;
    private Function<? super T, ?> keyFunction;
    private boolean activateOnClick;
    private Function<? super T, Tooltip> tooltips;
    private int columns;
    private float cellSize;
    private float spacing;
    private boolean selectable;
    private boolean slotsStale;
    // Editing in place, and the cell kept built while edited
    private CellEditor<T> cellEditor;
    private final CellEditSession session;
    private int pinned;

    // Selection: the current cell, the selected set as indexes and as bits for painting, and the range anchor
    private int selectedIndex;
    private final TreeSet<Integer> chosen;
    private final BitSet chosenBits;
    private SelectionMode mode;
    private int anchor;
    // The cell the keys move from, ringed while the grid has focus
    private int cursor;

    // Scrolling
    private final ScrollTrack bar;
    private final ThumbDrag thumbDrag;
    private float scrollY;
    private float maxScrollY;
    private boolean arranged;
    private boolean barShown;
    private Rect viewport;
    private Rect thumbRect;
    private int reveal;

    // Theme
    private Paint cellPaint;
    private Insets cellPadding = DEFAULT_CELL_PADDING;
    private Paint hoveredPaint;
    private Paint selectedPaint;
    private Paint focusPaint = Paint.NONE;
    private Paint hoveredBackPaint = Paint.NONE;
    private Insets highlightInsets = Insets.NONE;
    private Paint thumb = Paint.NONE;
    private Paint thumbHovered = Paint.NONE;
    private Paint track = Paint.NONE;

    /**
     * Creates a grid with no cells.
     *
     * @param ui the factory that owns the grid
     * @param cellFactory makes the component shown in a filled slot
     */
    @SuppressWarnings("this-escape")
    public GridViewImpl(final ComponentFactoryImpl ui, final Function<? super T, ? extends Component<?>> cellFactory) {
        super(ui);
        this.ui = ui;
        this.slots = new HashMap<>();
        this.items = new ArrayList<>();
        this.cellFactory = Objects.requireNonNull(cellFactory, "cellFactory");
        this.keyFunction = Function.identity();
        this.columns = DEFAULT_COLUMNS;
        this.cellSize = DEFAULT_CELL_SIZE;
        this.selectedIndex = -1;
        this.chosen = new TreeSet<>();
        this.chosenBits = new BitSet();
        this.mode = SelectionMode.SINGLE;
        this.anchor = -1;
        this.cursor = -1;
        this.session = new CellEditSession(ui);
        this.pinned = -1;
        this.bar = new ScrollTrack(true);
        this.thumbDrag = new ThumbDrag(this.bar, true);
        this.viewport = Rect.ZERO;
        this.thumbRect = Rect.ZERO;
        this.reveal = -1;
        this.cellPaint = Paint.NONE;
        this.hoveredPaint = Paint.NONE;
        this.selectedPaint = Paint.NONE;
    }

    // ------------------------------------------------------------------
    // Items and slots
    // ------------------------------------------------------------------

    @Override
    public List<T> items() {
        if (this.shown == null) {
            this.shown = Collections.unmodifiableList(new ArrayList<>(this.items));
        }
        return this.shown;
    }

    @Override
    public GridView<T> setItems(final List<? extends T> items) {
        final List<T> next = new ArrayList<>(items);
        this.session.cancel();
        this.items.clear();
        this.items.addAll(next);
        this.shown = null;
        this.rebuild();
        final int count = this.items.size();
        if (this.cursor >= count) {
            this.cursor = count - 1;
        }
        if (this.anchor >= count) {
            this.anchor = -1;
        }
        if (!this.chosen.isEmpty() && this.chosen.last() >= count || this.selectedIndex >= count) {
            final TreeSet<Integer> kept = new TreeSet<>(this.chosen.headSet(count));
            this.setSelection(kept, this.selectedIndex < count ? this.selectedIndex : this.currentFor(kept, -1));
        }
        return this;
    }

    @Override
    public T item(final int index) {
        return this.items.get(index);
    }

    @Override
    public GridView<T> setItem(final int index, final T item) {
        Objects.checkIndex(index, this.items.size());
        if (index == this.pinned) {
            this.session.cancel();
        }
        this.items.set(index, item);
        this.shown = null;
        final Slot<T> slot = this.slots.get(index);
        if (slot != null) {
            slot.fill(item);
        }
        return this;
    }

    @Override
    public GridView<T> refresh() {
        this.rebuild();
        return this;
    }

    @Override
    public GridView<T> refresh(final T item) {
        if (item == null) {
            return this;
        }
        final Object key = this.keyFunction.apply(item);
        for (final Slot<T> slot : this.slots.values()) {
            final T held = this.items.get(slot.index);
            if (held != null && Objects.equals(key, this.keyFunction.apply(held))) {
                slot.fill(held);
            }
        }
        return this;
    }

    @Override
    public GridView<T> setKeyFunction(final Function<? super T, ?> keyFunction) {
        this.keyFunction = Objects.requireNonNull(keyFunction, "keyFunction");
        return this;
    }

    @Override
    public GridView<T> setCellFactory(final Function<? super T, ? extends Component<?>> cellFactory) {
        this.cellFactory = Objects.requireNonNull(cellFactory, "cellFactory");
        this.rebuild();
        return this;
    }

    @Override
    public GridView<T> setTooltips(final Function<? super T, Tooltip> tooltips) {
        this.tooltips = tooltips;
        this.rebuild();
        return this;
    }

    /**
     * Drops every slot; the next arrange builds the slots in view again.
     */
    private void rebuild() {
        this.session.cancel();
        this.slotsStale = true;
        this.invalidateLayout();
    }

    // ------------------------------------------------------------------
    // Editing in place
    // ------------------------------------------------------------------

    @Override
    public GridView<T> setCellEditor(final CellEditor<T> editor) {
        this.cellEditor = editor;
        return this;
    }

    @Override
    public boolean isEditing() {
        return this.session.isEditing();
    }

    @Override
    public GridView<T> cancelEdit() {
        this.session.cancel();
        return this;
    }

    /**
     * Starts an edit of a filled cell for the user: asks {@code EDIT_CELL} and, if no handler cancels it, puts the
     * editor in the cell's slot, keeps it built and focuses the editor.
     *
     * @return true if the cell is editable, so the trigger is used up even when a handler cancels the edit
     */
    private boolean startEdit(final int index, final int codePoint, final Modifiers modifiers) {
        final CellEditor<T> editor = this.cellEditor;
        if (editor == null || index < 0 || index >= this.items.size() || this.items.get(index) == null) {
            return false;
        }
        final T item = this.items.get(index);
        this.request(EDIT_CELL, item, value -> {
            final Slot<T> slot = this.slotFor(index);
            if (slot == null) {
                return;
            }
            slot.edit(this.session.begin(editor, item,
                    edited -> this.report(CELL_EDITED, (Object) edited, (Object) item),
                    () -> this.restore(index), this::requestFocus));
            this.pinned = index;
            this.session.focus(codePoint, modifiers);
        });
        return true;
    }

    /**
     * Returns the slot built for a cell, scrolling it into view and laying out first if it is not built yet.
     */
    private Slot<T> slotFor(final int index) {
        if (this.slotsStale || !this.slots.containsKey(index)) {
            this.scrollToIndex(index);
            final Root root = this.hostRoot();
            if (root != null && this.isAttached()) {
                root.layout();
            }
        }
        return this.slotsStale ? null : this.slots.get(index);
    }

    /**
     * Puts a cell's content back after an edit and stops keeping its slot built.
     */
    private void restore(final int index) {
        this.pinned = -1;
        final Slot<T> slot = this.slots.get(index);
        if (slot != null && index < this.items.size()) {
            slot.fill(this.items.get(index));
        }
        this.invalidateLayout();
    }

    @Override
    public int columns() {
        return this.columns;
    }

    @Override
    public GridView<T> setColumns(final int columns) {
        if (columns < 1) {
            throw new IllegalArgumentException("At least one column is needed: " + columns);
        }
        this.columns = columns;
        this.invalidateLayout();
        return this;
    }

    @Override
    public float cellSize() {
        return this.cellSize;
    }

    @Override
    public GridView<T> setCellSize(final float size) {
        if (!(size > 0f) || Float.isInfinite(size)) {
            throw new IllegalArgumentException("Cell size must be finite and greater than 0: " + size);
        }
        this.cellSize = size;
        this.invalidateLayout();
        return this;
    }

    @Override
    public float spacing() {
        return this.spacing;
    }

    @Override
    public GridView<T> setSpacing(final float spacing) {
        if (!(spacing >= 0f) || Float.isInfinite(spacing)) {
            throw new IllegalArgumentException("Spacing must be finite and not negative: " + spacing);
        }
        this.spacing = spacing;
        this.invalidateLayout();
        return this;
    }

    // ------------------------------------------------------------------
    // Selection
    // ------------------------------------------------------------------

    @Override
    public boolean isSelectable() {
        return this.selectable;
    }

    @Override
    public GridView<T> setSelectable(final boolean selectable) {
        this.selectable = selectable;
        return this;
    }

    @Override
    public int selectedIndex() {
        return this.selectedIndex;
    }

    @Override
    public GridView<T> select(final int index) {
        if (index != -1) {
            Objects.checkIndex(index, this.items.size());
        }
        this.setSelection(index < 0 ? List.of() : List.of(index), index);
        this.anchor = index;
        if (index >= 0) {
            this.scrollToIndex(index);
        }
        return this;
    }

    @Override
    public Optional<T> selected() {
        return this.selectedIndex < 0 ? Optional.empty() : Optional.ofNullable(this.items.get(this.selectedIndex));
    }

    @Override
    public GridView<T> selectItem(final T item) {
        return this.select(Items.indexOf(this.items, this.keyFunction, item));
    }

    @Override
    public SelectionMode selectionMode() {
        return this.mode;
    }

    @Override
    public GridView<T> setSelectionMode(final SelectionMode mode) {
        this.mode = Objects.requireNonNull(mode, "mode");
        if (mode == SelectionMode.SINGLE && this.chosen.size() > 1) {
            this.setSelection(this.selectedIndex < 0 ? List.of() : List.of(this.selectedIndex), this.selectedIndex);
        }
        return this;
    }

    @Override
    public List<Integer> selectedIndexes() {
        return List.copyOf(this.chosen);
    }

    @Override
    public List<T> selectedItems() {
        // A list that holds the nulls of empty cells, which List.copyOf refuses
        final List<T> selected = new ArrayList<>(this.chosen.size());
        for (final int index : this.chosen) {
            selected.add(this.items.get(index));
        }
        return Collections.unmodifiableList(selected);
    }

    @Override
    public GridView<T> selectIndexes(final Collection<Integer> indexes) {
        final TreeSet<Integer> set = new TreeSet<>();
        for (final Integer index : indexes) {
            set.add(Objects.checkIndex(Objects.requireNonNull(index, "index"), this.items.size()));
        }
        if (this.mode == SelectionMode.SINGLE && set.size() > 1) {
            throw new IllegalArgumentException("A grid in SINGLE mode selects at most one cell: " + set);
        }
        final int current = this.currentFor(set, this.selectedIndex);
        this.setSelection(set, current);
        this.anchor = current;
        return this;
    }

    @Override
    public GridView<T> selectItems(final Collection<? extends T> items) {
        final List<Integer> indexes = new ArrayList<>(items.size());
        for (final T item : items) {
            final int index = Items.indexOf(this.items, this.keyFunction, item);
            if (index >= 0) {
                indexes.add(index);
            }
        }
        return this.selectIndexes(indexes);
    }

    @Override
    public GridView<T> selectAll() {
        if (this.mode == SelectionMode.MULTIPLE && !this.items.isEmpty()) {
            this.selectIndexes(this.allCells());
        }
        return this;
    }

    private List<Integer> allCells() {
        final List<Integer> all = new ArrayList<>(this.items.size());
        for (int i = 0; i < this.items.size(); i++) {
            all.add(i);
        }
        return all;
    }

    /**
     * The current cell for a new set: the wanted one if it is in the set, else the set's last cell, else none.
     */
    private int currentFor(final Collection<Integer> set, final int wanted) {
        if (set.contains(wanted)) {
            return wanted;
        }
        int last = -1;
        for (final int index : set) {
            last = Math.max(last, index);
        }
        return last;
    }

    /**
     * Replaces the selected set and the current cell, reporting the set first and then the current cell, each only
     * if it changed. The cursor follows a current cell.
     */
    private void setSelection(final Collection<Integer> set, final int current) {
        final List<Integer> before = List.copyOf(this.chosen);
        final int previous = this.selectedIndex;
        this.chosen.clear();
        this.chosen.addAll(set);
        this.chosenBits.clear();
        for (final int index : this.chosen) {
            this.chosenBits.set(index);
        }
        this.selectedIndex = current;
        if (current >= 0) {
            this.cursor = current;
        }
        final List<Integer> now = List.copyOf(this.chosen);
        if (!before.equals(now)) {
            this.report(SELECTED_MANY, now, before);
        }
        if (current != previous) {
            this.report(SELECTED, current, previous);
        }
    }

    /**
     * Returns the platform's shortcut modifier, which adds a clicked entry to the selection: the host's, or Control
     * while the control is not on a screen.
     */
    private Modifier shortcutModifier() {
        return this.hostRoot() == null ? Modifier.CONTROL : this.hostRoot().shortcutModifier();
    }

    /**
     * Picks a cell for the user: in {@link SelectionMode#MULTIPLE} mode alone, added or removed with the shortcut
     * modifier, or as the range from the anchor with Shift, through {@code SELECT_MANY}; otherwise through
     * {@code SELECT} if the current cell changes.
     */
    private void userPick(final int index, final boolean control, final boolean shift) {
        if (this.mode != SelectionMode.MULTIPLE) {
            if (index != this.selectedIndex) {
                this.request(SELECT, index, this::applySelect);
            }
            return;
        }
        final TreeSet<Integer> set;
        final int current;
        if (shift) {
            final int from = this.anchor >= 0 ? this.anchor : index;
            set = new TreeSet<>();
            for (int i = Math.min(from, index); i <= Math.max(from, index); i++) {
                set.add(i);
            }
            this.anchor = from;
            current = index;
        } else if (control) {
            set = new TreeSet<>(this.chosen);
            final boolean added = set.add(index);
            if (!added) {
                set.remove(index);
            }
            this.anchor = index;
            current = added ? index : this.currentFor(set, this.selectedIndex);
        } else {
            set = new TreeSet<>(List.of(index));
            this.anchor = index;
            current = index;
        }
        this.userSelectMany(set, current);
    }

    /**
     * Asks {@code SELECT_MANY} for a set the user is about to select, unless it is the selection already.
     */
    private void userSelectMany(final Collection<Integer> set, final int current) {
        final List<Integer> wanted = List.copyOf(new TreeSet<>(set));
        if (wanted.equals(List.copyOf(this.chosen)) && current == this.selectedIndex) {
            return;
        }
        this.request(SELECT_MANY, wanted, indexes -> this.applyMany(indexes, current));
    }

    /**
     * Applies a set a request ended with, ignoring indexes that are not cells. A null value is ignored.
     */
    private void applyMany(final List<Integer> indexes, final int wanted) {
        if (indexes == null) {
            return;
        }
        final TreeSet<Integer> set = new TreeSet<>();
        for (final Integer index : indexes) {
            if (Items.isIndex(index, this.items.size())) {
                set.add(index);
            }
        }
        final int current = this.currentFor(set, wanted);
        this.setSelection(set, current);
        if (current >= 0) {
            this.scrollToIndex(current);
        }
    }

    /**
     * Selects the cell a request ended with, or clears the selection for -1. A null or any other index outside the
     * cells is ignored.
     */
    private void applySelect(final Integer index) {
        if (Items.isIndexOrNone(index, this.items.size())) {
            this.select(index);
        }
    }

    @Override
    public GridView<T> setActivateOnClick(final boolean onClick) {
        this.activateOnClick = onClick;
        return this;
    }

    @Override
    public int hoveredIndex() {
        for (final Slot<T> slot : this.slots.values()) {
            if (this.nodeOf(slot).isHoverWithin()) {
                return slot.index;
            }
        }
        return -1;
    }

    /**
     * Handles a click on a cell: the cursor moves there, a left click asks to select it when the grid is selectable,
     * then the click itself is asked for and reported, then a left double click, or a single one with activate on
     * click, asks to activate the cell. If a selection handler replaced the items so that the cell is gone, the click
     * stops there.
     */
    private void clicked(final int index, final PointerButtonEvent event) {
        final boolean left = event.button() == MouseButton.LEFT;
        this.cursor = index;
        if (this.selectable && left) {
            this.userPick(index, event.modifiers().has(this.shortcutModifier()), event.modifiers().shift());
        }
        if (index >= this.items.size()) {
            // A selection handler replaced the items and the cell went, so the rest of the click goes with it
            return;
        }
        this.request(CLICK, new Click<>(index, this.items.get(index), event.button(), event.modifiers()),
                this::applyClick);
        if (left && event.clicks() == 2 && !this.activateOnClick
                && this.startEdit(index, -1, event.modifiers())) {
            return;
        }
        if (left && event.clicks() == (this.activateOnClick ? 1 : 2)) {
            this.request(ACTIVATE, index, this::applyActivate);
        }
    }

    /**
     * Reports the activation a request ended with. A null or an index outside the cells is ignored.
     */
    private void applyActivate(final Integer index) {
        if (Items.isIndex(index, this.items.size())) {
            this.report(ACTIVATED, index, index);
        }
    }

    /**
     * Reports the click a request ended with. A null click or one whose index is not a cell's is ignored.
     */
    private void applyClick(final Click<?> click) {
        if (click != null && click.index() >= 0 && click.index() < this.items.size()) {
            this.report(CLICKED, click, click);
        }
    }

    // ------------------------------------------------------------------
    // Scrolling
    // ------------------------------------------------------------------

    @Override
    public float scrollY() {
        return this.scrollY;
    }

    @Override
    public GridView<T> setScrollY(final float scrollY) {
        if (Float.isNaN(scrollY)) {
            throw new IllegalArgumentException("A scroll offset must not be NaN");
        }
        this.scrollTo(scrollY);
        return this;
    }

    @Override
    public GridView<T> scrollToIndex(final int index) {
        Objects.checkIndex(index, this.items.size());
        this.reveal = index;
        this.invalidateArrangement();
        return this;
    }

    /**
     * Moves to an offset, at least 0, and once the grid has been laid out at most what the current rows need in the
     * last viewport. The next arrange clamps it again.
     */
    private boolean scrollTo(final float scrollY) {
        final float max = this.arranged ? Math.max(0f, this.totalHeight() - this.viewport.height()) : Float.MAX_VALUE;
        final float next = Math.max(0f, Math.min(scrollY, max));
        if (next == this.scrollY) {
            return false;
        }
        this.scrollY = next;
        this.invalidateArrangement();
        return true;
    }

    private float step() {
        return this.cellSize + this.spacing;
    }

    private int rowCount() {
        return (this.items.size() + this.columns - 1) / this.columns;
    }

    private float totalHeight() {
        final int rows = this.rowCount();
        return rows == 0 ? 0f : rows * this.cellSize + (rows - 1) * this.spacing;
    }

    private float totalWidth() {
        return this.columns * this.cellSize + (this.columns - 1) * this.spacing;
    }

    // ------------------------------------------------------------------
    // Theme and layout
    // ------------------------------------------------------------------

    @Override
    protected Set<String> requiredStyleKeys() {
        return REQUIRED;
    }

    @Override
    protected void onApplyTheme(final Theme theme) {
        this.cellPaint = theme.paintOr(STYLE_CELL, Paint.NONE);
        this.hoveredPaint = theme.paintOr(STYLE_CELL_HOVERED, Paint.NONE);
        this.selectedPaint = theme.paintOr(STYLE_CELL_SELECTED, this.hoveredPaint);
        this.focusPaint = theme.paintOr(STYLE_FOCUS, theme.paintOr(Button.STYLE_FOCUS, Paint.NONE));
        this.hoveredBackPaint = theme.paintOr(STYLE_CELL_HOVERED_BACK, Paint.NONE);
        this.highlightInsets = theme.insetsOr(STYLE_CELL_HIGHLIGHT, Insets.NONE);
        this.cellPadding = theme.insetsOr(STYLE_CELL_PADDING, DEFAULT_CELL_PADDING);
        this.thumb = theme.paintOr(ScrollPanel.STYLE_THUMB, Paint.NONE);
        this.thumbHovered = theme.paintOr(ScrollPanel.STYLE_THUMB_HOVERED, this.thumb);
        this.track = theme.paintOr(ScrollPanel.STYLE_TRACK, Paint.NONE);
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        for (final Slot<T> slot : this.slots.values()) {
            context.measure(slot, this.cellSize, this.cellSize);
        }
        if (this.items.isEmpty()) {
            return Size.ZERO;
        }
        final float height = this.totalHeight();
        final boolean overflow = !Float.isInfinite(availableHeight) && height > availableHeight;
        return new Size(this.totalWidth() + (overflow ? GUTTER : 0f),
                overflow ? availableHeight : height);
    }

    @Override
    protected void arrange(final ArrangeContext context, final Rect bounds) {
        final float total = this.totalHeight();
        this.barShown = total > bounds.height();
        this.viewport = new Rect(bounds.x(), bounds.y(),
                Math.max(0f, bounds.width() - (this.barShown ? GUTTER : 0f)), bounds.height());
        this.maxScrollY = Math.max(0f, total - this.viewport.height());
        final float step = this.step();
        if (this.reveal >= 0 && this.reveal < this.items.size()) {
            final float top = (this.reveal / this.columns) * step;
            if (top < this.scrollY) {
                this.scrollY = top;
            } else if (top + this.cellSize > this.scrollY + this.viewport.height()) {
                this.scrollY = top + this.cellSize - this.viewport.height();
            }
        }
        this.reveal = -1;
        this.scrollY = Math.max(0f, Math.min(this.scrollY, this.maxScrollY));
        this.arranged = true;

        if (this.slotsStale) {
            this.slotsStale = false;
            this.slots.clear();
            this.clearChildren();
        }
        final int count = this.items.size();
        final int first;
        final int last;
        if (count == 0 || this.viewport.height() <= 0f) {
            first = 0;
            last = -1;
        } else {
            final int firstRow = Math.max(0, (int) Math.floor(this.scrollY / step));
            final int lastRow = (int) Math.floor((this.scrollY + this.viewport.height()) / step);
            first = firstRow * this.columns;
            last = Math.min(count - 1, (lastRow + 1) * this.columns - 1);
        }
        for (final Iterator<Map.Entry<Integer, Slot<T>>> it = this.slots.entrySet().iterator(); it.hasNext(); ) {
            final Map.Entry<Integer, Slot<T>> entry = it.next();
            if ((entry.getKey() < first || entry.getKey() > last) && entry.getKey() != this.pinned) {
                it.remove();
                this.removeChild(entry.getValue());
            }
        }
        final float offset = Math.round(this.scrollY);
        for (int i = first; i <= last; i++) {
            Slot<T> slot = this.slots.get(i);
            if (slot == null) {
                slot = new Slot<>(this.ui, this, i);
                this.slots.put(i, slot);
                this.addChild(slot);
                slot.fill(this.items.get(i));
                context.measure(slot, this.cellSize, this.cellSize);
            }
            final int column = i % this.columns;
            final int row = i / this.columns;
            context.arrange(slot, new Rect(bounds.x() + column * step, bounds.y() + row * step - offset,
                    this.cellSize, this.cellSize));
        }
        // A cell kept built for an edit follows the scrolling outside the viewport, where the clip hides it
        final Slot<T> kept = this.pinned >= 0 && (this.pinned < first || this.pinned > last)
                ? this.slots.get(this.pinned) : null;
        if (kept != null) {
            context.arrange(kept, new Rect(bounds.x() + (this.pinned % this.columns) * step,
                    bounds.y() + (this.pinned / this.columns) * step - offset, this.cellSize, this.cellSize));
        }
        // Right to left the bar sits at the left; the node mirrors the cells
        this.bar.update(this.flow(new Rect(bounds.right() - ScrollPanel.BAR_WIDTH, bounds.y(), ScrollPanel.BAR_WIDTH,
                bounds.height())), this.viewport.height(), Math.max(total, this.viewport.height()));
        this.thumbRect = this.bar.thumb(this.scrollY, this.maxScrollY);
    }

    @Override
    protected Rect childClip() {
        return this.flow(this.viewport);
    }

    @Override
    protected void paintOverChildren(final Painter painter, final Rect bounds) {
        if (this.barShown) {
            this.track.paint(painter, this.bar.track());
            (this.thumbDrag.isDragging() ? this.thumbHovered : this.thumb).paint(painter, this.thumbRect);
        }
    }

    // ------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------

    @Override
    protected void onFocusGained(final FocusEvent event) {
        if (this.cursor < 0 && !this.items.isEmpty()) {
            this.cursor = this.selectedIndex >= 0 ? this.selectedIndex : 0;
        }
    }

    @Override
    protected void onPointerPress(final PointerButtonEvent event) {
        this.thumbDrag.release();
        if (event.button() != MouseButton.LEFT || !this.barShown) {
            return;
        }
        final float page = Math.max(this.step(), this.viewport.height() - this.step());
        final float next = this.thumbDrag.press(event.x(), event.y(), this.scrollY, this.maxScrollY, page);
        if (!Float.isNaN(next)) {
            this.scrollTo(next);
            event.consume();
        }
    }

    @Override
    protected void onDragStart(final DragEvent event) {
        this.onDrag(event);
    }

    @Override
    protected void onDrag(final DragEvent event) {
        if (this.thumbDrag.isDragging()) {
            this.scrollTo(this.thumbDrag.drag(event.x(), event.y(), this.maxScrollY));
            event.consume();
        }
    }

    @Override
    protected void onDragEnd(final DragEvent event) {
        this.thumbDrag.release();
    }

    @Override
    protected void onPointerRelease(final PointerButtonEvent event) {
        if (event.button() == MouseButton.LEFT) {
            this.thumbDrag.release();
        }
    }

    @Override
    protected void onScroll(final ScrollEvent event) {
        if (this.scrollTo(this.scrollY - event.scrollY() * this.step())) {
            event.consume();
        }
    }

    @Override
    protected void onChar(final CharEvent event) {
        final int codePoint = event.codePoint();
        if (!event.modifiers().control() && !event.modifiers().alt() && !Character.isISOControl(codePoint)
                && !Character.isWhitespace(codePoint) && this.startEdit(this.cursor, codePoint, event.modifiers())) {
            event.consume();
        }
    }

    @Override
    protected void onKey(final KeyEvent event) {
        final int count = this.items.size();
        if (!event.action().isDown() || event.modifiers().alt() || count == 0) {
            return;
        }
        final Key key = event.key();
        final boolean control = event.modifiers().control();
        final boolean press = event.action() == KeyAction.PRESS;
        final int from = Math.min(Math.max(this.cursor, 0), count - 1);
        if (event.modifiers().has(event.shortcutModifier()) && key == Key.A) {
            if (press && this.selectable && this.mode == SelectionMode.MULTIPLE) {
                this.userSelectMany(this.allCells(), from);
                event.consume();
            }
            return;
        }
        if ((control || event.modifiers().has(event.shortcutModifier())) && key != Key.HOME && key != Key.END) {
            return;
        }
        if (press && key == Key.F2 && !control) {
            if (this.startEdit(this.cursor, -1, event.modifiers())) {
                event.consume();
            }
            return;
        }
        if (press && (key.isEnter() || key == Key.SPACE)) {
            if (this.cursor >= 0) {
                this.request(ACTIVATE, this.cursor, this::applyActivate);
            }
            event.consume();
            return;
        }
        final int columns = this.columns;
        final int rowStart = from - from % columns;
        final int page = Math.max(1, (int) Math.floor((this.viewport.height() + this.spacing) / this.step()));
        // Right to left the cells run from the right, so Left goes to the next one
        final int next = switch (Flow.key(key, this.isRightToLeft())) {
            case LEFT -> Math.max(0, from - 1);
            case RIGHT -> Math.min(count - 1, from + 1);
            case UP -> from >= columns ? from - columns : from;
            case DOWN -> from + columns < count ? from + columns : from;
            case HOME -> control ? 0 : rowStart;
            case END -> control ? count - 1 : Math.min(count - 1, rowStart + columns - 1);
            case PAGE_UP -> Math.max(from % columns, from - page * columns);
            case PAGE_DOWN -> this.pageDown(from, page);
            default -> -1;
        };
        if (next < 0) {
            return;
        }
        this.cursor = next;
        this.scrollToIndex(next);
        if (this.selectable) {
            this.userPick(next, false, event.modifiers().shift());
        }
        event.consume();
    }

    /**
     * Returns the cell a page below another in the same column, or the lowest cell of that column if the page runs
     * past the end.
     */
    private int pageDown(final int from, final int page) {
        int next = from + page * this.columns;
        while (next >= this.items.size()) {
            next -= this.columns;
        }
        return Math.max(from, next);
    }

    /**
     * One slot and its cell, if it holds an item.
     */
    private static final class Slot<T> extends BuiltInComposite<Slot<T>> implements HasTooltip<Slot<T>> {
        private final GridViewImpl<T> grid;
        private final int index;
        private Component<?> content;
        // Where the hovered, selected and focus paints go, and the bounds and insets it was made for
        private Rect highlight;
        private Rect highlightBounds;
        private Insets highlightInsets;

        Slot(final ComponentFactoryImpl ui, final GridViewImpl<T> grid, final int index) {
            super(ui);
            this.grid = grid;
            this.index = index;
        }

        /**
         * Puts an editor in place of the content.
         */
        void edit(final Component<?> editor) {
            if (this.content != null) {
                this.removeChild(this.content);
            }
            this.content = editor;
            this.addContent(editor);
            this.invalidateLayout();
        }

        void fill(final T item) {
            if (this.content != null) {
                this.removeChild(this.content);
                this.content = null;
            }
            if (item != null) {
                this.content = this.grid.cellFactory.apply(item);
                this.addContent(this.content);
            }
            final Tooltip shown = item == null || this.grid.tooltips == null ? null : this.grid.tooltips.apply(item);
            if (shown == null) {
                this.clearTooltip();
            } else {
                this.setTooltip(shown);
            }
            this.invalidateLayout();
        }

        @Override
        protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
            if (this.content != null) {
                final Insets padding = this.grid.cellPadding;
                context.measure(this.content, Math.max(0f, availableWidth - padding.horizontal()),
                        Math.max(0f, availableHeight - padding.vertical()));
            }
            return new Size(this.grid.cellSize, this.grid.cellSize);
        }

        @Override
        protected void arrange(final ArrangeContext context, final Rect bounds) {
            if (this.content != null) {
                context.arrange(this.content, bounds.shrink(this.grid.cellPadding));
            }
        }

        /**
         * Returns the area the hovered and selected paints cover, made again only when the bounds or the theme's
         * insets changed.
         */
        private Rect highlight(final Rect bounds) {
            final Insets insets = this.grid.highlightInsets;
            if (this.highlight == null || this.highlightBounds != bounds || this.highlightInsets != insets) {
                this.highlight = bounds.shrink(insets);
                this.highlightBounds = bounds;
                this.highlightInsets = insets;
            }
            return this.highlight;
        }

        @Override
        protected void paint(final Painter painter, final Rect bounds) {
            this.grid.cellPaint.paint(painter, bounds);
            if (this.grid.hoveredBackPaint != Paint.NONE && this.isHoverWithin()) {
                this.grid.hoveredBackPaint.paint(painter, this.highlight(bounds));
            }
        }

        @Override
        protected void paintOverChildren(final Painter painter, final Rect bounds) {
            if (this.grid.chosenBits.get(this.index)) {
                this.grid.selectedPaint.paint(painter, this.highlight(bounds));
            } else if (this.isHoverWithin()) {
                this.grid.hoveredPaint.paint(painter, this.highlight(bounds));
            }
            if (this.index == this.grid.cursor && this.grid.isFocused()) {
                this.grid.focusPaint.paint(painter, bounds);
            }
        }

        @Override
        protected void onClick(final PointerButtonEvent event) {
            this.grid.clicked(this.index, event);
            event.consume();
        }
    }
}
