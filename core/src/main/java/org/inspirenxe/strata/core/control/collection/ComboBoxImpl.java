package org.inspirenxe.strata.core.control.collection;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.control.collection.ComboBox;
import org.inspirenxe.strata.api.control.collection.ListView;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.event.KeyEvent;
import org.inspirenxe.strata.api.event.PointerButtonEvent;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geom.Insets;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.geom.Size;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.overlay.Overlay;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextOverflow;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.core.text.RichText;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The core {@link ComboBox}. The open list is a {@link ListView} in a popup, not focusable, whose selection is the
 * highlight; it is built when the list opens and dropped when it closes. Its rows are one line plus 4 units tall,
 * with the option text 4 units in from the left.
 *
 * <p>The box draws its text {@link #PADDING} units in from the left, cut short with {@code ...} if it does not fit,
 * and a small down arrow {@link #PADDING} units in from the right.</p>
 *
 * @param <T> the type of the options
 */
public final class ComboBoxImpl<T> extends CustomComponent<ComboBox<T>> implements ComboBox<T> {
    /**
     * The space between the box's edge and its text or arrow, in UI units.
     */
    public static final float PADDING = 4f;
    /**
     * The width of the arrow, in UI units.
     */
    public static final float ARROW_WIDTH = 7f;
    private static final Set<String> REQUIRED = Set.of(STYLE_NORMAL, STYLE_POPUP);

    private final ComponentFactory ui;
    private final List<Consumer<T>> listeners;
    private List<T> options;
    private Function<? super T, String> labelFunction;
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

    // Shortened caption for the last bounds width
    private String shownFor;
    private float shownForWidth;
    private TextSpan shown;
    private TextSpan shownDisabled;

    /**
     * Creates a box with no options.
     *
     * @param ui the factory that owns the box
     * @param labelFunction returns the text for an option
     */
    @SuppressWarnings("this-escape")
    public ComboBoxImpl(final ComponentFactory ui, final Function<? super T, String> labelFunction) {
        super(ui);
        this.ui = ui;
        this.listeners = new ArrayList<>();
        this.options = List.of();
        this.labelFunction = Objects.requireNonNull(labelFunction, "labelFunction");
        this.selectedIndex = -1;
        this.maxVisible = DEFAULT_MAX_VISIBLE_OPTIONS;
        this.normal = Paint.NONE;
        this.hoveredPaint = Paint.NONE;
        this.disabledPaint = Paint.NONE;
        this.popupPaint = Paint.NONE;
        this.textStyle = TextStyle.DEFAULT;
        this.disabledTextStyle = TextStyle.DEFAULT;
        this.shownForWidth = Float.NaN;
        this.setFocusable(true);
        this.setTabbable(true);
        this.setCursor(CursorKind.HAND);
    }

    // ------------------------------------------------------------------
    // ComboBox
    // ------------------------------------------------------------------

    @Override
    public List<T> options() {
        return this.options;
    }

    @Override
    public ComboBox<T> setOptions(final List<? extends T> options) {
        final Optional<T> before = this.selected();
        this.options = List.copyOf(options);
        int index = this.options.isEmpty() ? -1 : 0;
        if (before.isPresent()) {
            final int found = this.options.indexOf(before.get());
            if (found >= 0) {
                index = found;
            }
        }
        this.close();
        this.selectedIndex = index;
        this.invalidateLayout();
        if (!this.selected().equals(before) && index >= 0) {
            this.fire();
        }
        return this;
    }

    @Override
    public ComboBox<T> setLabelFunction(final Function<? super T, String> labelFunction) {
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
        Objects.checkIndex(index, this.options.size());
        if (index != this.selectedIndex) {
            this.selectedIndex = index;
            this.fire();
        }
        return this;
    }

    @Override
    public ComboBox<T> selectOption(final T option) {
        final int index = this.options.indexOf(option);
        return index < 0 ? this : this.select(index);
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
        if (this.isOpen() || !this.isAttached() || !this.isEnabled() || this.options.isEmpty()) {
            return this;
        }
        final float rowHeight = this.lineHeight + 4f;
        final ListViewImpl<T> options = new ListViewImpl<>(this.ui, this::cell);
        options.setFocusable(false);
        options.setTabbable(false);
        options.setRowHeight(rowHeight);
        options.setActivateOnClick(true);
        options.setItems(this.options);
        options.setBackground(this.popupPaint);
        options.setMinSize(this.bounds().width(), 0f);
        options.setHeight(rowHeight * Math.min(this.maxVisible, this.options.size()));
        if (this.selectedIndex >= 0) {
            options.select(this.selectedIndex);
        }
        options.onActivate(option -> {
            this.close();
            this.selectOption(option);
        });
        this.list = options;
        this.popup = this.ui.overlays().popup(options, this.bounds());
        this.popup.onClosed(() -> {
            this.popup = null;
            this.list = null;
        });
        return this;
    }

    private Label cell(final T option) {
        final Label label = this.ui.label(this.labelFunction.apply(option)).setStyle(this.textStyle);
        label.setMargin(Insets.of(PADDING, 0f));
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

    @Override
    public ComboBox<T> onSelectionChanged(final Consumer<T> listener) {
        this.listeners.add(Objects.requireNonNull(listener, "listener"));
        return this;
    }

    private void fire() {
        this.shownFor = null;
        final T option = this.options.get(this.selectedIndex);
        for (final Consumer<T> listener : List.copyOf(this.listeners)) {
            listener.accept(option);
        }
    }

    private String label(final int index) {
        return index < 0 ? "" : this.labelFunction.apply(this.options.get(index));
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
        this.popupPaint = theme.paintOr(STYLE_POPUP, Paint.NONE);
        this.textStyle = theme.textStyleOr(STYLE_TEXT, TextStyle.DEFAULT);
        this.disabledTextStyle = theme.textStyleOr(STYLE_TEXT_DISABLED, this.textStyle);
        this.shownFor = null;
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        final TextMetrics metrics = context.text();
        float widest = 0f;
        for (int i = 0; i < this.options.size(); i++) {
            widest = Math.max(widest, metrics.width(new TextSpan(this.label(i), this.textStyle)));
        }
        this.lineHeight = Math.max(metrics.lineHeight(this.textStyle), metrics.lineHeight(this.disabledTextStyle));
        this.shownFor = null;
        return new Size(PADDING + widest + PADDING + ARROW_WIDTH + PADDING, this.lineHeight + 6f);
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

        final String text = this.label(this.selectedIndex);
        final float textWidth = bounds.width() - PADDING * 3f - ARROW_WIDTH;
        if (!text.equals(this.shownFor) || textWidth != this.shownForWidth) {
            final TextMetrics metrics = this.environment().text();
            this.shown = shorten(new TextSpan(text, this.textStyle), textWidth, metrics);
            this.shownDisabled = shorten(new TextSpan(text, this.disabledTextStyle), textWidth, metrics);
            this.shownFor = text;
            this.shownForWidth = textWidth;
        }
        final float textY = bounds.y() + (bounds.height() - this.lineHeight) * 0.5f;
        painter.text(enabled ? this.shown : this.shownDisabled, bounds.x() + PADDING, textY);

        // A down arrow from four shrinking lines
        final int color = (enabled ? this.textStyle : this.disabledTextStyle).color();
        final float arrowX = bounds.right() - PADDING - ARROW_WIDTH;
        final float arrowY = bounds.y() + (bounds.height() - 4f) * 0.5f;
        for (int i = 0; i < 4; i++) {
            painter.fill(new Rect(arrowX + i, arrowY + i, ARROW_WIDTH - i * 2f, 1f), color);
        }
    }

    private static TextSpan shorten(final TextSpan span, final float width, final TextMetrics metrics) {
        final List<TextSpan> line = RichText.ellipsize(List.of(span), width, metrics);
        return line.isEmpty() ? new TextSpan("", span.style()) : line.getFirst();
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
                this.open();
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
                            this.select(chosen);
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
            this.open();
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
            this.select(next);
            event.consume();
        }
    }
}
