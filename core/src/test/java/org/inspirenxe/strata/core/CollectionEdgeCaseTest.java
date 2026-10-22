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
import org.inspirenxe.strata.api.control.attribute.item.Selection;
import org.inspirenxe.strata.api.control.attribute.item.SelectionMode;
import org.inspirenxe.strata.api.control.attribute.popup.Closable;
import org.inspirenxe.strata.api.control.collection.ComboBox;
import org.inspirenxe.strata.api.control.collection.ListView;
import org.inspirenxe.strata.api.control.collection.TableView;
import org.inspirenxe.strata.api.control.collection.TreeView;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifier;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.layout.AbsolutePanel;
import org.inspirenxe.strata.api.layout.GridLength;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
}
