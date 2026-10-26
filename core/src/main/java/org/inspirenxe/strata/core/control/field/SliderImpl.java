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

import org.inspirenxe.strata.core.layout.Flow;
import org.inspirenxe.strata.api.input.Key;
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
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.DoubleFunction;
import org.inspirenxe.strata.api.component.Validation;
import org.inspirenxe.strata.core.control.Validity;
import org.inspirenxe.strata.core.state.ConversionTarget;
import java.util.function.Function;

/**
 * The core {@link Slider}. The track covers the whole bounds; the thumb is {@value #THUMB_WIDTH} units along the
 * track and spans the slider across it. A pointer position maps to the value whose thumb would be centered on it,
 * along the track's axis only. A vertical slider swaps the desired width and height and counts from the bottom. A
 * horizontal slider that runs right to left counts from the right, and Left and Right swap on it.
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
public final class SliderImpl extends BuiltInComponent<Slider> implements Slider, ConversionTarget {
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
    /**
     * The closest ticks may be drawn, in UI units; closer ones are left out.
     */
    public static final float MIN_TICK_GAP = 3f;
    private static final Set<String> REQUIRED = Set.of(STYLE_TRACK, STYLE_THUMB, STYLE_THUMB_HOVERED);

    private final Validity validity;
    private final Caption caption;
    private double value;
    private double min;
    private double max;
    private double step;
    private DoubleFunction<String> label;
    private Orientation orientation;
    private String labelText;
    private boolean adjusting;
    private double committed;
    private double majorTicks;
    private int minorTicks;
    private boolean snapToTicks;
    private Paint tick;
    private Paint minorTick;
    // The tick marks for the bounds and ticks they were placed for, placed again when either changes
    private final List<Rect> tickRects = new ArrayList<>();
    private final List<Rect> minorTickRects = new ArrayList<>();
    private Rect tickBounds;
    private boolean ticksStale = true;
    private boolean ticksRightToLeft;

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
        this.validity = new Validity(this.node(), this::validated);
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
        this.tick = Paint.NONE;
        this.minorTick = Paint.NONE;
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
        this.ticksStale = true;
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
    public boolean isAdjusting() {
        return this.adjusting;
    }

    @Override
    public Slider setTicks(final double major, final int minorPerMajor) {
        if (!(major >= 0d) || Double.isInfinite(major) || minorPerMajor < 0) {
            throw new IllegalArgumentException("Ticks must be finite and not negative: " + major + ", " + minorPerMajor);
        }
        this.majorTicks = major;
        this.minorTicks = minorPerMajor;
        this.ticksStale = true;
        this.apply(this.normalize(this.value));
        this.repaint();
        return this;
    }

    @Override
    public double majorTicks() {
        return this.majorTicks;
    }

    @Override
    public int minorTicks() {
        return this.minorTicks;
    }

    @Override
    public Slider setSnapToTicks(final boolean snap) {
        this.snapToTicks = snap;
        this.apply(this.normalize(this.value));
        return this;
    }

    @Override
    public boolean snapsToTicks() {
        return this.snapToTicks;
    }

    /**
     * Returns the distance between the finest ticks, minor ones included, or 0 without ticks.
     */
    private double tickSpacing() {
        return this.majorTicks > 0d ? this.majorTicks / (this.minorTicks + 1) : 0d;
    }

    /**
     * Returns the grid values snap to: the ticks while snapping to them, else the step; 0 for none.
     */
    private double grid() {
        return this.snapToTicks && this.majorTicks > 0d ? this.tickSpacing() : this.step;
    }

    @Override
    public double keyStep() {
        if (this.grid() > 0d) {
            return this.grid();
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
        this.ticksStale = true;
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
        final double spacing = this.grid();
        if (spacing <= 0d) {
            return clamped;
        }
        // In decimal, so a step of 0.1 snaps to 0.3 and not to 0.30000000000000004
        final long steps = Math.round((clamped - this.min) / spacing);
        final BigDecimal size = this.snapToTicks && this.majorTicks > 0d
                ? BigDecimal.valueOf(this.majorTicks).divide(BigDecimal.valueOf(this.minorTicks + 1L), MathContext.DECIMAL64)
                : BigDecimal.valueOf(this.step);
        final double grid = BigDecimal.valueOf(this.min)
                .add(size.multiply(BigDecimal.valueOf(steps)))
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
        this.validity.check();
        if (!this.adjusting) {
            this.commit();
        }
    }

    /**
     * Reports {@link Slider#COMMITTED} if the value differs from the one last committed.
     */
    private void commit() {
        final double previous = this.committed;
        if (previous != this.value) {
            this.committed = this.value;
            this.report(COMMITTED, this.value, previous);
        }
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
            // Right to left the value rises toward the left
            final double along = this.isRightToLeft() ? (double) bounds.right() - x : (double) x - bounds.x();
            fraction = travel > 0f ? (along - THUMB_WIDTH * 0.5d) / travel : 0d;
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

    // ------------------------------------------------------------------
    // Validatable
    // ------------------------------------------------------------------

    @Override
    public Validation validation() {
        return this.validity.validation();
    }

    @Override
    public Slider setValidation(final Validation validation) {
        this.validity.set(validation);
        return this;
    }

    @Override
    public Slider setValidator(final Function<? super Slider, Validation> validator) {
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
        this.validity.applyTheme(theme, STYLE_INVALID);
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
        this.tick = theme.paintOr(STYLE_TICK, this.thumb);
        this.minorTick = theme.paintOr(STYLE_TICK_MINOR, this.tick);
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
        final Paint trackFace = !enabled ? this.trackDisabled
                : this.validity.face(this.isFocusVisible() ? this.trackFocused : this.track);
        trackFace.paint(painter, bounds);
        if (enabled) {
            this.validity.paintOutline(painter, bounds);
        }

        final Paint thumbFace;
        if (!enabled) {
            thumbFace = this.thumbDisabled;
        } else if (this.isHovered() || this.isFocusVisible() || this.isPressed()) {
            thumbFace = this.thumbHovered;
        } else {
            thumbFace = this.thumb;
        }
        if (this.orientation == Orientation.HORIZONTAL) {
            final float travel = Math.max(0f, bounds.width() - THUMB_WIDTH);
            final float along = (float) (this.fraction() * travel);
            final float width = Math.min(THUMB_WIDTH, bounds.width());
            this.thumbRect = Rects.reuse(this.thumbRect,
                    this.isRightToLeft() ? bounds.right() - width - along : bounds.x() + along, bounds.y(), width,
                    bounds.height());
        } else {
            final float length = Math.min(THUMB_WIDTH, bounds.height());
            final float travel = Math.max(0f, bounds.height() - THUMB_WIDTH);
            this.thumbRect = Rects.reuse(this.thumbRect, bounds.x(),
                    bounds.bottom() - length - (float) (this.fraction() * travel), bounds.width(), length);
        }
        this.paintTicks(painter, bounds);
        thumbFace.paint(painter, this.thumbRect);

        if (!this.labelText.isEmpty()) {
            if (this.labelSpan == null) {
                final TextMetrics metrics = this.environment().text();
                this.labelSpan = TextLine.of(Text.of(this.labelText), this.textStyle, metrics);
                this.disabledLabelSpan = TextLine.of(Text.of(this.labelText), this.disabledTextStyle, metrics);
                this.labelHeight = metrics.lineHeight(this.textStyle);
            }
            final Insets padding = this.padding;
            if (this.caption.draw(painter, enabled ? this.labelSpan : this.disabledLabelSpan, bounds, padding,
                    bounds.y() + padding.top() + (bounds.height() - padding.vertical() - this.labelHeight) * 0.5f,
                    this.environment().millis())) {
                this.repaintNextFrame();
            }
        }
    }

    /**
     * Draws the ticks under the thumb, placing them again only when the bounds or the ticks changed.
     */
    private void paintTicks(final Painter painter, final Rect bounds) {
        if (this.majorTicks <= 0d) {
            return;
        }
        if (this.ticksStale || !bounds.equals(this.tickBounds) || this.isRightToLeft() != this.ticksRightToLeft) {
            this.placeTicks(bounds);
        }
        for (int i = 0; i < this.minorTickRects.size(); i++) {
            this.minorTick.paint(painter, this.minorTickRects.get(i));
        }
        for (int i = 0; i < this.tickRects.size(); i++) {
            this.tick.paint(painter, this.tickRects.get(i));
        }
    }

    private void placeTicks(final Rect bounds) {
        this.tickBounds = bounds;
        this.ticksStale = false;
        // Right to left a horizontal slider's minimum is at its right, so its ticks are mirrored
        final boolean mirrored = this.isRightToLeft();
        this.ticksRightToLeft = mirrored;
        this.tickRects.clear();
        this.minorTickRects.clear();
        final boolean horizontal = this.orientation == Orientation.HORIZONTAL;
        final float travel = Math.max(0f, (horizontal ? bounds.width() : bounds.height()) - THUMB_WIDTH);
        final double range = this.max - this.min;
        final double spacing = this.tickSpacing();
        final boolean minors = this.minorTicks > 0 && spacing / range * travel >= MIN_TICK_GAP;
        if (this.majorTicks / range * travel < MIN_TICK_GAP) {
            return;
        }
        final long count = (long) Math.floor(range / spacing + 1e-9);
        for (long k = 0; k <= count; k++) {
            final boolean major = k % (this.minorTicks + 1) == 0;
            if (!major && !minors) {
                continue;
            }
            final float along = THUMB_WIDTH * 0.5f + (float) (k * spacing / range * travel);
            final float length = major ? 4f : 2f;
            final Rect mark = horizontal
                    ? new Rect(mirrored ? bounds.x() + bounds.right() - (float) Math.floor(bounds.x() + along) - 1f
                    : (float) Math.floor(bounds.x() + along), bounds.bottom() - length, 1f, length)
                    : new Rect(bounds.x(), (float) Math.floor(bounds.bottom() - along), length, 1f);
            (major ? this.tickRects : this.minorTickRects).add(mark);
        }
    }

    // ------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------

    @Override
    protected void onPointerPress(final PointerButtonEvent event) {
        if (event.button() == MouseButton.LEFT && this.isEnabled()) {
            this.adjusting = true;
            this.setFromPointer(event.x(), event.y());
            event.consume();
        }
    }

    @Override
    protected void onPointerRelease(final PointerButtonEvent event) {
        if (event.button() == MouseButton.LEFT) {
            this.endAdjusting();
        }
    }

    /**
     * Ends a press or drag and commits its value when the slider leaves the tree before the button is released.
     */
    @Override
    protected void onDetached() {
        this.endAdjusting();
    }

    private void endAdjusting() {
        if (this.adjusting) {
            this.adjusting = false;
            this.commit();
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
        // Right to left, Left raises a horizontal slider's value
        final Key key = this.orientation == Orientation.HORIZONTAL ? Flow.key(event.key(), this.isRightToLeft())
                : event.key();
        final double next = switch (key) {
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
