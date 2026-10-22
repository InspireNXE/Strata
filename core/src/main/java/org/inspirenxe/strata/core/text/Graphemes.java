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

import java.text.BreakIterator;

/**
 * Grapheme clusters, the characters a reader sees: a flag, an emoji with its skin tone, a letter with its accents.
 * They are found with {@link BreakIterator#getCharacterInstance()}, so wrapping, ellipsis and length limits never cut
 * one apart.
 */
public final class Graphemes {

    private Graphemes() {
    }

    /**
     * Returns the cluster boundaries of a text, for wrapping. The text is read once, now.
     *
     * @param text the text
     * @return its cluster boundaries
     */
    public static LineBreaks.Characters of(final CharSequence text) {
        final BreakIterator iterator = BreakIterator.getCharacterInstance();
        iterator.setText(text.toString());
        final int length = text.length();
        return new LineBreaks.Characters() {
            @Override
            public int next(final int index) {
                if (index >= length) {
                    return length;
                }
                final int boundary = iterator.following(Math.max(0, index));
                return boundary == BreakIterator.DONE ? length : boundary;
            }

            @Override
            public int previous(final int index) {
                if (index <= 0) {
                    return 0;
                }
                final int boundary = iterator.preceding(Math.min(index, length));
                return boundary == BreakIterator.DONE ? 0 : boundary;
            }
        };
    }

    /**
     * Returns the cluster boundary at or before an index, the start of the cluster the index falls in.
     *
     * @param characters the boundaries of the text
     * @param index an index, already clamped to the text
     * @return the boundary
     */
    public static int snap(final LineBreaks.Characters characters, final int index) {
        if (index <= 0) {
            return 0;
        }
        // The first boundary after the char before the index is the index itself when the index is a boundary
        final int after = characters.next(index - 1);
        return after == index ? index : characters.previous(after);
    }

    /**
     * Returns how many chars of the start of a text hold at most a number of code points without cutting a cluster
     * apart: a cluster that would cross the limit is left out whole.
     *
     * @param text the text
     * @param codePoints the most code points to keep
     * @return the length in chars of the prefix; 0 for a count of 0 or less
     */
    public static int cut(final String text, final int codePoints) {
        if (codePoints <= 0) {
            return 0;
        }
        if (text.codePointCount(0, text.length()) <= codePoints) {
            return text.length();
        }
        final int limit = text.offsetByCodePoints(0, codePoints);
        final BreakIterator iterator = BreakIterator.getCharacterInstance();
        iterator.setText(text);
        return iterator.isBoundary(limit) ? limit : Math.max(0, iterator.preceding(limit));
    }
}
