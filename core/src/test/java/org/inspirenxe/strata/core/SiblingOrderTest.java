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
import org.inspirenxe.strata.api.component.Focusable;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.window.Window;
import org.inspirenxe.strata.api.event.input.InputEvents;
import org.inspirenxe.strata.api.extension.ArrangeContext;
import org.inspirenxe.strata.api.extension.CustomComposite;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.layout.AbsolutePanel;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.inspirenxe.strata.api.overlay.Overlay;
import org.inspirenxe.strata.core.node.Node;
import org.inspirenxe.strata.core.paint.CanvasPainter;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sibling order (design 4.33): z indexes, raising and lowering, moving and inserting in the layout order, windows
 * that come to the front when pressed, and raising overlays.
 */
class SiblingOrderTest {
    private final TestHost test = new TestHost(300, 300);
    private final ComponentFactory ui = this.test.factory();
    private final List<String> painted = new ArrayList<>();

    /**
     * A box that writes its name down each time it is painted.
     */
    private Box box(final String name) {
        final Box box = new Box(this.ui, 50, 50);
        box.onEvent(Component.PAINT, event -> this.painted.add(name));
        return box;
    }

    private List<String> paint() {
        this.painted.clear();
        this.test.host().render(new NullCanvas());
        return List.copyOf(this.painted);
    }

    private Component<?> hit(final float x, final float y) {
        this.test.host().pointerMoved(x, y, Modifiers.NONE);
        final Node hovered = this.test.host().root().hovered();
        return hovered == null ? null : hovered.facade();
    }

    private Node content() {
        return this.test.host().root().content();
    }

    @Nested
    class ZIndexAndRaising {
        @Test
        void childrenDrawInTheOrderTheyWereAddedByDefault() {
            final Box a = SiblingOrderTest.this.box("a");
            final Box b = SiblingOrderTest.this.box("b");
            final AbsolutePanel panel = SiblingOrderTest.this.ui.absolutePanel().add(a).add(b);
            SiblingOrderTest.this.test.show(panel);

            assertEquals(0, a.zIndex());
            assertEquals(List.of("a", "b"), SiblingOrderTest.this.paint());
            assertEquals(List.of(a, b), panel.paintOrder());
            assertSame(b, SiblingOrderTest.this.hit(10, 10));
        }

        @Test
        void aHigherZIndexDrawsOverSiblingsAndIsHitFirst() {
            final Box a = SiblingOrderTest.this.box("a");
            final Box b = SiblingOrderTest.this.box("b");
            final Box c = SiblingOrderTest.this.box("c");
            final AbsolutePanel panel = SiblingOrderTest.this.ui.absolutePanel().add(a).add(b).add(c);
            SiblingOrderTest.this.test.show(panel);

            a.setZIndex(1);
            assertEquals(List.of("b", "c", "a"), SiblingOrderTest.this.paint());
            assertSame(a, SiblingOrderTest.this.hit(10, 10));
            c.setZIndex(-1);
            assertEquals(List.of("c", "b", "a"), SiblingOrderTest.this.paint());
            assertEquals(List.of(a, b, c), panel.children(), "the layout order stays");
        }

        @Test
        void raiseAndLowerOrderSiblingsWithTheSameZIndex() {
            final Box a = SiblingOrderTest.this.box("a");
            final Box b = SiblingOrderTest.this.box("b");
            final Box c = SiblingOrderTest.this.box("c");
            final AbsolutePanel panel = SiblingOrderTest.this.ui.absolutePanel().add(a).add(b).add(c);
            SiblingOrderTest.this.test.show(panel);

            panel.raise(a);
            assertEquals(List.of("b", "c", "a"), SiblingOrderTest.this.paint());
            assertSame(a, SiblingOrderTest.this.hit(10, 10));
            panel.lower(c);
            assertEquals(List.of(c, b, a), panel.paintOrder());
            assertSame(a, SiblingOrderTest.this.hit(10, 10));

            b.setZIndex(1);
            panel.raise(c);
            assertEquals(List.of("a", "c", "b"), SiblingOrderTest.this.paint(), "a raised child stays under higher z");
            assertSame(b, SiblingOrderTest.this.hit(10, 10));
            panel.lower(b);
            assertEquals(List.of(a, c, b), panel.paintOrder(), "a lowered child stays over lower z");
            assertEquals(List.of(a, b, c), panel.children());
        }

        @Test
        void raisingAndZIndexesLayNothingOut() {
            final Box a = SiblingOrderTest.this.box("a");
            final Box b = SiblingOrderTest.this.box("b");
            final AbsolutePanel panel = SiblingOrderTest.this.ui.absolutePanel().add(a).add(b);
            SiblingOrderTest.this.test.show(panel);

            panel.raise(a);
            a.setZIndex(3);
            panel.lower(b);
            assertFalse(SiblingOrderTest.this.content().isLayoutDirty());
        }

        @Test
        void aZIndexOrdersSiblingsOnly() {
            final Box inner = SiblingOrderTest.this.box("inner");
            final Box outer = SiblingOrderTest.this.box("outer");
            final AbsolutePanel group = SiblingOrderTest.this.ui.absolutePanel().add(inner);
            SiblingOrderTest.this.test.show(SiblingOrderTest.this.ui.absolutePanel().add(group).add(outer));

            inner.setZIndex(100);
            assertEquals(List.of("inner", "outer"), SiblingOrderTest.this.paint());
            assertSame(outer, SiblingOrderTest.this.hit(10, 10));
        }

        @Test
        void raisingSomethingNotAChildThrows() {
            final AbsolutePanel panel = SiblingOrderTest.this.ui.absolutePanel();
            final Box stranger = SiblingOrderTest.this.box("x");

            assertThrows(IllegalArgumentException.class, () -> panel.raise(stranger));
            assertThrows(IllegalArgumentException.class, () -> panel.lower(stranger));
        }
    }

    @Nested
    class InsertAndMove {
        @Test
        void insertPlacesTheChildInTheLayoutOrderAndDrawsItOnTop() {
            final Box a = SiblingOrderTest.this.box("a");
            final Box b = SiblingOrderTest.this.box("b");
            final Box first = SiblingOrderTest.this.box("first");
            final AbsolutePanel panel = SiblingOrderTest.this.ui.absolutePanel().add(a).add(b);
            SiblingOrderTest.this.test.show(panel);

            panel.insert(0, first);
            assertEquals(List.of(first, a, b), panel.children());
            assertEquals(0, panel.indexOf(first));
            assertEquals(2, panel.indexOf(b));
            assertEquals(List.of("a", "b", "first"), SiblingOrderTest.this.paint());
            assertTrue(first.isAttached());
        }

        @Test
        void insertChecksItsIndexAndRefusesAChildItHas() {
            final Box a = SiblingOrderTest.this.box("a");
            final AbsolutePanel panel = SiblingOrderTest.this.ui.absolutePanel().add(a);

            assertThrows(IndexOutOfBoundsException.class, () -> panel.insert(-1, SiblingOrderTest.this.box("x")));
            assertThrows(IndexOutOfBoundsException.class, () -> panel.insert(2, SiblingOrderTest.this.box("y")));
            assertThrows(IllegalArgumentException.class, () -> panel.insert(0, a));
            panel.insert(1, SiblingOrderTest.this.box("z"));
            assertEquals(2, panel.children().size());
            assertEquals(-1, panel.indexOf(SiblingOrderTest.this.box("w")));
        }

        @Test
        void moveChecksItsArguments() {
            final Box a = SiblingOrderTest.this.box("a");
            final AbsolutePanel panel = SiblingOrderTest.this.ui.absolutePanel().add(a);

            assertThrows(IllegalArgumentException.class, () -> panel.move(SiblingOrderTest.this.box("x"), 0));
            assertThrows(IndexOutOfBoundsException.class, () -> panel.move(a, 1));
            assertThrows(IndexOutOfBoundsException.class, () -> panel.move(a, -1));
        }

        @Test
        void moveKeepsFocusHoverAndTheDrawingOrderAndFiresNothing() {
            final Button a = SiblingOrderTest.this.ui.button("Ana");
            final Button b = SiblingOrderTest.this.ui.button("Ben");
            final Button c = SiblingOrderTest.this.ui.button("Cleo");
            final StackPanel column = SiblingOrderTest.this.ui.stackPanel().add(a).add(b).add(c);
            SiblingOrderTest.this.test.show(column);
            final List<String> events = new ArrayList<>();
            b.onEvent(Focusable.FOCUS_LOST, event -> events.add("focus lost"))
                    .onEvent(Focusable.FOCUS_GAINED, event -> events.add("focus gained"))
                    .onEvent(InputEvents.POINTER_EXIT, event -> events.add("exit"))
                    .onEvent(InputEvents.POINTER_ENTER, event -> events.add("enter"));
            final Rect where = b.bounds();
            SiblingOrderTest.this.test.host().pointerMoved(where.x() + 2, where.y() + 2, Modifiers.NONE);
            b.requestFocus();
            events.clear();

            column.move(b, 0);
            assertEquals(List.of(b, a, c), column.children());
            assertTrue(b.isFocused());
            assertSame(b, SiblingOrderTest.this.test.host().root().hovered().facade(), "b keeps hover");
            assertTrue(b.isAttached());
            assertEquals(List.of(), events);
            assertEquals(List.of(a, b, c), column.paintOrder(), "the drawing order stays");
            assertEquals(List.of(b, a, c), SiblingOrderTest.this.test.tabOrder(), "Tab follows the layout order");
            SiblingOrderTest.this.test.host().layout();
            assertTrue(b.bounds().y() < a.bounds().y(), "the column lays it out first");
        }
    }

    @Nested
    class Windows {
        private Window window(final String title, final float x, final float y, final AbsolutePanel panel) {
            final Window window = SiblingOrderTest.this.ui.window(title);
            window.setSize(100, 80);
            panel.add(window, x, y);
            return window;
        }

        @Test
        void aPressBringsAWindowToTheFront() {
            final AbsolutePanel desk = SiblingOrderTest.this.ui.absolutePanel();
            final Window ana = this.window("Ana", 0, 0, desk);
            final Window ben = this.window("Ben", 50, 40, desk);
            SiblingOrderTest.this.test.show(desk);
            assertTrue(ana.isRaiseOnPress());

            SiblingOrderTest.this.test.click(20, 60);
            assertEquals(List.of(ben, ana), desk.paintOrder());
            assertSame(ana, SiblingOrderTest.this.hit(70, 60));
            assertEquals(List.of(ana, ben), desk.children());
            SiblingOrderTest.this.test.click(140, 110);
            assertEquals(List.of(ana, ben), desk.paintOrder());
        }

        @Test
        void aPressOnAChildRaisesEvenWhenTheChildStopsIt() {
            final AbsolutePanel desk = SiblingOrderTest.this.ui.absolutePanel();
            final Window ana = this.window("Ana", 0, 0, desk);
            final Window ben = this.window("Ben", 50, 40, desk);
            final Box stubborn = new Box(SiblingOrderTest.this.ui, 20, 20);
            stubborn.onEvent(InputEvents.POINTER_PRESS, event -> {
                event.cancel();
                event.stopPropagation();
            });
            ana.add(stubborn);
            SiblingOrderTest.this.test.show(desk);
            desk.raise(ben);

            final Rect where = stubborn.bounds();
            SiblingOrderTest.this.test.click(where.x() + 2, where.y() + 2);
            assertEquals(List.of(ben, ana), desk.paintOrder());
        }

        @Test
        void aWindowThatDoesNotRaiseOnPressStaysWhereItIs() {
            final AbsolutePanel desk = SiblingOrderTest.this.ui.absolutePanel();
            final Window ana = this.window("Ana", 0, 0, desk).setRaiseOnPress(false);
            final Window ben = this.window("Ben", 50, 40, desk);
            SiblingOrderTest.this.test.show(desk);

            SiblingOrderTest.this.test.click(20, 60);
            assertEquals(List.of(ana, ben), desk.paintOrder());
        }

        @Test
        void aRaisedWindowStaysUnderAHigherZIndex() {
            final AbsolutePanel desk = SiblingOrderTest.this.ui.absolutePanel();
            final Window ana = this.window("Ana", 0, 0, desk);
            final Window ben = this.window("Ben", 50, 40, desk);
            ben.setZIndex(1);
            SiblingOrderTest.this.test.show(desk);

            SiblingOrderTest.this.test.click(20, 60);
            assertEquals(List.of(ana, ben), desk.paintOrder());
        }
    }

    @Nested
    class Overlays {
        @Test
        void aRaisedPopupIsDrawnAndHitAboveTheOthers() {
            SiblingOrderTest.this.test.show(SiblingOrderTest.this.ui.absolutePanel());
            final Box first = SiblingOrderTest.this.box("first");
            final Box second = SiblingOrderTest.this.box("second");
            final Overlay one = SiblingOrderTest.this.ui.overlays().popup(first, new Rect(0, 0, 10, 10));
            final Overlay two = SiblingOrderTest.this.ui.overlays().popup(second, new Rect(0, 0, 10, 10));
            assertSame(second, SiblingOrderTest.this.hit(20, 20));

            assertSame(one, one.raise());
            assertEquals(List.of(two, one), SiblingOrderTest.this.ui.overlays().open());
            assertEquals(List.of("second", "first"), SiblingOrderTest.this.paint());
            assertSame(first, SiblingOrderTest.this.hit(20, 20));
        }

        @Test
        void aPopupIsRaisedNoHigherThanTheNextDialog() {
            SiblingOrderTest.this.test.show(SiblingOrderTest.this.ui.absolutePanel());
            final Overlay one = SiblingOrderTest.this.ui.overlays().popup(SiblingOrderTest.this.box("1"), new Rect(0, 0, 10, 10));
            final Overlay two = SiblingOrderTest.this.ui.overlays().popup(SiblingOrderTest.this.box("2"), new Rect(0, 0, 10, 10));
            final Overlay dialog = SiblingOrderTest.this.ui.overlays().dialog(SiblingOrderTest.this.box("dialog"));
            final Overlay three = SiblingOrderTest.this.ui.overlays().popup(SiblingOrderTest.this.box("3"), new Rect(0, 0, 10, 10));
            final Overlay four = SiblingOrderTest.this.ui.overlays().popup(SiblingOrderTest.this.box("4"), new Rect(0, 0, 10, 10));

            one.raise();
            assertEquals(List.of(two, one, dialog, three, four), SiblingOrderTest.this.ui.overlays().open());
            three.raise();
            assertEquals(List.of(two, one, dialog, four, three), SiblingOrderTest.this.ui.overlays().open());
            dialog.raise();
            assertEquals(List.of(two, one, dialog, four, three), SiblingOrderTest.this.ui.overlays().open(),
                    "a dialog keeps its place");
        }

        @Test
        void raisingAClosedOverlayDoesNothing() {
            SiblingOrderTest.this.test.show(SiblingOrderTest.this.ui.absolutePanel());
            final Overlay one = SiblingOrderTest.this.ui.overlays().popup(SiblingOrderTest.this.box("1"), new Rect(0, 0, 10, 10));
            final Overlay two = SiblingOrderTest.this.ui.overlays().popup(SiblingOrderTest.this.box("2"), new Rect(0, 0, 10, 10));
            one.close();

            one.raise();
            assertEquals(List.of(two), SiblingOrderTest.this.ui.overlays().open());
        }
    }

    @Nested
    class AComposite {
        /**
         * A stack of cards, written as a developer would: each card sits a little lower and to the right of the one
         * before it, and the composite orders its cards with the protected child methods.
         */
        static final class Deck extends CustomComposite<Deck> {
            Deck(final ComponentFactory factory) {
                super(factory);
            }

            Deck deal(final Component<?> card) {
                this.addChild(card);
                return this;
            }

            Deck dealFirst(final Component<?> card) {
                this.insertChild(0, card);
                return this;
            }

            void cut(final Component<?> card, final int index) {
                this.moveChild(card, index);
            }

            void show(final Component<?> card) {
                this.raiseChild(card);
            }

            void hide(final Component<?> card) {
                this.lowerChild(card);
            }

            List<Component<?>> cards() {
                return this.childList();
            }

            @Override
            protected Size measure(final MeasureContext context, final float availableWidth,
                                   final float availableHeight) {
                float width = 0f;
                float height = 0f;
                final int count = this.childList().size();
                for (final Component<?> card : this.childList()) {
                    final Size size = context.measure(card, availableWidth, availableHeight);
                    width = Math.max(width, size.width());
                    height = Math.max(height, size.height());
                }
                return new Size(width + 10f * count, height + 10f * count);
            }

            @Override
            protected void arrange(final ArrangeContext context, final Rect bounds) {
                float step = 0f;
                for (final Component<?> card : this.childList()) {
                    final Size size = context.outerSize(card);
                    context.arrange(card, new Rect(bounds.x() + step, bounds.y() + step, size.width(), size.height()));
                    step += 10f;
                }
            }
        }

        @Test
        void aDeveloperCompositeOrdersItsParts() {
            final Box ace = SiblingOrderTest.this.box("ace");
            final Box king = SiblingOrderTest.this.box("king");
            final Box queen = SiblingOrderTest.this.box("queen");
            final Deck deck = new Deck(SiblingOrderTest.this.ui).deal(ace).deal(king);
            SiblingOrderTest.this.test.show(SiblingOrderTest.this.ui.absolutePanel().add(deck));

            deck.dealFirst(queen);
            assertEquals(List.of(queen, ace, king), deck.cards());
            assertEquals(List.of("ace", "king", "queen"), SiblingOrderTest.this.paint());
            assertSame(queen, SiblingOrderTest.this.hit(25, 25));

            deck.show(ace);
            assertEquals(List.of("king", "queen", "ace"), SiblingOrderTest.this.paint());
            assertSame(ace, SiblingOrderTest.this.hit(25, 25));
            deck.hide(ace);
            assertEquals(List.of("ace", "king", "queen"), SiblingOrderTest.this.paint());

            deck.cut(king, 0);
            assertEquals(List.of(king, queen, ace), deck.cards());
            assertEquals(List.of("ace", "king", "queen"), SiblingOrderTest.this.paint(), "moving keeps the drawing order");
            SiblingOrderTest.this.test.host().layout();
            assertEquals(0f, king.bounds().x() - deck.bounds().x(), "the deck lays it out first");
        }
    }

    @Nested
    class Allocation {
        @Test
        void paintingAndHitTestingAllocateNothingOnceSorted() {
            final AbsolutePanel panel = SiblingOrderTest.this.ui.absolutePanel();
            for (int i = 0; i < 20; i++) {
                final Box box = new Box(SiblingOrderTest.this.ui, 40, 40).setZIndex(i % 3 - 1);
                panel.add(box, i * 5f, i * 5f);
            }
            SiblingOrderTest.this.test.show(panel);
            panel.raise(panel.children().get(3));
            panel.lower(panel.children().get(15));
            final Node root = SiblingOrderTest.this.content();
            final CanvasPainter painter = new CanvasPainter();
            final NullCanvas canvas = new NullCanvas();
            for (int i = 0; i < 200; i++) {
                root.paint(painter.begin(canvas, false));
                root.hitTest(30, 30);
            }

            // What reading the counter costs on its own, which the measurement subtracts
            final com.sun.management.ThreadMXBean threads =
                    (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
            final long start = threads.getCurrentThreadAllocatedBytes();
            final long overhead = threads.getCurrentThreadAllocatedBytes() - start;

            final int frames = 1000;
            final long before = threads.getCurrentThreadAllocatedBytes();
            for (int i = 0; i < frames; i++) {
                root.paint(painter.begin(canvas, false));
                root.hitTest(30, 30);
            }
            final long allocated = threads.getCurrentThreadAllocatedBytes() - before - overhead;
            // A single object per frame would be at least 16 bytes a frame; the JVM's own bookkeeping is far less
            assertTrue(allocated < frames, allocated + " bytes allocated over " + frames + " frames");
        }
    }
}
