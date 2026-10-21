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
package org.inspirenxe.strata.core.control.field;

import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.control.field.ColorPicker;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.event.input.DragEvent;
import org.inspirenxe.strata.api.component.Focusable;
import org.inspirenxe.strata.api.event.input.PointerButtonEvent;
import org.inspirenxe.strata.api.extension.ArrangeContext;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.core.node.BuiltInComposite;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The core {@link ColorPicker}: the square and strips are painted, and the hex field is a private
 * {@link TextBoxImpl} whose internal filter admits only an optional {@code #} and up to eight hex digits. The field's
 * {@link TextBox} events stop at the picker.
 *
 * <p>The square is drawn as one column per unit of its width, each a vertical gradient from the column's saturation
 * at full brightness down to black, which is exact because brightness scales the color linearly. The hue strip is
 * six gradients between the primary and secondary colors, and the alpha strip one gradient over a checkerboard of
 * {@value #CHECKER}-unit cells. Columns, segments, checker cells and markers are worked out during arrange or after a
 * change, never while painting.</p>
 */
public final class ColorPickerImpl extends BuiltInComposite<ColorPicker> implements ColorPicker {
    /**
     * The side of each checkerboard cell behind translucent colors, in UI units.
     */
    public static final float CHECKER = 4f;
    private static final int CHECKER_LIGHT = 0xFFFFFFFF;
    private static final int CHECKER_DARK = 0xFFC8C8C8;
    private static final int[] HUES = {0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF, 0xFF0000FF, 0xFFFF00FF, 0xFFFF0000};
    private static final Paint DEFAULT_MARKER = Paint.solid(0xFFFFFFFF);
    private static final Paint DEFAULT_MARKER_OUTLINE = Paint.solid(0xFF000000);

    private final TextBoxImpl field;
    private int color;
    private int alpha;
    private float hue;
    private float saturation;
    private float brightness;
    private boolean alphaShown;
    private Area dragging;

    // Theme
    private Paint frame;
    private Paint marker;
    private Paint markerOutline;
    private Paint disabledPaint;

    // Computed during arrange
    private Rect square;
    private Rect hueStrip;
    private Rect alphaStrip;
    private Rect swatch;
    private List<Rect> frames;
    private Rect[] columns;
    private int[] columnTops;
    private Rect[] hueSegments;
    private List<Rect> alphaChecker;
    private List<Rect> swatchChecker;

    // Recomputed after a change, before the next paint
    private boolean columnsDirty;
    private boolean markersDirty;
    private final List<Rect> markerOutlines;
    private final List<Rect> markerFills;

    /**
     * Creates an opaque white picker with the alpha strip hidden.
     *
     * @param ui the factory that owns the picker
     */
    @SuppressWarnings("this-escape")
    public ColorPickerImpl(final ComponentFactoryImpl ui) {
        super(ui);
        this.color = 0xFFFFFFFF;
        this.alpha = 0xFF;
        this.brightness = 1f;
        this.dragging = Area.NONE;
        this.frame = Paint.NONE;
        this.marker = DEFAULT_MARKER;
        this.markerOutline = DEFAULT_MARKER_OUTLINE;
        this.disabledPaint = Paint.NONE;
        this.square = Rect.ZERO;
        this.hueStrip = Rect.ZERO;
        this.alphaStrip = Rect.ZERO;
        this.swatch = Rect.ZERO;
        this.frames = List.of();
        this.columns = new Rect[0];
        this.columnTops = new int[0];
        this.hueSegments = new Rect[0];
        this.alphaChecker = List.of();
        this.swatchChecker = List.of();
        this.markerOutlines = new ArrayList<>();
        this.markerFills = new ArrayList<>();
        this.field = new TextBoxImpl(ui);
        this.field.setMaxLength(9);
        this.field.setAnchor(Anchor.FILL);
        this.field.setInternalFilter(text -> text.matches("#?[0-9a-fA-F]{0,8}") ? text : null);
        this.field.onEvent(TextBox.SUBMITTED, event -> this.commitHex());
        this.field.onEvent(Focusable.FOCUS_LOST, event -> this.commitHex());
        this.addChild(this.field);
        this.showHex();
        this.setCursor(CursorKind.CROSSHAIR);
    }

    // ------------------------------------------------------------------
    // ColorPicker
    // ------------------------------------------------------------------

    @Override
    public int color() {
        return this.color;
    }

    @Override
    public ColorPicker setColor(final int argb) {
        if (argb != this.color) {
            final int previous = this.color;
            this.adopt(argb);
            this.report(PICKED, argb, previous);
        }
        return this;
    }

    @Override
    public float hue() {
        return this.hue;
    }

    @Override
    public float saturation() {
        return this.saturation;
    }

    @Override
    public float brightness() {
        return this.brightness;
    }

    @Override
    public boolean isAlphaShown() {
        return this.alphaShown;
    }

    @Override
    public ColorPicker setAlphaShown(final boolean shown) {
        if (shown != this.alphaShown) {
            this.alphaShown = shown;
            this.showHex();
            this.invalidateLayout();
        }
        return this;
    }

    /**
     * Takes a color set from code or by a rewritten request: works out the hue, saturation and brightness, keeping
     * the hue for a gray and the saturation too for black.
     */
    private void adopt(final int argb) {
        this.color = argb;
        this.alpha = argb >>> 24;
        final float red = ((argb >> 16) & 0xFF) / 255f;
        final float green = ((argb >> 8) & 0xFF) / 255f;
        final float blue = (argb & 0xFF) / 255f;
        final float max = Math.max(red, Math.max(green, blue));
        final float delta = max - Math.min(red, Math.min(green, blue));
        this.brightness = max;
        if (max > 0f) {
            this.saturation = delta / max;
        }
        if (delta > 0f) {
            final float hue;
            if (max == red) {
                hue = 60f * ((green - blue) / delta);
            } else if (max == green) {
                hue = 60f * ((blue - red) / delta + 2f);
            } else {
                hue = 60f * ((red - green) / delta + 4f);
            }
            this.setHue(hue < 0f ? hue + 360f : hue);
        }
        this.changed();
    }

    private void setHue(final float hue) {
        if (hue != this.hue) {
            this.hue = hue;
            this.columnsDirty = true;
        }
    }

    /**
     * Marks the drawing out of date and shows the color in the field.
     */
    private void changed() {
        this.markersDirty = true;
        this.showHex();
    }

    /**
     * Changes the color for the user from a hue, saturation, brightness and alpha. A change that leaves the color as
     * it is, such as a new hue for a gray, moves the markers without firing anything. Otherwise it asks with
     * {@code PICK}, and applies exactly these values if the request kept the color it was given.
     */
    private void userPick(final float hue, final float saturation, final float brightness, final int alpha) {
        final int next = (alpha << 24) | rgb(hue, saturation, brightness);
        if (next == this.color) {
            this.setHue(hue);
            this.saturation = saturation;
            this.brightness = brightness;
            this.markersDirty = true;
            return;
        }
        this.request(PICK, next, value -> {
            if (value == null) {
                return;
            }
            if (value != next) {
                this.setColor(value);
                return;
            }
            final int previous = this.color;
            this.color = next;
            this.alpha = alpha;
            this.setHue(hue);
            this.saturation = saturation;
            this.brightness = brightness;
            this.changed();
            this.report(PICKED, next, previous);
        });
    }

    /**
     * Takes the hex code typed into the field, or puts the current color back when it is not six or eight digits.
     */
    private void commitHex() {
        final String text = this.field.text().startsWith("#") ? this.field.text().substring(1) : this.field.text();
        final int parsed;
        if (text.length() == 6) {
            parsed = (this.color & 0xFF000000) | Integer.parseInt(text, 16);
        } else if (text.length() == 8) {
            parsed = Integer.parseUnsignedInt(text, 16);
        } else {
            this.showHex();
            return;
        }
        if (parsed != this.color && this.isEnabled()) {
            this.request(PICK, parsed, value -> {
                if (value != null) {
                    this.setColor(value);
                }
            });
        }
        this.showHex();
    }

    private void showHex() {
        this.field.setText(this.alphaShown
                ? String.format(Locale.ROOT, "#%08X", this.color)
                : String.format(Locale.ROOT, "#%06X", this.color & 0xFFFFFF));
    }

    /**
     * Returns the RGB color for a hue in degrees and a saturation and brightness from 0 to 1, with no alpha.
     */
    static int rgb(final float hue, final float saturation, final float brightness) {
        final float sector = ((hue % 360f) + 360f) % 360f / 60f;
        final int whole = (int) Math.floor(sector);
        final float fraction = sector - whole;
        final float low = brightness * (1f - saturation);
        final float falling = brightness * (1f - saturation * fraction);
        final float rising = brightness * (1f - saturation * (1f - fraction));
        final float red;
        final float green;
        final float blue;
        switch (whole) {
            case 0 -> {
                red = brightness;
                green = rising;
                blue = low;
            }
            case 1 -> {
                red = falling;
                green = brightness;
                blue = low;
            }
            case 2 -> {
                red = low;
                green = brightness;
                blue = rising;
            }
            case 3 -> {
                red = low;
                green = falling;
                blue = brightness;
            }
            case 4 -> {
                red = rising;
                green = low;
                blue = brightness;
            }
            default -> {
                red = brightness;
                green = low;
                blue = falling;
            }
        }
        return (channel(red) << 16) | (channel(green) << 8) | channel(blue);
    }

    private static int channel(final float value) {
        return Math.max(0, Math.min(255, Math.round(value * 255f)));
    }

    private static float fraction(final float value, final float start, final float length) {
        return length <= 0f ? 0f : Math.max(0f, Math.min(1f, (value - start) / length));
    }

    // ------------------------------------------------------------------
    // Theme, layout and painting
    // ------------------------------------------------------------------

    @Override
    protected void onApplyTheme(final Theme theme) {
        this.frame = theme.paintOr(STYLE_FRAME, Paint.NONE);
        this.marker = theme.paintOr(STYLE_MARKER, DEFAULT_MARKER);
        this.markerOutline = theme.paintOr(STYLE_MARKER_OUTLINE, DEFAULT_MARKER_OUTLINE);
        this.disabledPaint = theme.paintOr(STYLE_DISABLED, Paint.NONE);
    }

    private float stripsWidth() {
        return (SPACING + STRIP_WIDTH) * (this.alphaShown ? 2f : 1f);
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        final float width = DEFAULT_SQUARE_SIZE + this.stripsWidth();
        final float fieldHeight = context.measure(this.field, width, availableHeight).height();
        return new Size(width, DEFAULT_SQUARE_SIZE + SPACING + fieldHeight);
    }

    @Override
    protected void arrange(final ArrangeContext context, final Rect bounds) {
        final float fieldHeight = this.field.desiredHeight();
        final float rowY = bounds.bottom() - fieldHeight;
        this.square = new Rect(bounds.x(), bounds.y(), Math.max(0f, bounds.width() - this.stripsWidth()),
                Math.max(0f, bounds.height() - SPACING - fieldHeight));
        this.hueStrip = new Rect(this.square.right() + SPACING, bounds.y(), STRIP_WIDTH, this.square.height());
        this.alphaStrip = this.alphaShown
                ? new Rect(this.hueStrip.right() + SPACING, bounds.y(), STRIP_WIDTH, this.square.height())
                : Rect.ZERO;
        this.swatch = new Rect(bounds.x(), rowY, fieldHeight, fieldHeight);
        final float fieldX = this.swatch.right() + SPACING;
        context.arrange(this.field, new Rect(fieldX, rowY, Math.max(0f, bounds.right() - fieldX), fieldHeight));

        final List<Rect> frames = new ArrayList<>();
        for (final Rect area : List.of(this.square, this.hueStrip, this.alphaStrip, this.swatch)) {
            if (!area.isEmpty()) {
                frames.add(area.grow(Insets.of(1f)));
            }
        }
        this.frames = frames;

        final int count = Math.max(1, (int) Math.ceil(this.square.width()));
        final float columnWidth = this.square.width() / count;
        this.columns = new Rect[count];
        for (int i = 0; i < count; i++) {
            final float x = this.square.x() + i * columnWidth;
            this.columns[i] = new Rect(x, this.square.y(), Math.min(columnWidth + 0.5f, this.square.right() - x),
                    this.square.height());
        }
        this.columnTops = new int[count];
        this.columnsDirty = true;

        this.hueSegments = new Rect[HUES.length - 1];
        final float segment = this.hueStrip.height() / this.hueSegments.length;
        for (int i = 0; i < this.hueSegments.length; i++) {
            final float top = this.hueStrip.y() + i * segment;
            final float bottom = i == this.hueSegments.length - 1 ? this.hueStrip.bottom() : top + segment;
            this.hueSegments[i] = new Rect(this.hueStrip.x(), top, this.hueStrip.width(), bottom - top);
        }
        this.alphaChecker = checker(this.alphaStrip);
        this.swatchChecker = checker(this.swatch);
        this.markersDirty = true;
    }

    /**
     * The dark cells of a checkerboard over an area, clipped to it.
     */
    private static List<Rect> checker(final Rect area) {
        final List<Rect> cells = new ArrayList<>();
        int row = 0;
        for (float y = area.y(); y < area.bottom(); y += CHECKER, row++) {
            int column = 0;
            for (float x = area.x(); x < area.right(); x += CHECKER, column++) {
                if ((row + column) % 2 == 1) {
                    cells.add(new Rect(x, y, Math.min(CHECKER, area.right() - x), Math.min(CHECKER, area.bottom() - y)));
                }
            }
        }
        return cells;
    }

    /**
     * Works out the markers for the current values: three rings in the square, an outline, the marker and another
     * outline from the outside in, and a bar across each strip with an outline around it.
     */
    private void updateMarkers() {
        this.markersDirty = false;
        this.markerOutlines.clear();
        this.markerFills.clear();
        final float x = Math.round(this.square.x() + this.saturation * this.square.width());
        final float y = Math.round(this.square.y() + (1f - this.brightness) * this.square.height());
        this.ring(this.markerOutlines, x, y, 3f);
        this.ring(this.markerFills, x, y, 2f);
        this.ring(this.markerOutlines, x, y, 1f);
        this.bar(this.hueStrip, this.hue / 360f);
        if (this.alphaShown) {
            this.bar(this.alphaStrip, 1f - this.alpha / 255f);
        }
    }

    private void ring(final List<Rect> into, final float x, final float y, final float radius) {
        final float side = radius * 2f + 1f;
        into.add(new Rect(x - radius, y - radius, side, 1f));
        into.add(new Rect(x - radius, y + radius, side, 1f));
        into.add(new Rect(x - radius, y - radius + 1f, 1f, side - 2f));
        into.add(new Rect(x + radius, y - radius + 1f, 1f, side - 2f));
    }

    private void bar(final Rect strip, final float fraction) {
        final float y = Math.round(strip.y() + fraction * Math.max(0f, strip.height() - 1f));
        this.markerOutlines.add(new Rect(strip.x() - 1f, y - 1f, strip.width() + 2f, 3f));
        this.markerFills.add(new Rect(strip.x(), y, strip.width(), 1f));
    }

    @Override
    protected void paint(final Painter painter, final Rect bounds) {
        if (this.field.isEnabled() != this.isEnabled()) {
            this.field.setEnabled(this.isEnabled());
        }
        if (this.columnsDirty) {
            this.columnsDirty = false;
            for (int i = 0; i < this.columns.length; i++) {
                this.columnTops[i] = 0xFF000000 | rgb(this.hue, (i + 0.5f) / this.columns.length, 1f);
            }
        }
        if (this.markersDirty) {
            this.updateMarkers();
        }
        for (final Rect area : this.frames) {
            this.frame.paint(painter, area);
        }
        for (int i = 0; i < this.columns.length; i++) {
            painter.fillGradient(this.columns[i], this.columnTops[i], 0xFF000000);
        }
        for (int i = 0; i < this.hueSegments.length; i++) {
            painter.fillGradient(this.hueSegments[i], HUES[i], HUES[i + 1]);
        }
        final int rgb = this.color & 0xFFFFFF;
        if (this.alphaShown) {
            this.paintChecker(painter, this.alphaStrip, this.alphaChecker);
            painter.fillGradient(this.alphaStrip, 0xFF000000 | rgb, rgb);
        }
        this.paintChecker(painter, this.swatch, this.swatchChecker);
        painter.fill(this.swatch, this.color);
        if (!this.isEnabled()) {
            this.disabledPaint.paint(painter, this.square);
            this.disabledPaint.paint(painter, this.hueStrip);
            if (this.alphaShown) {
                this.disabledPaint.paint(painter, this.alphaStrip);
            }
        }
        for (final Rect rect : this.markerOutlines) {
            this.markerOutline.paint(painter, rect);
        }
        for (final Rect rect : this.markerFills) {
            this.marker.paint(painter, rect);
        }
    }

    private void paintChecker(final Painter painter, final Rect area, final List<Rect> cells) {
        painter.fill(area, CHECKER_LIGHT);
        for (final Rect cell : cells) {
            painter.fill(cell, CHECKER_DARK);
        }
    }

    // ------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------

    @Override
    protected void onPointerPress(final PointerButtonEvent event) {
        this.dragging = Area.NONE;
        if (event.button() != MouseButton.LEFT || !this.isEnabled()) {
            return;
        }
        if (this.square.contains(event.x(), event.y())) {
            this.dragging = Area.SQUARE;
        } else if (this.hueStrip.contains(event.x(), event.y())) {
            this.dragging = Area.HUE;
        } else if (this.alphaShown && this.alphaStrip.contains(event.x(), event.y())) {
            this.dragging = Area.ALPHA;
        } else {
            return;
        }
        this.pointAt(event.x(), event.y());
        event.consume();
    }

    @Override
    protected void onPointerRelease(final PointerButtonEvent event) {
        if (event.button() == MouseButton.LEFT) {
            this.dragging = Area.NONE;
        }
    }

    @Override
    protected void onDragStart(final DragEvent event) {
        this.onDrag(event);
    }

    @Override
    protected void onDrag(final DragEvent event) {
        if (this.dragging != Area.NONE && event.button() == MouseButton.LEFT && this.isEnabled()) {
            this.pointAt(event.x(), event.y());
            event.consume();
        }
    }

    @Override
    protected void onDragEnd(final DragEvent event) {
        this.dragging = Area.NONE;
    }

    /**
     * Changes the value of the area being dragged to the one under a point, clamped to the area.
     */
    private void pointAt(final float x, final float y) {
        switch (this.dragging) {
            case SQUARE -> this.userPick(this.hue,
                    fraction(x, this.square.x(), this.square.width()),
                    1f - fraction(y, this.square.y(), this.square.height()),
                    this.alpha);
            case HUE -> this.userPick(fraction(y, this.hueStrip.y(), this.hueStrip.height()) * 360f,
                    this.saturation, this.brightness, this.alpha);
            case ALPHA -> this.userPick(this.hue, this.saturation, this.brightness,
                    Math.round((1f - fraction(y, this.alphaStrip.y(), this.alphaStrip.height())) * 255f));
            default -> { }
        }
    }

    @Override
    protected Object snapshotState() {
        return new ColorPicker.State(this.color, this.hue, this.saturation, this.brightness, this.alphaShown);
    }

    /**
     * The part of the picker a drag changes.
     */
    private enum Area {
        NONE,
        SQUARE,
        HUE,
        ALPHA
    }
}
