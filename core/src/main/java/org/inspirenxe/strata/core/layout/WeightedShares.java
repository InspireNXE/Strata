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
package org.inspirenxe.strata.core.layout;

/**
 * Shares a length among weighted slots, each kept between a smallest and a largest size. Slots with a weight of 0
 * take no part and get 0.
 *
 * <p>{@link #resolve} gives every slot the length times its weight over the total weight. A slot whose share falls
 * outside its limits is held at the limit, and the slots still free share what the held ones leave by the same rule,
 * until no free slot crosses a limit. When raising slots to their minimum adds more length than lowering others to
 * their maximum takes away, a round holds only the raised slots, and otherwise only the lowered ones, as CSS
 * flexible boxes resolve their lengths, so no slot is held at a limit it would not reach in the end. A minimum wins
 * over a smaller maximum. When the minimums add up to more than the length, every slot still gets its minimum and
 * the slots together overflow the length.</p>
 *
 * <p>{@link #fit} finds the length a container asks for when it has no limit. The arrays are kept between uses, so
 * layout allocates only when the number of slots grows.</p>
 */
final class WeightedShares {
    private float[] weights;
    private float[] mins;
    private float[] maxes;
    private float[] needs;
    private float[] sizes;
    private boolean[] held;
    private int count;

    /**
     * Creates shares for no slots.
     */
    WeightedShares() {
        this.weights = new float[0];
        this.mins = new float[0];
        this.maxes = new float[0];
        this.needs = new float[0];
        this.sizes = new float[0];
        this.held = new boolean[0];
    }

    /**
     * Starts over with a number of slots, each with a weight of 0, no limits and no need.
     *
     * @param count the number of slots
     */
    void reset(final int count) {
        if (this.weights.length < count) {
            this.weights = new float[count];
            this.mins = new float[count];
            this.maxes = new float[count];
            this.needs = new float[count];
            this.sizes = new float[count];
            this.held = new boolean[count];
        }
        this.count = count;
        for (int i = 0; i < count; i++) {
            this.weights[i] = 0f;
            this.mins[i] = 0f;
            this.maxes[i] = Float.POSITIVE_INFINITY;
            this.needs[i] = 0f;
            this.sizes[i] = 0f;
        }
    }

    /**
     * Sets a slot's weight and limits.
     *
     * @param index the slot
     * @param weight its weight; 0 or less leaves it out
     * @param min the smallest size it may get
     * @param max the largest size it may get; may be infinite
     */
    void set(final int index, final float weight, final float min, final float max) {
        this.weights[index] = weight;
        this.mins[index] = min;
        this.maxes[index] = max;
    }

    /**
     * Sets the size a slot asks for, which {@link #fit} reads.
     *
     * @param index the slot
     * @param need its size
     */
    void need(final int index, final float need) {
        this.needs[index] = need;
    }

    /**
     * Returns a slot's size from the last {@link #resolve} or {@link #fit}.
     *
     * @param index the slot
     * @return its size; 0 for a slot left out
     */
    float size(final int index) {
        return this.sizes[index];
    }

    /**
     * Shares a length among the slots by weight within their limits.
     *
     * @param length the length to share; below 0 counts as 0
     */
    void resolve(final float length) {
        for (int i = 0; i < this.count; i++) {
            this.held[i] = this.weights[i] <= 0f;
            this.sizes[i] = 0f;
        }
        while (true) {
            float free = length;
            float total = 0f;
            for (int i = 0; i < this.count; i++) {
                if (this.weights[i] <= 0f) {
                    continue;
                }
                if (this.held[i]) {
                    free -= this.sizes[i];
                } else {
                    total += this.weights[i];
                }
            }
            if (total <= 0f) {
                return;
            }
            final float unit = Math.max(0f, free) / total;
            float violation = 0f;
            for (int i = 0; i < this.count; i++) {
                if (!this.held[i]) {
                    final float target = unit * this.weights[i];
                    this.sizes[i] = this.clamp(i, target);
                    violation += this.sizes[i] - target;
                }
            }
            if (violation == 0f) {
                return;
            }
            // Hold the slots that crossed the limit on the side that won, and share again
            for (int i = 0; i < this.count; i++) {
                if (!this.held[i]) {
                    final float target = unit * this.weights[i];
                    this.held[i] = violation > 0f ? this.sizes[i] > target : this.sizes[i] < target;
                }
            }
        }
    }

    /**
     * Sizes the slots by the smallest unit of length per weight that gives every slot its {@linkplain #need need},
     * each slot then kept within its limits. Sharing the sum of the sizes with {@link #resolve} gives the same
     * sizes.
     *
     * @return the length, the sum of the slots' sizes
     */
    float fit() {
        float unit = 0f;
        for (int i = 0; i < this.count; i++) {
            if (this.weights[i] > 0f) {
                unit = Math.max(unit, this.needs[i] / this.weights[i]);
            }
        }
        float length = 0f;
        for (int i = 0; i < this.count; i++) {
            this.sizes[i] = this.weights[i] > 0f ? this.clamp(i, unit * this.weights[i]) : 0f;
            length += this.sizes[i];
        }
        return length;
    }

    private float clamp(final int index, final float size) {
        return Math.max(this.mins[index], Math.min(this.maxes[index], size));
    }
}
