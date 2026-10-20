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
 * The text of a field whose text code may read: a {@link StringBuilder} that hands out {@code String}s.
 */
final class PlainBuffer implements EditBuffer {
    private final StringBuilder builder;

    PlainBuffer() {
        this.builder = new StringBuilder();
    }

    @Override
    public int length() {
        return this.builder.length();
    }

    @Override
    public char charAt(final int index) {
        return this.builder.charAt(index);
    }

    @Override
    public boolean isEmpty() {
        return this.builder.isEmpty();
    }

    @Override
    public int codePointAt(final int index) {
        return this.builder.codePointAt(index);
    }

    @Override
    public int codePointBefore(final int index) {
        return this.builder.codePointBefore(index);
    }

    @Override
    public int codePointCount(final int start, final int end) {
        return this.builder.codePointCount(start, end);
    }

    @Override
    public int offsetByCodePoints(final int index, final int codePoints) {
        return this.builder.offsetByCodePoints(index, codePoints);
    }

    @Override
    public void insert(final int index, final CharSequence text) {
        this.builder.insert(index, text);
    }

    @Override
    public void insertCodePoint(final int index, final int codePoint) {
        this.builder.insert(index, Character.toChars(codePoint));
    }

    @Override
    public void delete(final int start, final int end) {
        this.builder.delete(start, end);
    }

    @Override
    public void clear() {
        this.builder.setLength(0);
    }

    /**
     * Replaces the whole text.
     */
    void set(final String text) {
        this.builder.setLength(0);
        this.builder.append(text);
    }

    /**
     * Cuts the text to a number of chars.
     */
    void truncate(final int length) {
        this.builder.setLength(length);
    }

    /**
     * Replaces a range with other chars.
     */
    void replace(final int start, final int end, final String text) {
        this.builder.replace(start, end, text);
    }

    /**
     * Returns whether the text equals a string, without making one.
     */
    boolean contentEquals(final String text) {
        return text.contentEquals(this.builder);
    }

    /**
     * Returns a range as a string.
     */
    String substring(final int start, final int end) {
        return this.builder.substring(start, end);
    }

    /**
     * Returns where a string next occurs at or after an index, or -1.
     */
    int indexOf(final String text, final int from) {
        return this.builder.indexOf(text, from);
    }

    /**
     * Returns where a string last occurs at or before an index, or -1.
     */
    int lastIndexOf(final String text, final int from) {
        return this.builder.lastIndexOf(text, from);
    }

    /**
     * Returns the text.
     */
    @Override
    public String toString() {
        return this.builder.toString();
    }
}
