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
 * Code point and word boundaries over an {@link EditBuffer}. A word is a run of characters that are not whitespace;
 * a line break also ends a run, for text areas. With {@code oneWord} the whole text is one word, so a password box's
 * word movement and double click never look at what the text holds.
 */
final class TextBoundaries {
    private final EditBuffer buffer;
    private final boolean oneWord;

    TextBoundaries(final EditBuffer buffer, final boolean oneWord) {
        this.buffer = buffer;
        this.oneWord = oneWord;
    }

    /**
     * Clamps an index to the text and moves it off the middle of a surrogate pair.
     */
    int snap(final int index) {
        final int length = this.buffer.length();
        final int clamped = Math.max(0, Math.min(length, index));
        if (clamped > 0 && clamped < length
                && Character.isLowSurrogate(this.buffer.charAt(clamped))
                && Character.isHighSurrogate(this.buffer.charAt(clamped - 1))) {
            return clamped - 1;
        }
        return clamped;
    }

    /**
     * The boundary one code point before an index, or 0.
     */
    int previousCodePoint(final int index) {
        return index <= 0 ? 0 : index - Character.charCount(this.buffer.codePointBefore(index));
    }

    /**
     * The boundary one code point after an index, or the end.
     */
    int nextCodePoint(final int index) {
        final int length = this.buffer.length();
        return index >= length ? length : index + Character.charCount(this.buffer.codePointAt(index));
    }

    /**
     * For Ctrl+Left and Ctrl+Backspace: skips whitespace leftwards, then the word before it.
     */
    int previousWordStart(final int index) {
        if (this.oneWord) {
            return 0;
        }
        int i = index;
        while (i > 0 && Character.isWhitespace(this.buffer.codePointBefore(i))) {
            i = this.previousCodePoint(i);
        }
        while (i > 0 && !Character.isWhitespace(this.buffer.codePointBefore(i))) {
            i = this.previousCodePoint(i);
        }
        return i;
    }

    /**
     * For Ctrl+Right and Ctrl+Delete: skips whitespace rightwards, then the word after it.
     */
    int nextWordEnd(final int index) {
        final int length = this.buffer.length();
        if (this.oneWord) {
            return length;
        }
        int i = index;
        while (i < length && Character.isWhitespace(this.buffer.codePointAt(i))) {
            i = this.nextCodePoint(i);
        }
        while (i < length && !Character.isWhitespace(this.buffer.codePointAt(i))) {
            i = this.nextCodePoint(i);
        }
        return i;
    }

    /**
     * The run of word or whitespace characters around an index, as {start, end}, stopping at line breaks. At the very
     * end of the text, the run before it. The whole text when it is one word.
     */
    int[] wordAround(final int index) {
        final int length = this.buffer.length();
        if (length == 0) {
            return new int[] {0, 0};
        }
        if (this.oneWord) {
            return new int[] {0, length};
        }
        final int at = index >= length ? this.previousCodePoint(index) : index;
        final boolean whitespace = Character.isWhitespace(this.buffer.codePointAt(at));
        int start = at;
        while (start > 0 && Character.isWhitespace(this.buffer.codePointBefore(start)) == whitespace
                && this.buffer.charAt(start - 1) != '\n') {
            start = this.previousCodePoint(start);
        }
        int end = at;
        while (end < length && Character.isWhitespace(this.buffer.codePointAt(end)) == whitespace
                && this.buffer.charAt(end) != '\n') {
            end = this.nextCodePoint(end);
        }
        return new int[] {start, end};
    }
}
