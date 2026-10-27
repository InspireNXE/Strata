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
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.extension.ArrangeContext;
import org.inspirenxe.strata.api.extension.CustomContainer;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.text.TextOverflow;
import org.inspirenxe.strata.core.host.ScreenHostImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * New text in a label that measures to the size it had is only drawn again: nothing above the label is measured or
 * arranged. Text of another size, and a label whose size depends on the space it gets, lay out their parents again.
 */
class LabelRelayoutTest {
    private final TestHost test = new TestHost(300, 200);
    private final ScreenHostImpl host = this.test.host();
    private final ComponentFactory ui = this.test.factory();

    /**
     * A column that counts how often it is measured and arranged, written as a developer would.
     */
    static final class Column extends CustomContainer<Column> {
        int measures;
        int arranges;

        Column(final ComponentFactory ui) {
            super(ui);
        }

        @Override
        protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
            this.measures++;
            float width = 0f;
            float height = 0f;
            for (final Component<?> child : this.children()) {
                final Size size = context.measure(child, availableWidth, availableHeight);
                width = Math.max(width, size.width());
                height += size.height();
            }
            return new Size(width, height);
        }

        @Override
        protected void arrange(final ArrangeContext context, final Rect bounds) {
            this.arranges++;
            float y = bounds.y();
            for (final Component<?> child : this.children()) {
                final Size size = context.outerSize(child);
                context.arrange(child, new Rect(bounds.x(), y, size.width(), size.height()));
                y += size.height();
            }
        }
    }

    private DrawLog render() {
        final DrawLog log = new DrawLog();
        this.host.render(log);
        return log;
    }

    private Column showColumn(final Label label) {
        final Column column = new Column(this.ui).add(label).add(this.ui.label("Below"));
        this.test.show(column);
        this.render();
        column.measures = 0;
        column.arranges = 0;
        return column;
    }

    @Test
    void textOfTheSameSizeIsOnlyDrawnAgain() {
        final Label label = this.ui.label("Cost");
        final Column column = this.showColumn(label);
        final Rect before = label.bounds();
        label.setText(Text.of("Fees"));
        assertTrue(this.host.needsFrame(), "the new text asks for a frame");
        final DrawLog log = this.render();
        assertEquals(0, column.measures, "the column is not measured again");
        assertEquals(0, column.arranges, "nor arranged again");
        assertEquals(before, label.bounds());
        assertEquals(1, log.lines("text Fees").size(), "the new text is drawn");
        assertFalse(this.host.needsFrame(), "nothing is left to lay out");
    }

    @Test
    void textOfAnotherSizeLaysOutTheParent() {
        final Label label = this.ui.label("Cost");
        final Column column = this.showColumn(label);
        final float width = label.bounds().width();
        label.setText(Text.of("Shipping"));
        this.render();
        assertEquals(1, column.measures);
        assertEquals(1, column.arranges);
        assertEquals(width * 2f, label.bounds().width(), 0.001f, "twice the characters, twice as wide");
    }

    @Test
    void aSecondLineLaysOutTheParent() {
        final Label label = this.ui.label("Cost");
        final Column column = this.showColumn(label);
        final float below = column.children().get(1).bounds().y();
        label.setText(Text.of("Cost\nFees"));
        this.render();
        assertEquals(1, column.measures);
        assertTrue(column.children().get(1).bounds().y() > below, "the label below moves down");
    }

    @Test
    void aWrappingLabelLaysOutTheParent() {
        final Label label = this.ui.label("Cost").setWrap(true);
        final Column column = this.showColumn(label);
        label.setText(Text.of("Fees"));
        this.render();
        assertEquals(1, column.measures, "its size depends on the width it gets");
    }

    @Test
    void aShortenedLabelLaysOutTheParent() {
        final Label label = this.ui.label("Cost").setOverflow(TextOverflow.ELLIPSIS);
        final Column column = this.showColumn(label);
        label.setText(Text.of("Fees"));
        this.render();
        assertEquals(1, column.measures, "its size depends on the width it gets");
    }

    @Test
    void aMeasuredHandlerLaysOutTheParent() {
        final Label label = this.ui.label("Cost");
        label.onEvent(Component.MEASURED, event -> event.setWidth(event.width() + 4f));
        final Column column = this.showColumn(label);
        label.setText(Text.of("Fees"));
        this.render();
        assertEquals(1, column.measures, "the handler may size it from anything");
    }

    @Test
    void textChangedBeforeTheFirstLayoutLaysOutAsUsual() {
        final Label label = this.ui.label("Cost");
        label.setText(Text.of("Shipping"));
        final Column column = new Column(this.ui).add(label);
        this.test.show(column);
        this.render();
        assertEquals("Shipping", label.text().plain());
        assertTrue(label.bounds().width() > 0f);
    }
}
