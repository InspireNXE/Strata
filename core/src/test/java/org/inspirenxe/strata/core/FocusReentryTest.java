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
import org.inspirenxe.strata.api.control.attribute.item.Selection;
import org.inspirenxe.strata.api.control.attribute.value.Toggle;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.button.RadioButton;
import org.inspirenxe.strata.api.control.button.RadioGroup;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifier;
import org.inspirenxe.strata.api.input.Modifiers;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Focus and radio handlers that move focus or the selection again while they are being told about a change.
 */
class FocusReentryTest {
    private final TestHost test = new TestHost(300, 300);
    private final ComponentFactory ui = this.test.factory();

    @Test
    void aFieldThatTakesFocusBackWhenItLosesItEndsWithOnlyItselfFocused() {
        final TextBox first = this.ui.textBox();
        final TextBox second = this.ui.textBox();
        this.test.show(this.ui.stackPanel().add(first).add(second));
        first.requestFocus();
        final boolean[] refused = {false};
        first.onEvent(Focusable.FOCUS_LOST, event -> {
            if (!refused[0]) {
                refused[0] = true;
                first.requestFocus();
            }
        });
        final List<String> secondHeard = new ArrayList<>();
        second.onEvent(Focusable.FOCUS_GAINED, event -> secondHeard.add("gained"));
        second.onEvent(Focusable.FOCUS_LOST, event -> secondHeard.add("lost"));

        second.requestFocus();

        assertSame(first, this.test.focused());
        assertTrue(first.isFocused());
        assertFalse(second.isFocused());
        assertEquals(List.of(), secondHeard);
    }

    @Test
    void aFocusLostHandlerThatThrowsStillLetsTheNewFieldGainFocus() {
        final TextBox first = this.ui.textBox();
        final TextBox second = this.ui.textBox();
        this.test.show(this.ui.stackPanel().add(first).add(second));
        first.requestFocus();
        first.onEvent(Focusable.FOCUS_LOST, event -> {
            throw new IllegalStateException("handler failed on purpose");
        });

        assertThrows(IllegalStateException.class, second::requestFocus);

        assertSame(second, this.test.focused());
        assertTrue(second.isFocused());
        assertFalse(first.isFocused());
    }

    @Test
    void tabFromAFocusedNodeOutsideTheTabOrderGoesOnFromIt() {
        final Button first = this.ui.button("First");
        final Button middle = this.ui.button("Middle");
        final Button last = this.ui.button("Last");
        this.test.show(this.ui.stackPanel().add(first).add(middle).add(last));
        middle.setTabbable(false);
        middle.requestFocus();

        this.test.host().key(Key.TAB, KeyAction.PRESS, Modifiers.NONE);
        assertSame(last, this.test.focused());

        middle.requestFocus();
        this.test.host().key(Key.TAB, KeyAction.PRESS, Modifiers.NONE.with(Modifier.SHIFT));
        assertSame(first, this.test.focused());
    }

    @Test
    void aRadioButtonThatMovesTheSelectionWhenItLosesItReportsTheFinalSelectionLast() {
        final RadioGroup<String> group = this.ui.radioGroup();
        final RadioButton first = group.add("First", "first");
        final RadioButton second = group.add("Second", "second");
        final RadioButton third = group.add("Third", "third");
        this.test.show(this.ui.stackPanel().add(first).add(second).add(third));
        group.select(0);
        final boolean[] moved = {false};
        first.onEvent(Toggle.TOGGLED, event -> {
            if (!event.value() && !moved[0]) {
                moved[0] = true;
                group.select(2);
            }
        });
        final List<Integer> selected = new ArrayList<>();
        group.onEvent(Selection.SELECTED, event -> selected.add(event.value()));
        final List<Boolean> secondHeard = new ArrayList<>();
        second.onEvent(Toggle.TOGGLED, event -> secondHeard.add(event.value()));

        group.select(1);

        assertEquals(2, group.selectedIndex());
        assertEquals(2, selected.getLast());
        assertFalse(second.isSelected());
        assertEquals(List.of(), secondHeard, "the second button never had the selection");
    }
}
