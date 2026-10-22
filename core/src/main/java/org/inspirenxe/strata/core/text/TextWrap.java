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
import org.inspirenxe.strata.api.text.TextStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * Word wrapping of text in one style with an engine's text metrics, by the rule {@link LineBreaks} describes. Labels
 * use it, through {@link RichText}, to measure and paint wrapped text.
 */
public final class TextWrap {

    private TextWrap() {
    }

    /**
     * Splits text into lines no wider than a width. Newlines always break. Within a line, each row takes as much as
     * fits and breaks after the last whitespace that fits, or between grapheme clusters when a word alone is wider than
     * the width. The whitespace where a row breaks is dropped; whitespace anywhere else, at the start of a line included,
     * is kept.
     *
     * <p>A single cluster wider than the width still gets a line of its own, so the result can be wider than
     * {@code maxWidth} when the width is smaller than one character.</p>
     *
     * @param text the text, with {@code \n} as the only line break
     * @param style the style to measure with
     * @param maxWidth the widest a line may be, in UI units; {@link Float#POSITIVE_INFINITY} splits only at newlines
     * @param metrics the engine's text metrics
     * @return the lines in order, at least one; an empty paragraph gives an empty line
     */
    public static List<String> wrap(final String text, final TextStyle style, final float maxWidth, final TextMetrics metrics) {
        final List<String> lines = new ArrayList<>();
        final LineBreaks.Measure measure = (start, end) -> width(text.substring(start, end), style, metrics);
        final LineBreaks.Characters characters = Graphemes.of(text);
        int lineStart = 0;
        while (true) {
            final int newline = text.indexOf('\n', lineStart);
            final int lineEnd = newline < 0 ? text.length() : newline;
            int start = lineStart;
            do {
                final long row = LineBreaks.row(text, start, lineEnd, maxWidth, measure, characters);
                lines.add(text.substring(start, LineBreaks.drawnEnd(row)));
                start = LineBreaks.end(row);
            } while (start < lineEnd);
            if (newline < 0) {
                return lines;
            }
            lineStart = newline + 1;
        }
    }

    /**
     * Returns how much of the start of a string fits in a width, in whole grapheme clusters, so a flag or a letter
     * with its accents is kept or left out whole. It measures O(log n) prefixes, by binary search.
     *
     * @param text the text
     * @param style the style to measure with
     * @param maxWidth the width to fit, in UI units
     * @param metrics the engine's text metrics
     * @return the length in {@code char}s of the longest fitting prefix, never splitting a cluster; 0 if not even the
     *     first cluster fits
     */
    public static int fittingPrefix(final String text, final TextStyle style, final float maxWidth, final TextMetrics metrics) {
        if (text.isEmpty()) {
            return 0;
        }
        final LineBreaks.Characters clusters = Graphemes.of(text);
        // The longest prefix that fits lies in [low, high]
        int low = clusters.next(0);
        if (width(text.substring(0, low), style, metrics) > maxWidth) {
            return 0;
        }
        int high = text.length();
        while (low < high) {
            int mid = low + (high - low + 1) / 2;
            mid = Graphemes.snap(clusters, mid);
            if (mid <= low) {
                mid = clusters.next(low);
            }
            if (width(text.substring(0, mid), style, metrics) <= maxWidth) {
                low = mid;
            } else {
                high = clusters.previous(mid);
            }
        }
        return low;
    }

    private static float width(final String text, final TextStyle style, final TextMetrics metrics) {
        return metrics.width(new TextSpan(text, style));
    }
}
