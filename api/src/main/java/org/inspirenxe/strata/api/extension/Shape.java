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

import org.inspirenxe.strata.api.geometry.Rect;

import java.util.Arrays;

/**
 * A path of lines and curves, built once and drawn many times with {@link Painter#fillShape} and
 * {@link Painter#strokeShape}. Immutable: build it during layout, or once in a field, and draw it in every frame.
 *
 * <p>A shape is made of subpaths. Each starts with {@link Builder#moveTo}, runs through lines, quadratic and cubic
 * curves and arcs, and may be closed back to its start with {@link Builder#close}. A fill closes every subpath and
 * uses the nonzero winding rule, so a subpath inside another that runs the other way makes a hole. A stroke draws
 * each subpath as it was built, with caps on the ends of subpaths that are not closed.</p>
 *
 * <p>Coordinates are UI units, in the painter's coordinates when the shape is drawn, so a shape drawn inside
 * {@link Painter#translated} moves with it. Arcs are stored as cubic curves, so an engine sees only moves, lines,
 * quadratic and cubic curves and closes when it {@linkplain #walk walks} the shape. Two shapes are equal only when
 * they are the same object, so an engine may keep its own copy of a shape, made once, by identity.</p>
 */
public final class Shape {
    private static final byte MOVE = 0;
    private static final byte LINE = 1;
    private static final byte QUAD = 2;
    private static final byte CUBIC = 3;
    private static final byte CLOSE = 4;

    private final byte[] verbs;
    private final float[] coords;
    private final Rect bounds;

    private Shape(final byte[] verbs, final float[] coords, final Rect bounds) {
        this.verbs = verbs;
        this.coords = coords;
        this.bounds = bounds;
    }

    /**
     * Returns a builder for a new shape.
     *
     * @return an empty builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the smallest rectangle that holds every point the shape was built from, control points included. It
     * holds everything a fill draws, and may be larger than it. A stroke reaches past it by half the stroke's width.
     *
     * @return the bounds; {@link Rect#ZERO} for a shape with no points
     */
    public Rect bounds() {
        return this.bounds;
    }

    /**
     * Returns whether the shape has no segments, so drawing it draws nothing.
     *
     * @return true for a shape with no points
     */
    public boolean isEmpty() {
        return this.verbs.length == 0;
    }

    /**
     * Passes every segment of the shape to a sink, in the order it was built. Every subpath starts with
     * {@link Sink#moveTo}. Walking allocates nothing.
     *
     * @param sink what receives the segments
     */
    public void walk(final Sink sink) {
        final float[] c = this.coords;
        int at = 0;
        for (final byte verb : this.verbs) {
            switch (verb) {
                case MOVE -> {
                    sink.moveTo(c[at], c[at + 1]);
                    at += 2;
                }
                case LINE -> {
                    sink.lineTo(c[at], c[at + 1]);
                    at += 2;
                }
                case QUAD -> {
                    sink.quadTo(c[at], c[at + 1], c[at + 2], c[at + 3]);
                    at += 4;
                }
                case CUBIC -> {
                    sink.cubicTo(c[at], c[at + 1], c[at + 2], c[at + 3], c[at + 4], c[at + 5]);
                    at += 6;
                }
                default -> sink.close();
            }
        }
    }

    @Override
    public String toString() {
        return "Shape[segments=" + this.verbs.length + ", bounds=" + this.bounds + "]";
    }

    /**
     * Receives the segments of a shape from {@link Shape#walk}. Engines implement it to build their own path.
     */
    public interface Sink {

        /**
         * Starts a subpath at a point.
         *
         * @param x the x coordinate
         * @param y the y coordinate
         */
        void moveTo(float x, float y);

        /**
         * Adds a straight line from the current point.
         *
         * @param x the end's x coordinate
         * @param y the end's y coordinate
         */
        void lineTo(float x, float y);

        /**
         * Adds a quadratic curve from the current point.
         *
         * @param cx the control point's x coordinate
         * @param cy the control point's y coordinate
         * @param x the end's x coordinate
         * @param y the end's y coordinate
         */
        void quadTo(float cx, float cy, float x, float y);

        /**
         * Adds a cubic curve from the current point.
         *
         * @param c1x the first control point's x coordinate
         * @param c1y the first control point's y coordinate
         * @param c2x the second control point's x coordinate
         * @param c2y the second control point's y coordinate
         * @param x the end's x coordinate
         * @param y the end's y coordinate
         */
        void cubicTo(float c1x, float c1y, float c2x, float c2y, float x, float y);

        /**
         * Closes the current subpath with a straight line back to its start.
         */
        void close();
    }

    /**
     * Builds a {@link Shape}. A builder is not thread safe, and may go on being used after {@link #build()}; later
     * calls do not change shapes already built.
     */
    public static final class Builder {
        private static final double QUARTER = Math.PI / 2.0;

        private byte[] verbs = new byte[16];
        private float[] coords = new float[32];
        private int verbCount;
        private int coordCount;
        private boolean current;
        private boolean closed;
        private float startX;
        private float startY;
        private float minX = Float.POSITIVE_INFINITY;
        private float minY = Float.POSITIVE_INFINITY;
        private float maxX = Float.NEGATIVE_INFINITY;
        private float maxY = Float.NEGATIVE_INFINITY;

        private Builder() {
        }

        /**
         * Starts a new subpath at a point.
         *
         * @param x the x coordinate
         * @param y the y coordinate
         * @return this builder
         * @throws IllegalArgumentException if a coordinate is not finite
         */
        public Builder moveTo(final float x, final float y) {
            Builder.finite(x, y);
            this.verb(MOVE);
            this.point(x, y);
            this.current = true;
            this.closed = false;
            this.startX = x;
            this.startY = y;
            return this;
        }

        /**
         * Adds a straight line from the current point.
         *
         * @param x the end's x coordinate
         * @param y the end's y coordinate
         * @return this builder
         * @throws IllegalArgumentException if a coordinate is not finite
         * @throws IllegalStateException if no subpath was started with {@link #moveTo}
         */
        public Builder lineTo(final float x, final float y) {
            Builder.finite(x, y);
            this.segment(LINE);
            this.point(x, y);
            return this;
        }

        /**
         * Adds a quadratic curve from the current point.
         *
         * @param cx the control point's x coordinate
         * @param cy the control point's y coordinate
         * @param x the end's x coordinate
         * @param y the end's y coordinate
         * @return this builder
         * @throws IllegalArgumentException if a coordinate is not finite
         * @throws IllegalStateException if no subpath was started with {@link #moveTo}
         */
        public Builder quadTo(final float cx, final float cy, final float x, final float y) {
            Builder.finite(cx, cy);
            Builder.finite(x, y);
            this.segment(QUAD);
            this.point(cx, cy);
            this.point(x, y);
            return this;
        }

        /**
         * Adds a cubic curve from the current point.
         *
         * @param c1x the first control point's x coordinate
         * @param c1y the first control point's y coordinate
         * @param c2x the second control point's x coordinate
         * @param c2y the second control point's y coordinate
         * @param x the end's x coordinate
         * @param y the end's y coordinate
         * @return this builder
         * @throws IllegalArgumentException if a coordinate is not finite
         * @throws IllegalStateException if no subpath was started with {@link #moveTo}
         */
        public Builder cubicTo(final float c1x, final float c1y, final float c2x, final float c2y, final float x,
                               final float y) {
            Builder.finite(c1x, c1y);
            Builder.finite(c2x, c2y);
            Builder.finite(x, y);
            this.segment(CUBIC);
            this.point(c1x, c1y);
            this.point(c2x, c2y);
            this.point(x, y);
            return this;
        }

        /**
         * Adds part of the outline of an ellipse. The point at angle {@code a} is
         * {@code (centerX + width / 2 * cos a, centerY + height / 2 * sin a)}, so angle 0 is at the right of the oval
         * and angles turn clockwise on a screen whose y axis points down. With a subpath started, a straight line
         * joins its current point to the start of the arc; without one, the arc starts a subpath. A sweep of more
         * than a full turn either way draws one full turn.
         *
         * @param oval the rectangle the ellipse fills
         * @param startRadians the angle the arc starts at, in radians
         * @param sweepRadians how far the arc turns, in radians; negative turns counterclockwise
         * @return this builder
         * @throws IllegalArgumentException if the oval or an angle is not finite
         */
        public Builder arc(final Rect oval, final float startRadians, final float sweepRadians) {
            if (!Float.isFinite(startRadians) || !Float.isFinite(sweepRadians)) {
                throw new IllegalArgumentException("Arc angles must be finite: " + startRadians + ", " + sweepRadians);
            }
            Builder.finite(oval.x(), oval.y());
            Builder.finite(oval.width(), oval.height());
            final double rx = oval.width() / 2.0;
            final double ry = oval.height() / 2.0;
            final double cx = oval.x() + rx;
            final double cy = oval.y() + ry;
            final double sweep = Math.max(-2.0 * Math.PI, Math.min(2.0 * Math.PI, sweepRadians));
            double angle = startRadians;
            final float x0 = (float) (cx + rx * Math.cos(angle));
            final float y0 = (float) (cy + ry * Math.sin(angle));
            if (this.current) {
                this.lineTo(x0, y0);
            } else {
                this.moveTo(x0, y0);
            }
            // Angles given as floats miss a quarter turn by a little, which must not make a piece of its own
            final int pieces = (int) Math.ceil(Math.abs(sweep) / QUARTER - 1e-6);
            if (pieces == 0) {
                return this;
            }
            final double step = sweep / pieces;
            // Each piece of at most a quarter turn is one cubic, its handles 4/3 tan(step / 4) of the tangent long
            final double k = 4.0 / 3.0 * Math.tan(step / 4.0);
            for (int i = 0; i < pieces; i++) {
                final double next = angle + step;
                final double cos0 = Math.cos(angle);
                final double sin0 = Math.sin(angle);
                final double cos1 = Math.cos(next);
                final double sin1 = Math.sin(next);
                this.cubicTo(
                        (float) (cx + rx * (cos0 - k * sin0)), (float) (cy + ry * (sin0 + k * cos0)),
                        (float) (cx + rx * (cos1 + k * sin1)), (float) (cy + ry * (sin1 - k * cos1)),
                        (float) (cx + rx * cos1), (float) (cy + ry * sin1));
                angle = next;
            }
            return this;
        }

        /**
         * Closes the current subpath with a straight line back to its start. A segment added next, without a
         * {@link #moveTo}, starts a new subpath at the same start. Does nothing without a subpath, or when the
         * subpath is already closed.
         *
         * @return this builder
         */
        public Builder close() {
            if (this.current && !this.closed) {
                this.verb(CLOSE);
                this.closed = true;
            }
            return this;
        }

        /**
         * Builds the shape from everything added so far.
         *
         * @return the shape
         */
        public Shape build() {
            final Rect bounds = this.verbCount == 0 ? Rect.ZERO
                    : Rect.ofEdges(this.minX, this.minY, this.maxX, this.maxY);
            return new Shape(Arrays.copyOf(this.verbs, this.verbCount), Arrays.copyOf(this.coords, this.coordCount),
                    bounds);
        }

        private void segment(final byte verb) {
            if (!this.current) {
                throw new IllegalStateException("Start a subpath with moveTo before adding segments");
            }
            if (this.closed) {
                this.verb(MOVE);
                this.point(this.startX, this.startY);
                this.closed = false;
            }
            this.verb(verb);
        }

        private void verb(final byte verb) {
            if (this.verbCount == this.verbs.length) {
                this.verbs = Arrays.copyOf(this.verbs, this.verbs.length * 2);
            }
            this.verbs[this.verbCount++] = verb;
        }

        private static void finite(final float x, final float y) {
            if (!Float.isFinite(x) || !Float.isFinite(y)) {
                throw new IllegalArgumentException("Shape coordinates must be finite: " + x + ", " + y);
            }
        }

        private void point(final float x, final float y) {
            if (this.coordCount + 2 > this.coords.length) {
                this.coords = Arrays.copyOf(this.coords, this.coords.length * 2);
            }
            this.coords[this.coordCount++] = x;
            this.coords[this.coordCount++] = y;
            this.minX = Math.min(this.minX, x);
            this.minY = Math.min(this.minY, y);
            this.maxX = Math.max(this.maxX, x);
            this.maxY = Math.max(this.maxY, y);
        }
    }
}
