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
import org.inspirenxe.strata.api.control.attribute.text.TextInput;
import org.inspirenxe.strata.api.control.field.PasswordTextBox;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.control.menu.Menu;
import org.inspirenxe.strata.api.control.menu.MenuEntry;
import org.inspirenxe.strata.api.control.menu.MenuItem;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.layout.Anchor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The text fields' default context menu (design 4.40).
 */
class TextMenusTest {
    private final TestHost test = new TestHost(300, 300);
    private final ComponentFactory ui = this.test.factory();

    private void press(final Key key) {
        this.test.host().key(key, KeyAction.PRESS, Modifiers.NONE);
        this.test.host().key(key, KeyAction.RELEASE, Modifiers.NONE);
    }

    private void rightClick(final Rect bounds) {
        final float x = bounds.x() + 2;
        final float y = bounds.y() + bounds.height() / 2f;
        this.test.host().pointerButton(x, y, MouseButton.RIGHT, true, Modifiers.NONE);
        this.test.host().pointerButton(x, y, MouseButton.RIGHT, false, Modifiers.NONE);
        this.test.host().layout();
    }

    private static List<String> captions(final Menu menu) {
        final List<String> captions = new ArrayList<>();
        for (final MenuEntry entry : menu.items()) {
            captions.add(entry instanceof MenuItem item ? item.text().plain() : "-");
        }
        return captions;
    }

    private static boolean enabled(final Menu menu, final String caption) {
        for (final MenuEntry entry : menu.items()) {
            if (entry instanceof MenuItem item && item.text().plain().equals(caption)) {
                return item.isEnabled();
            }
        }
        throw new AssertionError(caption);
    }

    @Test
    void aTextBoxCutsFromItsMenuAndUndoesIt() {
        final List<String> edits = new ArrayList<>();
        final TextBox box = this.ui.textBox().setText("hello");
        box.onEvent(TextInput.EDIT, event -> edits.add(event.value()));
        this.test.show(box.setAnchor(Anchor.TOP_LEFT));
        box.requestFocus();
        box.selectAll();
        final Menu menu = (Menu) box.contextMenu().orElseThrow();
        assertEquals(List.of("Undo", "Redo", "-", "Cut", "Copy", "Paste", "Delete", "-", "Select all"),
                captions(menu));
        this.rightClick(box.bounds());
        assertTrue(menu.isOpen());
        assertFalse(enabled(menu, "Undo"));
        assertTrue(enabled(menu, "Cut"));
        assertFalse(enabled(menu, "Paste"));
        // C moves to the first item starting with it, Cut
        this.press(Key.C);
        this.press(Key.ENTER);
        assertEquals("", box.text());
        assertEquals("hello", this.test.platform().clipboard().get().orElseThrow());
        assertEquals(List.of(""), edits);
        this.rightClick(box.bounds());
        assertTrue(enabled(menu, "Undo"));
        assertTrue(enabled(menu, "Paste"));
        this.press(Key.U);
        this.press(Key.ENTER);
        assertEquals("hello", box.text());
    }

    @Test
    void aPasswordBoxLeavesOutHistoryAndCopying() {
        final PasswordTextBox box = this.ui.passwordTextBox();
        this.test.show(box.setAnchor(Anchor.TOP_LEFT));
        final Menu menu = (Menu) box.contextMenu().orElseThrow();
        assertEquals(List.of("Paste", "Delete", "-", "Select all"), captions(menu));
    }

    @Test
    void theDefaultMenuCanBeRemoved() {
        final TextBox box = this.ui.textBox().setContextMenu(null);
        assertTrue(box.contextMenu().isEmpty());
    }
}
