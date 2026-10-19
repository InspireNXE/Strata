package org.inspirenxe.strata.core.layout;

import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.layout.ScrollPanel;
import org.inspirenxe.strata.api.theme.Paint;

/**
 * The geometry of one scroll bar: its track, and a thumb sized by the visible fraction of the content and placed by
 * the offset. Used by {@link ScrollPanelImpl}, the list view and the text area, which paint their bars themselves,
 * and by the scroll bar control, so all of them measure alike.
 *
 * <p>The thumb is the track's length times the visible fraction, at least the minimum length and at most the track
 * less 8 units, but never longer than the track. When the whole content shows, the thumb fills the track.</p>
 */
public final class ScrollTrack {
    private final boolean vertical;
    private final float minThumbLength;
    private Rect track;
    private float visible;
    private float extent;

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
        this.track = track;
        this.visible = visible;
        this.extent = extent;
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
        float length = track * this.visible / this.extent;
        length = Math.max(length, this.minThumbLength);
        length = Math.min(length, Math.max(this.minThumbLength, track - 8f));
        return Math.min(length, track);
    }

    /**
     * Returns the thumb for an offset.
     *
     * @param offset the offset along the bar
     * @param maxOffset the largest offset
     * @return the thumb, in the host's coordinates
     */
    public Rect thumb(final float offset, final float maxOffset) {
        final float length = this.thumbLength();
        final float travel = this.trackLength() - length;
        final float along = maxOffset <= 0f ? 0f : travel * Math.max(0f, Math.min(1f, offset / maxOffset));
        return this.vertical
                ? new Rect(this.track.x(), this.track.y() + along, this.track.width(), length)
                : new Rect(this.track.x() + along, this.track.y(), length, this.track.height());
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
