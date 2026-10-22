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
package org.inspirenxe.strata.testing;

import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.visual.Visual;
import org.inspirenxe.strata.spi.Canvas;

import java.util.ArrayList;
import java.util.List;

/**
 * A canvas that draws nothing and writes down the text of every span it is asked to draw, in order.
 */
final class TextRecorder implements Canvas {
    private final List<String> text = new ArrayList<>();

    /**
     * Forgets what was recorded, before a new frame.
     */
    void clear() {
        this.text.clear();
    }

    /**
     * Returns the text recorded since the last {@link #clear()}.
     *
     * @return a copy, in drawing order
     */
    List<String> text() {
        return List.copyOf(this.text);
    }

    @Override
    public void fill(final Rect rect, final int argb) {
    }

    @Override
    public void fillGradient(final Rect rect, final int topArgb, final int bottomArgb) {
    }

    @Override
    public void sprite(final SpriteId sprite, final Rect rect, final int tint) {
    }

    @Override
    public void nineSlice(final SpriteId sprite, final Rect rect, final Insets border, final int tint) {
    }

    @Override
    public void text(final TextSpan span, final float x, final float y) {
        this.text.add(span.text());
    }

    @Override
    public void pushClip(final Rect rect) {
    }

    @Override
    public void popClip() {
    }

    @Override
    public void pushTranslation(final float dx, final float dy) {
    }

    @Override
    public void pushScale(final float scale) {
    }

    @Override
    public void popTransform() {
    }

    @Override
    public void visual(final Visual visual, final Rect rect, final int tint) {
    }

    @Override
    public void nextLayer() {
    }

    @Override
    public void requestCursor(final CursorKind cursor) {
    }
}
