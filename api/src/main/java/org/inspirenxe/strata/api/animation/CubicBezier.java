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
 * The easing {@link Easing#cubicBezier} makes. It finds the curve's parameter for a time by Newton's method, falling
 * back to bisection where the slope is too flat, and returns the curve's progress there.
 *
 * @param x1 the first control point's time
 * @param y1 the first control point's progress
 * @param x2 the second control point's time
 * @param y2 the second control point's progress
 */
record CubicBezier(float x1, float y1, float x2, float y2) implements Easing {
    private static final double EPSILON = 1e-6;

    CubicBezier {
        if (!(x1 >= 0f && x1 <= 1f) || !(x2 >= 0f && x2 <= 1f)) {
            throw new IllegalArgumentException("A control point's time must be from 0 to 1: " + x1 + ", " + x2);
        }
        if (!Float.isFinite(y1) || !Float.isFinite(y2)) {
            throw new IllegalArgumentException("A control point's progress must be finite: " + y1 + ", " + y2);
        }
    }

    @Override
    public float apply(final float t) {
        if (t <= 0f) {
            return 0f;
        }
        if (t >= 1f) {
            return 1f;
        }
        return (float) CubicBezier.curve(this.parameterAt(t), this.y1, this.y2);
    }

    /**
     * Returns the parameter whose point on the curve has the given time.
     */
    private double parameterAt(final double time) {
        double u = time;
        for (int i = 0; i < 8; i++) {
            final double error = CubicBezier.curve(u, this.x1, this.x2) - time;
            if (Math.abs(error) < EPSILON) {
                return u;
            }
            final double slope = CubicBezier.slope(u, this.x1, this.x2);
            if (Math.abs(slope) < EPSILON) {
                break;
            }
            u -= error / slope;
        }
        double low = 0d;
        double high = 1d;
        u = time;
        for (int i = 0; i < 40 && high - low > EPSILON; i++) {
            if (CubicBezier.curve(u, this.x1, this.x2) < time) {
                low = u;
            } else {
                high = u;
            }
            u = (low + high) / 2d;
        }
        return u;
    }

    /**
     * One coordinate of the curve from 0 to 1 through two control values, at parameter {@code u}.
     */
    private static double curve(final double u, final double a, final double b) {
        final double v = 1d - u;
        return 3d * v * v * u * a + 3d * v * u * u * b + u * u * u;
    }

    private static double slope(final double u, final double a, final double b) {
        final double v = 1d - u;
        return 3d * v * v * a + 6d * v * u * (b - a) + 3d * u * u * (1d - b);
    }
}
