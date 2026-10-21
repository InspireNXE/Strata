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

import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.control.collection.ComboBox;
import org.inspirenxe.strata.api.control.collection.ListView;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.event.input.KeyEvent;
import org.inspirenxe.strata.api.event.input.PointerButtonEvent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.overlay.Overlay;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextOverflow;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.core.node.BuiltInComponent;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;
import org.inspirenxe.strata.core.text.TextLine;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * The core {@link ComboBox}. The open list is a {@link ListView} in a popup, not focusable, whose selection is the
 * highlight; it is built when the list opens and dropped when it closes. The box routes the list's input to itself,
 * so the list fires no events: moving the highlight is silent, and a row picked from it becomes the box's own
 * {@code SELECT} request. Its rows are one line plus 4 units tall, with the option text inset by the list's cell
 * padding, {@value ListView#STYLE_CELL_PADDING}, which is 4 units unless the theme sets it.
 *
 * <p>The box draws its text inside its padding on the left, cut short with {@code ...} if it does not fit, and a
 * small down arrow inside its padding on the right, {@link #ARROW_GAP} units after the text. The padding is the
 * theme's {@value #STYLE_PADDING}, or else {@link #DEFAULT_PADDING}, and the box is a line tall plus its top and
 * bottom padding.</p>
 *
 * @param <T> the type of the options
 */
public final class ComboBoxImpl<T> extends BuiltInComponent<ComboBox<T>> implements ComboBox<T> {
    /**
     * The padding when the theme sets no {@value #STYLE_PADDING}: 4 at the sides and 3 at the top and bottom.
     */
    public static final Insets DEFAULT_PADDING = Insets.of(4f, 3f);
    /**
     * The least space between the text and the arrow, in UI units.
     */
    public static final float ARROW_GAP = 4f;
    /**
     * The width of the arrow, in UI units.
     */
    public static final float ARROW_WIDTH = 7f;
    private static final Set<String> REQUIRED = Set.of(STYLE_NORMAL, STYLE_POPUP);

    private final ComponentFactoryImpl ui;
    private Insets padding = DEFAULT_PADDING;
    private List<T> options;
    private Function<? super T, ?> labelFunction;
    private Function<? super T, ?> keyFunction;
    private int selectedIndex;
    private int maxVisible;
    private Overlay popup;
    private ListViewImpl<T> list;

    // Theme
    private Paint normal;
    private Paint hoveredPaint;
    private Paint disabledPaint;
    private Paint popupPaint;
    private TextStyle textStyle;
    private TextStyle disabledTextStyle;

    // Computed during measure
    private float lineHeight;

    // Shortened caption for the selected index and bounds width it was made for; -2 when it must be made again
    private int shownIndex;
    private float shownForWidth;
    private TextLine shown;
    private TextLine shownDisabled;

    /**
     * Creates a box with no options.
     *
     * @param ui the factory that owns the box
     * @param labelFunction returns the text for an option
     */
    @SuppressWarnings("this-escape")
    public ComboBoxImpl(final ComponentFactoryImpl ui, final Function<? super T, ?> labelFunction) {
        super(ui);
        this.ui = ui;
        this.options = List.of();
        this.labelFunction = Objects.requireNonNull(labelFunction, "labelFunction");
        this.keyFunction = Function.identity();
        this.selectedIndex = -1;
        this.maxVisible = DEFAULT_MAX_VISIBLE_OPTIONS;
        this.normal = Paint.NONE;
        this.hoveredPaint = Paint.NONE;
        this.disabledPaint = Paint.NONE;
        this.popupPaint = Paint.NONE;
        this.textStyle = TextStyle.DEFAULT;
        this.disabledTextStyle = TextStyle.DEFAULT;
        this.shownIndex = -2;
        this.shownForWidth = Float.NaN;
        this.shown = TextLine.EMPTY;
        this.shownDisabled = TextLine.EMPTY;
        this.setCursor(CursorKind.HAND);
    }

    // ------------------------------------------------------------------
    // ComboBox
    // ------------------------------------------------------------------

    @Override
    public List<T> items() {
        return this.options;
    }

    @Override
    public ComboBox<T> setItems(final List<? extends T> items) {
        final Optional<T> before = this.selected();
        final int beforeIndex = this.selectedIndex;
        final boolean had = beforeIndex >= 0;
        final T beforeItem = had ? this.options.get(beforeIndex) : null;
        this.options = List.copyOf(items);
        int index = this.options.isEmpty() ? -1 : 0;
        if (had) {
            final int found = this.indexOfKey(beforeItem);
            if (found >= 0) {
                index = found;
            }
        }
        this.close();
        this.selectedIndex = index;
        this.shownIndex = -2;
        this.invalidateLayout();
        if (index != beforeIndex || !this.selected().equals(before)) {
            this.report(SELECTED, index, beforeIndex);
        }
        return this;
    }

    @Override
    public ComboBox<T> refresh() {
        this.shownIndex = -2;
        this.invalidateLayout();
        if (this.list != null) {
            this.list.refresh();
        }
        return this;
    }

    @Override
    public ComboBox<T> setKeyFunction(final Function<? super T, ?> keyFunction) {
        this.keyFunction = Objects.requireNonNull(keyFunction, "keyFunction");
        return this;
    }

    /**
     * Finds the first option with the same key as an item.
     *
     * @return the option's index, or -1 if none has the key
     */
    private int indexOfKey(final T item) {
        final Object key = item == null ? null : this.keyFunction.apply(item);
        for (int i = 0; i < this.options.size(); i++) {
            final T option = this.options.get(i);
            if (Objects.equals(key, option == null ? null : this.keyFunction.apply(option))) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public ComboBox<T> setLabelFunction(final Function<? super T, ?> labelFunction) {
        this.labelFunction = Objects.requireNonNull(labelFunction, "labelFunction");
        this.invalidateLayout();
        return this;
    }

    @Override
    public Optional<T> selected() {
        return this.selectedIndex < 0 ? Optional.empty() : Optional.ofNullable(this.options.get(this.selectedIndex));
    }

    @Override
    public int selectedIndex() {
        return this.selectedIndex;
    }

    @Override
    public ComboBox<T> select(final int index) {
        if (index != -1) {
            Objects.checkIndex(index, this.options.size());
        }
        final int previous = this.selectedIndex;
        if (index != previous) {
            this.selectedIndex = index;
            this.shownIndex = -2;
            this.report(SELECTED, index, previous);
        }
        return this;
    }

    @Override
    public ComboBox<T> selectItem(final T item) {
        return this.select(this.indexOfKey(item));
    }

    @Override
    public int maxVisibleOptions() {
        return this.maxVisible;
    }

    @Override
    public ComboBox<T> setMaxVisibleOptions(final int count) {
        if (count < 1) {
            throw new IllegalArgumentException("At least one option must show: " + count);
        }
        this.maxVisible = count;
        return this;
    }

    @Override
    public boolean isOpen() {
        return this.popup != null && this.popup.isOpen();
    }

    @Override
    public ComboBox<T> open() {
        if (!this.canOpen()) {
            return this;
        }
        final float rowHeight = this.lineHeight + 4f;
        final ListViewImpl<T> options = new ListViewImpl<>(this.ui, this::cell);
        options.route(options::applySelect, this::pick, (index, previous) -> { });
        options.setFocusable(false);
        options.setTabbable(false);
        options.setRowHeight(rowHeight);
        options.setActivateOnClick(true);
        options.setItems(this.options);
        this.nodeOf(options).setBackground(this.popupPaint);
        options.setMinSize(this.bounds().width(), 0f);
        options.setHeight(rowHeight * Math.min(this.maxVisible, this.options.size()));
        if (this.selectedIndex >= 0) {
            options.select(this.selectedIndex);
        }
        this.list = options;
        this.popup = this.ui.overlays().popup(options, this.bounds());
        this.popup.onEvent(Overlay.CLOSED, event -> {
            this.popup = null;
            this.list = null;
            this.report(CLOSED);
        });
        this.report(OPENED);
        return this;
    }

    private boolean canOpen() {
        return !this.isOpen() && this.isAttached() && this.isEnabled() && !this.options.isEmpty();
    }

    /**
     * Closes the list after the user picked a row from it, then asks to select that option.
     */
    private void pick(final int index) {
        this.close();
        this.userSelect(index);
    }

    /**
     * Opens the list for the user if {@code OPEN} allows it. Does nothing if the list cannot open.
     */
    private void userOpen() {
        if (this.canOpen() && this.request(OPEN)) {
            this.open();
        }
    }

    /**
     * Asks to select an option the user chose. Does nothing if the option is already selected.
     */
    private void userSelect(final int index) {
        if (index != this.selectedIndex) {
            this.request(SELECT, index, this::applySelect);
        }
    }

    /**
     * Selects the option a request ended with. A null or an index outside the options is ignored.
     */
    private void applySelect(final Integer index) {
        if (index != null && index >= 0 && index < this.options.size()) {
            this.select(index);
        }
    }

    private Label cell(final T option) {
        final Label label = this.ui.cellPadded(
                this.ui.label(this.ui.appearances().nameOf(this.labelFunction.apply(option))).setStyle(this.textStyle));
        label.setOverflow(TextOverflow.ELLIPSIS);
        label.setAnchor(Anchor.MIDDLE_LEFT);
        return label;
    }

    @Override
    public ComboBox<T> close() {
        if (this.popup != null) {
            this.popup.close();
        }
        return this;
    }

    private Text label(final int index) {
        return index < 0 ? Text.empty() : this.ui.appearances().nameOf(this.labelFunction.apply(this.options.get(index)));
    }

    // ------------------------------------------------------------------
    // Theme, layout and painting
    // ------------------------------------------------------------------

    @Override
    protected Set<String> requiredStyleKeys() {
        return REQUIRED;
    }

    @Override
    protected void onApplyTheme(final Theme theme) {
        this.normal = theme.paintOr(STYLE_NORMAL, Paint.NONE);
        this.hoveredPaint = theme.paintOr(STYLE_HOVERED, this.normal);
        this.disabledPaint = theme.paintOr(STYLE_DISABLED, this.normal);
        this.padding = theme.insetsOr(STYLE_PADDING, DEFAULT_PADDING);
        this.popupPaint = theme.paintOr(STYLE_POPUP, Paint.NONE);
        this.textStyle = theme.textStyleOr(STYLE_TEXT, TextStyle.DEFAULT);
        this.disabledTextStyle = theme.textStyleOr(STYLE_TEXT_DISABLED, this.textStyle);
        this.shownIndex = -2;
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        final TextMetrics metrics = context.text();
        float widest = 0f;
        float height = Math.max(metrics.lineHeight(this.textStyle), metrics.lineHeight(this.disabledTextStyle));
        for (int i = 0; i < this.options.size(); i++) {
            final TextLine line = TextLine.of(this.label(i), this.textStyle, metrics);
            widest = Math.max(widest, line.width());
            height = Math.max(height, line.height());
        }
        this.lineHeight = height;
        this.shownIndex = -2;
        final Insets padding = this.padding;
        return new Size(padding.left() + widest + ARROW_GAP + ARROW_WIDTH + padding.right(),
                this.lineHeight + padding.vertical());
    }

    @Override
    protected void paint(final Painter painter, final Rect bounds) {
        final boolean enabled = this.isEnabled();
        final Paint face;
        if (!enabled) {
            face = this.disabledPaint;
        } else if (this.isHovered() || this.isFocused() || this.isOpen()) {
            face = this.hoveredPaint;
        } else {
            face = this.normal;
        }
        face.paint(painter, bounds);

        final Insets padding = this.padding;
        final float textWidth = bounds.width() - padding.horizontal() - ARROW_GAP - ARROW_WIDTH;
        if (this.selectedIndex != this.shownIndex || textWidth != this.shownForWidth) {
            final TextMetrics metrics = this.environment().text();
            final Text text = this.label(this.selectedIndex);
            this.shown = TextLine.of(text, this.textStyle, metrics).ellipsize(textWidth, this.textStyle, metrics);
            this.shownDisabled = TextLine.of(text, this.disabledTextStyle, metrics)
                    .ellipsize(textWidth, this.disabledTextStyle, metrics);
            this.shownIndex = this.selectedIndex;
            this.shownForWidth = textWidth;
        }
        final float innerY = bounds.y() + padding.top();
        final float innerHeight = bounds.height() - padding.vertical();
        final float textY = innerY + (innerHeight - this.lineHeight) * 0.5f;
        (enabled ? this.shown : this.shownDisabled).draw(painter, bounds.x() + padding.left(), textY);

        // A down arrow from four shrinking lines
        final int color = (enabled ? this.textStyle : this.disabledTextStyle).color();
        final float arrowX = bounds.right() - padding.right() - ARROW_WIDTH;
        final float arrowY = innerY + (innerHeight - 4f) * 0.5f;
        for (int i = 0; i < 4; i++) {
            painter.fill(new Rect(arrowX + i, arrowY + i, ARROW_WIDTH - i * 2f, 1f), color);
        }
    }

    // ------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------

    @Override
    protected void onClick(final PointerButtonEvent event) {
        if (event.button() == MouseButton.LEFT && this.isEnabled()) {
            if (this.isOpen()) {
                this.close();
            } else {
                this.userOpen();
            }
            event.consume();
        }
    }

    @Override
    protected void onKey(final KeyEvent event) {
        if (!event.action().isDown() || !this.isEnabled() || this.options.isEmpty()) {
            return;
        }
        final Key key = event.key();
        final boolean press = event.action() == KeyAction.PRESS;
        if (this.isOpen()) {
            final int current = this.list.selectedIndex();
            switch (key) {
                case UP -> this.list.select(Math.max(0, current - 1));
                case DOWN -> this.list.select(Math.min(this.options.size() - 1, current + 1));
                case ESCAPE -> this.close();
                default -> {
                    if (press && (key.isEnter() || key == Key.SPACE)) {
                        final int chosen = current;
                        this.close();
                        if (chosen >= 0) {
                            this.userSelect(chosen);
                        }
                    } else {
                        return;
                    }
                }
            }
            event.consume();
            return;
        }
        if (press && (key.isEnter() || key == Key.SPACE || key == Key.DOWN && event.modifiers().alt())) {
            this.userOpen();
            event.consume();
            return;
        }
        final int last = this.options.size() - 1;
        final int next = switch (key) {
            case UP -> Math.max(0, this.selectedIndex - 1);
            case DOWN -> Math.min(last, this.selectedIndex + 1);
            case HOME -> 0;
            case END -> last;
            default -> -1;
        };
        if (next >= 0) {
            this.userSelect(next);
            event.consume();
        }
    }
}
