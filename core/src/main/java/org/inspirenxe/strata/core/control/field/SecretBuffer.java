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

import java.util.Arrays;

/**
 * The text of a password box: a {@code char[]} and a length, kept zeroed wherever it holds no text.
 *
 * <ul>
 *     <li>Growing copies into a larger array and zeroes the old one.</li>
 *     <li>Every delete zeroes the chars freed at the end; {@link #clear()} zeroes the whole array.</li>
 *     <li>Text leaves only through {@link #copyTo}, and as a {@code String} only through {@link #reveal()}, which a
 *     revealed box calls to draw it.</li>
 *     <li>{@link #matches} compares in time that depends only on the lengths.</li>
 *     <li>It is not a {@link CharSequence}, so it cannot be passed where a {@code String} is expected by mistake, and
 *     {@link #toString()} prints only its length.</li>
 * </ul>
 */
final class SecretBuffer implements EditBuffer {
    private static final int MIN_CAPACITY = 16;

    private char[] chars;
    private int length;

    SecretBuffer() {
        this.chars = new char[MIN_CAPACITY];
    }

    @Override
    public int length() {
        return this.length;
    }

    @Override
    public char charAt(final int index) {
        if (index < 0 || index >= this.length) {
            throw new IndexOutOfBoundsException("Index " + index + " outside a length of " + this.length);
        }
        return this.chars[index];
    }

    @Override
    public void insert(final int index, final CharSequence text) {
        final int count = text.length();
        this.open(index, count);
        for (int i = 0; i < count; i++) {
            this.chars[index + i] = text.charAt(i);
        }
    }

    @Override
    public void insertCodePoint(final int index, final int codePoint) {
        this.open(index, Character.charCount(codePoint));
        Character.toChars(codePoint, this.chars, index);
    }

    /**
     * Inserts chars from an array.
     */
    void insert(final int index, final char[] source, final int from, final int count) {
        this.open(index, count);
        System.arraycopy(source, from, this.chars, index, count);
    }

    /**
     * Makes room for {@code count} chars at an index, growing the array if needed.
     */
    private void open(final int index, final int count) {
        if (index < 0 || index > this.length) {
            throw new IndexOutOfBoundsException("Index " + index + " outside a length of " + this.length);
        }
        this.ensureCapacity(this.length + count);
        System.arraycopy(this.chars, index, this.chars, index + count, this.length - index);
        this.length += count;
    }

    private void ensureCapacity(final int needed) {
        if (needed <= this.chars.length) {
            return;
        }
        final char[] grown = new char[Math.max(needed, this.chars.length * 2)];
        System.arraycopy(this.chars, 0, grown, 0, this.length);
        Arrays.fill(this.chars, '\0');
        this.chars = grown;
    }

    @Override
    public void delete(final int start, final int end) {
        if (start < 0 || end > this.length || start > end) {
            throw new IndexOutOfBoundsException("Range " + start + " to " + end + " outside a length of " + this.length);
        }
        final int count = end - start;
        System.arraycopy(this.chars, end, this.chars, start, this.length - end);
        Arrays.fill(this.chars, this.length - count, this.length, '\0');
        this.length -= count;
    }

    @Override
    public void clear() {
        Arrays.fill(this.chars, '\0');
        this.length = 0;
    }

    /**
     * Copies the text into the start of an array at least {@link #length()} long.
     */
    void copyTo(final char[] destination) {
        System.arraycopy(this.chars, 0, destination, 0, this.length);
    }

    /**
     * Makes a {@code String} of the text, for a revealed box to measure and draw. Nothing else may call it.
     */
    String reveal() {
        return new String(this.chars, 0, this.length);
    }

    /**
     * Returns whether another buffer holds the same chars, in time that depends only on the two lengths.
     */
    boolean matches(final SecretBuffer other) {
        return equalChars(this.chars, this.length, other.chars, other.length);
    }

    /**
     * Returns whether an array holds the same chars, in time that depends only on the two lengths.
     */
    boolean matches(final char[] other) {
        return equalChars(this.chars, this.length, other, other.length);
    }

    private static boolean equalChars(final char[] a, final int aLength, final char[] b, final int bLength) {
        int difference = aLength ^ bLength;
        final int longest = Math.max(aLength, bLength);
        for (int i = 0; i < longest; i++) {
            final char x = i < aLength ? a[i] : 0;
            final char y = i < bLength ? b[i] : 0;
            difference |= x ^ y;
        }
        return difference == 0;
    }

    /**
     * Returns the array that holds the text, for tests that check what is left in it. Nothing else may call it.
     */
    char[] storage() {
        return this.chars;
    }

    /**
     * Prints only the length, never the text.
     */
    @Override
    public String toString() {
        return "SecretBuffer[length=" + this.length + "]";
    }
}
