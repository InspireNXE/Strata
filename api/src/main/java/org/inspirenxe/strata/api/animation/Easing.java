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
package org.inspirenxe.strata.api.animation;

/**
 * How an animation's progress through its time turns into progress through its values. An animator calls it once a
 * frame with the fraction of the time gone, from 0 to 1, and moves the value by what it returns. Whatever it
 * returns, the animator's last step is exactly 1.
 *
 * <p>An easing must return 0 for 0 and 1 for 1, and may go below 0 or above 1 in between, to overshoot and settle.
 * It is called on the UI thread, every frame of every animation that uses it, so it must be quick and must not
 * allocate.</p>
 */
@FunctionalInterface
public interface Easing {
    /**
     * Even speed from start to end.
     */
    Easing LINEAR = t -> t;
    /**
     * Starts slowly and ends at full speed: CSS's {@code ease-in}, {@code cubicBezier(0.42, 0, 1, 1)}.
     */
    Easing EASE_IN = Easing.cubicBezier(0.42f, 0f, 1f, 1f);
    /**
     * Starts at full speed and slows to a stop: CSS's {@code ease-out}, {@code cubicBezier(0, 0, 0.58, 1)}. The
     * usual choice for something that answers the user, such as a panel opening.
     */
    Easing EASE_OUT = Easing.cubicBezier(0f, 0f, 0.58f, 1f);
    /**
     * Starts slowly, speeds up and slows to a stop: CSS's {@code ease-in-out}, {@code cubicBezier(0.42, 0, 0.58, 1)}.
     */
    Easing EASE_IN_OUT = Easing.cubicBezier(0.42f, 0f, 0.58f, 1f);

    /**
     * Returns the progress through the values for a fraction of the time.
     *
     * @param t the fraction of the animation's time gone, from 0 to 1
     * @return the fraction of the way from the start value to the end value; 0 for 0 and 1 for 1, possibly outside
     *     0 to 1 in between
     */
    float apply(float t);

    /**
     * Returns the easing of a cubic Bézier curve from (0, 0) to (1, 1) with two control points, as CSS's
     * {@code cubic-bezier()} defines it. The x of each point is time and must be from 0 to 1; the y is progress and
     * may go outside 0 to 1, which makes the animation overshoot.
     *
     * @param x1 the first control point's time, from 0 to 1
     * @param y1 the first control point's progress
     * @param x2 the second control point's time, from 0 to 1
     * @param y2 the second control point's progress
     * @return the easing
     * @throws IllegalArgumentException if {@code x1} or {@code x2} is outside 0 to 1, or a value is not finite
     */
    static Easing cubicBezier(final float x1, final float y1, final float x2, final float y2) {
        return new CubicBezier(x1, y1, x2, y2);
    }
}
