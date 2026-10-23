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
import org.inspirenxe.strata.api.control.attribute.popup.Openable;
import org.inspirenxe.strata.api.control.button.DropDownButton;
import org.inspirenxe.strata.api.control.collection.ComboBox;
import org.inspirenxe.strata.api.control.display.InfoBar;
import org.inspirenxe.strata.api.control.field.DatePicker;
import org.inspirenxe.strata.api.control.menu.Flyout;
import org.inspirenxe.strata.api.control.menu.Menu;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.layout.AbsolutePanel;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Code written against {@link Openable} opens what it closes, and a popup opened with {@code open()} opens where it
 * last opened, or at the pointer. Text in the test platform is 6 units per code point and 9 per line.
 */
class OpeningTest {
    private final TestHost test = new TestHost(300, 200);
    private final ComponentFactory ui = this.test.factory();

    private static void reopen(final Openable<?> control, final List<String> heard) {
        control.close();
        assertFalse(control.isOpen(), control + " closes");
        control.open();
        assertTrue(control.isOpen(), control + " opens again");
        heard.add(control.getClass().getSimpleName());
    }

    @Test
    void codeAgainstTheAttributeOpensEveryKind() {
        final ComboBox<String> combo = this.ui.<String>comboBox().setItems(List.of("Ana", "Ben"));
        final DropDownButton drop = this.ui.dropDownButton("Sort", this.ui.menu().addItem("Name", () -> { }));
        final DatePicker date = this.ui.datePicker();
        final InfoBar bar = this.ui.infoBar();
        final AbsolutePanel panel = this.ui.absolutePanel().add(combo, 10, 10).add(drop, 10, 40).add(date, 10, 70)
                .add(bar, 10, 100);
        this.test.show(panel);
        final Menu menu = this.ui.menu().addItem("Copy", () -> { });
        final Flyout flyout = this.ui.flyout(this.ui.label("Cleo"));
        final List<String> heard = new ArrayList<>();
        final List<Openable<?>> controls = List.of(combo, drop, date, bar, menu, flyout);

        for (final Openable<?> control : controls) {
            control.onEvent(Openable.OPEN, event -> heard.add("asked"));
            control.open();
            this.test.host().layout();
            assertTrue(control.isOpen(), control + " opens");
            reopen(control, heard);
            control.close();
        }

        assertEquals(List.of("ComboBoxImpl", "DropDownButtonImpl", "DatePickerImpl", "InfoBarImpl", "MenuImpl",
                "FlyoutImpl"), heard, "opening from code never asks");
    }

    @Test
    void aMenuOpensAgainAtTheAnchorItLastOpenedAt() {
        this.test.show(this.ui.absolutePanel());
        final Menu menu = this.ui.menu().addItem("Copy", () -> { });
        menu.open(new Rect(40, 30, 50, 20));
        final Rect first = menu.bounds();
        menu.close();

        menu.open();

        assertEquals(first, menu.bounds());
        assertEquals(40f, first.x());
        assertEquals(50f, first.y(), "below the anchor");
    }

    @Test
    void aPopupThatNeverOpenedOpensAtThePointer() {
        this.test.show(this.ui.absolutePanel());
        this.test.host().pointerMoved(120, 80, Modifiers.NONE);
        final Menu menu = this.ui.menu().addItem("Copy", () -> { });
        final Flyout flyout = this.ui.flyout(this.ui.label("Cleo"));

        menu.open();
        assertEquals(120f, menu.bounds().x());
        assertEquals(80f, menu.bounds().y());
        menu.close();

        flyout.open();
        assertEquals(120f, flyout.bounds().x());
        assertEquals(80f, flyout.bounds().y());
    }

    @Test
    void aFlyoutOpenedInAnAreaOpensThereAgain() {
        this.test.show(this.ui.absolutePanel());
        final Flyout flyout = this.ui.flyout(this.ui.label("Cleo"));
        flyout.openIn(new Rect(150, 20, 100, 100));
        final Rect first = flyout.bounds();
        flyout.close();
        this.test.host().pointerMoved(5, 5, Modifiers.NONE);

        flyout.open();

        assertEquals(first, flyout.bounds());
        assertEquals(150f, first.x());
    }
}
