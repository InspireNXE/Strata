package org.inspirenxe.strata.core;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.control.collection.ComboBox;
import org.inspirenxe.strata.api.control.collection.ListView;
import org.inspirenxe.strata.api.control.collection.SlotGrid;
import org.inspirenxe.strata.api.control.collection.TableView;
import org.inspirenxe.strata.api.control.collection.TreeView;
import org.inspirenxe.strata.api.event.ValueEvent;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.layout.AbsolutePanel;
import org.inspirenxe.strata.api.layout.GridLength;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The events of the collection controls: each Pre request fires for the user's changes only and can be accepted,
 * canceled or rewritten, and each Post report fires after every change, from the user or from code.
 */
class CollectionEventsTest {
    private final TestHost test = new TestHost(300, 300);
    private final ComponentFactory ui = this.test.factory();
    private final AbsolutePanel panel = this.ui.absolutePanel();

    private void key(final Key key) {
        this.test.host().key(key, KeyAction.PRESS, Modifiers.NONE);
    }

    private void rightClick(final float x, final float y) {
        this.test.host().pointerButton(x, y, MouseButton.RIGHT, true, Modifiers.NONE);
        this.test.host().pointerButton(x, y, MouseButton.RIGHT, false, Modifiers.NONE);
    }

    private void show(final Component<?> control, final float x, final float y) {
        this.test.show(this.panel.add(control, x, y));
    }

    /**
     * Formats a report as {@code previous->value}.
     */
    private static String change(final ValueEvent<?, ?> event) {
        return event.previous() + "->" + event.value();
    }

    @Nested
    class Lists {
        private final List<Object> asked = new ArrayList<>();
        private final List<String> heard = new ArrayList<>();
        private ListView<Integer> list;

        private void show() {
            final ComponentFactory ui = CollectionEventsTest.this.ui;
            this.list = ui.<Integer>listView(item -> ui.label(Integer.toString(item)))
                    .setItems(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
            this.list.setSize(100, 100);
            this.list.onEvent(ListView.SELECTED, e -> this.heard.add(change(e)));
            CollectionEventsTest.this.show(this.list, 0, 0);
        }

        @Test
        void aPressAsksToSelectAndReports() {
            this.show();
            this.list.onEvent(ListView.SELECT, e -> this.asked.add(e.value()));
            CollectionEventsTest.this.test.click(10, 45);
            assertEquals(List.of(2), this.asked);
            assertEquals(2, this.list.selectedIndex());
            assertEquals(List.of("-1->2"), this.heard);
        }

        @Test
        void aCanceledSelectKeepsTheSelection() {
            this.show();
            this.list.onEvent(ListView.SELECT, e -> e.cancel());
            CollectionEventsTest.this.test.click(10, 45);
            assertEquals(-1, this.list.selectedIndex());
            assertEquals(List.of(), this.heard);
        }

        @Test
        void aRewrittenIndexIsSelectedAndABadOneIgnored() {
            this.show();
            final int[] rewrite = {5};
            this.list.onEvent(ListView.SELECT, e -> e.setValue(rewrite[0]));
            CollectionEventsTest.this.test.click(10, 45);
            assertEquals(5, this.list.selectedIndex());
            rewrite[0] = 99;
            CollectionEventsTest.this.test.click(10, 25);
            assertEquals(5, this.list.selectedIndex());
            assertEquals(List.of("-1->5"), this.heard);
        }

        @Test
        void keysAndDeselectingAskOnlyWhenTheSelectionWouldChange() {
            this.show();
            this.list.onEvent(ListView.SELECT, e -> this.asked.add(e.value()));
            this.list.setDeselectable(true);
            this.list.requestFocus();
            CollectionEventsTest.this.key(Key.DOWN);
            CollectionEventsTest.this.key(Key.END);
            CollectionEventsTest.this.key(Key.END);
            this.list.select(2);
            CollectionEventsTest.this.test.click(10, 45);
            assertEquals(List.of(0, 9, -1), this.asked);
            assertEquals(List.of("-1->0", "0->9", "9->2", "2->-1"), this.heard);
        }

        @Test
        void codeReportsButNeverAsks() {
            this.show();
            this.list.onEvent(ListView.SELECT, e -> this.asked.add(e.value()));
            this.list.select(4);
            this.list.selectItem(6);
            this.list.select(6);
            this.list.setKeyFunction(item -> item % 100);
            this.list.setItems(List.of(9, 8, 106, 7));
            this.list.setItems(List.of(9, 8, 6, 7));
            this.list.setItems(List.of(1));
            assertEquals(List.of(), this.asked);
            // Replacing the selected item with an unequal one of the same key reports the same index
            assertEquals(List.of("-1->4", "4->6", "6->2", "2->2", "2->-1"), this.heard);
        }

        @Test
        void aDoublePressAndEnterAskToActivate() {
            this.show();
            final List<String> activated = new ArrayList<>();
            this.list.onEvent(ListView.ACTIVATED, e -> activated.add(change(e)));
            this.list.onEvent(ListView.ACTIVATE, e -> this.asked.add(e.value()));
            CollectionEventsTest.this.test.click(10, 45);
            CollectionEventsTest.this.test.click(10, 45);
            assertEquals(List.of("2->2"), activated);

            this.list.onEvent(ListView.ACTIVATE, 1, e -> e.cancel());
            CollectionEventsTest.this.key(Key.ENTER);
            assertEquals(List.of(2, 2), this.asked);
            assertEquals(1, activated.size());
        }

        @Test
        void aRewrittenActivationReportsTheNewRowAndABadOneNothing() {
            this.show();
            final List<Integer> activated = new ArrayList<>();
            final int[] rewrite = {7};
            this.list.onEvent(ListView.ACTIVATED, e -> activated.add(e.value()));
            this.list.onEvent(ListView.ACTIVATE, e -> e.setValue(rewrite[0]));
            this.list.setActivateOnClick(true);
            CollectionEventsTest.this.test.click(10, 65);
            rewrite[0] = 10;
            CollectionEventsTest.this.test.click(10, 65);
            assertEquals(List.of(7), activated);
        }

        @Test
        void anAncestorCanRefuseAndHear() {
            this.show();
            final AbsolutePanel panel = CollectionEventsTest.this.panel;
            final List<ListView<?>> heard = new ArrayList<>();
            panel.onEvent(ListView.SELECTED, e -> heard.add(e.control()));
            panel.onEvent(ListView.SELECT, e -> e.cancel());
            CollectionEventsTest.this.test.click(10, 45);
            assertEquals(-1, this.list.selectedIndex());
            this.list.select(3);
            assertEquals(List.of(this.list), heard);
        }
    }

    @Nested
    class ComboBoxes {
        private final List<Object> asked = new ArrayList<>();
        private final List<String> heard = new ArrayList<>();
        private final List<String> opened = new ArrayList<>();
        private ComboBox<String> combo;

        private void show() {
            final ComponentFactory ui = CollectionEventsTest.this.ui;
            this.combo = ui.<String>comboBox(String::toUpperCase).setOptions(List.of("a", "b", "c"));
            this.combo.onEvent(ComboBox.SELECTED, e -> this.heard.add(change(e)))
                    .onEvent(ComboBox.OPENED, e -> this.opened.add("opened"));
            CollectionEventsTest.this.show(this.combo, 10, 10);
        }

        private void clickBox() {
            CollectionEventsTest.this.test.click(this.combo.bounds().x() + 5, this.combo.bounds().y() + 5);
        }

        private void clickThirdRow() {
            CollectionEventsTest.this.test.host().layout();
            // Rows are a line plus 4 tall, below the box
            CollectionEventsTest.this.test.click(this.combo.bounds().x() + 5, this.combo.bounds().bottom() + 13 * 2 + 6);
        }

        @Test
        void clicksAndKeysAskToOpen() {
            this.show();
            final boolean[] refuse = {true};
            this.combo.onEvent(ComboBox.OPEN, e -> {
                this.asked.add("open");
                if (refuse[0]) {
                    e.cancel();
                }
            });
            this.clickBox();
            this.combo.requestFocus();
            CollectionEventsTest.this.key(Key.SPACE);
            assertFalse(this.combo.isOpen());
            assertEquals(List.of(), this.opened);
            refuse[0] = false;
            this.clickBox();
            assertTrue(this.combo.isOpen());
            assertEquals(3, this.asked.size());
            assertEquals(List.of("opened"), this.opened);
        }

        @Test
        void openingFromCodeReportsButNeverAsks() {
            this.show();
            this.combo.onEvent(ComboBox.OPEN, e -> this.asked.add("open"));
            this.combo.open();
            assertTrue(this.combo.isOpen());
            assertEquals(List.of(), this.asked);
            assertEquals(List.of("opened"), this.opened);
        }

        @Test
        void aRowPickedFromTheListAsksAfterTheListClosed() {
            this.show();
            this.combo.onEvent(ComboBox.SELECT, e -> this.asked.add(e.value() + (this.combo.isOpen() ? " open" : " closed")));
            this.clickBox();
            this.clickThirdRow();
            assertFalse(this.combo.isOpen());
            assertEquals(List.of("2 closed"), this.asked);
            assertEquals(List.of("0->2"), this.heard);
        }

        @Test
        void keysOnTheClosedBoxAskAndCanBeRefusedOrRewritten() {
            this.show();
            final Integer[] rewrite = {null};
            final boolean[] refuse = {false};
            this.combo.onEvent(ComboBox.SELECT, e -> {
                this.asked.add(e.value());
                if (refuse[0]) {
                    e.cancel();
                } else if (rewrite[0] != null) {
                    e.setValue(rewrite[0]);
                }
            });
            this.combo.requestFocus();
            CollectionEventsTest.this.key(Key.DOWN);
            refuse[0] = true;
            CollectionEventsTest.this.key(Key.DOWN);
            refuse[0] = false;
            rewrite[0] = 0;
            CollectionEventsTest.this.key(Key.END);
            rewrite[0] = 7;
            CollectionEventsTest.this.key(Key.END);
            assertEquals(0, this.combo.selectedIndex());
            assertEquals(List.of(1, 2, 2, 2), this.asked);
            assertEquals(List.of("0->1", "1->0"), this.heard);
        }

        @Test
        void codeReportsButNeverAsks() {
            this.show();
            this.combo.onEvent(ComboBox.SELECT, e -> this.asked.add(e.value()));
            this.combo.select(2);
            this.combo.selectOption("b");
            this.combo.setOptions(List.of("z", "b"));
            this.combo.setOptions(List.of());
            assertEquals(List.of(), this.asked);
            // "b" keeps its index in the new options, so only the last change reports
            assertEquals(List.of("0->2", "2->1", "1->-1"), this.heard);
        }

        /**
         * Listens for every list event on the open popup's list.
         */
        private void watchPopup(final List<String> leaked) {
            final Component<?> popup = CollectionEventsTest.this.ui.overlays().open().getFirst().content();
            popup.onEvent(ListView.SELECT, e -> leaked.add("select"));
            popup.onEvent(ListView.SELECTED, e -> leaked.add("selected"));
            popup.onEvent(ListView.ACTIVATE, e -> leaked.add("activate"));
            popup.onEvent(ListView.ACTIVATED, e -> leaked.add("activated"));
        }

        @Test
        void thePopupListFiresNothingOfItsOwn() {
            this.show();
            final List<String> leaked = new ArrayList<>();
            CollectionEventsTest.this.panel.onEvent(ListView.SELECTED, e -> leaked.add("panel"));
            this.combo.requestFocus();
            CollectionEventsTest.this.key(Key.SPACE);
            this.watchPopup(leaked);
            CollectionEventsTest.this.key(Key.DOWN);
            CollectionEventsTest.this.key(Key.ENTER);
            CollectionEventsTest.this.key(Key.SPACE);
            this.watchPopup(leaked);
            this.clickThirdRow();
            assertEquals(List.of(), leaked);
            assertEquals(List.of("0->1", "1->2"), this.heard);
        }

        @Test
        void anAncestorCanRefuse() {
            this.show();
            CollectionEventsTest.this.panel.onEvent(ComboBox.SELECT, e -> e.cancel());
            this.combo.requestFocus();
            CollectionEventsTest.this.key(Key.DOWN);
            assertEquals(0, this.combo.selectedIndex());
            assertEquals(List.of(), this.heard);
        }
    }

    record Offer(String name, double price) {
    }

    @Nested
    class Tables {
        private final Offer apple = new Offer("apple", 3);
        private final Offer cherry = new Offer("Cherry", 1);
        private final Offer banana = new Offer("banana", 2);
        private final List<Object> asked = new ArrayList<>();
        private final List<String> heard = new ArrayList<>();
        private TableView<Offer> table;

        private void show() {
            this.table = CollectionEventsTest.this.ui.<Offer>tableView()
                    .addTextColumn("Name", GridLength.weighted(1), Offer::name)
                    .setItems(List.of(this.apple, this.cherry, this.banana));
            this.table.setSize(200, 120);
            this.table.onEvent(TableView.SELECTED, e -> this.heard.add(change(e)));
            CollectionEventsTest.this.show(this.table, 0, 0);
        }

        @Test
        void aPressAsksWithTheShownIndex() {
            this.show();
            final Integer[] rewrite = {null};
            final boolean[] refuse = {false};
            this.table.onEvent(TableView.SELECT, e -> {
                this.asked.add(e.value());
                if (refuse[0]) {
                    e.cancel();
                } else if (rewrite[0] != null) {
                    e.setValue(rewrite[0]);
                }
            });
            this.table.sortBy(0, true);
            CollectionEventsTest.this.test.click(10, 30);
            assertEquals(Optional.of(this.apple), this.table.selected());
            refuse[0] = true;
            CollectionEventsTest.this.test.click(10, 50);
            assertEquals(Optional.of(this.apple), this.table.selected());
            refuse[0] = false;
            rewrite[0] = 2;
            CollectionEventsTest.this.test.click(10, 50);
            assertEquals(Optional.of(this.cherry), this.table.selected());
            rewrite[0] = -1;
            CollectionEventsTest.this.test.click(10, 50);
            assertEquals(Optional.empty(), this.table.selected());
            assertEquals(List.of(0, 1, 1, 1), this.asked);
            assertEquals(List.of("-1->0", "0->2", "2->-1"), this.heard);
        }

        @Test
        void codeAndSortingReportButNeverAsk() {
            this.show();
            this.table.onEvent(TableView.SELECT, e -> this.asked.add(e.value()));
            this.table.select(1);
            this.table.selectItem(this.banana);
            // Sorting by name moves banana to the middle, where the descending sort keeps it
            this.table.sortBy(0, true);
            this.table.sortBy(0, false);
            assertEquals(List.of(), this.asked);
            assertEquals(List.of("-1->1", "1->2", "2->1"), this.heard);
            assertEquals(Optional.of(this.banana), this.table.selected());
        }

        @Test
        void aDoublePressAsksToActivate() {
            this.show();
            final List<String> activated = new ArrayList<>();
            this.table.onEvent(TableView.ACTIVATED, e -> activated.add(this.table.shownItems().get(e.value()).name()
                    + " " + change(e)));
            CollectionEventsTest.this.test.click(10, 50);
            CollectionEventsTest.this.test.click(10, 50);
            assertEquals(List.of("Cherry 1->1"), activated);
            this.table.onEvent(TableView.ACTIVATE, e -> e.setValue(2));
            CollectionEventsTest.this.key(Key.ENTER);
            this.table.onEvent(TableView.ACTIVATE, 1, e -> e.cancel());
            CollectionEventsTest.this.key(Key.ENTER);
            assertEquals(List.of("Cherry 1->1", "banana 2->2"), activated);
        }

        @Test
        void aTitleClickAsksToSort() {
            this.show();
            final List<String> sorted = new ArrayList<>();
            this.table.onEvent(TableView.SORTED, e -> sorted.add(change(e)));
            final boolean[] refuse = {true};
            this.table.onEvent(TableView.SORT, e -> {
                this.asked.add(e.value());
                if (refuse[0]) {
                    e.cancel();
                }
            });
            CollectionEventsTest.this.test.click(10, 10);
            assertEquals(-1, this.table.sortColumn());
            assertEquals(List.of(this.apple, this.cherry, this.banana), this.table.shownItems());
            refuse[0] = false;
            CollectionEventsTest.this.test.click(10, 10);
            CollectionEventsTest.this.test.click(10, 10);
            assertEquals(List.of(new TableView.Sort(0, true), new TableView.Sort(0, true), new TableView.Sort(0, false)),
                    this.asked);
            assertEquals(List.of(this.cherry, this.banana, this.apple), this.table.shownItems());
            assertEquals(List.of("Sort[column=-1, ascending=true]->Sort[column=0, ascending=true]",
                    "Sort[column=0, ascending=true]->Sort[column=0, ascending=false]"), sorted);
        }

        @Test
        void aRewrittenSortIsAppliedAndABadOneIgnored() {
            this.show();
            final List<TableView.Sort> sorted = new ArrayList<>();
            this.table.onEvent(TableView.SORTED, e -> sorted.add(e.value()));
            final TableView.Sort[] rewrite = {new TableView.Sort(0, false)};
            this.table.onEvent(TableView.SORT, e -> e.setValue(rewrite[0]));
            CollectionEventsTest.this.test.click(10, 10);
            assertFalse(this.table.isSortAscending());
            rewrite[0] = new TableView.Sort(4, true);
            CollectionEventsTest.this.test.click(10, 10);
            assertEquals(List.of(new TableView.Sort(0, false)), sorted);
        }

        @Test
        void sortingFromCodeReportsOnlyChangesAndNeverAsks() {
            this.show();
            final List<TableView.Sort> sorted = new ArrayList<>();
            this.table.onEvent(TableView.SORTED, e -> sorted.add(e.value()));
            this.table.onEvent(TableView.SORT, e -> this.asked.add(e.value()));
            this.table.sortBy(0, true);
            this.table.sortBy(0, true);
            this.table.setColumnComparator(0, null);
            assertEquals(List.of(), this.asked);
            assertEquals(List.of(new TableView.Sort(0, true), new TableView.Sort(-1, true)), sorted);
        }

        @Test
        void theBodyListsEventsStayInsideTheTable() {
            this.show();
            final List<String> seen = new ArrayList<>();
            final AbsolutePanel panel = CollectionEventsTest.this.panel;
            panel.onEvent(ListView.SELECT, e -> seen.add("list select"));
            panel.onEvent(ListView.SELECTED, e -> seen.add("list selected"));
            panel.onEvent(ListView.ACTIVATED, e -> seen.add("list activated"));
            this.table.onEvent(ListView.SELECTED, e -> seen.add("list selected at the table"));
            panel.onEvent(TableView.SELECTED, e -> seen.add("table selected"));
            panel.onEvent(TableView.ACTIVATED, e -> seen.add("table activated"));
            CollectionEventsTest.this.test.click(10, 50);
            CollectionEventsTest.this.test.click(10, 50);
            this.table.select(0);
            assertEquals(List.of("table selected", "table activated", "table selected"), seen);
        }
    }

    record Node(String name, List<Node> kids) {
        Node(final String name, final Node... kids) {
            this(name, Arrays.asList(kids));
        }
    }

    @Nested
    class Trees {
        private final Node leaf = new Node("A1");
        private final Node a = new Node("A", this.leaf);
        private final Node b = new Node("B");
        private final Node leafC = new Node("C1");
        private final Node c = new Node("C", this.leafC);
        private final List<Object> asked = new ArrayList<>();
        private final List<String> heard = new ArrayList<>();
        private final List<String> expansions = new ArrayList<>();
        private TreeView<Node> tree;

        private void show() {
            final ComponentFactory ui = CollectionEventsTest.this.ui;
            this.tree = ui.<Node>treeView(node -> ui.label(node.name()))
                    .setChildren(Node::kids)
                    .setRoots(List.of(this.a, this.b, this.c));
            this.tree.setSize(200, 200);
            this.tree.onEvent(TreeView.SELECTED, e -> this.heard.add(change(e)));
            this.tree.onEvent(TreeView.EXPANDED, e -> {
                assertEquals(e.value().item(), e.previous().item());
                assertEquals(!e.value().expanded(), e.previous().expanded());
                this.expansions.add(((Node) e.value().item()).name() + (e.value().expanded() ? "+" : "-"));
            });
            CollectionEventsTest.this.show(this.tree, 0, 0);
        }

        @Test
        void pressesAndKeysAskToSelect() {
            this.show();
            final Integer[] rewrite = {null};
            final boolean[] refuse = {false};
            this.tree.onEvent(TreeView.SELECT, e -> {
                this.asked.add(e.value());
                if (refuse[0]) {
                    e.cancel();
                } else if (rewrite[0] != null) {
                    e.setValue(rewrite[0]);
                }
            });
            CollectionEventsTest.this.test.click(50, 30);
            assertEquals(Optional.of(this.b), this.tree.selected());
            rewrite[0] = 0;
            CollectionEventsTest.this.key(Key.DOWN);
            assertEquals(Optional.of(this.a), this.tree.selected());
            rewrite[0] = null;
            refuse[0] = true;
            CollectionEventsTest.this.key(Key.DOWN);
            assertEquals(Optional.of(this.a), this.tree.selected());
            assertEquals(List.of(1, 2, 1), this.asked);
            assertEquals(List.of("-1->1", "1->0"), this.heard);
        }

        @Test
        void rightAndLeftAskToExpandAndSelect() {
            this.show();
            final boolean[] refuse = {true};
            this.tree.onEvent(TreeView.SELECT, e -> this.asked.add(e.value()));
            this.tree.onEvent(TreeView.EXPAND, e -> {
                this.asked.add(((Node) e.value().item()).name() + (e.value().expanded() ? "+" : "-"));
                if (refuse[0]) {
                    e.cancel();
                }
            });
            CollectionEventsTest.this.test.click(50, 10);
            CollectionEventsTest.this.key(Key.RIGHT);
            assertFalse(this.tree.isExpanded(this.a));
            refuse[0] = false;
            CollectionEventsTest.this.key(Key.RIGHT);
            assertTrue(this.tree.isExpanded(this.a));
            CollectionEventsTest.this.key(Key.RIGHT);
            assertEquals(Optional.of(this.leaf), this.tree.selected());
            CollectionEventsTest.this.key(Key.LEFT);
            CollectionEventsTest.this.key(Key.LEFT);
            assertFalse(this.tree.isExpanded(this.a));
            assertEquals(List.of(0, "A+", "A+", 1, 0, "A-"), this.asked);
            assertEquals(List.of("A+", "A-"), this.expansions);
        }

        @Test
        void aRewrittenExpansionAppliesToTheItemItNames() {
            this.show();
            this.tree.onEvent(TreeView.EXPAND, e -> e.setValue(new TreeView.Expansion<>(this.c, true)));
            CollectionEventsTest.this.test.click(6, 10);
            assertFalse(this.tree.isExpanded(this.a));
            assertTrue(this.tree.isExpanded(this.c));
            assertEquals(List.of(this.a, this.b, this.c, this.leafC), this.tree.shownItems());
            assertEquals(List.of("C+"), this.expansions);
        }

        @Test
        void codeReportsOnceForEachChangeButNeverAsks() {
            this.show();
            this.tree.onEvent(TreeView.SELECT, e -> this.asked.add(e.value()));
            this.tree.onEvent(TreeView.EXPAND, e -> this.asked.add(e.value()));
            this.tree.select(this.leaf);
            this.tree.setExpanded(this.a, false);
            this.tree.expandAll();
            this.tree.collapseAll();
            assertEquals(List.of(), this.asked);
            assertEquals(List.of("-1->1", "1->0"), this.heard);
            assertEquals(List.of("A+", "A-", "C+", "A+", "A-", "C-"), this.expansions);
        }

        @Test
        void expandingAboveTheSelectionReportsItsNewRow() {
            this.show();
            this.tree.select(this.b);
            this.tree.setExpanded(this.a, true);
            assertEquals(List.of("-1->1", "1->2"), this.heard);
            assertEquals(Optional.of(this.b), this.tree.selected());
        }

        @Test
        void aDoublePressAsksToActivate() {
            this.show();
            final List<String> activated = new ArrayList<>();
            this.tree.onEvent(TreeView.ACTIVATED, e -> activated.add(this.tree.shownItems().get(e.value()).name()
                    + " " + change(e)));
            CollectionEventsTest.this.test.click(50, 30);
            CollectionEventsTest.this.test.click(50, 30);
            this.tree.onEvent(TreeView.ACTIVATE, e -> e.cancel());
            CollectionEventsTest.this.key(Key.ENTER);
            assertEquals(List.of("B 1->1"), activated);
        }

        @Test
        void theRowListsEventsStayInsideTheTree() {
            this.show();
            final List<String> seen = new ArrayList<>();
            final AbsolutePanel panel = CollectionEventsTest.this.panel;
            panel.onEvent(ListView.SELECTED, e -> seen.add("list selected"));
            panel.onEvent(ListView.ACTIVATED, e -> seen.add("list activated"));
            this.tree.onEvent(ListView.SELECTED, e -> seen.add("list selected at the tree"));
            panel.onEvent(TreeView.SELECTED, e -> seen.add("tree selected"));
            panel.onEvent(TreeView.SELECT, e -> e.cancel());
            CollectionEventsTest.this.test.click(50, 30);
            this.tree.select(this.b);
            assertEquals(List.of("tree selected"), seen);
        }
    }

    @Nested
    class SlotGrids {
        private final List<Object> asked = new ArrayList<>();
        private final List<String> heard = new ArrayList<>();
        private final List<SlotGrid.Click<?>> clicks = new ArrayList<>();
        private SlotGrid<String> grid;

        private void show() {
            final ComponentFactory ui = CollectionEventsTest.this.ui;
            this.grid = ui.<String>slotGrid(ui::label).setColumns(3).setItems(List.of("a", "b", "c")).setSelectable(true);
            this.grid.onEvent(SlotGrid.SELECTED, e -> this.heard.add(change(e)))
                    .onEvent(SlotGrid.CLICKED, e -> {
                        assertEquals(e.value(), e.previous());
                        this.clicks.add(e.value());
                    });
            CollectionEventsTest.this.show(this.grid, 0, 0);
        }

        @Test
        void aLeftClickAsksToSelect() {
            this.show();
            final Integer[] rewrite = {null};
            final boolean[] refuse = {false};
            this.grid.onEvent(SlotGrid.SELECT, e -> {
                this.asked.add(e.value());
                if (refuse[0]) {
                    e.cancel();
                } else if (rewrite[0] != null) {
                    e.setValue(rewrite[0]);
                }
            });
            CollectionEventsTest.this.test.click(23, 5);
            refuse[0] = true;
            CollectionEventsTest.this.test.click(5, 5);
            assertEquals(1, this.grid.selectedIndex());
            refuse[0] = false;
            rewrite[0] = 2;
            CollectionEventsTest.this.test.click(5, 5);
            rewrite[0] = 5;
            CollectionEventsTest.this.test.click(5, 5);
            assertEquals(2, this.grid.selectedIndex());
            assertEquals(List.of(1, 0, 0, 0), this.asked);
            assertEquals(List.of("-1->1", "1->2"), this.heard);
        }

        @Test
        void codeReportsButNeverAsks() {
            this.show();
            this.grid.onEvent(SlotGrid.SELECT, e -> this.asked.add(e.value()));
            this.grid.select(2);
            this.grid.setItems(List.of("a"));
            assertEquals(List.of(), this.asked);
            assertEquals(List.of("-1->2", "2->-1"), this.heard);
        }

        @Test
        void aClickAsksAndReportsEvenWhenTheSelectionWasRefused() {
            this.show();
            this.grid.onEvent(SlotGrid.SELECT, e -> e.cancel());
            this.grid.onEvent(SlotGrid.CLICK, e -> this.asked.add(e.value().index()));
            CollectionEventsTest.this.rightClick(41, 5);
            CollectionEventsTest.this.test.click(5, 5);
            assertEquals(-1, this.grid.selectedIndex());
            assertEquals(List.of(2, 0), this.asked);
            assertEquals(2, this.clicks.size());
            assertEquals(new SlotGrid.Click<>(2, "c", MouseButton.RIGHT, Modifiers.NONE), this.clicks.getFirst());
        }

        @Test
        void aCanceledClickReportsNothingButStillSelects() {
            this.show();
            this.grid.onEvent(SlotGrid.CLICK, e -> e.cancel());
            CollectionEventsTest.this.test.click(5, 5);
            assertEquals(List.of(), this.clicks);
            assertEquals(0, this.grid.selectedIndex());
        }

        @Test
        void aRewrittenClickIsReportedAsGivenUnlessItsSlotIsMissing() {
            this.show();
            final int[] index = {1};
            this.grid.onEvent(SlotGrid.CLICK, e -> e.setValue(new SlotGrid.Click<>(index[0], "x", MouseButton.MIDDLE,
                    Modifiers.NONE)));
            CollectionEventsTest.this.test.click(5, 5);
            index[0] = 3;
            CollectionEventsTest.this.test.click(5, 5);
            assertEquals(List.of(new SlotGrid.Click<>(1, "x", MouseButton.MIDDLE, Modifiers.NONE)), this.clicks);
        }

        @Test
        void anAncestorCanRefuse() {
            this.show();
            CollectionEventsTest.this.panel.onEvent(SlotGrid.CLICK, e -> e.cancel());
            CollectionEventsTest.this.test.click(5, 5);
            assertEquals(List.of(), this.clicks);
        }
    }
}
