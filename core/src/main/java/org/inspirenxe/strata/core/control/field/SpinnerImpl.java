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
package org.inspirenxe.strata.core.control.field;

import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.field.Spinner;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.component.Focusable;
import org.inspirenxe.strata.api.event.input.KeyEvent;
import org.inspirenxe.strata.api.event.input.PointerButtonEvent;
import org.inspirenxe.strata.api.event.input.PointerEvent;
import org.inspirenxe.strata.api.event.input.ScrollEvent;
import org.inspirenxe.strata.api.extension.ArrangeContext;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.core.control.Rings;
import org.inspirenxe.strata.core.node.BuiltInComposite;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;
import org.inspirenxe.strata.core.text.Numbers;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;
import org.inspirenxe.strata.api.component.Validation;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.core.control.Validity;
import org.inspirenxe.strata.core.state.ConversionTarget;
import java.util.function.Function;

/**
 * The core {@link Spinner}: a {@link TextBoxImpl} child with the two step buttons painted in a column at the right.
 * The buttons are not components; the spinner itself is what the pointer hits there. While the spinner is disabled
 * its field is disabled too.
 *
 * <p>The step buttons, the wheel, Up, Down, Page Up and Page Down, and typed text taken on Enter or focus loss fire
 * {@link Spinner#CHANGE} when they would change the value. A step first takes any typed text, which can fire it once
 * for the text and once for the step. Whatever the handlers do, the field then shows the current value.
 * {@link #increment} and {@link #decrement} called from code fire no request.</p>
 *
 * <p>A step adds the step to the value, then clamps and rounds it. When rounding to the decimals would undo the
 * step, as a step of 0.25 with no decimals does, the value moves by one unit of the last decimal instead, so a step
 * moves the value whenever the range leaves room.</p>
 *
 * <p>The field is a private {@link TextBoxImpl} that shows the value through {@link Numbers} of the translator's
 * locale. Its internal filter takes ASCII digits and the locale's, one decimal separator, the locale's or a period,
 * followed by no more digits than the decimals allow, and a leading minus sign, the locale's or a hyphen, when the
 * minimum is negative. A measure that finds the translator's locale changed shows the value again in the new one.
 * Taking the text clamps the number to the range. The spinner is a composite and not a container, so the field's {@link TextBox} events stop
 * at the spinner.</p>
 */
public final class SpinnerImpl extends BuiltInComposite<Spinner> implements Spinner, ConversionTarget {
    private final Validity validity;
    private final TextBoxImpl field;
    private double value;
    private double min;
    private double max;
    private double step;
    private int decimals;
    private Pattern accepted;
    private Numbers numbers;
    private float pointerY;
    private Rect buttons;
    // The halves of the button column, worked out during arrange
    private Rect upButton;
    private Rect downButton;

    // Theme
    private Paint button;
    private Paint buttonHovered;
    private Paint buttonDisabled;
    private int arrowColor;
    private int arrowDisabledColor;

    /**
     * Creates a spinner from 0 to 100 in steps of 1, at 0.
     *
     * @param ui the factory that owns the spinner
     */
    @SuppressWarnings("this-escape")
    public SpinnerImpl(final ComponentFactoryImpl ui) {
        super(ui);
        this.validity = new Validity(this.node(), this::validated);
        this.max = 100.0;
        this.step = 1.0;
        this.buttons = Rect.ZERO;
        this.upButton = Rect.ZERO;
        this.downButton = Rect.ZERO;
        this.button = Paint.NONE;
        this.buttonHovered = Paint.NONE;
        this.buttonDisabled = Paint.NONE;
        this.field = new TextBoxImpl(ui);
        this.field.setAnchor(Anchor.FILL);
        this.field.setInternalFilter(text -> this.accepted.matcher(text).matches() ? text : null);
        this.field.onEvent(TextBox.SUBMITTED, event -> this.commit(true));
        this.field.onEvent(Focusable.FOCUS_LOST, event -> this.commit(true));
        this.setFocusDelegate(this.field);
        this.addChild(this.field);
        this.numbers = Numbers.current();
        this.updatePattern();
        this.show();
        this.setCursor(CursorKind.HAND);
    }

    @Override
    public double value() {
        return this.value;
    }

    @Override
    public Spinner setValue(final double value) {
        if (Double.isNaN(value)) {
            throw new IllegalArgumentException("Value must not be NaN");
        }
        final double next = this.fit(value);
        final double previous = this.value;
        this.value = next;
        this.show();
        if (next != previous) {
            this.report(CHANGED, next, previous);
            this.validity.check();
        }
        return this;
    }

    @Override
    public double min() {
        return this.min;
    }

    @Override
    public double max() {
        return this.max;
    }

    @Override
    public Spinner setRange(final double min, final double max) {
        if (!Double.isFinite(min) || !Double.isFinite(max) || min > max) {
            throw new IllegalArgumentException("Range must be finite with min <= max: " + min + " to " + max);
        }
        this.min = min;
        this.max = max;
        this.updatePattern();
        return this.setValue(this.value);
    }

    @Override
    public double step() {
        return this.step;
    }

    @Override
    public Spinner setStep(final double step) {
        if (!(step > 0.0) || Double.isInfinite(step)) {
            throw new IllegalArgumentException("Step must be finite and greater than 0: " + step);
        }
        this.step = step;
        return this;
    }

    @Override
    public int decimals() {
        return this.decimals;
    }

    @Override
    public Spinner setDecimals(final int decimals) {
        if (decimals < 0 || decimals > 9) {
            throw new IllegalArgumentException("Decimals must be from 0 to 9: " + decimals);
        }
        this.decimals = decimals;
        this.updatePattern();
        return this.setValue(this.value);
    }

    @Override
    public HorizontalAlignment textAlignment() {
        return this.field.textAlignment();
    }

    @Override
    public Spinner setTextAlignment(final HorizontalAlignment alignment) {
        this.field.setTextAlignment(alignment);
        return this;
    }

    @Override
    public void increment() {
        if (this.isEnabled()) {
            this.commit(false);
            this.setValue(this.stepped(1));
        }
    }

    @Override
    public void decrement() {
        if (this.isEnabled()) {
            this.commit(false);
            this.setValue(this.stepped(-1));
        }
    }

    /**
     * Returns the value some steps from the current one, clamped and rounded. When rounding to the decimals would
     * undo the steps, it is the value one unit of the last decimal away instead, so a step finer than the decimals
     * still moves the value while the range leaves room.
     */
    private double stepped(final int steps) {
        final double target = this.fit(this.value + this.step * steps);
        if (target != this.value || steps == 0) {
            return target;
        }
        final double unit = BigDecimal.ONE.movePointLeft(this.decimals).doubleValue();
        return this.fit(this.value + (steps > 0 ? unit : -unit));
    }

    /**
     * Steps the value for the user: takes the typed text first, then moves by {@code steps} steps in one
     * {@code change}. Does nothing while disabled.
     */
    private void stepBy(final int steps) {
        if (this.isEnabled()) {
            this.commit(true);
            this.change(this.stepped(steps));
        }
    }

    /**
     * Sets a value the user asked for. A value that differs from the current one after clamping and rounding fires
     * {@link Spinner#CHANGE}, whose final value goes through {@code setValue}, and then the field shows the value
     * again, whatever the handlers did.
     */
    private void change(final double requested) {
        final double fitted = this.fit(requested);
        if (fitted != this.value) {
            this.request(CHANGE, fitted, value -> {
                // A handler that rewrote the value to null or NaN leaves it as it is
                if (value != null && !value.isNaN()) {
                    this.setValue(value);
                }
            });
        }
        this.show();
    }

    /**
     * Clamps and rounds a value, then clamps again in case rounding left the range.
     */
    private double fit(final double value) {
        final double clamped = Math.max(this.min, Math.min(this.max, value));
        final double rounded = BigDecimal.valueOf(clamped).setScale(this.decimals, RoundingMode.HALF_UP).doubleValue();
        return Math.max(this.min, Math.min(this.max, rounded));
    }

    private String format(final double value) {
        return this.numbers.format(value, this.decimals);
    }

    /**
     * Shows the value in the field.
     */
    private void show() {
        this.field.setText(this.format(this.value));
    }

    /**
     * Turns the field's text into the value, or puts the value back when the text is not a number.
     *
     * @param user true to set the value through {@code change}, false to call {@link #setValue} directly
     */
    private void commit(final boolean user) {
        final BigDecimal number = this.numbers.parse(this.field.text(), false);
        if (number == null) {
            this.show();
            return;
        }
        final double parsed = number.doubleValue();
        if (user) {
            this.change(parsed);
        } else {
            this.setValue(parsed);
        }
    }

    private void updatePattern() {
        final String digit = "[0-9" + escaped(this.numbers.zero()) + "-" + escaped((char) (this.numbers.zero() + 9))
                + "]";
        final String sign = this.min < 0.0 ? "[-\\x{2212}" + escaped(this.numbers.minus()) + "]?" : "";
        final String fraction = this.decimals > 0
                ? "([." + escaped(this.numbers.separator()) + "]" + digit + "{0," + this.decimals + "})?" : "";
        this.accepted = Pattern.compile(sign + digit + "*" + fraction);
    }

    /**
     * Writes a char as a regular expression escape, so a locale's sign or digit can sit in a character class.
     */
    private static String escaped(final char c) {
        return "\\x{" + Integer.toHexString(c) + "}";
    }

    // ------------------------------------------------------------------
    // Theme, layout and painting
    // ------------------------------------------------------------------

    // ------------------------------------------------------------------
    // Validatable
    // ------------------------------------------------------------------

    @Override
    public Validation validation() {
        return this.validity.validation();
    }

    @Override
    public Spinner setValidation(final Validation validation) {
        this.validity.set(validation);
        return this;
    }

    @Override
    public Spinner setValidator(final Function<? super Spinner, Validation> validator) {
        this.validity.setValidator(validator == null ? null : () -> validator.apply(this));
        return this;
    }

    @Override
    public void setConversionError(final Text message) {
        this.validity.setConversionError(message);
    }

    private void validated(final Validation validation, final Validation previous) {
        this.report(VALIDATED, validation, previous);
    }

    @Override
    protected void onApplyTheme(final Theme theme) {
        this.validity.applyTheme(theme, null);
        this.button = theme.paintOr(STYLE_BUTTON, theme.paintOr(Button.STYLE_NORMAL, Paint.NONE));
        this.buttonHovered = theme.paintOr(STYLE_BUTTON_HOVERED, theme.paintOr(Button.STYLE_HOVERED, this.button));
        this.buttonDisabled = theme.paintOr(STYLE_BUTTON_DISABLED, theme.paintOr(Button.STYLE_DISABLED, this.button));
        final TextStyle text = theme.textStyleOr(Button.STYLE_TEXT, TextStyle.DEFAULT);
        this.arrowColor = text.color();
        this.arrowDisabledColor = theme.textStyleOr(Button.STYLE_TEXT_DISABLED, text).color();
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        final Numbers current = Numbers.current();
        if (!current.locale().equals(this.numbers.locale())) {
            this.numbers = current;
            this.updatePattern();
            this.show();
        }
        final Size field = context.measure(this.field, Math.max(0f, availableWidth - BUTTON_WIDTH), availableHeight);
        return new Size(DEFAULT_WIDTH, field.height());
    }

    @Override
    protected void arrange(final ArrangeContext context, final Rect bounds) {
        final float fieldWidth = Math.max(0f, bounds.width() - BUTTON_WIDTH);
        context.arrange(this.field, new Rect(bounds.x(), bounds.y(), fieldWidth, bounds.height()));
        this.buttons = new Rect(bounds.x() + fieldWidth, bounds.y(), bounds.width() - fieldWidth, bounds.height());
        final float half = (float) Math.floor(this.buttons.height() * 0.5f);
        this.upButton = new Rect(this.buttons.x(), this.buttons.y(), this.buttons.width(), half);
        this.downButton = new Rect(this.buttons.x(), this.buttons.y() + half, this.buttons.width(),
                this.buttons.height() - half);
    }

    @Override
    protected void onEnabledChanged() {
        this.field.setEnabled(this.isEnabled());
    }

    @Override
    protected float baseline() {
        // The field sits at the spinner's top
        return this.nodeOf(this.field).baseline();
    }

    @Override
    protected void paint(final Painter painter, final Rect bounds) {
        this.paintButton(painter, this.upButton, this.value < this.max, true);
        this.paintButton(painter, this.downButton, this.value > this.min, false);
    }

    @Override
    protected void paintOverChildren(final Painter painter, final Rect bounds) {
        // Around the field and the buttons both, so over the field that covers most of the bounds
        if (this.isEnabled()) {
            this.validity.paintOutline(painter, bounds);
        }
    }

    private void paintButton(final Painter painter, final Rect rect, final boolean usable, final boolean upward) {
        final boolean enabled = usable && this.isEnabled();
        final Paint face;
        if (!enabled) {
            face = this.buttonDisabled;
        } else {
            face = this.isHovered() && rect.contains(rect.x(), this.pointerY) ? this.buttonHovered : this.button;
        }
        face.paint(painter, rect);
        final float x = rect.x() + (float) Math.floor((rect.width() - Rings.ARROW_LONG) * 0.5f);
        final float y = rect.y() + (float) Math.floor((rect.height() - Rings.ARROW_SHORT) * 0.5f);
        final int color = enabled ? this.arrowColor : this.arrowDisabledColor;
        if (upward) {
            Rings.up(painter, color, x, y);
        } else {
            Rings.down(painter, color, x, y);
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
        if (event.button() != MouseButton.LEFT || !this.buttons.contains(event.x(), event.y())) {
            return;
        }
        if (event.y() < this.buttons.y() + (float) Math.floor(this.buttons.height() * 0.5f)) {
            this.stepBy(1);
        } else {
            this.stepBy(-1);
        }
        event.consume();
    }

    @Override
    protected void onScroll(final ScrollEvent event) {
        if (!this.isEnabled() || event.scrollY() == 0f) {
            return;
        }
        if (event.scrollY() > 0f) {
            this.stepBy(1);
        } else {
            this.stepBy(-1);
        }
        event.consume();
    }

    @Override
    protected void onKey(final KeyEvent event) {
        if (event.target() != this.field || !event.action().isDown() || !this.isEnabled() || event.modifiers().control()
                || event.modifiers().alt()) {
            return;
        }
        final int steps = switch (event.key()) {
            case UP -> 1;
            case DOWN -> -1;
            case PAGE_UP -> 10;
            case PAGE_DOWN -> -10;
            default -> 0;
        };
        if (steps != 0) {
            this.stepBy(steps);
            event.consume();
        }
    }

    @Override
    protected Object snapshotState() {
        return new Spinner.State(this.value, this.min, this.max, this.step, this.decimals);
    }
}
