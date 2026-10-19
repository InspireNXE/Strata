package org.inspirenxe.strata.core.control.button;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.button.CycleButton;
import org.inspirenxe.strata.api.event.KeyEvent;
import org.inspirenxe.strata.api.event.PointerButtonEvent;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.geom.Size;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * The core {@link CycleButton}. It draws with the {@link Button} theme keys: the disabled paint when disabled, the
 * pressed paint while held under the pointer, the hovered paint while hovered or focused, and the normal paint
 * otherwise, plus the focus ring when the theme has one. The text is {@code label: value}, centered.
 *
 * <p>The button is focusable and tabbable, shows a hand cursor, and is at least 20 by 20 units. Its desired size is
 * the widest text over all items, with 8 units of padding on the left and right and 5 on the top and bottom. A new
 * button has no items and shows only its label.</p>
 *
 * @param <T> the type of the values
 */
public final class CycleButtonImpl<T> extends CustomComponent<CycleButton<T>> implements CycleButton<T> {
    private static final Set<String> REQUIRED = Set.of(Button.STYLE_NORMAL, Button.STYLE_HOVERED,
            Button.STYLE_PRESSED, Button.STYLE_DISABLED);

    private String text;
    private List<T> items;
    private Function<? super T, String> valueLabelFunction;
    private Function<? super T, ?> keyFunction;
    private int selectedIndex;

    private Paint normal;
    private Paint hoveredPaint;
    private Paint pressedPaint;
    private Paint disabled;
    private Paint focusRing;
    private TextStyle textStyle;
    private TextStyle disabledTextStyle;

    // Computed during measure
    private float lineHeight;

    // The joined text last painted, with its spans and widths
    private String shownFor;
    private TextSpan shown;
    private TextSpan shownDisabled;
    private float shownWidth;
    private float shownDisabledWidth;

    /**
     * Creates a button with no items.
     *
     * @param ui the factory that owns the button
     * @param text the label before the value
     * @param valueLabelFunction returns the text for a value
     */
    @SuppressWarnings("this-escape")
    public CycleButtonImpl(final ComponentFactory ui, final String text,
            final Function<? super T, String> valueLabelFunction) {
        super(ui);
        this.text = Objects.requireNonNull(text, "text");
        this.valueLabelFunction = Objects.requireNonNull(valueLabelFunction, "valueLabelFunction");
        this.items = List.of();
        this.keyFunction = Function.identity();
        this.selectedIndex = -1;
        this.normal = Paint.NONE;
        this.hoveredPaint = Paint.NONE;
        this.pressedPaint = Paint.NONE;
        this.disabled = Paint.NONE;
        this.focusRing = Paint.NONE;
        this.textStyle = TextStyle.DEFAULT;
        this.disabledTextStyle = TextStyle.DEFAULT;
        this.setFocusable(true);
        this.setTabbable(true);
        this.setCursor(CursorKind.HAND);
        this.setMinSize(20f, 20f);
    }

    // ------------------------------------------------------------------
    // CycleButton
    // ------------------------------------------------------------------

    @Override
    public String text() {
        return this.text;
    }

    @Override
    public CycleButton<T> setText(final String text) {
        this.text = Objects.requireNonNull(text, "text");
        this.shownFor = null;
        this.invalidateLayout();
        return this;
    }

    @Override
    public CycleButton<T> setValueLabelFunction(final Function<? super T, String> valueLabelFunction) {
        this.valueLabelFunction = Objects.requireNonNull(valueLabelFunction, "valueLabelFunction");
        this.shownFor = null;
        this.invalidateLayout();
        return this;
    }

    @Override
    public List<T> items() {
        return this.items;
    }

    @Override
    public CycleButton<T> setItems(final List<? extends T> items) {
        final Optional<T> before = this.selected();
        final int beforeIndex = this.selectedIndex;
        final boolean had = beforeIndex >= 0;
        final T beforeItem = had ? this.items.get(beforeIndex) : null;
        this.items = List.copyOf(items);
        int index = this.items.isEmpty() ? -1 : 0;
        if (had) {
            final int found = this.indexOfKey(beforeItem);
            if (found >= 0) {
                index = found;
            }
        }
        this.selectedIndex = index;
        this.shownFor = null;
        this.invalidateLayout();
        if (index != beforeIndex || !this.selected().equals(before)) {
            this.report(SELECTED, index, beforeIndex);
        }
        return this;
    }

    @Override
    public CycleButton<T> refresh() {
        this.shownFor = null;
        this.invalidateLayout();
        return this;
    }

    @Override
    public CycleButton<T> setKeyFunction(final Function<? super T, ?> keyFunction) {
        this.keyFunction = Objects.requireNonNull(keyFunction, "keyFunction");
        return this;
    }

    /**
     * Finds the first item with the same key as another.
     *
     * @return the item's index, or -1 if none has the key
     */
    private int indexOfKey(final T item) {
        final Object key = item == null ? null : this.keyFunction.apply(item);
        for (int i = 0; i < this.items.size(); i++) {
            final T candidate = this.items.get(i);
            if (Objects.equals(key, candidate == null ? null : this.keyFunction.apply(candidate))) {
                return i;
            }
        }
        return -1;
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
    public CycleButton<T> select(final int index) {
        if (index != -1) {
            Objects.checkIndex(index, this.items.size());
        }
        final int previous = this.selectedIndex;
        if (index != previous) {
            this.selectedIndex = index;
            this.shownFor = null;
            this.report(SELECTED, index, previous);
        }
        return this;
    }

    @Override
    public CycleButton<T> selectItem(final T item) {
        return this.select(this.indexOfKey(item));
    }

    @Override
    public CycleButton<T> cycle(final int steps) {
        final int target = this.target(steps);
        if (target >= 0) {
            this.select(target);
        }
        return this;
    }

    /**
     * Returns the index that a number of steps from the selection lands on, wrapping at the ends.
     *
     * @return the index, or -1 if there are no items
     */
    private int target(final int steps) {
        final int count = this.items.size();
        if (count == 0) {
            return -1;
        }
        final int from = this.selectedIndex >= 0 ? this.selectedIndex : steps > 0 ? -1 : 0;
        return Math.floorMod(from + steps % count, count);
    }

    /**
     * Asks to step for the user. Does nothing when the step would not change the selection.
     */
    private void userCycle(final int steps) {
        final int target = this.target(steps);
        if (this.isEnabled() && target >= 0 && target != this.selectedIndex) {
            this.request(SELECT, target, this::applySelect);
        }
    }

    /**
     * Selects the index a request ended with. A null or an index outside the items is ignored; -1 clears.
     */
    private void applySelect(final Integer index) {
        if (index != null && index >= -1 && index < this.items.size()) {
            this.select(index);
        }
    }

    private String valueLabel(final int index) {
        return index < 0 ? "" : this.valueLabelFunction.apply(this.items.get(index));
    }

    /**
     * Joins the label and a value as shown: {@code label: value}, or whichever of them is not empty.
     */
    private String join(final String value) {
        if (this.text.isEmpty()) {
            return value;
        }
        return value.isEmpty() ? this.text : this.text + ": " + value;
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
        this.normal = theme.paintOr(Button.STYLE_NORMAL, Paint.NONE);
        this.hoveredPaint = theme.paintOr(Button.STYLE_HOVERED, this.normal);
        this.pressedPaint = theme.paintOr(Button.STYLE_PRESSED, this.hoveredPaint);
        this.disabled = theme.paintOr(Button.STYLE_DISABLED, this.normal);
        this.focusRing = theme.paintOr(Button.STYLE_FOCUS, Paint.NONE);
        this.textStyle = theme.textStyleOr(Button.STYLE_TEXT, TextStyle.DEFAULT);
        this.disabledTextStyle = theme.textStyleOr(Button.STYLE_TEXT_DISABLED, this.textStyle);
        this.shownFor = null;
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        final TextMetrics metrics = context.text();
        float widest = metrics.width(new TextSpan(this.join(""), this.textStyle));
        for (int i = 0; i < this.items.size(); i++) {
            final String joined = this.join(this.valueLabel(i));
            widest = Math.max(widest, Math.max(metrics.width(new TextSpan(joined, this.textStyle)),
                    metrics.width(new TextSpan(joined, this.disabledTextStyle))));
        }
        this.lineHeight = Math.max(metrics.lineHeight(this.textStyle), metrics.lineHeight(this.disabledTextStyle));
        this.shownFor = null;
        return new Size(widest + 16f, this.lineHeight + 10f);
    }

    @Override
    protected void paint(final Painter painter, final Rect bounds) {
        final boolean enabled = this.isEnabled();
        final Paint face;
        if (!enabled) {
            face = this.disabled;
        } else if (this.isPressed() && this.isHovered()) {
            face = this.pressedPaint;
        } else if (this.isHovered() || this.isFocused()) {
            face = this.hoveredPaint;
        } else {
            face = this.normal;
        }
        face.paint(painter, bounds);

        if (this.isFocused() && this.focusRing != Paint.NONE) {
            this.focusRing.paint(painter, new Rect(bounds.x(), bounds.y(), bounds.width(), 1f));
            this.focusRing.paint(painter, new Rect(bounds.x(), bounds.bottom() - 1f, bounds.width(), 1f));
            this.focusRing.paint(painter, new Rect(bounds.x(), bounds.y() + 1f, 1f, bounds.height() - 2f));
            this.focusRing.paint(painter, new Rect(bounds.right() - 1f, bounds.y() + 1f, 1f, bounds.height() - 2f));
        }

        final String joined = this.join(this.valueLabel(this.selectedIndex));
        if (!joined.equals(this.shownFor)) {
            final TextMetrics metrics = this.environment().text();
            this.shown = new TextSpan(joined, this.textStyle);
            this.shownDisabled = new TextSpan(joined, this.disabledTextStyle);
            this.shownWidth = metrics.width(this.shown);
            this.shownDisabledWidth = metrics.width(this.shownDisabled);
            this.shownFor = joined;
        }
        final float width = enabled ? this.shownWidth : this.shownDisabledWidth;
        painter.text(enabled ? this.shown : this.shownDisabled, bounds.x() + (bounds.width() - width) * 0.5f,
                bounds.y() + (bounds.height() - this.lineHeight) * 0.5f);
    }

    // ------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------

    @Override
    protected void onClick(final PointerButtonEvent event) {
        final MouseButton button = event.button();
        if (button == MouseButton.LEFT || button == MouseButton.RIGHT) {
            this.userCycle(button == MouseButton.RIGHT || event.modifiers().shift() ? -1 : 1);
            event.consume();
        }
    }

    @Override
    protected void onKey(final KeyEvent event) {
        if (event.action() == KeyAction.PRESS && (event.key().isEnter() || event.key() == Key.SPACE)) {
            this.userCycle(event.modifiers().shift() ? -1 : 1);
            event.consume();
        }
    }
}
