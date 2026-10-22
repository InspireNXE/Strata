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
import org.inspirenxe.strata.api.control.attribute.value.Toggle;
import org.inspirenxe.strata.api.control.button.CheckBox;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.control.menu.Menu;
import org.inspirenxe.strata.api.control.menu.RadioMenuItem;
import org.inspirenxe.strata.api.control.pane.Expander;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.layout.Anchor;
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
 * Radio items in menus, and components in an expander's header (design 4.57).
 */
class MenuRadioExpanderTest {
    private final TestHost test = new TestHost(300, 300);
    private final ComponentFactory ui = this.test.factory();
    private final List<String> heard = new ArrayList<>();

    @Nested
    class RadioItems {
        private final Object sortBy = new Object();
        private final RadioMenuItem name = MenuRadioExpanderTest.this.ui.radioMenuItem("Name").setGroup(this.sortBy);
        private final RadioMenuItem date = MenuRadioExpanderTest.this.ui.radioMenuItem("Date").setGroup(this.sortBy);
        private final RadioMenuItem size = MenuRadioExpanderTest.this.ui.radioMenuItem("Size").setGroup(this.sortBy);
        private final RadioMenuItem loose = MenuRadioExpanderTest.this.ui.radioMenuItem("Loose");
        private final Menu menu = MenuRadioExpanderTest.this.ui.menu()
                .addItem(this.name).addItem(this.date).addItem(this.size).addItem(this.loose);

        @Test
        void choosingOneSelectsItAndClearsTheOthersOfItsGroup() {
            final MenuRadioExpanderTest outer = MenuRadioExpanderTest.this;
            this.name.setSelected(true).setAction(() -> outer.heard.add("sort by name"));
            this.loose.setSelected(true);
            this.date.setAction(() -> outer.heard.add("sort by date"));
            this.menu.onEvent(Menu.CHOSEN, event -> outer.heard.add("chosen " + event.value()));
            outer.test.show(outer.ui.button("Target").setAnchor(Anchor.TOP_LEFT));
            this.menu.openAt(10, 10);
            outer.test.host().layout();
            outer.test.host().key(Key.DOWN, KeyAction.PRESS, Modifiers.NONE);
            outer.test.host().key(Key.DOWN, KeyAction.PRESS, Modifiers.NONE);
            outer.test.host().key(Key.ENTER, KeyAction.PRESS, Modifiers.NONE);
            assertTrue(this.date.isSelected());
            assertFalse(this.name.isSelected());
            assertFalse(this.size.isSelected());
            assertTrue(this.loose.isSelected());
            assertEquals(List.of("sort by date", "chosen RadioMenuItem[Date]"), outer.heard);
            assertFalse(this.menu.isOpen());
        }

        @Test
        void codeSelectsWithinTheGroupAndItemsWithoutOneShareTheirOwn() {
            final RadioMenuItem other = MenuRadioExpanderTest.this.ui.radioMenuItem("Other");
            this.menu.addItem(other);
            this.size.setSelected(true);
            this.name.setSelected(true);
            assertFalse(this.size.isSelected());
            this.loose.setSelected(true);
            other.setSelected(true);
            assertFalse(this.loose.isSelected());
            assertTrue(this.name.isSelected());
            assertEquals(Optional.of(this.sortBy), this.name.group());
            assertEquals(Optional.empty(), other.group());
            this.name.setSelected(false);
            assertFalse(this.name.isSelected());
        }

        @Test
        void aDisabledItemCannotBeChosen() {
            final MenuRadioExpanderTest outer = MenuRadioExpanderTest.this;
            final RadioMenuItem off = outer.ui.radioMenuItem("Off").setEnabled(false);
            final Menu only = outer.ui.menu().addItem(off);
            assertFalse(off.isEnabled());
            outer.test.show(outer.ui.button("Target").setAnchor(Anchor.TOP_LEFT));
            only.openAt(10, 10);
            outer.test.host().key(Key.ENTER, KeyAction.PRESS, Modifiers.NONE);
            assertFalse(off.isSelected());
        }
    }

    @Nested
    class Headers {
        private final CheckBox enable = MenuRadioExpanderTest.this.ui.checkBox("Proxy");
        private final Label body = MenuRadioExpanderTest.this.ui.label("Host and port");
        private final Expander expander = MenuRadioExpanderTest.this.ui.expander("Proxy settings")
                .setHeader(this.enable).setContent(this.body);

        Headers() {
            final MenuRadioExpanderTest outer = MenuRadioExpanderTest.this;
            this.expander.setWidth(200);
            outer.test.show(outer.ui.stackPanel().add(this.expander)
                    .onEvent(Toggle.TOGGLED, event -> outer.heard.add("toggled " + event.value()))
                    .setAnchor(Anchor.TOP_LEFT));
            outer.test.host().layout();
        }

        @Test
        void theComponentTakesTheTitlesPlaceAfterTheArrow() {
            assertEquals(Optional.of(this.enable), this.expander.header());
            final Rect box = this.enable.bounds();
            assertEquals(4f + 7f + 4f, box.x());
            assertTrue(this.expander.bounds().height() >= box.height());
            final DrawLog log = new DrawLog();
            MenuRadioExpanderTest.this.test.host().render(log);
            assertTrue(log.lines("text").stream().noneMatch(line -> line.contains("Proxy settings")));
            assertTrue(log.lines("text").stream().anyMatch(line -> line.contains("Proxy")));
        }

        @Test
        void aClickOnTheControlGoesToItAndElsewhereOnTheHeaderFlipsTheExpander() {
            final Rect box = this.enable.bounds();
            MenuRadioExpanderTest.this.test.click(box.x() + 4f, box.y() + box.height() / 2f);
            assertTrue(this.enable.isSelected());
            assertFalse(this.expander.isExpanded());
            assertEquals(List.of("toggled true"), MenuRadioExpanderTest.this.heard);
            MenuRadioExpanderTest.this.test.platform().advanceMillis(1000);
            MenuRadioExpanderTest.this.test.click(190f, box.y() + box.height() / 2f);
            assertTrue(this.expander.isExpanded());
            MenuRadioExpanderTest.this.test.host().layout();
            assertTrue(this.body.bounds().y() >= box.bottom());
        }

        @Test
        void theHeaderComponentComesBeforeTheContentAndCanBeTakenAway() {
            this.expander.setExpanded(true);
            MenuRadioExpanderTest.this.test.host().layout();
            final List<?> order = MenuRadioExpanderTest.this.test.tabOrder();
            assertSame(this.expander, order.get(0));
            assertSame(this.enable, order.get(1));
            this.expander.setHeader(null);
            assertTrue(this.enable.parent().isEmpty());
            assertEquals(Optional.empty(), this.expander.header());
        }
    }
}
