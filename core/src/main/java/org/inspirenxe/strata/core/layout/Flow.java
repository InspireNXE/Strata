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

import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;

/**
 * Mirroring for right-to-left flow (design 4.49). Layout and controls work out their parts as for left to right and
 * mirror them inside a box, where each part's left edge moves to where its right edge was, measured from the box's
 * other side. Mirroring twice gives the rectangle back.
 */
public final class Flow {

    private Flow() {
    }

    /**
     * Mirrors a rectangle inside a box.
     *
     * @param rect the rectangle
     * @param box the box to mirror inside; only its left and right edges matter
     * @return the mirrored rectangle, as tall and as wide
     */
    public static Rect mirror(final Rect rect, final Rect box) {
        return new Rect(box.x() + box.right() - rect.right(), rect.y(), rect.width(), rect.height());
    }

    /**
     * Mirrors an x inside a box, for a pointer: the x a left-to-right layout would have at the same place.
     *
     * @param x the x
     * @param box the box
     * @return the mirrored x
     */
    public static float mirrorX(final float x, final Rect box) {
        return box.x() + box.right() - x;
    }

    /**
     * Returns the alignment that puts content at the same end of the line in a direction: left and right swap right
     * to left, so {@link HorizontalAlignment#LEFT} means the start. {@link HorizontalAlignment#STRETCH} counts as
     * {@code LEFT}, as it does for text.
     *
     * @param alignment the alignment, as for left to right
     * @param rightToLeft whether the content runs right to left
     * @return the alignment to draw with
     */
    public static HorizontalAlignment resolve(final HorizontalAlignment alignment, final boolean rightToLeft) {
        if (!rightToLeft) {
            return alignment;
        }
        return switch (alignment) {
            case LEFT, STRETCH -> HorizontalAlignment.RIGHT;
            case RIGHT -> HorizontalAlignment.LEFT;
            case CENTER -> HorizontalAlignment.CENTER;
        };
    }
}
