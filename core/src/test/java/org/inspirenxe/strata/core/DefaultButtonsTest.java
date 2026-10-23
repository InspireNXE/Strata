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
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.control.window.MessageBox;
import org.inspirenxe.strata.api.control.window.Window;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifier;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.inspirenxe.strata.api.overlay.Overlay;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Themes;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Default and cancel buttons, and the default result of a message box (design 4.40).
 */
class DefaultButtonsTest {
    private final TestHost test = new TestHost(400, 300);
    private final ComponentFactory ui = this.test.factory();
    private final List<String> heard = new ArrayList<>();

    private boolean press(final Key key) {
        return this.press(key, Modifiers.NONE);
    }

    private boolean press(final Key key, final Modifiers modifiers) {
        final boolean used = this.test.host().key(key, KeyAction.PRESS, modifiers);
        this.test.host().key(key, KeyAction.RELEASE, modifiers);
        return used;
    }

    private Button button(final String name) {
        return this.ui.button(name)
                .onEvent(Button.PRESS, event -> this.heard.add("press " + name))
                .onEvent(Button.PRESSED, event -> this.heard.add("pressed " + name));
    }

    @Test
    void enterInAFieldPressesTheDefaultButton() {
        final TextBox name = this.ui.textBox();
        final Button ok = this.button("ok").setDefault(true);
        this.test.show(this.ui.stackPanel().add(name).add(ok).setAnchor(Anchor.TOP_LEFT));
        name.requestFocus();
        assertTrue(this.press(Key.ENTER));
        assertTrue(this.press(Key.KP_ENTER));
        assertEquals(List.of("press ok", "pressed ok", "press ok", "pressed ok"), this.heard);
        assertFalse(this.press(Key.ENTER, Modifiers.of(Modifier.CONTROL)));
        assertTrue(ok.isDefault());
    }

    @Test
    void aFieldThatSubmitsKeepsEnter() {
        final TextBox name = this.ui.textBox().onEvent(TextBox.SUBMITTED, event -> this.heard.add("submitted"));
        final Button ok = this.button("ok").setDefault(true);
        this.test.show(this.ui.stackPanel().add(name).add(ok).setAnchor(Anchor.TOP_LEFT));
        name.requestFocus();
        this.press(Key.ENTER);
        assertEquals(List.of("submitted"), this.heard);
    }

    @Test
    void aFocusedButtonPressesItself() {
        final Button other = this.button("other");
        final Button ok = this.button("ok").setDefault(true);
        this.test.show(this.ui.stackPanel().add(other).add(ok).setAnchor(Anchor.TOP_LEFT));
        other.requestFocus();
        this.press(Key.ENTER);
        assertEquals(List.of("press other", "pressed other"), this.heard);
    }

    @Test
    void aDisabledOrHiddenDefaultButtonIsSkipped() {
        final TextBox name = this.ui.textBox();
        final Button ok = this.button("ok").setDefault(true);
        final StackPanel group = this.ui.stackPanel().add(ok);
        this.test.show(this.ui.stackPanel().add(name).add(group).setAnchor(Anchor.TOP_LEFT));
        name.requestFocus();
        group.setEnabled(false);
        assertFalse(this.press(Key.ENTER));
        group.setEnabled(true);
        ok.setVisible(false);
        assertFalse(this.press(Key.ENTER));
        assertEquals(List.of(), this.heard);
    }

    @Test
    void theNearestWindowIsTheScope() {
        final TextBox inA = this.ui.textBox();
        final Window a = this.ui.window("A").add(this.ui.stackPanel().add(inA).add(this.button("a").setDefault(true)));
        final TextBox outside = this.ui.textBox();
        final Window b = this.ui.window("B").add(this.button("b").setDefault(true));
        final Button screen = this.button("screen").setDefault(true);
        this.test.show(this.ui.stackPanel().add(a).add(b).add(outside).add(screen).setAnchor(Anchor.TOP_LEFT));
        inA.requestFocus();
        this.press(Key.ENTER);
        outside.requestFocus();
        this.press(Key.ENTER);
        assertEquals(List.of("press a", "pressed a", "press screen", "pressed screen"), this.heard);
    }

    @Test
    void escapePressesTheCancelButtonBeforeTheOverlayCloses() {
        this.test.show(this.ui.button("Target"));
        final TextBox name = this.ui.textBox();
        final Button cancel = this.button("cancel").setCancel(true);
        final Overlay dialog = this.ui.overlays().dialog(this.ui.stackPanel().add(name).add(cancel));
        name.requestFocus();
        assertTrue(this.press(Key.ESCAPE));
        assertEquals(List.of("press cancel", "pressed cancel"), this.heard);
        assertTrue(dialog.isOpen());
        cancel.setCancel(false);
        this.press(Key.ESCAPE);
        assertFalse(dialog.isOpen());
    }

    @Test
    void aWindowInAnOverlayPressesItsCancelButtonInPlaceOfClosing() {
        this.test.show(this.ui.button("Target"));
        final TextBox name = this.ui.textBox();
        final Window window = this.ui.window("Edit").add(this.ui.stackPanel().add(name)
                .add(this.button("cancel").setCancel(true)));
        final Overlay dialog = this.ui.overlays().dialog(window);
        name.requestFocus();
        this.press(Key.ESCAPE);
        assertEquals(List.of("press cancel", "pressed cancel"), this.heard);
        assertTrue(dialog.isOpen());
    }

    @Test
    void aDefaultButtonDrawsTheDefaultFace() {
        this.test.host().setTheme(Themes.flat().toBuilder().paint(Button.STYLE_DEFAULT, Paint.solid(0xFF000001)).build());
        final Button ok = this.ui.button("OK").setAnchor(Anchor.TOP_LEFT);
        this.test.show(ok);
        final DrawLog plain = new DrawLog();
        this.test.host().render(plain);
        assertFalse(plain.lines().get(0).endsWith("#FF000001"));
        ok.setDefault(true);
        final DrawLog marked = new DrawLog();
        this.test.host().render(marked);
        assertTrue(marked.lines().get(0).endsWith("#FF000001"), marked.lines().toString());
    }

    @Test
    void aMessageBoxFocusesItsDefaultResultsButton() {
        this.test.show(this.ui.button("Target"));
        final List<MessageBox.Result> results = new ArrayList<>();
        final MessageBox box = this.ui.messageBox("Delete", "Delete it?", MessageBox.Buttons.YES_NO)
                .onEvent(MessageBox.ANSWERED, event -> results.add(event.value()));
        assertSame(MessageBox.Result.YES, box.defaultResult());
        box.setDefaultResult(MessageBox.Result.NO);
        assertSame(MessageBox.Result.NO, box.defaultResult());
        this.ui.overlays().dialog(box);
        final Button focused = (Button) this.test.focused();
        assertEquals("No", focused.text().plain());
        assertTrue(focused.isDefault());
        assertTrue(focused.isCancel());
        this.test.host().root().clearFocus();
        this.press(Key.ENTER);
        assertEquals(List.of(MessageBox.Result.NO), results);
    }

    @Test
    void aMessageBoxRefusesADefaultItHasNoButtonFor() {
        final MessageBox box = this.ui.messageBox("Delete", "Delete it?", MessageBox.Buttons.YES_NO);
        assertThrows(IllegalArgumentException.class, () -> box.setDefaultResult(MessageBox.Result.OK));
        box.setDefaultResult(MessageBox.Result.NO).setButtons(MessageBox.Buttons.OK_CANCEL);
        assertSame(MessageBox.Result.OK, box.defaultResult());
    }
}
