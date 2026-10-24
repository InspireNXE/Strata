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
import org.inspirenxe.strata.api.text.InlineElement;
import org.inspirenxe.strata.api.text.InlineSprite;
import org.inspirenxe.strata.api.text.InlineVisual;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;

import java.util.List;
import java.util.Objects;

/**
 * Text metrics that measure {@linkplain TextSpan#isInline() inline elements} themselves and pass every other span
 * to the engine's metrics, so an engine never sees an element. Hosts wrap the platform's metrics in one, and every
 * {@code MeasureContext} and {@code Environment} hands it out.
 *
 * <p>An element's width is its size: a visual's {@linkplain org.inspirenxe.strata.api.visual.Visual#size() natural
 * size}, a sprite's own size, or for a sprite that {@linkplain InlineSprite#fitsLine() fits the line} the line
 * height of the span's style. Line heights stay the engine's, since they describe a style, not a span; lines that
 * hold elements take their height from {@link #lineHeight(List, TextStyle)}.</p>
 */
public final class InlineMetrics implements TextMetrics {
    private final TextMetrics engine;

    private InlineMetrics(final TextMetrics engine) {
        this.engine = engine;
    }

    /**
     * Wraps an engine's metrics, or returns them as they are when they are already wrapped.
     *
     * @param metrics the metrics
     * @return metrics that measure inline elements
     */
    public static InlineMetrics of(final TextMetrics metrics) {
        Objects.requireNonNull(metrics, "metrics");
        return metrics instanceof final InlineMetrics inline ? inline : new InlineMetrics(metrics);
    }

    @Override
    public float width(final TextSpan span) {
        return span.element() == null ? this.engine.width(span) : this.elementWidth(span.element(), span.style());
    }

    @Override
    public float lineHeight(final TextStyle style) {
        return this.engine.lineHeight(style);
    }

    @Override
    public float ascent(final TextStyle style) {
        return this.engine.ascent(style);
    }

    @Override
    public long version() {
        return this.engine.version();
    }

    /**
     * Returns the height a span takes in a line: its style's line height, or an element's height.
     *
     * @param span the span
     * @return the height in UI units
     */
    public float height(final TextSpan span) {
        final InlineElement element = span.element();
        return switch (element) {
            case null -> this.engine.lineHeight(span.style());
            case final InlineVisual visual -> visual.visual().size().height();
            case final InlineSprite sprite -> sprite.fitsLine() ? this.engine.lineHeight(span.style()) : sprite.height();
        };
    }

    private float elementWidth(final InlineElement element, final TextStyle style) {
        return switch (element) {
            case final InlineVisual visual -> visual.visual().size().width();
            case final InlineSprite sprite -> sprite.fitsLine() ? this.engine.lineHeight(style) : sprite.width();
        };
    }

    /**
     * Returns the height of the characters in a line: the largest line height of the base style and of the styles of
     * its spans that are not elements.
     *
     * @param spans the line
     * @param base the style the line is at least as tall as
     * @return the height in UI units
     */
    public float textHeight(final List<TextSpan> spans, final TextStyle base) {
        float height = this.engine.lineHeight(base);
        for (int i = 0; i < spans.size(); i++) {
            final TextSpan span = spans.get(i);
            if (span.element() == null) {
                height = Math.max(height, this.engine.lineHeight(span.style()));
            }
        }
        return height;
    }

    /**
     * Returns the height of a line: its {@linkplain #textHeight text height}, or the tallest element in it if that
     * is taller.
     *
     * @param spans the line
     * @param base the style the line is at least as tall as
     * @return the height in UI units
     */
    public float lineHeight(final List<TextSpan> spans, final TextStyle base) {
        float height = this.textHeight(spans, base);
        for (int i = 0; i < spans.size(); i++) {
            final TextSpan span = spans.get(i);
            if (span.element() != null) {
                height = Math.max(height, this.height(span));
            }
        }
        return height;
    }

    /**
     * Draws a line of spans in the order the bidirectional algorithm gives, each centered on the line: characters by
     * the line's text height, so spans of different styles keep a shared top as they do without elements, and each
     * element by its own height. Allocates nothing for spans that are not elements.
     *
     * @param painter the painter
     * @param runs the line, in drawing order
     * @param x the left edge
     * @param y the top of the line
     * @param lineHeight the line's height, from {@link #lineHeight(List, TextStyle)} or taller
     * @param textHeight the line's text height, from {@link #textHeight}
     */
    public void draw(final Painter painter, final BidiRuns runs, final float x, final float y,
                     final float lineHeight, final float textHeight) {
        final List<TextSpan> spans = runs.spans();
        final float textTop = y + (lineHeight - textHeight) * 0.5f;
        float left = x;
        for (int i = 0; i < spans.size(); i++) {
            final TextSpan span = spans.get(i);
            runs.draw(painter, i, left, span.element() == null ? textTop : y + (lineHeight - this.height(span)) * 0.5f);
            if (i + 1 < spans.size()) {
                left += this.width(span);
            }
        }
    }

    /**
     * Draws a line of spans that is already in drawing order, as {@link #draw(Painter, BidiRuns, float, float, float,
     * float)} draws one, with every span left to right.
     *
     * @param painter the painter
     * @param spans the line
     * @param x the left edge
     * @param y the top of the line
     * @param lineHeight the line's height, from {@link #lineHeight(List, TextStyle)} or taller
     * @param textHeight the line's text height, from {@link #textHeight}
     */
    public void draw(final Painter painter, final List<TextSpan> spans, final float x, final float y,
                     final float lineHeight, final float textHeight) {
        final float textTop = y + (lineHeight - textHeight) * 0.5f;
        float left = x;
        for (int i = 0; i < spans.size(); i++) {
            final TextSpan span = spans.get(i);
            if (span.element() == null) {
                painter.text(span, left, textTop);
            } else {
                painter.text(span, left, y + (lineHeight - this.height(span)) * 0.5f);
            }
            if (i + 1 < spans.size()) {
                left += this.width(span);
            }
        }
    }
}
