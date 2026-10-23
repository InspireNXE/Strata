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
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.input.ScrollKind;
import org.inspirenxe.strata.api.layout.AbsolutePanel;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.FlowDirection;
import org.inspirenxe.strata.api.layout.GridLength;
import org.inspirenxe.strata.api.layout.GridPanel;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.api.layout.ScrollPanel;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.inspirenxe.strata.api.layout.VerticalAlignment;
import org.inspirenxe.strata.api.layout.WrapPanel;
import org.inspirenxe.strata.core.text.Translations;
import org.inspirenxe.strata.spi.Translator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Right-to-left flow in layout (design 4.49): each panel lays out as for left to right, and a right-to-left parent
 * mirrors its children inside its bounds.
 */
class FlowDirectionTest {
    private final TestHost test = new TestHost(200, 100);
    private final ComponentFactory ui = this.test.factory();

    @AfterEach
    void resetTranslator() {
        Translations.setTranslator(Translator.NONE);
    }

    @Nested
    class Directions {
        @Test
        void languagesWrittenRightToLeftAreKnown() {
            assertEquals(FlowDirection.RIGHT_TO_LEFT, FlowDirection.of(Locale.forLanguageTag("ar-EG")));
            assertEquals(FlowDirection.RIGHT_TO_LEFT, FlowDirection.of(Locale.forLanguageTag("he")));
            assertEquals(FlowDirection.RIGHT_TO_LEFT, FlowDirection.of(Locale.forLanguageTag("fa")));
            assertEquals(FlowDirection.RIGHT_TO_LEFT, FlowDirection.of(Locale.forLanguageTag("az-Arab")));
            assertEquals(FlowDirection.LEFT_TO_RIGHT, FlowDirection.of(Locale.forLanguageTag("az")));
            assertEquals(FlowDirection.LEFT_TO_RIGHT, FlowDirection.of(Locale.forLanguageTag("ar-Latn")));
            assertEquals(FlowDirection.LEFT_TO_RIGHT, FlowDirection.of(Locale.ENGLISH));
        }

        @Test
        void aComponentInheritsItsParentsDirection() {
            final Box box = new Box(FlowDirectionTest.this.ui, 10, 10);
            final StackPanel inner = FlowDirectionTest.this.ui.stackPanel().add(box);
            final StackPanel outer = FlowDirectionTest.this.ui.stackPanel().add(inner);
            assertEquals(FlowDirection.LEFT_TO_RIGHT, box.flowDirection());
            outer.setFlowDirection(FlowDirection.RIGHT_TO_LEFT);
            assertEquals(FlowDirection.RIGHT_TO_LEFT, box.flowDirection());
            inner.setFlowDirection(FlowDirection.LEFT_TO_RIGHT);
            assertEquals(FlowDirection.LEFT_TO_RIGHT, box.flowDirection());
            inner.setFlowDirection(null);
            assertEquals(FlowDirection.RIGHT_TO_LEFT, box.flowDirection());
        }

        @Test
        void theScreenFollowsTheLanguage() {
            final LocaleTranslator translator = new LocaleTranslator(Locale.forLanguageTag("ar"));
            Translations.setTranslator(translator);
            final Box box = new Box(FlowDirectionTest.this.ui, 20, 10);
            final StackPanel row = FlowDirectionTest.this.ui.stackPanel(Orientation.HORIZONTAL).add(box);
            row.setAnchor(Anchor.FILL);
            FlowDirectionTest.this.test.show(row);
            assertEquals(FlowDirection.RIGHT_TO_LEFT, row.flowDirection());
            assertEquals(new Rect(180, 0, 20, 10), box.bounds());

            translator.set(Locale.ENGLISH, Map.of());
            FlowDirectionTest.this.test.host().layout();
            assertEquals(new Rect(0, 0, 20, 10), box.bounds());
        }
    }

    @Nested
    class Panels {
        @Test
        void aRowStartsAtTheRight() {
            final Box a = new Box(FlowDirectionTest.this.ui, 20, 10);
            final Box b = new Box(FlowDirectionTest.this.ui, 30, 10).setMargin(5, 0, 0, 0);
            final StackPanel row = FlowDirectionTest.this.ui.stackPanel(Orientation.HORIZONTAL).setSpacing(4)
                    .add(a).add(b);
            row.setPadding(10, 0).setAnchor(Anchor.FILL).setFlowDirection(FlowDirection.RIGHT_TO_LEFT);
            FlowDirectionTest.this.test.show(row);
            // Left to right: a at 10, b at 10 + 20 + 4 + 5 = 39; mirrored inside 0 to 200
            assertEquals(new Rect(170, 0, 20, 10), a.bounds());
            assertEquals(new Rect(131, 0, 30, 10), b.bounds());
        }

        @Test
        void leftAndRightAlignmentMeanTheStartAndTheEnd() {
            final Box start = new Box(FlowDirectionTest.this.ui, 20, 10);
            final Box end = new Box(FlowDirectionTest.this.ui, 20, 10)
                    .setAlignment(HorizontalAlignment.RIGHT, VerticalAlignment.TOP);
            final StackPanel column = FlowDirectionTest.this.ui.stackPanel().add(start).add(end);
            column.setAnchor(Anchor.FILL).setFlowDirection(FlowDirection.RIGHT_TO_LEFT);
            FlowDirectionTest.this.test.show(column);
            assertEquals(new Rect(180, 0, 20, 10), start.bounds());
            assertEquals(new Rect(0, 10, 20, 10), end.bounds());
        }

        @Test
        void gridColumnsRunFromTheRight() {
            final Box first = new Box(FlowDirectionTest.this.ui, 10, 10);
            final Box second = new Box(FlowDirectionTest.this.ui, 10, 10).setAnchor(Anchor.FILL);
            final GridPanel grid = FlowDirectionTest.this.ui.gridPanel()
                    .columns(GridLength.fixed(50), GridLength.weighted(1)).rows(GridLength.fixed(20));
            grid.add(first, 0, 0).add(second, 0, 1);
            grid.setAnchor(Anchor.FILL).setFlowDirection(FlowDirection.RIGHT_TO_LEFT);
            FlowDirectionTest.this.test.show(grid);
            assertEquals(new Rect(190, 0, 10, 10), first.bounds());
            assertEquals(new Rect(0, 0, 150, 20), second.bounds());
        }

        @Test
        void wrappedRowsStartAtTheRight() {
            final Box a = new Box(FlowDirectionTest.this.ui, 120, 10);
            final Box b = new Box(FlowDirectionTest.this.ui, 120, 10);
            final WrapPanel wrap = FlowDirectionTest.this.ui.wrapPanel().add(a).add(b);
            wrap.setAnchor(Anchor.FILL).setFlowDirection(FlowDirection.RIGHT_TO_LEFT);
            FlowDirectionTest.this.test.show(wrap);
            assertEquals(new Rect(80, 0, 120, 10), a.bounds());
            assertEquals(new Rect(80, 10, 120, 10), b.bounds());
        }

        @Test
        void absolutePositionsAreMirrored() {
            final Box box = new Box(FlowDirectionTest.this.ui, 30, 10);
            final AbsolutePanel panel = FlowDirectionTest.this.ui.absolutePanel().add(box, 15, 5);
            panel.setAnchor(Anchor.FILL).setFlowDirection(FlowDirection.RIGHT_TO_LEFT);
            FlowDirectionTest.this.test.show(panel);
            assertEquals(new Rect(155, 5, 30, 10), box.bounds());
        }

        @Test
        void aLeftToRightChildKeepsItsOwnOrderInsideAMirroredSlot() {
            final Box a = new Box(FlowDirectionTest.this.ui, 20, 10);
            final Box b = new Box(FlowDirectionTest.this.ui, 20, 10);
            final StackPanel inner = FlowDirectionTest.this.ui.stackPanel(Orientation.HORIZONTAL).add(a).add(b);
            inner.setFlowDirection(FlowDirection.LEFT_TO_RIGHT);
            final StackPanel outer = FlowDirectionTest.this.ui.stackPanel(Orientation.HORIZONTAL).add(inner);
            outer.setAnchor(Anchor.FILL).setFlowDirection(FlowDirection.RIGHT_TO_LEFT);
            FlowDirectionTest.this.test.show(outer);
            assertEquals(new Rect(160, 0, 40, 10), inner.bounds());
            assertEquals(new Rect(160, 0, 20, 10), a.bounds());
            assertEquals(new Rect(180, 0, 20, 10), b.bounds());
        }

        @Test
        void nestedRightToLeftPanelsMirrorOnceEach() {
            final Box a = new Box(FlowDirectionTest.this.ui, 20, 10);
            final Box b = new Box(FlowDirectionTest.this.ui, 20, 10);
            final StackPanel inner = FlowDirectionTest.this.ui.stackPanel(Orientation.HORIZONTAL).add(a).add(b);
            final StackPanel outer = FlowDirectionTest.this.ui.stackPanel(Orientation.HORIZONTAL).add(inner);
            outer.setAnchor(Anchor.FILL).setFlowDirection(FlowDirection.RIGHT_TO_LEFT);
            FlowDirectionTest.this.test.show(outer);
            assertEquals(new Rect(160, 0, 40, 10), inner.bounds());
            assertEquals(new Rect(180, 0, 20, 10), a.bounds());
            assertEquals(new Rect(160, 0, 20, 10), b.bounds());
        }

        @Test
        void changingTheDirectionLaysOutAgain() {
            final Box box = new Box(FlowDirectionTest.this.ui, 20, 10);
            final StackPanel row = FlowDirectionTest.this.ui.stackPanel(Orientation.HORIZONTAL).add(box);
            row.setAnchor(Anchor.FILL);
            FlowDirectionTest.this.test.show(row);
            assertEquals(0f, box.bounds().x());
            row.setFlowDirection(FlowDirection.RIGHT_TO_LEFT);
            FlowDirectionTest.this.test.host().layout();
            assertEquals(180f, box.bounds().x());
            row.setFlowDirection(FlowDirection.LEFT_TO_RIGHT);
            FlowDirectionTest.this.test.host().layout();
            assertEquals(0f, box.bounds().x());
        }

        @Test
        void thePointerFindsTheMirroredChild() {
            final Box box = new Box(FlowDirectionTest.this.ui, 20, 10);
            final AbsolutePanel panel = FlowDirectionTest.this.ui.absolutePanel().add(box, 0, 0);
            panel.setAnchor(Anchor.FILL).setFlowDirection(FlowDirection.RIGHT_TO_LEFT);
            FlowDirectionTest.this.test.show(panel);
            FlowDirectionTest.this.test.host().pointerButton(190, 5, MouseButton.LEFT,
                    true, Modifiers.NONE);
            assertTrue(box.pressed());
        }

        @Test
        void anOverlayFollowsTheScreensRoot() {
            final StackPanel root = FlowDirectionTest.this.ui.stackPanel();
            root.setFlowDirection(FlowDirection.RIGHT_TO_LEFT);
            FlowDirectionTest.this.test.show(root);
            final Box shown = new Box(FlowDirectionTest.this.ui, 10, 10);
            final StackPanel content = FlowDirectionTest.this.ui.stackPanel().add(shown);
            assertEquals(FlowDirection.LEFT_TO_RIGHT, content.flowDirection());
            FlowDirectionTest.this.ui.overlays().dialog(content);
            assertSame(FlowDirection.RIGHT_TO_LEFT, shown.flowDirection());
        }
    }

    @Nested
    class Scrolling {
        private ScrollPanel panel;
        private Box content;

        private void show() {
            this.content = new Box(FlowDirectionTest.this.ui, 400, 300);
            this.panel = FlowDirectionTest.this.ui.scrollPanel().add(this.content);
            this.panel.setPolicy(ScrollPanel.ScrollPolicy.AUTO, ScrollPanel.ScrollPolicy.AUTO);
            this.panel.setAnchor(Anchor.FILL).setFlowDirection(FlowDirection.RIGHT_TO_LEFT);
            FlowDirectionTest.this.test.show(this.panel);
        }

        @Test
        void theViewportAndBarsAreMirrored() {
            this.show();
            final float gutter = ScrollPanel.BAR_WIDTH + ScrollPanel.BAR_GAP;
            assertEquals(new Rect(gutter, 0, 200 - gutter, 100 - gutter), this.panel.viewport());
            // At an offset of 0 the content's right edge, its start, meets the viewport's
            assertEquals(200f, this.content.bounds().right());
            this.panel.setScroll(this.panel.maxScrollX(), 0);
            FlowDirectionTest.this.test.host().layout();
            assertEquals(gutter, this.content.bounds().x());
        }

        @Test
        void aWheelTurnedLeftScrollsTowardTheEnd() {
            this.show();
            // A positive sideways scroll turns toward the left, which is the end right to left
            FlowDirectionTest.this.test.host().scroll(100, 40, 1, 0, ScrollKind.WHEEL, Modifiers.NONE);
            assertTrue(this.panel.scrollX() > 0f);
        }

        @Test
        void scrollingAMirroredChildIntoViewMovesTowardIt() {
            this.show();
            // The content's left part is its end, so showing it scrolls to the largest offset
            this.panel.scrollToVisible(new Rect(this.content.bounds().x(), 0, 10, 10));
            FlowDirectionTest.this.test.host().layout();
            assertEquals(this.panel.maxScrollX(), this.panel.scrollX());
        }
    }
}
