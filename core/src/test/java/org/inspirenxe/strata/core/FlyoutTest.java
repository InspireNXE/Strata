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
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.ComponentSnapshot;
import org.inspirenxe.strata.api.control.attribute.popup.Closable;
import org.inspirenxe.strata.api.control.attribute.popup.Openable;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.control.menu.Flyout;
import org.inspirenxe.strata.api.event.input.InputEvents;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifier;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.layout.AbsolutePanel;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.Themes;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The flyout: its size and place, opening and closing, focus and Tab, light dismiss, its content and the events that
 * come out of it. Text in the test platform is 6 units per code point and 9 per line, so a label of "Hello" is 30
 * by 9 and a flyout around it, with the default padding of 4, 38 by 17.
 */
class FlyoutTest {
    private static final int POPUP = 0xFF16171A;

    private final TestHost test = new TestHost(300, 200);
    private final ComponentFactory ui = this.test.factory();
    private final List<String> heard = new ArrayList<>();
    private Button behind;

    /**
     * Shows a screen with one button at (10, 10), 46 by 20, which logs its presses.
     */
    private void show() {
        this.behind = this.ui.button("Other").onEvent(Button.PRESSED, event -> this.heard.add("behind pressed"));
        final AbsolutePanel panel = this.ui.absolutePanel().add(this.behind, 10, 10);
        this.test.show(panel);
    }

    private Flyout logged(final Flyout flyout) {
        return flyout.onEvent(Openable.OPEN, event -> this.heard.add("open?"))
                .onEvent(Openable.OPENED, event -> this.heard.add("opened"))
                .onEvent(Closable.CLOSE, event -> this.heard.add("close?"))
                .onEvent(Closable.CLOSED, event -> this.heard.add("closed"));
    }

    private boolean key(final Key key) {
        return this.key(key, Modifiers.NONE);
    }

    private boolean key(final Key key, final Modifiers modifiers) {
        return this.test.host().key(key, KeyAction.PRESS, modifiers);
    }

    @Nested
    class Placing {
        @Test
        void sizesToItsContentPlusPaddingBelowItsAnchor() {
            FlyoutTest.this.show();
            final Label label = FlyoutTest.this.ui.label("Hello");
            final Flyout flyout = FlyoutTest.this.ui.flyout(label);
            flyout.open(FlyoutTest.this.behind.bounds());
            assertEquals(new Rect(10, 30, 38, 17), flyout.bounds());
            assertEquals(new Rect(14, 34, 30, 9), label.bounds());
        }

        @Test
        void opensAtAPoint() {
            FlyoutTest.this.show();
            final Flyout flyout = FlyoutTest.this.ui.flyout(FlyoutTest.this.ui.label("Hello"));
            flyout.openAt(100, 120);
            assertEquals(new Rect(100, 120, 38, 17), flyout.bounds());
        }

        @Test
        void opensAboveWhenThereIsMoreRoomThere() {
            FlyoutTest.this.show();
            final Flyout flyout = FlyoutTest.this.ui.flyout(FlyoutTest.this.ui.spacer(50, 60));
            flyout.open(new Rect(10, 180, 40, 10));
            assertEquals(new Rect(10, 180 - 68, 58, 68), flyout.bounds());
        }

        @Test
        void staysInsideTheHost() {
            FlyoutTest.this.show();
            final Flyout flyout = FlyoutTest.this.ui.flyout(FlyoutTest.this.ui.spacer(500, 50));
            flyout.open(FlyoutTest.this.behind.bounds());
            assertEquals(new Rect(0, 30, 300, 58), flyout.bounds());
        }

        @Test
        void readsItsBackgroundAndPaddingFromTheTheme() {
            FlyoutTest.this.show();
            final Flyout flyout = FlyoutTest.this.ui.flyout(FlyoutTest.this.ui.label("Hello"));
            flyout.open(FlyoutTest.this.behind.bounds());
            final DropDownButtonTest.Recording canvas = new DropDownButtonTest.Recording();
            FlyoutTest.this.test.host().render(canvas);
            // Without flyout.background, it fills with the combo box's popup paint
            assertTrue(canvas.fills.contains(new DropDownButtonTest.Fill(new Rect(10, 30, 38, 17), POPUP)));

            final Theme theme = Themes.flat().toBuilder()
                    .paint(Flyout.STYLE_BACKGROUND, Paint.solid(0xFF112233))
                    .insets(Flyout.STYLE_PADDING, new Insets(1, 2, 3, 4))
                    .build();
            final TestHost host = new TestHost(new TestPlatform(), theme, 300, 200);
            host.show(host.factory().absolutePanel());
            final Flyout themed = host.factory().flyout(host.factory().label("Hello"));
            themed.openAt(0, 0);
            assertEquals(new Rect(0, 0, 34, 15), themed.bounds());
            final DropDownButtonTest.Recording other = new DropDownButtonTest.Recording();
            host.host().render(other);
            assertTrue(other.fills.contains(new DropDownButtonTest.Fill(new Rect(0, 0, 34, 15), 0xFF112233)));
        }

        @Test
        void needsNoKeysOfItsOwn() {
            final TestHost host = new TestHost(new TestPlatform(), Theme.builder("bare").build(), 200, 200);
            host.show(host.factory().absolutePanel());
            final Flyout flyout = host.factory().flyout(host.factory().label("Hi"));
            flyout.openAt(0, 0);
            assertTrue(flyout.isOpen());
        }
    }

    @Nested
    class OpeningAndClosing {
        @Test
        void reportsOpenedAndClosedButNeverAsks() {
            FlyoutTest.this.show();
            final Flyout flyout = FlyoutTest.this.logged(FlyoutTest.this.ui.flyout(FlyoutTest.this.ui.label("Hello")));
            assertFalse(flyout.isOpen());
            flyout.openAt(50, 50);
            flyout.openAt(60, 60);
            assertTrue(flyout.isOpen());
            flyout.close();
            flyout.close();
            assertFalse(flyout.isOpen());
            assertEquals(List.of("opened", "closed"), FlyoutTest.this.heard);
        }

        @Test
        void withNoContentItDoesNothing() {
            FlyoutTest.this.show();
            final Flyout flyout = FlyoutTest.this.logged(FlyoutTest.this.ui.flyout());
            flyout.openAt(50, 50);
            assertFalse(flyout.isOpen());
            assertEquals(Optional.empty(), flyout.content());
            assertEquals(List.of(), FlyoutTest.this.heard);
        }

        @Test
        void escapeClosesIt() {
            FlyoutTest.this.show();
            final Flyout flyout = FlyoutTest.this.logged(FlyoutTest.this.ui.flyout(FlyoutTest.this.ui.label("Hello")));
            flyout.openAt(50, 50);
            assertTrue(FlyoutTest.this.key(Key.ESCAPE));
            assertFalse(flyout.isOpen());
            // Escape asks the flyout first (design 4.57)
            assertEquals(List.of("opened", "close?", "closed"), FlyoutTest.this.heard);
        }

        @Test
        void throwsWithoutAHost() {
            final ComponentFactoryImpl alone = new ComponentFactoryImpl(Themes::flat, new TestPlatform());
            final Flyout flyout = alone.flyout(alone.label("Hello"));
            assertThrows(IllegalStateException.class, () -> flyout.openAt(0, 0));
            assertThrows(IllegalStateException.class, () -> alone.menu().addItem("One", () -> { }).openAt(0, 0));
        }

        @Test
        void theSnapshotSaysWhetherItWasOpen() {
            FlyoutTest.this.show();
            final Flyout flyout = FlyoutTest.this.ui.flyout(FlyoutTest.this.ui.label("Hello"));
            final List<ComponentSnapshot> seen = new ArrayList<>();
            flyout.onEvent(InputEvents.POINTER_PRESS, event -> seen.add(event.snapshot()));
            flyout.openAt(50, 50);
            FlyoutTest.this.test.click(52, 52);
            assertEquals(new Flyout.State(true), seen.getFirst().state());
        }
    }

    @Nested
    class Focus {
        private TextBox name;
        private Button save;
        private Flyout flyout;

        /**
         * Opens a flyout holding a label, a text box and a button, with the screen's button focused.
         */
        private void open() {
            FlyoutTest.this.show();
            this.name = FlyoutTest.this.ui.textBox();
            this.save = FlyoutTest.this.ui.button("Save");
            final StackPanel form = FlyoutTest.this.ui.stackPanel()
                    .add(FlyoutTest.this.ui.label("Name"))
                    .add(this.name)
                    .add(this.save);
            this.flyout = FlyoutTest.this.ui.flyout(form);
            FlyoutTest.this.behind.requestFocus();
            this.flyout.open(FlyoutTest.this.behind.bounds());
        }

        @Test
        void movesToTheFirstTabStopInTheContent() {
            this.open();
            assertSame(this.name, FlyoutTest.this.test.focused());
        }

        @Test
        void restsOnTheFlyoutWhenTheContentHasNothingToFocus() {
            FlyoutTest.this.show();
            final Flyout flyout = FlyoutTest.this.ui.flyout(FlyoutTest.this.ui.label("Hello"));
            flyout.openAt(50, 50);
            assertSame(flyout, FlyoutTest.this.test.focused());
            assertFalse(flyout.isTabbable());
        }

        @Test
        void tabCyclesWithinTheContent() {
            this.open();
            assertTrue(FlyoutTest.this.key(Key.TAB));
            assertSame(this.save, FlyoutTest.this.test.focused());
            FlyoutTest.this.key(Key.TAB);
            assertSame(this.name, FlyoutTest.this.test.focused());
            FlyoutTest.this.key(Key.TAB, Modifiers.of(Modifier.SHIFT));
            assertSame(this.save, FlyoutTest.this.test.focused());
            FlyoutTest.this.test.host().key(Key.TAB, KeyAction.REPEAT, Modifiers.NONE);
            assertSame(this.name, FlyoutTest.this.test.focused());
        }

        @Test
        void tabStaysOnTheFlyoutWhenTheContentHasNothingToFocus() {
            FlyoutTest.this.show();
            final Flyout flyout = FlyoutTest.this.ui.flyout(FlyoutTest.this.ui.label("Hello"));
            flyout.openAt(50, 50);
            assertTrue(FlyoutTest.this.key(Key.TAB));
            assertSame(flyout, FlyoutTest.this.test.focused());
        }

        @Test
        void focusGoesBackWhenItCloses() {
            this.open();
            FlyoutTest.this.key(Key.ESCAPE);
            assertFalse(this.flyout.isOpen());
            assertSame(FlyoutTest.this.behind, FlyoutTest.this.test.focused());

            this.flyout.open(FlyoutTest.this.behind.bounds());
            this.flyout.close();
            assertSame(FlyoutTest.this.behind, FlyoutTest.this.test.focused());
        }

        @Test
        void newContentWhileOpenTakesFocusIfTheOldHadIt() {
            this.open();
            final Button other = FlyoutTest.this.ui.button("Other");
            this.flyout.setContent(other);
            assertTrue(this.flyout.isOpen());
            assertSame(other, FlyoutTest.this.test.focused());
            assertSame(other, this.flyout.content().orElseThrow());
            assertFalse(this.name.isAttached());
            FlyoutTest.this.test.host().layout();
            assertEquals(new Rect(14, 34, 6 * 5 + 16, 20), other.bounds());
        }

        @Test
        void clearingTheContentWhileOpenClosesIt() {
            this.open();
            FlyoutTest.this.logged(this.flyout);
            this.flyout.setContent(null);
            assertFalse(this.flyout.isOpen());
            assertEquals(List.of("closed"), FlyoutTest.this.heard);
            assertSame(FlyoutTest.this.behind, FlyoutTest.this.test.focused());
        }
    }

    @Nested
    class LightDismiss {
        @Test
        void aPressOutsideClosesItAndGoesNoFurther() {
            FlyoutTest.this.show();
            final Flyout flyout = FlyoutTest.this.logged(FlyoutTest.this.ui.flyout(FlyoutTest.this.ui.label("Hello")));
            assertTrue(flyout.isLightDismiss());
            flyout.openAt(100, 100);
            FlyoutTest.this.test.click(15, 15);
            assertFalse(flyout.isOpen());
            // A press outside asks the flyout first (design 4.57)
            assertEquals(List.of("opened", "close?", "closed"), FlyoutTest.this.heard);
        }

        @Test
        void withoutItAPressOutsideGoesThroughAndTheFlyoutStays() {
            FlyoutTest.this.show();
            final Flyout flyout = FlyoutTest.this.logged(FlyoutTest.this.ui.flyout(FlyoutTest.this.ui.label("Hello"))
                    .setLightDismiss(false));
            flyout.openAt(100, 100);
            FlyoutTest.this.test.click(15, 15);
            assertTrue(flyout.isOpen());
            assertEquals(List.of("opened", "behind pressed"), FlyoutTest.this.heard);
            assertSame(FlyoutTest.this.behind, FlyoutTest.this.test.focused());
            // Escape still closes it
            FlyoutTest.this.key(Key.ESCAPE);
            assertFalse(flyout.isOpen());
        }

        @Test
        void changingItWhileOpenAppliesAtOnce() {
            FlyoutTest.this.show();
            final Flyout flyout = FlyoutTest.this.ui.flyout(FlyoutTest.this.ui.label("Hello"));
            flyout.openAt(100, 100);
            flyout.setLightDismiss(false);
            FlyoutTest.this.test.click(15, 15);
            assertTrue(flyout.isOpen());
            flyout.setLightDismiss(true);
            FlyoutTest.this.test.click(15, 15);
            assertFalse(flyout.isOpen());
            assertEquals(List.of("behind pressed"), FlyoutTest.this.heard);
        }

        @Test
        void aPopupAboveItStillClosesOnAPressOutside() {
            FlyoutTest.this.show();
            final Flyout flyout = FlyoutTest.this.ui.flyout(FlyoutTest.this.ui.label("Hello")).setLightDismiss(false);
            flyout.openAt(100, 100);
            final Flyout above = FlyoutTest.this.ui.flyout(FlyoutTest.this.ui.label("Above"));
            above.openAt(200, 150);
            FlyoutTest.this.test.click(15, 15);
            assertFalse(above.isOpen());
            assertTrue(flyout.isOpen());
            assertEquals(List.of(), FlyoutTest.this.heard);
        }
    }

    @Nested
    class Content {
        @Test
        void controlEventsInTheContentReachTheFlyoutButNotTheScreen() {
            FlyoutTest.this.show();
            final List<String> screen = new ArrayList<>();
            FlyoutTest.this.behind.parent().orElseThrow().onEvent(Button.PRESSED, event -> screen.add("screen heard"));
            final Button inside = FlyoutTest.this.ui.button("Inside");
            final Flyout flyout = FlyoutTest.this.ui.flyout(FlyoutTest.this.ui.stackPanel().add(inside));
            flyout.onEvent(Button.PRESSED, event -> FlyoutTest.this.heard.add("flyout heard " + event.target()));
            flyout.openAt(100, 100);
            FlyoutTest.this.test.click(110, 110);
            assertEquals(List.of("flyout heard ButtonImpl"), FlyoutTest.this.heard);
            assertEquals(List.of(), screen);
        }

        @Test
        void contentAlreadyInATreeIsRefused() {
            FlyoutTest.this.show();
            final Flyout flyout = FlyoutTest.this.ui.flyout();
            assertThrows(IllegalArgumentException.class, () -> flyout.setContent(FlyoutTest.this.behind));
            final Component<?> label = FlyoutTest.this.ui.label("Hi");
            FlyoutTest.this.ui.flyout(label);
            assertThrows(IllegalArgumentException.class, () -> flyout.setContent(label));
            final TestHost other = new TestHost(100, 100);
            assertThrows(IllegalArgumentException.class, () -> flyout.setContent(other.factory().label("Hi")));
            assertEquals(Optional.empty(), flyout.content());
        }

        @Test
        void contentCanBeReplacedWhileClosed() {
            final Label first = FlyoutTest.this.ui.label("First");
            final Label second = FlyoutTest.this.ui.label("Second");
            final Flyout flyout = FlyoutTest.this.ui.flyout(first);
            flyout.setContent(second);
            assertSame(second, flyout.content().orElseThrow());
            assertEquals(Optional.empty(), first.parent());
            // The old content is free to go elsewhere
            FlyoutTest.this.ui.flyout(first);
        }
    }
}
