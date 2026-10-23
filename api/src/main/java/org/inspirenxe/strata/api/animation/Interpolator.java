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

import org.inspirenxe.strata.api.geometry.Insets;

/**
 * The value a fraction of the way between two values, for animating a property of any type. The built-in ones
 * return {@code from} for exactly 0 and {@code to} for exactly 1, and go past either end for a fraction outside 0
 * to 1, as an easing that overshoots asks.
 *
 * <p>An interpolator is called on the UI thread, once a frame for each property it animates. It must not change
 * either value.</p>
 *
 * @param <V> the type of the values
 */
@FunctionalInterface
public interface Interpolator<V> {
    /**
     * Straight between two doubles.
     */
    Interpolator<Double> DOUBLE = (from, to, t) -> t == 0f ? from : t == 1f ? to : from + (to - from) * t;
    /**
     * Straight between two floats.
     */
    Interpolator<Float> FLOAT = (from, to, t) -> t == 0f ? from : t == 1f ? to : from + (to - from) * t;
    /**
     * Between two ARGB colors, each of alpha, red, green and blue on its own, rounded and kept from 0 to 255.
     */
    Interpolator<Integer> ARGB = (from, to, t) -> t == 0f ? from : t == 1f ? to
            : Interpolator.channel(from, to, t, 24) | Interpolator.channel(from, to, t, 16)
            | Interpolator.channel(from, to, t, 8) | Interpolator.channel(from, to, t, 0);
    /**
     * Between two insets, each side on its own, kept from going below 0.
     */
    Interpolator<Insets> INSETS = (from, to, t) -> t == 0f ? from : t == 1f ? to : new Insets(
            Math.max(0f, from.left() + (to.left() - from.left()) * t),
            Math.max(0f, from.top() + (to.top() - from.top()) * t),
            Math.max(0f, from.right() + (to.right() - from.right()) * t),
            Math.max(0f, from.bottom() + (to.bottom() - from.bottom()) * t));

    /**
     * Returns the value a fraction of the way from one value to another.
     *
     * @param from the value at 0
     * @param to the value at 1
     * @param t the fraction, usually from 0 to 1 and outside it when the easing overshoots
     * @return the value between
     */
    V between(V from, V to, float t);

    /**
     * One 8-bit channel of an ARGB color, interpolated, rounded and kept from 0 to 255, in its place.
     */
    private static int channel(final int from, final int to, final float t, final int shift) {
        final int a = (from >>> shift) & 0xFF;
        final int b = (to >>> shift) & 0xFF;
        return Math.max(0, Math.min(255, Math.round(a + (b - a) * t))) << shift;
    }
}
