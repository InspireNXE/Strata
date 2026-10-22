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
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifier;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.ScrollKind;
import org.inspirenxe.strata.api.layout.ScrollPanel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sideways scrolling from Left, Right and Shift with the wheel (design 4.41).
 */
class ScrollKeysTest {
    private final TestHost test = new TestHost(300, 300);
    private final ComponentFactory ui = this.test.factory();
    private ScrollPanel panel;

    private void show(final ScrollPanel.ScrollPolicy horizontal) {
        final Button button = this.ui.button("Focus");
        this.panel = this.ui.scrollPanel().add(this.ui.stackPanel().add(button).add(new Box(this.ui, 400, 400)));
        this.panel.setPolicy(horizontal, ScrollPanel.ScrollPolicy.AUTO).setSize(100, 100);
        this.test.show(this.ui.absolutePanel().add(this.panel, 0, 0));
        button.requestFocus();
    }

    private boolean press(final Key key) {
        final boolean used = this.test.host().key(key, KeyAction.PRESS, Modifiers.NONE);
        this.test.host().key(key, KeyAction.RELEASE, Modifiers.NONE);
        this.test.host().layout();
        return used;
    }

    @Test
    void leftAndRightScrollSideways() {
        this.show(ScrollPanel.ScrollPolicy.AUTO);
        assertTrue(this.press(Key.RIGHT));
        assertEquals(this.panel.scrollRate(), this.panel.scrollX());
        assertTrue(this.press(Key.LEFT));
        assertEquals(0f, this.panel.scrollX());
        assertFalse(this.press(Key.LEFT));
        assertTrue(this.press(Key.DOWN));
        assertEquals(this.panel.scrollRate(), this.panel.scrollY());
    }

    @Test
    void aPanelThatDoesNotScrollSidewaysLeavesLeftAndRight() {
        this.show(ScrollPanel.ScrollPolicy.NEVER);
        assertFalse(this.press(Key.RIGHT));
        assertEquals(0f, this.panel.scrollX());
    }

    @Test
    void shiftTurnsTheWheelSideways() {
        this.show(ScrollPanel.ScrollPolicy.AUTO);
        assertTrue(this.test.host().scroll(20, 50, 0f, -1f, ScrollKind.WHEEL, Modifiers.of(Modifier.SHIFT)));
        this.test.host().layout();
        assertEquals(this.panel.scrollRate(), this.panel.scrollX());
        assertEquals(0f, this.panel.scrollY());
        // A horizontal scroll with Shift is taken as it is
        assertTrue(this.test.host().scroll(20, 50, 1f, 0f, ScrollKind.WHEEL, Modifiers.of(Modifier.SHIFT)));
        this.test.host().layout();
        assertEquals(0f, this.panel.scrollX());
    }
}
