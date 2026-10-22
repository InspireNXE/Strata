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
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.component.Focusable;
import org.inspirenxe.strata.api.control.button.IconPlacement;
import org.inspirenxe.strata.api.control.button.RadioGroup;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.visual.Visual;
import org.inspirenxe.strata.spi.Canvas;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Controls draw the same rectangles from one frame to the next while nothing moves, so painting does not allocate.
 */
class PaintReuseTest {
    private final TestHost test = new TestHost(300, 300);
    private final ComponentFactory ui = this.test.factory();

    private List<Rect> frame() {
        final RectLog canvas = new RectLog();
        this.test.host().render(canvas);
        return canvas.rects;
    }

    private <C extends Component<C> & Focusable<C>> void assertReused(final C control) {
        this.test.show(this.ui.stackPanel().add(control));
        control.requestFocus();
        final List<Rect> first = this.frame();
        final List<Rect> second = this.frame();
        assertFalse(first.isEmpty());
        assertEquals(first.size(), second.size());
        for (int i = 0; i < first.size(); i++) {
            assertSame(first.get(i), second.get(i), "rectangle " + i + ": " + first.get(i));
        }
    }

    @Test
    void aCheckedCheckBox() {
        this.assertReused(this.ui.checkBox("Agree").setSelected(true));
    }

    @Test
    void aSelectedRadioButton() {
        final RadioGroup<String> group = this.ui.radioGroup();
        final var button = group.add("Ana", "ana");
        group.select(0);
        this.assertReused(button);
    }

    @Test
    void aToggleSwitchThatIsOn() {
        this.assertReused(this.ui.toggleSwitch("Sound").setSelected(true));
    }

    @Test
    void aToggleButtonThatIsOn() {
        this.assertReused(this.ui.toggleButton("Bold").setSelected(true));
    }

    @Test
    void aButtonWithItsIconAfterACaptionTooWide() {
        this.assertReused(this.ui.button("A long caption").setIcon(SpriteId.of("test", "icon"))
                .setIconPlacement(IconPlacement.END).setWidth(60));
    }

    @Test
    void aButtonWithAnIconAndACaptionTooWide() {
        this.assertReused(this.ui.button("A long caption").setIcon(SpriteId.of("test", "icon")).setWidth(60));
    }

    @Test
    void aCycleButton() {
        this.assertReused(this.ui.cycleButton("Size").setItems(List.of("Small", "Large")));
    }

    @Test
    void anExpanderAndASeparator() {
        this.test.show(this.ui.stackPanel(Orientation.VERTICAL)
                .add(this.ui.expander("More"))
                .add(this.ui.separator()));
        final List<Rect> first = this.frame();
        final List<Rect> second = this.frame();
        assertEquals(first.size(), second.size());
        for (int i = 0; i < first.size(); i++) {
            assertSame(first.get(i), second.get(i), "rectangle " + i + ": " + first.get(i));
        }
    }

    /**
     * Logs every rectangle drawn or clipped to, by identity, leaving out the ones the host makes itself for each
     * frame's root clip.
     */
    private static final class RectLog implements Canvas {
        private final List<Rect> rects = new ArrayList<>();

        @Override
        public void fill(final Rect rect, final int argb) {
            this.rects.add(rect);
        }

        @Override
        public void fillGradient(final Rect rect, final int topArgb, final int bottomArgb) {
            this.rects.add(rect);
        }

        @Override
        public void sprite(final SpriteId sprite, final Rect rect, final int tint) {
            this.rects.add(rect);
        }

        @Override
        public void nineSlice(final SpriteId sprite, final Rect rect, final Insets border, final int tint) {
            this.rects.add(rect);
        }

        @Override
        public void text(final TextSpan span, final float x, final float y) {
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
            this.rects.add(rect);
        }

        @Override
        public void nextLayer() {
        }

        @Override
        public void requestCursor(final CursorKind cursor) {
        }
    }
}
