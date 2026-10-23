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
import org.inspirenxe.strata.api.command.Command;
import org.inspirenxe.strata.api.control.field.TextArea;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.KeyStroke;
import org.inspirenxe.strata.api.input.Modifier;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.theme.Themes;
import org.inspirenxe.strata.spi.Conventions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The text fields' own shortcuts on a platform whose shortcut modifier is Command (design 4.40).
 */
class TextShortcutsTest {
    private static final Modifiers COMMAND = Modifiers.of(Modifier.SUPER);

    private final TestHost test;
    private final ComponentFactory ui;

    TextShortcutsTest() {
        final TestPlatform platform = new TestPlatform();
        platform.setConventions(new Conventions(false, Modifier.SUPER, true));
        this.test = new TestHost(platform, Themes.flat(), 300, 200);
        this.ui = this.test.factory();
    }

    private boolean press(final Key key, final Modifiers modifiers) {
        final boolean used = this.test.host().key(key, KeyAction.PRESS, modifiers);
        this.test.host().key(key, KeyAction.RELEASE, modifiers);
        return used;
    }

    @Test
    void theClipboardAndHistoryKeysTakeTheShortcutModifier() {
        final TextBox box = this.ui.textBox().setText("abc");
        this.test.show(box.setAnchor(Anchor.TOP_LEFT));
        box.requestFocus();
        assertTrue(this.press(Key.A, COMMAND));
        assertEquals(0, box.selectionStart());
        assertEquals(3, box.selectionEnd());
        this.press(Key.X, COMMAND);
        assertEquals("", box.text());
        assertEquals("abc", this.test.platform().clipboard().get().orElseThrow());
        this.press(Key.Z, COMMAND);
        assertEquals("abc", box.text());
        this.press(Key.Z, Modifiers.of(Modifier.SUPER, Modifier.SHIFT));
        assertEquals("", box.text());
    }

    @Test
    void wordMovesKeepControl() {
        final TextArea area = this.ui.textArea().setText("one two");
        this.test.show(area.setAnchor(Anchor.TOP_LEFT));
        area.requestFocus();
        this.press(Key.HOME, Modifiers.of(Modifier.CONTROL));
        this.press(Key.RIGHT, Modifiers.of(Modifier.CONTROL));
        assertEquals(3, area.caret());
    }

    @Test
    void aCommandKeyAFieldDoesNotUseReachesTheHostsShortcuts() {
        final List<String> heard = new ArrayList<>();
        this.ui.shortcuts().add(Command.of("Save", () -> heard.add("save")).withShortcut(KeyStroke.shortcut(Key.S)));
        final TextBox box = this.ui.textBox();
        this.test.show(box.setAnchor(Anchor.TOP_LEFT));
        box.requestFocus();
        assertTrue(this.press(Key.S, COMMAND));
        assertEquals(List.of("save"), heard);
        assertFalse(box.text().contains("s"));
    }
}
