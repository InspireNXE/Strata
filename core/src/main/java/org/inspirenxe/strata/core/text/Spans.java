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

import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Character-range work on lists of spans, which {@link CoreText} uses to resolve its operations. Offsets are
 * {@code char} indices into the spans' joined text, and ranges are half-open.
 */
public final class Spans {

    private Spans() {
    }

    /**
     * Returns the spans' joined text.
     *
     * @param spans the spans
     * @return the text
     */
    public static String plain(final List<TextSpan> spans) {
        if (spans.size() == 1) {
            return spans.get(0).text();
        }
        final StringBuilder joined = new StringBuilder();
        for (final TextSpan span : spans) {
            joined.append(span.text());
        }
        return joined.toString();
    }

    /**
     * Returns the spans without empty ones and with each run of adjacent spans of equal style joined into one.
     *
     * @param spans the spans
     * @return an unmodifiable list
     */
    public static List<TextSpan> normalize(final List<TextSpan> spans) {
        final List<TextSpan> out = new ArrayList<>(spans.size());
        for (final TextSpan span : spans) {
            if (span.text().isEmpty()) {
                continue;
            }
            if (!out.isEmpty() && !span.isInline() && !out.get(out.size() - 1).isInline()
                    && out.get(out.size() - 1).style().equals(span.style())) {
                final TextSpan last = out.remove(out.size() - 1);
                out.add(last.withText(last.text() + span.text()));
            } else {
                out.add(span);
            }
        }
        return List.copyOf(out);
    }

    /**
     * Returns the part of the spans between two offsets.
     *
     * @param spans the spans
     * @param start the first offset, inclusive
     * @param end the last offset, exclusive
     * @return the spans in the range, split where the range cuts them
     */
    public static List<TextSpan> slice(final List<TextSpan> spans, final int start, final int end) {
        final List<TextSpan> out = new ArrayList<>();
        int offset = 0;
        for (final TextSpan span : spans) {
            final int spanEnd = offset + span.text().length();
            final int from = Math.max(start, offset);
            final int to = Math.min(end, spanEnd);
            if (from < to) {
                out.add(span.withText(span.text().substring(from - offset, to - offset)));
            }
            offset = spanEnd;
        }
        return out;
    }

    /**
     * Returns the spans with the style of every character between two offsets changed.
     *
     * @param spans the spans
     * @param start the first offset, inclusive
     * @param end the last offset, exclusive
     * @param change the change to each style in the range
     * @return the spans, split where the range cuts them
     */
    public static List<TextSpan> restyle(final List<TextSpan> spans, final int start, final int end,
                                         final UnaryOperator<TextStyle> change) {
        final List<TextSpan> out = new ArrayList<>(spans.size() + 2);
        int offset = 0;
        for (final TextSpan span : spans) {
            final String text = span.text();
            final int spanEnd = offset + text.length();
            final int from = clamp(start - offset, 0, text.length());
            final int to = clamp(end - offset, 0, text.length());
            if (from >= to) {
                out.add(span);
            } else {
                if (from > 0) {
                    out.add(span.withText(text.substring(0, from)));
                }
                out.add(span.withText(text.substring(from, to)).withStyle(change.apply(span.style())));
                if (to < text.length()) {
                    out.add(span.withText(text.substring(to)));
                }
            }
            offset = spanEnd;
        }
        return out;
    }

    /**
     * Returns the style of the character at an offset.
     *
     * @param spans the spans
     * @param offset the offset
     * @param fallback the style for an offset past the end
     * @return the style
     */
    public static TextStyle styleAt(final List<TextSpan> spans, final int offset, final TextStyle fallback) {
        int start = 0;
        for (final TextSpan span : spans) {
            final int end = start + span.text().length();
            if (offset < end) {
                return span.style();
            }
            start = end;
        }
        return fallback;
    }

    /**
     * Does what {@code Math.clamp(long, int, int)} does, which Java 17 lacks.
     */
    private static int clamp(final long value, final int min, final int max) {
        if (min > max) {
            throw new IllegalArgumentException(min + " > " + max);
        }
        return (int) Math.min(max, Math.max(value, min));
    }
}
