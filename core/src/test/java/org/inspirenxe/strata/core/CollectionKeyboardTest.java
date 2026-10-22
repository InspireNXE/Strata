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
import org.inspirenxe.strata.api.component.Focusable;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.collection.TableView;
import org.inspirenxe.strata.api.control.collection.TreeView;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.layout.AbsolutePanel;
import org.inspirenxe.strata.api.layout.GridLength;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Focus and keys of the collection controls: tables and trees that keep focus on their rows, the grid's cursor and
 * type-ahead.
 */
class CollectionKeyboardTest {
    private static final List<String> PEOPLE = List.of("Ana", "Ben", "Cleo", "Dev", "Eli");

    private final TestHost test = new TestHost(300, 300);
    private final ComponentFactory ui = this.test.factory();
    private final AbsolutePanel panel = this.ui.absolutePanel();
    private final List<String> heard = new ArrayList<>();

    private void key(final Key key) {
        this.test.host().key(key, KeyAction.PRESS, Modifiers.NONE);
    }

    @Nested
    class FocusDelegates {
        private TableView<String> table;
        private Button other;

        private void show() {
            final ComponentFactory ui = CollectionKeyboardTest.this.ui;
            this.table = ui.<String>tableView()
                    .addTextColumn("Name", GridLength.weighted(1f), name -> name)
                    .setItems(PEOPLE)
                    .onEvent(Focusable.FOCUS_GAINED, e -> CollectionKeyboardTest.this.heard.add("gained"))
                    .onEvent(Focusable.FOCUS_LOST, e -> CollectionKeyboardTest.this.heard.add("lost"));
            this.table.setSize(200, 150);
            this.other = ui.button("Next");
            CollectionKeyboardTest.this.test.show(CollectionKeyboardTest.this.panel
                    .add(this.table, 0, 0)
                    .add(this.other, 0, 200));
        }

        @Test
        void aTableTakesFocusThroughItsBody() {
            this.show();
            assertTrue(this.table.isFocusable());
            assertTrue(this.table.requestFocus());
            assertTrue(this.table.isFocused());
            assertEquals(List.of("gained"), CollectionKeyboardTest.this.heard);
            CollectionKeyboardTest.this.key(Key.DOWN);
            assertEquals(0, this.table.selectedIndex());
            this.other.requestFocus();
            assertFalse(this.table.isFocused());
            assertEquals(List.of("gained", "lost"), CollectionKeyboardTest.this.heard);
        }

        @Test
        void theTabOrderHoldsATableOnce() {
            this.show();
            assertEquals(2, CollectionKeyboardTest.this.test.tabOrder().size());
            CollectionKeyboardTest.this.key(Key.TAB);
            assertTrue(this.table.isFocused());
            CollectionKeyboardTest.this.key(Key.TAB);
            assertTrue(this.other.isFocused());
        }

        @Test
        void aPressOnTheHeaderFocusesTheTable() {
            this.show();
            this.other.requestFocus();
            CollectionKeyboardTest.this.test.click(50, 5);
            assertTrue(this.table.isFocused());
        }

        @Test
        void theTableSettingsReachItsBody() {
            this.show();
            this.table.setTabbable(false);
            assertFalse(this.table.isTabbable());
            assertEquals(1, CollectionKeyboardTest.this.test.tabOrder().size());
            this.table.setFocusable(false);
            assertFalse(this.table.requestFocus());
            assertFalse(this.table.isFocused());
        }

        @Test
        void aTreeTakesFocusThroughItsRows() {
            final TreeView<String> tree = CollectionKeyboardTest.this.ui.<String>treeView()
                    .setChildren(name -> name.equals("Ana") ? List.of("Ana's notes") : List.of())
                    .setItems(PEOPLE);
            tree.setSize(200, 150);
            CollectionKeyboardTest.this.test.show(CollectionKeyboardTest.this.panel.add(tree, 0, 0));
            assertTrue(tree.requestFocus());
            assertTrue(tree.isFocused());
            CollectionKeyboardTest.this.key(Key.DOWN);
            CollectionKeyboardTest.this.key(Key.RIGHT);
            assertTrue(tree.isExpanded("Ana"));
        }
    }
}
