package org.inspirenxe.strata.core.control.collection;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.collection.DualList;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.extension.ArrangeContext;
import org.inspirenxe.strata.api.extension.CustomComposite;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.geom.Size;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.text.TextOverflow;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * The core {@link DualList}: two {@link ListViewImpl}s, four buttons and two title labels, all private children. The
 * lists route their input to the dual list, so they fire no events of their own: a row the user selects becomes the
 * dual list's {@code SELECT} request and a row the user activates becomes a {@code MOVE} request. The buttons'
 * {@link Button#PRESSED} events stop at the dual list, which handles them.
 *
 * <p>The selection lives in the two lists, at most one of which has a selected row; the dual list works out its
 * combined index from them and reports {@code SELECTED} when that index or the selected item changed.</p>
 *
 * @param <T> the type of the items
 */
public final class DualListImpl<T> extends CustomComposite<DualList<T>> implements DualList<T> {
    private final ComponentFactory ui;
    private final ListViewImpl<T> left;
    private final ListViewImpl<T> right;
    private final Label leftTitle;
    private final Label rightTitle;
    private final Button moveRight;
    private final Button moveAllRight;
    private final Button moveLeft;
    private final Button moveAllLeft;
    private final List<Button> buttons;
    private Function<? super T, ? extends Component<?>> cellFactory;
    private Function<? super T, ?> keyFunction;
    private int visibleRows;

    // What SELECTED last reported, to report only changes
    private int reportedIndex;
    private Optional<T> reportedItem;
    // Set while several changes to the lists make one change of selection
    private boolean updating;

    // Computed during measure
    private float titleHeight;
    private float buttonWidth;

    /**
     * Creates a dual list with no items and no titles.
     *
     * @param ui the factory that owns the list
     * @param cellFactory makes the component for an item's row
     */
    @SuppressWarnings("this-escape")
    public DualListImpl(final ComponentFactory ui, final Function<? super T, ? extends Component<?>> cellFactory) {
        super(ui);
        this.ui = ui;
        this.cellFactory = Objects.requireNonNull(cellFactory, "cellFactory");
        this.keyFunction = Function.identity();
        this.visibleRows = DEFAULT_VISIBLE_ROWS;
        this.reportedIndex = -1;
        this.reportedItem = Optional.empty();
        this.left = this.newList(Side.LEFT);
        this.right = this.newList(Side.RIGHT);
        this.leftTitle = ui.label("");
        this.rightTitle = ui.label("");
        this.leftTitle.setOverflow(TextOverflow.ELLIPSIS);
        this.rightTitle.setOverflow(TextOverflow.ELLIPSIS);
        this.leftTitle.setAnchor(Anchor.FILL);
        this.rightTitle.setAnchor(Anchor.FILL);
        this.moveRight = this.newButton(ui, ">", () -> this.userMoveSelected(Side.LEFT));
        this.moveAllRight = this.newButton(ui, ">>", () -> this.userMoveAll(Side.LEFT));
        this.moveLeft = this.newButton(ui, "<", () -> this.userMoveSelected(Side.RIGHT));
        this.moveAllLeft = this.newButton(ui, "<<", () -> this.userMoveAll(Side.RIGHT));
        this.buttons = List.of(this.moveRight, this.moveAllRight, this.moveLeft, this.moveAllLeft);
        this.addChild(this.leftTitle);
        this.addChild(this.rightTitle);
        this.addChild(this.left);
        this.buttons.forEach(this::addChild);
        this.addChild(this.right);
        this.updateButtons();
    }

    private ListViewImpl<T> newList(final Side side) {
        final ListViewImpl<T> list = new ListViewImpl<>(this.ui, item -> this.cellFactory.apply(item));
        list.route(index -> this.userSelect(side, index), index -> this.userMove(side, index),
                (index, previous) -> this.listSelectionChanged());
        list.setKeyFunction(item -> this.keyFunction.apply(item));
        list.setAnchor(Anchor.FILL);
        return list;
    }

    private Button newButton(final ComponentFactory ui, final String text, final Runnable action) {
        return ui.button(text)
                .setAnchor(Anchor.FILL)
                .onEvent(Button.PRESSED, event -> action.run());
    }

    private ListViewImpl<T> list(final Side side) {
        return side == Side.LEFT ? this.left : this.right;
    }

    // ------------------------------------------------------------------
    // Items
    // ------------------------------------------------------------------

    @Override
    public List<T> items(final Side side) {
        return this.list(Objects.requireNonNull(side, "side")).items();
    }

    @Override
    public DualList<T> setItems(final Side side, final List<? extends T> items) {
        final ListViewImpl<T> list = this.list(Objects.requireNonNull(side, "side"));
        this.updating = true;
        try {
            list.setItems(items);
        } finally {
            this.updating = false;
        }
        this.changed();
        return this;
    }

    @Override
    public DualList<T> refresh() {
        this.left.refresh();
        this.right.refresh();
        return this;
    }

    @Override
    public DualList<T> setCellFactory(final Function<? super T, ? extends Component<?>> cellFactory) {
        this.cellFactory = Objects.requireNonNull(cellFactory, "cellFactory");
        return this.refresh();
    }

    @Override
    public DualList<T> setKeyFunction(final Function<? super T, ?> keyFunction) {
        this.keyFunction = Objects.requireNonNull(keyFunction, "keyFunction");
        return this;
    }

    @Override
    public String title(final Side side) {
        return this.titleLabel(Objects.requireNonNull(side, "side")).text();
    }

    @Override
    public DualList<T> setTitle(final Side side, final String title) {
        this.titleLabel(Objects.requireNonNull(side, "side")).setText(Objects.requireNonNull(title, "title"));
        this.invalidateLayout();
        return this;
    }

    private Label titleLabel(final Side side) {
        return side == Side.LEFT ? this.leftTitle : this.rightTitle;
    }

    @Override
    public int visibleRows() {
        return this.visibleRows;
    }

    @Override
    public DualList<T> setVisibleRows(final int rows) {
        if (rows < 1) {
            throw new IllegalArgumentException("At least one row must show: " + rows);
        }
        this.visibleRows = rows;
        this.invalidateLayout();
        return this;
    }

    private int indexOfKey(final Side side, final T item) {
        final Object key = this.keyFunction.apply(item);
        final List<T> items = this.list(side).items();
        for (int i = 0; i < items.size(); i++) {
            if (Objects.equals(key, this.keyFunction.apply(items.get(i)))) {
                return i;
            }
        }
        return -1;
    }

    // ------------------------------------------------------------------
    // Selection
    // ------------------------------------------------------------------

    @Override
    public Optional<Side> selectedSide() {
        if (this.left.selectedIndex() >= 0) {
            return Optional.of(Side.LEFT);
        }
        return this.right.selectedIndex() >= 0 ? Optional.of(Side.RIGHT) : Optional.empty();
    }

    @Override
    public int selectedIndex() {
        if (this.left.selectedIndex() >= 0) {
            return this.left.selectedIndex();
        }
        return this.right.selectedIndex() >= 0 ? this.left.items().size() + this.right.selectedIndex() : -1;
    }

    @Override
    public Optional<T> selected() {
        return this.left.selectedIndex() >= 0 ? this.left.selected() : this.right.selected();
    }

    @Override
    public DualList<T> select(final int index) {
        final int leftSize = this.left.items().size();
        if (index != -1) {
            Objects.checkIndex(index, leftSize + this.right.items().size());
        }
        this.updating = true;
        try {
            if (index < 0) {
                this.left.select(-1);
                this.right.select(-1);
            } else if (index < leftSize) {
                this.right.select(-1);
                this.left.select(index);
            } else {
                this.left.select(-1);
                this.right.select(index - leftSize);
            }
        } finally {
            this.updating = false;
        }
        this.changed();
        return this;
    }

    @Override
    public DualList<T> selectItem(final T item) {
        final int inLeft = this.indexOfKey(Side.LEFT, item);
        if (inLeft >= 0) {
            return this.select(inLeft);
        }
        final int inRight = this.indexOfKey(Side.RIGHT, item);
        return this.select(inRight < 0 ? -1 : this.left.items().size() + inRight);
    }

    /**
     * Asks to select a row the user pressed or moved to in one list, as an index across both lists.
     */
    private void userSelect(final Side side, final int index) {
        final int combined = index < 0 ? -1 : side == Side.LEFT ? index : this.left.items().size() + index;
        if (combined != this.selectedIndex()) {
            this.request(SELECT, combined, this::applySelect);
        }
    }

    /**
     * Selects the index a request ended with. A null or an index outside both lists is ignored.
     */
    private void applySelect(final Integer index) {
        if (index != null && index >= -1 && index < this.left.items().size() + this.right.items().size()) {
            this.select(index);
        }
    }

    private void listSelectionChanged() {
        if (!this.updating) {
            this.changed();
        }
    }

    /**
     * Updates the buttons and reports {@code SELECTED} if the selected index or item differs from the last report.
     */
    private void changed() {
        this.updateButtons();
        final int index = this.selectedIndex();
        final Optional<T> item = this.selected();
        if (index != this.reportedIndex || !item.equals(this.reportedItem)) {
            final int previous = this.reportedIndex;
            this.reportedIndex = index;
            this.reportedItem = item;
            this.report(SELECTED, index, previous);
        }
    }

    private void updateButtons() {
        this.moveRight.setEnabled(this.left.selectedIndex() >= 0);
        this.moveLeft.setEnabled(this.right.selectedIndex() >= 0);
        this.moveAllRight.setEnabled(!this.left.items().isEmpty());
        this.moveAllLeft.setEnabled(!this.right.items().isEmpty());
    }

    // ------------------------------------------------------------------
    // Moving
    // ------------------------------------------------------------------

    @Override
    public DualList<T> move(final T item, final Side to) {
        final Side from = Objects.requireNonNull(to, "to").opposite();
        final int index = this.indexOfKey(from, item);
        if (index >= 0) {
            this.applyMove(from, index, this.list(to).items().size());
        }
        return this;
    }

    @Override
    public DualList<T> moveAll(final Side to) {
        final Side from = Objects.requireNonNull(to, "to").opposite();
        while (!this.list(from).items().isEmpty()) {
            this.applyMove(from, 0, this.list(to).items().size());
        }
        return this;
    }

    /**
     * Asks to move the selected item of one side across. Does nothing if that side has no selection.
     */
    private void userMoveSelected(final Side from) {
        final int index = this.list(from).selectedIndex();
        if (index >= 0) {
            this.userMove(from, index);
        }
    }

    /**
     * Asks to move every item of one side across, one request per item, in order.
     */
    private void userMoveAll(final Side from) {
        for (final T item : List.copyOf(this.list(from).items())) {
            final int index = this.indexOfKey(from, item);
            if (index >= 0) {
                this.userMove(from, index);
            }
        }
    }

    /**
     * Asks to move the item at an index of one side to the end of the other.
     */
    private void userMove(final Side from, final int index) {
        final List<T> items = this.list(from).items();
        if (index < 0 || index >= items.size() || !this.isEnabled()) {
            return;
        }
        final Side to = from.opposite();
        this.request(MOVE, new Move<>(items.get(index), to, this.list(to).items().size()), this::applyRequestedMove);
    }

    /**
     * Applies the move a request ended with: finds its item on the side opposite the one it names and moves it to
     * its index there, clamped. A null move or one whose item is not on that side is ignored.
     */
    @SuppressWarnings("unchecked")
    private void applyRequestedMove(final Move<?> move) {
        if (move == null) {
            return;
        }
        final Side from = move.side().opposite();
        final int index = this.indexOfKey(from, (T) move.item());
        if (index >= 0) {
            this.applyMove(from, index, move.index());
        }
    }

    /**
     * Moves the item at an index of one side to an index of the other, clamped to that list. If the item was
     * selected, the selection goes to the item that took its place, or the one before it, or clears. Reports
     * {@code MOVED}, then {@code SELECTED} if the selection changed.
     */
    private void applyMove(final Side from, final int index, final int at) {
        final Side to = from.opposite();
        final ListViewImpl<T> source = this.list(from);
        final ListViewImpl<T> target = this.list(to);
        final boolean wasSelected = source.selectedIndex() == index;
        final List<T> remaining = new ArrayList<>(source.items());
        final T item = remaining.remove(index);
        final List<T> grown = new ArrayList<>(target.items());
        final int placed = Math.max(0, Math.min(at, grown.size()));
        grown.add(placed, item);
        this.updating = true;
        try {
            source.setItems(remaining);
            target.setItems(grown);
            if (wasSelected && !remaining.isEmpty()) {
                source.select(Math.min(index, remaining.size() - 1));
            }
        } finally {
            this.updating = false;
        }
        this.updateButtons();
        this.report(MOVED, new Move<>(item, to, placed), new Move<>(item, from, index));
        this.changed();
    }

    // ------------------------------------------------------------------
    // Theme and layout
    // ------------------------------------------------------------------

    @Override
    protected void onApplyTheme(final Theme theme) {
        final Paint background = theme.paintOr(STYLE_LIST, Paint.NONE);
        this.left.setBackground(background);
        this.right.setBackground(background);
        final TextStyle titleStyle = theme.textStyleOr(STYLE_TITLE, theme.textStyleOr(Label.STYLE_TEXT, TextStyle.DEFAULT));
        this.leftTitle.setStyle(titleStyle);
        this.rightTitle.setStyle(titleStyle);
    }

    private boolean hasTitles() {
        return !this.leftTitle.text().isEmpty() || !this.rightTitle.text().isEmpty();
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        this.buttonWidth = 0f;
        float buttonsHeight = 0f;
        for (final Button button : this.buttons) {
            final Size size = context.measure(button, availableWidth, availableHeight);
            this.buttonWidth = Math.max(this.buttonWidth, size.width());
            buttonsHeight += size.height();
        }
        buttonsHeight += SPACING * (this.buttons.size() - 1);
        this.titleHeight = 0f;
        if (this.hasTitles()) {
            final float titleWidth = DEFAULT_LIST_WIDTH;
            this.titleHeight = Math.max(context.measure(this.leftTitle, titleWidth, availableHeight).height(),
                    context.measure(this.rightTitle, titleWidth, availableHeight).height()) + SPACING;
        }
        final float listHeight = this.visibleRows * this.left.rowHeight() + (this.visibleRows - 1) * this.left.rowSpacing();
        final float width = DEFAULT_LIST_WIDTH * 2f + SPACING * 2f + this.buttonWidth;
        return new Size(width, this.titleHeight + Math.max(listHeight, buttonsHeight));
    }

    @Override
    protected void arrange(final ArrangeContext context, final Rect bounds) {
        final float listWidth = Math.max(0f, (bounds.width() - this.buttonWidth - SPACING * 2f) * 0.5f);
        final float listsY = bounds.y() + this.titleHeight;
        final float listHeight = Math.max(0f, bounds.height() - this.titleHeight);
        final float rightX = bounds.right() - listWidth;
        final boolean titles = this.titleHeight > 0f;
        final float titleRowHeight = titles ? this.titleHeight - SPACING : 0f;
        context.arrange(this.leftTitle, new Rect(bounds.x(), bounds.y(), titles ? listWidth : 0f, titleRowHeight));
        context.arrange(this.rightTitle, new Rect(rightX, bounds.y(), titles ? listWidth : 0f, titleRowHeight));
        context.arrange(this.left, new Rect(bounds.x(), listsY, listWidth, listHeight));
        context.arrange(this.right, new Rect(rightX, listsY, listWidth, listHeight));

        float buttonsHeight = SPACING * (this.buttons.size() - 1);
        for (final Button button : this.buttons) {
            buttonsHeight += button.desiredHeight();
        }
        final float buttonX = bounds.x() + listWidth + SPACING;
        float y = listsY + Math.max(0f, (listHeight - buttonsHeight) * 0.5f);
        for (final Button button : this.buttons) {
            final float height = button.desiredHeight();
            context.arrange(button, new Rect(buttonX, y, this.buttonWidth, height));
            y += height + SPACING;
        }
    }

    @Override
    protected Object snapshotState() {
        return new DualList.State(this.left.items(), this.right.items(), this.selectedIndex());
    }
}
