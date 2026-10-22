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
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.CustomContainer;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.layout.AnchorPanel;
import org.inspirenxe.strata.api.layout.GridLength;
import org.inspirenxe.strata.api.layout.GridPanel;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.inspirenxe.strata.api.layout.VerticalAlignment;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Design 4.53: baselines, reported by components and lined up by panels. Text in the test platform is 6 units per
 * code point and 9 per line, with the baseline 7 below a line's top, so a label's baseline is 7.
 */
class BaselineTest {
    private final TestHost test = new TestHost(200, 100);
    private final ComponentFactory ui = this.test.factory();

    /**
     * A component as a developer would write one: a 40 by 20 box with its text's baseline 12 below its top, like a
     * text box that pads its text.
     */
    static final class Field extends CustomComponent<Field> {
        Field(final ComponentFactory ui) {
            super(ui);
        }

        @Override
        protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
            return new Size(40, 20);
        }

        @Override
        protected float baseline() {
            return 12;
        }
    }

    /**
     * A container as a developer would write one: it stacks its children and reports where each one's baseline lies.
     */
    static final class Probe extends CustomContainer<Probe> {
        float measured;
        float arranged;

        Probe(final ComponentFactory ui) {
            super(ui);
        }

        @Override
        protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
            final Component<?> child = this.children().getFirst();
            final Size size = context.measure(child, availableWidth, availableHeight);
            this.measured = context.baseline(child);
            return size;
        }

        @Override
        protected void arrange(final ArrangeContext context, final Rect bounds) {
            final Component<?> child = this.children().getFirst();
            this.arranged = context.baseline(child);
            context.arrange(child, bounds);
        }
    }

    private <T extends Component<T>> T onBaseline(final T component) {
        return component.setAlignment(HorizontalAlignment.LEFT, VerticalAlignment.BASELINE);
    }

    @Test
    void aRowLinesUpBaselines() {
        final Label label = this.onBaseline(this.ui.label("Price"));
        final Field field = this.onBaseline(new Field(this.ui));
        final Label unit = this.onBaseline(this.ui.label("per unit"));
        final StackPanel row = this.ui.stackPanel(Orientation.HORIZONTAL).add(label).add(field).add(unit);
        this.test.show(row);

        assertEquals(new Rect(0, 5, 30, 9), label.bounds());
        assertEquals(new Rect(30, 0, 40, 20), field.bounds());
        assertEquals(new Rect(70, 5, 48, 9), unit.bounds());
        assertEquals(20, row.desiredHeight());
    }

    @Test
    void aChildWithNoBaselinePutsItsBottomOnTheLine() {
        final AnchorPanel box = this.onBaseline(this.ui.anchorPanel());
        box.setSize(10, 30);
        final Field field = this.onBaseline(new Field(this.ui));
        final StackPanel row = this.ui.stackPanel(Orientation.HORIZONTAL).add(box).add(field);
        this.test.show(row);

        assertEquals(new Rect(0, 0, 10, 30), box.bounds());
        assertEquals(new Rect(10, 18, 40, 20), field.bounds());
        assertEquals(38, row.desiredHeight());
    }

    @Test
    void theMarginCountsAboveTheBaseline() {
        final Label label = this.onBaseline(this.ui.label("Price"));
        label.setMargin(0, 10, 0, 0);
        final Field field = this.onBaseline(new Field(this.ui));
        this.test.show(this.ui.stackPanel(Orientation.HORIZONTAL).add(label).add(field));

        assertEquals(new Rect(0, 10, 30, 9), label.bounds());
        assertEquals(new Rect(30, 5, 40, 20), field.bounds());
    }

    @Test
    void childrenNotOnTheBaselineKeepTheirAlignment() {
        final Label top = this.ui.label("Top");
        final Field field = this.onBaseline(new Field(this.ui));
        final Label bottom = this.ui.label("Bottom");
        bottom.setAlignment(HorizontalAlignment.LEFT, VerticalAlignment.BOTTOM);
        final AnchorPanel tall = this.ui.anchorPanel();
        tall.setSize(5, 40);
        this.test.show(this.ui.stackPanel(Orientation.HORIZONTAL).add(top).add(field).add(bottom).add(tall));

        assertEquals(0, top.bounds().y());
        assertEquals(0, field.bounds().y());
        assertEquals(31, bottom.bounds().y());
    }

    @Test
    void aColumnPlacesBaselineChildrenAtTheTop() {
        final Label label = this.onBaseline(this.ui.label("Price"));
        final Field field = this.onBaseline(new Field(this.ui));
        this.test.show(this.ui.stackPanel().add(label).add(field));

        assertEquals(new Rect(0, 0, 30, 9), label.bounds());
        assertEquals(new Rect(0, 9, 40, 20), field.bounds());
    }

    @Test
    void aWrapPanelLinesUpEachRow() {
        final Label a = this.onBaseline(this.ui.label("Price"));
        final Field b = this.onBaseline(new Field(this.ui));
        final Label c = this.onBaseline(this.ui.label("Quantity"));
        final Field d = this.onBaseline(new Field(this.ui));
        final Label e = this.onBaseline(this.ui.label("Grand total"));
        this.test.show(this.ui.wrapPanel().setSpacing(0, 4).add(a).add(b).add(c).add(d).add(e));

        // 30 + 40 + 48 + 40 = 158 fits in 200, and the last label, 66 wide, wraps
        assertEquals(new Rect(0, 5, 30, 9), a.bounds());
        assertEquals(new Rect(30, 0, 40, 20), b.bounds());
        assertEquals(new Rect(70, 5, 48, 9), c.bounds());
        assertEquals(new Rect(118, 0, 40, 20), d.bounds());
        assertEquals(new Rect(0, 24, 66, 9), e.bounds());
    }

    @Test
    void aGridRowLinesUpItsCells() {
        final Label label = this.onBaseline(this.ui.label("Price"));
        final Field field = this.onBaseline(new Field(this.ui));
        final Label after = this.onBaseline(this.ui.label("Next"));
        final GridPanel grid = this.ui.gridPanel()
                .columns(GridLength.auto(), GridLength.auto())
                .rows(GridLength.auto(), GridLength.auto())
                .add(label, 0, 0)
                .add(field, 0, 1)
                .add(after, 1, 0);
        this.test.show(grid);

        assertEquals(new Rect(0, 5, 30, 9), label.bounds());
        assertEquals(new Rect(30, 0, 40, 20), field.bounds());
        assertEquals(new Rect(0, 20, 24, 9), after.bounds());
    }

    @Test
    void aGridRowGrowsToHoldItsLinedUpCells() {
        final Label label = this.onBaseline(this.ui.label("Price"));
        label.setMargin(0, 20, 0, 0);
        final Field field = this.onBaseline(new Field(this.ui));
        final Label after = this.ui.label("Next");
        final GridPanel grid = this.ui.gridPanel()
                .columns(GridLength.auto(), GridLength.auto())
                .rows(GridLength.auto(), GridLength.auto())
                .add(label, 0, 0)
                .add(field, 0, 1)
                .add(after, 1, 0);
        this.test.show(grid);

        // The label's baseline is 27 below its slot; the field moves down 15 and ends at 35
        assertEquals(new Rect(30, 15, 40, 20), field.bounds());
        assertEquals(35, after.bounds().y());
    }

    @Test
    void aLabelReportsItsFirstLinesBaseline() {
        final Label label = this.ui.label("One\nTwo").setTextScale(2);
        final Probe probe = new Probe(this.ui);
        probe.add(label);
        this.test.show(probe);

        assertEquals(14, probe.measured);
        assertEquals(14, probe.arranged);
    }

    @Test
    void aHiddenChildHasNoBaseline() {
        final Probe probe = new Probe(this.ui);
        probe.add(new Field(this.ui).setVisible(false));
        this.test.show(probe);

        assertTrue(Float.isNaN(probe.measured));
    }

    @Test
    void theDefaultAscentIsFourFifthsOfTheLine() {
        final TextMetrics metrics = new TextMetrics() {
            @Override
            public float width(final TextSpan span) {
                return 0;
            }

            @Override
            public float lineHeight(final TextStyle style) {
                return 10;
            }
        };
        assertEquals(8, metrics.ascent(TextStyle.DEFAULT));
    }
}
