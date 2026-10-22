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
package org.inspirenxe.strata.core.control.field;

/**
 * What the text fields keep of the text they are given, so that pasting, setting text from code and typing agree.
 * Typing never inserts a control character or a line or paragraph separator; text from anywhere else loses the same
 * characters, except the line breaks and tabs a text area keeps.
 *
 * <p>A line break is any of LF, VT, FF, CR, NEL (U+0085), the line separator (U+2028) and the paragraph separator
 * (U+2029), with CR LF counting as one.</p>
 */
final class FieldText {

    private FieldText() {
    }

    /**
     * Returns whether a code point breaks a line.
     */
    static boolean isLineBreak(final int codePoint) {
        return codePoint >= '\n' && codePoint <= '\r' || codePoint == 0x85 || codePoint == 0x2028
                || codePoint == 0x2029;
    }

    /**
     * Returns whether typing a code point inserts it: not a control character and not a line or paragraph
     * separator.
     */
    static boolean isTypeable(final int codePoint) {
        return !Character.isISOControl(codePoint) && codePoint != 0x2028 && codePoint != 0x2029;
    }

    /**
     * Returns text as a single-line field holds it: without line breaks and without any other control character.
     */
    static String singleLine(final String text) {
        int i = 0;
        while (i < text.length() && isTypeable(text.charAt(i))) {
            i++;
        }
        if (i == text.length()) {
            return text;
        }
        final StringBuilder kept = new StringBuilder(text.length());
        kept.append(text, 0, i);
        for (; i < text.length(); i++) {
            final char c = text.charAt(i);
            if (isTypeable(c)) {
                kept.append(c);
            }
        }
        return kept.toString();
    }

    /**
     * Returns text as a text area holds it: each line break becomes {@code \n}, tabs stay, and every other control
     * character is dropped.
     */
    static String lines(final String text) {
        int i = 0;
        while (i < text.length() && (isTypeable(text.charAt(i)) || text.charAt(i) == '\n' || text.charAt(i) == '\t')) {
            i++;
        }
        if (i == text.length()) {
            return text;
        }
        final StringBuilder kept = new StringBuilder(text.length());
        kept.append(text, 0, i);
        for (; i < text.length(); i++) {
            final char c = text.charAt(i);
            if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                continue;
            }
            if (isLineBreak(c)) {
                kept.append('\n');
            } else if (c == '\t' || isTypeable(c)) {
                kept.append(c);
            }
        }
        return kept.toString();
    }

    /**
     * Returns text without its line breaks, for a password box, which refuses a paste that still holds a control
     * character.
     */
    static String withoutLineBreaks(final String text) {
        int i = 0;
        while (i < text.length() && !isLineBreak(text.charAt(i))) {
            i++;
        }
        if (i == text.length()) {
            return text;
        }
        final StringBuilder kept = new StringBuilder(text.length());
        kept.append(text, 0, i);
        for (; i < text.length(); i++) {
            final char c = text.charAt(i);
            if (!isLineBreak(c)) {
                kept.append(c);
            }
        }
        return kept.toString();
    }
}
