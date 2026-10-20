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
import org.inspirenxe.strata.api.control.attribute.item.SingleSelection;
import org.inspirenxe.strata.api.control.attribute.popup.Closable;
import org.inspirenxe.strata.api.control.attribute.popup.Openable;
import org.inspirenxe.strata.api.control.attribute.text.HasText;
import org.inspirenxe.strata.api.control.attribute.value.Ranged;
import org.inspirenxe.strata.api.control.attribute.value.Toggle;
import org.inspirenxe.strata.api.control.button.CheckBox;
import org.inspirenxe.strata.api.control.button.RadioButton;
import org.inspirenxe.strata.api.control.button.RadioGroup;
import org.inspirenxe.strata.api.control.button.ToggleSwitch;
import org.inspirenxe.strata.api.control.collection.ComboBox;
import org.inspirenxe.strata.api.control.collection.GridView;
import org.inspirenxe.strata.api.control.collection.TableView;
import org.inspirenxe.strata.api.control.display.ProgressBar;
import org.inspirenxe.strata.api.control.menu.Menu;
import org.inspirenxe.strata.api.control.menu.MenuBar;
import org.inspirenxe.strata.api.control.pane.Tab;
import org.inspirenxe.strata.api.control.pane.TabBar;
import org.inspirenxe.strata.api.event.control.ValueEvent;
import org.inspirenxe.strata.api.layout.AbsolutePanel;
import org.inspirenxe.strata.api.layout.GridLength;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The attributes controls share: one handler for an attribute's event hears every control that has it, and the
 * methods an attribute adds behave the same on each control.
 */
class AttributesTest {
    private final TestHost test = new TestHost(300, 300);
    private final ComponentFactory ui = this.test.factory();
    private final AbsolutePanel panel = this.ui.absolutePanel();
    private final List<String> heard = new ArrayList<>();

    private void show(final Component<?> control, final float x, final float y) {
        this.test.show(this.panel.add(control, x, y));
    }

    private static String change(final ValueEvent<?, ?> event) {
        return event.previous() + "->" + event.value();
    }

    @Nested
    class Toggles {

        @Test
        void oneHandlerHearsEveryKindOfToggle() {
            final ComponentFactory ui = AttributesTest.this.ui;
            final CheckBox box = ui.checkBox("Box");
            final ToggleSwitch flip = ui.toggleSwitch("Flip");
            final RadioGroup<String> group = ui.radioGroup();
            final RadioButton a = group.add("A", "a");
            final RadioButton b = group.add("B", "b");
            AttributesTest.this.panel.add(box, 0, 0).add(flip, 0, 30).add(a, 0, 60).add(b, 0, 90);
            AttributesTest.this.test.show(AttributesTest.this.panel);
            AttributesTest.this.panel.onEvent(Toggle.TOGGLED, e -> AttributesTest.this.heard.add(
                    ((HasText<?>) e.target()).text().plain() + " " + e.value()));
            box.setSelected(true);
            flip.toggle();
            group.selectItem("a");
            group.selectItem("b");
            assertEquals(List.of("Box true", "Flip true", "A true", "A false", "B true"), AttributesTest.this.heard);
        }

        @Test
        void aRadioButtonAsksBeforeTheUserSelectsIt() {
            final RadioGroup<String> group = AttributesTest.this.ui.radioGroup();
            final RadioButton a = group.add("A", "a");
            AttributesTest.this.show(a, 0, 0);
            a.onEvent(Toggle.TOGGLE, e -> {
                AttributesTest.this.heard.add("asked " + e.value());
                e.cancel();
            });
            AttributesTest.this.test.click(5, 5);
            assertFalse(a.isSelected());
            assertEquals(List.of("asked true"), AttributesTest.this.heard);
        }
    }

    @Test
    void aProgressBarReportsChangesToItsValue() {
        final ProgressBar bar = this.ui.progressBar();
        bar.onEvent(Ranged.CHANGED, e -> this.heard.add(change(e)));
        bar.setValue(0.5).setValue(0.5).setRange(0, 0.25);
        assertEquals(List.of("0.0->0.5", "0.5->0.25"), this.heard);
    }

    @Nested
    class ComboBoxes {
        record Option(int id, String label) {
        }

        @Test
        void selectionFollowsTheKeyAndCanBeCleared() {
            final ComboBox<Option> combo = AttributesTest.this.ui.<Option>comboBox(Option::label)
                    .setKeyFunction(Option::id)
                    .setItems(List.of(new Option(1, "One"), new Option(2, "Two")));
            combo.selectItem(new Option(2, "Renamed"));
            assertEquals(1, combo.selectedIndex());
            combo.setItems(List.of(new Option(3, "Three"), new Option(2, "Two again")));
            assertEquals(Optional.of(new Option(2, "Two again")), combo.selected());
            combo.clearSelection();
            assertEquals(-1, combo.selectedIndex());
            assertEquals(Optional.empty(), combo.selected());
            combo.selectItem(new Option(9, "Missing"));
            assertEquals(-1, combo.selectedIndex());
        }

        @Test
        void openingAndClosingAreReported() {
            final ComboBox<String> combo = AttributesTest.this.ui.<String>comboBox(String::toString)
                    .setItems(List.of("a", "b"));
            combo.onEvent(Openable.OPENED, e -> AttributesTest.this.heard.add("opened"))
                    .onEvent(Closable.CLOSED, e -> AttributesTest.this.heard.add("closed"));
            AttributesTest.this.show(combo, 0, 0);
            combo.open();
            assertTrue(combo.isOpen());
            combo.close().close();
            assertEquals(List.of("opened", "closed"), AttributesTest.this.heard);
        }
    }

    @Nested
    class GridViews {
        private GridView<String> grid;

        private void show() {
            final ComponentFactory ui = AttributesTest.this.ui;
            this.grid = ui.<String>gridView(ui::label)
                    .setColumns(3)
                    .setItems(List.of("a", "b", "c"));
            this.grid.onEvent(Activatable.ACTIVATED, e -> AttributesTest.this.heard.add("activated " + e.value()));
            AttributesTest.this.show(this.grid, 0, 0);
        }

        @Test
        void aDoubleClickActivatesACell() {
            this.show();
            AttributesTest.this.test.click(20, 5);
            assertEquals(List.of(), AttributesTest.this.heard);
            AttributesTest.this.test.click(20, 5);
            assertEquals(List.of("activated 1"), AttributesTest.this.heard);
        }

        @Test
        void aSingleClickActivatesWhenAsked() {
            this.show();
            this.grid.setActivateOnClick(true);
            AttributesTest.this.test.click(40, 5);
            assertEquals(List.of("activated 2"), AttributesTest.this.heard);
        }

        @Test
        void itemsAreFoundByKey() {
            this.show();
            this.grid.selectItem("c");
            assertEquals(2, this.grid.selectedIndex());
            assertEquals(Optional.of("c"), this.grid.selected());
            this.grid.selectItem("z");
            assertEquals(-1, this.grid.selectedIndex());
        }
    }

    @Test
    void tablesHaveEveryRowViewSetting() {
        final TableView<String> table = this.ui.<String>tableView()
                .addTextColumn("Name", GridLength.weighted(1), name -> name)
                .setItems(List.of("Ana", "Ben", "Cleo"))
                .setRowSpacing(2)
                .setDeselectable(true)
                .setActivateOnClick(true);
        table.setSize(200, 100);
        table.onEvent(Activatable.ACTIVATED, e -> this.heard.add("activated " + e.value()));
        this.show(table, 0, 0);
        assertEquals(2f, table.rowSpacing());
        assertTrue(table.isDeselectable());
        // The header is one row tall; the first body row starts below it
        this.test.click(10, table.rowHeight() + 5);
        assertEquals(List.of("activated 0"), this.heard);
    }

    @Test
    void aTabBarSelectsItsTabs() {
        final TabBar tabs = this.ui.tabBar()
                .addTab("Buy")
                .addTab("Sell");
        tabs.onEvent(SingleSelection.SELECTED, e -> this.heard.add(change(e)));
        final Tab sell = tabs.tabs().get(1);
        tabs.selectItem(sell);
        assertEquals(Optional.of(sell), tabs.selected());
        tabs.selectItem(AttributesTest.this.ui.tab("Trade"));
        assertEquals(-1, tabs.selectedIndex());
        assertEquals(List.of("0->1", "1->-1"), this.heard);
    }

    @Test
    void aMenuBarsSelectionIsItsOpenMenu() {
        final Menu file = this.ui.menu().addItem("Quit", () -> { });
        final Menu view = this.ui.menu().addItem("Zoom", () -> { });
        final MenuBar bar = this.ui.menuBar()
                .addMenu("File", file)
                .addMenu("View", view);
        bar.onEvent(SingleSelection.SELECTED, e -> this.heard.add(change(e)));
        file.onEvent(Openable.OPENED, e -> this.heard.add("file opened"));
        this.show(bar, 0, 0);
        bar.select(0);
        assertSame(file, bar.selected().orElseThrow());
        bar.selectItem(view);
        assertEquals(1, bar.selectedIndex());
        bar.clearSelection();
        assertFalse(view.isOpen());
        assertEquals(List.of("file opened", "-1->0", "0->1", "1->-1"), this.heard);
    }
}
