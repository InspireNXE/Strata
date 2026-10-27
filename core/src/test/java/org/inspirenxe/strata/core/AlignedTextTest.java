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
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.layout.VerticalAlignment;
import org.inspirenxe.strata.api.text.TextSpan;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A developer's component drawing one line of text aligned in a rectangle.
 */
class AlignedTextTest {
    private static final TextSpan SPAN = TextSpan.of("Ana");

    private final TestHost test = new TestHost(200, 100);
    private final ComponentFactory ui = this.test.factory();
    private final float width = this.test.platform().textMetrics().width(SPAN);
    private final float height = this.test.platform().textMetrics().lineHeight(SPAN.style());

    private String draw(final HorizontalAlignment horizontal, final VerticalAlignment vertical) {
        final Tag tag = new Tag(this.ui, horizontal, vertical);
        this.test.show(this.ui.absolutePanel().add(tag, 10, 20));
        final DrawLog canvas = new DrawLog();
        this.test.host().render(canvas);
        final List<String> texts = canvas.lines("text ");
        assertEquals(1, texts.size(), texts.toString());
        return texts.get(0).split(" ")[2];
    }

    private static String at(final float x, final float y) {
        return (x == Math.rint(x) ? Integer.toString((int) x) : Float.toString(x)) + ","
                + (y == Math.rint(y) ? Integer.toString((int) y) : Float.toString(y));
    }

    @Test
    void putsTheLineAtTheTopLeftForLeftAndTop() {
        assertEquals(at(10, 20), this.draw(HorizontalAlignment.LEFT, VerticalAlignment.TOP));
        assertEquals(at(10, 20), this.draw(HorizontalAlignment.STRETCH, VerticalAlignment.STRETCH));
        assertEquals(at(10, 20), this.draw(HorizontalAlignment.LEFT, VerticalAlignment.BASELINE));
    }

    @Test
    void centersOnWholeUnits() {
        final float x = 10 + (float) (Math.floor(Tag.WIDTH / 2d) - Math.floor(this.width / 2d));
        final float y = 20 + (float) (Math.floor(Tag.HEIGHT / 2d) - Math.floor(this.height / 2d));
        assertEquals(at(x, y), this.draw(HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE));
    }

    @Test
    void putsTheLineAgainstTheRightAndBottomEdges() {
        assertEquals(at(10 + Tag.WIDTH - this.width, 20 + Tag.HEIGHT - this.height),
                this.draw(HorizontalAlignment.RIGHT, VerticalAlignment.BOTTOM));
    }

    /**
     * A tag that draws its name aligned in its bounds, as a developer would.
     */
    static final class Tag extends CustomComponent<Tag> {
        static final float WIDTH = 61;
        static final float HEIGHT = 31;

        private final HorizontalAlignment horizontal;
        private final VerticalAlignment vertical;

        Tag(final ComponentFactory ui, final HorizontalAlignment horizontal, final VerticalAlignment vertical) {
            super(ui);
            this.horizontal = horizontal;
            this.vertical = vertical;
        }

        @Override
        protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
            return new Size(WIDTH, HEIGHT);
        }

        @Override
        protected void paint(final Painter painter, final Rect bounds) {
            painter.text(SPAN, bounds, this.horizontal, this.vertical);
        }
    }
}
