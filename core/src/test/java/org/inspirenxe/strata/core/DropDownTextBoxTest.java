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
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifier;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A text box made a drop-down: an editable combo box (design 4.56 and 4.57).
 */
class DropDownTextBoxTest {
    private final TestHost test = new TestHost(300, 300);
    private final ComponentFactory ui = this.test.factory();
    private final TextBox box = this.ui.textBox().setText("Be")
            .setSuggestionProvider(TextBox.SuggestionProvider.words(List.of("Ana", "Ben", "Cleo")))
            .setDropDown(true);

    DropDownTextBoxTest() {
        this.box.setWidth(120);
        this.test.show(this.ui.absolutePanel().add(this.box, 10, 10));
        this.test.host().layout();
    }

    private void pressArrow() {
        final float x = this.box.bounds().right() - 6f;
        final float y = this.box.bounds().y() + this.box.bounds().height() / 2f;
        this.test.host().pointerButton(x, y, MouseButton.LEFT, true, Modifiers.NONE);
        this.test.host().pointerButton(x, y, MouseButton.LEFT, false, Modifiers.NONE);
    }

    private int listedRows() {
        final DrawLog log = new DrawLog();
        this.test.host().render(log);
        return (int) log.lines("text").stream().filter(line -> line.contains("Ana") || line.contains("Ben")
                || line.contains("Cleo")).count();
    }

    @Test
    void theArrowShowsEverySuggestionAndAcceptingReplacesTheWholeText() {
        assertTrue(this.box.isDropDown());
        this.pressArrow();
        assertEquals(3, this.listedRows());
        this.box.requestFocus();
        this.test.host().key(Key.DOWN, KeyAction.PRESS, Modifiers.NONE);
        this.test.host().key(Key.DOWN, KeyAction.PRESS, Modifiers.NONE);
        this.test.host().key(Key.ENTER, KeyAction.PRESS, Modifiers.NONE);
        assertEquals("Cleo", this.box.text());
        this.pressArrow();
        this.pressArrow();
        // Only the box's own text is left
        assertEquals(1, this.listedRows());
    }

    @Test
    void altDownOpensTheListAndTheArrowTakesRoomFromTheText() {
        final TextBox plain = this.ui.textBox();
        assertFalse(plain.isDropDown());
        this.box.requestFocus();
        this.test.host().key(Key.DOWN, KeyAction.PRESS, Modifiers.of(Modifier.ALT));
        assertEquals(3, this.listedRows());
        final DrawLog log = new DrawLog();
        this.test.host().render(log);
        // The arrow's widest line, 7 units at the right inside the padding of 4
        final float right = this.box.bounds().right();
        assertTrue(log.lines("fill").stream().anyMatch(line -> line.contains(" 7x1 ")
                && line.contains(((int) (right - 4f - 7f)) + ",")), log.lines().toString());
    }
}
