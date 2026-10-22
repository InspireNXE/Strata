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

import org.inspirenxe.strata.api.animation.Animator;
import org.inspirenxe.strata.api.appearance.Appearances;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.control.attribute.item.SelectionMode;
import org.inspirenxe.strata.api.control.collection.ListView;
import org.inspirenxe.strata.api.event.input.CharEvent;
import org.inspirenxe.strata.api.event.input.DragEvent;
import org.inspirenxe.strata.api.event.input.KeyEvent;
import org.inspirenxe.strata.api.event.input.PointerButtonEvent;
import org.inspirenxe.strata.api.event.input.PointerEvent;
import org.inspirenxe.strata.api.event.input.ScrollEvent;
import org.inspirenxe.strata.api.extension.ArrangeContext;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.layout.ScrollPanel;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.core.control.Caption;
import org.inspirenxe.strata.core.control.Motion;
import org.inspirenxe.strata.core.layout.ScrollTrack;
import org.inspirenxe.strata.core.layout.ThumbDrag;
import org.inspirenxe.strata.core.node.BuiltInComposite;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;
import org.inspirenxe.strata.core.text.TextLine;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntUnaryOperator;

/**
 * The core {@link ListView}. Rows are built during arrange, once the viewport is known: rows that scrolled out are
 * removed, rows that scrolled in are built, measured and placed, and the change schedules one more layout. Rows sit
 * inside the list's border widths. A wheel notch scrolls one row and its spacing. Arrange also makes the rectangles
 * painting reads, the rows', the edges' and the thumb's, so a frame with nothing new to draw allocates nothing.
 *
 * <p>A wheel notch and {@link #scrollToIndex}, which the arrow keys use, glide to their offset over the theme's
 * {@value ScrollPanel#STYLE_SMOOTH} time through a {@link Motion}; a further notch adds to where the list is headed.
 * {@link #setScrollY} and the thumb move at once, as everything does when the time is 0, the list is detached, or it
 * has not been laid out yet.</p>
 *
 * @param <T> the type of the items
 */
public final class ListViewImpl<T> extends BuiltInComposite<ListView<T>> implements ListView<T> {
    /**
     * The narrowest a list asks to be, in UI units.
     */
    public static final float MIN_WIDTH = 40f;
    private static final Set<String> REQUIRED = Set.of(STYLE_ROW_SELECTED, ScrollPanel.STYLE_THUMB);
    private static final float GUTTER = ScrollPanel.BAR_WIDTH + ScrollPanel.BAR_GAP;

    private final Map<Integer, Component<?>> rows;
    private final ScrollTrack bar;
    private final ThumbDrag thumbDrag;
    private final Runnable drawRows;
    private List<T> items;
    private Function<? super T, ? extends Component<?>> cellFactory;
    private Function<? super T, ?> keyFunction;
    // What type-ahead matches, or null while it is off
    private Function<? super T, ?> typeAheadName;
    private final TypeAhead typeAhead;
    private float rowHeight;
    private float rowSpacing;
    private int selectedIndex;
    // The selected set, which is the current row alone, or nothing, in SINGLE mode
    private final TreeSet<Integer> chosen;
    private SelectionMode mode;
    private int anchor;
    private boolean deselectable;
    private boolean activateOnClick;
    private Text placeholder;
    private TextLine placeholderLine = TextLine.EMPTY;
    private boolean rowsStale;
    private int reveal;

    // Layout results
    private float scrollY;
    private long smoothMillis;
    private final Motion glide = new Motion(offset -> this.scrollTo(offset));
    private float maxScrollY;
    private boolean arranged;
    private boolean barShown;
    private Rect viewport;
    private float rowWidth;
    private float rowX;
    // Set for a list the factory made, which follows the theme's row width and edges
    private boolean standalone;

    // Made during arrange for painting: each built row's index and rectangle, its ring once first drawn, the edge
    // lines and the thumb
    private int[] paintIndex;
    private Rect[] paintRect;
    private Rect[][] paintRing;
    private int paintCount;
    private Rect[] edges;
    private Rect thumbRect;
    // The selected set as bits, so painting tests a row without boxing its index
    private final BitSet chosenBits;

    // Pointer
    private float pointerY;

    // Theme
    private Paint background;
    private Paint row;
    private Paint rowHovered;
    private Paint rowSelected;
    private Paint rowSelectedUnfocused;
    private Paint ring;
    private Paint ringUnfocused;
    private Paint edge;
    private Paint edgeOuter;
    private float rowLimit;
    private Insets padding = Insets.NONE;
    private float barGap = ScrollPanel.BAR_GAP;
    private float scrollRows = 1f;
    private Paint thumb;
    private Paint thumbHovered;
    private Paint track;
    private TextStyle placeholderStyle;

    // Set only while paint runs
    private Painter painter;

    // Set by a control that owns this list and fires the list's events as its own
    private IntConsumer selectRoute;
    private IntConsumer activateRoute;
    private SelectionReport selectedRoute;
    private ManyRequest selectManyRoute;
    private ManyReport selectedManyRoute;

    /**
     * Creates an empty list.
     *
     * @param ui the factory that owns the list
     * @param cellFactory makes the component for an item's row
     */
    @SuppressWarnings("this-escape")
    public ListViewImpl(final ComponentFactoryImpl ui, final Function<? super T, ? extends Component<?>> cellFactory) {
        super(ui);
        this.rows = new HashMap<>();
        this.bar = new ScrollTrack(true);
        this.thumbDrag = new ThumbDrag(this.bar, true);
        this.drawRows = this::drawRows;
        this.items = List.of();
        this.cellFactory = Objects.requireNonNull(cellFactory, "cellFactory");
        this.keyFunction = Function.identity();
        this.typeAheadName = Function.identity();
        this.typeAhead = new TypeAhead();
        this.rowHeight = DEFAULT_ROW_HEIGHT;
        this.selectedIndex = -1;
        this.chosen = new TreeSet<>();
        this.chosenBits = new BitSet();
        this.paintIndex = new int[0];
        this.paintRect = new Rect[0];
        this.paintRing = new Rect[0][];
        this.edges = new Rect[0];
        this.thumbRect = Rect.ZERO;
        this.mode = SelectionMode.SINGLE;
        this.anchor = -1;
        this.reveal = -1;
        this.viewport = Rect.ZERO;
        this.background = Paint.NONE;
        this.row = Paint.NONE;
        this.rowHovered = Paint.NONE;
        this.rowSelected = Paint.NONE;
        this.thumb = Paint.NONE;
        this.thumbHovered = Paint.NONE;
        this.track = Paint.NONE;
        this.placeholderStyle = TextStyle.DEFAULT;
        this.placeholder = Text.empty();
    }

    // ------------------------------------------------------------------
    // ListView
    // ------------------------------------------------------------------

    @Override
    public List<T> items() {
        return this.items;
    }

    @Override
    public ListView<T> setItems(final List<? extends T> items) {
        final List<T> next = Items.copyOf(items);
        this.replaceItems(next, this.keyMoves(next));
        return this;
    }

    /**
     * Works out where the selected rows move when the items become {@code next}, by key, as
     * {@link Items#moves} describes. Changes nothing, so an owner can run it before it changes its own state.
     *
     * @param next the new items
     * @return the new index of each selected row, or -1
     */
    IntUnaryOperator keyMoves(final List<T> next) {
        final Set<Integer> rows = new HashSet<>(this.chosen);
        if (this.selectedIndex >= 0) {
            rows.add(this.selectedIndex);
        }
        return Items.moves(this.items, next, this.keyFunction, rows);
    }

    /**
     * Replaces the items and moves each selected row where {@code moves} says, dropping rows it sends to -1. The
     * current row moves too, or becomes the last selected row if it went. Calls nothing a developer wrote before the
     * new state is in place, then reports {@code SELECTED_MANY} and {@code SELECTED} as {@link #setItems} does.
     *
     * @param next the new items, which the list keeps as they are
     * @param moves the new index of each selected row, or -1
     */
    void replaceItems(final List<T> next, final IntUnaryOperator moves) {
        final Optional<T> before = this.selected();
        final int beforeIndex = this.selectedIndex;
        final List<Integer> beforeSet = List.copyOf(this.chosen);
        final List<T> beforeItems = this.selectedItems();
        final TreeSet<Integer> set = new TreeSet<>();
        for (final int row : this.chosen) {
            final int moved = moves.applyAsInt(row);
            if (moved >= 0) {
                set.add(moved);
            }
        }
        int index = beforeIndex < 0 ? -1 : moves.applyAsInt(beforeIndex);
        if (index < 0 && !set.isEmpty()) {
            index = set.last();
        }
        if (index >= 0 && this.mode == SelectionMode.SINGLE) {
            set.clear();
            set.add(index);
        }
        this.items = next;
        this.chosen.clear();
        this.chosen.addAll(set);
        this.chosenChanged();
        this.selectedIndex = index;
        this.anchor = index;
        this.rowsStale = true;
        this.invalidateLayout();
        if (!beforeSet.equals(List.copyOf(this.chosen)) || !beforeItems.equals(this.selectedItems())) {
            this.manyChanged(beforeSet);
        }
        if (index != beforeIndex || !this.selected().equals(before)) {
            this.selectionChanged(beforeIndex);
        }
    }

    @Override
    public ListView<T> refresh() {
        this.rowsStale = true;
        this.invalidateLayout();
        return this;
    }

    /**
     * Returns the components built for the rows in view, for an owner that lays them out again or measures them.
     */
    Collection<Component<?>> builtRows() {
        return this.rows.values();
    }

    @Override
    public ListView<T> refresh(final T item) {
        final Object key = this.keyFunction.apply(item);
        boolean dropped = false;
        for (final Iterator<Map.Entry<Integer, Component<?>>> it = this.rows.entrySet().iterator(); it.hasNext(); ) {
            final Map.Entry<Integer, Component<?>> entry = it.next();
            if (Objects.equals(key, this.keyFunction.apply(this.items.get(entry.getKey())))) {
                it.remove();
                this.removeChild(entry.getValue());
                dropped = true;
            }
        }
        if (dropped) {
            // The next arrange builds the dropped rows again, as rows scrolled into view
            this.invalidateLayout();
        }
        return this;
    }

    @Override
    public ListView<T> setCellFactory(final Function<? super T, ? extends Component<?>> cellFactory) {
        this.cellFactory = Objects.requireNonNull(cellFactory, "cellFactory");
        return this.refresh();
    }

    @Override
    public ListView<T> setKeyFunction(final Function<? super T, ?> keyFunction) {
        this.keyFunction = Objects.requireNonNull(keyFunction, "keyFunction");
        return this;
    }

    @Override
    public ListView<T> setTypeAhead(final Function<? super T, ?> name) {
        this.typeAheadName = name;
        this.typeAhead.reset();
        return this;
    }

    @Override
    public float rowHeight() {
        return this.rowHeight;
    }

    @Override
    public ListView<T> setRowHeight(final float height) {
        if (!(height > 0f) || Float.isInfinite(height)) {
            throw new IllegalArgumentException("Row height must be finite and greater than 0: " + height);
        }
        this.rowHeight = height;
        return this.refresh();
    }

    @Override
    public float rowSpacing() {
        return this.rowSpacing;
    }

    @Override
    public ListView<T> setRowSpacing(final float spacing) {
        if (!(spacing >= 0f) || Float.isInfinite(spacing)) {
            throw new IllegalArgumentException("Row spacing must be finite and not negative: " + spacing);
        }
        this.rowSpacing = spacing;
        return this.refresh();
    }

    @Override
    public int selectedIndex() {
        return this.selectedIndex;
    }

    @Override
    public Optional<T> selected() {
        return this.selectedIndex < 0 ? Optional.empty() : Optional.ofNullable(this.items.get(this.selectedIndex));
    }

    @Override
    public ListView<T> select(final int index) {
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
    public SelectionMode selectionMode() {
        return this.mode;
    }

    @Override
    public ListView<T> setSelectionMode(final SelectionMode mode) {
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
        final List<T> selected = new ArrayList<>(this.chosen.size());
        for (final int index : this.chosen) {
            selected.add(this.items.get(index));
        }
        return selected;
    }

    @Override
    public ListView<T> selectIndexes(final Collection<Integer> indexes) {
        final TreeSet<Integer> set = new TreeSet<>();
        for (final Integer index : indexes) {
            set.add(Objects.checkIndex(Objects.requireNonNull(index, "index"), this.items.size()));
        }
        if (this.mode == SelectionMode.SINGLE && set.size() > 1) {
            throw new IllegalArgumentException("A list in SINGLE mode selects at most one row: " + set);
        }
        final int current = this.currentFor(set, this.selectedIndex);
        this.setSelection(set, current);
        this.anchor = current;
        return this;
    }

    @Override
    public ListView<T> selectItems(final Collection<? extends T> items) {
        // The first row of each key, as selectItem finds it
        final Map<Object, Integer> rows = Items.firstIndexes(this.items, this.keyFunction);
        final List<Integer> indexes = new ArrayList<>(items.size());
        for (final T item : items) {
            final Integer index = item == null ? null : rows.get(this.keyFunction.apply(item));
            if (index != null) {
                indexes.add(index);
            }
        }
        return this.selectIndexes(indexes);
    }

    @Override
    public ListView<T> selectAll() {
        if (this.mode == SelectionMode.MULTIPLE && !this.items.isEmpty()) {
            final List<Integer> all = new ArrayList<>(this.items.size());
            for (int i = 0; i < this.items.size(); i++) {
                all.add(i);
            }
            this.selectIndexes(all);
        }
        return this;
    }

    /**
     * The current row for a new set: the wanted one if it is in the set, else the set's last row, else none.
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
     * Brings the bits painting reads up to date with the selected set.
     */
    private void chosenChanged() {
        this.chosenBits.clear();
        for (final int index : this.chosen) {
            this.chosenBits.set(index);
        }
    }

    /**
     * Replaces the selected set and the current row, reporting the set first and then the current row, each only if
     * it changed.
     */
    private void setSelection(final Collection<Integer> set, final int current) {
        final List<Integer> before = List.copyOf(this.chosen);
        final int previous = this.selectedIndex;
        this.chosen.clear();
        this.chosen.addAll(set);
        this.chosenChanged();
        this.selectedIndex = current;
        if (!before.equals(List.copyOf(this.chosen))) {
            this.manyChanged(before);
        }
        if (current != previous) {
            this.selectionChanged(previous);
        }
    }

    /**
     * Applies a set the user's request ended with, ignoring rows the list does not show. The current row is the wanted
     * one if it is still in the set. A null value is ignored.
     *
     * @param indexes the rows to select, or null
     * @param wanted the row the user's action made current
     */
    void applyMany(final List<Integer> indexes, final int wanted) {
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
     * Runs a change of the set by the user through the owner's route, or asks with {@link ListView#SELECT_MANY}.
     */
    private void userSelectMany(final Collection<Integer> set, final int current) {
        final List<Integer> wanted = List.copyOf(new TreeSet<>(set));
        if (wanted.equals(List.copyOf(this.chosen)) && current == this.selectedIndex) {
            if (current >= 0) {
                this.scrollToIndex(current);
            }
            return;
        }
        if (this.selectManyRoute != null) {
            this.selectManyRoute.request(wanted, current);
        } else {
            this.request(SELECT_MANY, wanted, indexes -> this.applyMany(indexes, current));
        }
    }

    /**
     * Picks a row with the pointer or the keys in MULTIPLE mode: alone, added or removed with Control, or as the
     * range from the anchor with Shift.
     */
    private void userPick(final int index, final boolean control, final boolean shift, final boolean single) {
        if (shift) {
            final int from = this.anchor >= 0 ? this.anchor : index;
            final List<Integer> range = new ArrayList<>();
            for (int i = Math.min(from, index); i <= Math.max(from, index); i++) {
                range.add(i);
            }
            this.anchor = from;
            this.userSelectMany(range, index);
        } else if (control) {
            final TreeSet<Integer> set = new TreeSet<>(this.chosen);
            final boolean added = set.add(index);
            if (!added) {
                set.remove(index);
            }
            this.anchor = index;
            this.userSelectMany(set, added ? index : this.currentFor(set, this.selectedIndex));
        } else if (single && this.deselectable && this.chosen.equals(Set.of(index))) {
            this.anchor = -1;
            this.userSelectMany(List.of(), -1);
        } else {
            this.anchor = index;
            this.userSelectMany(List.of(index), index);
        }
    }

    @Override
    public ListView<T> selectItem(final T item) {
        return this.select(Items.indexOf(this.items, this.keyFunction, item));
    }

    @Override
    public boolean isDeselectable() {
        return this.deselectable;
    }

    @Override
    public ListView<T> setDeselectable(final boolean deselectable) {
        this.deselectable = deselectable;
        return this;
    }

    @Override
    public ListView<T> setActivateOnClick(final boolean onClick) {
        this.activateOnClick = onClick;
        return this;
    }

    @Override
    public Text placeholder() {
        return this.placeholder;
    }

    @Override
    public ListView<T> setPlaceholder(final Text placeholder) {
        this.placeholder = Objects.requireNonNull(placeholder, "placeholder");
        this.invalidateLayout();
        return this;
    }

    @Override
    public float scrollY() {
        return this.scrollY;
    }

    @Override
    public ListView<T> setScrollY(final float scrollY) {
        if (Float.isNaN(scrollY)) {
            throw new IllegalArgumentException("A scroll offset must not be NaN");
        }
        this.glide.stop();
        this.scrollTo(scrollY);
        return this;
    }

    @Override
    public ListView<T> scrollToIndex(final int index) {
        Objects.checkIndex(index, this.items.size());
        this.reveal = index;
        this.invalidateArrangement();
        return this;
    }

    /**
     * Reports a change of selection: to the owner if one {@linkplain #route routed} this list, or as
     * {@link ListView#SELECTED}.
     */
    private void selectionChanged(final int previous) {
        if (this.selectedRoute != null) {
            this.selectedRoute.selected(this.selectedIndex, previous);
        } else {
            this.report(SELECTED, this.selectedIndex, previous);
        }
    }

    /**
     * Reports a change of the selected set: to the owner if one {@linkplain #routeMany routed} it, or as
     * {@link ListView#SELECTED_MANY}.
     */
    private void manyChanged(final List<Integer> previous) {
        final List<Integer> now = List.copyOf(this.chosen);
        if (this.selectedManyRoute != null) {
            this.selectedManyRoute.selected(now, previous);
        } else {
            this.report(SELECTED_MANY, now, previous);
        }
    }

    /**
     * Sends this list's changes of the selected set to the control that owns it, as {@link #route} does for the
     * current row.
     *
     * @param select takes a set the user is about to select, with the row the action makes current; the owner applies
     *               it with {@link #applyMany}
     * @param selected told the new and previous set after every change
     */
    void routeMany(final ManyRequest select, final ManyReport selected) {
        this.selectManyRoute = Objects.requireNonNull(select, "select");
        this.selectedManyRoute = Objects.requireNonNull(selected, "selected");
    }

    /**
     * Makes this list a private part of the control that owns it. The list then fires none of its own events: each
     * selection and activation by the user goes to the owner, which applies a selection with {@link #applySelect},
     * and every change of selection, from any source, is told to the owner instead of being reported. Only core
     * controls that hold a list as a private part call it; it is public for the ones outside this package.
     *
     * @param select takes a row the user is about to select, or -1
     * @param activate takes a row the user activated
     * @param selected told the new and previous index after every change of selection
     */
    public void route(final IntConsumer select, final IntConsumer activate, final SelectionReport selected) {
        this.selectRoute = Objects.requireNonNull(select, "select");
        this.activateRoute = Objects.requireNonNull(activate, "activate");
        this.selectedRoute = Objects.requireNonNull(selected, "selected");
    }

    /**
     * Selects the row a request ended with, as {@link #select} does, or clears the selection for -1. A null or any
     * other index outside the items is ignored.
     *
     * @param index the row to select, -1 for none, or null
     */
    public void applySelect(final Integer index) {
        if (Items.isIndexOrNone(index, this.items.size())) {
            this.select(index);
        }
    }

    /**
     * Reports the row a request ended with as activated. A null or an index outside the items is ignored.
     */
    private void applyActivate(final Integer index) {
        if (Items.isIndex(index, this.items.size())) {
            this.report(ACTIVATED, index, index);
        }
    }

    /**
     * Runs a selection by the user through the owner's route, or asks with {@link ListView#SELECT}.
     */
    private void userSelect(final int index) {
        if (this.selectRoute != null) {
            this.selectRoute.accept(index);
        } else {
            this.request(SELECT, index, this::applySelect);
        }
    }

    /**
     * Runs an activation by the user through the owner's route, or asks with {@link ListView#ACTIVATE}.
     */
    private void userActivate(final int index) {
        if (this.activateRoute != null) {
            this.activateRoute.accept(index);
        } else {
            this.request(ACTIVATE, index, this::applyActivate);
        }
    }

    /**
     * Selects a row the keys moved to: through a request if the selection changes, or just into view if it does not.
     */
    private void keySelect(final int index) {
        if (index == this.selectedIndex) {
            this.scrollToIndex(index);
        } else {
            this.userSelect(index);
        }
    }

    /**
     * Moves the selection to a row for a key the user pressed, as Up and Down do: in {@link SelectionMode#MULTIPLE}
     * mode the row alone, or the range from the anchor with Shift, through {@link ListView#SELECT_MANY}; otherwise
     * through {@link ListView#SELECT} if the selection changes.
     *
     * @param index the row
     * @param shift whether Shift is held
     */
    void keyMove(final int index, final boolean shift) {
        if (this.mode == SelectionMode.MULTIPLE) {
            this.userPick(index, false, shift, false);
        } else {
            this.keySelect(index);
        }
    }

    /**
     * Moves to an offset, at least 0. Once the list has been laid out it is also clamped to the rows, measured from
     * the current items and the last viewport, so an offset set right after {@link #setItems} is not clamped to the
     * old items. The next arrange clamps it again.
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

    /**
     * Glides to an offset, clamped as {@link #scrollTo} clamps, over the theme's smooth scrolling time.
     *
     * @return true if where the list is headed changed
     */
    private boolean glideTo(final float scrollY) {
        final float max = this.arranged ? Math.max(0f, this.totalHeight() - this.viewport.height()) : Float.MAX_VALUE;
        final float next = Math.max(0f, Math.min(scrollY, max));
        if (next == this.glide.destination(this.scrollY)) {
            return false;
        }
        this.glide.move(this.motionAnimator(), this.smoothMillis, this.scrollY, next);
        return true;
    }

    private float step() {
        return this.rowHeight + this.rowSpacing;
    }

    private float totalHeight() {
        final int count = this.items.size();
        return count == 0 ? 0f
                : count * this.rowHeight + (count - 1) * this.rowSpacing + this.padding.top() + this.padding.bottom();
    }

    /**
     * The width rows get when the list is arranged in a rectangle: the inside of the border, less the scroll bar's
     * gutter when the rows overflow. A table lays its header out with this before the list is arranged.
     */
    float rowWidthFor(final Rect bounds) {
        final Rect inner = bounds.shrink(this.border().widths());
        return Math.max(0f, inner.width() - (this.totalHeight() > inner.height() ? GUTTER : 0f));
    }

    /**
     * Makes this list follow the theme's {@value ListView#STYLE_ROW_WIDTH} and {@value ListView#STYLE_PADDING} and draw its {@value ListView#STYLE_EDGE}
     * lines, as a list the factory makes does. Lists inside other controls leave both out.
     *
     * @return this list
     */
    public ListViewImpl<T> standalone() {
        this.standalone = true;
        this.invalidateLayout();
        return this;
    }

    private int rowAt(final float y) {
        if (!this.viewport.contains(this.viewport.x(), y)) {
            return -1;
        }
        final float along = y - this.viewport.y() + Math.round(this.scrollY) - this.padding.top();
        if (along < 0f) {
            return -1;
        }
        final int index = (int) Math.floor(along / this.step());
        if (index < 0 || index >= this.items.size() || along - index * this.step() >= this.rowHeight) {
            return -1;
        }
        return index;
    }

    /**
     * Asked before the user changes the selected set of a routed list.
     */
    @FunctionalInterface
    interface ManyRequest {
        /**
         * Asked before the user changes the selected set.
         *
         * @param indexes the rows about to be selected, in order
         * @param current the row the action makes current, or -1
         */
        void request(List<Integer> indexes, int current);
    }

    /**
     * Told after the selected set of a routed list changed.
     */
    @FunctionalInterface
    interface ManyReport {
        /**
         * Told after the selected set changed.
         *
         * @param indexes the selected rows, in order
         * @param previous the rows before
         */
        void selected(List<Integer> indexes, List<Integer> previous);
    }

    /**
     * Told the new and previous selected index of a routed list.
     */
    @FunctionalInterface
    public interface SelectionReport {
        /**
         * Told after the selection changed.
         *
         * @param index the selected index, or -1
         * @param previous the index before, or -1
         */
        void selected(int index, int previous);
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
        this.background = theme.paintOr(STYLE_BACKGROUND, Paint.NONE);
        this.row = theme.paintOr(STYLE_ROW, Paint.NONE);
        this.rowHovered = theme.paintOr(STYLE_ROW_HOVERED, this.row);
        this.rowSelected = theme.paintOr(STYLE_ROW_SELECTED, Paint.NONE);
        this.rowSelectedUnfocused = theme.paintOr(STYLE_ROW_SELECTED_UNFOCUSED, this.rowSelected);
        this.ring = theme.paintOr(STYLE_ROW_RING, Paint.NONE);
        this.ringUnfocused = theme.paintOr(STYLE_ROW_RING_UNFOCUSED, Paint.NONE);
        this.edge = theme.paintOr(STYLE_EDGE, Paint.NONE);
        this.edgeOuter = theme.paintOr(STYLE_EDGE_OUTER, Paint.NONE);
        this.rowLimit = Math.max(0f, theme.metricOr(STYLE_ROW_WIDTH, 0f));
        this.padding = this.standalone ? theme.insetsOr(STYLE_PADDING, Insets.NONE) : Insets.NONE;
        this.barGap = Math.max(0f, theme.metricOr(STYLE_BAR_GAP, ScrollPanel.BAR_GAP));
        this.scrollRows = Math.max(0f, theme.metricOr(STYLE_SCROLL_ROWS, 1f));
        this.thumb = theme.paintOr(ScrollPanel.STYLE_THUMB, Paint.NONE);
        this.thumbHovered = theme.paintOr(ScrollPanel.STYLE_THUMB_HOVERED, this.thumb);
        this.track = theme.paintOr(ScrollPanel.STYLE_TRACK, Paint.NONE);
        this.smoothMillis = Motion.millis(theme, ScrollPanel.STYLE_SMOOTH);
        this.placeholderStyle = theme.textStyleOr(STYLE_PLACEHOLDER, TextStyle.DEFAULT);
        this.typeAhead.applyTheme(theme);
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        this.placeholderLine = this.placeholder.isEmpty() ? TextLine.EMPTY
                : TextLine.of(this.placeholder, this.placeholderStyle, context.text());
        final float insetWidth = this.border().widths().horizontal();
        final float insetHeight = this.border().widths().vertical();
        final float total = this.totalHeight();
        final boolean overflow = !Float.isInfinite(availableHeight) && total > availableHeight - insetHeight;
        final float gutter = overflow ? GUTTER : 0f;
        final float cellWidth = availableWidth - insetWidth - gutter;
        float widest = 0f;
        for (final Component<?> cell : this.childList()) {
            widest = Math.max(widest, context.measure(cell, cellWidth, this.rowHeight).width());
        }
        final float width = Math.max(MIN_WIDTH, widest + insetWidth + gutter);
        final float height = total + insetHeight;
        return new Size(width, Float.isInfinite(availableHeight) ? height : Math.min(height, availableHeight));
    }

    @Override
    protected void arrange(final ArrangeContext context, final Rect bounds) {
        final Rect inner = bounds.shrink(this.border().widths());
        final float total = this.totalHeight();
        this.barShown = total > inner.height();
        // A centered column keeps its bar beside it, so the rows stay centered in the whole list
        final boolean column = this.standalone && this.rowLimit > 0f && this.rowLimit < inner.width();
        this.viewport = new Rect(inner.x(), inner.y(),
                Math.max(0f, inner.width() - (this.barShown && !column ? GUTTER : 0f)), inner.height());
        this.maxScrollY = Math.max(0f, total - this.viewport.height());
        if (this.reveal >= 0 && this.reveal < this.items.size()) {
            final float aim = this.glide.destination(this.scrollY);
            final float top = this.padding.top() + this.reveal * this.step();
            float target = aim;
            if (top < aim) {
                target = top;
            } else if (top + this.rowHeight > aim + this.viewport.height()) {
                target = top + this.rowHeight - this.viewport.height();
            }
            target = Math.max(0f, Math.min(target, this.maxScrollY));
            final Animator animator = this.motionAnimator();
            if (target != aim && this.arranged && animator != null && this.smoothMillis > 0L) {
                this.glide.move(animator, this.smoothMillis, this.scrollY, target);
            } else if (target != aim) {
                this.glide.stop();
                this.scrollY = target;
            }
        }
        this.reveal = -1;
        this.scrollY = Math.max(0f, Math.min(this.scrollY, this.maxScrollY));
        this.arranged = true;
        this.rowWidth = column ? this.rowLimit : this.viewport.width();
        this.rowX = column ? Caption.center(this.viewport.x(), this.viewport.width(), this.rowWidth) : this.viewport.x();

        if (this.rowsStale) {
            this.rowsStale = false;
            this.rows.clear();
            this.clearChildren();
        }
        final int first;
        final int last;
        if (this.items.isEmpty() || this.viewport.height() <= 0f) {
            first = 0;
            last = -1;
        } else {
            final float from = this.scrollY - this.padding.top();
            first = Math.max(0, (int) Math.floor(from / this.step()));
            last = Math.min(this.items.size() - 1, (int) Math.floor((from + this.viewport.height()) / this.step()));
        }
        for (final Iterator<Map.Entry<Integer, Component<?>>> it = this.rows.entrySet().iterator(); it.hasNext(); ) {
            final Map.Entry<Integer, Component<?>> entry = it.next();
            if (entry.getKey() < first || entry.getKey() > last) {
                it.remove();
                this.removeChild(entry.getValue());
            }
        }
        final float offset = Math.round(this.scrollY);
        final int count = Math.max(0, last - first + 1);
        if (this.paintRect.length < count) {
            this.paintIndex = new int[count];
            this.paintRect = new Rect[count];
            this.paintRing = new Rect[count][];
        }
        this.paintCount = 0;
        for (int i = first; i <= last; i++) {
            Component<?> cell = this.rows.get(i);
            if (cell == null) {
                cell = this.cellFactory.apply(this.items.get(i));
                this.rows.put(i, cell);
                this.addContent(cell);
                context.measure(cell, this.rowWidth, this.rowHeight);
            }
            final Rect rect = new Rect(this.rowX, this.viewport.y() + this.padding.top() + i * this.step() - offset,
                    this.rowWidth, this.rowHeight);
            context.arrange(cell, rect);
            this.paintIndex[this.paintCount] = i;
            this.paintRect[this.paintCount] = rect;
            this.paintRing[this.paintCount] = null;
            this.paintCount++;
        }
        final float barX = column
                ? this.rowX + this.rowWidth + this.barGap
                : inner.right() - ScrollPanel.BAR_WIDTH;
        this.bar.update(new Rect(barX, inner.y(), ScrollPanel.BAR_WIDTH, inner.height()),
                this.viewport.height(), Math.max(total, this.viewport.height()));
        this.thumbRect = this.bar.thumb(this.scrollY, this.maxScrollY);
        this.edges = this.standalone ? new Rect[] {
                new Rect(bounds.x(), bounds.y() - 1f, bounds.width(), 1f),
                new Rect(bounds.x(), bounds.bottom(), bounds.width(), 1f),
                new Rect(bounds.x(), bounds.y() - 2f, bounds.width(), 1f),
                new Rect(bounds.x(), bounds.bottom() + 1f, bounds.width(), 1f)
        } : new Rect[0];
    }

    @Override
    protected Rect childClip() {
        return this.viewport;
    }

    @Override
    protected void paint(final Painter painter, final Rect bounds) {
        if (this.edges.length == 4) {
            this.edge.paint(painter, this.edges[0]);
            this.edge.paint(painter, this.edges[1]);
            this.edgeOuter.paint(painter, this.edges[2]);
            this.edgeOuter.paint(painter, this.edges[3]);
        }
        this.background.paint(painter, bounds);
        if (this.items.isEmpty()) {
            final TextLine line = this.placeholderLine;
            if (!line.isEmpty()) {
                line.draw(painter, bounds.x() + (bounds.width() - line.width()) * 0.5f,
                        bounds.y() + (bounds.height() - line.height()) * 0.5f);
            }
            return;
        }
        this.painter = painter;
        try {
            painter.clipped(this.viewport, this.drawRows);
        } finally {
            this.painter = null;
        }
    }

    /**
     * Draws each built row's background, inside the viewport clip, from the rectangles arrange made.
     */
    private void drawRows() {
        final int hoveredRow = this.isHoverWithin() ? this.rowAt(this.pointerY) : -1;
        final boolean focused = this.isFocused();
        for (int i = 0; i < this.paintCount; i++) {
            final int index = this.paintIndex[i];
            final Rect rect = this.paintRect[i];
            if (this.chosenBits.get(index)) {
                (focused ? this.rowSelected : this.rowSelectedUnfocused).paint(this.painter, rect);
                final Paint edge = focused ? this.ring : this.ringUnfocused;
                if (edge != Paint.NONE) {
                    if (this.paintRing[i] == null) {
                        this.paintRing[i] = Shapes.ring(rect);
                    }
                    Shapes.paint(this.painter, edge, this.paintRing[i]);
                }
            } else {
                (index == hoveredRow ? this.rowHovered : this.row).paint(this.painter, rect);
            }
        }
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
    protected void onPointerMove(final PointerEvent event) {
        this.pointerY = event.y();
    }

    @Override
    protected void onPointerPress(final PointerButtonEvent event) {
        this.pointerY = event.y();
        this.thumbDrag.release();
        if (event.button() != MouseButton.LEFT) {
            return;
        }
        if (this.barShown) {
            final float page = Math.max(this.step(), this.viewport.height() - this.step());
            final float next = this.thumbDrag.press(event.x(), event.y(), this.scrollY, this.maxScrollY, page);
            if (!Float.isNaN(next)) {
                this.glide.stop();
                this.scrollTo(next);
                event.consume();
                return;
            }
        }
        final int index = this.rowAt(event.y());
        if (index < 0) {
            return;
        }
        if (this.mode == SelectionMode.MULTIPLE) {
            this.userPick(index, event.modifiers().control(), event.modifiers().shift(), event.clicks() == 1);
        } else if (this.deselectable && index == this.selectedIndex && event.clicks() == 1) {
            this.userSelect(-1);
        } else if (index != this.selectedIndex) {
            this.userSelect(index);
        }
        if (event.clicks() == 2 && !this.activateOnClick) {
            this.userActivate(index);
        }
        event.consume();
    }

    @Override
    protected void onClick(final PointerButtonEvent event) {
        if (this.activateOnClick && event.button() == MouseButton.LEFT) {
            final int index = this.rowAt(event.y());
            if (index >= 0) {
                this.userActivate(index);
                event.consume();
            }
        }
    }

    @Override
    protected void onDragStart(final DragEvent event) {
        this.onDrag(event);
    }

    @Override
    protected void onDrag(final DragEvent event) {
        if (this.thumbDrag.isDragging()) {
            this.glide.stop();
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
        // A press on the thumb that never became a drag ends here, with no drag end
        if (event.button() == MouseButton.LEFT) {
            this.thumbDrag.release();
        }
    }

    @Override
    protected void onScroll(final ScrollEvent event) {
        if (this.glideTo(this.glide.destination(this.scrollY) - event.scrollY() * this.step() * this.scrollRows)) {
            event.consume();
        }
    }

    @Override
    protected void onChar(final CharEvent event) {
        final Function<? super T, ?> name = this.typeAheadName;
        if (name == null || this.items.isEmpty()) {
            return;
        }
        final Appearances appearances = this.node().factory().appearances();
        final int found = this.typeAhead.type(event, this.environment().millis(), this.selectedIndex,
                this.items.size(), index -> appearances.nameOf(name.apply(this.items.get(index))).plain());
        if (found == TypeAhead.NOT_TAKEN) {
            return;
        }
        if (found >= 0) {
            this.keyMove(found, false);
        }
        event.consume();
    }

    @Override
    protected void onKey(final KeyEvent event) {
        if (!event.action().isDown() || event.modifiers().alt() || this.items.isEmpty()) {
            return;
        }
        if (event.modifiers().control()) {
            if (this.mode == SelectionMode.MULTIPLE && event.key() == Key.A && event.action() == KeyAction.PRESS) {
                final List<Integer> all = new ArrayList<>(this.items.size());
                for (int i = 0; i < this.items.size(); i++) {
                    all.add(i);
                }
                this.userSelectMany(all, this.selectedIndex >= 0 ? this.selectedIndex : 0);
                event.consume();
            }
            return;
        }
        final int page = Math.max(1, (int) Math.floor(this.viewport.height() / this.step()));
        final int last = this.items.size() - 1;
        final int current = this.selectedIndex;
        final int next = switch (event.key()) {
            case UP -> current < 0 ? last : Math.max(0, current - 1);
            case DOWN -> current < 0 ? 0 : Math.min(last, current + 1);
            case PAGE_UP -> Math.max(0, (current < 0 ? 0 : current) - page);
            case PAGE_DOWN -> Math.min(last, (current < 0 ? 0 : current) + page);
            case HOME -> 0;
            case END -> last;
            default -> -2;
        };
        if (next >= 0) {
            this.keyMove(next, event.modifiers().shift());
            event.consume();
        } else if (event.key().isEnter() && event.action() == KeyAction.PRESS && current >= 0) {
            this.userActivate(current);
            event.consume();
        }
    }
}
