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

import org.inspirenxe.strata.core.text.LineBreaks;

import java.text.BreakIterator;

/**
 * Character and word boundaries over an {@link EditBuffer}.
 *
 * <p>Over a field's {@link PlainBuffer} a character is a grapheme cluster as {@link BreakIterator#getCharacterInstance()}
 * finds it, so a flag, an emoji with a skin tone or a letter with its accents is one step. Words are the segments of
 * {@link BreakIterator#getWordInstance()}: a segment that holds a letter or a digit is a word, so an apostrophe or a
 * decimal point inside one stays in it; neighboring segments of punctuation, symbols or emoji make one run; and
 * neighboring whitespace makes one run, which a line break ends. The iterators are given the text again only after it
 * changed, and only when a boundary is asked for.</p>
 *
 * <p>Over a password box's {@link SecretBuffer} a character is a code point, as its mask draws one glyph for each, and
 * the whole text is one word, so word movement and double clicks never look at what the text holds and no
 * {@code String} of it is made.</p>
 */
final class TextBoundaries implements LineBreaks.Characters {
    private static final int SPACE = 0;
    private static final int LINE = 1;
    private static final int WORD = 2;
    private static final int OTHER = 3;

    private final EditBuffer buffer;
    private final PlainBuffer plain;
    private BreakIterator characters;
    private BreakIterator words;
    // The buffer's change count when the iterators last got its text
    private long synced;

    /**
     * Creates the boundaries of a field's text: grapheme clusters and words.
     */
    TextBoundaries(final PlainBuffer buffer) {
        this.buffer = buffer;
        this.plain = buffer;
        this.synced = -1L;
    }

    /**
     * Creates the boundaries of a password: code points, and the whole text one word.
     */
    TextBoundaries(final SecretBuffer buffer) {
        this.buffer = buffer;
        this.plain = null;
        this.synced = -1L;
    }

    /**
     * Gives the iterators the text again if it changed since they last got it.
     */
    private void sync() {
        if (this.synced == this.buffer.changes()) {
            return;
        }
        if (this.characters == null) {
            this.characters = BreakIterator.getCharacterInstance();
            this.words = BreakIterator.getWordInstance();
        }
        final String text = this.plain.toString();
        this.characters.setText(text);
        this.words.setText(text);
        this.synced = this.buffer.changes();
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
     * The boundary one character before an index, or 0: for Left and Backspace.
     */
    @Override
    public int previous(final int index) {
        if (this.plain == null || index <= 0) {
            return this.previousCodePoint(index);
        }
        this.sync();
        final int at = Math.min(index, this.buffer.length());
        final int boundary = this.characters.preceding(at);
        return boundary == BreakIterator.DONE ? 0 : boundary;
    }

    /**
     * The boundary one character after an index, or the end: for Right and Delete.
     */
    @Override
    public int next(final int index) {
        final int length = this.buffer.length();
        if (this.plain == null || index >= length) {
            return this.nextCodePoint(index);
        }
        this.sync();
        final int boundary = this.characters.following(Math.max(0, index));
        return boundary == BreakIterator.DONE ? length : boundary;
    }

    /**
     * For Ctrl+Left and Ctrl+Backspace: skips whitespace leftwards, then the word or run of punctuation before it.
     */
    int previousWordStart(final int index) {
        if (this.plain == null) {
            return 0;
        }
        this.sync();
        int i = Math.min(index, this.buffer.length());
        while (i > 0) {
            final int start = this.wordBefore(i);
            final int kind = this.kind(start, i);
            if (kind != SPACE && kind != LINE) {
                break;
            }
            i = start;
        }
        if (i == 0) {
            return 0;
        }
        int start = this.wordBefore(i);
        final int kind = this.kind(start, i);
        while (kind == OTHER && start > 0) {
            final int before = this.wordBefore(start);
            if (this.kind(before, start) != OTHER) {
                break;
            }
            start = before;
        }
        return start;
    }

    /**
     * For Ctrl+Right and Ctrl+Delete: skips whitespace rightwards, then the word or run of punctuation after it.
     */
    int nextWordEnd(final int index) {
        final int length = this.buffer.length();
        if (this.plain == null) {
            return length;
        }
        this.sync();
        int i = Math.max(0, index);
        while (i < length) {
            final int end = this.wordAfter(i);
            final int kind = this.kind(i, end);
            if (kind != SPACE && kind != LINE) {
                break;
            }
            i = end;
        }
        if (i >= length) {
            return length;
        }
        int end = this.wordAfter(i);
        final int kind = this.kind(i, end);
        while (kind == OTHER && end < length) {
            final int after = this.wordAfter(end);
            if (this.kind(end, after) != OTHER) {
                break;
            }
            end = after;
        }
        return end;
    }

    /**
     * The word, run of punctuation or run of whitespace around an index, as {start, end}, for a double click. A run
     * of whitespace stops at line breaks. At a line break or the very end of the text it is the run before, and
     * when the text is one word it is the whole text.
     */
    int[] wordAround(final int index) {
        final int length = this.buffer.length();
        if (length == 0) {
            return new int[] {0, 0};
        }
        if (this.plain == null) {
            return new int[] {0, length};
        }
        this.sync();
        int at = Math.max(0, Math.min(index, length));
        if ((at == length || this.buffer.charAt(at) == '\n') && at > 0 && this.buffer.charAt(at - 1) != '\n') {
            at = this.previousCodePoint(at);
        }
        if (at == length || this.buffer.charAt(at) == '\n') {
            return new int[] {at, at};
        }
        int start = this.words.isBoundary(at) ? at : this.wordBefore(at);
        int end = this.wordAfter(at);
        final int kind = this.kind(start, end);
        if (kind == SPACE || kind == OTHER) {
            while (start > 0) {
                final int before = this.wordBefore(start);
                if (this.kind(before, start) != kind) {
                    break;
                }
                start = before;
            }
            while (end < length) {
                final int after = this.wordAfter(end);
                if (this.kind(end, after) != kind) {
                    break;
                }
                end = after;
            }
        }
        return new int[] {start, end};
    }

    /**
     * The word boundary before an index, which is above 0.
     */
    private int wordBefore(final int index) {
        final int boundary = this.words.preceding(index);
        return boundary == BreakIterator.DONE ? 0 : boundary;
    }

    /**
     * The word boundary after an index, which is below the length.
     */
    private int wordAfter(final int index) {
        final int boundary = this.words.following(index);
        return boundary == BreakIterator.DONE ? this.buffer.length() : boundary;
    }

    /**
     * What a segment of the text is: a line break, whitespace, a word if it holds a letter or a digit, or else
     * punctuation, symbols or emoji.
     */
    private int kind(final int start, final int end) {
        final int first = this.buffer.codePointAt(start);
        if (first == '\n') {
            return LINE;
        }
        if (Character.isWhitespace(first)) {
            return SPACE;
        }
        for (int i = start; i < end; i = this.nextCodePoint(i)) {
            if (Character.isLetterOrDigit(this.buffer.codePointAt(i))) {
                return WORD;
            }
        }
        return OTHER;
    }
}
