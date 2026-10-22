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
import org.inspirenxe.strata.api.control.attribute.item.MultiSelection;
import org.inspirenxe.strata.api.control.attribute.item.Selection;
import org.inspirenxe.strata.api.control.attribute.item.SelectionMode;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.collection.ComboBox;
import org.inspirenxe.strata.api.control.collection.ListView;
import org.inspirenxe.strata.api.control.collection.TableView;
import org.inspirenxe.strata.api.control.collection.TreeView;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.layout.AbsolutePanel;
import org.inspirenxe.strata.api.layout.GridLength;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    private void type(final String text) {
        text.codePoints().forEach(c -> this.test.host().charTyped(c, Modifiers.NONE));
    }

    @Nested
    class TypeAheads {
        private static final List<String> NAMES = List.of("Ana", "Ben", "Cleo", "Cara", "Dev", "Clara");

        private ListView<String> list() {
            final ListView<String> list = CollectionKeyboardTest.this.ui.<String>listView().setItems(NAMES)
                    .onEvent(Selection.SELECT, e -> CollectionKeyboardTest.this.heard.add("select " + e.value()));
            list.setSize(150, 200);
            CollectionKeyboardTest.this.test.show(CollectionKeyboardTest.this.panel.add(list, 0, 0));
            list.requestFocus();
            return list;
        }

        @Test
        void typingAPrefixSelectsTheFirstMatchAndGrowsInPlace() {
            final ListView<String> list = this.list();
            CollectionKeyboardTest.this.type("c");
            assertEquals(2, list.selectedIndex());
            CollectionKeyboardTest.this.type("l");
            assertEquals(2, list.selectedIndex());
            CollectionKeyboardTest.this.type("a");
            assertEquals(5, list.selectedIndex());
            assertEquals(List.of("select 2", "select 5"), CollectionKeyboardTest.this.heard);
        }

        @Test
        void oneLetterAgainCyclesThroughItsItems() {
            final ListView<String> list = this.list();
            CollectionKeyboardTest.this.type("ccc");
            assertEquals(5, list.selectedIndex());
            CollectionKeyboardTest.this.type("c");
            assertEquals(2, list.selectedIndex());
        }

        @Test
        void thePrefixStartsAgainAfterTheTimeout() {
            final ListView<String> list = this.list();
            CollectionKeyboardTest.this.type("c");
            CollectionKeyboardTest.this.test.platform().advanceMillis(ListView.DEFAULT_TYPEAHEAD_TIMEOUT + 1);
            CollectionKeyboardTest.this.type("d");
            assertEquals(4, list.selectedIndex());
            CollectionKeyboardTest.this.type("e");
            assertEquals(4, list.selectedIndex());
        }

        @Test
        void caseIsIgnoredAndNoMatchLeavesTheSelection() {
            final ListView<String> list = this.list();
            CollectionKeyboardTest.this.type("BE");
            assertEquals(1, list.selectedIndex());
            CollectionKeyboardTest.this.type("x");
            assertEquals(1, list.selectedIndex());
        }

        @Test
        void aSpaceNeverStartsAPrefixAndNullTurnsItOff() {
            final ListView<String> list = this.list();
            assertFalse(CollectionKeyboardTest.this.test.host().charTyped(' ', Modifiers.NONE));
            list.setTypeAhead(null);
            assertFalse(CollectionKeyboardTest.this.test.host().charTyped('b', Modifiers.NONE));
            assertEquals(-1, list.selectedIndex());
        }

        @Test
        void aMultipleListAsksForTheSet() {
            final ListView<String> list = this.list().setSelectionMode(SelectionMode.MULTIPLE).selectIndexes(List.of(0, 1));
            list.onEvent(MultiSelection.SELECT_MANY, e -> CollectionKeyboardTest.this.heard.add("many " + e.value()));
            CollectionKeyboardTest.this.type("d");
            assertEquals(List.of(4), list.selectedIndexes());
            assertEquals(List.of("many [4]"), CollectionKeyboardTest.this.heard);
        }

        @Test
        void aTableMatchesItsFirstTextColumnInTheShownOrder() {
            final TableView<String> table = CollectionKeyboardTest.this.ui.<String>tableView()
                    .addColumn("Mark", GridLength.fixed(20), name -> CollectionKeyboardTest.this.ui.label("*"))
                    .addTextColumn("Reversed", GridLength.weighted(1f), name -> new StringBuilder(name).reverse())
                    .setItems(NAMES);
            table.setSize(200, 200);
            CollectionKeyboardTest.this.test.show(CollectionKeyboardTest.this.panel.add(table, 0, 0));
            table.requestFocus();
            CollectionKeyboardTest.this.type("a");
            assertEquals(Optional.of("Ana"), table.selected());
            CollectionKeyboardTest.this.type("r");
            assertEquals(Optional.of("Cara"), table.selected());
            CollectionKeyboardTest.this.type("al");
            assertEquals(Optional.of("Clara"), table.selected());
            table.setTypeAhead(name -> name);
            CollectionKeyboardTest.this.test.platform().advanceMillis(ListView.DEFAULT_TYPEAHEAD_TIMEOUT + 1);
            CollectionKeyboardTest.this.type("b");
            assertEquals(Optional.of("Ben"), table.selected());
        }

        @Test
        void aTreeSearchesItsShownRows() {
            final TreeView<String> tree = CollectionKeyboardTest.this.ui.<String>treeView()
                    .setChildren(name -> name.equals("Ana") ? List.of("Ada", "Bea") : List.of())
                    .setItems(List.of("Ana", "Ben"));
            tree.setSize(200, 150);
            CollectionKeyboardTest.this.test.show(CollectionKeyboardTest.this.panel.add(tree, 0, 0));
            tree.requestFocus();
            CollectionKeyboardTest.this.type("b");
            assertEquals(Optional.of("Ben"), tree.selected());
            tree.setExpanded("Ana", true);
            CollectionKeyboardTest.this.type("b");
            assertEquals(Optional.of("Bea"), tree.selected());
        }

        @Test
        void aClosedComboBoxSelectsAndAnOpenOneHighlights() {
            final ComboBox<String> combo = CollectionKeyboardTest.this.ui.<String>comboBox().setItems(NAMES);
            CollectionKeyboardTest.this.test.show(CollectionKeyboardTest.this.panel.add(combo, 0, 0));
            combo.requestFocus();
            CollectionKeyboardTest.this.type("d");
            assertEquals(4, combo.selectedIndex());
            assertFalse(combo.isOpen());
            combo.open();
            CollectionKeyboardTest.this.test.platform().advanceMillis(ListView.DEFAULT_TYPEAHEAD_TIMEOUT + 1);
            CollectionKeyboardTest.this.type("b");
            assertEquals(4, combo.selectedIndex());
            CollectionKeyboardTest.this.key(Key.ENTER);
            assertEquals(1, combo.selectedIndex());
        }

        @Test
        void aCharacterATextBoxInARowTakesNeverReachesTheList() {
            final ComponentFactory ui = CollectionKeyboardTest.this.ui;
            final List<TextBox> boxes = new ArrayList<>();
            final ListView<String> list = ui.<String>listView(name -> {
                final TextBox box = ui.textBox();
                boxes.add(box);
                return box;
            }).setItems(NAMES);
            list.setSize(150, 200);
            CollectionKeyboardTest.this.test.show(CollectionKeyboardTest.this.panel.add(list, 0, 0));
            boxes.getFirst().requestFocus();
            CollectionKeyboardTest.this.type("d");
            assertEquals(-1, list.selectedIndex());
            assertEquals("d", boxes.getFirst().text());
        }
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
