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

import java.util.Objects;

/**
 * How a line or an outline is drawn: its width, the shape of its open ends and the shape of its corners. Make one
 * once and keep it in a field; it is a value.
 *
 * @param width the width in UI units, centered on the path for lines, polylines and shapes; 0 draws nothing
 * @param cap the shape of the ends of an open path
 * @param join the shape of the corners where two segments meet
 */
public record Stroke(float width, Cap cap, Join join) {

    /**
     * Checks the arguments.
     *
     * @throws IllegalArgumentException if the width is negative or not finite
     */
    public Stroke {
        if (!(width >= 0f) || Float.isInfinite(width)) {
            throw new IllegalArgumentException("Stroke width must be finite and not negative: " + width);
        }
        Objects.requireNonNull(cap, "cap");
        Objects.requireNonNull(join, "join");
    }

    /**
     * Returns a stroke of a width with {@link Cap#BUTT} ends and {@link Join#MITER} corners.
     *
     * @param width the width in UI units
     * @return the stroke
     * @throws IllegalArgumentException if the width is negative or not finite
     */
    public static Stroke of(final float width) {
        return new Stroke(width, Cap.BUTT, Join.MITER);
    }

    /**
     * The shape of the ends of an open path.
     */
    public enum Cap {
        /** The stroke stops square at the end point. */
        BUTT,
        /** The stroke ends in a half circle around the end point, half the width across. */
        ROUND,
        /** The stroke goes on past the end point by half its width and stops square. */
        SQUARE
    }

    /**
     * The shape of the corner where two segments of a path meet.
     */
    public enum Join {
        /** The outer edges run on until they meet in a point, or are cut off as {@link #BEVEL} when the point is far. */
        MITER,
        /** The corner is rounded, half the width across. */
        ROUND,
        /** The corner is cut off straight between the ends of the outer edges. */
        BEVEL
    }
}
