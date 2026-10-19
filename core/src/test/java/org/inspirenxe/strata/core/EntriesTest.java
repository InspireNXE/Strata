package org.inspirenxe.strata.core;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Tooltip;
import org.inspirenxe.strata.api.control.attribute.item.SingleSelection;
import org.inspirenxe.strata.api.control.collection.ListView;
import org.inspirenxe.strata.api.control.collection.TableColumn;
import org.inspirenxe.strata.api.control.collection.TableView;
import org.inspirenxe.strata.api.control.display.Image;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.control.display.Separator;
import org.inspirenxe.strata.api.control.menu.CheckMenuItem;
import org.inspirenxe.strata.api.control.menu.Menu;
import org.inspirenxe.strata.api.control.menu.MenuBar;
import org.inspirenxe.strata.api.control.menu.MenuEntry;
import org.inspirenxe.strata.api.control.menu.MenuItem;
import org.inspirenxe.strata.api.control.menu.SubmenuItem;
import org.inspirenxe.strata.api.control.pane.SplitPane;
import org.inspirenxe.strata.api.control.pane.Tab;
import org.inspirenxe.strata.api.control.pane.TabBar;
import org.inspirenxe.strata.api.control.window.MessageBox;
import org.inspirenxe.strata.api.event.control.ValueEvent;
import org.inspirenxe.strata.api.layout.GridLength;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.api.layout.VerticalAlignment;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;
import org.inspirenxe.strata.core.node.ContainerNode;
import org.inspirenxe.strata.core.node.Node;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Entries modeled in the API (menu items, menu bar titles, tabs and table columns) and the factory's forms that take
 * no arguments.
 */
class EntriesTest {
    private final TestHost test = new TestHost(300, 300);
    private final ComponentFactory ui = this.test.factory();
    private final List<String> heard = new ArrayList<>();

    private static String change(final ValueEvent<?, ?> event) {
        return event.previous() + "->" + event.value();
    }

    @Nested
    class Menus {

        @Test
        void shorthandsMakeEntriesTheMenuLists() {
            final ComponentFactory ui = EntriesTest.this.ui;
            final Menu submenu = ui.menu();
            final Menu menu = ui.menu()
                    .addItem("Copy", "Ctrl+C", () -> { })
                    .addSeparator()
                    .addCheckItem("Prices", true, selected -> { })
                    .addSubmenu("More", submenu);
            final List<MenuEntry> items = menu.items();
            assertEquals(4, items.size());
            final MenuItem copy = (MenuItem) items.getFirst();
            assertEquals("Copy", copy.text());
            assertEquals("Ctrl+C", copy.shortcut());
            assertTrue(((CheckMenuItem) items.get(2)).isSelected());
            assertSame(submenu, ((SubmenuItem) items.get(3)).menu().orElseThrow());
        }

        @Test
        void anEntryBelongsToOneMenuAtATime() {
            final MenuItem item = EntriesTest.this.ui.menuItem("Copy");
            final Menu first = EntriesTest.this.ui.menu().addItem(item);
            final Menu second = EntriesTest.this.ui.menu();
            assertThrows(IllegalArgumentException.class, () -> second.addItem(item));
            assertThrows(IllegalArgumentException.class, () -> first.addItem(item));
            first.removeItem(item);
            second.addItem(0, item);
            assertEquals(List.of(item), second.items());
            assertEquals(List.of(), first.items());
        }

        @Test
        void aSubmenuOpensFromOnePlace() {
            final ComponentFactory ui = EntriesTest.this.ui;
            final Menu shared = ui.menu();
            final Menu menu = ui.menu().addSubmenu("A", shared);
            assertThrows(IllegalArgumentException.class, () -> ui.menu().addSubmenu("B", shared));
            assertThrows(IllegalArgumentException.class, () -> shared.addSubmenu("Loop", menu));
            menu.removeItem(menu.items().getFirst());
            ui.menu().addSubmenu("B", shared);
        }

        @Test
        void choosingAnEntryRunsItsActionAndReportsIt() {
            final MenuItem copy = EntriesTest.this.ui.menuItem("Copy").setAction(() -> EntriesTest.this.heard.add("copy"));
            final Menu menu = EntriesTest.this.ui.menu().addItem(copy);
            menu.onEvent(Menu.CHOSEN, e -> assertSame(copy, e.value()));
            EntriesTest.this.test.show(EntriesTest.this.ui.absolutePanel());
            menu.openAt(10, 10);
            EntriesTest.this.test.host().layout();
            EntriesTest.this.test.click(30, 10 + 2 + 7);
            assertEquals(List.of("copy"), EntriesTest.this.heard);
            assertFalse(menu.isOpen());
        }

        @Test
        void aMenuBarHoldsSubmenuItems() {
            final ComponentFactory ui = EntriesTest.this.ui;
            final SubmenuItem file = ui.submenuItem("File").setMenu(ui.menu().addItem("Quit", () -> { }));
            final MenuBar bar = ui.menuBar()
                    .addMenu(file)
                    .addMenu("View", ui.menu().addItem("Zoom", () -> { }));
            assertEquals(List.of("File", "View"), bar.menus().stream().map(SubmenuItem::text).toList());
            EntriesTest.this.test.show(ui.absolutePanel().add(bar, 0, 0));
            file.setEnabled(false);
            bar.select(0);
            assertEquals(-1, bar.selectedIndex());
            bar.select(1);
            assertEquals(1, bar.selectedIndex());
            bar.removeMenu(bar.menus().get(1));
            assertEquals(-1, bar.selectedIndex());
            assertEquals(List.of(file), bar.menus());
        }
    }

    @Nested
    class Tabs {

        @Test
        void tabsAreEntriesTheBarSelects() {
            final ComponentFactory ui = EntriesTest.this.ui;
            final Tab sell = ui.tab("Sell");
            final TabBar bar = ui.tabBar().addTab("Buy").addTab(sell);
            bar.onEvent(SingleSelection.SELECTED, e -> EntriesTest.this.heard.add(change(e)));
            bar.selectItem(sell);
            assertEquals(Optional.of(sell), bar.selected());
            bar.addTab(0, ui.tab("Trade"));
            assertEquals(2, bar.selectedIndex());
            bar.removeTab(sell);
            assertEquals("Buy", bar.selected().orElseThrow().text());
            assertEquals(List.of("0->1", "1->2", "2->1"), EntriesTest.this.heard);
            assertThrows(IllegalArgumentException.class, () -> ui.tabBar().addTab(bar.tabs().getFirst()));
        }

        @Test
        void aTabChangeShowsInItsBar() {
            final Tab buy = EntriesTest.this.ui.tab("Buy");
            final TabBar bar = EntriesTest.this.ui.tabBar().addTab(buy).addTab("Sell");
            bar.setWidth(200);
            EntriesTest.this.test.show(EntriesTest.this.ui.absolutePanel().add(bar, 0, 0));
            bar.tabs().get(1).setEnabled(false).setTooltip(Tooltip.text("Empty"));
            EntriesTest.this.test.click(150, 5);
            assertEquals(0, bar.selectedIndex());
            assertTrue(EntriesTest.this.test.host().root().tooltip().isPresent());
        }
    }

    @Nested
    class Columns {
        record Offer(String name, double price) {
        }

        @Test
        void columnsAreEntriesTheTableSortsBy() {
            final ComponentFactory ui = EntriesTest.this.ui;
            final TableColumn<Offer> price = ui.<Offer>tableColumn("Price")
                    .setWidth(GridLength.fixed(60))
                    .setCellText(offer -> Double.toString(offer.price()))
                    .setComparator(Comparator.comparingDouble(Offer::price));
            final Offer apple = new Offer("Apple", 3);
            final Offer pear = new Offer("Pear", 1);
            final TableView<Offer> table = ui.<Offer>tableView()
                    .addTextColumn("Name", GridLength.weighted(1), Offer::name)
                    .addColumn(price)
                    .setItems(List.of(apple, pear));
            table.onEvent(TableView.SORTED, e -> EntriesTest.this.heard.add(change(e)));
            table.sortBy(price, true);
            assertEquals(List.of(pear, apple), table.shownItems());
            price.setComparator(null);
            assertEquals(Optional.empty(), table.sortColumn());
            assertEquals(List.of(apple, pear), table.shownItems());
            table.sortBy(table.columns().getFirst(), false);
            table.removeColumn(table.columns().getFirst());
            assertEquals(List.of(price), table.columns());
            assertEquals(4, EntriesTest.this.heard.size());
            assertThrows(IllegalArgumentException.class, () -> table.sortBy(price, true));
            assertThrows(IllegalArgumentException.class, () -> ui.<Offer>tableView().addColumn(price));
        }

        @Test
        void aNewColumnShowsTextAndDoesNotSort() {
            final TableColumn<Offer> column = EntriesTest.this.ui.tableColumn("Offer");
            assertEquals(GridLength.weighted(1f), column.width());
            assertFalse(column.isSortable());
        }
    }

    @Nested
    class Factory {

        @Test
        void everyFactoryMethodHasAFormWithNoArguments() {
            final ComponentFactory ui = EntriesTest.this.ui;
            assertEquals("", ui.label().text());
            assertEquals("", ui.button().text());
            assertFalse(ui.checkBox().isSelected());
            assertFalse(ui.toggleSwitch().isSelected());
            assertEquals("", ui.window().title());
            assertEquals("", ui.expander().title());
            assertEquals(Orientation.HORIZONTAL, ui.separator().orientation());
            assertEquals(Orientation.HORIZONTAL, ui.splitPane().orientation());
            final Image image = ui.image();
            assertNull(image.sprite());
            assertNull(image.visual());
            final MessageBox box = ui.messageBox();
            assertEquals(MessageBox.Buttons.OK, box.buttons());
            assertEquals("", box.message());
        }

        @Test
        void collectionsShowEachItemAsTextByDefault() {
            final ListView<Integer> list = EntriesTest.this.ui.<Integer>listView().setItems(List.of(7, 8));
            list.setSize(100, 100);
            EntriesTest.this.test.show(EntriesTest.this.ui.absolutePanel().add(list, 0, 0));
            final List<String> texts = new ArrayList<>();
            labels(((ComponentFactoryImpl) EntriesTest.this.ui).nodeOf(list), texts);
            assertEquals(List.of("7", "8"), texts);
            assertEquals("String", EntriesTest.this.ui.<String>comboBox().setItems(List.of("String")).selected().orElseThrow());
        }

        private static void labels(final Node node, final List<String> texts) {
            if ((Object) node.facade() instanceof Label label) {
                texts.add(label.text());
            }
            if (node instanceof ContainerNode container) {
                for (final Node child : container.childNodes()) {
                    labels(child, texts);
                }
            }
        }

        @Test
        void orientationAndMessageBoxSettersApply() {
            final Separator line = EntriesTest.this.ui.separator().setOrientation(Orientation.VERTICAL);
            assertEquals(HorizontalAlignment.LEFT, line.horizontalAlignment());
            assertEquals(VerticalAlignment.STRETCH, line.verticalAlignment());
            final SplitPane split = EntriesTest.this.ui.splitPane().setOrientation(Orientation.VERTICAL);
            assertEquals(Orientation.VERTICAL, split.orientation());
            final MessageBox box = EntriesTest.this.ui.messageBox()
                    .setTitle("Delete?")
                    .setMessage("This cannot be undone.")
                    .setButtonText(MessageBox.Result.NO, "Keep")
                    .setButtons(MessageBox.Buttons.YES_NO);
            assertEquals("Delete?", box.window().title());
            assertEquals("This cannot be undone.", box.message());
            assertEquals(MessageBox.Buttons.YES_NO, box.buttons());
        }
    }
}
