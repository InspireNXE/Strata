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
import org.inspirenxe.strata.api.control.attribute.popup.Closable;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.collection.ComboBox;
import org.inspirenxe.strata.api.control.field.DatePicker;
import org.inspirenxe.strata.api.control.menu.Flyout;
import org.inspirenxe.strata.api.control.menu.Menu;
import org.inspirenxe.strata.api.event.cause.Origin;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Escape and a press outside ask a popup to close, so a handler can keep it open (design 4.57).
 */
class PopupCloseRequestTest {
    private final TestHost test = new TestHost(300, 300);
    private final ComponentFactory ui = this.test.factory();
    private final List<String> heard = new ArrayList<>();
    private final Button behind = this.ui.button("Behind").onEvent(Button.PRESSED, event -> this.heard.add("pressed"));

    PopupCloseRequestTest() {
        this.test.show(this.ui.absolutePanel().add(this.behind, 10, 10));
        this.test.host().layout();
    }

    private boolean escape() {
        return this.test.host().key(Key.ESCAPE, KeyAction.PRESS, Modifiers.NONE);
    }

    @Test
    void aFlyoutThatRefusesKeepsItsFormOpenAndUsesTheKeyAndThePress() {
        final Flyout flyout = this.ui.flyout(this.ui.textBox().setText("unsaved"))
                .onEvent(Closable.CLOSE, event -> {
                    assertInstanceOf(Origin.User.class, event.origin());
                    this.heard.add("close?");
                    event.cancel();
                });
        flyout.openAt(150, 150);
        assertTrue(this.escape());
        assertTrue(flyout.isOpen());
        this.test.click(20, 20);
        assertTrue(flyout.isOpen());
        assertEquals(List.of("close?", "close?"), this.heard);
        flyout.close();
        assertFalse(flyout.isOpen());
        assertEquals(List.of("close?", "close?"), this.heard);
    }

    @Test
    void aMenuAsksBeforeEscapeAndAPressOutsideClose() {
        final Menu menu = this.ui.menu().addItem("Open", () -> { })
                .onEvent(Closable.CLOSE, event -> this.heard.add("close?"));
        menu.openAt(150, 150);
        this.test.host().layout();
        this.escape();
        assertFalse(menu.isOpen());
        menu.openAt(150, 150);
        this.test.host().layout();
        menu.onEvent(Closable.CLOSE, event -> event.cancel());
        this.test.click(20, 20);
        assertTrue(menu.isOpen());
        assertEquals(List.of("close?", "close?"), this.heard);
    }

    @Test
    void aComboBoxAsksForItsList() {
        final ComboBox<String> box = this.ui.<String>comboBox().setItems(List.of("Ana", "Ben"))
                .onEvent(Closable.CLOSE, event -> {
                    this.heard.add("close?");
                    event.cancel();
                });
        this.test.show(this.ui.absolutePanel().add(box, 10, 10));
        this.test.host().layout();
        box.open();
        box.requestFocus();
        this.escape();
        assertTrue(box.isOpen());
        this.test.click(250, 250);
        assertTrue(box.isOpen());
        assertEquals(List.of("close?", "close?"), this.heard);
    }

    @Test
    void aDatePickerAsksForItsCalendar() {
        final DatePicker picker = this.ui.datePicker(LocalDate.of(2026, 10, 22))
                .onEvent(Closable.CLOSE, event -> this.heard.add("close?"));
        this.test.show(this.ui.absolutePanel().add(picker, 10, 10));
        this.test.host().layout();
        picker.open();
        this.test.click(290, 290);
        assertFalse(picker.isOpen());
        assertEquals(List.of("close?"), this.heard);
    }
}
