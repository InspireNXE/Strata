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
package org.inspirenxe.strata.core.control.button;

import org.inspirenxe.strata.api.Strata;
import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.button.CycleButton;
import org.inspirenxe.strata.api.event.input.KeyEvent;
import org.inspirenxe.strata.api.event.input.PointerButtonEvent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.core.control.Caption;
import org.inspirenxe.strata.core.control.Activation;
import org.inspirenxe.strata.core.control.Rings;
import org.inspirenxe.strata.core.node.BuiltInComponent;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;
import org.inspirenxe.strata.core.text.TextLine;

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
 * <p>The button is focusable and tabbable, shows a hand cursor, is at least 20 units wide, and measures at least 20
 * tall, though a smaller height set on it applies. Its desired size is
 * the widest text over all items, with the theme's {@value Button#STYLE_PADDING} around it, or
 * {@link ButtonImpl#DEFAULT_PADDING} when the theme sets none. A new button has no items and shows only its label.</p>
 *
 * @param <T> the type of the values
 */
public final class CycleButtonImpl<T> extends BuiltInComponent<CycleButton<T>> implements CycleButton<T> {
    private static final Set<String> REQUIRED = Set.of(Button.STYLE_NORMAL, Button.STYLE_HOVERED,
            Button.STYLE_PRESSED, Button.STYLE_DISABLED);

    private Text text;
    private boolean textEmpty;
    private List<T> items;
    private Function<? super T, ?> valueLabelFunction;
    private Function<? super T, ?> keyFunction;
    private int selectedIndex;

    private Paint normal;
    private Paint hoveredPaint;
    private Paint pressedPaint;
    private Paint disabled;
    private Paint focusRing;
    private TextStyle textStyle;
    private TextStyle disabledTextStyle;

    private final Activation activation;
    private final Rings.Ring ring = new Rings.Ring();

    // Computed during measure
    private float lineHeight;
    private Insets padding = ButtonImpl.DEFAULT_PADDING;
    private final Caption caption = new Caption();

    // The joined text last painted, for the selected index it was made for; -2 when it must be made again
    private int shownIndex;
    private TextLine shown;
    private TextLine shownDisabled;

    /**
     * Creates a button with no items.
     *
     * @param ui the factory that owns the button
     * @param text the label before the value
     * @param valueLabelFunction returns the text for a value
     */
    @SuppressWarnings("this-escape")
    public CycleButtonImpl(final ComponentFactoryImpl ui, final Text text,
            final Function<? super T, ?> valueLabelFunction) {
        super(ui);
        this.text = Objects.requireNonNull(text, "text");
        this.valueLabelFunction = Objects.requireNonNull(valueLabelFunction, "valueLabelFunction");
        this.items = List.of();
        this.keyFunction = Function.identity();
        this.selectedIndex = -1;
        this.shownIndex = -2;
        this.shown = TextLine.EMPTY;
        this.shownDisabled = TextLine.EMPTY;
        this.normal = Paint.NONE;
        this.hoveredPaint = Paint.NONE;
        this.pressedPaint = Paint.NONE;
        this.disabled = Paint.NONE;
        this.focusRing = Paint.NONE;
        this.textStyle = TextStyle.DEFAULT;
        this.disabledTextStyle = TextStyle.DEFAULT;
        this.activation = new Activation(this::activatesOnPress, this::clicked);
        this.setCursor(CursorKind.HAND);
        this.setMinSize(20f, 0f);
    }

    // ------------------------------------------------------------------
    // CycleButton
    // ------------------------------------------------------------------

    @Override
    public Text text() {
        return this.text;
    }

    @Override
    public CycleButton<T> setText(final Text text) {
        this.text = Objects.requireNonNull(text, "text");
        this.shownIndex = -2;
        this.invalidateLayout();
        return this;
    }

    @Override
    public CycleButton<T> setValueLabelFunction(final Function<? super T, ?> valueLabelFunction) {
        this.valueLabelFunction = Objects.requireNonNull(valueLabelFunction, "valueLabelFunction");
        this.shownIndex = -2;
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
        this.shownIndex = -2;
        this.invalidateLayout();
        if (index != beforeIndex || !this.selected().equals(before)) {
            this.report(SELECTED, index, beforeIndex);
        }
        return this;
    }

    @Override
    public CycleButton<T> refresh() {
        this.shownIndex = -2;
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
            this.shownIndex = -2;
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

    private Text valueLabel(final int index) {
        return index < 0 ? Text.empty() : Strata.appearances().nameOf(this.valueLabelFunction.apply(this.items.get(index)));
    }

    /**
     * Joins the label and a value as shown: {@code label: value}, or whichever of them is not empty.
     */
    private Text join(final Text value) {
        if (this.textEmpty) {
            return value;
        }
        return value.isEmpty() ? this.text : this.text.append(": ").append(value);
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
        this.padding = theme.insetsOr(Button.STYLE_PADDING, ButtonImpl.DEFAULT_PADDING);
        this.caption.applyTheme(theme, Button.STYLE_CLIP, Button.STYLE_TEXT_SCROLL, Button.STYLE_TEXT_SCROLL_MINIMUM);
        this.shownIndex = -2;
    }

    @Override
    protected Size measureMinimum(final MeasureContext context) {
        // The caption does not shrink, and measuring does not depend on the space offered
        return this.measure(context, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        final TextMetrics metrics = context.text();
        this.textEmpty = this.text.isEmpty();
        float widest = TextLine.of(this.join(Text.empty()), this.textStyle, metrics).width();
        float height = Math.max(metrics.lineHeight(this.textStyle), metrics.lineHeight(this.disabledTextStyle));
        for (int i = 0; i < this.items.size(); i++) {
            final Text joined = this.join(this.valueLabel(i));
            final TextLine line = TextLine.of(joined, this.textStyle, metrics);
            final TextLine disabledLine = TextLine.of(joined, this.disabledTextStyle, metrics);
            widest = Math.max(widest, Math.max(line.width(), disabledLine.width()));
            height = Math.max(height, Math.max(line.height(), disabledLine.height()));
        }
        this.lineHeight = height;
        this.shownIndex = -2;
        return new Size(widest + this.padding.horizontal(),
                Math.max(ButtonImpl.MIN_HEIGHT, this.lineHeight + this.padding.vertical()));
    }

    @Override
    protected float baseline() {
        final Insets padding = this.padding;
        return padding.top() + (this.desiredHeight() - padding.vertical() - this.lineHeight) * 0.5f
                + this.environment().text().ascent(this.textStyle);
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
            this.ring.paint(painter, this.focusRing, bounds);
        }

        if (this.selectedIndex != this.shownIndex) {
            final TextMetrics metrics = this.environment().text();
            final Text joined = this.join(this.valueLabel(this.selectedIndex));
            this.shown = TextLine.of(joined, this.textStyle, metrics);
            this.shownDisabled = TextLine.of(joined, this.disabledTextStyle, metrics);
            this.shownIndex = this.selectedIndex;
        }
        final TextLine line = enabled ? this.shown : this.shownDisabled;
        final Insets padding = this.padding;
        this.caption.draw(painter, line, bounds, padding,
                bounds.y() + padding.top() + (bounds.height() - padding.vertical() - this.lineHeight) * 0.5f,
                this.environment().millis());
    }

    // ------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------

    @Override
    protected void onPointerPress(final PointerButtonEvent event) {
        this.activation.press(event);
    }

    @Override
    protected void onClick(final PointerButtonEvent event) {
        this.activation.click(event);
    }

    /**
     * Acts on the pointer press or click the platform's conventions say buttons act on: the left button steps
     * forward, or back with Shift, and the right button steps back.
     */
    private void clicked(final PointerButtonEvent event) {
        final MouseButton button = event.button();
        if (button == MouseButton.LEFT || button == MouseButton.RIGHT) {
            this.userCycle(button == MouseButton.RIGHT || event.modifiers().shift() ? -1 : 1);
            event.consume();
        }
    }

    @Override
    protected void onKey(final KeyEvent event) {
        if (Activation.isActivationKey(event)) {
            this.userCycle(event.modifiers().shift() ? -1 : 1);
            event.consume();
        }
    }
}
