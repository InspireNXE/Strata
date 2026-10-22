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
import org.inspirenxe.strata.api.control.attribute.item.Activatable;
import org.inspirenxe.strata.api.control.attribute.item.MultiSelection;
import org.inspirenxe.strata.api.control.attribute.item.Selection;
import org.inspirenxe.strata.api.control.attribute.item.SelectionMode;
import org.inspirenxe.strata.api.control.attribute.popup.Closable;
import org.inspirenxe.strata.api.control.collection.ComboBox;
import org.inspirenxe.strata.api.control.collection.GridView;
import org.inspirenxe.strata.api.control.collection.ListView;
import org.inspirenxe.strata.api.control.collection.TableColumn;
import org.inspirenxe.strata.api.control.collection.TableView;
import org.inspirenxe.strata.api.control.collection.TreeView;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifier;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.layout.AbsolutePanel;
import org.inspirenxe.strata.api.layout.GridLength;
import org.inspirenxe.strata.api.layout.ScrollPanel;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Themes;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Edge cases of the collection controls: offsets set before a layout, selections across changed items, handlers that
 * change a control while it reports, and callbacks that throw.
 */
class CollectionEdgeCaseTest {
    private final TestHost test = new TestHost(300, 300);
    private final ComponentFactory ui = this.test.factory();
    private final AbsolutePanel panel = this.ui.absolutePanel();

    private static List<Integer> numbers(final int count) {
        return IntStream.range(0, count).boxed().toList();
    }

    @Nested
    class ScrollOffsets {
        private ListView<Integer> list(final int count) {
            final ListView<Integer> list = CollectionEdgeCaseTest.this.ui.<Integer>listView().setItems(numbers(count));
            list.setSize(100, 100);
            return list;
        }

        @Test
        void anOffsetSetBeforeTheFirstLayoutIsKept() {
            final ListView<Integer> list = this.list(50);
            list.setScrollY(300);
            assertEquals(300f, list.scrollY());
            CollectionEdgeCaseTest.this.test.show(CollectionEdgeCaseTest.this.panel.add(list, 0, 0));
            assertEquals(300f, list.scrollY());
        }

        @Test
        void anOffsetSetRightAfterNewItemsIsKept() {
            final ListView<Integer> list = this.list(10);
            CollectionEdgeCaseTest.this.test.show(CollectionEdgeCaseTest.this.panel.add(list, 0, 0));
            list.setItems(numbers(50));
            list.setScrollY(500);
            assertEquals(500f, list.scrollY());
            CollectionEdgeCaseTest.this.test.host().layout();
            assertEquals(500f, list.scrollY());
        }

        @Test
        void anOffsetIsClampedToTheRowsAtOnceOnceLaidOut() {
            final ListView<Integer> list = this.list(10);
            CollectionEdgeCaseTest.this.test.show(CollectionEdgeCaseTest.this.panel.add(list, 0, 0));
            list.setScrollY(1000);
            assertEquals(100f, list.scrollY());
            list.setScrollY(-5);
            assertEquals(0f, list.scrollY());
        }

        @Test
        void aTableKeepsAnOffsetSetBeforeItsFirstLayout() {
            final TableView<Integer> table = CollectionEdgeCaseTest.this.ui.<Integer>tableView()
                    .addTextColumn("N", GridLength.weighted(1f), n -> n)
                    .setItems(numbers(50));
            table.setSize(100, 120);
            table.setScrollY(240);
            CollectionEdgeCaseTest.this.test.show(CollectionEdgeCaseTest.this.panel.add(table, 0, 0));
            assertEquals(240f, table.scrollY());
        }
    }

    @Nested
    class TreeKeys {
        private final List<String> asked = new ArrayList<>();
        private TreeView<String> tree;

        private void show() {
            final ComponentFactory ui = CollectionEdgeCaseTest.this.ui;
            this.tree = ui.<String>treeView()
                    .setChildren(name -> name.equals("Ana") ? List.of("Ana's notes", "Ana's mail") : List.of())
                    .setItems(List.of("Ana", "Ben"))
                    .setSelectionMode(SelectionMode.MULTIPLE)
                    .onEvent(Selection.SELECT, e -> this.asked.add("select " + e.value()))
                    .onEvent(MultiSelection.SELECT_MANY, e -> this.asked.add("many " + e.value()));
            this.tree.setSize(200, 200);
            this.tree.setExpanded("Ana", true);
            CollectionEdgeCaseTest.this.test.show(CollectionEdgeCaseTest.this.panel.add(this.tree, 0, 0));
            CollectionEdgeCaseTest.this.test.click(50, 10);
            this.asked.clear();
        }

        private void key(final Key key, final Modifiers modifiers) {
            CollectionEdgeCaseTest.this.test.host().key(key, KeyAction.PRESS, modifiers);
        }

        @Test
        void rightAndLeftAskForTheSetInMultipleMode() {
            this.show();
            this.key(Key.RIGHT, Modifiers.NONE);
            assertEquals(List.of(1), this.tree.selectedIndexes());
            this.key(Key.LEFT, Modifiers.NONE);
            assertEquals(List.of(0), this.tree.selectedIndexes());
            assertEquals(List.of("many [1]", "many [0]"), this.asked);
        }

        @Test
        void shiftWithRightExtendsTheSetLikeShiftWithDown() {
            this.show();
            this.key(Key.RIGHT, Modifiers.of(Modifier.SHIFT));
            assertEquals(List.of(0, 1), this.tree.selectedIndexes());
            assertEquals(1, this.tree.selectedIndex());
        }
    }

    @Nested
    class ComboBoxes {
        private ComboBox<String> combo;

        private void show() {
            final ComponentFactory ui = CollectionEdgeCaseTest.this.ui;
            this.combo = ui.<String>comboBox().setItems(List.of("Ana", "Ben", "Cleo"));
            CollectionEdgeCaseTest.this.test.show(CollectionEdgeCaseTest.this.panel
                    .add(this.combo, 10, 10)
                    .add(ui.button("Next"), 10, 200));
        }

        @Test
        void aClosedHandlerSeesAConsistentBoxWhenNewItemsCloseIt() {
            this.show();
            this.combo.select(2).open();
            final List<Optional<String>> seen = new ArrayList<>();
            this.combo.onEvent(Closable.CLOSED, e -> seen.add(this.combo.selected()));
            this.combo.setItems(List.of("Dev"));
            assertEquals(List.of(Optional.of("Cleo")), seen);
            assertTrue(this.combo.selectedIndex() < this.combo.items().size());
        }

        @Test
        void tabbingAwayClosesTheList() {
            this.show();
            this.combo.requestFocus();
            CollectionEdgeCaseTest.this.test.host().key(Key.SPACE, KeyAction.PRESS, Modifiers.NONE);
            assertTrue(this.combo.isOpen());
            CollectionEdgeCaseTest.this.test.host().key(Key.TAB, KeyAction.PRESS, Modifiers.NONE);
            assertFalse(this.combo.isFocused());
            assertFalse(this.combo.isOpen());
        }
    }

    @Nested
    class ListThumbs {
        private static final String THUMB = "#FF111111";
        private static final String HOVERED = "#FF222222";

        private List<String> thumbFills(final TestHost host) {
            final DrawLog canvas = new DrawLog();
            host.host().render(canvas);
            return canvas.lines("fill 94,").stream()
                    .filter(line -> line.endsWith(THUMB) || line.endsWith(HOVERED))
                    .map(line -> line.substring(line.length() - THUMB.length()))
                    .toList();
        }

        @Test
        void aPressOnTheThumbWithoutADragLeavesItPlainOnRelease() {
            final TestHost host = new TestHost(new TestPlatform(), Themes.flat().toBuilder()
                    .paint(ScrollPanel.STYLE_THUMB, Paint.solid(0xFF111111))
                    .paint(ScrollPanel.STYLE_THUMB_HOVERED, Paint.solid(0xFF222222))
                    .build(), 300, 300);
            final ListView<Integer> list = host.factory().<Integer>listView().setItems(numbers(50));
            list.setSize(100, 100);
            host.show(host.factory().absolutePanel().add(list, 0, 0));
            assertEquals(List.of(THUMB), this.thumbFills(host));
            host.host().pointerButton(97, 5, MouseButton.LEFT, true, Modifiers.NONE);
            assertEquals(List.of(HOVERED), this.thumbFills(host));
            host.host().pointerButton(97, 5, MouseButton.LEFT, false, Modifiers.NONE);
            assertEquals(List.of(THUMB), this.thumbFills(host));
        }
    }

    /**
     * Items keyed by their first letter, so "a1" and "a2" share a key.
     */
    private static Object letter(final String item) {
        if (item.equals("boom")) {
            throw new IllegalStateException("No key for " + item);
        }
        return item.charAt(0);
    }

    @Nested
    class EqualKeys {
        private final List<String> items = List.of("a1", "b1", "a2");

        @Test
        void aListKeepsTheSelectionOnTheSameOneOfSeveralEqualKeys() {
            final ListView<String> list = CollectionEdgeCaseTest.this.ui.<String>listView()
                    .setKeyFunction(CollectionEdgeCaseTest::letter)
                    .setItems(this.items)
                    .select(2);
            list.setItems(List.of("a1", "c1", "a2", "b1"));
            assertEquals(2, list.selectedIndex());
            list.setItems(List.of("a3"));
            assertEquals(0, list.selectedIndex());
        }

        @Test
        void aMultipleListSelectsOnlyTheMatchingOneOfSeveralEqualKeys() {
            final ListView<String> list = CollectionEdgeCaseTest.this.ui.<String>listView()
                    .setKeyFunction(CollectionEdgeCaseTest::letter)
                    .setSelectionMode(SelectionMode.MULTIPLE)
                    .setItems(this.items)
                    .selectIndexes(List.of(1, 2));
            list.setItems(this.items);
            assertEquals(List.of(1, 2), list.selectedIndexes());
        }

        @Test
        void sortingATableKeepsTheSelectedItemAmongEqualKeys() {
            final TableView<String> table = CollectionEdgeCaseTest.this.ui.<String>tableView()
                    .addTextColumn("Name", GridLength.weighted(1f), item -> item)
                    .setKeyFunction(CollectionEdgeCaseTest::letter)
                    .setItems(this.items)
                    .select(0);
            final TableColumn<String> name = table.columns().getFirst();
            table.sortBy(name, false);
            assertEquals(List.of("b1", "a2", "a1"), table.shownItems());
            assertEquals(Optional.of("a1"), table.selected());
            table.sortBy(name, true);
            assertEquals(Optional.of("a1"), table.selected());
            assertEquals(0, table.selectedIndex());
        }
    }

    @Nested
    class ThrowingCallbacks {
        @Test
        void aKeyFunctionThatThrowsLeavesTheListAsItWas() {
            final ListView<String> list = CollectionEdgeCaseTest.this.ui.<String>listView()
                    .setKeyFunction(CollectionEdgeCaseTest::letter)
                    .setItems(List.of("a1", "b1", "c1"))
                    .select(2);
            assertThrows(IllegalStateException.class, () -> list.setItems(List.of("a1", "boom")));
            assertEquals(List.of("a1", "b1", "c1"), list.items());
            assertEquals(Optional.of("c1"), list.selected());
        }

        @Test
        void aComparatorThatThrowsLeavesTheTableAsItWas() {
            final TableColumn<String> name = CollectionEdgeCaseTest.this.ui.<String>tableColumn("Name")
                    .setCellText(item -> item)
                    .setComparator(Comparator.comparing(item -> letter(item).toString()));
            final TableView<String> table = CollectionEdgeCaseTest.this.ui.<String>tableView()
                    .addColumn(name)
                    .setItems(List.of("c1", "boom"))
                    .select(1);
            assertThrows(IllegalStateException.class, () -> table.sortBy(name, true));
            assertEquals(Optional.empty(), table.sortColumn());
            assertEquals(List.of("c1", "boom"), table.shownItems());
            assertEquals(Optional.of("boom"), table.selected());

            table.setItems(List.of("b1", "a1")).sortBy(name, true).select(1);
            assertThrows(IllegalStateException.class, () -> table.setItems(List.of("c1", "boom")));
            assertEquals(List.of("b1", "a1"), table.items());
            assertEquals(List.of("a1", "b1"), table.shownItems());
            assertEquals(Optional.of("b1"), table.selected());
        }

        @Test
        void aChildrenFunctionThatThrowsLeavesTheTreeAsItWas() {
            final TreeView<String> tree = CollectionEdgeCaseTest.this.ui.<String>treeView()
                    .setChildren(item -> {
                        if (item.equals("boom")) {
                            throw new IllegalStateException("No children for " + item);
                        }
                        return List.of();
                    })
                    .setItems(List.of("Ana", "Ben"))
                    .select(1);
            assertThrows(IllegalStateException.class, () -> tree.setItems(List.of("Cleo", "boom")));
            assertEquals(List.of("Ana", "Ben"), tree.items());
            assertEquals(List.of("Ana", "Ben"), tree.shownItems());
            assertEquals(Optional.of("Ben"), tree.selected());
        }
    }

    @Nested
    class ChangesDuringInput {
        @Test
        void aGridWhoseSelectHandlerShrinksTheItemsDropsTheClick() {
            final ComponentFactory ui = CollectionEdgeCaseTest.this.ui;
            final GridView<String> grid = ui.<String>gridView(ui::label).setColumns(3)
                    .setItems(List.of("a", "b", "c")).setSelectable(true).setActivateOnClick(true);
            final List<String> heard = new ArrayList<>();
            grid.onEvent(Selection.SELECT, e -> grid.setItems(List.of("x")));
            grid.onEvent(GridView.CLICK, e -> heard.add("click " + e.value().index()));
            grid.onEvent(Activatable.ACTIVATE, e -> heard.add("activate " + e.value()));
            CollectionEdgeCaseTest.this.test.show(CollectionEdgeCaseTest.this.panel.add(grid, 0, 0));
            assertDoesNotThrow(() -> CollectionEdgeCaseTest.this.test.click(41, 5));
            assertEquals(List.of(), heard);
            assertEquals(-1, grid.selectedIndex());
        }
    }

    @Nested
    class CyclicTrees {
        @Test
        void anItemThatIsItsOwnAncestorShowsOnceMoreAsALeaf() {
            final TreeView<String> tree = CollectionEdgeCaseTest.this.ui.<String>treeView()
                    .setChildren(name -> name.equals("Ana") ? List.of("Ben") : List.of("Ana"))
                    .setItems(List.of("Ana"));
            assertDoesNotThrow(tree::expandAll);
            assertEquals(List.of("Ana", "Ben", "Ana"), tree.shownItems());
            assertDoesNotThrow(() -> tree.selectItem("Ben"));
            assertEquals(1, tree.selectedIndex());
        }
    }
}
