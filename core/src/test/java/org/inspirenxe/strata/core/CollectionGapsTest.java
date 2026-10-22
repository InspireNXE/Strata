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
import org.inspirenxe.strata.api.control.attribute.item.Activatable;
import org.inspirenxe.strata.api.control.attribute.item.CellEditing;
import org.inspirenxe.strata.api.control.attribute.item.MultiSelection;
import org.inspirenxe.strata.api.control.attribute.item.SelectionMode;
import org.inspirenxe.strata.api.control.collection.ComboBox;
import org.inspirenxe.strata.api.control.collection.GridView;
import org.inspirenxe.strata.api.control.collection.ListView;
import org.inspirenxe.strata.api.control.collection.TableColumn;
import org.inspirenxe.strata.api.control.collection.TableView;
import org.inspirenxe.strata.api.control.collection.TreeView;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.event.input.InputEvents;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifier;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.input.ScrollKind;
import org.inspirenxe.strata.api.layout.AbsolutePanel;
import org.inspirenxe.strata.api.layout.GridLength;
import org.inspirenxe.strata.api.layout.ScrollPanel;
import org.inspirenxe.strata.api.theme.Themes;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    class Tables {
        private static final List<String> PEOPLE = List.of("Cleo", "Ana", "Ben", "Ada");

        private TableView<String> table;
        private TableColumn<String> name;
        private TableColumn<String> size;

        private void show(final ScrollPanel.ScrollPolicy policy) {
            final ComponentFactory ui = CollectionGapsTest.this.ui;
            this.name = ui.<String>tableColumn("Name").setWidth(GridLength.fixed(60)).setCellText(n -> n);
            this.size = ui.<String>tableColumn("Size").setWidth(GridLength.fixed(60)).setCellText(n -> n.length())
                    .setComparator(Comparator.comparingInt(String::length));
            this.table = ui.<String>tableView().addColumn(this.name).addColumn(this.size).setItems(PEOPLE)
                    .setHorizontalPolicy(policy)
                    .onEvent(TableView.SORT, e -> CollectionGapsTest.this.heard.add("sort " + this.describe(e.value())))
                    .onEvent(TableView.SORTED_MANY, e -> CollectionGapsTest.this.heard.add("many " + e.value().size()))
                    .onEvent(TableView.COLUMN_RESIZED, e -> CollectionGapsTest.this.heard.add("resized " + e.value().width()))
                    .onEvent(TableView.COLUMN_MOVED, e -> CollectionGapsTest.this.heard.add("moved " + e.value().index()));
            this.table.setSize(100, 120);
            CollectionGapsTest.this.test.show(CollectionGapsTest.this.panel.add(this.table, 0, 0));
        }

        private String describe(final TableView.Sort sort) {
            return sort.column() == null ? "none" : sort.column().title().plain() + (sort.ascending() ? " up" : " down");
        }

        private void press(final float x, final float y, final Modifiers modifiers) {
            CollectionGapsTest.this.test.host().pointerButton(x, y, MouseButton.LEFT, true, modifiers);
        }

        private void release(final float x, final float y) {
            CollectionGapsTest.this.test.host().pointerButton(x, y, MouseButton.LEFT, false, Modifiers.NONE);
        }

        private void drag(final float fromX, final float toX) {
            this.press(fromX, 10, Modifiers.NONE);
            CollectionGapsTest.this.test.host().pointerMoved((fromX + toX) / 2f, 10, Modifiers.NONE);
            CollectionGapsTest.this.test.host().pointerMoved(toX, 10, Modifiers.NONE);
            this.release(toX, 10);
            CollectionGapsTest.this.test.host().layout();
        }

        private void click(final float x, final Modifiers modifiers) {
            this.press(x, 10, modifiers);
            CollectionGapsTest.this.test.host().pointerButton(x, 10, MouseButton.LEFT, false, modifiers);
            CollectionGapsTest.this.test.platform().advanceMillis(1000);
        }

        @Test
        void aTitleClickGoesUpThenDownThenBackToTheSetOrder() {
            this.show(ScrollPanel.ScrollPolicy.NEVER);
            this.click(80, Modifiers.NONE);
            assertEquals(List.of("Ana", "Ben", "Ada", "Cleo"), this.table.shownItems());
            this.click(80, Modifiers.NONE);
            assertEquals("Cleo", this.table.shownItems().getFirst());
            this.click(80, Modifiers.NONE);
            assertEquals(PEOPLE, this.table.shownItems());
            assertEquals(List.of("sort Size up", "many 1", "sort Size down", "many 1", "sort none", "many 0"),
                    CollectionGapsTest.this.heard);
        }

        @Test
        void shiftAddsAColumnToTheSortOrder() {
            this.show(ScrollPanel.ScrollPolicy.NEVER);
            this.name.setComparator(Comparator.naturalOrder());
            this.click(80, Modifiers.NONE);
            CollectionGapsTest.this.heard.clear();
            this.click(10, Modifiers.of(Modifier.SHIFT));
            assertEquals(List.of("Ada", "Ana", "Ben", "Cleo"), this.table.shownItems());
            assertEquals(List.of(new TableView.Sort(this.size, true), new TableView.Sort(this.name, true)),
                    this.table.sortOrder());
            assertEquals(Optional.of(this.size), this.table.sortColumn());
            assertEquals(List.of("many 2"), CollectionGapsTest.this.heard);
        }

        @Test
        void aSortOrderNamesEachColumnOnce() {
            this.show(ScrollPanel.ScrollPolicy.NEVER);
            this.name.setComparator(Comparator.naturalOrder());
            assertThrows(IllegalArgumentException.class, () -> this.table.sortBy(
                    List.of(new TableView.Sort(this.name, true), new TableView.Sort(this.name, false))));
            this.table.sortBy(List.of(new TableView.Sort(this.size, false), new TableView.Sort(this.name, true)));
            assertEquals(List.of("Cleo", "Ada", "Ana", "Ben"), this.table.shownItems());
            this.table.removeColumn(this.size);
            assertEquals(List.of(new TableView.Sort(this.name, true)), this.table.sortOrder());
        }

        @Test
        void draggingATitlesEdgeResizesItWithinItsLimits() {
            this.show(ScrollPanel.ScrollPolicy.NEVER);
            this.name.setWidthLimits(20, 80);
            this.drag(60, 70);
            assertEquals(GridLength.fixed(70), this.name.width());
            this.drag(70, 200);
            assertEquals(GridLength.fixed(80), this.name.width());
            assertEquals("resized 80.0", CollectionGapsTest.this.heard.getLast());
            this.name.setResizable(false);
            this.drag(80, 40);
            assertEquals(GridLength.fixed(80), this.name.width());
        }

        @Test
        void aDoubleClickOnTheEdgeFitsTheColumn() {
            this.show(ScrollPanel.ScrollPolicy.NEVER);
            this.press(60, 10, Modifiers.NONE);
            this.release(60, 10);
            this.press(60, 10, Modifiers.NONE);
            this.release(60, 10);
            final float fitted = ((GridLength.Fixed) this.name.width()).size();
            assertTrue(fitted < 60f && fitted >= "Name".length() * TestPlatform.CHAR_WIDTH, "fitted " + fitted);
        }

        @Test
        void draggingATitleMovesItsColumn() {
            this.show(ScrollPanel.ScrollPolicy.NEVER);
            this.drag(20, 99);
            assertEquals(List.of(this.size, this.name), this.table.columns());
            assertEquals(List.of("moved 1"), CollectionGapsTest.this.heard);
            this.name.setMovable(false);
            this.drag(80, 5);
            assertEquals(List.of(this.size, this.name), this.table.columns());
        }

        @Test
        void aHiddenColumnTakesNoRoomAndARightClickOffersItBack() {
            this.show(ScrollPanel.ScrollPolicy.NEVER);
            this.name.setShown(false);
            CollectionGapsTest.this.test.host().layout();
            final DrawLog canvas = new DrawLog();
            CollectionGapsTest.this.test.host().render(canvas);
            assertTrue(canvas.lines().stream().noneMatch(line -> line.contains("Cleo")));
            CollectionGapsTest.this.test.host().pointerButton(20, 10, MouseButton.RIGHT, true, Modifiers.NONE);
            CollectionGapsTest.this.test.host().pointerButton(20, 10, MouseButton.RIGHT, false, Modifiers.NONE);
            assertEquals(1, CollectionGapsTest.this.ui.overlays().open().size());
        }

        @Test
        void columnsWiderThanTheTableScrollSidewaysWhenAsked() {
            this.show(ScrollPanel.ScrollPolicy.AUTO);
            this.size.setWidth(GridLength.fixed(120));
            CollectionGapsTest.this.test.host().layout();
            assertEquals(0f, this.table.scrollX());
            CollectionGapsTest.this.test.host().scroll(50, 50, -2, 0, ScrollKind.WHEEL, Modifiers.NONE);
            CollectionGapsTest.this.test.host().layout();
            assertEquals(20f, this.table.scrollX());
            final DrawLog canvas = new DrawLog();
            CollectionGapsTest.this.test.host().render(canvas);
            assertTrue(canvas.lines().stream().anyMatch(line -> line.startsWith("text Cleo -16,")), canvas.lines().toString());
        }

        @Test
        void theHeaderHeightIsAThemeMetric() {
            final TestHost host = new TestHost(new TestPlatform(), Themes.flat().toBuilder()
                    .metric(TableView.STYLE_HEADER_HEIGHT, 30f).build(), 300, 300);
            final TableView<String> table = host.factory().<String>tableView()
                    .addTextColumn("Name", GridLength.weighted(1f), n -> n).setItems(PEOPLE);
            table.setSize(100, 120);
            host.show(host.factory().absolutePanel().add(table, 0, 0));
            host.host().pointerButton(50, 35, MouseButton.LEFT, true, Modifiers.NONE);
            assertEquals(0, table.selectedIndex());
        }

        @Test
        void anAutoColumnFitsItsCellsInView() {
            final ComponentFactory ui = CollectionGapsTest.this.ui;
            final TableColumn<String> auto = ui.<String>tableColumn("N").setWidth(GridLength.auto()).setCellText(n -> n + n);
            final TableView<String> table = ui.<String>tableView().addColumn(auto).setItems(PEOPLE);
            table.setSize(200, 120);
            CollectionGapsTest.this.test.show(CollectionGapsTest.this.panel.add(table, 0, 0));
            CollectionGapsTest.this.test.host().layout();
            final DrawLog canvas = new DrawLog();
            CollectionGapsTest.this.test.host().render(canvas);
            assertTrue(canvas.lines().stream().anyMatch(line -> line.contains("CleoCleo")), canvas.lines().toString());
            assertTrue(canvas.lines().stream().noneMatch(line -> line.contains("...")), canvas.lines().toString());
        }
    }

    @Nested
    class EditingCells {
        private final List<TextBox> editors = new ArrayList<>();

        /**
         * An editor as a project would write one: a text box that commits on Enter and cancels on Escape.
         */
        private Component<?> editor(final ComponentFactory ui, final String item, final Consumer<? super String> commit,
                                    final Runnable cancel) {
            final TextBox box = ui.textBox().setText(item);
            box.onEvent(TextBox.SUBMITTED, e -> commit.accept(e.value()));
            box.onEvent(InputEvents.KEY, e -> {
                if (e.key() == Key.ESCAPE && e.action() == KeyAction.PRESS) {
                    cancel.run();
                    e.consume();
                }
            });
            this.editors.add(box);
            return box;
        }

        private ListView<String> list() {
            final ListView<String> list = CollectionGapsTest.this.ui.<String>listView().setItems(List.of("Ana", "Ben", "Cleo"))
                    .setCellEditor(this::editor)
                    .onEvent(CellEditing.EDIT_CELL, e -> CollectionGapsTest.this.heard.add("edit " + e.value()))
                    .onEvent(CellEditing.CELL_EDITED, e -> CollectionGapsTest.this.heard.add(e.previous() + "->" + e.value()))
                    .onEvent(Activatable.ACTIVATED, e -> CollectionGapsTest.this.heard.add("activated " + e.value()));
            list.setSize(150, 100);
            CollectionGapsTest.this.test.show(CollectionGapsTest.this.panel.add(list, 0, 0));
            return list;
        }

        private void key(final Key key) {
            CollectionGapsTest.this.test.host().key(key, KeyAction.PRESS, Modifiers.NONE);
        }

        @Test
        void f2EditsTheCurrentRowAndEnterCommits() {
            final ListView<String> list = this.list().select(1);
            list.requestFocus();
            this.key(Key.F2);
            assertTrue(list.isEditing());
            final TextBox box = this.editors.getFirst();
            assertTrue(box.isFocused());
            box.setText("Bea");
            this.key(Key.ENTER);
            assertFalse(list.isEditing());
            assertTrue(list.isFocused());
            assertEquals(List.of("Ana", "Ben", "Cleo"), list.items());
            assertEquals(List.of("edit Ben", "Ben->Bea"), CollectionGapsTest.this.heard);
            assertFalse(box.isAttached());
        }

        @Test
        void escapeCancelsAndACanceledRequestEditsNothing() {
            final ListView<String> list = this.list().select(0);
            list.requestFocus();
            this.key(Key.F2);
            this.key(Key.ESCAPE);
            assertFalse(list.isEditing());
            assertEquals(List.of("edit Ana"), CollectionGapsTest.this.heard);
            list.onEvent(CellEditing.EDIT_CELL, e -> e.cancel());
            this.key(Key.F2);
            assertFalse(list.isEditing());
            assertEquals(1, this.editors.size());
        }

        @Test
        void aDoublePressEditsInsteadOfActivating() {
            final ListView<String> list = this.list();
            CollectionGapsTest.this.test.click(20, 30);
            CollectionGapsTest.this.test.click(20, 30);
            assertTrue(list.isEditing());
            assertEquals(List.of("edit Ben"), CollectionGapsTest.this.heard);
        }

        @Test
        void aTypedCharacterStartsAnEditWhenTypeAheadIsOff() {
            final ListView<String> list = this.list().setTypeAhead(null).select(2);
            list.requestFocus();
            CollectionGapsTest.this.test.host().charTyped('x', Modifiers.NONE);
            assertTrue(list.isEditing());
            assertEquals("Cleox", this.editors.getFirst().text());
        }

        @Test
        void newItemsCancelTheEditAndTheEditedRowStaysBuiltWhileScrolledOut() {
            final ListView<String> list = this.list().setItems(numbers(50).stream().map(String::valueOf).toList()).select(0);
            list.requestFocus();
            this.key(Key.F2);
            final TextBox box = this.editors.getFirst();
            list.setScrollY(500);
            CollectionGapsTest.this.test.host().layout();
            assertTrue(box.isAttached());
            assertTrue(list.isEditing());
            list.setItems(List.of("Ana"));
            assertFalse(list.isEditing());
            assertFalse(box.isAttached());
        }

        @Test
        void aTableEditsTheColumnUnderThePointerAndATreeItsCell() {
            final ComponentFactory ui = CollectionGapsTest.this.ui;
            final TableColumn<String> fixed = ui.<String>tableColumn("Name").setWidth(GridLength.fixed(50)).setCellText(n -> n);
            final TableColumn<String> note = ui.<String>tableColumn("Note").setWidth(GridLength.fixed(50))
                    .setCellText(n -> n).setCellEditor(this::editor);
            final TableView<String> table = ui.<String>tableView().addColumn(fixed).addColumn(note)
                    .setItems(List.of("Ana", "Ben"))
                    .onEvent(CellEditing.CELL_EDITED, e -> CollectionGapsTest.this.heard.add(e.previous() + "->" + e.value()));
            table.setSize(100, 100);
            CollectionGapsTest.this.test.show(CollectionGapsTest.this.panel.add(table, 0, 0));
            CollectionGapsTest.this.test.click(20, 30);
            CollectionGapsTest.this.test.click(20, 30);
            assertFalse(table.isEditing());
            CollectionGapsTest.this.test.platform().advanceMillis(1000);
            CollectionGapsTest.this.test.click(70, 30);
            CollectionGapsTest.this.test.click(70, 30);
            assertTrue(table.isEditing());
            final TextBox box = this.editors.getFirst();
            CollectionGapsTest.this.test.host().layout();
            assertTrue(box.bounds().x() >= 50f, "the editor sits in the note column");
            box.setText("Bo");
            this.key(Key.ENTER);
            assertEquals(List.of("Ana->Bo"), CollectionGapsTest.this.heard);
            assertTrue(table.isFocused());

            final TreeView<String> tree = ui.<String>treeView().setItems(List.of("Ana")).setCellEditor(this::editor);
            tree.setSize(100, 100);
            CollectionGapsTest.this.test.show(CollectionGapsTest.this.panel.add(tree, 150, 0));
            tree.select(0).requestFocus();
            this.key(Key.F2);
            assertTrue(tree.isEditing());
            CollectionGapsTest.this.test.host().layout();
            assertTrue(this.editors.getLast().bounds().x() >= 150f + TreeView.ARROW_WIDTH);
            tree.cancelEdit();
            assertFalse(tree.isEditing());
        }

        @Test
        void aGridEditsFilledCellsOnly() {
            final ComponentFactory ui = CollectionGapsTest.this.ui;
            final GridView<String> grid = ui.<String>gridView().setCellSize(40).setColumns(3)
                    .setItems(Arrays.asList("Ana", null, "Ben")).setCellEditor(this::editor);
            CollectionGapsTest.this.test.show(CollectionGapsTest.this.panel.add(grid, 0, 0));
            grid.requestFocus();
            this.key(Key.RIGHT);
            this.key(Key.F2);
            assertFalse(grid.isEditing());
            this.key(Key.RIGHT);
            this.key(Key.F2);
            assertTrue(grid.isEditing());
            this.editors.getFirst().setText("Bo");
            this.key(Key.ENTER);
            assertFalse(grid.isEditing());
            assertEquals(Arrays.asList("Ana", null, "Ben"), grid.items());
        }
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
