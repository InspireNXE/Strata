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

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.Container;
import org.inspirenxe.strata.api.control.collection.ListView;
import org.inspirenxe.strata.api.control.field.DateTimePicker;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.core.text.Translations;
import org.inspirenxe.strata.spi.Translator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The date and time controls, in American English, on the test platform's 20 October 2026: what their ranges allow,
 * and how their fields and popups follow the value.
 */
class DateFieldsTest {
    private TestHost test;

    @BeforeEach
    void setUp() {
        Translations.setTranslator(new LocaleTranslator(Locale.US));
        this.test = new TestHost(500, 400);
    }

    @AfterEach
    void resetTranslator() {
        Translations.setTranslator(Translator.NONE);
    }

    private void press(final Key key) {
        this.test.host().key(key, KeyAction.PRESS, Modifiers.NONE);
        this.test.host().key(key, KeyAction.RELEASE, Modifiers.NONE);
    }

    @Nested
    class DateTimePopup {
        private DateTimePicker picker;

        private void show() {
            this.picker = DateFieldsTest.this.test.factory().dateTimePicker();
            DateFieldsTest.this.test.show(DateFieldsTest.this.test.factory().stackPanel(Orientation.VERTICAL)
                    .add(this.picker));
        }

        @SuppressWarnings("unchecked")
        private ListView<Integer> column(final int index) {
            final Component<?> content = DateFieldsTest.this.test.factory().overlays().open().getFirst().content();
            final Container<?> columns = (Container<?>) ((Container<?>) content).children().get(1);
            return (ListView<Integer>) columns.children().get(index);
        }

        private void pressRow(final ListView<Integer> list, final int row) {
            final Rect bounds = list.bounds();
            final float y = bounds.y() + row * 13f + 6f - list.scrollY();
            DateFieldsTest.this.test.host().pointerButton(bounds.x() + 10f, y, MouseButton.LEFT, true, Modifiers.NONE);
            DateFieldsTest.this.test.host().pointerButton(bounds.x() + 10f, y, MouseButton.LEFT, false, Modifiers.NONE);
        }

        @Test
        void aRowSetsItsPartAtOnce() {
            this.show();
            this.picker.setDateTime(LocalDateTime.of(2026, 10, 20, 9, 0));
            this.picker.requestFocus();
            DateFieldsTest.this.press(Key.F4);
            DateFieldsTest.this.test.host().layout();
            DateFieldsTest.this.test.host().render(new NullCanvas());
            DateFieldsTest.this.test.host().layout();
            final ListView<Integer> hours = this.column(0);
            this.pressRow(hours, 10);

            assertEquals(LocalDateTime.of(2026, 10, 20, 10, 0), this.picker.dateTime().orElseThrow());
            assertEquals(10, hours.selectedIndex());
        }

        @Test
        void aRowWhosePickIsCanceledGivesWayToTheValueAgain() {
            this.show();
            this.picker.setDateTime(LocalDateTime.of(2026, 10, 20, 9, 0));
            this.picker.onEvent(DateTimePicker.PICK, event -> event.cancel());
            this.picker.requestFocus();
            DateFieldsTest.this.press(Key.F4);
            DateFieldsTest.this.test.host().layout();
            DateFieldsTest.this.test.host().render(new NullCanvas());
            DateFieldsTest.this.test.host().layout();
            final ListView<Integer> hours = this.column(0);
            this.pressRow(hours, 10);

            assertEquals(LocalDateTime.of(2026, 10, 20, 9, 0), this.picker.dateTime().orElseThrow());
            assertEquals(9, hours.selectedIndex(), "the hours column shows nine again, as the time picker's does");
        }
    }
}
