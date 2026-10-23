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
package org.inspirenxe.strata.api.extension;

import java.util.List;

/**
 * A linear gradient at any angle, with any number of color stops. Build it once and keep it in a field.
 *
 * <p>The gradient runs through the middle of the rectangle it fills, in the direction of its angle. Offset 0 lies on
 * the line through the corner that comes first along that direction and offset 1 on the line through the corner that
 * comes last, so the stops span the whole rectangle at every angle. Between two stops each channel of the color,
 * alpha included, blends in a straight line. Before the first stop the color is the first stop's, and after the
 * last it is the last stop's.</p>
 *
 * @param angleRadians the direction in radians: 0 runs left to right, {@code PI / 2} top to bottom, {@code PI} right
 *                     to left, as angles turn clockwise on a screen whose y axis points down
 * @param stops the stops, at least two, in order of offset
 */
public record Gradient(float angleRadians, List<Stop> stops) {

    /**
     * Checks the arguments and copies the stops.
     *
     * @throws IllegalArgumentException if the angle is not finite, there are fewer than two stops, or their offsets
     *                                  go down
     */
    public Gradient {
        if (!Float.isFinite(angleRadians)) {
            throw new IllegalArgumentException("Gradient angle must be finite: " + angleRadians);
        }
        stops = List.copyOf(stops);
        if (stops.size() < 2) {
            throw new IllegalArgumentException("A gradient needs at least two stops: " + stops);
        }
        for (int i = 1; i < stops.size(); i++) {
            if (stops.get(i).offset() < stops.get(i - 1).offset()) {
                throw new IllegalArgumentException("Gradient stops must be in order of offset: " + stops);
            }
        }
    }

    /**
     * Returns a gradient from one color at offset 0 to another at offset 1.
     *
     * @param angleRadians the direction in radians; 0 runs left to right
     * @param fromArgb the color at offset 0
     * @param toArgb the color at offset 1
     * @return the gradient
     * @throws IllegalArgumentException if the angle is not finite
     */
    public static Gradient of(final float angleRadians, final int fromArgb, final int toArgb) {
        return new Gradient(angleRadians, List.of(new Stop(0f, fromArgb), new Stop(1f, toArgb)));
    }

    /**
     * Returns the color at an offset along the gradient.
     *
     * @param offset the offset; below 0 gives the first stop's color and above 1 the last stop's
     * @return the color as ARGB
     */
    public int colorAt(final float offset) {
        final List<Stop> stops = this.stops;
        final Stop first = stops.get(0);
        if (!(offset > first.offset())) {
            return first.argb();
        }
        for (int i = 1; i < stops.size(); i++) {
            final Stop next = stops.get(i);
            if (offset <= next.offset()) {
                final Stop previous = stops.get(i - 1);
                final float span = next.offset() - previous.offset();
                return span > 0f ? Gradient.blend(previous.argb(), next.argb(), (offset - previous.offset()) / span)
                        : next.argb();
            }
        }
        return stops.get(stops.size() - 1).argb();
    }

    /**
     * Blends two ARGB colors channel by channel, alpha included.
     *
     * @param from the color at 0
     * @param to the color at 1
     * @param t how far from {@code from} to {@code to}, from 0 to 1
     * @return the blended color
     */
    public static int blend(final int from, final int to, final float t) {
        if (t <= 0f) {
            return from;
        }
        if (t >= 1f) {
            return to;
        }
        int result = 0;
        for (int shift = 0; shift < 32; shift += 8) {
            final int a = (from >>> shift) & 0xFF;
            final int b = (to >>> shift) & 0xFF;
            result |= Math.round(a + (b - a) * t) << shift;
        }
        return result;
    }

    /**
     * One color at one offset along a gradient.
     *
     * @param offset where the color sits, from 0 to 1
     * @param argb the color
     */
    public record Stop(float offset, int argb) {

        /**
         * Checks the offset.
         *
         * @throws IllegalArgumentException if the offset is not between 0 and 1
         */
        public Stop {
            if (!(offset >= 0f) || offset > 1f) {
                throw new IllegalArgumentException("Gradient stop offset must be from 0 to 1: " + offset);
            }
        }
    }
}
