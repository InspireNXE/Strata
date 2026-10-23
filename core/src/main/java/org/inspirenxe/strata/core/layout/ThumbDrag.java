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

/**
 * Follows the pointer on one {@link ScrollTrack} for the component that paints it: {@link #press} pages or starts a
 * drag of the thumb, {@link #drag} gives the offset for the pointer during the drag, and {@link #release} ends it.
 * A host that paints the thumb hovered while {@link #isDragging()} calls {@code release} on every release of the
 * button, since a press that never moved ends with no drag end.
 */
public final class ThumbDrag {
    private final ScrollTrack bar;
    private final boolean vertical;
    private boolean dragging;
    private float grab;

    /**
     * Creates a helper for a bar.
     *
     * @param bar the bar it follows the pointer on
     * @param vertical true if the bar scrolls up and down, as it was made
     */
    public ThumbDrag(final ScrollTrack bar, final boolean vertical) {
        this.bar = bar;
        this.vertical = vertical;
    }

    /**
     * Handles a press at a point, in the host's coordinates. A press on the thumb starts a drag; a press on the track
     * beside the thumb moves one page toward the point. Any press first ends a drag that is going on.
     *
     * @param x the pointer's x
     * @param y the pointer's y
     * @param offset the offset along the bar
     * @param maxOffset the largest offset
     * @param page how far one page moves, in the offset's units
     * @return the offset after the press, not clamped: the same offset on the thumb, or one page before or after it
     *     on the track; NaN when the point is not on the track
     */
    public float press(final float x, final float y, final float offset, final float maxOffset, final float page) {
        this.dragging = false;
        if (!this.bar.track().contains(x, y)) {
            return Float.NaN;
        }
        final Rect thumb = this.bar.thumb(offset, maxOffset);
        final float along = this.vertical ? y : x;
        final float thumbStart = this.vertical ? thumb.y() : thumb.x();
        if (thumb.contains(x, y)) {
            this.dragging = true;
            this.grab = along - thumbStart;
            return offset;
        }
        return offset + (along < thumbStart ? -page : page);
    }

    /**
     * Returns whether a press on the thumb started a drag that has not been released.
     *
     * @return true while the thumb is dragged
     */
    public boolean isDragging() {
        return this.dragging;
    }

    /**
     * Returns the offset that keeps the point of the thumb that was pressed under the pointer.
     *
     * @param x the pointer's x, in the host's coordinates
     * @param y the pointer's y, in the host's coordinates
     * @param maxOffset the largest offset
     * @return the offset, not clamped; meaningful only while {@link #isDragging()}
     */
    public float drag(final float x, final float y, final float maxOffset) {
        return this.bar.offsetForThumbAt((this.vertical ? y : x) - this.grab, maxOffset);
    }

    /**
     * Ends a drag of the thumb, if one is going on.
     */
    public void release() {
        this.dragging = false;
    }
}
