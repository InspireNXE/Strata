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
import org.inspirenxe.strata.api.control.field.PasswordTextBox;
import org.inspirenxe.strata.api.control.field.Spinner;
import org.inspirenxe.strata.api.control.field.TextArea;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Text alignment in the text fields (design 4.44). The test platform draws every code point 6 units wide, and the
 * fields here are 200 wide with 4 units of padding, so 192 of room.
 */
class TextAlignmentTest {
    private final TestHost test = new TestHost(300, 200);
    private final ComponentFactory ui = this.test.factory();

    private List<String> drawnText(final Component<?> content) {
        this.test.show(content);
        final DrawLog log = new DrawLog();
        this.test.host().render(log);
        return log.lines("text");
    }

    private static String x(final String line) {
        return line.split(" ")[2].split(",")[0];
    }

    @Test
    void aShortValueSitsAtTheRightOrTheCenter() {
        final TextBox box = this.ui.textBox().setAnchor(Anchor.TOP_LEFT).setWidth(200).setText("12");
        assertEquals(HorizontalAlignment.LEFT, box.textAlignment());
        assertEquals("4", x(this.drawnText(box).getFirst()));

        box.setTextAlignment(HorizontalAlignment.RIGHT);
        assertEquals("183", x(this.drawnText(box).getFirst()));
        box.setTextAlignment(HorizontalAlignment.CENTER);
        assertEquals("94", x(this.drawnText(box).getFirst()));
        box.setTextAlignment(HorizontalAlignment.STRETCH);
        assertEquals("4", x(this.drawnText(box).getFirst()));
        assertThrows(NullPointerException.class, () -> box.setTextAlignment(null));
    }

    @Test
    void textWiderThanTheBoxIsNotShifted() {
        final String wide = "x".repeat(40);
        final TextBox left = this.ui.textBox().setAnchor(Anchor.TOP_LEFT).setWidth(200).setText(wide);
        final String leftX = x(this.drawnText(left).getFirst());
        final TextBox right = this.ui.textBox().setAnchor(Anchor.TOP_LEFT).setWidth(200).setText(wide)
                .setTextAlignment(HorizontalAlignment.RIGHT);
        assertEquals(leftX, x(this.drawnText(right).getFirst()));
    }

    @Test
    void aPressFindsTheShiftedText() {
        final TextBox box = this.ui.textBox().setAnchor(Anchor.TOP_LEFT).setWidth(200).setText("1234")
                .setTextAlignment(HorizontalAlignment.RIGHT);
        this.drawnText(box);
        // The text starts at 4 + (192 - 25) = 171; a press just right of its second digit lands after it
        this.test.host().pointerButton(171 + 12 + 1, 10, MouseButton.LEFT, true, Modifiers.NONE);
        this.test.host().pointerButton(171 + 12 + 1, 10, MouseButton.LEFT, false, Modifiers.NONE);
        assertEquals(2, box.caret());
    }

    @Test
    void thePlaceholderAlignsByItsOwnWidth() {
        final TextBox box = this.ui.textBox().setAnchor(Anchor.TOP_LEFT).setWidth(200).setPlaceholder("none")
                .setTextAlignment(HorizontalAlignment.RIGHT);
        assertEquals("172", x(this.drawnText(box).getFirst()));
    }

    @Test
    void eachRowOfATextAreaAligns() {
        final TextArea area = this.ui.textArea().setAnchor(Anchor.TOP_LEFT).setWidth(200).setText("abcd\nab")
                .setTextAlignment(HorizontalAlignment.RIGHT);
        final List<String> lines = this.drawnText(area);
        assertEquals("171", x(lines.get(0)));
        assertEquals("183", x(lines.get(1)));

        // Up keeps the caret's place on screen, so it lands under the same column of the right-aligned text
        area.requestFocus();
        this.test.host().key(org.inspirenxe.strata.api.input.Key.UP, org.inspirenxe.strata.api.input.KeyAction.PRESS,
                Modifiers.NONE);
        assertEquals(4, area.caret());
    }

    @Test
    void passwordBoxesAndSpinnersAlignToo() {
        final PasswordTextBox box = this.ui.passwordTextBox().setAnchor(Anchor.TOP_LEFT).setWidth(200);
        box.setPassword("abc".toCharArray());
        final int left = Integer.parseInt(x(this.drawnText(box).getFirst()));
        // Showing the box again would wipe it with the screen it left, so draw the same screen again
        box.setTextAlignment(HorizontalAlignment.RIGHT);
        final DrawLog log = new DrawLog();
        this.test.host().render(log);
        final int right = Integer.parseInt(x(log.lines("text").getFirst()));
        assertEquals(HorizontalAlignment.RIGHT, box.textAlignment());
        assertTrue(right > left + 100, left + " then " + right);

        final Spinner spinner = this.ui.spinner().setTextAlignment(HorizontalAlignment.RIGHT);
        assertEquals(HorizontalAlignment.RIGHT, spinner.textAlignment());
    }
}
