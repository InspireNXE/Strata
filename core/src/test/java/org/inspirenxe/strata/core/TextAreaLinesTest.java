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
import org.inspirenxe.strata.api.control.field.TextArea;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifier;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The text area's scrolling from code, its lines and columns, and Tab (design 4.44). The test platform's lines are
 * 9 units tall.
 */
class TextAreaLinesTest {
    private final TestHost test = new TestHost(300, 200);
    private final ComponentFactory ui = this.test.factory();

    private boolean press(final Key key, final Modifiers modifiers) {
        final boolean handled = this.test.host().key(key, KeyAction.PRESS, modifiers);
        this.test.host().key(key, KeyAction.RELEASE, modifiers);
        return handled;
    }

    private TextArea tall(final int lines) {
        final TextArea area = this.ui.textArea().setAnchor(Anchor.TOP_LEFT).setSize(200, 44);
        this.test.show(area);
        final StringBuilder text = new StringBuilder();
        for (int i = 0; i < lines; i++) {
            text.append(i == 0 ? "" : "\n").append("line ").append(i);
        }
        area.setText(text.toString());
        return area;
    }

    @Test
    void linesAndColumnsCountLineBreaksOnly() {
        final TextArea area = this.ui.textArea().setWrap(true).setWidth(30);
        area.setText("first line that wraps\n\n😀x");
        assertEquals(3, area.lineCount());
        assertEquals(0, area.lineOf(5));
        assertEquals(1, area.lineOf(22));
        assertEquals(2, area.lineOf(23));
        assertEquals(2, area.lineOf(999));
        assertEquals(0, area.lineOf(-3));
        assertEquals(0, area.lineStart(0));
        assertEquals(22, area.lineStart(1));
        assertEquals(23, area.lineStart(2));
        assertEquals(1, area.columnOf(25));
        assertEquals(0, area.columnOf(24));
        assertEquals(5, area.columnOf(5));
        assertThrows(IndexOutOfBoundsException.class, () -> area.lineStart(3));
        assertThrows(IndexOutOfBoundsException.class, () -> area.lineStart(-1));

        area.setText("");
        assertEquals(1, area.lineCount());
        area.setText("end\n");
        assertEquals(2, area.lineCount());
        assertEquals(4, area.lineStart(1));
    }

    @Test
    void setScrollYClampsAndStaysUntilTheCaretMoves() {
        final TextArea area = this.tall(20);
        this.test.host().render(new NullCanvas());
        assertSame(area, area.setScrollY(45f));
        assertEquals(45f, area.scrollY());
        this.test.host().render(new NullCanvas());
        assertEquals(45f, area.scrollY());

        area.setScrollY(-4f);
        assertEquals(0f, area.scrollY());
        area.setScrollY(10_000f);
        final float max = area.scrollY();
        assertTrue(max > 0f && max < 10_000f);
        assertThrows(IllegalArgumentException.class, () -> area.setScrollY(Float.NaN));
    }

    @Test
    void scrollToCaretShowsTheCaretsRow() {
        final TextArea area = this.tall(20);
        this.test.host().render(new NullCanvas());
        area.setScrollY(0f);
        area.select(area.lineStart(15), area.lineStart(15));
        area.setScrollY(0f);
        area.scrollToCaret();
        final float shown = area.scrollY();
        assertTrue(shown > 0f, "scrolled to " + shown);
        assertTrue(15 * 9f >= shown && 16 * 9f <= shown + 44f);
    }

    @Test
    void scrollToCaretBeforeLayoutWaitsForThePaint() {
        final TextArea area = this.ui.textArea().setAnchor(Anchor.TOP_LEFT).setSize(200, 44);
        area.setText("a\nb\nc\nd\ne\nf\ng\nh");
        area.scrollToCaret();
        assertEquals(0f, area.scrollY());
        this.test.show(area);
        this.test.host().render(new NullCanvas());
        assertTrue(area.scrollY() > 0f);
    }

    @Test
    void tabTypesATabWhenAccepted() {
        final TextBox after = this.ui.textBox();
        final TextArea area = this.ui.textArea().setAcceptsTab(true);
        this.test.show(this.ui.stackPanel().add(area).add(after));
        assertTrue(area.acceptsTab());
        area.requestFocus();
        area.setText("ab");
        assertTrue(this.press(Key.TAB, Modifiers.NONE));
        assertEquals("ab\t", area.text());
        assertTrue(this.press(Key.TAB, Modifiers.of(Modifier.SHIFT)));
        assertEquals("ab\t", area.text());
        assertTrue(area.isFocused());

        assertTrue(this.press(Key.TAB, Modifiers.of(Modifier.CONTROL)));
        assertTrue(after.isFocused());
    }

    @Test
    void tabMovesFocusByDefaultAndWhenReadOnly() {
        final TextBox after = this.ui.textBox();
        final TextArea area = this.ui.textArea();
        final StackPanel panel = this.ui.stackPanel().add(area).add(after);
        this.test.show(panel);
        assertFalse(area.acceptsTab());
        area.requestFocus();
        this.press(Key.TAB, Modifiers.NONE);
        assertTrue(after.isFocused());

        area.setAcceptsTab(true).setReadOnly(true);
        area.requestFocus();
        this.press(Key.TAB, Modifiers.NONE);
        assertTrue(after.isFocused());
        assertEquals("", area.text());
    }

    @Test
    void aTypedTabIsUndoneWithTheTypingAroundIt() {
        final TextArea area = this.ui.textArea().setAcceptsTab(true);
        this.test.show(area);
        area.requestFocus();
        this.test.host().charTyped('x', Modifiers.NONE);
        this.press(Key.TAB, Modifiers.NONE);
        this.test.host().charTyped('y', Modifiers.NONE);
        this.press(Key.Z, Modifiers.of(Modifier.CONTROL));
        assertEquals("x\t", area.text());
    }
}
