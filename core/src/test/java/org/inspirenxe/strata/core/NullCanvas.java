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
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.visual.Visual;
import org.inspirenxe.strata.spi.Canvas;

import java.util.ArrayList;
import java.util.List;

/**
 * A canvas that records fills and text, and checks clip and transform pushes stay balanced.
 */
public final class NullCanvas implements Canvas {
    private final List<Rect> fills;
    private final List<TextSpan> texts;
    private int clipDepth;
    private int transformDepth;

    public NullCanvas() {
        this.fills = new ArrayList<>();
        this.texts = new ArrayList<>();
    }

    public List<TextSpan> texts() {
        return this.texts;
    }

    public List<Rect> fills() {
        return this.fills;
    }

    public int clipDepth() {
        return this.clipDepth;
    }

    @Override
    public void fill(final Rect rect, final int argb) {
        this.fills.add(rect);
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
        this.texts.add(span);
    }

    @Override
    public void pushClip(final Rect rect) {
        this.clipDepth++;
    }

    @Override
    public void popClip() {
        if (--this.clipDepth < 0) {
            throw new IllegalStateException("popClip without pushClip");
        }
    }

    @Override
    public void pushTranslation(final float dx, final float dy) {
        this.transformDepth++;
    }

    @Override
    public void pushScale(final float scale) {
        this.transformDepth++;
    }

    @Override
    public void visual(final Visual visual, final Rect rect, final int tint) {
    }

    @Override
    public void popTransform() {
        if (--this.transformDepth < 0) {
            throw new IllegalStateException("popTransform without pushTranslation");
        }
    }

    @Override
    public void nextLayer() {
    }

    @Override
    public void requestCursor(final CursorKind cursor) {
    }
}
