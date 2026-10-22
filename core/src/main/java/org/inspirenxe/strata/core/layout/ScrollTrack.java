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

import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.layout.ScrollPanel;
import org.inspirenxe.strata.api.theme.Paint;

/**
 * The geometry of one scroll bar: its track, and a thumb sized by the visible fraction of the content and placed by
 * the offset. Used by {@link ScrollPanelImpl}, the list view and the text area, which paint their bars themselves,
 * and by the scroll bar control, so all of them measure alike.
 *
 * <p>The thumb is the track's length times the visible fraction, at least the minimum length and at most the track
 * less 8 units, but never longer than the track. On a track shorter than twice the minimum, the minimum is half the
 * track, so the thumb always has room to travel. When the whole content shows, the thumb fills the track.</p>
 *
 * <p>The last thumb is kept and made again only when the track, the fractions or the offset change, so painting an
 * unchanged bar allocates nothing.</p>
 *
 * <p>It can also follow the pointer for its host: {@link #press} pages or starts a drag of the thumb,
 * {@link #drag} gives the offset for the pointer during the drag, and {@link #release} ends it. A host that paints
 * the thumb hovered while {@link #isDragging()} calls {@code release} on every release of the button, since a press
 * that never moved ends with no drag end.</p>
 */
public final class ScrollTrack {
    private final boolean vertical;
    private final float minThumbLength;
    private Rect track;
    private float visible;
    private float extent;
    // The last thumb and the offsets it was made for; null after an update
    private Rect thumb;
    private float thumbOffset;
    private float thumbMaxOffset;
    private boolean dragging;
    private float grab;

    /**
     * Creates a bar with an empty track and a thumb at least {@link ScrollPanel#MIN_THUMB_LENGTH} long.
     *
     * @param vertical true for a bar that scrolls up and down
     */
    public ScrollTrack(final boolean vertical) {
        this(vertical, ScrollPanel.MIN_THUMB_LENGTH);
    }

    /**
     * Creates a bar with an empty track.
     *
     * @param vertical true for a bar that scrolls up and down
     * @param minThumbLength the shortest the thumb gets, in UI units
     */
    public ScrollTrack(final boolean vertical, final float minThumbLength) {
        this.vertical = vertical;
        this.minThumbLength = minThumbLength;
        this.track = Rect.ZERO;
    }

    /**
     * Sets the track and how much of the content shows.
     *
     * @param track the track, in the host's coordinates
     * @param visible the length of the viewport along the bar
     * @param extent the length of the content along the bar
     */
    public void update(final Rect track, final float visible, final float extent) {
        if (!track.equals(this.track) || visible != this.visible || extent != this.extent) {
            this.track = track;
            this.visible = visible;
            this.extent = extent;
            this.thumb = null;
        }
    }

    /**
     * Returns the track.
     *
     * @return the track, as of the last update
     */
    public Rect track() {
        return this.track;
    }

    private float trackLength() {
        return this.vertical ? this.track.height() : this.track.width();
    }

    private float thumbLength() {
        final float track = this.trackLength();
        if (this.extent <= 0f || this.visible >= this.extent) {
            return track;
        }
        final float least = Math.min(this.minThumbLength, track * 0.5f);
        float length = track * this.visible / this.extent;
        length = Math.max(length, least);
        length = Math.min(length, Math.max(least, track - 8f));
        return Math.min(length, track);
    }

    /**
     * Returns the thumb for an offset. The same rectangle comes back until the track, the fractions or the offsets
     * change.
     *
     * @param offset the offset along the bar
     * @param maxOffset the largest offset
     * @return the thumb, in the host's coordinates
     */
    public Rect thumb(final float offset, final float maxOffset) {
        if (this.thumb != null && offset == this.thumbOffset && maxOffset == this.thumbMaxOffset) {
            return this.thumb;
        }
        final float length = this.thumbLength();
        final float travel = this.trackLength() - length;
        final float along = maxOffset <= 0f ? 0f : travel * Math.max(0f, Math.min(1f, offset / maxOffset));
        this.thumb = this.vertical
                ? new Rect(this.track.x(), this.track.y() + along, this.track.width(), length)
                : new Rect(this.track.x() + along, this.track.y(), length, this.track.height());
        this.thumbOffset = offset;
        this.thumbMaxOffset = maxOffset;
        return this.thumb;
    }

    /**
     * Returns the offset that puts the thumb's start at a point along the bar.
     *
     * @param thumbStart where the thumb should start, in the host's coordinates on the bar's axis
     * @param maxOffset the largest offset
     * @return the offset, not clamped
     */
    public float offsetForThumbAt(final float thumbStart, final float maxOffset) {
        final float travel = this.trackLength() - this.thumbLength();
        if (travel <= 0f) {
            return 0f;
        }
        final float start = this.vertical ? this.track.y() : this.track.x();
        return (thumbStart - start) / travel * maxOffset;
    }

    /**
     * Handles a press at a point, in the host's coordinates. A press on the thumb starts a drag; a press on the track
     * beside the thumb moves one page toward the point. A press anywhere else ends any drag and does nothing more.
     *
     * @param x the pointer's x
     * @param y the pointer's y
     * @param offset the offset along the bar
     * @param maxOffset the largest offset
     * @param page how far one page moves, in the offset's units
     * @return the offset after the press, not clamped: the same offset on the thumb, one page before or after it on
     *     the track; or NaN when the point is not on the track
     */
    public float press(final float x, final float y, final float offset, final float maxOffset, final float page) {
        this.dragging = false;
        if (!this.track.contains(x, y)) {
            return Float.NaN;
        }
        final Rect thumb = this.thumb(offset, maxOffset);
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
        return this.offsetForThumbAt((this.vertical ? y : x) - this.grab, maxOffset);
    }

    /**
     * Ends a drag of the thumb, if one is going on.
     */
    public void release() {
        this.dragging = false;
    }

    /**
     * Draws the track and the thumb.
     *
     * @param painter what to draw with
     * @param offset the offset along the bar
     * @param maxOffset the largest offset
     * @param trackPaint the track's paint
     * @param thumbPaint the thumb's paint
     */
    public void paint(final Painter painter, final float offset, final float maxOffset, final Paint trackPaint,
                      final Paint thumbPaint) {
        trackPaint.paint(painter, this.track);
        thumbPaint.paint(painter, this.thumb(offset, maxOffset));
    }
}
