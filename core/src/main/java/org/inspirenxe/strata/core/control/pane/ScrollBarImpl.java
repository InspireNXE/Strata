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

import org.inspirenxe.strata.core.layout.Flow;
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

import java.util.ArrayList;
import java.util.List;
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
    // Where the pointer is along the bar while the track is held, and which way the hold pages
    private float holdAlong;
    private int holdDirection;
    private int holdValueDirection;

    // Arrows: the buttons and their triangles, worked out with the thumb; -1 or 1 for the start or end arrow, 0 for
    // neither
    private boolean arrowsShown;
    private Rect startArrow;
    private Rect endArrow;
    private final List<Rect> startTriangle = new ArrayList<>();
    private final List<Rect> endTriangle = new ArrayList<>();
    private int hoveredArrow;
    private int pressedArrow;
    // The faces of the start and end buttons for idle, hovered and pressed, null without a theme key
    private final Paint[] startFaces = new Paint[3];
    private final Paint[] endFaces = new Paint[3];

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
        this.startArrow = Rect.ZERO;
        this.endArrow = Rect.ZERO;
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
        this.apply(Math.clamp(value, this.min, this.max));
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
        this.apply(Math.clamp(this.value, min, max));
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
        this.theme().ifPresent(this::readArrowFaces);
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

    @Override
    public boolean arrowsShown() {
        return this.arrowsShown;
    }

    @Override
    public ScrollBar setArrowsShown(final boolean shown) {
        this.arrowsShown = shown;
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
        final double clamped = Math.clamp(requested, this.min, this.max);
        if (clamped == this.value) {
            return false;
        }
        final double before = this.value;
        this.request(CHANGE, clamped, value -> {
            // A handler that rewrote the value to null or NaN leaves it as it is
            if (value != null && !value.isNaN()) {
                this.setValue(value);
            }
        });
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
        final boolean rightToLeft = this.isRightToLeft();
        if (this.stale || !bounds.equals(this.measuredBounds) || rightToLeft != this.track.isReversed()) {
            final double range = this.max - this.min;
            // Right to left a horizontal bar starts at its right end
            this.track.setReversed(rightToLeft);
            this.track.update(this.arrowsShown ? this.placeArrows(bounds) : bounds, (float) this.visible,
                    (float) (range + this.visible));
            this.thumbRect = this.track.thumb((float) (this.value - this.min), (float) range);
            this.measuredBounds = bounds;
            this.stale = false;
        }
        return this.thumbRect;
    }

    /**
     * Places the arrow buttons at the bar's ends and their triangles, and returns the track between them.
     */
    private Rect placeArrows(final Rect bounds) {
        final boolean vertical = this.orientation == Orientation.VERTICAL;
        final float thickness = vertical ? bounds.width() : bounds.height();
        final float length = vertical ? bounds.height() : bounds.width();
        final float size = Math.min(thickness, length * 0.5f);
        if (vertical) {
            this.startArrow = new Rect(bounds.x(), bounds.y(), bounds.width(), size);
            this.endArrow = new Rect(bounds.x(), bounds.bottom() - size, bounds.width(), size);
        } else {
            this.startArrow = new Rect(bounds.x(), bounds.y(), size, bounds.height());
            this.endArrow = new Rect(bounds.right() - size, bounds.y(), size, bounds.height());
        }
        triangle(this.startTriangle, this.startArrow, vertical, true);
        triangle(this.endTriangle, this.endArrow, vertical, false);
        return vertical
                ? new Rect(bounds.x(), bounds.y() + size, bounds.width(), Math.max(0f, bounds.height() - size * 2f))
                : new Rect(bounds.x() + size, bounds.y(), Math.max(0f, bounds.width() - size * 2f), bounds.height());
    }

    /**
     * Fills a list with the one-unit lines of a triangle centered in a button, pointing to the start or the end of
     * the bar. Its base is the largest odd number of units not above the button's shorter side less 2, and at least
     * 1, and it is half as long, rounded up, so its lines step out by one unit on each side.
     */
    private static void triangle(final List<Rect> lines, final Rect button, final boolean vertical,
                                 final boolean start) {
        lines.clear();
        final int side = (int) Math.floor(Math.min(button.width(), button.height()));
        int base = Math.max(1, side - 2);
        if (base % 2 == 0) {
            base--;
        }
        final int length = (base + 1) / 2;
        final float left = button.x() + (float) Math.floor((button.width() - (vertical ? base : length)) * 0.5f);
        final float top = button.y() + (float) Math.floor((button.height() - (vertical ? length : base)) * 0.5f);
        for (int row = 0; row < length; row++) {
            // Counted from the start of the bar; a start arrow's tip is there, an end arrow's base
            final int inset = start ? length - 1 - row : row;
            final float span = base - inset * 2f;
            lines.add(vertical
                    ? new Rect(left + inset, top + row, span, 1f)
                    : new Rect(left + row, top + inset, 1f, span));
        }
    }

    /**
     * Returns which arrow a point is on: -1 for the start, 1 for the end, 0 for neither.
     */
    private int arrowAt(final float x, final float y) {
        if (!this.arrowsShown) {
            return 0;
        }
        this.thumb();
        if (this.startArrow.contains(x, y)) {
            return -1;
        }
        return this.endArrow.contains(x, y) ? 1 : 0;
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
        this.readArrowFaces(theme);
    }

    /**
     * Reads the faces of the arrow buttons: for each, the key with its direction's suffix, then the plain key, for
     * idle, hovered and pressed, each state falling back to the one before it.
     */
    private void readArrowFaces(final Theme theme) {
        final boolean vertical = this.orientation == Orientation.VERTICAL;
        readFaces(theme, this.startFaces, vertical ? ".up" : ".left");
        readFaces(theme, this.endFaces, vertical ? ".down" : ".right");
    }

    private static void readFaces(final Theme theme, final Paint[] faces, final String direction) {
        final String[] keys = {STYLE_ARROW, STYLE_ARROW_HOVERED, STYLE_ARROW_PRESSED};
        Paint previous = null;
        for (int i = 0; i < keys.length; i++) {
            final String key = keys[i];
            final Paint face = theme.paint(key.replace("scroll.arrow", "scroll.arrow" + direction))
                    .or(() -> theme.paint(key)).orElse(previous);
            faces[i] = face;
            previous = face;
        }
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
        if (this.arrowsShown) {
            final Paint glyph = this.slim ? this.slimThumbPaint : this.thumbPaint;
            this.paintArrow(painter, this.startArrow, this.startFaces, this.startTriangle, -1, glyph);
            this.paintArrow(painter, this.endArrow, this.endFaces, this.endTriangle, 1, glyph);
        }
    }

    private void paintArrow(final Painter painter, final Rect button, final Paint[] faces, final List<Rect> triangle,
                            final int which, final Paint glyph) {
        final int state = this.pressedArrow == which ? 2 : this.hoveredArrow == which ? 1 : 0;
        if (faces[state] != null) {
            faces[state].paint(painter, button);
            return;
        }
        for (int i = 0; i < triangle.size(); i++) {
            glyph.paint(painter, triangle.get(i));
        }
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
        final int arrow = this.arrowAt(event.x(), event.y());
        if (arrow != 0) {
            this.pressedArrow = arrow;
            // The left arrow of a reversed bar moves toward the larger values
            final int step = this.track.isReversed() ? -arrow : arrow;
            this.change(this.value + step * this.rate);
            this.holdRepeat(() -> this.change(this.value + step * this.rate));
            return;
        }
        final Rect thumb = this.thumb();
        final float along = this.along(event.x(), event.y());
        final float thumbStart = this.along(thumb.x(), thumb.y());
        if (thumb.contains(event.x(), event.y())) {
            this.dragging = true;
            this.dragGrab = along - thumbStart;
        } else {
            this.holdAlong = along;
            this.holdDirection = along < thumbStart ? -1 : 1;
            // A reversed bar's value grows toward its left
            this.holdValueDirection = this.track.isReversed() ? -this.holdDirection : this.holdDirection;
            this.change(this.value + this.holdValueDirection * this.page());
            this.holdRepeat(this::pageAgain);
        }
    }

    private void holdRepeat(final Runnable action) {
        if (this.hostRoot() != null) {
            this.hostRoot().startHoldRepeat(this.node(), action);
        }
    }

    /**
     * Pages once more toward where the pointer holds the track, or stops once the thumb has reached it.
     */
    private void pageAgain() {
        final Rect thumb = this.thumb();
        final float start = this.along(thumb.x(), thumb.y());
        final float end = start + (this.orientation == Orientation.VERTICAL ? thumb.height() : thumb.width());
        final boolean reached = this.holdDirection < 0 ? this.holdAlong >= start : this.holdAlong < end;
        if (reached || !this.change(this.value + this.holdValueDirection * this.page())) {
            this.stopHold();
        }
    }

    private void stopHold() {
        this.holdDirection = 0;
        this.pressedArrow = 0;
        if (this.hostRoot() != null) {
            this.hostRoot().stopHoldRepeat(this.node());
        }
    }

    @Override
    protected void onDragStart(final DragEvent event) {
        this.onDrag(event);
    }

    @Override
    protected void onDrag(final DragEvent event) {
        if (this.holdDirection != 0) {
            this.holdAlong = this.along(event.x(), event.y());
        }
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
            this.stopHold();
        }
    }

    @Override
    protected void onPointerMove(final PointerEvent event) {
        this.thumbHovered = event.target() == this && this.thumb().contains(event.x(), event.y());
        this.hoveredArrow = event.target() == this ? this.arrowAt(event.x(), event.y()) : 0;
    }

    @Override
    protected void onPointerExit(final PointerEvent event) {
        this.thumbHovered = false;
        this.hoveredArrow = 0;
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
        // Right to left, Left moves a horizontal bar toward its end
        final double next = switch (vertical ? event.key() : Flow.key(event.key(), this.isRightToLeft())) {
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
}
