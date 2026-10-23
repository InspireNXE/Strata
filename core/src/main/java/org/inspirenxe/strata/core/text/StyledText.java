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
package org.inspirenxe.strata.core.text;

import org.inspirenxe.strata.api.text.StyleRun;
import org.inspirenxe.strata.api.text.TextHighlighter;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * A text field's text split into styled segments: the runs a {@link TextHighlighter} returns, with the gaps between
 * them in the field's base style. Text fields measure and draw through it so every part of the text is measured in
 * the style it is drawn in.
 *
 * <p>{@link #update} runs the highlighter only when the text, the base style or the highlighter changed, and checks
 * its runs: a run out of order, overlapping an earlier one, empty, or past the end of the text is dropped and its
 * range drawn in the base style. The segments then cover the whole text in order. The width of each whole segment is
 * kept once measured, until the segments change or other metrics measure, so measuring a range allocates a span only
 * for the segments it cuts, at most two; callers cache what they paint.</p>
 */
public final class StyledText {
    private String text;
    private TextStyle base;
    private TextHighlighter highlighter;
    private boolean useRuns;

    private int count;
    private int[] starts;
    private int[] ends;
    private TextStyle[] styles;
    // The width of each whole segment once measured with widthsFor, else NaN
    private float[] widths;
    private TextMetrics widthsFor;

    /**
     * Creates empty text with no highlighter.
     */
    public StyledText() {
        this.text = "";
        this.base = TextStyle.DEFAULT;
        this.highlighter = TextHighlighter.NONE;
        this.starts = new int[4];
        this.ends = new int[4];
        this.styles = new TextStyle[4];
        this.widths = new float[4];
    }

    /**
     * Brings the segments up to date.
     *
     * @param text the text
     * @param base the style for text no run covers
     * @param highlighter the highlighter
     * @param useRuns false to ignore the highlighter, as a disabled field or a password box does
     * @return true if the segments changed
     */
    public boolean update(final String text, final TextStyle base, final TextHighlighter highlighter,
                          final boolean useRuns) {
        if (text.equals(this.text) && base.equals(this.base) && highlighter == this.highlighter
                && useRuns == this.useRuns && this.count > 0) {
            return false;
        }
        this.text = text;
        this.base = base;
        this.highlighter = highlighter;
        this.useRuns = useRuns;
        this.count = 0;
        this.widthsFor = null;
        final List<StyleRun> runs = useRuns && !text.isEmpty()
                ? Objects.requireNonNull(highlighter.highlight(text, base), "highlighter returned null")
                : List.of();
        int at = 0;
        for (final StyleRun run : runs) {
            if (run == null || run.start() < at || run.end() <= run.start() || run.end() > text.length()) {
                continue;
            }
            if (run.start() > at) {
                this.add(at, run.start(), base);
            }
            this.add(run.start(), run.end(), run.style());
            at = run.end();
        }
        if (at < text.length() || this.count == 0) {
            this.add(at, text.length(), base);
        }
        return true;
    }

    private void add(final int start, final int end, final TextStyle style) {
        if (this.count == this.starts.length) {
            final int grown = this.count * 2;
            this.starts = Arrays.copyOf(this.starts, grown);
            this.ends = Arrays.copyOf(this.ends, grown);
            this.styles = Arrays.copyOf(this.styles, grown);
            this.widths = Arrays.copyOf(this.widths, grown);
        }
        this.starts[this.count] = start;
        this.ends[this.count] = end;
        this.styles[this.count] = style;
        this.count++;
    }

    /**
     * Returns the text the segments cover.
     *
     * @return the text
     */
    public String text() {
        return this.text;
    }

    /**
     * Returns the width of a range of the text, each part measured in its own style.
     *
     * @param metrics the engine's text metrics
     * @param from the first index
     * @param to the first index after the range
     * @return the width; 0 for an empty range
     */
    public float width(final TextMetrics metrics, final int from, final int to) {
        if (metrics != this.widthsFor) {
            this.widthsFor = metrics;
            Arrays.fill(this.widths, 0, this.count, Float.NaN);
        }
        float width = 0f;
        for (int i = this.first(from); i < this.count && this.starts[i] < to; i++) {
            final int start = Math.max(from, this.starts[i]);
            final int end = Math.min(to, this.ends[i]);
            if (start >= end) {
                continue;
            }
            if (start == this.starts[i] && end == this.ends[i]) {
                if (Float.isNaN(this.widths[i])) {
                    this.widths[i] = metrics.width(new TextSpan(this.text.substring(start, end), this.styles[i]));
                }
                width += this.widths[i];
            } else {
                width += metrics.width(new TextSpan(this.text.substring(start, end), this.styles[i]));
            }
        }
        return width;
    }

    /**
     * Returns the spans that draw a range of the text, one per segment it crosses.
     *
     * @param from the first index
     * @param to the first index after the range
     * @return the spans in order; empty for an empty range
     */
    public TextSpan[] spans(final int from, final int to) {
        int n = 0;
        final int first = this.first(from);
        for (int i = first; i < this.count && this.starts[i] < to; i++) {
            if (Math.max(from, this.starts[i]) < Math.min(to, this.ends[i])) {
                n++;
            }
        }
        final TextSpan[] spans = new TextSpan[n];
        n = 0;
        for (int i = first; i < this.count && this.starts[i] < to; i++) {
            final int start = Math.max(from, this.starts[i]);
            final int end = Math.min(to, this.ends[i]);
            if (start < end) {
                spans[n++] = new TextSpan(this.text.substring(start, end), this.styles[i]);
            }
        }
        return spans;
    }

    /**
     * Returns where each span starts, measured from the start of the first, for drawing them side by side.
     *
     * @param metrics the engine's text metrics
     * @param spans spans from {@link #spans}
     * @return the offsets, one per span
     */
    public static float[] offsets(final TextMetrics metrics, final TextSpan[] spans) {
        final float[] offsets = new float[spans.length];
        float x = 0f;
        for (int i = 0; i < spans.length; i++) {
            offsets[i] = x;
            x += metrics.width(spans[i]);
        }
        return offsets;
    }

    /**
     * Returns how wide spans drawn at their offsets are together.
     *
     * @param metrics the engine's text metrics
     * @param spans the spans
     * @param offsets where each span starts, from {@link #offsets}
     * @return the width; 0 for no spans
     */
    public static float extent(final TextMetrics metrics, final TextSpan[] spans, final float[] offsets) {
        return spans.length == 0 ? 0f : offsets[spans.length - 1] + metrics.width(spans[spans.length - 1]);
    }

    /**
     * The first segment that could hold {@code index}.
     */
    private int first(final int index) {
        int low = 0;
        int high = this.count - 1;
        while (low < high) {
            final int mid = (low + high + 1) >>> 1;
            if (this.starts[mid] <= index) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        return low;
    }
}
