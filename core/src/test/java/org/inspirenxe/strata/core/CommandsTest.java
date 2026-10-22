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
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.button.CheckBox;
import org.inspirenxe.strata.api.control.button.ToggleSwitch;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.control.menu.CheckMenuItem;
import org.inspirenxe.strata.api.control.menu.Flyout;
import org.inspirenxe.strata.api.control.menu.Menu;
import org.inspirenxe.strata.api.control.menu.MenuBar;
import org.inspirenxe.strata.api.control.menu.MenuItem;
import org.inspirenxe.strata.api.event.Subscription;
import org.inspirenxe.strata.api.event.cause.CauseKeys;
import org.inspirenxe.strata.api.event.cause.Device;
import org.inspirenxe.strata.api.event.cause.Origin;
import org.inspirenxe.strata.api.event.input.InputEvents;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.KeyStroke;
import org.inspirenxe.strata.api.input.Modifier;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.inspirenxe.strata.api.state.State;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.theme.Themes;
import org.inspirenxe.strata.spi.Conventions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Key strokes, commands on controls and entries, and the shortcuts a host runs from them (design 4.40).
 */
class CommandsTest {
    private static final Modifiers CTRL = Modifiers.of(Modifier.CONTROL);

    private final TestHost test = new TestHost(300, 300);
    private final ComponentFactory ui = this.test.factory();
    private final List<String> heard = new ArrayList<>();

    private boolean press(final Key key, final Modifiers modifiers) {
        final boolean used = this.test.host().key(key, KeyAction.PRESS, modifiers);
        this.test.host().key(key, KeyAction.RELEASE, modifiers);
        return used;
    }

    private void clickCenter(final Component<?> component) {
        final Rect bounds = component.bounds();
        this.test.click(bounds.x() + bounds.width() / 2f, bounds.y() + bounds.height() / 2f);
    }

    @Nested
    class Strokes {
        @Test
        void aShortcutHoldsThePlatformsModifier() {
            final KeyStroke save = KeyStroke.shortcut(Key.S, Modifier.SHIFT);
            assertEquals(Modifiers.of(Modifier.CONTROL, Modifier.SHIFT), save.resolve(Modifier.CONTROL));
            assertEquals(Modifiers.of(Modifier.SUPER, Modifier.SHIFT), save.resolve(Modifier.SUPER));
            assertTrue(save.matches(Key.S, Modifiers.of(Modifier.SHIFT, Modifier.CONTROL), Modifier.CONTROL));
            assertFalse(save.matches(Key.S, Modifiers.of(Modifier.CONTROL), Modifier.CONTROL));
            assertEquals(Modifiers.NONE, KeyStroke.of(Key.F1).resolve(Modifier.SUPER));
        }

        @Test
        void aKeyEventMatchesAStrokeOnItsPlatform() {
            final TestPlatform platform = new TestPlatform();
            platform.setConventions(new Conventions(false, Modifier.SUPER, true));
            final TestHost mac = new TestHost(platform, Themes.flat(), 100, 100);
            final List<Boolean> matched = new ArrayList<>();
            final Button target = mac.factory().button("Target");
            target.onEvent(InputEvents.KEY, event -> {
                if (event.action() == KeyAction.PRESS) {
                    matched.add(event.is(KeyStroke.shortcut(Key.S)));
                    assertSame(Modifier.SUPER, event.shortcutModifier());
                }
            });
            mac.show(target);
            target.requestFocus();
            mac.host().key(Key.S, KeyAction.PRESS, Modifiers.of(Modifier.SUPER));
            mac.host().key(Key.S, KeyAction.PRESS, CTRL);
            assertEquals(List.of(true, false), matched);
        }

        @Test
        void keyTextNamesModifiersInOrderAndTheKey() {
            final ComponentFactory ui = CommandsTest.this.ui;
            assertEquals("Ctrl+Shift+S", ui.keyText(KeyStroke.shortcut(Key.S, Modifier.SHIFT)).plain());
            assertEquals("Ctrl+Alt+Del", ui.keyText(KeyStroke.of(Key.DELETE, Modifier.ALT, Modifier.CONTROL)).plain());
            assertEquals("F5", ui.keyText(KeyStroke.of(Key.F5)).plain());
            assertEquals("Esc", ui.keyText(KeyStroke.of(Key.ESCAPE)).plain());
            assertEquals("Num 5", ui.keyText(KeyStroke.of(Key.KP_5)).plain());
            assertEquals("Num Enter", ui.keyText(KeyStroke.of(Key.KP_ENTER)).plain());
            assertEquals("Shift+/", ui.keyText(KeyStroke.of(Key.SLASH, Modifier.SHIFT)).plain());
        }

        @Test
        void keyTextNamesTheShortcutModifierOfThePlatform() {
            final TestPlatform platform = new TestPlatform();
            platform.setConventions(new Conventions(false, Modifier.SUPER, true));
            final TestHost mac = new TestHost(platform, Themes.flat(), 100, 100);
            assertEquals("Super+S", mac.factory().keyText(KeyStroke.shortcut(Key.S)).plain());
        }

        @Test
        void theOneArgumentConventionsKeepControlAndMnemonics() {
            final Conventions conventions = new Conventions(true);
            assertSame(Modifier.CONTROL, conventions.shortcutModifier());
            assertTrue(conventions.mnemonics());
            assertEquals(new Conventions(false), Conventions.DEFAULT);
        }
    }

    @Nested
    class Controls {
        @Test
        void aButtonTakesTheTextFollowsEnabledAndRunsAfterItsReport() {
            final CommandsTest outer = CommandsTest.this;
            final State<Boolean> dirty = State.of(false);
            final Command.Action save = Command.of("Save", () -> outer.heard.add("save")).withEnabled(dirty);
            final Button button = outer.ui.button().setCommand(save).setAnchor(Anchor.TOP_LEFT);
            button.onEvent(Button.PRESSED, event -> outer.heard.add("pressed"));
            outer.test.show(button);
            assertEquals("Save", button.text().plain());
            assertFalse(button.isEnabled());
            dirty.set(true);
            outer.test.host().flush();
            assertTrue(button.isEnabled());
            outer.clickCenter(button);
            assertEquals(List.of("pressed", "save"), outer.heard);
            button.press();
            assertEquals(List.of("pressed", "save", "pressed", "save"), outer.heard);
            assertSame(save, button.command().orElseThrow());
        }

        @Test
        void aCanceledPressRunsNoCommand() {
            final CommandsTest outer = CommandsTest.this;
            final Button button = outer.ui.button().setCommand(Command.of("Go", () -> outer.heard.add("go")));
            button.setAnchor(Anchor.TOP_LEFT).onEvent(Button.PRESS, event -> event.cancel());
            outer.test.show(button);
            outer.clickCenter(button);
            assertEquals(List.of(), outer.heard);
        }

        @Test
        void clearingTheCommandKeepsTheTextAndStopsFollowing() {
            final CommandsTest outer = CommandsTest.this;
            final State<Boolean> enabled = State.of(true);
            final Button button = outer.ui.button().setCommand(Command.of("Save", () -> { }).withEnabled(enabled));
            outer.test.show(button);
            button.setCommand(null);
            enabled.set(false);
            outer.test.host().flush();
            assertTrue(button.isEnabled());
            assertEquals("Save", button.text().plain());
            assertTrue(button.command().isEmpty());
        }

        @Test
        void aCheckBoxAndASwitchShareTheToggleState() {
            final CommandsTest outer = CommandsTest.this;
            final State<Boolean> grid = State.of(false);
            final Command.Toggle toggle = Command.toggle("Grid", grid);
            final CheckBox box = outer.ui.checkBox().setCommand(toggle);
            final ToggleSwitch flip = outer.ui.toggleSwitch().setCommand(toggle);
            final StackPanel column = outer.ui.stackPanel().add(box).add(flip);
            outer.test.show(column.setAnchor(Anchor.TOP_LEFT));
            assertEquals("Grid", box.text().plain());
            outer.clickCenter(box);
            assertTrue(grid.get());
            outer.test.host().flush();
            assertTrue(flip.isSelected());
            grid.set(false);
            outer.test.host().flush();
            assertFalse(box.isSelected());
            assertFalse(flip.isSelected());
        }

        @Test
        void aMenuItemRunsItsCommandAfterChosen() {
            final CommandsTest outer = CommandsTest.this;
            final State<Boolean> enabled = State.of(true);
            final MenuItem item = outer.ui.menuItem().setCommand(
                    Command.of("Open", () -> outer.heard.add("open")).withEnabled(enabled));
            final Menu menu = outer.ui.menu().addItem(item).onEvent(Menu.CHOSEN, event -> outer.heard.add("chosen"));
            outer.test.show(outer.ui.button("Target").setAnchor(Anchor.TOP_LEFT));
            assertEquals("Open", item.text().plain());
            menu.openAt(10, 10);
            outer.test.host().layout();
            outer.press(Key.DOWN, Modifiers.NONE);
            outer.press(Key.ENTER, Modifiers.NONE);
            assertEquals(List.of("chosen", "open"), outer.heard);
            enabled.set(false);
            assertFalse(item.isEnabled());
        }

        @Test
        void aMenuItemShowsTheStrokeWhenItHasNoHint() {
            final CommandsTest outer = CommandsTest.this;
            final MenuItem item = outer.ui.menuItem().setCommand(
                    Command.of("Save", () -> { }).withShortcut(KeyStroke.shortcut(Key.S)));
            final Menu menu = outer.ui.menu().addItem(item);
            outer.test.show(outer.ui.button("Target").setAnchor(Anchor.TOP_LEFT));
            menu.openAt(10, 10);
            final DrawLog log = new DrawLog();
            outer.test.host().render(log);
            assertTrue(log.lines().stream().anyMatch(line -> line.startsWith("text Ctrl+S ")), log.lines().toString());
            item.setShortcut("Custom");
            final DrawLog again = new DrawLog();
            outer.test.host().render(again);
            assertTrue(again.lines().stream().anyMatch(line -> line.startsWith("text Custom ")));
        }

        @Test
        void aCheckMenuItemShowsAndFlipsTheToggleState() {
            final CommandsTest outer = CommandsTest.this;
            final State<Boolean> prices = State.of(true);
            final CheckMenuItem item = outer.ui.checkMenuItem().setCommand(Command.toggle("Prices", prices));
            final Menu menu = outer.ui.menu().addItem(item);
            outer.test.show(outer.ui.button("Target").setAnchor(Anchor.TOP_LEFT));
            assertTrue(item.isSelected());
            menu.openAt(10, 10);
            outer.test.host().layout();
            outer.press(Key.DOWN, Modifiers.NONE);
            outer.press(Key.ENTER, Modifiers.NONE);
            assertFalse(prices.get());
            item.setCommand(null);
            prices.set(true);
            assertFalse(item.isSelected());
        }
    }

    @Nested
    class Shortcuts {
        @Test
        void aHostShortcutRunsWhenNothingConsumedTheKey() {
            final CommandsTest outer = CommandsTest.this;
            final State<Boolean> enabled = State.of(true);
            final Command.Action save = Command.of("Save", () -> outer.heard.add("save"))
                    .withShortcut(KeyStroke.shortcut(Key.S)).withEnabled(enabled);
            final Button target = outer.ui.button("Target").setAnchor(Anchor.TOP_LEFT);
            outer.test.show(target);
            target.requestFocus();
            final Subscription added = outer.ui.shortcuts().add(save);
            assertTrue(outer.press(Key.S, CTRL));
            assertFalse(outer.press(Key.S, Modifiers.of(Modifier.CONTROL, Modifier.SHIFT)));
            enabled.set(false);
            assertFalse(outer.press(Key.S, CTRL));
            enabled.set(true);
            added.unsubscribe();
            assertFalse(outer.press(Key.S, CTRL));
            assertEquals(List.of("save"), outer.heard);
        }

        @Test
        void theFocusPathComesFirst() {
            final CommandsTest outer = CommandsTest.this;
            outer.ui.shortcuts().add(Command.of("All", () -> outer.heard.add("all"))
                    .withShortcut(KeyStroke.shortcut(Key.A)));
            final TextBox box = outer.ui.textBox().setText("abc");
            outer.test.show(box.setAnchor(Anchor.TOP_LEFT));
            box.requestFocus();
            outer.press(Key.A, CTRL);
            assertEquals(List.of(), outer.heard);
        }

        @Test
        void theInnermostScopeWinsAndScopesNeedFocusInside() {
            final CommandsTest outer = CommandsTest.this;
            final Button inside = outer.ui.button("Inside");
            final Button outside = outer.ui.button("Outside");
            final StackPanel group = outer.ui.stackPanel().add(inside);
            final StackPanel column = outer.ui.stackPanel().add(group).add(outside);
            final KeyStroke f2 = KeyStroke.of(Key.F2);
            outer.ui.shortcuts().add(Command.of("Host", () -> outer.heard.add("host")).withShortcut(f2));
            outer.ui.shortcuts().add(column, Command.of("Column", () -> outer.heard.add("column")).withShortcut(f2));
            outer.ui.shortcuts().add(group, Command.of("Group", () -> outer.heard.add("group")).withShortcut(f2));
            outer.test.show(column.setAnchor(Anchor.TOP_LEFT));
            inside.requestFocus();
            outer.press(Key.F2, Modifiers.NONE);
            outside.requestFocus();
            outer.press(Key.F2, Modifiers.NONE);
            outer.test.host().root().clearFocus();
            outer.press(Key.F2, Modifiers.NONE);
            assertEquals(List.of("group", "column", "column"), outer.heard);
        }

        @Test
        void aDialogBlocksTheHostsShortcutsButNotItsOwn() {
            final CommandsTest outer = CommandsTest.this;
            final KeyStroke f2 = KeyStroke.of(Key.F2);
            outer.ui.shortcuts().add(Command.of("Host", () -> outer.heard.add("host")).withShortcut(f2));
            outer.test.show(outer.ui.button("Target").setAnchor(Anchor.TOP_LEFT));
            final Button ok = outer.ui.button("OK");
            final StackPanel content = outer.ui.stackPanel().add(ok);
            outer.ui.shortcuts().add(content, Command.of("Dialog", () -> outer.heard.add("dialog"))
                    .withShortcut(KeyStroke.of(Key.F3)));
            outer.ui.overlays().dialog(content);
            outer.press(Key.F2, Modifiers.NONE);
            outer.press(Key.F3, Modifiers.NONE);
            assertEquals(List.of("dialog"), outer.heard);
        }

        @Test
        void aFlyoutDoesNotBlockTheHostsShortcuts() {
            final CommandsTest outer = CommandsTest.this;
            outer.ui.shortcuts().add(Command.of("Host", () -> outer.heard.add("host"))
                    .withShortcut(KeyStroke.of(Key.F2)));
            final Button target = outer.ui.button("Target").setAnchor(Anchor.TOP_LEFT);
            outer.test.show(target);
            final Flyout flyout = outer.ui.flyout(outer.ui.button("Inside"));
            flyout.open(target.bounds());
            outer.test.host().layout();
            assertTrue(flyout.isOpen());
            outer.press(Key.F2, Modifiers.NONE);
            assertEquals(List.of("host"), outer.heard);
        }

        @Test
        void aShortcutRunsAsTheUserWithTheCommandOnTheChain() {
            final CommandsTest outer = CommandsTest.this;
            final List<Object> seen = new ArrayList<>();
            final Command.Action[] holder = new Command.Action[1];
            holder[0] = Command.of("Save", () -> {
                seen.add(outer.ui.causes().origin() instanceof Origin.User);
                seen.add(outer.ui.causes().context(CauseKeys.DEVICE).orElse(null));
                seen.add(outer.ui.causes().current().all().contains(holder[0]));
            }).withShortcut(KeyStroke.of(Key.F2));
            outer.ui.shortcuts().add(holder[0]);
            outer.test.show(outer.ui.button("Target"));
            outer.press(Key.F2, Modifiers.NONE);
            assertEquals(List.of(true, Device.KEYBOARD, true), seen);
        }

        @Test
        void aToggleShortcutFlipsTheState() {
            final CommandsTest outer = CommandsTest.this;
            final State<Boolean> grid = State.of(false);
            outer.ui.shortcuts().add(Command.toggle("Grid", grid).withShortcut(KeyStroke.of(Key.F7)));
            outer.test.show(outer.ui.button("Target"));
            outer.press(Key.F7, Modifiers.NONE);
            assertTrue(grid.get());
        }

        @Test
        void aMenuBarRunsTheShortcutsOfItsMenusWhileShown() {
            final CommandsTest outer = CommandsTest.this;
            final State<Boolean> grid = State.of(false);
            final Menu view = outer.ui.menu()
                    .addItem(outer.ui.checkMenuItem().setCommand(Command.toggle("Grid", grid)
                            .withShortcut(KeyStroke.shortcut(Key.G))));
            final Menu file = outer.ui.menu()
                    .addItem(outer.ui.menuItem().setCommand(Command.of("Open", () -> outer.heard.add("open"))
                            .withShortcut(KeyStroke.shortcut(Key.O))))
                    .addSubmenu(Text.of("More"), view);
            final MenuBar bar = outer.ui.menuBar().addMenu(Text.of("File"), file);
            final Button target = outer.ui.button("Target");
            outer.test.show(outer.ui.stackPanel().add(bar).add(target).setAnchor(Anchor.TOP_LEFT));
            target.requestFocus();
            assertTrue(outer.press(Key.O, CTRL));
            assertTrue(outer.press(Key.G, CTRL));
            assertTrue(grid.get());
            bar.setVisible(false);
            assertFalse(outer.press(Key.O, CTRL));
            assertEquals(List.of("open"), outer.heard);
        }

        @Test
        void aMenuOpenedFromCodeAddsNoShortcuts() {
            final CommandsTest outer = CommandsTest.this;
            final Menu menu = outer.ui.menu().addItem(outer.ui.menuItem().setCommand(
                    Command.of("Open", () -> outer.heard.add("open")).withShortcut(KeyStroke.of(Key.F4))));
            outer.test.show(outer.ui.button("Target").setAnchor(Anchor.TOP_LEFT));
            menu.openAt(10, 10);
            outer.press(Key.F4, Modifiers.NONE);
            assertEquals(List.of(), outer.heard);
        }
    }
}
