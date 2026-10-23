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
import org.inspirenxe.strata.api.component.registry.ComponentId;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.AnchorPanel;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.api.layout.VerticalAlignment;
import org.inspirenxe.strata.api.layout.WrapPanel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * How a wrap panel breaks its children into lines and places them. Text in the test platform is 6 units per code
 * point and 9 per line, so a four-letter label is 24 by 9.
 */
class WrapPanelTest {
    private final TestHost test = new TestHost(100, 100);
    private final ComponentFactory ui = this.test.factory();

    private WrapPanel filling(final WrapPanel wrap) {
        wrap.setAnchor(Anchor.FILL);
        this.test.show(wrap);
        return wrap;
    }

    private Label chip() {
        return this.ui.label("Chip");
    }

    @Test
    void aRowWrapsWhenTheNextChildWouldPassTheEnd() {
        final Label a = this.chip();
        final Label b = this.chip();
        final Label c = this.chip();
        final Label d = this.chip();
        this.filling(this.ui.wrapPanel().setSpacing(4, 2).add(a).add(b).add(c).add(d));

        assertEquals(new Rect(0, 0, 24, 9), a.bounds());
        assertEquals(new Rect(28, 0, 24, 9), b.bounds());
        assertEquals(new Rect(56, 0, 24, 9), c.bounds());
        assertEquals(new Rect(0, 11, 24, 9), d.bounds());
    }

    @Test
    void aRowIsAsTallAsItsTallestChildAndChildrenAlignInIt() {
        final Label tall = this.ui.label("A\nB");
        final Label middle = this.ui.label("M");
        middle.setAlignment(HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE);
        final Label bottom = this.ui.label("B");
        bottom.setAlignment(HorizontalAlignment.LEFT, VerticalAlignment.BOTTOM);
        final Label next = this.ui.label("01234567890123");
        this.filling(this.ui.wrapPanel().add(tall).add(middle).add(bottom).add(next));

        assertEquals(new Rect(0, 0, 6, 18), tall.bounds());
        assertEquals(new Rect(6, 4.5f, 6, 9), middle.bounds());
        assertEquals(new Rect(12, 9, 6, 9), bottom.bounds());
        assertEquals(new Rect(0, 18, 84, 9), next.bounds());
    }

    @Test
    void theLineAlignmentPlacesEachRowsChildren() {
        final Label a = this.chip();
        final Label b = this.chip();
        final Label c = this.chip();
        final WrapPanel wrap = this.filling(this.ui.wrapPanel().setSpacing(4, 0).add(a).add(b).add(c));

        wrap.setLineAlignment(HorizontalAlignment.CENTER);
        this.test.host().layout();
        assertEquals(10, a.bounds().x());
        assertEquals(66, c.bounds().x());

        wrap.setLineAlignment(HorizontalAlignment.RIGHT);
        this.test.host().layout();
        assertEquals(20, a.bounds().x());
        assertEquals(76, c.bounds().x());

        wrap.setLineAlignment(HorizontalAlignment.STRETCH);
        this.test.host().layout();
        assertEquals(0, a.bounds().x());
        assertEquals(38, b.bounds().x());
        assertEquals(76, c.bounds().x());
    }

    @Test
    void aSpreadRowOfOneStaysAtTheStart() {
        final Label only = this.chip();
        this.filling(this.ui.wrapPanel().setLineAlignment(HorizontalAlignment.STRETCH).add(only));

        assertEquals(0, only.bounds().x());
    }

    @Test
    void aChildWiderThanThePanelTakesALineOfItsOwn() {
        final Label a = this.chip();
        final Label wide = this.ui.label("01234567890123456789");
        final Label b = this.chip();
        this.filling(this.ui.wrapPanel().add(a).add(wide).add(b));

        assertEquals(new Rect(0, 0, 24, 9), a.bounds());
        assertEquals(new Rect(0, 9, 120, 9), wide.bounds());
        assertEquals(new Rect(0, 18, 24, 9), b.bounds());
    }

    @Test
    void columnsFillTopToBottom() {
        final AnchorPanel a = this.ui.anchorPanel();
        a.setSize(10, 40);
        final AnchorPanel b = this.ui.anchorPanel();
        b.setSize(20, 40);
        final AnchorPanel c = this.ui.anchorPanel();
        c.setSize(10, 40);
        this.filling(this.ui.wrapPanel(Orientation.VERTICAL).setSpacing(5, 3).add(a).add(b).add(c));

        assertEquals(new Rect(0, 0, 10, 40), a.bounds());
        assertEquals(new Rect(0, 45, 20, 40), b.bounds());
        assertEquals(new Rect(23, 0, 10, 40), c.bounds());
    }

    @Test
    void thePanelAsksForItsLongestLineAndItsLines() {
        final WrapPanel wrap = this.ui.wrapPanel().setSpacing(4, 2).add(this.chip()).add(this.chip())
                .add(this.chip()).add(this.chip());
        wrap.setPadding(1);
        this.test.show(wrap);

        assertEquals(82, wrap.desiredWidth());
        assertEquals(22, wrap.desiredHeight());
    }

    @Test
    void withUnlimitedWidthEveryChildIsInOneRow() {
        final WrapPanel wrap = this.ui.wrapPanel().setSpacing(4, 2);
        for (int i = 0; i < 5; i++) {
            wrap.add(this.chip());
        }
        this.test.show(this.ui.stackPanel(Orientation.HORIZONTAL).add(wrap));

        assertEquals(136, wrap.desiredWidth());
        assertEquals(9, wrap.desiredHeight());
    }

    @Test
    void hiddenChildrenTakeNoSpaceAndNoGap() {
        final Label a = this.chip();
        final Label hidden = this.chip().setVisible(false);
        final Label b = this.chip();
        this.filling(this.ui.wrapPanel().setSpacing(4, 0).add(a).add(hidden).add(b));

        assertEquals(28, b.bounds().x());
    }

    @Test
    void itsMinimumIsItsWidestChildsPlusThePadding() {
        final Label word = this.ui.label("aaaa bbbbbb").setWrap(true);
        final WrapPanel wrap = this.ui.wrapPanel().add(word).add(this.chip());
        wrap.setPadding(new Insets(1, 2, 3, 4));
        wrap.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.STRETCH);
        final AnchorPanel body = this.ui.anchorPanel().setAnchor(Anchor.FILL);
        this.test.show(this.ui.stackPanel(Orientation.HORIZONTAL).add(wrap, 1).add(body, 1)
                .setSize(30, 100));

        // The longest word is 36 wide, which the panel keeps although its share is 15
        assertEquals(40, wrap.bounds().width());
    }

    @Test
    void spacingMustBeFinite() {
        assertThrows(IllegalArgumentException.class, () -> this.ui.wrapPanel().setSpacing(Float.NaN, 0));
        assertThrows(IllegalArgumentException.class, () -> this.ui.wrapPanel().setSpacing(0, Float.POSITIVE_INFINITY));
    }

    @Test
    void itIsRegistered() {
        assertInstanceOf(WrapPanel.class, this.ui.create(ComponentId.of("strata", "wrap_panel")));
        assertInstanceOf(WrapPanel.class, this.ui.create(WrapPanel.class));
    }
}
