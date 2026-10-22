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

import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;

import java.util.List;

/**
 * One line of resolved text, measured once so it can be drawn without allocating. Controls make one per caption
 * during measure and draw it during paint. Line breaks are not honored; the spans are drawn one after another. A
 * line that holds inline elements is tall enough for them, and each span is centered on it as
 * {@link InlineMetrics#draw} centers them.
 */
public final class TextLine {
    /**
     * A line with no text and no height.
     */
    public static final TextLine EMPTY = new TextLine(List.of(), new float[0], new float[0], 0f, 0f, Float.NaN);

    private final List<TextSpan> spans;
    private final float[] offsets;
    // How far below the line's top each span is drawn
    private final float[] tops;
    private final float width;
    private final float height;
    private final float baseline;

    private TextLine(final List<TextSpan> spans, final float[] offsets, final float[] tops, final float width,
                     final float height, final float baseline) {
        this.spans = spans;
        this.offsets = offsets;
        this.tops = tops;
        this.width = width;
        this.height = height;
        this.baseline = baseline;
    }

    /**
     * Resolves text and measures it.
     *
     * @param text the text
     * @param base the style plain parts are drawn in
     * @param metrics the engine's text metrics
     * @return the line; its height is at least the base style's line height
     */
    public static TextLine of(final Text text, final TextStyle base, final TextMetrics metrics) {
        return TextLine.of(text.resolve(base), base, metrics);
    }

    /**
     * Measures spans that are already resolved.
     *
     * @param spans the spans
     * @param base the style whose line height the line is at least as tall as
     * @param metrics the engine's text metrics
     * @return the line
     */
    public static TextLine of(final List<TextSpan> spans, final TextStyle base, final TextMetrics metrics) {
        final InlineMetrics inline = InlineMetrics.of(metrics);
        final float textHeight = inline.textHeight(spans, base);
        final float height = inline.lineHeight(spans, base);
        final float[] offsets = new float[spans.size()];
        final float[] tops = new float[spans.size()];
        float x = 0f;
        for (int i = 0; i < spans.size(); i++) {
            final TextSpan span = spans.get(i);
            offsets[i] = x;
            tops[i] = (height - (span.isInline() ? inline.height(span) : textHeight)) * 0.5f;
            x += inline.width(span);
        }
        final float baseline = (height - textHeight) * 0.5f + inline.ascent(base);
        return new TextLine(List.copyOf(spans), offsets, tops, x, height, baseline);
    }

    /**
     * Returns the spans.
     *
     * @return the spans, in drawing order
     */
    public List<TextSpan> spans() {
        return this.spans;
    }

    /**
     * Returns the width.
     *
     * @return the width in UI units
     */
    public float width() {
        return this.width;
    }

    /**
     * Returns the height.
     *
     * @return the tallest line height of the base style and the spans' styles, or the tallest inline element if that
     *         is taller
     */
    public float height() {
        return this.height;
    }

    /**
     * Returns where the characters' baseline lies: their top, centered in the line as drawing centers them, plus the
     * base style's ascent.
     *
     * @return the baseline in UI units below the line's top; NaN for {@link #EMPTY}
     */
    public float baseline() {
        return this.baseline;
    }

    /**
     * Returns whether the line has no characters.
     *
     * @return whether there are no spans
     */
    public boolean isEmpty() {
        return this.spans.isEmpty();
    }

    /**
     * Returns this line cut short with {@link RichText#ELLIPSIS} so it fits a width, or this line if it already fits.
     *
     * @param maxWidth the widest the line may be
     * @param base the style for the line height
     * @param metrics the engine's text metrics
     * @return the line
     */
    public TextLine ellipsize(final float maxWidth, final TextStyle base, final TextMetrics metrics) {
        if (this.width <= maxWidth) {
            return this;
        }
        return TextLine.of(RichText.ellipsize(this.spans, maxWidth, metrics), base, metrics);
    }

    /**
     * Draws the line with its left edge at {@code x} and its top at {@code y}.
     *
     * @param painter the painter
     * @param x the left edge
     * @param y the top edge
     */
    public void draw(final Painter painter, final float x, final float y) {
        for (int i = 0; i < this.offsets.length; i++) {
            painter.text(this.spans.get(i), x + this.offsets[i], y + this.tops[i]);
        }
    }
}
