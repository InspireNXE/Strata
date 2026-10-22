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
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.AnchorPanel;
import org.inspirenxe.strata.api.layout.GridLength;
import org.inspirenxe.strata.api.layout.GridPanel;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.inspirenxe.strata.api.layout.VerticalAlignment;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LayoutTest {

    @Test
    void panelPlacesChildrenByAnchor() {
        final TestHost test = new TestHost(200, 100);
        final ComponentFactory ui = test.factory();
        final Box center = new Box(ui, 20, 10).setAnchor(Anchor.CENTER);
        final Box bottomRight = new Box(ui, 30, 5).setAnchor(Anchor.BOTTOM_RIGHT).setMargin(2);
        final Box fill = new Box(ui, 1, 1).setAnchor(Anchor.FILL);
        final AnchorPanel panel = ui.anchorPanel().setPadding(10).add(center).add(bottomRight).add(fill);
        panel.setAnchor(Anchor.FILL);

        test.show(panel);

        assertEquals(new Rect(0, 0, 200, 100), panel.bounds());
        assertEquals(new Rect(90, 45, 20, 10), center.bounds());
        assertEquals(new Rect(158, 83, 30, 5), bottomRight.bounds());
        assertEquals(new Rect(10, 10, 180, 80), fill.bounds());
    }

    @Test
    void verticalStackSumsHeightsWithSpacing() {
        final TestHost test = new TestHost(500, 500);
        final ComponentFactory ui = test.factory();
        final Box a = new Box(ui, 40, 10);
        final Box b = new Box(ui, 60, 20).setMargin(0, 5);
        final Box c = new Box(ui, 10, 10).setAlignment(HorizontalAlignment.RIGHT, VerticalAlignment.TOP);
        final StackPanel stack = ui.stackPanel().setSpacing(4).add(a).add(b).add(c);

        test.show(stack);

        // 10 + (5 + 20 + 5) + 10 plus two gaps of 4
        assertEquals(58, stack.desiredHeight());
        assertEquals(60, stack.desiredWidth());
        assertEquals(new Rect(0, 0, 40, 10), a.bounds());
        assertEquals(new Rect(0, 19, 60, 20), b.bounds());
        assertEquals(new Rect(50, 48, 10, 10), c.bounds());
    }

    @Test
    void hiddenChildrenTakeNoSpaceInStack() {
        final TestHost test = new TestHost(100, 100);
        final ComponentFactory ui = test.factory();
        final Box a = new Box(ui, 10, 10);
        final Box hidden = new Box(ui, 10, 50).setVisible(false);
        final Box b = new Box(ui, 10, 10);
        final StackPanel stack = ui.stackPanel().setSpacing(2).add(a).add(hidden).add(b);

        test.show(stack);

        assertEquals(22, stack.desiredHeight());
        assertEquals(12, b.bounds().y());
    }

    @Test
    void gridSharesLeftoverSpaceByWeight() {
        final TestHost test = new TestHost(200, 50);
        final ComponentFactory ui = test.factory();
        final Box fixed = new Box(ui, 5, 5);
        final Box auto = new Box(ui, 30, 5);
        final Box one = new Box(ui, 5, 5).setAnchor(Anchor.FILL);
        final Box two = new Box(ui, 5, 5).setAnchor(Anchor.FILL);
        final GridPanel grid = ui.gridPanel()
                .columns(GridLength.fixed(20), GridLength.auto(), GridLength.weighted(1), GridLength.weighted(2))
                .rows(GridLength.weighted(1));
        grid.add(fixed, 0, 0).add(auto, 0, 1).add(one, 0, 2).add(two, 0, 3);
        grid.setAnchor(Anchor.FILL);

        test.show(grid);

        // 200 - 20 - 30 = 150 left, split 1:2
        assertEquals(new Rect(50, 0, 50, 50), one.bounds());
        assertEquals(new Rect(100, 0, 100, 50), two.bounds());
    }

    @Test
    void gridSpanningChildGrowsAutoTracks() {
        final TestHost test = new TestHost(500, 500);
        final ComponentFactory ui = test.factory();
        final Box left = new Box(ui, 10, 10);
        final Box right = new Box(ui, 10, 10);
        final Box wide = new Box(ui, 60, 10);
        final GridPanel grid = ui.gridPanel()
                .columns(GridLength.auto(), GridLength.auto())
                .rows(GridLength.auto(), GridLength.auto());
        grid.add(left, 0, 0).add(right, 0, 1).add(wide, 1, 0, 1, 2);

        test.show(grid);

        assertEquals(60, grid.desiredWidth());
        assertEquals(30, right.bounds().x());
    }

    @Test
    void wrappingLabelInAWeightedColumnWrapsAtTheColumn() {
        final TestHost test = new TestHost(150, 200);
        final ComponentFactory ui = test.factory();
        // 6 units per character: "hello world again" is 102 wide, "hello world" 66
        final Label label = ui.label("hello world again").setWrap(true);
        final GridPanel grid = ui.gridPanel()
                .columns(GridLength.fixed(70), GridLength.weighted(1))
                .rows(GridLength.auto());
        grid.add(new Box(ui, 10, 10), 0, 0).add(label, 0, 1);
        grid.setAnchor(Anchor.FILL);

        test.show(grid);

        // The weighted column gets 150 - 70 = 80, so the label wraps after "hello world"
        assertEquals(66, label.desiredWidth());
        assertEquals(2 * TestPlatform.LINE_HEIGHT, label.desiredHeight());
        assertTrue(label.bounds().right() <= 150);
    }

    @Test
    void wrappingLabelBreaksAtAvailableWidth() {
        final TestHost test = new TestHost(500, 500);
        final ComponentFactory ui = test.factory();
        // 6 units per character: "hello world" is 66 wide
        final Label label = ui.label("hello world again").setWrap(true);
        final AnchorPanel panel = ui.anchorPanel().add(label);
        panel.setWidth(70);

        test.show(panel);

        assertEquals(66, label.desiredWidth());
        assertEquals(2 * TestPlatform.LINE_HEIGHT, label.desiredHeight());
    }

    @Test
    void layoutRunsAgainAfterAChange() {
        final TestHost test = new TestHost(100, 100);
        final Box box = new Box(test.factory(), 10, 10);
        test.show(box);
        assertEquals(new Rect(0, 0, 10, 10), box.bounds());

        box.setMargin(5);
        test.host().layout();
        assertEquals(new Rect(5, 5, 10, 10), box.bounds());
    }

    @Test
    void minAndMaxClampDesiredSize() {
        final TestHost test = new TestHost(1000, 1000);
        final Box box = new Box(test.factory(), 500, 1).setMinSize(0, 20).setMaxSize(100, Float.POSITIVE_INFINITY);
        test.show(box);

        assertEquals(100, box.desiredWidth());
        assertEquals(20, box.desiredHeight());
    }

    /**
     * Records the space it was last measured with.
     */
    private static final class Probe extends CustomComponent<Probe> {
        private float offeredWidth;
        private float offeredHeight;

        Probe(final ComponentFactory ui) {
            super(ui);
        }

        @Override
        protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
            this.offeredWidth = availableWidth;
            this.offeredHeight = availableHeight;
            return new Size(5, 5);
        }
    }

    @Test
    void gridWeightedColumnKeepsItsChildsMinimum() {
        final TestHost test = new TestHost(200, 50);
        final ComponentFactory ui = test.factory();
        final Box wide = new Box(ui, 5, 5).setAnchor(Anchor.FILL).setMinSize(150, 0);
        final Box other = new Box(ui, 5, 5).setAnchor(Anchor.FILL);
        final GridPanel grid = ui.gridPanel()
                .columns(GridLength.weighted(1), GridLength.weighted(1))
                .rows(GridLength.weighted(1));
        grid.add(wide, 0, 0).add(other, 0, 1);
        grid.setAnchor(Anchor.FILL);

        test.show(grid);

        // Half of 200 is below 150, so the first column keeps 150 and the second gets the rest
        assertEquals(new Rect(0, 0, 150, 50), wide.bounds());
        assertEquals(new Rect(150, 0, 50, 50), other.bounds());
    }

    @Test
    void gridCappedWeightedColumnLeavesItsShareToTheOthers() {
        final TestHost test = new TestHost(200, 50);
        final ComponentFactory ui = test.factory();
        final Box capped = new Box(ui, 5, 5).setAnchor(Anchor.FILL).setMaxSize(40, Float.POSITIVE_INFINITY);
        final Box other = new Box(ui, 5, 5).setAnchor(Anchor.FILL);
        final GridPanel grid = ui.gridPanel()
                .columns(GridLength.weighted(1), GridLength.weighted(1))
                .rows(GridLength.weighted(1));
        grid.add(capped, 0, 0).add(other, 0, 1);
        grid.setAnchor(Anchor.FILL);

        test.show(grid);

        assertEquals(new Rect(0, 0, 40, 50), capped.bounds());
        assertEquals(new Rect(40, 0, 160, 50), other.bounds());
    }

    @Test
    void gridWithoutDefinitionsFollowsASmallerSlot() {
        final TestHost test = new TestHost(300, 300);
        final ComponentFactory ui = test.factory();
        final Box box = new Box(ui, 10, 10).setAnchor(Anchor.FILL);
        final GridPanel grid = ui.gridPanel().add(box);
        grid.setAnchor(Anchor.FILL);
        final Squeeze squeeze = new Squeeze(ui, 200, 100).add(grid);
        test.show(squeeze);
        assertEquals(new Rect(0, 0, 200, 100), box.bounds());

        squeeze.squeeze(100, 50);
        test.host().layout();

        assertEquals(new Rect(0, 0, 100, 50), box.bounds());
    }

    @Test
    void gridOffersChildrenOfWeightedRowsTheirRowsShare() {
        final TestHost test = new TestHost(200, 100);
        final ComponentFactory ui = test.factory();
        final Probe autoColumn = new Probe(ui);
        final Probe weightedColumn = new Probe(ui);
        final GridPanel grid = ui.gridPanel()
                .columns(GridLength.auto(), GridLength.weighted(1))
                .rows(GridLength.fixed(20), GridLength.weighted(1), GridLength.weighted(3));
        grid.add(autoColumn, 1, 0).add(weightedColumn, 2, 1);
        grid.setAnchor(Anchor.FILL);

        test.show(grid);

        // The weighted rows share the 80 the fixed row leaves by 1 to 3; the auto column is 5 wide
        assertEquals(20, autoColumn.offeredHeight);
        assertEquals(60, weightedColumn.offeredHeight);
        assertEquals(195, weightedColumn.offeredWidth);
    }

    @Test
    void gridDefinitionsRefuseANullLength() {
        final TestHost test = new TestHost(100, 100);
        final GridPanel grid = test.factory().gridPanel().columns(GridLength.auto());

        assertThrows(NullPointerException.class, () -> grid.columns(GridLength.auto(), null));
        assertThrows(NullPointerException.class, () -> grid.rows((GridLength) null));
        assertEquals(List.of(GridLength.auto()), grid.columnDefinitions());
    }
}
