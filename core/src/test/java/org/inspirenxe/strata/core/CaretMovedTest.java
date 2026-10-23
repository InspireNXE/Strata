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
import org.inspirenxe.strata.api.control.attribute.text.TextEntry;
import org.inspirenxe.strata.api.control.attribute.text.TextInput;
import org.inspirenxe.strata.api.control.field.PasswordTextBox;
import org.inspirenxe.strata.api.control.field.TextArea;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifier;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code TextEntry.CARET_MOVED} on the three text fields (design 4.44). With the test platform every code point is 6
 * units wide.
 */
class CaretMovedTest {
    private static final Modifiers SHIFT = Modifiers.of(Modifier.SHIFT);

    private final TestHost test = new TestHost(300, 200);
    private final ComponentFactory ui = this.test.factory();

    private void press(final Key key, final Modifiers modifiers) {
        this.test.host().key(key, KeyAction.PRESS, modifiers);
        this.test.host().key(key, KeyAction.RELEASE, modifiers);
    }

    private void type(final String text) {
        text.codePoints().forEach(codePoint -> this.test.host().charTyped(codePoint, Modifiers.NONE));
    }

    /**
     * Records every report on a field, in order, carets as "anchor:caret".
     */
    private static List<String> log(final TextEntry<?> field) {
        final List<String> seen = new ArrayList<>();
        field.onEvent(TextEntry.CARET_MOVED, event -> seen.add("caret " + event.previous().anchor() + ":"
                + event.previous().caret() + " > " + event.value().anchor() + ":" + event.value().caret()));
        field.onEvent(TextEntry.MODIFIED, event -> seen.add("modified " + event.value()));
        if (field instanceof final TextInput<?> input) {
            input.onEvent(TextInput.EDITED, event -> seen.add("edited " + event.value()));
        }
        return seen;
    }

    private TextBox showBox(final String text) {
        final TextBox box = this.ui.textBox().setAnchor(Anchor.TOP_LEFT).setWidth(200);
        this.test.show(box);
        box.setText(text);
        box.requestFocus();
        return box;
    }

    @Test
    void typingReportsOnceAfterTheEdit() {
        final TextBox box = this.showBox("ab");
        final List<String> seen = log(box);
        this.type("c");
        assertEquals(List.of("edited abc", "modified 3", "caret 2:2 > 3:3"), seen);
    }

    @Test
    void keysAndShiftSelectionReportTheAnchorAndCaret() {
        final TextBox box = this.showBox("hello");
        final List<String> seen = log(box);
        this.press(Key.LEFT, SHIFT);
        this.press(Key.LEFT, SHIFT);
        this.press(Key.HOME, Modifiers.NONE);
        assertEquals(List.of("caret 5:5 > 5:4", "caret 5:4 > 5:3", "caret 5:3 > 0:0"), seen);
    }

    @Test
    void codeReportsToo() {
        final TextBox box = this.showBox("hello");
        final List<String> seen = log(box);
        box.select(1, 3);
        box.selectAll();
        box.selectAll();
        box.setText("hi");
        assertEquals(List.of("caret 5:5 > 1:3", "caret 1:3 > 0:5", "edited hi", "modified 2", "caret 0:5 > 2:2"),
                seen);
    }

    @Test
    void aKeyThatDoesNotMoveReportsNothing() {
        final TextBox box = this.showBox("hello");
        final List<String> seen = log(box);
        this.press(Key.END, Modifiers.NONE);
        this.press(Key.RIGHT, Modifiers.NONE);
        assertTrue(seen.isEmpty());
    }

    @Test
    void aPressThatGivesFocusReportsOnce() {
        final TextBox other = this.ui.textBox();
        final TextBox box = this.ui.textBox().setWidth(200);
        final StackPanel panel = this.ui.stackPanel().add(other).add(box);
        this.test.show(panel);
        box.setText("hello");
        other.requestFocus();
        final List<String> seen = log(box);
        final float y = box.bounds().y() + 5f;
        this.test.host().pointerButton(box.bounds().x() + 4f + 6f * 2f + 1f, y, MouseButton.LEFT, true, Modifiers.NONE);
        assertEquals(List.of("caret 5:5 > 2:2"), seen);
    }

    @Test
    void aTextAreaReportsLikeABox() {
        final TextArea area = this.ui.textArea().setAnchor(Anchor.TOP_LEFT).setWidth(200);
        this.test.show(area);
        area.setText("one\ntwo");
        area.requestFocus();
        final List<String> seen = log(area);
        this.press(Key.UP, Modifiers.NONE);
        this.press(Key.ENTER, Modifiers.NONE);
        assertEquals(List.of("caret 7:7 > 3:3", "edited one\n\ntwo", "modified 8", "caret 3:3 > 4:4"), seen);
    }

    @Test
    void aPasswordBoxReportsPositionsOnly() {
        final PasswordTextBox box = this.ui.passwordTextBox().setAnchor(Anchor.TOP_LEFT).setWidth(200);
        this.test.show(box);
        box.setPassword("secret".toCharArray());
        box.requestFocus();
        final List<String> seen = log(box);
        this.type("!");
        this.press(Key.LEFT, SHIFT);
        assertEquals(List.of("modified 7", "caret 6:6 > 7:7", "caret 7:7 > 7:6"), seen);
    }

    @Test
    void aWipeReportsNothingAndTheNextMoveStartsFromTheStart() {
        final PasswordTextBox box = this.ui.passwordTextBox();
        this.test.show(box);
        box.setPassword("secret".toCharArray());
        final List<String> seen = log(box);
        this.test.show(this.ui.label("next"));
        assertTrue(seen.isEmpty());
        this.test.show(box);
        box.setPassword("ab".toCharArray());
        assertEquals(List.of("modified 2", "caret 0:0 > 2:2"), seen);
    }

    @Test
    void nothingIsMadeWithoutAHandler() {
        final TextBox box = this.showBox("hello");
        box.select(0, 2);
        assertFalse(box.selectedText().isEmpty());
    }
}
