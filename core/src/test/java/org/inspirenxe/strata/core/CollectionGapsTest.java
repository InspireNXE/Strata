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
import org.inspirenxe.strata.api.control.attribute.item.MultiSelection;
import org.inspirenxe.strata.api.control.attribute.item.SelectionMode;
import org.inspirenxe.strata.api.control.collection.ComboBox;
import org.inspirenxe.strata.api.control.collection.GridView;
import org.inspirenxe.strata.api.control.collection.ListView;
import org.inspirenxe.strata.api.control.collection.TableView;
import org.inspirenxe.strata.api.control.collection.TreeView;
import org.inspirenxe.strata.api.input.Modifier;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.input.ScrollKind;
import org.inspirenxe.strata.api.layout.AbsolutePanel;
import org.inspirenxe.strata.api.layout.GridLength;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the collection controls gained beside their peers in other toolkits: grids that build only the rows in view
 * and select several cells, trees that ask for children only when they open, refreshing one item, placeholders,
 * table columns and editing cells in place.
 */
class CollectionGapsTest {
    private final TestHost test = new TestHost(300, 300);
    private final ComponentFactory ui = this.test.factory();
    private final AbsolutePanel panel = this.ui.absolutePanel();
    private final List<String> heard = new ArrayList<>();

    private static List<Integer> numbers(final int count) {
        return IntStream.range(0, count).boxed().toList();
    }

    @Nested
    class RefreshingOneItem {
        private final List<String> built = new ArrayList<>();

        @Test
        void aListBuildsOnlyTheRowsOfThatItemAgain() {
            final ListView<String> list = CollectionGapsTest.this.ui.<String>listView(name -> {
                this.built.add(name);
                return CollectionGapsTest.this.ui.label(name);
            }).setItems(List.of("Ana", "Ben", "Cleo"));
            list.setSize(100, 100);
            CollectionGapsTest.this.test.show(CollectionGapsTest.this.panel.add(list, 0, 0));
            this.built.clear();
            list.refresh("Ben");
            CollectionGapsTest.this.test.host().layout();
            assertEquals(List.of("Ben"), this.built);
            list.refresh("Dev");
            CollectionGapsTest.this.test.host().layout();
            assertEquals(List.of("Ben"), this.built);
        }

        @Test
        void aTableAndAGridFindTheItemByKey() {
            final ComponentFactory ui = CollectionGapsTest.this.ui;
            final TableView<String> table = ui.<String>tableView()
                    .addColumn("Name", GridLength.weighted(1f), name -> {
                        this.built.add("row " + name);
                        return ui.label(name);
                    })
                    .setKeyFunction(name -> name.charAt(0))
                    .setItems(List.of("Ana", "Ben"));
            table.setSize(100, 100);
            final GridView<String> grid = ui.<String>gridView(name -> {
                this.built.add("cell " + name);
                return ui.label(name);
            }).setKeyFunction(name -> name.charAt(0)).setItems(Arrays.asList("Ana", null, "Ben"));
            CollectionGapsTest.this.test.show(CollectionGapsTest.this.panel.add(table, 0, 0).add(grid, 0, 150));
            this.built.clear();
            table.refresh("Bea");
            grid.refresh("Bea");
            CollectionGapsTest.this.test.host().layout();
            assertEquals(List.of("cell Ben", "row Ben"), this.built.stream().sorted().toList());
        }

        @Test
        void aComboBoxShowsItsSelectedOptionAgain() {
            final StringBuilder name = new StringBuilder("Ana");
            final ComboBox<StringBuilder> combo = CollectionGapsTest.this.ui.<StringBuilder>comboBox()
                    .setItems(List.of(name));
            CollectionGapsTest.this.test.show(CollectionGapsTest.this.panel.add(combo, 0, 0));
            final float before = combo.bounds().width();
            name.append("stasia");
            combo.refresh(name);
            CollectionGapsTest.this.test.host().layout();
            assertTrue(combo.bounds().width() > before);
        }
    }

    @Nested
    class LazyTrees {
        private final List<String> asked = new ArrayList<>();

        private TreeView<String> tree() {
            return CollectionGapsTest.this.ui.<String>treeView()
                    .setChildren(name -> {
                        this.asked.add(name);
                        return name.length() < 3 ? List.of(name + "a", name + "b") : List.of();
                    })
                    .setHasChildren(name -> name.length() < 3)
                    .setItems(List.of("A", "B"));
        }

        @Test
        void childrenAreAskedForOnlyWhenAnItemOpensAndKept() {
            final TreeView<String> tree = this.tree();
            assertEquals(List.of(), this.asked);
            tree.setExpanded("A", true);
            assertEquals(List.of("A"), this.asked);
            assertEquals(List.of("A", "Aa", "Ab", "B"), tree.shownItems());
            tree.setExpanded("B", true);
            tree.setExpanded("A", false);
            tree.setExpanded("A", true);
            assertEquals(List.of("A", "B"), this.asked);
        }

        @Test
        void refreshingAnItemAsksForItsChildrenAgain() {
            final TreeView<String> tree = this.tree();
            tree.setExpanded("A", true).setExpanded("B", true);
            this.asked.clear();
            tree.refresh("A");
            assertEquals(List.of("A"), this.asked);
            tree.refresh();
            assertEquals(List.of("A", "A", "B"), this.asked);
        }

        @Test
        void anItemWhoseChildrenTurnOutEmptyLosesItsArrowOnceOpen() {
            final TreeView<String> tree = CollectionGapsTest.this.ui.<String>treeView()
                    .setChildren(name -> List.of())
                    .setHasChildren(name -> true)
                    .setItems(List.of("Ana"));
            tree.setExpanded("Ana", true);
            assertEquals(List.of("Ana"), tree.shownItems());
            assertTrue(tree.isExpanded("Ana"));
        }

        @Test
        void selectingAnItemSearchesOnlyUnderItemsThatHaveChildren() {
            final TreeView<String> tree = this.tree();
            tree.selectItem("Bb");
            assertEquals(java.util.Optional.of("Bb"), tree.selected());
            // The leaves Aaa and the rest are never asked for, since the test says they have no children
            assertEquals(List.of("A", "Aa", "Ab", "B", "Ba"), this.asked);
        }
    }

    @Nested
    class ComboBoxPlaceholders {
        @Test
        void aBoxWithNothingSelectedShowsItsPlaceholder() {
            final ComboBox<String> combo = CollectionGapsTest.this.ui.<String>comboBox()
                    .setPlaceholder("Pick a country")
                    .clearSelection()
                    .setItems(List.of("Ana"));
            CollectionGapsTest.this.test.show(CollectionGapsTest.this.panel.add(combo, 0, 0));
            assertEquals(-1, combo.selectedIndex());
            final DrawLog canvas = new DrawLog();
            CollectionGapsTest.this.test.host().render(canvas);
            assertTrue(canvas.lines().stream().anyMatch(line -> line.contains("Pick a country")), canvas.lines().toString());
            combo.select(0);
            final DrawLog after = new DrawLog();
            CollectionGapsTest.this.test.host().render(after);
            assertTrue(after.lines().stream().noneMatch(line -> line.contains("Pick a country")));
        }
    }

    @Nested
    class Grids {
        private final List<Integer> built = new ArrayList<>();

        private GridView<Integer> grid(final int count) {
            final ComponentFactory ui = CollectionGapsTest.this.ui;
            return ui.<Integer>gridView(n -> {
                this.built.add(n);
                return ui.label(String.valueOf(n));
            }).setColumns(5).setCellSize(20).setItems(numbers(count));
        }

        @Test
        void aGridInALimitedHeightBuildsOnlyTheRowsInView() {
            final GridView<Integer> grid = this.grid(5000);
            grid.setHeight(100);
            CollectionGapsTest.this.test.show(CollectionGapsTest.this.panel.add(grid, 0, 0));
            assertTrue(this.built.size() <= 30, "built " + this.built.size());
            assertEquals(100f, grid.bounds().height());
            this.built.clear();
            grid.scrollToIndex(4999);
            CollectionGapsTest.this.test.host().layout();
            assertEquals(1000 * 20f - 100f, grid.scrollY());
            assertTrue(this.built.contains(4999));
            assertTrue(this.built.size() <= 30, "built " + this.built.size());
        }

        @Test
        void aGridWithRoomForEveryRowBuildsEveryCell() {
            final GridView<Integer> grid = this.grid(23);
            CollectionGapsTest.this.test.show(CollectionGapsTest.this.panel.add(grid, 0, 0));
            assertEquals(23, this.built.size());
            assertEquals(5 * 20f, grid.bounds().height());
            assertEquals(5 * 20f, grid.bounds().width());
        }

        @Test
        void theWheelScrollsALimitedGridAndItsBarTakesAGutter() {
            final GridView<Integer> grid = this.grid(100);
            grid.setHeight(100);
            CollectionGapsTest.this.test.show(CollectionGapsTest.this.panel.add(grid, 0, 0));
            assertTrue(grid.bounds().width() > 5 * 20f);
            CollectionGapsTest.this.test.host().scroll(10, 10, 0, -1, ScrollKind.WHEEL, Modifiers.NONE);
            CollectionGapsTest.this.test.host().layout();
            assertEquals(20f, grid.scrollY());
        }

        @Test
        void controlAndShiftSelectSeveralCells() {
            final GridView<Integer> grid = this.grid(10).setSelectable(true).setSelectionMode(SelectionMode.MULTIPLE)
                    .onEvent(MultiSelection.SELECT_MANY, e -> CollectionGapsTest.this.heard.add("ask " + e.value()));
            CollectionGapsTest.this.test.show(CollectionGapsTest.this.panel.add(grid, 0, 0));
            this.click(1, Modifiers.NONE);
            this.click(3, Modifiers.of(Modifier.CONTROL));
            this.click(7, Modifiers.of(Modifier.SHIFT));
            assertEquals(List.of(3, 4, 5, 6, 7), grid.selectedIndexes());
            assertEquals(7, grid.selectedIndex());
            assertEquals(List.of("ask [1]", "ask [1, 3]", "ask [3, 4, 5, 6, 7]"), CollectionGapsTest.this.heard);
        }

        @Test
        void newItemsDropTheSelectedCellsThatWent() {
            final GridView<Integer> grid = this.grid(10).setSelectionMode(SelectionMode.MULTIPLE)
                    .selectIndexes(List.of(2, 8, 9));
            grid.setItems(numbers(9));
            assertEquals(List.of(2, 8), grid.selectedIndexes());
            assertEquals(8, grid.selectedIndex());
            grid.setItems(numbers(5));
            assertEquals(List.of(2), grid.selectedIndexes());
            assertEquals(2, grid.selectedIndex());
        }

        private void click(final int cell, final Modifiers modifiers) {
            final float x = (cell % 5) * 20f + 10f;
            final float y = (cell / 5) * 20f + 10f;
            CollectionGapsTest.this.test.host().pointerButton(x, y, MouseButton.LEFT, true, modifiers);
            CollectionGapsTest.this.test.host().pointerButton(x, y, MouseButton.LEFT, false, modifiers);
        }
    }
}
