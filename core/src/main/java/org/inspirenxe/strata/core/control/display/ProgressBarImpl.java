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
package org.inspirenxe.strata.core.control.display;

import org.inspirenxe.strata.core.layout.Flow;
import org.inspirenxe.strata.api.control.display.ProgressBar;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.core.control.Motion;
import org.inspirenxe.strata.core.control.RectCache;
import org.inspirenxe.strata.core.node.BuiltInComponent;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;
import org.inspirenxe.strata.core.text.TextLine;

import java.util.Objects;
import java.util.Set;

/**
 * The core {@link ProgressBar}. The fill covers the value's share of the range, as a share of the bounds' width from
 * the left edge, over the whole height, or on a vertical bar as a share of the height from the bottom edge, over the
 * whole width.
 *
 * <p>The fill eases to a new value over the theme's {@value ProgressBar#STYLE_ANIMATION} metric through a
 * {@link Motion} of the drawn fraction, and jumps when the metric is 0 or the bar is detached; the value itself
 * changes at once.</p>
 *
 * <p>While indeterminate, the segment's left edge steps one unit at a time from a segment's length before the left
 * edge to the right edge, or on a vertical bar its top edge from a segment's length below the bottom to the top, over {@link #SEGMENT_PERIOD} milliseconds of the environment's clock. Every position's
 * rectangle, already cut to the bounds, is worked out once when the bounds change, so painting picks one and
 * allocates nothing.</p>
 */
public final class ProgressBarImpl extends BuiltInComponent<ProgressBar> implements ProgressBar {
    /**
     * The width the bar asks for, in UI units.
     */
    public static final float DEFAULT_WIDTH = 100f;
    /**
     * The space around the bar's text when the theme sets no {@value #STYLE_PADDING}: 2 on every side.
     */
    public static final Insets DEFAULT_PADDING = Insets.of(2f);
    /**
     * How long the indeterminate segment takes to cross the bar, in milliseconds.
     */
    public static final long SEGMENT_PERIOD = 1500L;
    /**
     * The indeterminate segment's length as a share of the bar's width.
     */
    public static final float SEGMENT_SHARE = 0.25f;
    /**
     * The shortest the indeterminate segment gets, in UI units, unless the bar is narrower.
     */
    public static final float MIN_SEGMENT_LENGTH = 8f;
    private static final Rect[] NO_SEGMENTS = new Rect[0];
    private static final Set<String> REQUIRED = Set.of(STYLE_TRACK, STYLE_FILL);

    private double value;
    private double min;
    private double max;
    private Text text;
    private boolean indeterminate;
    private Orientation orientation = Orientation.HORIZONTAL;
    private Paint trackPaint;
    private Paint fillPaint;
    private Paint segmentPaint;
    private TextStyle textStyle;
    private TextLine line;
    private float textWidth;
    private float lineHeight;
    private Insets padding = DEFAULT_PADDING;
    // The indeterminate segment at each whole-unit step, for the bounds they were worked out for
    private Rect[] segments;
    private Rect segmentBounds;
    private boolean segmentsRightToLeft;
    private final RectCache fillRect = new RectCache();
    // The fraction the fill is drawn at, which eases toward the value's
    private float shownFraction;
    private long animationMillis;
    private final Motion fillMotion = new Motion(fraction -> {
        this.shownFraction = fraction;
        this.repaint();
    });

    /**
     * Creates an empty bar with no caption.
     *
     * @param ui the factory that owns the bar
     */
    public ProgressBarImpl(final ComponentFactoryImpl ui) {
        super(ui);
        this.max = 1.0;
        this.text = Text.empty();
        this.trackPaint = Paint.NONE;
        this.fillPaint = Paint.NONE;
        this.segmentPaint = Paint.NONE;
        this.segments = NO_SEGMENTS;
        this.segmentBounds = Rect.ZERO;
        this.textStyle = TextStyle.DEFAULT;
        this.line = TextLine.EMPTY;
    }

    @Override
    public double value() {
        return this.value;
    }

    @Override
    public ProgressBar setValue(final double value) {
        if (Double.isNaN(value)) {
            throw new IllegalArgumentException("value is NaN");
        }
        this.apply(value);
        return this;
    }

    @Override
    public Orientation orientation() {
        return this.orientation;
    }

    @Override
    public ProgressBar setOrientation(final Orientation orientation) {
        this.orientation = Objects.requireNonNull(orientation, "orientation");
        this.segmentBounds = Rect.ZERO;
        this.invalidateLayout();
        return this;
    }

    private boolean vertical() {
        return this.orientation == Orientation.VERTICAL;
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
    public ProgressBar setRange(final double min, final double max) {
        if (!Double.isFinite(min) || !Double.isFinite(max) || max <= min) {
            throw new IllegalArgumentException("Bad range " + min + " to " + max);
        }
        this.min = min;
        this.max = max;
        this.apply(this.value);
        return this;
    }

    /**
     * Clamps a value to the range and reports it if it differs from the current one.
     */
    private void apply(final double value) {
        final double clamped = Math.max(this.min, Math.min(this.max, value));
        final double previous = this.value;
        if (clamped != previous) {
            this.value = clamped;
        }
        final float fraction = (float) ((clamped - this.min) / (this.max - this.min));
        if (fraction != this.fillMotion.destination(this.shownFraction)) {
            this.fillMotion.move(this.motionAnimator(), this.animationMillis, this.shownFraction, fraction);
        }
        if (clamped != previous) {
            this.report(CHANGED, clamped, previous);
        }
    }

    @Override
    public boolean isIndeterminate() {
        return this.indeterminate;
    }

    @Override
    public ProgressBar setIndeterminate(final boolean indeterminate) {
        if (this.indeterminate != indeterminate) {
            this.indeterminate = indeterminate;
            this.repaint();
        }
        return this;
    }

    @Override
    public Text text() {
        return this.text;
    }

    @Override
    public ProgressBar setText(final Text text) {
        this.text = Objects.requireNonNull(text, "text");
        this.invalidateLayout();
        return this;
    }

    @Override
    protected Set<String> requiredStyleKeys() {
        return REQUIRED;
    }

    @Override
    protected void onApplyTheme(final Theme theme) {
        this.trackPaint = theme.paintOr(STYLE_TRACK, Paint.NONE);
        this.fillPaint = theme.paintOr(STYLE_FILL, Paint.NONE);
        this.segmentPaint = theme.paintOr(STYLE_SEGMENT, this.fillPaint);
        this.textStyle = theme.textStyleOr(STYLE_TEXT, TextStyle.DEFAULT);
        this.padding = theme.insetsOr(STYLE_PADDING, DEFAULT_PADDING);
        this.animationMillis = Motion.millis(theme, STYLE_ANIMATION);
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        this.line = TextLine.of(this.text, this.textStyle, context.text());
        this.textWidth = this.line.width();
        this.lineHeight = this.line.height();
        final float thickness = this.lineHeight + this.padding.vertical();
        if (this.vertical()) {
            return new Size(Math.max(thickness, this.textWidth + this.padding.horizontal()), DEFAULT_WIDTH);
        }
        return new Size(Math.max(DEFAULT_WIDTH, this.textWidth + this.padding.horizontal()), thickness);
    }

    @Override
    protected void paint(final Painter painter, final Rect bounds) {
        this.trackPaint.paint(painter, bounds);
        if (this.indeterminate) {
            final Rect[] steps = this.segments(bounds, this.isRightToLeft());
            if (steps.length > 0) {
                final long phase = Math.floorMod(this.environment().millis(), SEGMENT_PERIOD);
                final int index = (int) (phase * steps.length / SEGMENT_PERIOD);
                // The segment moves at the first phase that maps to the next step
                final long next = ((index + 1L) * SEGMENT_PERIOD + steps.length - 1L) / steps.length;
                this.repaintAfter(Math.max(1L, next - phase));
                final Rect segment = steps[index];
                if (segment != null) {
                    this.segmentPaint.paint(painter, segment);
                }
            }
        } else {
            final float fraction = this.shownFraction;
            if (fraction > 0f && this.vertical()) {
                final float height = bounds.height() * fraction;
                this.fillPaint.paint(painter, this.fillRect.at(bounds.x(), bounds.bottom() - height, bounds.width(),
                        height));
            } else if (fraction > 0f) {
                // Right to left the fill grows from the right
                final float width = bounds.width() * fraction;
                this.fillPaint.paint(painter, this.fillRect.at(this.isRightToLeft() ? bounds.right() - width
                        : bounds.x(), bounds.y(), width, bounds.height()));
            }
        }
        if (!this.line.isEmpty()) {
            final Insets padding = this.padding;
            this.line.draw(painter,
                    bounds.x() + padding.left() + (bounds.width() - padding.horizontal() - this.textWidth) * 0.5f,
                    bounds.y() + padding.top() + (bounds.height() - padding.vertical() - this.lineHeight) * 0.5f);
        }
    }

    /**
     * Returns the segment at each whole-unit step across the bounds, cut to them, working them out again only when
     * the bounds or the direction changed. Right to left the segments are mirrored, so they run from the right. A
     * step whose segment would be empty holds null.
     */
    private Rect[] segments(final Rect bounds, final boolean rightToLeft) {
        if (bounds.equals(this.segmentBounds) && rightToLeft == this.segmentsRightToLeft) {
            return this.segments;
        }
        final boolean vertical = this.vertical();
        final float width = vertical ? bounds.height() : bounds.width();
        final float length = Math.min(width, Math.max(MIN_SEGMENT_LENGTH, width * SEGMENT_SHARE));
        final int count = width > 0f ? (int) Math.ceil(width + length) : 0;
        final Rect[] steps = new Rect[count];
        for (int i = 0; i < count; i++) {
            if (vertical) {
                // Measured up from the bottom edge, whatever the direction
                final float low = Math.max(0f, i - length);
                final float high = Math.min(width, i);
                steps[i] = high > low ? new Rect(bounds.x(), bounds.bottom() - high, bounds.width(), high - low) : null;
            } else {
                final float left = Math.max(bounds.x(), bounds.x() - length + i);
                final float right = Math.min(bounds.right(), bounds.x() + i);
                final Rect step = right > left ? new Rect(left, bounds.y(), right - left, bounds.height()) : null;
                steps[i] = step != null && rightToLeft ? Flow.mirror(step, bounds) : step;
            }
        }
        this.segments = steps;
        this.segmentBounds = bounds;
        this.segmentsRightToLeft = rightToLeft;
        return steps;
    }
}
