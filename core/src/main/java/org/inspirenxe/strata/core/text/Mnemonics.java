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

import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.text.TextSpan;

import java.util.ArrayList;
import java.util.List;

/**
 * Finds the mnemonic of a caption: the letter after the first single underscore in its resolved text, as in
 * {@code "_File"} or {@code "Save _as"}. A doubled underscore stands for one underscore and marks nothing, and an
 * underscore at the very end is kept as it is. Every marker is taken out of the text that is drawn, whether or not
 * mnemonics are shown, so a caption never shows its markers.
 *
 * <p>The mnemonic is compared in lower case in the root locale, as a code point; {@link #NONE} when the caption has
 * none. The marked spans are the drawn spans with the mnemonic's letter split into a span of its own, underlined in
 * the style of the span it came from.</p>
 *
 * @param plain the spans to draw while mnemonics are hidden, markers taken out
 * @param marked the spans to draw while mnemonics show, the mnemonic's letter underlined; the same list as
 *     {@code plain} when there is no mnemonic
 * @param mnemonic the mnemonic in lower case, or {@link #NONE}
 */
public record Mnemonics(List<TextSpan> plain, List<TextSpan> marked, int mnemonic) {
    /**
     * The mnemonic of a caption that has none.
     */
    public static final int NONE = -1;

    /**
     * Parses resolved spans. Spans with no underscore are returned as they are.
     *
     * @param spans the resolved caption
     * @return the parsed caption
     */
    public static Mnemonics parse(final List<TextSpan> spans) {
        boolean any = false;
        for (final TextSpan span : spans) {
            if (!span.isInline() && span.text().indexOf('_') >= 0) {
                any = true;
                break;
            }
        }
        if (!any) {
            return new Mnemonics(spans, spans, NONE);
        }
        final List<TextSpan> plain = new ArrayList<>(spans.size());
        final List<TextSpan> marked = new ArrayList<>(spans.size() + 2);
        int mnemonic = NONE;
        for (final TextSpan span : spans) {
            if (span.isInline() || span.text().indexOf('_') < 0) {
                plain.add(span);
                marked.add(span);
                continue;
            }
            final String text = span.text();
            final StringBuilder out = new StringBuilder(text.length());
            int markAt = -1;
            int markEnd = -1;
            for (int i = 0; i < text.length(); i++) {
                final char c = text.charAt(i);
                if (c != '_' || i + 1 >= text.length()) {
                    out.append(c);
                    continue;
                }
                final char next = text.charAt(i + 1);
                if (next == '_') {
                    out.append('_');
                    i++;
                    continue;
                }
                final int codePoint = text.codePointAt(i + 1);
                if (mnemonic == NONE && markAt < 0) {
                    mnemonic = Character.toLowerCase(codePoint);
                    markAt = out.length();
                    markEnd = markAt + Character.charCount(codePoint);
                }
            }
            final String stripped = out.toString();
            if (stripped.isEmpty()) {
                continue;
            }
            final TextSpan strippedSpan = span.withText(stripped);
            plain.add(strippedSpan);
            if (markAt < 0) {
                marked.add(strippedSpan);
                continue;
            }
            if (markAt > 0) {
                marked.add(span.withText(stripped.substring(0, markAt)));
            }
            marked.add(new TextSpan(stripped.substring(markAt, markEnd), span.style().withUnderline(true)));
            if (markEnd < stripped.length()) {
                marked.add(span.withText(stripped.substring(markEnd)));
            }
        }
        final List<TextSpan> drawn = List.copyOf(plain);
        return new Mnemonics(drawn, mnemonic == NONE ? drawn : List.copyOf(marked), mnemonic);
    }

    /**
     * Returns the mnemonic a key stands for on a US layout: its letter in lower case, or its digit on the number
     * row.
     *
     * @param key the key
     * @return the code point, or {@link #NONE} for a key that types no letter or digit
     */
    public static int of(final Key key) {
        if (key.compareTo(Key.A) >= 0 && key.compareTo(Key.Z) <= 0) {
            return 'a' + key.ordinal() - Key.A.ordinal();
        }
        if (key.compareTo(Key.NUM_0) >= 0 && key.compareTo(Key.NUM_9) <= 0) {
            return '0' + key.ordinal() - Key.NUM_0.ordinal();
        }
        return NONE;
    }

    /**
     * Returns the first code point of a caption's drawn text in lower case, for a menu's type-ahead.
     *
     * @param spans the drawn spans
     * @return the code point, or {@link #NONE} for a caption with no text
     */
    public static int initial(final List<TextSpan> spans) {
        for (final TextSpan span : spans) {
            if (!span.isInline() && !span.text().isEmpty()) {
                return Character.toLowerCase(span.text().codePointAt(0));
            }
        }
        return NONE;
    }
}
