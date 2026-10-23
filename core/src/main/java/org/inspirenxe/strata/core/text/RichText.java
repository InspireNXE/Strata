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

import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextSpan;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapping, measuring and shortening text made of styled spans. A line is a list of spans drawn one after another;
 * its width is the sum of their widths.
 */
public final class RichText {
    /**
     * What a shortened line ends with.
     */
    public static final String ELLIPSIS = "...";

    private RichText() {
    }

    /**
     * Splits spans into lines no wider than a width, by the rule {@link LineBreaks} describes, as {@link TextWrap#wrap}
     * splits text in one style: newlines always break, a row breaks after the last whitespace that fits, the
     * whitespace where it breaks is dropped, and a word wider than a whole line breaks between grapheme clusters. Each part
     * of a line is measured in its own span's style, and an inline element is one character that is not whitespace. A
     * single span wraps exactly as {@link TextWrap} wraps its text.
     *
     * @param spans the spans, in reading order
     * @param maxWidth the widest a line may be, in UI units; {@link Float#POSITIVE_INFINITY} splits only at newlines
     * @param metrics the engine's text metrics
     * @return the lines in order, at least one; empty spans are dropped from lines
     */
    public static List<List<TextSpan>> wrap(final List<TextSpan> spans, final float maxWidth, final TextMetrics metrics) {
        final List<List<TextSpan>> lines = new ArrayList<>();
        if (spans.size() == 1 && !spans.get(0).isInline()) {
            final TextSpan only = spans.get(0);
            for (final String line : TextWrap.wrap(only.text(), only.style(), maxWidth, metrics)) {
                lines.add(line.isEmpty() ? List.of() : List.of(only.withText(line)));
            }
            return lines;
        }

        // Join the text, remembering which span each char came from
        final StringBuilder joined = new StringBuilder();
        final List<Integer> owner = new ArrayList<>();
        for (int i = 0; i < spans.size(); i++) {
            final String text = spans.get(i).text();
            joined.append(text);
            for (int c = 0; c < text.length(); c++) {
                owner.add(i);
            }
        }
        final String text = joined.toString();
        final int[] owners = owner.stream().mapToInt(Integer::intValue).toArray();
        final LineBreaks.Measure measure = (start, end) -> width(spans, owners, text, start, end, metrics);
        final LineBreaks.Characters characters = Graphemes.of(text);

        int paragraphStart = 0;
        while (true) {
            final int newline = text.indexOf('\n', paragraphStart);
            final int paragraphEnd = newline < 0 ? text.length() : newline;
            int start = paragraphStart;
            do {
                final long row = LineBreaks.row(text, start, paragraphEnd, maxWidth, measure, characters);
                lines.add(slice(spans, owners, text, start, LineBreaks.drawnEnd(row)));
                start = LineBreaks.end(row);
            } while (start < paragraphEnd);
            if (newline < 0) {
                break;
            }
            paragraphStart = newline + 1;
        }
        return lines;
    }

    /**
     * Returns the width of a line.
     *
     * @param line the spans of the line
     * @param metrics the engine's text metrics
     * @return the sum of the spans' widths
     */
    public static float width(final List<TextSpan> line, final TextMetrics metrics) {
        float width = 0f;
        for (final TextSpan span : line) {
            width += metrics.width(span);
        }
        return width;
    }

    /**
     * Shortens a line to fit a width, ending it in {@link #ELLIPSIS} in the style of the last character kept. The
     * cut falls between grapheme clusters. A line that already fits comes back unchanged.
     *
     * @param line the spans of the line
     * @param maxWidth the width to fit, in UI units
     * @param metrics the engine's text metrics
     * @return the shortened line; empty if not even the dots fit
     */
    public static List<TextSpan> ellipsize(final List<TextSpan> line, final float maxWidth, final TextMetrics metrics) {
        if (width(line, metrics) <= maxWidth || line.isEmpty()) {
            return line;
        }
        final List<TextSpan> kept = new ArrayList<>();
        float used = 0f;
        for (final TextSpan span : line) {
            final float dots = metrics.width(new TextSpan(ELLIPSIS, span.style()));
            final float spanWidth = metrics.width(span);
            if (used + spanWidth + dots <= maxWidth) {
                kept.add(span);
                used += spanWidth;
                continue;
            }
            if (used + dots <= maxWidth) {
                kept.add(shortened(span, maxWidth - used - dots, metrics));
                return kept;
            }
            break;
        }
        // The dots do not fit in this span's style, so end on an earlier span instead
        while (!kept.isEmpty()) {
            final TextSpan last = kept.remove(kept.size() - 1);
            final float before = width(kept, metrics);
            final float dots = metrics.width(new TextSpan(ELLIPSIS, last.style()));
            if (before + dots <= maxWidth) {
                kept.add(shortened(last, maxWidth - before - dots, metrics));
                return kept;
            }
        }
        return List.of();
    }

    /**
     * Returns as much of a span as fits a width, followed by the ellipsis in its style. An inline element is never
     * cut, so it gives way to the ellipsis alone.
     */
    private static TextSpan shortened(final TextSpan span, final float width, final TextMetrics metrics) {
        if (span.isInline()) {
            return new TextSpan(ELLIPSIS, span.style());
        }
        final int fit = TextWrap.fittingPrefix(span.text(), span.style(), width, metrics);
        return new TextSpan(span.text().substring(0, fit) + ELLIPSIS, span.style());
    }

    private static float width(final List<TextSpan> spans, final int[] owners, final String text, final int start,
                               final int end, final TextMetrics metrics) {
        float width = 0f;
        int i = start;
        while (i < end) {
            final int span = owners[i];
            int j = i;
            while (j < end && owners[j] == span) {
                j++;
            }
            width += metrics.width(spans.get(span).withText(text.substring(i, j)));
            i = j;
        }
        return width;
    }

    private static List<TextSpan> slice(final List<TextSpan> spans, final int[] owners, final String text,
                                        final int start, final int end) {
        final List<TextSpan> line = new ArrayList<>();
        int i = start;
        while (i < end) {
            final int span = owners[i];
            int j = i;
            while (j < end && owners[j] == span) {
                j++;
            }
            line.add(spans.get(span).withText(text.substring(i, j)));
            i = j;
        }
        return line;
    }
}
