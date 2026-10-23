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

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.visual.Visual;
import org.inspirenxe.strata.spi.Canvas;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * How a button cuts a caption too wide for it, with and without an icon. Text in the test platform is 6 units per
 * code point and 9 per line, and a button pads by 8 at the sides and 5 at the top and bottom. Each log starts with
 * the clip of the panel around the button, which is the button's size.
 */
class CaptionClipTest {
    private static final SpriteId ICON = SpriteId.of("test", "icon");

    private final TestHost test = new TestHost(300, 100);
    private final ComponentFactory ui = this.test.factory();

    private List<String> render(final Button button) {
        this.test.show(this.ui.absolutePanel().add(button, 0, 0));
        final ClipLog canvas = new ClipLog();
        this.test.host().render(canvas);
        return canvas.lines;
    }

    @Test
    void aCaptionTooWideBesideAnIconIsCutAfterTheIcon() {
        final Button button = this.ui.button("A long caption").setIcon(ICON).setWidth(60);
        // The clip area is 8 to 52; the icon takes 8 to 24 and the gap 4 more, so the caption gets 28 to 52
        assertEquals(List.of("clip 0,0 60x26", "sprite 8,5", "clip 28,0 24x26", "text A long caption 28,8.5",
                "pop", "pop"),
                this.render(button));
    }

    @Test
    void aCaptionThatFitsBesideAnIconIsNotCut() {
        final Button button = this.ui.button("OK").setIcon(ICON);
        assertEquals(List.of("clip 0,0 48x26", "sprite 8,5", "text OK 28,8.5", "pop"), this.render(button));
    }

    @Test
    void aCaptionTooWideWithoutAnIconIsCutAtThePadding() {
        final Button button = this.ui.button("A long caption").setWidth(60);
        assertEquals(List.of("clip 0,0 60x20", "clip 8,0 44x20", "text A long caption 8,5.5", "pop", "pop"), this.render(button));
    }

    /**
     * Logs sprites, clips and text, and nothing else.
     */
    private static final class ClipLog implements Canvas {
        private final List<String> lines = new ArrayList<>();

        private static String number(final float value) {
            return value == Math.rint(value) ? Integer.toString((int) value) : Float.toString(value);
        }

        @Override
        public void fill(final Rect rect, final int argb) {
        }

        @Override
        public void fillGradient(final Rect rect, final int topArgb, final int bottomArgb) {
        }

        @Override
        public void sprite(final SpriteId sprite, final Rect rect, final int tint) {
            this.lines.add("sprite " + ClipLog.number(rect.x()) + "," + ClipLog.number(rect.y()));
        }

        @Override
        public void nineSlice(final SpriteId sprite, final Rect rect, final Insets border, final int tint) {
        }

        @Override
        public void text(final TextSpan span, final float x, final float y) {
            this.lines.add("text " + span.text() + " " + ClipLog.number(x) + "," + ClipLog.number(y));
        }

        @Override
        public void pushClip(final Rect rect) {
            this.lines.add("clip " + DrawLog.rect(rect));
        }

        @Override
        public void popClip() {
            this.lines.add("pop");
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
}
