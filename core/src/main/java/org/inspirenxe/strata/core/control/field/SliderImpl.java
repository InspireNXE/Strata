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
import org.inspirenxe.strata.api.control.field.Slider;
import org.inspirenxe.strata.api.event.input.DragEvent;
import org.inspirenxe.strata.api.event.input.KeyEvent;
import org.inspirenxe.strata.api.event.input.PointerButtonEvent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.core.control.Caption;
import org.inspirenxe.strata.core.node.BuiltInComponent;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;
import org.inspirenxe.strata.core.text.TextLine;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Set;
import java.util.function.DoubleFunction;

/**
 * The core {@link Slider}. The track covers the whole bounds; the thumb is {@value #THUMB_WIDTH} units along the
 * track and spans the slider across it. A pointer position maps to the value whose thumb would be centered on it,
 * along the track's axis only. A vertical slider swaps the desired width and height and counts from the bottom.
 *
 * <p>Snapping: a value is clamped to the range, then moved to the nearest {@code min + k * step}, worked out in
 * decimal from the two numbers as written so that steps such as 0.1 land on the decimals they name; if the maximum is
 * nearer to the requested value than that grid point, the maximum is used instead, so the maximum is always
 * reachable. The label is clipped to the bounds, and its text is measured when it changes, during painting, through
 * the environment's text metrics. The thumb's rectangle is kept from frame to frame and made again only when it
 * moves.</p>
 *
 * <p>A press, drag or key that would change the value fires {@link Slider#CHANGE}, and the final value is set as
 * {@link #setValue} sets it. A press, drag or key that leaves the value as it is fires nothing.</p>
 */
public final class SliderImpl extends BuiltInComponent<Slider> implements Slider {
    /**
     * The thumb's length along the track: its width on a horizontal slider, its height on a vertical one.
     */
    public static final float THUMB_WIDTH = 8f;
    /**
     * The desired length along the track: the width of a horizontal slider, the height of a vertical one.
     */
    public static final float DEFAULT_WIDTH = 150f;
    /**
     * The desired thickness across the track: the height of a horizontal slider, the width of a vertical one.
     */
    public static final float DEFAULT_HEIGHT = 20f;
    /**
     * How many key steps Page Up and Page Down move.
     */
    public static final int PAGE_STEPS = 10;
    private static final Set<String> REQUIRED = Set.of(STYLE_TRACK, STYLE_THUMB, STYLE_THUMB_HOVERED);

    private final Caption caption;
    private double value;
    private double min;
    private double max;
    private double step;
    private DoubleFunction<String> label;
    private Orientation orientation;
    private String labelText;

    private Paint track;
    private Paint trackFocused;
    private Paint trackDisabled;
    private Paint thumb;
    private Paint thumbHovered;
    private Paint thumbDisabled;
    private TextStyle textStyle;
    private TextStyle disabledTextStyle;

    // Rebuilt during painting when the label text or theme changes; null when stale
    private TextLine labelSpan;
    private TextLine disabledLabelSpan;
    private float labelHeight;
    private Insets padding = Insets.NONE;
    private float keyTravel;
    // Drawn last frame, made again only when the thumb moves
    private Rect thumbRect;

    /**
     * Creates a slider over 0 to 1, at 0, with no step.
     *
     * @param ui the factory that owns the slider
     */
    @SuppressWarnings("this-escape")
    public SliderImpl(final ComponentFactoryImpl ui) {
        super(ui);
        this.caption = new Caption();
        this.min = 0d;
        this.max = 1d;
        this.labelText = "";
        this.orientation = Orientation.HORIZONTAL;
        this.track = Paint.NONE;
        this.trackFocused = Paint.NONE;
        this.trackDisabled = Paint.NONE;
        this.thumb = Paint.NONE;
        this.thumbHovered = Paint.NONE;
        this.thumbDisabled = Paint.NONE;
        this.textStyle = TextStyle.DEFAULT;
        this.disabledTextStyle = TextStyle.DEFAULT;
        this.setCursor(CursorKind.HAND);
    }

    // ------------------------------------------------------------------
    // Values
    // ------------------------------------------------------------------

    @Override
    public double value() {
        return this.value;
    }

    @Override
    public Slider setValue(final double value) {
        if (Double.isNaN(value)) {
            throw new IllegalArgumentException("Value must not be NaN");
        }
        this.apply(this.normalize(value));
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
    public Slider setRange(final double min, final double max) {
        if (!Double.isFinite(min) || !Double.isFinite(max) || !(max > min)) {
            throw new IllegalArgumentException("Range must be finite with max above min: " + min + " to " + max);
        }
        this.min = min;
        this.max = max;
        this.apply(this.normalize(this.value));
        return this;
    }

    @Override
    public double step() {
        return this.step;
    }

    @Override
    public Slider setStep(final double step) {
        if (!(step >= 0d) || Double.isInfinite(step)) {
            throw new IllegalArgumentException("Step must be finite and not negative: " + step);
        }
        this.step = step;
        this.apply(this.normalize(this.value));
        return this;
    }

    @Override
    public double keyStep() {
        if (this.step > 0d) {
            return this.step;
        }
        final Rect bounds = this.bounds();
        final float length = this.orientation == Orientation.HORIZONTAL ? bounds.width() : bounds.height();
        if (this.keyTravel > 0f && length > THUMB_WIDTH) {
            return (this.max - this.min) * this.keyTravel / (length - THUMB_WIDTH);
        }
        return (this.max - this.min) / 100d;
    }

    @Override
    public Orientation orientation() {
        return this.orientation;
    }

    @Override
    public Slider setOrientation(final Orientation orientation) {
        this.orientation = Objects.requireNonNull(orientation, "orientation");
        this.invalidateLayout();
        return this;
    }

    @Override
    public Slider setLabel(final DoubleFunction<String> label) {
        this.label = label;
        this.updateLabel();
        return this;
    }

    @Override
    public String labelText() {
        return this.labelText;
    }

    private double normalize(final double requested) {
        final double clamped = Math.clamp(requested, this.min, this.max);
        if (this.step <= 0d) {
            return clamped;
        }
        // In decimal, so a step of 0.1 snaps to 0.3 and not to 0.30000000000000004
        final long steps = Math.round((clamped - this.min) / this.step);
        final double grid = BigDecimal.valueOf(this.min)
                .add(BigDecimal.valueOf(this.step).multiply(BigDecimal.valueOf(steps)))
                .doubleValue();
        final double snapped = Math.min(grid, this.max);
        return this.max - clamped < Math.abs(clamped - snapped) ? this.max : snapped;
    }

    private void apply(final double next) {
        if (next == this.value) {
            return;
        }
        final double previous = this.value;
        this.value = next;
        this.updateLabel();
        this.report(CHANGED, next, previous);
    }

    private void updateLabel() {
        final String text = this.label == null ? "" : Objects.requireNonNullElse(this.label.apply(this.value), "");
        if (!text.equals(this.labelText)) {
            this.labelText = text;
            this.labelSpan = null;
        }
    }

    private double fraction() {
        return (this.value - this.min) / (this.max - this.min);
    }

    private void setFromPointer(final float x, final float y) {
        final Rect bounds = this.bounds();
        final double fraction;
        if (this.orientation == Orientation.HORIZONTAL) {
            final float travel = bounds.width() - THUMB_WIDTH;
            fraction = travel > 0f ? ((double) x - bounds.x() - THUMB_WIDTH * 0.5d) / travel : 0d;
        } else {
            final float travel = bounds.height() - THUMB_WIDTH;
            fraction = travel > 0f ? ((double) bounds.bottom() - y - THUMB_WIDTH * 0.5d) / travel : 0d;
        }
        this.change(this.min + fraction * (this.max - this.min));
    }

    /**
     * Applies a value the user asked for, clamped and snapped. A value that differs from the current one fires
     * {@link Slider#CHANGE} first, and the final value goes through {@code setValue}.
     */
    private void change(final double requested) {
        final double normalized = this.normalize(requested);
        if (normalized != this.value) {
            this.request(CHANGE, normalized, value -> {
                // A handler that rewrote the value to null or NaN leaves it as it is
                if (value != null && !value.isNaN()) {
                    this.setValue(value);
                }
            });
        }
    }

    // ------------------------------------------------------------------
    // Theme, layout and drawing
    // ------------------------------------------------------------------

    @Override
    protected Set<String> requiredStyleKeys() {
        return REQUIRED;
    }

    @Override
    protected void onApplyTheme(final Theme theme) {
        this.track = theme.paintOr(STYLE_TRACK, Paint.NONE);
        this.trackFocused = theme.paintOr(STYLE_TRACK_FOCUSED, this.track);
        this.trackDisabled = theme.paintOr(STYLE_TRACK_DISABLED, this.track);
        this.thumb = theme.paintOr(STYLE_THUMB, Paint.NONE);
        this.thumbHovered = theme.paintOr(STYLE_THUMB_HOVERED, this.thumb);
        this.thumbDisabled = theme.paintOr(STYLE_THUMB_DISABLED, this.thumb);
        this.textStyle = theme.textStyleOr(STYLE_TEXT, TextStyle.DEFAULT);
        this.disabledTextStyle = theme.textStyleOr(STYLE_TEXT_DISABLED, this.textStyle);
        this.padding = theme.insetsOr(STYLE_PADDING, Insets.NONE);
        this.keyTravel = Math.max(0f, theme.metricOr(STYLE_KEY_TRAVEL, 0f));
        this.caption.applyTheme(theme, STYLE_PADDING, STYLE_TEXT_SCROLL, STYLE_TEXT_SCROLL_MINIMUM);
        this.labelSpan = null;
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        return this.orientation == Orientation.HORIZONTAL
                ? new Size(DEFAULT_WIDTH, DEFAULT_HEIGHT)
                : new Size(DEFAULT_HEIGHT, DEFAULT_WIDTH);
    }

    @Override
    protected void paint(final Painter painter, final Rect bounds) {
        final boolean enabled = this.isEnabled();
        final Paint trackFace = !enabled ? this.trackDisabled : this.isFocused() ? this.trackFocused : this.track;
        trackFace.paint(painter, bounds);

        final Paint thumbFace;
        if (!enabled) {
            thumbFace = this.thumbDisabled;
        } else if (this.isHovered() || this.isFocused() || this.isPressed()) {
            thumbFace = this.thumbHovered;
        } else {
            thumbFace = this.thumb;
        }
        if (this.orientation == Orientation.HORIZONTAL) {
            final float travel = Math.max(0f, bounds.width() - THUMB_WIDTH);
            this.thumbRect = Rects.reuse(this.thumbRect, bounds.x() + (float) (this.fraction() * travel), bounds.y(),
                    Math.min(THUMB_WIDTH, bounds.width()), bounds.height());
        } else {
            final float length = Math.min(THUMB_WIDTH, bounds.height());
            final float travel = Math.max(0f, bounds.height() - THUMB_WIDTH);
            this.thumbRect = Rects.reuse(this.thumbRect, bounds.x(),
                    bounds.bottom() - length - (float) (this.fraction() * travel), bounds.width(), length);
        }
        thumbFace.paint(painter, this.thumbRect);

        if (!this.labelText.isEmpty()) {
            if (this.labelSpan == null) {
                final TextMetrics metrics = this.environment().text();
                this.labelSpan = TextLine.of(Text.of(this.labelText), this.textStyle, metrics);
                this.disabledLabelSpan = TextLine.of(Text.of(this.labelText), this.disabledTextStyle, metrics);
                this.labelHeight = metrics.lineHeight(this.textStyle);
            }
            final Insets padding = this.padding;
            this.caption.draw(painter, enabled ? this.labelSpan : this.disabledLabelSpan, bounds, padding,
                    bounds.y() + padding.top() + (bounds.height() - padding.vertical() - this.labelHeight) * 0.5f,
                    this.environment().millis());
        }
    }

    // ------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------

    @Override
    protected void onPointerPress(final PointerButtonEvent event) {
        if (event.button() == MouseButton.LEFT && this.isEnabled()) {
            this.setFromPointer(event.x(), event.y());
            event.consume();
        }
    }

    @Override
    protected void onDragStart(final DragEvent event) {
        if (event.button() == MouseButton.LEFT) {
            event.consume();
        }
    }

    @Override
    protected void onDrag(final DragEvent event) {
        if (event.button() == MouseButton.LEFT && this.isEnabled()) {
            this.setFromPointer(event.x(), event.y());
            event.consume();
        }
    }

    @Override
    protected void onDragEnd(final DragEvent event) {
        if (event.button() == MouseButton.LEFT) {
            event.consume();
        }
    }

    @Override
    protected void onKey(final KeyEvent event) {
        if (!event.action().isDown() || !this.isEnabled()) {
            return;
        }
        final double keyStep = this.keyStep();
        final double next = switch (event.key()) {
            case LEFT, DOWN -> this.value - keyStep;
            case RIGHT, UP -> this.value + keyStep;
            case PAGE_DOWN -> this.value - keyStep * PAGE_STEPS;
            case PAGE_UP -> this.value + keyStep * PAGE_STEPS;
            case HOME -> this.min;
            case END -> this.max;
            default -> Double.NaN;
        };
        if (!Double.isNaN(next)) {
            this.change(next);
            event.consume();
        }
    }

    @Override
    protected Object snapshotState() {
        return new Slider.State(this.value, this.min, this.max, this.step);
    }
}
