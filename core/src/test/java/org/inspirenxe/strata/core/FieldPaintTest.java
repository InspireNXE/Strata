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

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.control.field.PasswordTextBox;
import org.inspirenxe.strata.api.control.field.Slider;
import org.inspirenxe.strata.api.control.field.TextArea;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.Themes;
import org.inspirenxe.strata.api.visual.Visual;
import org.inspirenxe.strata.spi.Canvas;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Painting a field twice with nothing changed in between draws the very same rectangles and spans, so a frame that
 * repeats the last one allocates none of them. The theme draws selected text in its own style, so the selected parts
 * are drawn in clips of their own.
 */
class FieldPaintTest {
    private static final Theme THEME = Themes.flat().toBuilder()
            .textStyle(TextBox.STYLE_TEXT_SELECTED, TextStyle.DEFAULT.withColor(0xFF1F1FFF))
            .metric(TextBox.STYLE_SELECTION_EXTEND, 1f)
            .metric(TextBox.STYLE_CARET_EXTEND, 1f)
            .build();

    private final TestHost test = new TestHost(new TestPlatform(), THEME, 300, 200);

    private void show(final Component<?> field) {
        this.test.show(this.test.factory().absolutePanel().add(field));
    }

    /**
     * Renders twice and checks that the second frame drew the same objects as the first.
     */
    private void assertSecondFrameRepeatsTheFirst() {
        final Recorder first = new Recorder();
        this.test.host().render(first);
        final Recorder second = new Recorder();
        this.test.host().render(second);
        assertFalse(first.calls.isEmpty());
        assertEquals(first.calls.size(), second.calls.size());
        for (int i = 0; i < first.calls.size(); i++) {
            assertSame(first.calls.get(i), second.calls.get(i), "draw call " + i + ": " + second.calls.get(i));
        }
    }

    @Test
    void aTextBoxWithASelectionRepeatsItsFrame() {
        final TextBox box = this.test.factory().textBox().setWidth(200);
        this.show(box);
        box.setText("hello world");
        box.requestFocus();
        box.select(2, 7);

        this.assertSecondFrameRepeatsTheFirst();
    }

    @Test
    void aPasswordBoxWithASelectionRepeatsItsFrame() {
        final PasswordTextBox box = this.test.factory().passwordTextBox().setWidth(200);
        this.show(box);
        box.setPassword("open sesame".toCharArray());
        box.requestFocus();
        box.select(2, 7);

        this.assertSecondFrameRepeatsTheFirst();
    }

    @Test
    void aTextAreaWithASelectionOverRowsRepeatsItsFrame() {
        final TextArea area = this.test.factory().textArea().setAnchor(Anchor.TOP_LEFT).setWidth(200);
        this.show(area);
        area.setText("one two\nthree four\nfive");
        area.requestFocus();
        area.select(4, 14);

        this.assertSecondFrameRepeatsTheFirst();
    }

    @Test
    void aSliderRepeatsItsFrame() {
        final Slider slider = this.test.factory().slider().setValue(0.25);
        this.show(slider);

        this.assertSecondFrameRepeatsTheFirst();
    }

    /**
     * Records the rectangles and spans each draw call is given.
     */
    private static final class Recorder implements Canvas {
        private final List<Object> calls = new ArrayList<>();

        @Override
        public void fill(final Rect rect, final int argb) {
            this.calls.add(rect);
        }

        @Override
        public void fillGradient(final Rect rect, final int topArgb, final int bottomArgb) {
            this.calls.add(rect);
        }

        @Override
        public void sprite(final SpriteId sprite, final Rect rect, final int tint) {
            this.calls.add(rect);
        }

        @Override
        public void nineSlice(final SpriteId sprite, final Rect rect, final Insets border, final int tint) {
            this.calls.add(rect);
        }

        @Override
        public void text(final TextSpan span, final float x, final float y) {
            this.calls.add(span);
        }

        @Override
        public void pushClip(final Rect rect) {
            this.calls.add(rect);
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
        public void visual(final Visual visual, final Rect rect, final int tint) {
            this.calls.add(rect);
        }

        @Override
        public void popTransform() {
        }

        @Override
        public void nextLayer() {
        }

        @Override
        public void requestCursor(final CursorKind cursor) {
        }
    }
}
