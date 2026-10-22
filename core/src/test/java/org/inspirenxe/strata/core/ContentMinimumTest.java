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
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.AnchorPanel;
import org.inspirenxe.strata.api.layout.GridLength;
import org.inspirenxe.strata.api.layout.GridPanel;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.inspirenxe.strata.api.text.TextOverflow;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Content minimums: what a component reports from {@code measureMinimum}, and how weighted stack and grid shares keep
 * to it. Text in the test platform is 6 units per code point and 9 per line.
 */
class ContentMinimumTest {

    /**
     * A component as a developer would write one: 10 by 10, with a content minimum it is told.
     */
    static final class Minimal extends CustomComponent<Minimal> {
        private float minimumWidth;
        int calls;
        boolean failing;

        Minimal(final ComponentFactory ui, final float minimumWidth) {
            super(ui);
            this.minimumWidth = minimumWidth;
        }

        Minimal setMinimum(final float minimumWidth) {
            this.minimumWidth = minimumWidth;
            this.invalidateLayout();
            return this;
        }

        @Override
        protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
            return new Size(10, 10);
        }

        @Override
        protected Size measureMinimum(final MeasureContext context) {
            this.calls++;
            if (this.failing) {
                throw new IllegalStateException("No minimum yet");
            }
            return new Size(this.minimumWidth, 0);
        }
    }

    private static AnchorPanel body(final ComponentFactory ui) {
        return ui.anchorPanel().setAnchor(Anchor.FILL);
    }

    private static StackPanel row(final TestHost test, final StackPanel stack) {
        stack.setAnchor(Anchor.FILL);
        test.show(stack);
        return stack;
    }

    @Nested
    class Stacks {
        private final TestHost test = new TestHost(200, 100);
        private final ComponentFactory ui = this.test.factory();

        @Test
        void aWeightedChildGetsAtLeastItsContentMinimum() {
            final Minimal wide = new Minimal(this.ui, 150).setAnchor(Anchor.FILL);
            final AnchorPanel body = body(this.ui);
            row(this.test, this.ui.stackPanel(Orientation.HORIZONTAL).add(wide, 1).add(body, 1));

            assertEquals(150, wide.bounds().width());
            assertEquals(new Rect(150, 0, 50, 100), body.bounds());
        }

        @Test
        void theMinimumIsAskedOnceUntilTheLayoutIsInvalidated() {
            final Minimal wide = new Minimal(this.ui, 150).setAnchor(Anchor.FILL);
            final AnchorPanel body = body(this.ui);
            row(this.test, this.ui.stackPanel(Orientation.HORIZONTAL).add(wide, 1).add(body, 1));
            assertEquals(1, wide.calls);

            wide.setMinimum(120);
            this.test.host().layout();

            assertEquals(2, wide.calls);
            assertEquals(new Rect(120, 0, 80, 100), body.bounds());
        }

        @Test
        void aHiddenChildIsNotAsked() {
            final Minimal hidden = new Minimal(this.ui, 150);
            hidden.setVisible(false);
            final AnchorPanel body = body(this.ui);
            row(this.test, this.ui.stackPanel(Orientation.HORIZONTAL).add(hidden, 1).add(body, 1));

            assertEquals(0, hidden.calls);
            assertEquals(new Rect(0, 0, 200, 100), body.bounds());
        }

        @Test
        void aMinimumThatThrowsIsAskedAgainOnTheNextLayout() {
            final Minimal wide = new Minimal(this.ui, 150).setAnchor(Anchor.FILL);
            wide.failing = true;
            final AnchorPanel body = body(this.ui);
            final StackPanel stack = this.ui.stackPanel(Orientation.HORIZONTAL).add(wide, 1).add(body, 1);
            stack.setAnchor(Anchor.FILL);

            assertThrows(IllegalStateException.class, () -> this.test.show(stack));
            wide.failing = false;
            this.test.host().layout();

            assertEquals(new Rect(150, 0, 50, 100), body.bounds());
        }

        @Test
        void minimumsThatAreNotFiniteOrAreNegativeCountAsZero() {
            final AnchorPanel body = body(this.ui);
            row(this.test, this.ui.stackPanel(Orientation.HORIZONTAL)
                    .add(new Minimal(this.ui, Float.NaN), 1)
                    .add(new Minimal(this.ui, Float.POSITIVE_INFINITY), 1)
                    .add(new Minimal(this.ui, -5), 1)
                    .add(body, 1));

            assertEquals(new Rect(150, 0, 50, 100), body.bounds());
        }

        @Test
        void aFixedWidthHoldsAWeightedShareUp() {
            final AnchorPanel fixed = body(this.ui);
            fixed.setWidth(150);
            final AnchorPanel body = body(this.ui);
            row(this.test, this.ui.stackPanel(Orientation.HORIZONTAL).add(fixed, 1).add(body, 1));

            assertEquals(new Rect(150, 0, 50, 100), body.bounds());
        }

        @Test
        void aMaximumSizeCapsTheContentMinimum() {
            final Minimal wide = new Minimal(this.ui, 150).setAnchor(Anchor.FILL);
            wide.setMaxSize(120, Float.POSITIVE_INFINITY);
            final AnchorPanel body = body(this.ui);
            row(this.test, this.ui.stackPanel(Orientation.HORIZONTAL).add(wide, 1).add(body, 1));

            assertEquals(new Rect(120, 0, 80, 100), body.bounds());
        }

        @Test
        void aNestedStackPassesItsChildrenMinimumsUp() {
            final TestHost narrow = new TestHost(60, 100);
            final ComponentFactory ui = narrow.factory();
            // "Abc" and "Defg" do not shrink, so the inner row needs 18 + 2 + 24
            final StackPanel inner = ui.stackPanel(Orientation.HORIZONTAL).add(ui.label("Abc")).add(ui.label("Defg"))
                    .setSpacing(2);
            final AnchorPanel body = body(ui);
            row(narrow, ui.stackPanel(Orientation.HORIZONTAL).add(inner, 1).add(body, 1));

            assertEquals(new Rect(44, 0, 16, 100), body.bounds());
        }

        @Test
        void aVerticalStackKeepsToMinimumHeights() {
            final TestHost tall = new TestHost(100, 30);
            final ComponentFactory ui = tall.factory();
            final Label label = ui.label("One\nTwo");
            final AnchorPanel body = body(ui);
            row(tall, ui.stackPanel().add(label, 1).add(body, 1));

            assertEquals(new Rect(0, 18, 100, 12), body.bounds());
        }
    }

    @Nested
    class Controls {
        private final TestHost test = new TestHost(60, 100);
        private final ComponentFactory ui = this.test.factory();

        @Test
        void aWrappingLabelKeepsItsLongestWord() {
            final Label label = this.ui.label("Lengthy words").setWrap(true);
            final AnchorPanel body = body(this.ui);
            row(this.test, this.ui.stackPanel(Orientation.HORIZONTAL).add(label, 1).add(body, 1));

            assertEquals(new Rect(42, 0, 18, 100), body.bounds());
        }

        @Test
        void aLabelThatDoesNotWrapKeepsItsWholeLine() {
            final Label label = this.ui.label("Lengthy");
            final AnchorPanel body = body(this.ui);
            row(this.test, this.ui.stackPanel(Orientation.HORIZONTAL).add(label, 1).add(body, 1));

            assertEquals(new Rect(42, 0, 18, 100), body.bounds());
        }

        @Test
        void aLabelThatEndsInDotsNeedsOnlyTheDots() {
            final Label label = this.ui.label("Lengthy words").setOverflow(TextOverflow.ELLIPSIS);
            final AnchorPanel body = body(this.ui);
            row(this.test, this.ui.stackPanel(Orientation.HORIZONTAL).add(label, 1).add(body, 1));

            assertEquals(new Rect(30, 0, 30, 100), body.bounds());
        }

        @Test
        void aButtonKeepsItsCaption() {
            final Button button = this.ui.button("Save");
            final AnchorPanel body = body(this.ui);
            row(this.test, this.ui.stackPanel(Orientation.HORIZONTAL).add(button, 1).add(body, 1));

            assertTrue(button.desiredWidth() > 30);
            assertEquals(button.desiredWidth(), body.bounds().x());
            assertEquals(60 - button.desiredWidth(), body.bounds().width());
        }
    }

    @Nested
    class Grids {
        private final TestHost test = new TestHost(100, 100);
        private final ComponentFactory ui = this.test.factory();

        @Test
        void aWeightedColumnGetsAtLeastItsChildrensMinimum() {
            final AnchorPanel body = body(this.ui);
            final GridPanel grid = this.ui.gridPanel()
                    .columns(GridLength.weighted(1), GridLength.weighted(1))
                    .add(new Minimal(this.ui, 80), 0, 0)
                    .add(body, 0, 1);
            grid.setAnchor(Anchor.FILL);
            this.test.show(grid);

            assertEquals(new Rect(80, 0, 20, 100), body.bounds());
        }

        @Test
        void aGridsMinimumIsItsFixedTracksAndItsChildrensMinimums() {
            final TestHost narrow = new TestHost(60, 100);
            final ComponentFactory ui = narrow.factory();
            final GridPanel grid = ui.gridPanel()
                    .columns(GridLength.fixed(20), GridLength.auto())
                    .add(new Minimal(ui, 30), 0, 1);
            grid.setSpacing(2, 0);
            final AnchorPanel body = body(ui);
            row(narrow, ui.stackPanel(Orientation.HORIZONTAL).add(grid, 1).add(body, 1));

            assertEquals(new Rect(52, 0, 8, 100), body.bounds());
        }
    }
}
