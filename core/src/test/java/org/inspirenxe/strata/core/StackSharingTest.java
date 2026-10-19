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
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.AnchorPanel;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.api.layout.Spacer;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * How a stack panel shares its space: weighted children, spreading and spacers. Text in the test platform is 6 units
 * per code point and 9 per line.
 */
class StackSharingTest {
    private final TestHost test = new TestHost(200, 100);
    private final ComponentFactory ui = this.test.factory();

    private StackPanel filling(final StackPanel stack) {
        stack.setAnchor(Anchor.FILL);
        this.test.show(stack);
        return stack;
    }

    private AnchorPanel body() {
        return this.ui.anchorPanel().setAnchor(Anchor.FILL);
    }

    @Test
    void aWeightedBodyTakesWhatTheHeaderAndFooterLeave() {
        final Label header = this.ui.label("Header");
        final AnchorPanel body = this.body();
        final Label footer = this.ui.label("Footer");
        this.filling(this.ui.stackPanel().add(header).add(body, 1).add(footer).setSpacing(2));

        assertEquals(new Rect(0, 0, 36, 9), header.bounds());
        assertEquals(new Rect(0, 11, 200, 78), body.bounds());
        assertEquals(new Rect(0, 91, 36, 9), footer.bounds());
    }

    @Test
    void weightsShareTheRestInProportion() {
        final Label fixed = this.ui.label("Fixed");
        final AnchorPanel one = this.body();
        final AnchorPanel three = this.body();
        this.filling(this.ui.stackPanel(Orientation.HORIZONTAL).add(fixed).add(one, 1).add(three, 3));

        assertEquals(30, fixed.bounds().width());
        assertEquals(new Rect(30, 0, 42.5f, 100), one.bounds());
        assertEquals(new Rect(72.5f, 0, 127.5f, 100), three.bounds());
    }

    @Test
    void aWeightedChildIsPlacedInItsShareByItsOwnAlignment() {
        final Label centered = this.ui.label("Mid");
        centered.setAnchor(Anchor.CENTER);
        this.filling(this.ui.stackPanel().add(centered, 1));

        assertEquals(new Rect(91, 45.5f, 18, 9), centered.bounds());
    }

    @Test
    void aWeightedSpacerPushesTheNextChildToTheEnd() {
        final Label left = this.ui.label("A");
        final Spacer spacer = this.ui.spacer();
        final Label right = this.ui.label("B");
        this.filling(this.ui.stackPanel(Orientation.HORIZONTAL).add(left).add(spacer, 1).add(right));

        assertEquals(0, left.bounds().x());
        // A spacer stretches by default, so it fills its share
        assertEquals(new Rect(6, 0, 188, 100), spacer.bounds());
        assertEquals(194, right.bounds().x());
    }

    @Test
    void aFixedSpacerKeepsAGap() {
        final Label left = this.ui.label("A");
        final Label right = this.ui.label("B");
        this.filling(this.ui.stackPanel(Orientation.HORIZONTAL).add(left).add(this.ui.spacer(10, 0)).add(right));

        assertEquals(16, right.bounds().x());
    }

    @Test
    void aSpacerMeasuresAsItsFixedOrMinimumSize() {
        final Spacer least = this.ui.spacer().setMinSize(4, 5);
        final Spacer fixed = this.ui.spacer(12, 3);
        this.test.show(this.ui.absolutePanel().add(least).add(fixed));

        assertEquals(4, least.desiredWidth());
        assertEquals(5, least.desiredHeight());
        assertEquals(new Rect(0, 0, 12, 3), fixed.bounds());
    }

    @Test
    void spreadingSharesTheExtraRoomEquallyBetweenTheGaps() {
        final Label first = this.ui.label("A");
        final Label second = this.ui.label("B");
        final Label third = this.ui.label("C");
        final StackPanel row = this.filling(this.ui.stackPanel(Orientation.HORIZONTAL)
                .setSpread(true)
                .add(first)
                .add(second)
                .add(third));

        assertTrue(row.isSpread());
        assertEquals(0, first.bounds().x());
        assertEquals(97, second.bounds().x());
        assertEquals(194, third.bounds().x());
        // The panel still asks only for its children and the spacing
        assertEquals(18, row.desiredWidth());
    }

    @Test
    void spreadingNeverMakesAGapSmallerThanTheSpacing() {
        final Label first = this.ui.label("AAAAAAAAAAAAAAAAAAAA");
        final Label second = this.ui.label("BBBBBBBBBBBBBBBBBBBB");
        this.filling(this.ui.stackPanel(Orientation.HORIZONTAL)
                .setSpread(true)
                .setSpacing(90)
                .add(first)
                .add(second));

        assertEquals(210, second.bounds().x());
    }

    @Test
    void spreadingLeavesASingleChildAtTheStartAndYieldsToWeights() {
        final Label only = this.ui.label("A");
        final StackPanel row = this.filling(this.ui.stackPanel(Orientation.HORIZONTAL).setSpread(true).add(only));
        assertEquals(0, only.bounds().x());

        final Label last = this.ui.label("B");
        final AnchorPanel fill = this.body();
        row.add(fill, 1).add(last);
        this.test.host().layout();
        assertEquals(new Rect(6, 0, 188, 100), fill.bounds());
        assertEquals(194, last.bounds().x());
    }

    @Test
    void measuredWithoutALimitTheWeightedChildrenKeepTheirProportions() {
        final Label fixed = this.ui.label("Top");
        final AnchorPanel two = this.body().setMinSize(0, 20);
        final AnchorPanel one = this.body().setMinSize(0, 30);
        final StackPanel column = this.ui.stackPanel().add(fixed).add(two, 2).add(one, 1);
        this.test.show(this.ui.absolutePanel().add(column));

        // One unit fits both: 20 / 2 and 30 / 1, so 30, giving 60 and 30
        assertEquals(9 + 90, column.desiredHeight());
        assertEquals(60, two.bounds().height());
        assertEquals(30, one.bounds().height());
    }

    @Test
    void hiddenWeightedChildrenTakeNoShareAndNoGap() {
        final AnchorPanel hidden = this.body();
        final AnchorPanel shown = this.body();
        this.filling(this.ui.stackPanel().setSpacing(4).add(hidden, 1).add(shown, 1));
        hidden.setVisible(false);
        this.test.host().layout();

        assertEquals(new Rect(0, 0, 200, 100), shown.bounds());
    }

    @Test
    void weightsBelongToChildren() {
        final Label child = this.ui.label("A");
        final StackPanel stack = this.ui.stackPanel().add(child, 2);
        assertEquals(2, stack.weight(child));
        assertEquals(0, stack.weight(this.ui.label("B")));

        stack.setWeight(child, 0);
        assertEquals(0, stack.weight(child));
        stack.setWeight(child, 1).remove(child);
        assertEquals(0, stack.weight(child));

        final Component<?> stranger = this.ui.label("C");
        assertThrows(IllegalArgumentException.class, () -> stack.setWeight(stranger, 1));
        stack.add(child);
        assertThrows(IllegalArgumentException.class, () -> stack.setWeight(child, -1));
        assertThrows(IllegalArgumentException.class, () -> stack.setWeight(child, Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> stack.setWeight(child, Float.POSITIVE_INFINITY));
    }
}
