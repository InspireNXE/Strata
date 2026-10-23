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
package org.inspirenxe.strata.core;

import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.visual.Visual;
import org.inspirenxe.strata.spi.Canvas;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A canvas that writes each drawing call down as a line of text, so a test can compare what was drawn in order: fills
 * with their color, sprites and visuals with their rectangle, text with its position and color, the transforms and
 * the opacities.
 * Clips, layers and cursor requests are left out. Numbers print without a fraction when they are whole.
 */
public class DrawLog implements Canvas {
    private final List<String> lines = new ArrayList<>();
    private final List<TextStyle> textStyles = new ArrayList<>();

    /**
     * Returns every line written so far.
     *
     * @return the lines, in drawing order
     */
    public List<String> lines() {
        return this.lines;
    }

    /**
     * Returns the lines that start with a prefix.
     *
     * @param prefix such as {@code "text"}
     * @return the matching lines, in drawing order
     */
    public List<String> lines(final String prefix) {
        return this.lines.stream().filter(line -> line.startsWith(prefix)).toList();
    }

    /**
     * Returns the style of every text drawn so far.
     *
     * @return the styles, in drawing order
     */
    public List<TextStyle> textStyles() {
        return this.textStyles;
    }

    /**
     * Writes a rectangle as {@code x,y wxh}.
     *
     * @param rect the rectangle
     * @return the text
     */
    public static String rect(final Rect rect) {
        return DrawLog.number(rect.x()) + "," + DrawLog.number(rect.y()) + " " + DrawLog.number(rect.width()) + "x"
                + DrawLog.number(rect.height());
    }

    private static String number(final float value) {
        return value == Math.rint(value) ? Integer.toString((int) value) : Float.toString(value);
    }

    private static String color(final int argb) {
        return String.format(Locale.ROOT, "#%08X", argb);
    }

    @Override
    public void fill(final Rect rect, final int argb) {
        this.lines.add("fill " + DrawLog.rect(rect) + " " + DrawLog.color(argb));
    }

    @Override
    public void fillGradient(final Rect rect, final int topArgb, final int bottomArgb) {
        this.lines.add("gradient " + DrawLog.rect(rect));
    }

    @Override
    public void sprite(final SpriteId sprite, final Rect rect, final int tint) {
        this.lines.add("sprite " + sprite + " " + DrawLog.rect(rect) + " " + DrawLog.color(tint));
    }

    @Override
    public void nineSlice(final SpriteId sprite, final Rect rect, final Insets border, final int tint) {
        this.lines.add("nineslice " + sprite + " " + DrawLog.rect(rect));
    }

    @Override
    public void text(final TextSpan span, final float x, final float y) {
        this.textStyles.add(span.style());
        this.lines.add("text " + span.text() + " " + DrawLog.number(x) + "," + DrawLog.number(y) + " "
                + DrawLog.color(span.style().color()));
    }

    @Override
    public void pushClip(final Rect rect) {
    }

    @Override
    public void pushRoundClip(final Rect rect, final float radius) {
        this.lines.add("roundclip " + DrawLog.rect(rect) + " " + DrawLog.number(radius));
    }

    @Override
    public void popClip() {
    }

    @Override
    public void pushTranslation(final float dx, final float dy) {
        this.lines.add("translate " + DrawLog.number(dx) + "," + DrawLog.number(dy));
    }

    @Override
    public void pushScale(final float scale) {
        this.lines.add("scale " + DrawLog.number(scale));
    }

    @Override
    public void popTransform() {
        this.lines.add("pop");
    }

    @Override
    public void pushOpacity(final float opacity) {
        this.lines.add("opacity " + DrawLog.number(opacity));
    }

    @Override
    public void popOpacity() {
        this.lines.add("opacity end");
    }

    @Override
    public void visual(final Visual visual, final Rect rect, final int tint) {
        this.lines.add("visual " + visual + " " + DrawLog.rect(rect) + " " + DrawLog.color(tint));
    }

    @Override
    public void nextLayer() {
    }

    @Override
    public void requestCursor(final CursorKind cursor) {
    }
}
