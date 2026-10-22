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
package org.inspirenxe.strata.api.layout;

/**
 * How big a {@link GridPanel} row or column is.
 *
 * <p>The grid first gives fixed tracks their size and auto tracks the size of their largest child. Whatever width
 * (or height) remains is then shared among weighted tracks in proportion to their weights. When the grid is
 * measured without a size limit, weighted tracks take the smallest size that fits their children while keeping
 * their proportions.</p>
 */
public sealed interface GridLength {
    /**
     * The shared auto length.
     */
    GridLength AUTO = new Auto();

    /**
     * Returns a track that sizes to its largest child.
     *
     * @return {@link #AUTO}
     */
    static GridLength auto() {
        return AUTO;
    }

    /**
     * Returns a track of a fixed size.
     *
     * @param size the size in UI units
     * @return the fixed length
     * @throws IllegalArgumentException if {@code size} is negative, infinite or NaN
     */
    static GridLength fixed(final float size) {
        return new Fixed(size);
    }

    /**
     * Returns a track that takes a share of the leftover space.
     *
     * @param weight the share; a track of weight 2 gets twice the space of a track of weight 1
     * @return the weighted length
     * @throws IllegalArgumentException if {@code weight} is not positive, or is infinite or NaN
     */
    static GridLength weighted(final float weight) {
        return new Weighted(weight);
    }

    /**
     * A track as large as the largest child that sits only in it. Children spanning several tracks grow the auto
     * tracks they span if they would not fit otherwise.
     */
    record Auto() implements GridLength {
    }

    /**
     * A track of a fixed size, whatever its children need.
     *
     * @param size the size in UI units, finite and not negative
     */
    record Fixed(float size) implements GridLength {
        /**
         * Checks the size.
         *
         * @throws IllegalArgumentException if {@code size} is negative, infinite or NaN
         */
        public Fixed {
            if (!(size >= 0f) || Float.isInfinite(size)) {
                throw new IllegalArgumentException("Fixed size must be finite and not negative: " + size);
            }
        }
    }

    /**
     * A track that takes a share of the space left after fixed and auto tracks.
     *
     * @param weight the share, positive and finite
     */
    record Weighted(float weight) implements GridLength {
        /**
         * Checks the weight.
         *
         * @throws IllegalArgumentException if {@code weight} is not positive, or is infinite or NaN
         */
        public Weighted {
            if (!(weight > 0f) || Float.isInfinite(weight)) {
                throw new IllegalArgumentException("Weight must be positive and finite: " + weight);
            }
        }
    }
}
