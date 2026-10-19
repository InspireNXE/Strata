package org.inspirenxe.strata.core;

import org.inspirenxe.strata.api.control.attribute.text.TextInput;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifier;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.core.control.field.TextBoxImpl;
import org.inspirenxe.strata.spi.ScreenHost;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * With the test platform every code point is 6 units wide, and the box's text starts 4 units in (its padding), so
 * the boundary before index {@code i} sits at x = 4 + 6i.
 */
class TextBoxTest {
    private static final Modifiers SHIFT = Modifiers.of(Modifier.SHIFT);
    private static final Modifiers CONTROL = Modifiers.of(Modifier.CONTROL);

    private final TestHost test = new TestHost(300, 100);
    private final ScreenHost host = this.test.host();
    private final TextBox box = this.test.factory().textBox().setWidth(200);

    TextBoxTest() {
        this.test.show(this.test.factory().absolutePanel().add(this.box));
    }

    private static float x(final int index) {
        return 4f + 6f * index;
    }

    private void type(final String text) {
        text.codePoints().forEach(codePoint -> this.host.charTyped(codePoint, Modifiers.NONE));
    }

    private boolean press(final Key key, final Modifiers modifiers) {
        final boolean handled = this.host.key(key, KeyAction.PRESS, modifiers);
        this.host.key(key, KeyAction.RELEASE, modifiers);
        return handled;
    }

    private void clickAt(final int index, final Modifiers modifiers) {
        this.host.pointerButton(x(index), 10, MouseButton.LEFT, true, modifiers);
        this.host.pointerButton(x(index), 10, MouseButton.LEFT, false, modifiers);
    }

    private void focusWith(final String text) {
        this.box.setText(text);
        this.box.requestFocus();
    }

    @Test
    void typingInsertsAtCaretAndNotifies() {
        final List<String> seen = new ArrayList<>();
        this.box.onEvent(TextInput.EDITED, event -> seen.add(event.value()));
        this.box.requestFocus();

        this.type("hi");
        this.press(Key.LEFT, Modifiers.NONE);
        this.type("o");

        assertEquals("hoi", this.box.text());
        assertEquals(2, this.box.caret());
        assertEquals(List.of("h", "hi", "hoi"), seen);
    }

    @Test
    void backspaceAndDeleteRemoveWholeCodePoints() {
        this.box.requestFocus();
        this.type("a😀b");

        this.press(Key.BACKSPACE, Modifiers.NONE);
        assertEquals("a😀", this.box.text());
        this.press(Key.BACKSPACE, Modifiers.NONE);
        assertEquals("a", this.box.text());

        this.box.setText("x😀y");
        this.box.select(1, 1);
        this.press(Key.DELETE, Modifiers.NONE);
        assertEquals("xy", this.box.text());
    }

    @Test
    void shiftArrowsSelectAndTypingReplacesSelection() {
        this.focusWith("hello");

        this.press(Key.LEFT, SHIFT);
        this.press(Key.LEFT, SHIFT);
        assertEquals("lo", this.box.selectedText());

        this.type("p!");
        assertEquals("help!", this.box.text());
    }

    @Test
    void plainArrowCollapsesSelectionToItsEdge() {
        this.focusWith("hello");
        this.box.select(1, 4);

        this.press(Key.LEFT, Modifiers.NONE);

        assertEquals(1, this.box.caret());
        assertEquals("", this.box.selectedText());
    }

    @Test
    void clickPlacesCaretAtNearestBoundary() {
        this.box.setText("hello world");

        this.host.pointerButton(x(5) + 2f, 10, MouseButton.LEFT, true, Modifiers.NONE);
        this.host.pointerButton(x(5) + 2f, 10, MouseButton.LEFT, false, Modifiers.NONE);

        assertEquals(5, this.box.caret());
        assertTrue(this.box.isFocused());
    }

    @Test
    void shiftClickExtendsSelection() {
        this.box.setText("hello world");
        this.clickAt(2, Modifiers.NONE);
        this.test.platform().advanceMillis(1000);
        this.clickAt(8, SHIFT);

        assertEquals("llo wo", this.box.selectedText());
    }

    @Test
    void doubleClickSelectsTheWordUnderThePointer() {
        this.box.setText("hello world");

        // At the first letter of "world": Wintergreen selected "hello world" here
        this.clickAt(6, Modifiers.NONE);
        this.clickAt(6, Modifiers.NONE);

        assertEquals("world", this.box.selectedText());
    }

    @Test
    void doubleClickDragExtendsByWords() {
        this.box.setText("one two three");
        this.clickAt(5, Modifiers.NONE);
        this.host.pointerButton(x(5), 10, MouseButton.LEFT, true, Modifiers.NONE);
        this.host.pointerMoved(x(10), 10, Modifiers.NONE);
        this.host.pointerButton(x(10), 10, MouseButton.LEFT, false, Modifiers.NONE);

        assertEquals("two three", this.box.selectedText());
    }

    @Test
    void tripleClickSelectsEverything() {
        this.box.setText("hello world");

        this.clickAt(2, Modifiers.NONE);
        this.clickAt(2, Modifiers.NONE);
        this.clickAt(2, Modifiers.NONE);

        assertEquals("hello world", this.box.selectedText());
    }

    @Test
    void dragSelectsByCharacter() {
        this.box.setText("hello world");

        this.host.pointerButton(x(1), 10, MouseButton.LEFT, true, Modifiers.NONE);
        this.host.pointerMoved(x(4), 10, Modifiers.NONE);
        this.host.pointerButton(x(4), 10, MouseButton.LEFT, false, Modifiers.NONE);

        assertEquals("ell", this.box.selectedText());
        assertEquals(4, this.box.caret());
    }

    @Test
    void copyCutAndPasteUseTheHostClipboard() {
        this.focusWith("hello world");
        this.box.select(0, 5);

        this.press(Key.C, CONTROL);
        assertEquals("hello", this.test.platform().clipboard().get().orElseThrow());

        this.press(Key.X, CONTROL);
        assertEquals(" world", this.box.text());

        this.test.platform().clipboard().set("big\r\nnew");
        this.press(Key.V, CONTROL);
        assertEquals("bignew world", this.box.text());
    }

    @Test
    void passwordModeMasksAndBlocksCopyAndCut() {
        this.box.setPassword(true);
        this.focusWith("secret");
        this.test.platform().clipboard().set("untouched");

        this.press(Key.A, CONTROL);
        this.press(Key.C, CONTROL);
        this.press(Key.X, CONTROL);

        assertEquals("untouched", this.test.platform().clipboard().get().orElseThrow());
        assertEquals("secret", this.box.text());
    }

    @Test
    void maxLengthLimitsTypingPastingAndSetText() {
        this.box.setMaxLength(5);
        this.focusWith("abc");

        this.type("defg");
        assertEquals("abcde", this.box.text());

        this.box.setText("1234567");
        assertEquals("12345", this.box.text());

        this.box.select(1, 3);
        this.test.platform().clipboard().set("xyz");
        this.press(Key.V, CONTROL);
        assertEquals("1xy45", this.box.text(), "the replaced selection frees room, and the paste is cut to fit");
    }

    @Test
    void setTextDropsNewlines() {
        this.box.setText("one\ntwo\r\n");
        assertEquals("onetwo", this.box.text());
    }

    @Test
    void controlArrowsAndBackspaceWorkByWord() {
        this.focusWith("one two  three");

        this.press(Key.LEFT, CONTROL);
        assertEquals(9, this.box.caret());
        this.press(Key.LEFT, CONTROL);
        assertEquals(4, this.box.caret());
        this.press(Key.RIGHT, CONTROL);
        assertEquals(7, this.box.caret());

        this.press(Key.END, Modifiers.NONE);
        this.press(Key.BACKSPACE, CONTROL);
        assertEquals("one two  ", this.box.text());
    }

    @Test
    void keyboardFocusStartsTextInputWithCaretAtEnd() {
        this.box.setText("abc");
        assertNull(this.test.platform().textInputArea());

        this.press(Key.TAB, Modifiers.NONE);

        assertSame(this.box, this.test.focused());
        assertEquals(3, this.box.caret());
        assertNotNull(this.test.platform().textInputArea(), "focusing a text box starts engine text input");
    }

    @Test
    void tabLeavesTheBoxAndStopsTextInput() {
        final TestHost two = new TestHost(300, 100);
        final TextBox field = two.factory().textBox();
        final Button after = two.factory().button("After");
        two.show(two.factory().absolutePanel().add(field).add(after, 0, 40));
        field.requestFocus();

        assertTrue(two.host().key(Key.TAB, KeyAction.PRESS, Modifiers.NONE));

        assertSame(after, two.focused());
        assertNull(two.platform().textInputArea());
    }

    @Test
    void printableKeysAreConsumedWhileFocused() {
        this.box.requestFocus();

        assertTrue(this.press(Key.E, Modifiers.NONE), "a focused box keeps typing keys from reaching engine shortcuts");
        assertFalse(this.press(Key.ESCAPE, Modifiers.NONE), "Escape still reaches the engine");
    }

    @Test
    void caretBlinksOnTheHostClock() {
        this.focusWith("ab");

        final NullCanvas shown = new NullCanvas();
        this.host.render(shown);
        assertTrue(hasCaret(shown), "the caret shows right after an edit");

        this.test.platform().advanceMillis(TextBoxImpl.BLINK_MILLIS);
        final NullCanvas hidden = new NullCanvas();
        this.host.render(hidden);
        assertFalse(hasCaret(hidden), "and hides for the next half second");
    }

    private static boolean hasCaret(final NullCanvas canvas) {
        return canvas.fills().stream().anyMatch(rect -> rect.width() == 1f && rect.height() == TestPlatform.LINE_HEIGHT);
    }

    @Test
    void longTextScrollsToKeepCaretVisible() {
        final TextBox narrow = this.test.factory().textBox().setWidth(40);
        this.test.show(this.test.factory().absolutePanel().add(narrow));
        narrow.requestFocus();
        narrow.setText("abcdefghijklmnop");

        final NullCanvas canvas = new NullCanvas();
        this.host.render(canvas);

        // 16 chars are 96 wide in a 32-wide content box; the caret sits at the right edge of the content
        final Rect caret = canvas.fills().stream().filter(rect -> rect.width() == 1f).findFirst().orElseThrow();
        assertEquals(4f + 32f - 1f, caret.x());
    }
}
