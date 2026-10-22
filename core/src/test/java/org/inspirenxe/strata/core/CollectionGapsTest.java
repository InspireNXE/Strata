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
import org.inspirenxe.strata.api.control.collection.GridView;
import org.inspirenxe.strata.api.input.Modifier;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.input.ScrollKind;
import org.inspirenxe.strata.api.layout.AbsolutePanel;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
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
