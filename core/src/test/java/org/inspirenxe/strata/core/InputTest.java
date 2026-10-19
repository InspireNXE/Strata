package org.inspirenxe.strata.core;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.event.EventTypes;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifier;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.layout.AbsolutePanel;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.ThemeException;
import org.inspirenxe.strata.api.theme.Themes;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputTest {
    private final TestHost test = new TestHost(300, 300);
    private final ComponentFactory ui = this.test.factory();

    private TestHost show(final AbsolutePanel panel) {
        return this.test.show(panel);
    }

    @Test
    void clickCountsGrowWithinTimeAndDistance() {
        final Box box = new Box(this.ui, 50, 50);
        final List<Integer> counts = new ArrayList<>();
        box.onEvent(EventTypes.CLICK, event -> counts.add(event.clicks()));
        this.show(this.ui.absolutePanel().add(box, 10, 10));

        this.test.click(20, 20);
        this.test.platform().advanceMillis(100);
        this.test.click(21, 21);
        this.test.platform().advanceMillis(100);
        this.test.click(22, 20);
        this.test.platform().advanceMillis(100);
        this.test.click(22, 20);
        this.test.platform().advanceMillis(1000);
        this.test.click(22, 20);

        assertEquals(List.of(1, 2, 3, 1, 1), counts);
    }

    @Test
    void clickMovedTooFarStartsOver() {
        final Box box = new Box(this.ui, 100, 100);
        final List<Integer> counts = new ArrayList<>();
        box.onEvent(EventTypes.CLICK, event -> counts.add(event.clicks()));
        this.show(this.ui.absolutePanel().add(box));

        this.test.click(10, 10);
        this.test.click(30, 10);

        assertEquals(List.of(1, 1), counts);
    }

    @Test
    void dragPastThresholdSuppressesClick() {
        final Box box = new Box(this.ui, 100, 100);
        final List<String> seen = new ArrayList<>();
        box.onEvent(EventTypes.CLICK, event -> seen.add("click"));
        box.onEvent(EventTypes.DRAG_START, event -> seen.add("start"));
        box.onEvent(EventTypes.DRAG, event -> seen.add("drag"));
        box.onEvent(EventTypes.DRAG_END, event -> seen.add("end"));
        this.show(this.ui.absolutePanel().add(box));

        this.test.host().pointerButton(10, 10, MouseButton.LEFT, true, Modifiers.NONE);
        this.test.host().pointerMoved(12, 12, Modifiers.NONE);
        assertTrue(seen.isEmpty(), "movement under the threshold is not a drag");
        this.test.host().pointerMoved(30, 10, Modifiers.NONE);
        this.test.host().pointerButton(30, 10, MouseButton.LEFT, false, Modifiers.NONE);

        assertEquals(List.of("start", "drag", "end"), seen);
    }

    @Test
    void dragKeepsGoingOutsideTheTarget() {
        final Box box = new Box(this.ui, 20, 20);
        final List<Float> xs = new ArrayList<>();
        box.onEvent(EventTypes.DRAG, event -> xs.add(event.x()));
        this.show(this.ui.absolutePanel().add(box));

        this.test.host().pointerButton(5, 5, MouseButton.LEFT, true, Modifiers.NONE);
        this.test.host().pointerMoved(200, 5, Modifiers.NONE);

        assertEquals(List.of(200f), xs);
    }

    @Test
    void releasingOneButtonDoesNotEndAnotherButtonsDrag() {
        final Box box = new Box(this.ui, 100, 100);
        final List<MouseButton> ended = new ArrayList<>();
        box.onEvent(EventTypes.DRAG_END, event -> ended.add(event.button()));
        this.show(this.ui.absolutePanel().add(box));

        this.test.host().pointerButton(10, 10, MouseButton.LEFT, true, Modifiers.NONE);
        this.test.host().pointerButton(10, 10, MouseButton.RIGHT, true, Modifiers.NONE);
        this.test.host().pointerMoved(50, 10, Modifiers.NONE);
        this.test.host().pointerButton(50, 10, MouseButton.RIGHT, false, Modifiers.NONE);
        assertEquals(List.of(MouseButton.RIGHT), ended);
        assertTrue(box.isPressed(), "left is still held");

        this.test.host().pointerButton(50, 10, MouseButton.LEFT, false, Modifiers.NONE);
        assertEquals(List.of(MouseButton.RIGHT, MouseButton.LEFT), ended);
        assertFalse(box.isPressed());
    }

    @Test
    void cancelingSkipsDefaultBehavior() {
        final Button button = this.ui.button("Go");
        final int[] presses = {0};
        button.onEvent(Button.PRESSED, event -> presses[0]++);
        this.show(this.ui.absolutePanel().add(button));

        this.test.click(5, 5);
        assertEquals(1, presses[0]);

        button.onEvent(EventTypes.CLICK, event -> event.cancel());
        this.test.platform().advanceMillis(1000);
        this.test.click(5, 5);
        assertEquals(1, presses[0], "a canceled click must not press the button");
    }

    @Test
    void handlersRemovedBySubscription() {
        final Box box = new Box(this.ui, 10, 10);
        final int[] calls = {0};
        box.subscribe(EventTypes.CLICK, 0, event -> calls[0]++).close();
        box.subscribe(EventTypes.POINTER_PRESS, 5, event -> calls[0]++).close();
        this.show(this.ui.absolutePanel().add(box));

        this.test.click(5, 5);
        assertEquals(0, calls[0]);
    }

    @Test
    void eventsReportPublicComponents() {
        final Box box = new Box(this.ui, 10, 10);
        final AbsolutePanel panel = this.ui.absolutePanel().add(box);
        final List<Object> currents = new ArrayList<>();
        box.onEvent(EventTypes.CLICK, event -> currents.add(event.current()));
        panel.onEvent(EventTypes.CLICK, event -> {
            currents.add(event.current());
            assertSame(box, event.target());
        });
        this.show(panel);

        this.test.click(5, 5);

        assertEquals(List.of(box, panel), currents);
    }

    @Test
    void pressFocusesAndEmptySpaceClears() {
        final Button button = this.ui.button("A");
        this.show(this.ui.absolutePanel().add(button));

        this.test.click(5, 5);
        assertSame(button, this.test.focused());
        assertTrue(button.isFocused());

        this.test.click(250, 250);
        assertNull(this.test.focused());
        assertFalse(button.isFocused());
    }

    @Test
    void tabCyclesByTabIndexThenTreeOrder() {
        final Button first = this.ui.button("1");
        final Button second = this.ui.button("2");
        final Button early = this.ui.button("0").setTabIndex(-1);
        final Button disabled = this.ui.button("x").setEnabled(false);
        this.show(this.ui.absolutePanel()
                .add(first, 0, 0)
                .add(disabled, 0, 30)
                .add(second, 0, 60)
                .add(early, 0, 90));

        assertEquals(List.of(early, first, second), this.test.tabOrder());

        this.test.host().key(Key.TAB, KeyAction.PRESS, Modifiers.NONE);
        assertSame(early, this.test.focused());
        this.test.host().key(Key.TAB, KeyAction.PRESS, Modifiers.NONE);
        assertSame(first, this.test.focused());
        this.test.host().key(Key.TAB, KeyAction.PRESS, Modifiers.of(Modifier.SHIFT));
        assertSame(early, this.test.focused());
        this.test.host().key(Key.TAB, KeyAction.PRESS, Modifiers.of(Modifier.SHIFT));
        assertSame(second, this.test.focused(), "shift-tab wraps to the end");
    }

    @Test
    void enterPressesFocusedButton() {
        final Button button = this.ui.button("Go");
        final int[] presses = {0};
        button.onEvent(Button.PRESSED, event -> presses[0]++);
        this.show(this.ui.absolutePanel().add(button));

        button.requestFocus();
        assertTrue(this.test.host().key(Key.ENTER, KeyAction.PRESS, Modifiers.NONE));
        assertEquals(1, presses[0]);
    }

    @Test
    void removingFocusedComponentClearsFocus() {
        final Button button = this.ui.button("Go");
        final AbsolutePanel panel = this.ui.absolutePanel().add(button);
        this.show(panel);

        button.requestFocus();
        panel.remove(button);

        assertNull(this.test.focused());
        assertFalse(button.isAttached());
        assertNull(button.parent());
    }

    @Test
    void disablingFocusedComponentClearsFocus() {
        final Button button = this.ui.button("Go");
        this.show(this.ui.absolutePanel().add(button));

        button.requestFocus();
        button.setEnabled(false);

        assertNull(this.test.focused());
    }

    @Test
    void synthesizedRepeatsWhenEngineSendsNone() {
        final TestPlatform quiet = new TestPlatform(false);
        final TestHost host = new TestHost(quiet, Themes.flat(), 100, 100);
        final Box box = new Box(host.factory(), 10, 10).setFocusable(true);
        final List<KeyAction> actions = new ArrayList<>();
        box.onEvent(EventTypes.KEY, event -> actions.add(event.action()));
        host.show(box);
        box.requestFocus();

        host.host().key(Key.LEFT, KeyAction.PRESS, Modifiers.NONE);
        quiet.advanceMillis(399);
        host.host().render(new NullCanvas());
        quiet.advanceMillis(1);
        host.host().render(new NullCanvas());
        quiet.advanceMillis(33);
        host.host().render(new NullCanvas());
        host.host().key(Key.LEFT, KeyAction.RELEASE, Modifiers.NONE);
        quiet.advanceMillis(500);
        host.host().render(new NullCanvas());

        assertEquals(List.of(KeyAction.PRESS, KeyAction.REPEAT, KeyAction.REPEAT, KeyAction.RELEASE), actions);
    }

    @Test
    void themeMissingKeysListsThemAll() {
        final Theme bare = Theme.builder("bare").build();
        final TestHost host = new TestHost(new TestPlatform(), bare, 100, 100);
        final Button button = host.factory().button("x");

        final ThemeException error = assertThrows(ThemeException.class, () -> host.show(button));
        assertEquals(Set.of(Button.STYLE_NORMAL, Button.STYLE_HOVERED, Button.STYLE_PRESSED, Button.STYLE_DISABLED),
                error.missingKeys());
        assertTrue(host.host().screen().isEmpty(), "a rejected screen is not opened");
        assertFalse(button.isAttached());
    }

    @Test
    void clipsStayBalanced() {
        final NullCanvas canvas = new NullCanvas();
        this.show(this.ui.absolutePanel().add(this.ui.anchorPanel().add(new Box(this.ui, 5, 5))));

        this.test.host().render(canvas);

        assertEquals(0, canvas.clipDepth());
    }
}
