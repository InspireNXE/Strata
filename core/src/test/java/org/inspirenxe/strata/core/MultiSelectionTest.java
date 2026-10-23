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

import org.inspirenxe.strata.api.control.attribute.item.MultiSelection;
import org.inspirenxe.strata.api.control.attribute.item.Selection;
import org.inspirenxe.strata.api.control.attribute.item.SelectionMode;
import org.inspirenxe.strata.api.control.collection.GridView;
import org.inspirenxe.strata.api.control.collection.ListView;
import org.inspirenxe.strata.api.control.collection.TableView;
import org.inspirenxe.strata.api.control.collection.TreeView;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifier;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.layout.GridLength;
import org.inspirenxe.strata.api.theme.Themes;
import org.inspirenxe.strata.spi.Conventions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Selecting several rows in a list, a table and a tree, and the events each change fires.
 */
class MultiSelectionTest {
    private static final List<String> PEOPLE = List.of("Ana", "Ben", "Cleo", "Dev", "Eli");

    private final List<String> heard = new ArrayList<>();
    private TestHost test;
    private ListView<String> list;

    @BeforeEach
    void show() {
        this.test = new TestHost(300, 300);
        this.list = this.test.factory().<String>listView()
                .setItems(PEOPLE)
                .setSelectionMode(SelectionMode.MULTIPLE)
                .onEvent(MultiSelection.SELECT_MANY, event -> this.heard.add("ask " + event.value()))
                .onEvent(MultiSelection.SELECTED_MANY, event -> this.heard.add(event.previous() + "->" + event.value()))
                .onEvent(Selection.SELECTED, event -> this.heard.add("current " + event.value()));
        this.list.setSize(200, 200);
        this.test.show(this.test.factory().absolutePanel().add(this.list, 0, 0));
    }

    private void click(final int row, final Modifier... modifiers) {
        final float y = this.list.bounds().y() + row * this.list.rowHeight() + this.list.rowHeight() / 2f;
        final Modifiers held = modifiers.length == 0 ? Modifiers.NONE : Modifiers.of(modifiers);
        this.test.host().pointerButton(20, y, MouseButton.LEFT, true, held);
        this.test.host().pointerButton(20, y, MouseButton.LEFT, false, held);
    }

    @Test
    void controlAddsAndRemovesRows() {
        this.click(0);
        this.click(2, Modifier.CONTROL);
        this.click(3, Modifier.CONTROL);
        assertEquals(List.of(0, 2, 3), this.list.selectedIndexes());
        assertEquals(3, this.list.selectedIndex());
        this.click(3, Modifier.CONTROL);
        assertEquals(List.of(0, 2), this.list.selectedIndexes());
        assertEquals(2, this.list.selectedIndex());
        assertEquals(List.of("Ana", "Cleo"), this.list.selectedItems());
    }

    @Test
    void shiftSelectsTheRangeFromTheAnchor() {
        this.click(1);
        this.click(3, Modifier.SHIFT);
        assertEquals(List.of(1, 2, 3), this.list.selectedIndexes());
        this.click(0, Modifier.SHIFT);
        assertEquals(List.of(0, 1), this.list.selectedIndexes());
    }

    @Test
    void theSetIsAskedForAndReportedBeforeTheCurrentRow() {
        this.click(1);
        assertEquals(List.of("ask [1]", "[]->[1]", "current 1"), this.heard);
    }

    @Test
    void aCanceledRequestKeepsTheSelection() {
        this.click(1);
        this.list.onEvent(MultiSelection.SELECT_MANY, event -> event.cancel());
        this.click(2, Modifier.CONTROL);
        assertEquals(List.of(1), this.list.selectedIndexes());
    }

    @Test
    void keysExtendTheRangeAndControlASelectsAll() {
        this.click(1);
        this.test.host().key(Key.DOWN, KeyAction.PRESS, Modifiers.of(Modifier.SHIFT));
        this.test.host().key(Key.DOWN, KeyAction.PRESS, Modifiers.of(Modifier.SHIFT));
        assertEquals(List.of(1, 2, 3), this.list.selectedIndexes());
        this.test.host().key(Key.A, KeyAction.PRESS, Modifiers.of(Modifier.CONTROL));
        assertEquals(List.of(0, 1, 2, 3, 4), this.list.selectedIndexes());
    }

    @Test
    void codeReplacesTheSetAndSingleModeKeepsOnlyTheCurrentRow() {
        this.list.selectIndexes(List.of(4, 1, 1));
        assertEquals(List.of(1, 4), this.list.selectedIndexes());
        assertEquals(4, this.list.selectedIndex());
        this.list.setSelectionMode(SelectionMode.SINGLE);
        assertEquals(List.of(4), this.list.selectedIndexes());
        assertThrows(IllegalArgumentException.class, () -> this.list.selectIndexes(List.of(0, 1)));
        this.list.selectAll();
        assertEquals(List.of(4), this.list.selectedIndexes());
    }

    @Test
    void newItemsKeepTheSelectedOnesByKey() {
        this.list.selectIndexes(List.of(1, 3));
        this.list.setItems(List.of("Dev", "Fay", "Ben"));
        assertEquals(List.of(0, 2), this.list.selectedIndexes());
        assertEquals(List.of("Dev", "Ben"), this.list.selectedItems());
    }

    @Test
    void tablesAndTreesPassTheSelectionThrough() {
        final TableView<String> table = this.test.factory().<String>tableView()
                .addTextColumn("Name", GridLength.weighted(1f), name -> name)
                .setItems(PEOPLE)
                .setSelectionMode(SelectionMode.MULTIPLE);
        final List<List<Integer>> sets = new ArrayList<>();
        table.onEvent(MultiSelection.SELECTED_MANY, event -> sets.add(event.value()));
        table.selectIndexes(List.of(0, 4));
        assertEquals(List.of("Ana", "Eli"), table.selectedItems());
        assertEquals(List.of(List.of(0, 4)), sets);

        final TreeView<String> tree = this.test.factory().<String>treeView()
                .setChildren(name -> name.equals("Ana") ? List.of("Ana's notes") : List.of())
                .setItems(List.of("Ana", "Ben"))
                .setSelectionMode(SelectionMode.MULTIPLE);
        tree.expandAll();
        tree.selectIndexes(List.of(1, 2));
        assertEquals(List.of("Ana's notes", "Ben"), tree.selectedItems());
        tree.setExpanded("Ana", false);
        assertEquals(List.of("Ben"), tree.selectedItems());
    }

    @Test
    void aPlatformWhoseShortcutsUseSuperTakesSuperForTheListAndTheGrid() {
        final TestPlatform platform = new TestPlatform();
        platform.setConventions(new Conventions(false, Modifier.SUPER, true));
        final TestHost mac = new TestHost(platform, Themes.flat(), 300, 300);
        final ListView<String> names = mac.factory().<String>listView().setItems(PEOPLE)
                .setSelectionMode(SelectionMode.MULTIPLE);
        names.setSize(200, 100);
        final GridView<String> cells = mac.factory().<String>gridView().setItems(PEOPLE).setColumns(5).setSelectable(true)
                .setSelectionMode(SelectionMode.MULTIPLE);
        mac.show(mac.factory().absolutePanel().add(names, 0, 0).add(cells, 0, 150));
        mac.host().layout();
        final float rowY = names.bounds().y() + names.rowHeight() / 2f;
        mac.host().pointerButton(20, rowY, MouseButton.LEFT, true, Modifiers.NONE);
        mac.host().pointerButton(20, rowY, MouseButton.LEFT, false, Modifiers.NONE);
        final float thirdY = rowY + names.rowHeight() * 2f;
        mac.host().pointerButton(20, thirdY, MouseButton.LEFT, true, Modifiers.of(Modifier.CONTROL));
        mac.host().pointerButton(20, thirdY, MouseButton.LEFT, false, Modifiers.of(Modifier.CONTROL));
        assertEquals(List.of(2), names.selectedIndexes());
        mac.host().pointerButton(20, rowY, MouseButton.LEFT, true, Modifiers.of(Modifier.SUPER));
        mac.host().pointerButton(20, rowY, MouseButton.LEFT, false, Modifiers.of(Modifier.SUPER));
        assertEquals(List.of(0, 2), names.selectedIndexes());
        mac.host().key(Key.A, KeyAction.PRESS, Modifiers.of(Modifier.CONTROL));
        assertEquals(List.of(0, 2), names.selectedIndexes());
        mac.host().key(Key.A, KeyAction.PRESS, Modifiers.of(Modifier.SUPER));
        assertEquals(List.of(0, 1, 2, 3, 4), names.selectedIndexes());

        cells.requestFocus();
        mac.host().key(Key.A, KeyAction.PRESS, Modifiers.of(Modifier.CONTROL));
        assertEquals(List.of(), cells.selectedIndexes());
        mac.host().key(Key.A, KeyAction.PRESS, Modifiers.of(Modifier.SUPER));
        assertEquals(List.of(0, 1, 2, 3, 4), cells.selectedIndexes());
    }
}
