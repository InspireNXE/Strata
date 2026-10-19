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

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.control.display.ProgressBar;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;

import java.util.Objects;
import java.util.Set;

/**
 * The core {@link ProgressBar}. The fill covers the value's share of the range, as a share of the bounds' width from
 * the left edge, over the whole height.
 *
 * <p>While indeterminate, the segment's left edge steps one unit at a time from a segment's length before the left
 * edge to the right edge, over {@link #SEGMENT_PERIOD} milliseconds of the environment's clock. Every position's
 * rectangle, already cut to the bounds, is worked out once when the bounds change, so painting picks one and
 * allocates nothing.</p>
 */
public final class ProgressBarImpl extends CustomComponent<ProgressBar> implements ProgressBar {
    /**
     * The width the bar asks for, in UI units.
     */
    public static final float DEFAULT_WIDTH = 100f;
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
    private String text;
    private boolean indeterminate;
    private Paint trackPaint;
    private Paint fillPaint;
    private Paint segmentPaint;
    private TextStyle textStyle;
    private TextSpan span;
    private float textWidth;
    private float lineHeight;
    // The indeterminate segment at each whole-unit step, for the bounds they were worked out for
    private Rect[] segments;
    private Rect segmentBounds;

    /**
     * Creates an empty bar with no caption.
     *
     * @param ui the factory that owns the bar
     */
    public ProgressBarImpl(final ComponentFactory ui) {
        super(ui);
        this.max = 1.0;
        this.text = "";
        this.trackPaint = Paint.NONE;
        this.fillPaint = Paint.NONE;
        this.segmentPaint = Paint.NONE;
        this.segments = NO_SEGMENTS;
        this.segmentBounds = Rect.ZERO;
        this.textStyle = TextStyle.DEFAULT;
        this.span = TextSpan.of("");
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
            this.report(CHANGED, clamped, previous);
        }
    }

    @Override
    public boolean isIndeterminate() {
        return this.indeterminate;
    }

    @Override
    public ProgressBar setIndeterminate(final boolean indeterminate) {
        this.indeterminate = indeterminate;
        return this;
    }

    @Override
    public String text() {
        return this.text;
    }

    @Override
    public ProgressBar setText(final String text) {
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
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        final TextMetrics metrics = context.text();
        this.span = new TextSpan(this.text, this.textStyle);
        this.textWidth = metrics.width(this.span);
        this.lineHeight = metrics.lineHeight(this.textStyle);
        return new Size(Math.max(DEFAULT_WIDTH, this.textWidth + 4f), this.lineHeight + 4f);
    }

    @Override
    protected void paint(final Painter painter, final Rect bounds) {
        this.trackPaint.paint(painter, bounds);
        if (this.indeterminate) {
            final Rect[] steps = this.segments(bounds);
            if (steps.length > 0) {
                final long phase = Math.floorMod(this.environment().millis(), SEGMENT_PERIOD);
                final Rect segment = steps[(int) (phase * steps.length / SEGMENT_PERIOD)];
                if (segment != null) {
                    this.segmentPaint.paint(painter, segment);
                }
            }
        } else {
            final float fraction = (float) ((this.value - this.min) / (this.max - this.min));
            if (fraction > 0f) {
                this.fillPaint.paint(painter, new Rect(bounds.x(), bounds.y(), bounds.width() * fraction,
                        bounds.height()));
            }
        }
        if (!this.text.isEmpty()) {
            painter.text(this.span, bounds.x() + (bounds.width() - this.textWidth) * 0.5f,
                    bounds.y() + (bounds.height() - this.lineHeight) * 0.5f);
        }
    }

    /**
     * Returns the segment at each whole-unit step across the bounds, cut to them, working them out again only when
     * the bounds changed. A step whose segment would be empty holds null.
     */
    private Rect[] segments(final Rect bounds) {
        if (bounds.equals(this.segmentBounds)) {
            return this.segments;
        }
        final float width = bounds.width();
        final float length = Math.min(width, Math.max(MIN_SEGMENT_LENGTH, width * SEGMENT_SHARE));
        final int count = width > 0f ? (int) Math.ceil(width + length) : 0;
        final Rect[] steps = new Rect[count];
        for (int i = 0; i < count; i++) {
            final float left = Math.max(bounds.x(), bounds.x() - length + i);
            final float right = Math.min(bounds.right(), bounds.x() + i);
            steps[i] = right > left ? new Rect(left, bounds.y(), right - left, bounds.height()) : null;
        }
        this.segments = steps;
        this.segmentBounds = bounds;
        return steps;
    }
}
