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
import org.inspirenxe.strata.api.control.pane.SplitPane;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifier;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.Orientation;
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
 * The split pane's divider as a tab stop that keys move (design 4.41). The pane is 204 wide, so 200 is shared and
 * each side starts at 100.
 */
class SplitPaneKeysTest {
    private final TestHost test = new TestHost(204, 100);
    private final ComponentFactory ui = this.test.factory();
    private SplitPane pane;
    private Button left;
    private Button right;

    private void show(final Orientation orientation) {
        this.left = this.ui.button("Left");
        this.right = this.ui.button("Right");
        this.pane = this.ui.splitPane(orientation).setFirst(this.left.setAnchor(Anchor.FILL))
                .setSecond(this.right.setAnchor(Anchor.FILL));
        this.test.show(this.pane.setAnchor(Anchor.FILL));
    }

    private void press(final Key key, final Modifiers modifiers) {
        this.test.host().key(key, KeyAction.PRESS, modifiers);
        this.test.host().key(key, KeyAction.RELEASE, modifiers);
        this.test.host().layout();
    }

    private float firstWidth() {
        return this.left.bounds().width();
    }

    @Test
    void theDividerIsATabStopBetweenTheSides() {
        this.show(Orientation.HORIZONTAL);
        this.left.requestFocus();
        this.press(Key.TAB, Modifiers.NONE);
        assertTrue(this.pane.isFocused());
        this.press(Key.TAB, Modifiers.NONE);
        assertSame(this.right, this.test.focused());
        assertEquals(3, this.test.tabOrder().size());
    }

    @Test
    void arrowsMoveTheDividerAndShiftMovesItByOne() {
        this.show(Orientation.HORIZONTAL);
        final List<Float> asked = new ArrayList<>();
        this.pane.onEvent(SplitPane.MOVE, event -> asked.add(event.value()));
        assertTrue(this.pane.requestFocus());
        this.press(Key.RIGHT, Modifiers.NONE);
        assertEquals(110f, this.firstWidth());
        this.press(Key.LEFT, Modifiers.of(Modifier.SHIFT));
        assertEquals(109f, this.firstWidth());
        this.press(Key.UP, Modifiers.NONE);
        assertEquals(109f, this.firstWidth());
        assertEquals(2, asked.size());
    }

    @Test
    void homeAndEndMoveToTheSidesMinimums() {
        this.show(Orientation.VERTICAL);
        this.pane.setMinSizes(10f, 30f);
        this.pane.requestFocus();
        this.press(Key.HOME, Modifiers.NONE);
        assertEquals(10f, this.left.bounds().height());
        this.press(Key.END, Modifiers.NONE);
        assertEquals(30f, this.right.bounds().height());
        this.press(Key.UP, Modifiers.NONE);
        assertEquals(40f, this.right.bounds().height());
    }

    @Test
    void aCanceledMoveKeepsTheDivider() {
        this.show(Orientation.HORIZONTAL);
        this.pane.onEvent(SplitPane.MOVE, event -> event.cancel());
        this.pane.requestFocus();
        this.press(Key.RIGHT, Modifiers.NONE);
        assertEquals(100f, this.firstWidth());
    }

    @Test
    void theFocusedDividerDrawsARing() {
        this.test.host().setTheme(Themes.flat().toBuilder().paint(SplitPane.STYLE_FOCUS, Paint.solid(0xFF000001))
                .build());
        this.show(Orientation.HORIZONTAL);
        final DrawLog idle = new DrawLog();
        this.test.host().render(idle);
        assertFalse(idle.lines().stream().anyMatch(line -> line.endsWith("#FF000001")));
        this.pane.requestFocus();
        final DrawLog focused = new DrawLog();
        this.test.host().render(focused);
        assertEquals(4, focused.lines().stream().filter(line -> line.endsWith("#FF000001")).count());
    }
}
