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

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.layout.VerticalAlignment;

/**
 * Lines up the children of one row that are aligned {@link VerticalAlignment#BASELINE}: the shared line is the
 * lowest of their baselines, measured from the top of each child's margin, and a child without a baseline counts its
 * bottom as one. A panel adds each such child, then reads how tall the row must be to hold them and where each one's
 * slot starts. Kept by its panel and reused, so it allocates nothing.
 */
final class BaselineRow {
    private float line;
    private float below;
    private boolean any;

    /**
     * Returns whether a panel that lines up baselines should line up this child.
     *
     * @param child the child
     * @return true if the child is visible and aligned to the baseline
     */
    static boolean aligns(final Component<?> child) {
        return child.isVisible() && child.verticalAlignment() == VerticalAlignment.BASELINE;
    }

    /**
     * Forgets every child added, for the next row.
     */
    void reset() {
        this.line = 0f;
        this.below = 0f;
        this.any = false;
    }

    /**
     * Adds a child.
     *
     * @param baseline the child's baseline below the top of its margin, or NaN for none
     * @param outerHeight the child's height with its margin
     */
    void add(final float baseline, final float outerHeight) {
        final float offset = offset(baseline, outerHeight);
        this.line = Math.max(this.line, offset);
        this.below = Math.max(this.below, outerHeight - offset);
        this.any = true;
    }

    /**
     * Returns where the shared line lies below the top of the row.
     *
     * @return the lowest baseline added, or 0 when none was added
     */
    float line() {
        return this.line;
    }

    /**
     * Returns how tall the row must be for the children added, lined up.
     *
     * @return the height, or 0 when none was added
     */
    float height() {
        return this.any ? this.line + this.below : 0f;
    }

    /**
     * Returns where a child's slot starts so that its baseline meets the line.
     *
     * @param rowTop the top of the row
     * @param baseline the child's baseline below the top of its margin, or NaN for none
     * @param outerHeight the child's height with its margin
     * @return the top of the child's slot
     */
    float top(final float rowTop, final float baseline, final float outerHeight) {
        return rowTop + this.line - offset(baseline, outerHeight);
    }

    private static float offset(final float baseline, final float outerHeight) {
        return Float.isNaN(baseline) ? outerHeight : baseline;
    }
}
