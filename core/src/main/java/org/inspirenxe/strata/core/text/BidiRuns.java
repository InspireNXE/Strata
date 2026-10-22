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
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.core.paint.CanvasPainter;

import java.text.Bidi;
import java.util.ArrayList;
import java.util.List;

/**
 * One line of spans in the order it is drawn, by the bidirectional algorithm of the JDK's {@link Bidi} (design
 * 4.49). A Hebrew line that holds an English name shows the name's letters left to right inside the Hebrew read
 * right to left.
 *
 * <p>The line's spans are split where the embedding level changes and the pieces put in visual order. Each piece
 * remembers whether it runs right to left. An engine that {@linkplain org.inspirenxe.strata.spi.Capabilities#shapesRightToLeft
 * shapes right-to-left text} gets such a piece in logical order through
 * {@link org.inspirenxe.strata.spi.Canvas#text(TextSpan, float, float, boolean)}; any other engine gets it with its
 * grapheme clusters in reverse order and its brackets mirrored, which reads correctly for Hebrew and draws Arabic
 * unjoined. Wrapping and ellipsis work on the logical spans before this, as the algorithm requires.</p>
 *
 * <p>A line with no right-to-left text, in a paragraph that runs left to right, keeps its spans as they are and
 * allocates nothing more. Instances are immutable.</p>
 */
public final class BidiRuns {
    private static final boolean[] NONE = new boolean[0];

    private final List<TextSpan> spans;
    private final boolean[] rightToLeft;
    private final TextSpan[] reversed;

    private BidiRuns(final List<TextSpan> spans, final boolean[] rightToLeft, final TextSpan[] reversed) {
        this.spans = spans;
        this.rightToLeft = rightToLeft;
        this.reversed = reversed;
    }

    /**
     * Orders a line's spans for drawing.
     *
     * @param logical the spans in reading order, with no line breaks
     * @param paragraph the paragraph's direction: true for right to left, false for left to right, or null for the
     *     direction of the first letter that has one, left to right when there is none
     * @return the runs
     */
    public static BidiRuns of(final List<TextSpan> logical, final Boolean paragraph) {
        final StringBuilder joined = new StringBuilder();
        for (final TextSpan span : logical) {
            joined.append(span.text());
        }
        final String text = joined.toString();
        if (!Boolean.TRUE.equals(paragraph) && !Bidi.requiresBidi(text.toCharArray(), 0, text.length())) {
            return new BidiRuns(logical, NONE, null);
        }
        final int flags = paragraph == null ? Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT
                : paragraph ? Bidi.DIRECTION_RIGHT_TO_LEFT : Bidi.DIRECTION_LEFT_TO_RIGHT;
        final Bidi bidi = new Bidi(text, flags);
        if (bidi.isLeftToRight()) {
            return new BidiRuns(logical, NONE, null);
        }
        // Split the spans where a run ends, in logical order
        final List<TextSpan> pieces = new ArrayList<>();
        final List<Integer> levels = new ArrayList<>();
        int spanStart = 0;
        for (final TextSpan span : logical) {
            final int spanEnd = spanStart + span.text().length();
            int at = spanStart;
            while (at < spanEnd) {
                final int level = bidi.getLevelAt(at);
                int end = at + 1;
                while (end < spanEnd && bidi.getLevelAt(end) == level) {
                    end++;
                }
                pieces.add(at == spanStart && end == spanEnd ? span
                        : span.withText(text.substring(at, end)));
                levels.add(level);
                at = end;
            }
            spanStart = spanEnd;
        }
        final int count = pieces.size();
        final Object[] order = new Object[count];
        final byte[] levelBytes = new byte[count];
        for (int i = 0; i < count; i++) {
            order[i] = i;
            levelBytes[i] = (byte) (int) levels.get(i);
        }
        Bidi.reorderVisually(levelBytes, 0, order, 0, count);
        final List<TextSpan> visual = new ArrayList<>(count);
        final boolean[] rightToLeft = new boolean[count];
        final TextSpan[] reversed = new TextSpan[count];
        for (int i = 0; i < count; i++) {
            final int index = (Integer) order[i];
            final TextSpan piece = pieces.get(index);
            visual.add(piece);
            rightToLeft[i] = (levels.get(index) & 1) == 1;
            reversed[i] = rightToLeft[i] && !piece.isInline() ? piece.withText(reverse(piece.text())) : piece;
        }
        return new BidiRuns(List.copyOf(visual), rightToLeft, reversed);
    }

    /**
     * Returns the spans in drawing order, left to right.
     *
     * @return the spans
     */
    public List<TextSpan> spans() {
        return this.spans;
    }

    /**
     * Returns whether a span runs right to left.
     *
     * @param index the span's place in {@link #spans()}
     * @return true for a right-to-left run
     */
    public boolean isRightToLeft(final int index) {
        return index < this.rightToLeft.length && this.rightToLeft[index];
    }

    /**
     * Draws one span at a point: as it is when it runs left to right, and otherwise in logical order with its
     * direction to an engine that shapes right-to-left text, or reversed to any other.
     *
     * @param painter what to draw with
     * @param index the span's place in {@link #spans()}
     * @param x its left edge
     * @param y its top
     */
    public void draw(final Painter painter, final int index, final float x, final float y) {
        final TextSpan span = this.spans.get(index);
        if (!this.isRightToLeft(index) || span.isInline()) {
            painter.text(span, x, y);
        } else if (painter instanceof final CanvasPainter canvas) {
            canvas.rightToLeftText(span, this.reversed[index], x, y);
        } else {
            painter.text(this.reversed[index], x, y);
        }
    }

    /**
     * Reverses text by grapheme cluster, so a letter keeps its marks after it, and mirrors the brackets.
     */
    static String reverse(final String text) {
        final LineBreaks.Characters clusters = Graphemes.of(text);
        final StringBuilder out = new StringBuilder(text.length());
        int end = text.length();
        while (end > 0) {
            final int start = clusters.previous(end);
            if (end - start == 1 || end - start == 2 && Character.isHighSurrogate(text.charAt(start))) {
                out.appendCodePoint(mirror(text.codePointAt(start)));
            } else {
                out.append(text, start, end);
            }
            end = start;
        }
        return out.toString();
    }

    private static int mirror(final int codePoint) {
        return switch (codePoint) {
            case '(' -> ')';
            case ')' -> '(';
            case '[' -> ']';
            case ']' -> '[';
            case '{' -> '}';
            case '}' -> '{';
            case '<' -> '>';
            case '>' -> '<';
            case '«' -> '»';
            case '»' -> '«';
            case '‹' -> '›';
            case '›' -> '‹';
            case '≤' -> '≥';
            case '≥' -> '≤';
            default -> codePoint;
        };
    }
}
