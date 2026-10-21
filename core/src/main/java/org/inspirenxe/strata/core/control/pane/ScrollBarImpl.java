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
package org.inspirenxe.strata.core.control.pane;

import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.control.pane.ScrollBar;
import org.inspirenxe.strata.api.event.input.DragEvent;
import org.inspirenxe.strata.api.event.input.KeyEvent;
import org.inspirenxe.strata.api.event.input.PointerButtonEvent;
import org.inspirenxe.strata.api.event.input.PointerEvent;
import org.inspirenxe.strata.api.event.input.ScrollEvent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.api.layout.ScrollPanel;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.core.layout.ScrollTrack;
import org.inspirenxe.strata.core.node.BuiltInComponent;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;

import java.util.Objects;
import java.util.Set;

/**
 * The core {@link ScrollBar}. It measures its thumb with the same {@link ScrollTrack} the scroll panel, the list
 * and the text area use, so a standalone bar and theirs look alike. The thumb's rectangle is worked out again only
 * when the bounds, the range, the visible amount or the offset changed, so painting allocates nothing.
 */
public final class ScrollBarImpl extends BuiltInComponent<ScrollBar> implements ScrollBar {
    /**
     * The desired length along the bar, in UI units.
     */
    public static final float DEFAULT_LENGTH = 100f;
    private static final Set<String> REQUIRED = Set.of(STYLE_THUMB);

    private double value;
    private double min;
    private double max;
    private double visible;
    private double rate;
    private Orientation orientation;
    private boolean slim;

    // Geometry, worked out again when stale
    private ScrollTrack track;
    private Rect measuredBounds;
    private Rect thumbRect;
    private boolean stale;

    // Pointer
    private boolean dragging;
    private float dragGrab;
    private boolean thumbHovered;

    // Theme
    private Paint trackPaint;
    private Paint thumbPaint;
    private Paint thumbHoveredPaint;
    private Paint thumbDisabledPaint;
    private Paint slimTrackPaint;
    private Paint slimThumbPaint;
    private Paint slimThumbHoveredPaint;
    private Paint slimThumbDisabledPaint;

    /**
     * Creates a vertical bar with nothing to scroll.
     *
     * @param ui the factory that owns the bar
     */
    @SuppressWarnings("this-escape")
    public ScrollBarImpl(final ComponentFactoryImpl ui) {
        super(ui);
        // The content a bar scrolls usually takes the keys, so the bar takes focus only when asked to
        this.setFocusable(false);
        this.rate = ScrollPanel.DEFAULT_SCROLL_RATE;
        this.orientation = Orientation.VERTICAL;
        this.track = this.newTrack();
        this.measuredBounds = Rect.ZERO;
        this.thumbRect = Rect.ZERO;
        this.stale = true;
        this.trackPaint = Paint.NONE;
        this.thumbPaint = Paint.NONE;
        this.thumbHoveredPaint = Paint.NONE;
        this.thumbDisabledPaint = Paint.NONE;
        this.slimTrackPaint = Paint.NONE;
        this.slimThumbPaint = Paint.NONE;
        this.slimThumbHoveredPaint = Paint.NONE;
        this.slimThumbDisabledPaint = Paint.NONE;
        this.setCursor(CursorKind.HAND);
    }

    // ------------------------------------------------------------------
    // Values
    // ------------------------------------------------------------------

    @Override
    public double value() {
        return this.value;
    }

    @Override
    public ScrollBar setValue(final double value) {
        if (Double.isNaN(value)) {
            throw new IllegalArgumentException("Value must not be NaN");
        }
        this.apply(clamp(value, this.min, this.max));
        return this;
    }

    @Override
    public double min() {
        return this.min;
    }

    @Override
    public double max() {
        return this.max;
    }

    @Override
    public ScrollBar setRange(final double min, final double max) {
        if (!Double.isFinite(min) || !Double.isFinite(max) || min > max) {
            throw new IllegalArgumentException("Range must be finite with max not below min: " + min + " to " + max);
        }
        this.min = min;
        this.max = max;
        this.stale = true;
        this.apply(clamp(this.value, min, max));
        return this;
    }

    @Override
    public double visibleAmount() {
        return this.visible;
    }

    @Override
    public ScrollBar setVisibleAmount(final double visibleAmount) {
        if (!(visibleAmount >= 0d) || Double.isInfinite(visibleAmount)) {
            throw new IllegalArgumentException("Visible amount must be finite and not negative: " + visibleAmount);
        }
        this.visible = visibleAmount;
        this.stale = true;
        return this;
    }

    @Override
    public ScrollBar setExtent(final double viewport, final double extent) {
        if (!(viewport >= 0d) || Double.isInfinite(viewport) || !(extent >= 0d) || Double.isInfinite(extent)) {
            throw new IllegalArgumentException("Lengths must be finite and not negative: " + viewport + ", " + extent);
        }
        this.setVisibleAmount(viewport);
        return this.setRange(0d, Math.max(0d, extent - viewport));
    }

    @Override
    public double scrollRate() {
        return this.rate;
    }

    @Override
    public ScrollBar setScrollRate(final double rate) {
        if (!(rate > 0d) || Double.isInfinite(rate)) {
            throw new IllegalArgumentException("Scroll rate must be finite and greater than 0: " + rate);
        }
        this.rate = rate;
        return this;
    }

    @Override
    public Orientation orientation() {
        return this.orientation;
    }

    @Override
    public ScrollBar setOrientation(final Orientation orientation) {
        this.orientation = Objects.requireNonNull(orientation, "orientation");
        this.track = this.newTrack();
        this.stale = true;
        this.invalidateLayout();
        return this;
    }

    @Override
    public boolean isSlim() {
        return this.slim;
    }

    @Override
    public ScrollBar setSlim(final boolean slim) {
        this.slim = slim;
        this.track = this.newTrack();
        this.stale = true;
        this.invalidateLayout();
        return this;
    }

    private ScrollTrack newTrack() {
        return new ScrollTrack(this.orientation == Orientation.VERTICAL,
                this.slim ? SLIM_MIN_THUMB_LENGTH : ScrollPanel.MIN_THUMB_LENGTH);
    }

    private void apply(final double next) {
        if (next == this.value) {
            return;
        }
        final double previous = this.value;
        this.value = next;
        this.stale = true;
        this.report(CHANGED, next, previous);
    }

    /**
     * Applies an offset the user asked for, clamped. One that differs from the current offset fires
     * {@link ScrollBar#CHANGE} first, and the final value goes through {@code setValue}.
     *
     * @return true if the offset changed
     */
    private boolean change(final double requested) {
        final double clamped = clamp(requested, this.min, this.max);
        if (clamped == this.value) {
            return false;
        }
        final double before = this.value;
        this.request(CHANGE, clamped, this::setValue);
        return this.value != before;
    }

    private boolean scrollable() {
        return this.max > this.min;
    }

    private double page() {
        return Math.max(this.rate, this.visible - this.rate);
    }

    /**
     * Brings the track and the thumb up to date with the bounds and the values.
     */
    private Rect thumb() {
        final Rect bounds = this.bounds();
        if (this.stale || !bounds.equals(this.measuredBounds)) {
            final double range = this.max - this.min;
            this.track.update(bounds, (float) this.visible, (float) (range + this.visible));
            this.thumbRect = this.track.thumb((float) (this.value - this.min), (float) range);
            this.measuredBounds = bounds;
            this.stale = false;
        }
        return this.thumbRect;
    }

    private float along(final float x, final float y) {
        return this.orientation == Orientation.VERTICAL ? y : x;
    }

    // ------------------------------------------------------------------
    // Theme, layout and drawing
    // ------------------------------------------------------------------

    @Override
    protected Set<String> requiredStyleKeys() {
        return REQUIRED;
    }

    @Override
    protected void onApplyTheme(final Theme theme) {
        this.trackPaint = theme.paintOr(STYLE_TRACK, Paint.NONE);
        this.thumbPaint = theme.paintOr(STYLE_THUMB, Paint.NONE);
        this.thumbHoveredPaint = theme.paintOr(STYLE_THUMB_HOVERED, this.thumbPaint);
        this.thumbDisabledPaint = theme.paintOr(STYLE_THUMB_DISABLED, this.thumbPaint);
        this.slimTrackPaint = theme.paintOr(STYLE_SLIM_TRACK, this.trackPaint);
        this.slimThumbPaint = theme.paintOr(STYLE_SLIM_THUMB, this.thumbPaint);
        this.slimThumbHoveredPaint = theme.paintOr(STYLE_SLIM_THUMB_HOVERED, this.slimThumbPaint);
        this.slimThumbDisabledPaint = theme.paintOr(STYLE_SLIM_THUMB_DISABLED, this.slimThumbPaint);
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        final float thickness = this.slim ? SLIM_THICKNESS : THICKNESS;
        return this.orientation == Orientation.VERTICAL
                ? new Size(thickness, DEFAULT_LENGTH)
                : new Size(DEFAULT_LENGTH, thickness);
    }

    @Override
    protected void paint(final Painter painter, final Rect bounds) {
        final Rect thumb = this.thumb();
        (this.slim ? this.slimTrackPaint : this.trackPaint).paint(painter, bounds);
        final Paint face;
        if (!this.isEnabled() || !this.scrollable()) {
            face = this.slim ? this.slimThumbDisabledPaint : this.thumbDisabledPaint;
        } else if (this.thumbHovered || this.dragging) {
            face = this.slim ? this.slimThumbHoveredPaint : this.thumbHoveredPaint;
        } else {
            face = this.slim ? this.slimThumbPaint : this.thumbPaint;
        }
        face.paint(painter, thumb);
    }

    // ------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------

    @Override
    protected void onPointerPress(final PointerButtonEvent event) {
        this.dragging = false;
        if (event.button() != MouseButton.LEFT || !this.isEnabled()) {
            return;
        }
        event.consume();
        if (!this.scrollable()) {
            return;
        }
        final Rect thumb = this.thumb();
        final float along = this.along(event.x(), event.y());
        final float thumbStart = this.along(thumb.x(), thumb.y());
        if (thumb.contains(event.x(), event.y())) {
            this.dragging = true;
            this.dragGrab = along - thumbStart;
        } else {
            this.change(this.value + (along < thumbStart ? -this.page() : this.page()));
        }
    }

    @Override
    protected void onDragStart(final DragEvent event) {
        this.onDrag(event);
    }

    @Override
    protected void onDrag(final DragEvent event) {
        if (this.dragging && event.button() == MouseButton.LEFT) {
            this.thumb();
            final float start = this.along(event.x(), event.y()) - this.dragGrab;
            this.change(this.min + this.track.offsetForThumbAt(start, (float) (this.max - this.min)));
            event.consume();
        }
    }

    @Override
    protected void onDragEnd(final DragEvent event) {
        if (this.dragging) {
            this.dragging = false;
            event.consume();
        }
    }

    @Override
    protected void onPointerRelease(final PointerButtonEvent event) {
        if (event.button() == MouseButton.LEFT && event.target() == this) {
            this.dragging = false;
        }
    }

    @Override
    protected void onPointerMove(final PointerEvent event) {
        this.thumbHovered = event.target() == this && this.thumb().contains(event.x(), event.y());
    }

    @Override
    protected void onPointerExit(final PointerEvent event) {
        this.thumbHovered = false;
    }

    @Override
    protected void onScroll(final ScrollEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        float amount = this.orientation == Orientation.VERTICAL ? event.scrollY() : event.scrollX();
        if (this.orientation == Orientation.HORIZONTAL && amount == 0f) {
            amount = event.scrollY();
        }
        if (amount != 0f && this.change(this.value - amount * this.rate)) {
            event.consume();
        }
    }

    @Override
    protected void onKey(final KeyEvent event) {
        if (!event.action().isDown() || !this.isEnabled()) {
            return;
        }
        final boolean vertical = this.orientation == Orientation.VERTICAL;
        final double next = switch (event.key()) {
            case UP -> vertical ? this.value - this.rate : Double.NaN;
            case DOWN -> vertical ? this.value + this.rate : Double.NaN;
            case LEFT -> vertical ? Double.NaN : this.value - this.rate;
            case RIGHT -> vertical ? Double.NaN : this.value + this.rate;
            case PAGE_UP -> this.value - this.page();
            case PAGE_DOWN -> this.value + this.page();
            case HOME -> this.min;
            case END -> this.max;
            default -> Double.NaN;
        };
        if (!Double.isNaN(next)) {
            this.change(next);
            event.consume();
        }
    }

    @Override
    protected Object snapshotState() {
        return new ScrollBar.State(this.value, this.min, this.max, this.visible);
    }

    /**
     * Does what {@code Math.clamp(double, double, double)} does, which Java 17 lacks.
     */
    private static double clamp(final double value, final double min, final double max) {
        if (!(min <= max)) {
            throw new IllegalArgumentException(min + " > " + max);
        }
        return Math.min(max, Math.max(value, min));
    }
}
