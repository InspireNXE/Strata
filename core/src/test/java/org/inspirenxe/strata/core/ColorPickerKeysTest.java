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
import org.inspirenxe.strata.api.control.field.ColorPicker;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifier;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Themes;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The color picker as a tab stop that keys pick with (design 4.41).
 */
class ColorPickerKeysTest {
    private static final Modifiers SHIFT = Modifiers.of(Modifier.SHIFT);

    private final TestHost test = new TestHost(300, 300);
    private final ComponentFactory ui = this.test.factory();
    private final List<Integer> asked = new ArrayList<>();
    private ColorPicker picker;

    private void show() {
        this.picker = this.ui.colorPicker().onEvent(ColorPicker.PICK, event -> this.asked.add(event.value()));
        this.test.show(this.picker.setAnchor(Anchor.TOP_LEFT));
        this.picker.requestFocus();
    }

    private boolean press(final Key key, final Modifiers modifiers) {
        final boolean used = this.test.host().key(key, KeyAction.PRESS, modifiers);
        this.test.host().key(key, KeyAction.RELEASE, modifiers);
        return used;
    }

    @Test
    void thePickerIsATabStopBeforeItsField() {
        this.show();
        assertEquals(2, this.test.tabOrder().size());
        assertSame(this.picker, this.test.tabOrder().get(0));
        assertTrue(this.test.tabOrder().get(1) instanceof TextBox);
        this.test.host().root().clearFocus();
        this.test.click(20, 20);
        assertTrue(this.picker.isFocused());
    }

    @Test
    void arrowsMoveThePointInTheSquare() {
        this.show();
        assertTrue(this.press(Key.RIGHT, Modifiers.NONE));
        assertEquals(0.01f, this.picker.saturation(), 1e-6f);
        this.press(Key.RIGHT, SHIFT);
        assertEquals(0.11f, this.picker.saturation(), 1e-6f);
        this.press(Key.DOWN, Modifiers.NONE);
        assertEquals(0.99f, this.picker.brightness(), 1e-6f);
        this.press(Key.UP, SHIFT);
        assertEquals(1f, this.picker.brightness(), 1e-6f);
        assertEquals(4, this.asked.size());
        assertEquals(this.picker.color(), this.asked.get(this.asked.size() - 1));
    }

    @Test
    void pagesMoveTheHue() {
        this.show();
        this.picker.setColor(0xFFFF0000);
        this.press(Key.PAGE_DOWN, Modifiers.NONE);
        assertEquals(1f, this.picker.hue(), 1e-4f);
        this.press(Key.PAGE_DOWN, SHIFT);
        assertEquals(11f, this.picker.hue(), 1e-4f);
        this.press(Key.PAGE_UP, SHIFT);
        this.press(Key.PAGE_UP, SHIFT);
        assertEquals(0f, this.picker.hue(), 1e-4f);
    }

    @Test
    void homeAndEndSetTheAlphaWhileItsStripShows() {
        this.show();
        assertFalse(this.press(Key.END, Modifiers.NONE));
        this.picker.setAlphaShown(true);
        this.test.host().layout();
        assertTrue(this.press(Key.END, Modifiers.NONE));
        assertEquals(0, this.picker.color() >>> 24);
        this.press(Key.HOME, Modifiers.NONE);
        assertEquals(0xFF, this.picker.color() >>> 24);
    }

    @Test
    void aCanceledPickKeepsTheColor() {
        this.show();
        this.picker.onEvent(ColorPicker.PICK, event -> event.cancel());
        this.press(Key.RIGHT, Modifiers.NONE);
        assertEquals(0xFFFFFFFF, this.picker.color());
    }

    @Test
    void theFocusedPickerDrawsARing() {
        this.test.host().setTheme(Themes.flat().toBuilder().paint(ColorPicker.STYLE_FOCUS, Paint.solid(0xFF000001))
                .build());
        this.show();
        final DrawLog log = new DrawLog();
        this.test.host().render(log);
        assertEquals(4, log.lines().stream().filter(line -> line.endsWith("#FF000001")).count());
    }
}
