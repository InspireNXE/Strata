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
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A character break iterator over extended grapheme clusters, found with the {@code \X} of {@link Pattern}. Java 17's
 * {@link BreakIterator#getCharacterInstance()} splits flags and emoji sequences, which Java 20's keeps whole. Setting
 * the text finds every boundary at once.
 */
public final class ClusterBreaks extends BreakIterator {

    private static final Pattern CLUSTER = Pattern.compile("\\X");

    private CharacterIterator text = new StringCharacterIterator("");
    private int[] boundaries = {0};
    private int position;

    /**
     * Makes an iterator over the empty text.
     */
    public ClusterBreaks() {
    }

    @Override
    public void setText(final String newText) {
        this.text = new StringCharacterIterator(newText);
        final int[] found = new int[newText.length() + 1];
        int count = 1;
        final Matcher matcher = ClusterBreaks.CLUSTER.matcher(newText);
        while (matcher.find()) {
            found[count++] = matcher.end();
        }
        this.boundaries = Arrays.copyOf(found, count);
        this.position = 0;
    }

    @Override
    public void setText(final CharacterIterator newText) {
        final StringBuilder builder = new StringBuilder(newText.getEndIndex() - newText.getBeginIndex());
        for (char c = newText.first(); c != CharacterIterator.DONE; c = newText.next()) {
            builder.append(c);
        }
        this.setText(builder.toString());
    }

    @Override
    public CharacterIterator getText() {
        return this.text;
    }

    @Override
    public int first() {
        this.position = 0;
        return this.boundaries[0];
    }

    @Override
    public int last() {
        this.position = this.boundaries.length - 1;
        return this.boundaries[this.position];
    }

    @Override
    public int next(final int n) {
        final int target = this.position + n;
        if (target < 0 || target >= this.boundaries.length) {
            this.position = target < 0 ? 0 : this.boundaries.length - 1;
            return BreakIterator.DONE;
        }
        this.position = target;
        return this.boundaries[target];
    }

    @Override
    public int next() {
        return this.next(1);
    }

    @Override
    public int previous() {
        return this.next(-1);
    }

    @Override
    public int following(final int offset) {
        this.check(offset);
        // The index of the first boundary above the offset
        int index = Arrays.binarySearch(this.boundaries, offset);
        index = index >= 0 ? index + 1 : -index - 1;
        if (index >= this.boundaries.length) {
            this.position = this.boundaries.length - 1;
            return BreakIterator.DONE;
        }
        this.position = index;
        return this.boundaries[index];
    }

    @Override
    public int preceding(final int offset) {
        this.check(offset);
        // The index of the last boundary below the offset
        int index = Arrays.binarySearch(this.boundaries, offset);
        index = index >= 0 ? index - 1 : -index - 2;
        if (index < 0) {
            this.position = 0;
            return BreakIterator.DONE;
        }
        this.position = index;
        return this.boundaries[index];
    }

    @Override
    public boolean isBoundary(final int offset) {
        this.check(offset);
        final int index = Arrays.binarySearch(this.boundaries, offset);
        if (index >= 0) {
            this.position = index;
            return true;
        }
        this.following(offset);
        return false;
    }

    @Override
    public int current() {
        return this.boundaries[this.position];
    }

    private void check(final int offset) {
        if (offset < 0 || offset > this.boundaries[this.boundaries.length - 1]) {
            throw new IllegalArgumentException("offset out of bounds: " + offset);
        }
    }
}
