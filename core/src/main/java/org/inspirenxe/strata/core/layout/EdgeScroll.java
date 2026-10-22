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

import org.inspirenxe.strata.api.layout.ScrollPanel;

/**
 * How far a control scrolls in one frame while a drag that carries a transfer holds the pointer near the edge of its
 * viewport. Within {@link #ZONE} units of an edge, inside the viewport, it scrolls toward that edge by a line times
 * how deep into the zone the pointer is, at least an eighth of a line and at most a whole line, so it goes faster the
 * nearer the edge.
 */
public final class EdgeScroll {
    /**
     * How near an edge of the viewport, in UI units, the pointer starts scrolling.
     */
    public static final float ZONE = ScrollPanel.DRAG_SCROLL_ZONE;

    private EdgeScroll() {
    }

    /**
     * Returns how far to scroll along one axis this frame.
     *
     * @param position the pointer's position on the axis, in the host's coordinates
     * @param start where the viewport starts on the axis
     * @param end where the viewport ends on the axis
     * @param line the distance of one line, such as a wheel notch or a row
     * @param scroll the current offset on the axis
     * @param maxScroll the largest offset on the axis
     * @return a negative distance toward the start, a positive one toward the end, or 0 when the pointer is outside
     *     the viewport, outside both zones, or at an end the control cannot scroll past
     */
    public static float step(final float position, final float start, final float end, final float line,
                             final float scroll, final float maxScroll) {
        if (!(position >= start && position < end) || !(line > 0f)) {
            return 0f;
        }
        final float fromStart = position - start;
        final float fromEnd = end - position;
        if (fromStart < ZONE && fromStart <= fromEnd && scroll > 0f) {
            return -line * EdgeScroll.speed(fromStart);
        }
        if (fromEnd < ZONE && fromEnd < fromStart && scroll < maxScroll) {
            return line * EdgeScroll.speed(fromEnd);
        }
        return 0f;
    }

    private static float speed(final float distance) {
        return Math.max(0.125f, (ZONE - distance) / ZONE);
    }
}
