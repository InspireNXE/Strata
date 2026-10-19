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
package org.inspirenxe.strata.api.geometry;

/**
 * Space on each side of a rectangle, in UI units. Strata uses insets for margins (outside a component), padding
 * (inside a container) and nine-slice borders.
 *
 * <p>Values may be negative; a negative margin lets a component overlap its neighbors. Layout clamps the
 * resulting sizes at zero.</p>
 *
 * @param left space on the left
 * @param top space on the top
 * @param right space on the right
 * @param bottom space on the bottom
 */
public record Insets(float left, float top, float right, float bottom) {
    /**
     * No space on any side.
     */
    public static final Insets NONE = new Insets(0, 0, 0, 0);

    /**
     * Creates insets with the same value on all four sides.
     *
     * @param all the space on every side
     * @return the insets
     */
    public static Insets of(final float all) {
        return new Insets(all, all, all, all);
    }

    /**
     * Creates insets with one value for the left and right sides and another for the top and bottom.
     *
     * @param horizontal the space on the left and right
     * @param vertical the space on the top and bottom
     * @return the insets
     */
    public static Insets of(final float horizontal, final float vertical) {
        return new Insets(horizontal, vertical, horizontal, vertical);
    }

    /**
     * Returns the total horizontal space.
     *
     * @return {@code left + right}
     */
    public float horizontal() {
        return this.left + this.right;
    }

    /**
     * Returns the total vertical space.
     *
     * @return {@code top + bottom}
     */
    public float vertical() {
        return this.top + this.bottom;
    }
}
