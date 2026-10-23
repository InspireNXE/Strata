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

/**
 * Where wrapped text breaks into rows. Labels, tooltips, toasts and info bars wrap by it through {@link TextWrap} and
 * {@link RichText}, and text areas call it directly, so they all break text the same way.
 *
 * <p>A row takes as much of its line as fits the width and breaks after the last whitespace that fits. A word wider
 * than a whole row breaks between characters, and a row always takes at least one character, so a row can be wider
 * than the width when one character is. Whitespace is what {@link Character#isWhitespace(int)} says, so a no-break
 * space never breaks. The whitespace where a row breaks hangs at its end: it belongs to the row, where a text area
 * can put the caret in it, but it is not drawn and does not count toward the row's width. Whitespace anywhere else,
 * at the start of a line included, is kept.</p>
 *
 * <p>Finding a row measures O(log n) ranges no longer than about twice the row: it doubles a probe until the text no
 * longer fits, then searches between the last two probes by halves, over character boundaries. A line much longer
 * than its rows is never measured whole. It assumes a longer range is never narrower than a shorter one that starts at
 * the same place.</p>
 */
public final class LineBreaks {
    // How many chars the first probe past the first character reaches
    private static final int FIRST_PROBE = 32;

    private LineBreaks() {
    }

    /**
     * Finds the row that starts at {@code start}.
     *
     * @param text the text
     * @param start where the row starts
     * @param lineEnd where its line ends, before the line break or at the end of the text
     * @param maxWidth the widest the row may be, in UI units; {@link Float#POSITIVE_INFINITY} gives the whole line
     * @param measure measures ranges of the text
     * @param characters the boundaries between characters, which a word too wide for a row breaks at
     * @return the row, packed: read it with {@link #end(long)} and {@link #drawnEnd(long)}
     */
    public static long row(final CharSequence text, final int start, final int lineEnd, final float maxWidth,
                           final Measure measure, final Characters characters) {
        if (start >= lineEnd || Float.isInfinite(maxWidth)) {
            return pack(lineEnd, lineEnd);
        }
        // The furthest character boundary that fits, but at least one character past the start
        int low = Math.min(lineEnd, characters.next(start));
        int high = lineEnd;
        if (measure.width(start, low) <= maxWidth) {
            int step = FIRST_PROBE;
            while (low < lineEnd) {
                int probe = lineEnd - low <= step ? lineEnd : characters.previous(characters.next(low + step));
                if (probe <= low) {
                    probe = characters.next(low);
                }
                if (measure.width(start, probe) > maxWidth) {
                    high = characters.previous(probe);
                    break;
                }
                low = probe;
                step = step > (1 << 29) ? step : step * 2;
            }
            while (low < high) {
                int mid = low + (high - low + 1) / 2;
                mid = mid >= lineEnd ? lineEnd : characters.previous(characters.next(mid));
                if (mid <= low) {
                    mid = Math.min(high, characters.next(low));
                }
                if (measure.width(start, mid) <= maxWidth) {
                    low = mid;
                } else {
                    high = characters.previous(mid);
                }
            }
        }
        final int fit = low;
        if (fit >= lineEnd) {
            return pack(lineEnd, lineEnd);
        }
        if (Character.isWhitespace(Character.codePointAt(text, fit))) {
            // The whitespace that does not fit hangs at the end of the row
            int end = fit;
            while (end < lineEnd && Character.isWhitespace(Character.codePointAt(text, end))) {
                end += Character.charCount(Character.codePointAt(text, end));
            }
            return pack(end, trimmed(text, start, fit));
        }
        // Break after the last whitespace that fits, or between characters
        for (int i = fit; i > start; i -= Character.charCount(Character.codePointBefore(text, i))) {
            if (Character.isWhitespace(Character.codePointBefore(text, i))) {
                return pack(i, trimmed(text, start, i));
            }
        }
        return pack(fit, fit);
    }

    /**
     * Returns where a row ends, after the whitespace that hangs at its end. The next row starts here.
     *
     * @param row a row from {@link #row}
     * @return the index after the row
     */
    public static int end(final long row) {
        return (int) (row >>> 32);
    }

    /**
     * Returns where the drawn part of a row ends, before the whitespace that hangs at its end.
     *
     * @param row a row from {@link #row}
     * @return the index after the last character drawn; the row's start when it draws nothing
     */
    public static int drawnEnd(final long row) {
        return (int) row;
    }

    private static long pack(final int end, final int drawnEnd) {
        return ((long) end << 32) | (drawnEnd & 0xFFFFFFFFL);
    }

    /**
     * Steps back over the whitespace before an index, no further than the row's start.
     */
    private static int trimmed(final CharSequence text, final int start, final int index) {
        int i = index;
        while (i > start && Character.isWhitespace(Character.codePointBefore(text, i))) {
            i -= Character.charCount(Character.codePointBefore(text, i));
        }
        return i;
    }

    /**
     * Measures ranges of the text being wrapped.
     */
    @FunctionalInterface
    public interface Measure {

        /**
         * Returns how wide a range of the text is drawn.
         *
         * @param start the first index
         * @param end the index after the range
         * @return the width, in UI units; 0 for an empty range
         */
        float width(int start, int end);
    }

    /**
     * The boundaries between the characters of the text being wrapped: its grapheme clusters, as {@link Graphemes}
     * finds them, or a password's code points.
     */
    public interface Characters {

        /**
         * Returns the boundary after an index.
         *
         * @param index an index from 0 to below the length; it need not be a boundary
         * @return the first boundary after it
         */
        int next(int index);

        /**
         * Returns the boundary before an index.
         *
         * @param index an index from above 0 to the length; it need not be a boundary
         * @return the last boundary before it
         */
        int previous(int index);
    }
}
