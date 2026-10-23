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
 * The text a field edits, with the operations editing needs. Indexes are char indexes, and callers keep them on code
 * point boundaries. {@link PlainBuffer} gives text fields their {@code String}s; {@link SecretBuffer} keeps a
 * password in a {@code char[]} it zeroes.
 */
interface EditBuffer {

    /**
     * Returns the length in chars.
     */
    int length();

    /**
     * Returns the char at an index.
     */
    char charAt(int index);

    /**
     * Returns how many times the text changed, so a reader can tell whether what it worked out from the text is still
     * current.
     */
    long changes();

    /**
     * Returns whether there are no chars.
     */
    default boolean isEmpty() {
        return this.length() == 0;
    }

    /**
     * Returns the code point that starts at an index, joining a surrogate pair.
     */
    default int codePointAt(final int index) {
        final char high = this.charAt(index);
        if (Character.isHighSurrogate(high) && index + 1 < this.length()) {
            final char low = this.charAt(index + 1);
            if (Character.isLowSurrogate(low)) {
                return Character.toCodePoint(high, low);
            }
        }
        return high;
    }

    /**
     * Returns the code point that ends just before an index, joining a surrogate pair.
     */
    default int codePointBefore(final int index) {
        final char low = this.charAt(index - 1);
        if (Character.isLowSurrogate(low) && index - 2 >= 0) {
            final char high = this.charAt(index - 2);
            if (Character.isHighSurrogate(high)) {
                return Character.toCodePoint(high, low);
            }
        }
        return low;
    }

    /**
     * Returns how many code points a range holds; an unpaired surrogate counts as one.
     */
    default int codePointCount(final int start, final int end) {
        int count = 0;
        int i = start;
        while (i < end) {
            i += Character.charCount(this.codePointAt(i));
            count++;
        }
        return count;
    }

    /**
     * Returns the index a number of code points after another, stopping at the end.
     */
    default int offsetByCodePoints(final int index, final int codePoints) {
        int i = index;
        for (int n = 0; n < codePoints && i < this.length(); n++) {
            i += Character.charCount(this.codePointAt(i));
        }
        return Math.min(i, this.length());
    }

    /**
     * Inserts chars at an index.
     */
    void insert(int index, CharSequence text);

    /**
     * Inserts one code point at an index, as one or two chars.
     */
    void insertCodePoint(int index, int codePoint);

    /**
     * Removes a range.
     */
    void delete(int start, int end);

    /**
     * Removes everything.
     */
    void clear();
}
