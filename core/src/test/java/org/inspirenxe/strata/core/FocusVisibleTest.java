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
import org.inspirenxe.strata.api.component.Focusable;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.event.input.FocusEvent;
import org.inspirenxe.strata.api.event.input.FocusReason;
import org.inspirenxe.strata.api.event.input.InputEvents;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.layout.AbsolutePanel;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.Themes;
import org.inspirenxe.strata.spi.ScreenStack;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why focus moved, and whether it should be shown.
 */
class FocusVisibleTest {
    private static final Paint PLAIN = Paint.solid(0xFF000001);
    private static final Paint FOCUSED = Paint.solid(0xFF000002);
    private static final Paint SHOWN = Paint.solid(0xFF000003);
    private static final Theme THEME = Themes.flat().toBuilder()
            .paint("menu.item", PLAIN)
            .paint("menu.item.focused", FOCUSED)
            .paint("menu.item.focus_visible", SHOWN)
            .build();

    private final TestHost test = new TestHost(new TestPlatform(), THEME, 300, 100);
    private final ComponentFactory ui = this.test.factory();
    private final MenuItem first = new MenuItem(this.ui);
    private final MenuItem second = new MenuItem(this.ui);
    private final AbsolutePanel panel = this.ui.absolutePanel().add(this.first, 0, 0).add(this.second, 100, 0);
    private final List<String> heard = new ArrayList<>();

    FocusVisibleTest() {
        for (final MenuItem item : List.of(this.first, this.second)) {
            final String name = item == this.first ? "first" : "second";
            item.onEvent(Focusable.FOCUS_GAINED, e -> this.heard.add(name + " gained " + e.reason()));
            item.onEvent(Focusable.FOCUS_LOST, e -> this.heard.add(name + " lost " + e.reason()));
        }
        this.test.show(this.panel);
    }

    private void key(final Key key) {
        this.test.host().key(key, KeyAction.PRESS, Modifiers.NONE);
        this.test.host().key(key, KeyAction.RELEASE, Modifiers.NONE);
    }

    @Test
    void aClickFocusesForThePointerWithoutShowingIt() {
        this.test.click(10, 10);
        assertEquals(List.of("first gained POINTER"), this.heard);
        assertTrue(this.first.isFocused());
        assertFalse(this.first.isFocusVisible());
    }

    @Test
    void tabFocusesForTheKeyboardAndShowsIt() {
        this.key(Key.TAB);
        assertEquals(List.of("first gained KEYBOARD"), this.heard);
        assertTrue(this.first.isFocusVisible());
        this.key(Key.TAB);
        assertEquals(List.of("first gained KEYBOARD", "first lost KEYBOARD", "second gained KEYBOARD"), this.heard);
        assertFalse(this.first.isFocusVisible());
        assertTrue(this.second.isFocusVisible());
    }

    @Test
    void codeWithNoInputShowsFocusUntilAPointerWasPressed() {
        this.first.requestFocus();
        assertEquals(List.of("first gained CODE"), this.heard);
        assertTrue(this.first.isFocusVisible(), "nothing was pressed yet");
        this.test.click(150, 50);
        this.second.requestFocus();
        assertEquals(FocusReason.CODE, this.second.lastReason);
        assertFalse(this.second.isFocusVisible(), "the last press was a pointer's");
    }

    @Test
    void focusMovedFromAKeyHandlerCountsAsKeyboardAndFromAClickHandlerAsPointer() {
        this.first.onEvent(InputEvents.KEY, e -> this.second.requestFocus());
        this.first.requestFocus();
        this.key(Key.DOWN);
        assertEquals(FocusReason.KEYBOARD, this.second.lastReason);
        assertTrue(this.second.isFocusVisible());

        final Box box = new Box(this.ui, 20, 20);
        this.panel.add(box, 200, 0);
        box.onEvent(InputEvents.CLICK, e -> this.first.requestFocus());
        this.test.click(205, 5);
        assertEquals(FocusReason.POINTER, this.first.lastReason);
        assertFalse(this.first.isFocusVisible());
    }

    @Test
    void aKeyShowsFocusAClickGaveAndAPressHidesItAgain() {
        this.test.click(10, 10);
        assertFalse(this.first.isFocusVisible());
        this.key(Key.LEFT_SHIFT);
        assertFalse(this.first.isFocusVisible(), "a modifier alone does not show focus");
        this.key(Key.SPACE);
        assertTrue(this.first.isFocusVisible());
        this.test.click(10, 10);
        assertFalse(this.first.isFocusVisible());
    }

    @Test
    void theStyleStatePicksTheFocusVisibleKeyOnlyWhileFocusIsShown() {
        assertEquals(PLAIN, this.first.face());
        this.test.click(10, 10);
        // Moved away so the item is not hovered either
        this.test.host().pointerMoved(250, 90, Modifiers.NONE);
        assertEquals(FOCUSED, this.first.face());
        this.key(Key.TAB);
        this.key(Key.TAB);
        assertEquals(SHOWN, this.first.face());
    }

    @Test
    void deactivationTakesFocusAwayAndActivationGivesItBackAsItWasShown() {
        this.key(Key.TAB);
        this.heard.clear();
        this.test.host().activeChanged(false);
        assertNull(this.test.focused());
        assertEquals(List.of("first lost ACTIVATION"), this.heard);
        this.test.host().activeChanged(false);
        assertEquals(1, this.heard.size(), "a repeated state does nothing");
        this.test.host().activeChanged(true);
        assertSame(this.first, this.test.focused());
        assertEquals(List.of("first lost ACTIVATION", "first gained ACTIVATION"), this.heard);
        assertTrue(this.first.isFocusVisible());
    }

    @Test
    void activationKeepsFocusHiddenWhenAClickGaveIt() {
        this.test.click(10, 10);
        this.test.host().activeChanged(false);
        this.test.host().activeChanged(true);
        assertSame(this.first, this.test.focused());
        assertFalse(this.first.isFocusVisible());
    }

    @Test
    void activationGivesNothingBackWhenTheComponentCannotTakeFocusAnyMore() {
        this.first.requestFocus();
        this.test.host().activeChanged(false);
        this.first.setVisible(false);
        this.test.host().activeChanged(true);
        assertNull(this.test.focused());
    }

    @Test
    void aClickedButtonDoesNotLookFocusedOnceThePointerLeaves() {
        final Button button = this.ui.button("OK");
        this.test.show(this.ui.absolutePanel().add(button, 0, 0));
        final List<String> idle = this.render();

        this.test.click(5, 5);
        this.test.host().pointerMoved(250, 90, Modifiers.NONE);
        assertTrue(button.isFocused());
        assertEquals(idle, this.render(), "focus from a click draws neither the lit face nor the ring");

        this.key(Key.TAB);
        this.key(Key.TAB);
        assertTrue(button.isFocusVisible());
        assertNotEquals(idle, this.render(), "focus from the keyboard shows");
    }

    @Test
    void aStackMakesEveryScreenInactiveAndScreensPushedLaterStartThatWay() {
        final ScreenStack stack = StrataCore.createScreenStack(new TestPlatform(), Themes.flat());
        stack.resize(200, 100);
        final MenuItem[] items = new MenuItem[2];
        stack.push(ui -> items[0] = new MenuItem(ui));
        items[0].requestFocus();
        stack.activeChanged(false);
        assertFalse(items[0].isFocused());
        stack.push(ui -> items[1] = new MenuItem(ui));
        items[1].requestFocus();
        stack.activeChanged(true);
        assertTrue(items[1].isFocused(), "the top screen's item had focus when the stack became active");
    }

    private List<String> render() {
        final DrawLog canvas = new DrawLog();
        this.test.host().render(canvas);
        return canvas.lines();
    }

    /**
     * A menu item that a developer writes, drawn from the theme by its style states.
     */
    static final class MenuItem extends CustomComponent<MenuItem> implements Focusable<MenuItem> {
        FocusReason lastReason;

        MenuItem(final ComponentFactory ui) {
            super(ui);
            this.onEvent(Focusable.FOCUS_GAINED, (FocusEvent e) -> this.lastReason = e.reason());
        }

        Paint face() {
            return this.stylePaint("menu.item", Paint.NONE);
        }

        @Override
        protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
            return new Size(50, 20);
        }

        @Override
        protected void paint(final Painter painter, final Rect bounds) {
            this.face().paint(painter, bounds);
        }
    }
}
