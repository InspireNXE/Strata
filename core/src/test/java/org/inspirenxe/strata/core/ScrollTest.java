package org.inspirenxe.strata.core;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.control.collection.ListView;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.geom.Size;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.input.ScrollKind;
import org.inspirenxe.strata.api.layout.AbsolutePanel;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.api.layout.ScrollPanel;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Scroll panels, lists and incremental layout. Text in the test platform is 6 units per code point and 9 per line.
 */
class ScrollTest {
    private final TestHost test = new TestHost(300, 300);
    private final ComponentFactory ui = this.test.factory();

    /**
     * Counts how often it is measured.
     */
    static final class Counted extends CustomComponent<Counted> {
        final float width;
        final float height;
        int measures;

        Counted(final ComponentFactory ui, final float width, final float height) {
            super(ui);
            this.width = width;
            this.height = height;
        }

        @Override
        protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
            this.measures++;
            return new Size(this.width, this.height);
        }
    }

    private boolean wheel(final float x, final float y, final float notches) {
        return this.test.host().scroll(x, y, 0f, notches, ScrollKind.WHEEL, Modifiers.NONE);
    }

    @Nested
    class Panels {
        private ScrollPanel panel;
        private Counted content;

        private void show() {
            this.content = new Counted(ScrollTest.this.ui, 50, 300);
            this.panel = ScrollTest.this.ui.scrollPanel().add(this.content);
            this.panel.setSize(100, 100);
            final AbsolutePanel root = ScrollTest.this.ui.absolutePanel().add(this.panel, 10, 10);
            ScrollTest.this.test.show(root);
        }

        @Test
        void contentTallerThanThePanelScrolls() {
            this.show();
            assertEquals(200f, this.panel.maxScrollY());
            assertEquals(new Rect(10, 10, 100 - ScrollPanel.BAR_WIDTH - ScrollPanel.BAR_GAP, 100), this.panel.viewport());
            assertTrue(ScrollTest.this.wheel(30, 30, -1));
            ScrollTest.this.test.host().layout();
            assertEquals(10f, this.panel.scrollY());
            assertEquals(0f, this.content.bounds().y());
        }

        @Test
        void scrollingMovesWithoutMeasuringAgain() {
            this.show();
            final int measured = this.content.measures;
            this.panel.setScroll(0, 120);
            ScrollTest.this.test.host().layout();
            assertEquals(measured, this.content.measures);
            assertEquals(10f - 120f, this.content.bounds().y());
        }

        @Test
        void aScrollPastTheEndIsNotConsumed() {
            this.show();
            this.panel.setScroll(0, 1000);
            ScrollTest.this.test.host().layout();
            assertEquals(200f, this.panel.scrollY());
            assertFalse(ScrollTest.this.wheel(30, 30, -1));
            assertTrue(ScrollTest.this.wheel(30, 30, 1));
        }

        @Test
        void listenersHearInputAndSetters() {
            this.show();
            final List<Float> heard = new ArrayList<>();
            this.panel.onScrollChanged(state -> heard.add(state.scrollY()));
            ScrollTest.this.wheel(30, 30, -2);
            this.panel.setScroll(0, 50);
            this.panel.setScroll(0, 50);
            assertEquals(List.of(20f, 50f), heard);
        }

        @Test
        void keysScrollWhenNothingFocusedConsumesThem() {
            this.show();
            this.panel.setFocusable(true);
            this.panel.requestFocus();
            ScrollTest.this.test.host().key(Key.END, KeyAction.PRESS, Modifiers.NONE);
            assertEquals(200f, this.panel.scrollY());
            ScrollTest.this.test.host().key(Key.UP, KeyAction.PRESS, Modifiers.NONE);
            assertEquals(190f, this.panel.scrollY());
        }

        @Test
        void draggingTheThumbScrolls() {
            this.show();
            final float barX = 10 + 100 - ScrollPanel.BAR_WIDTH / 2f;
            ScrollTest.this.test.host().pointerButton(barX, 15, MouseButton.LEFT, true, Modifiers.NONE);
            ScrollTest.this.test.host().pointerMoved(barX, 15 + 34, Modifiers.NONE);
            ScrollTest.this.test.host().pointerButton(barX, 15 + 34, MouseButton.LEFT, false, Modifiers.NONE);
            // The thumb is a third of the track (100 * 100 / 300, raised to 33.3); 34 units of 66.7 of travel
            assertEquals(200f * 34f / (100f - 100f / 3f), this.panel.scrollY(), 0.01f);
        }

        @Test
        void focusingADescendantScrollsItIntoView() {
            final TextBox box = ScrollTest.this.ui.textBox();
            final StackPanel column = ScrollTest.this.ui.stackPanel().add(new Box(ScrollTest.this.ui, 10, 250)).add(box);
            this.panel = ScrollTest.this.ui.scrollPanel().add(column);
            this.panel.setSize(150, 100);
            ScrollTest.this.test.show(ScrollTest.this.ui.absolutePanel().add(this.panel, 0, 0));
            box.requestFocus();
            ScrollTest.this.test.host().layout();
            assertEquals(box.bounds().bottom(), this.panel.viewport().bottom(), 0.01f);
        }

        @Test
        void childrenOutsideTheViewportCannotBeHit() {
            this.show();
            ScrollTest.this.test.host().pointerMoved(108, 50, Modifiers.NONE);
            assertSame(this.panel, ScrollTest.this.test.host().hovered().orElseThrow());
            ScrollTest.this.test.host().pointerMoved(30, 50, Modifiers.NONE);
            assertSame(this.content, ScrollTest.this.test.host().hovered().orElseThrow());
        }
    }

    @Nested
    class Lists {
        private final List<Integer> built = new ArrayList<>();
        private ListView<Integer> list;

        private void show(final int count) {
            this.list = ScrollTest.this.ui.<Integer>listView(item -> {
                this.built.add(item);
                return new Box(ScrollTest.this.ui, 20, 10);
            }).setRowHeight(20);
            this.list.setItems(IntStream.range(0, count).boxed().toList());
            this.list.setSize(100, 100);
            ScrollTest.this.test.show(ScrollTest.this.ui.absolutePanel().add(this.list, 0, 0));
            ScrollTest.this.test.host().layout();
        }

        @Test
        void onlyRowsInViewAreBuilt() {
            this.show(1000);
            assertEquals(List.of(0, 1, 2, 3, 4, 5), this.built.stream().sorted().toList());
        }

        @Test
        void scrollingBuildsTheRowsThatComeIntoView() {
            this.show(1000);
            this.built.clear();
            ScrollTest.this.wheel(10, 10, -3);
            ScrollTest.this.test.host().layout();
            assertEquals(60f, this.list.scrollY());
            assertEquals(List.of(6, 7, 8), this.built.stream().sorted().toList());
        }

        @Test
        void aPressSelectsAndADoubleClickActivates() {
            this.show(10);
            final List<Integer> activated = new ArrayList<>();
            this.list.onActivate(activated::add);
            ScrollTest.this.test.click(10, 45);
            assertEquals(Optional.of(2), this.list.selected());
            ScrollTest.this.test.click(10, 45);
            assertEquals(List.of(2), activated);
        }

        @Test
        void keysMoveTheSelectionAndKeepItInView() {
            this.show(50);
            this.list.requestFocus();
            for (int i = 0; i < 8; i++) {
                ScrollTest.this.test.host().key(Key.DOWN, KeyAction.PRESS, Modifiers.NONE);
            }
            ScrollTest.this.test.host().layout();
            assertEquals(7, this.list.selectedIndex());
            assertEquals(60f, this.list.scrollY());
        }

        @Test
        void newItemsKeepTheSelectionByKey() {
            this.show(10);
            this.list.setKeyFunction(item -> item % 100);
            this.list.select(4);
            final List<Optional<Integer>> heard = new ArrayList<>();
            this.list.onSelectionChanged(heard::add);
            this.list.setItems(List.of(9, 8, 104, 7));
            assertEquals(2, this.list.selectedIndex());
            assertEquals(List.of(Optional.of(104)), heard);
            this.list.setItems(List.of(1, 2));
            assertEquals(-1, this.list.selectedIndex());
        }

        @Test
        void refreshBuildsNewCells() {
            this.show(3);
            final List<Integer> before = List.copyOf(this.built);
            this.list.refresh();
            ScrollTest.this.test.host().layout();
            assertEquals(before.size() * 2, this.built.size());
            assertNotSame(before, this.built);
        }
    }

    @Nested
    class Incremental {
        @Test
        void aChangeMeasuresOnlyWhatItAffects() {
            final Counted left = new Counted(ScrollTest.this.ui, 20, 20);
            final Counted right = new Counted(ScrollTest.this.ui, 20, 20);
            final AbsolutePanel root = ScrollTest.this.ui.absolutePanel().add(left, 0, 0).add(right, 100, 0);
            ScrollTest.this.test.show(root);
            final int rightMeasures = right.measures;
            left.invalidateLayout();
            ScrollTest.this.test.host().layout();
            assertEquals(rightMeasures, right.measures);
            assertTrue(left.measures > 1);
        }

        @Test
        void anOffsetMovesOnlyThatComponent() {
            final Box left = new Box(ScrollTest.this.ui, 20, 20);
            final Box right = new Box(ScrollTest.this.ui, 20, 20);
            final StackPanel row = ScrollTest.this.ui.stackPanel(Orientation.HORIZONTAL)
                    .add(left).add(right);
            ScrollTest.this.test.show(row);
            left.setOffset(5, 7);
            ScrollTest.this.test.host().layout();
            assertEquals(new Rect(5, 7, 20, 20), left.bounds());
            assertEquals(new Rect(20, 0, 20, 20), right.bounds());
        }
    }
}
