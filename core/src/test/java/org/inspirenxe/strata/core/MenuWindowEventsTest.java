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
import org.inspirenxe.strata.api.control.attribute.item.SingleSelection;
import org.inspirenxe.strata.api.control.attribute.popup.Closable;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.menu.CheckMenuItem;
import org.inspirenxe.strata.api.control.menu.Menu;
import org.inspirenxe.strata.api.control.menu.MenuBar;
import org.inspirenxe.strata.api.control.menu.MenuEntry;
import org.inspirenxe.strata.api.control.menu.MenuItem;
import org.inspirenxe.strata.api.control.window.MessageBox;
import org.inspirenxe.strata.api.control.window.Window;
import org.inspirenxe.strata.api.event.Subscription;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.layout.AbsolutePanel;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.overlay.Overlay;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The events of menus, menu bars, windows, message boxes and overlays. Text in the test platform is 6 units per code
 * point and 9 per line.
 */
class MenuWindowEventsTest {
    private final TestHost test = new TestHost(300, 300);
    private final ComponentFactory ui = this.test.factory();
    private final List<String> heard = new ArrayList<>();

    private boolean key(final Key key) {
        return this.test.host().key(key, KeyAction.PRESS, Modifiers.NONE);
    }

    private void move(final float x, final float y) {
        this.test.host().pointerMoved(x, y, Modifiers.NONE);
    }

    @Nested
    class Menus {
        private Menu menu;
        private Menu more;

        /**
         * Opens a menu at (10, 10): One, a separator, Two, Check, the More submenu, and the disabled Off. Items start
         * 2 units in; each is 15 tall and the separator 5.
         */
        private void show() {
            final MenuWindowEventsTest outer = MenuWindowEventsTest.this;
            final Button target = outer.ui.button("Target").setAnchor(Anchor.TOP_LEFT);
            outer.test.show(target);
            target.requestFocus();
            this.more = outer.ui.menu().addItem("Deep", () -> outer.heard.add("deep"));
            this.menu = outer.ui.menu()
                    .addItem("One", () -> outer.heard.add("one"))
                    .addSeparator()
                    .addItem("Two", () -> outer.heard.add("two"))
                    .addCheckItem("Check", false, checked -> outer.heard.add("check " + checked))
                    .addSubmenu("More", this.more)
                    .addItem("Off", () -> outer.heard.add("off"));
            ((MenuItem) this.menu.items().get(5)).setEnabled(false);
            this.menu.openAt(10, 10);
            outer.test.host().layout();
        }

        private int index(final MenuEntry entry) {
            return this.menu.items().indexOf(entry);
        }

        private CheckMenuItem check() {
            return (CheckMenuItem) this.menu.items().get(3);
        }

        private void clickTwo() {
            MenuWindowEventsTest.this.test.click(30, 10 + 2 + 15 + 5 + 7);
        }

        @Test
        void acceptingAChoiceClosesRunsTheActionAndReports() {
            this.show();
            final List<String> heard = MenuWindowEventsTest.this.heard;
            this.menu.onEvent(Menu.CHOOSE, e -> heard.add("choose " + this.index(e.value())))
                    .onEvent(Closable.CLOSED, e -> heard.add("closed"))
                    .onEvent(Menu.CHOSEN, e -> heard.add("chosen " + this.index(e.previous()) + " " + this.index(e.value())));
            this.clickTwo();
            assertEquals(List.of("choose 2", "closed", "two", "chosen 2 2"), heard);
            assertFalse(this.menu.isOpen());
        }

        @Test
        void acceptingAKeyFlipsACheckItem() {
            this.show();
            final List<Integer> chosen = new ArrayList<>();
            this.menu.onEvent(Menu.CHOSEN, e -> chosen.add(this.index(e.value())));
            MenuWindowEventsTest.this.key(Key.UP);
            MenuWindowEventsTest.this.key(Key.UP);
            MenuWindowEventsTest.this.key(Key.SPACE);
            assertEquals(List.of("check true"), MenuWindowEventsTest.this.heard);
            assertEquals(List.of(3), chosen);
            assertTrue(this.check().isSelected());
            assertFalse(this.menu.isOpen());
        }

        @Test
        void cancelingKeepsTheMenuOpenAndTheItemUnchanged() {
            this.show();
            final List<Integer> requested = new ArrayList<>();
            final List<Integer> chosen = new ArrayList<>();
            this.menu.onEvent(Menu.CHOOSE, e -> {
                requested.add(this.index(e.value()));
                e.cancel();
            }).onEvent(Menu.CHOSEN, e -> chosen.add(this.index(e.value())));
            MenuWindowEventsTest.this.key(Key.UP);
            MenuWindowEventsTest.this.key(Key.UP);
            assertTrue(MenuWindowEventsTest.this.key(Key.ENTER));
            MenuWindowEventsTest.this.test.click(30, 10 + 2 + 7);
            assertEquals(List.of(3, 0), requested);
            assertEquals(List.of(), chosen);
            assertEquals(List.of(), MenuWindowEventsTest.this.heard);
            assertFalse(this.check().isSelected());
            assertTrue(this.menu.isOpen());
        }

        @Test
        void aRewrittenEntryRunsThatItem() {
            this.show();
            final List<Integer> chosen = new ArrayList<>();
            this.menu.onEvent(Menu.CHOOSE, e -> e.setValue(this.menu.items().getFirst()))
                    .onEvent(Menu.CHOSEN, e -> chosen.add(this.index(e.value())));
            this.clickTwo();
            assertEquals(List.of("one"), MenuWindowEventsTest.this.heard);
            assertEquals(List.of(0), chosen);
            assertFalse(this.menu.isOpen());
        }

        @Test
        void aRewrittenEntryThatCannotBeChosenIsIgnored() {
            this.show();
            final MenuEntry[] rewrite = {null};
            final List<Integer> chosen = new ArrayList<>();
            this.menu.onEvent(Menu.CHOOSE, e -> e.setValue(rewrite[0]))
                    .onEvent(Menu.CHOSEN, e -> chosen.add(this.index(e.value())));
            // Null, another menu's item, a separator, a submenu item and a disabled item
            final List<MenuEntry> items = this.menu.items();
            for (final MenuEntry entry : java.util.Arrays.asList(null, this.more.items().getFirst(), items.get(1), items.get(4),
                    items.get(5))) {
                rewrite[0] = entry;
                this.clickTwo();
            }
            assertEquals(List.of(), MenuWindowEventsTest.this.heard);
            assertEquals(List.of(), chosen);
            assertTrue(this.menu.isOpen());
            assertFalse(this.more.isOpen());
        }

        @Test
        void aSubmenuItemOpensWithoutARequestAndTheSubmenuFiresItsOwn() {
            this.show();
            final List<String> heard = MenuWindowEventsTest.this.heard;
            this.menu.onEvent(Menu.CHOOSE, e -> heard.add("menu " + this.index(e.value())));
            this.more.onEvent(Menu.CHOOSE, e -> heard.add("more " + this.more.items().indexOf(e.value())))
                    .onEvent(Menu.CHOSEN, e -> heard.add("more chosen " + this.more.items().indexOf(e.value())));
            MenuWindowEventsTest.this.key(Key.UP);
            MenuWindowEventsTest.this.key(Key.ENTER);
            assertTrue(this.more.isOpen());
            MenuWindowEventsTest.this.key(Key.ENTER);
            assertEquals(List.of("more 0", "deep", "more chosen 0"), heard);
            assertFalse(this.menu.isOpen());
        }

        @Test
        void rightMovesIntoASubmenuThatHoveringOpened() {
            this.show();
            final MenuWindowEventsTest outer = MenuWindowEventsTest.this;
            // More is the fifth row: 2 in, then One, the separator, Two and Check
            outer.move(30, 10 + 2 + 15 + 5 + 15 + 15 + 7);
            assertTrue(this.more.isOpen());
            assertSame(this.menu, outer.test.focused(), "hovering keeps focus in the menu");
            assertTrue(outer.key(Key.RIGHT));
            assertSame(this.more, outer.test.focused());
            outer.key(Key.ENTER);
            assertEquals(List.of("deep"), outer.heard);
        }

        @Test
        void enterMovesIntoASubmenuThatHoveringOpened() {
            this.show();
            final MenuWindowEventsTest outer = MenuWindowEventsTest.this;
            outer.move(30, 10 + 2 + 15 + 5 + 15 + 15 + 7);
            outer.key(Key.ENTER);
            assertSame(this.more, outer.test.focused());
            outer.key(Key.ENTER);
            assertEquals(List.of("deep"), outer.heard);
        }

        @Test
        void codeFiresNoRequestButReportsClosing() {
            this.show();
            final List<String> heard = MenuWindowEventsTest.this.heard;
            this.menu.onEvent(Menu.CHOOSE, e -> heard.add("choose"))
                    .onEvent(Menu.CHOSEN, e -> heard.add("chosen"))
                    .onEvent(Closable.CLOSED, e -> heard.add("closed"));
            this.check().setSelected(true);
            this.menu.close();
            this.menu.close();
            assertEquals(List.of("closed"), heard);
            assertFalse(this.menu.isOpen());
        }

        @Test
        void closingByTheUserReportsEachMenuOfTheChain() {
            this.show();
            final List<String> heard = MenuWindowEventsTest.this.heard;
            this.menu.onEvent(Closable.CLOSED, e -> heard.add("menu"));
            this.more.onEvent(Closable.CLOSED, e -> heard.add("more"));
            MenuWindowEventsTest.this.key(Key.UP);
            MenuWindowEventsTest.this.key(Key.ENTER);
            MenuWindowEventsTest.this.key(Key.ESCAPE);
            assertEquals(List.of("more"), heard);
            MenuWindowEventsTest.this.key(Key.ESCAPE);
            assertEquals(List.of("more", "menu"), heard);
        }
    }

    @Nested
    class MenuBars {
        private Menu file;
        private Menu edit;
        private MenuBar bar;
        private AbsolutePanel panel;

        /**
         * Shows a bar with File and Edit at the top left of a panel. Titles are 40 units wide and the bar 15 tall.
         */
        private void show() {
            final MenuWindowEventsTest outer = MenuWindowEventsTest.this;
            this.file = outer.ui.menu().addItem("Quit", () -> { });
            this.edit = outer.ui.menu().addItem("Undo", () -> { });
            this.bar = outer.ui.menuBar().addMenu("File", this.file).addMenu("Edit", this.edit);
            this.panel = outer.ui.absolutePanel().add(this.bar, 0, 0);
            outer.test.show(this.panel);
        }

        private void record() {
            final List<String> heard = MenuWindowEventsTest.this.heard;
            this.bar.onEvent(SingleSelection.SELECT, e -> heard.add("open " + e.value()))
                    .onEvent(SingleSelection.SELECTED, e -> heard.add("opened " + e.previous() + " " + e.value()));
        }

        private void clickFile() {
            MenuWindowEventsTest.this.move(20, 7);
            MenuWindowEventsTest.this.test.click(20, 7);
        }

        @Test
        void acceptingOpensFromClicksMovesAndKeysAndReportsThePreviousMenu() {
            this.show();
            this.record();
            this.clickFile();
            assertTrue(this.file.isOpen());
            MenuWindowEventsTest.this.move(60, 7);
            assertTrue(this.edit.isOpen());
            assertFalse(this.file.isOpen());
            MenuWindowEventsTest.this.key(Key.RIGHT);
            assertTrue(this.file.isOpen());
            assertEquals(0, this.bar.selectedIndex());
            assertEquals(List.of("open 0", "opened -1 0", "open 1", "opened 0 1", "open 0", "opened 1 0"),
                    MenuWindowEventsTest.this.heard);
        }

        @Test
        void cancelingLeavesTheOpenMenuAsItIs() {
            this.show();
            this.clickFile();
            this.record();
            this.bar.onEvent(SingleSelection.SELECT, 1, e -> e.cancel());
            MenuWindowEventsTest.this.move(60, 7);
            MenuWindowEventsTest.this.key(Key.LEFT);
            assertEquals(List.of("open 1", "open 1"), MenuWindowEventsTest.this.heard);
            assertTrue(this.file.isOpen());
            assertFalse(this.edit.isOpen());
            assertEquals(0, this.bar.selectedIndex());
        }

        @Test
        void cancelingTheFirstOpenLeavesEverythingClosed() {
            this.show();
            this.bar.onEvent(SingleSelection.SELECT, e -> e.cancel());
            this.clickFile();
            assertFalse(this.file.isOpen());
            assertEquals(-1, this.bar.selectedIndex());
        }

        @Test
        void aRewrittenIndexOpensThatMenu() {
            this.show();
            this.record();
            this.bar.onEvent(SingleSelection.SELECT, -1, e -> e.setValue(1));
            this.clickFile();
            assertTrue(this.edit.isOpen());
            assertFalse(this.file.isOpen());
            assertEquals(List.of("open 1", "opened -1 1"), MenuWindowEventsTest.this.heard);
        }

        @Test
        void aRewrittenIndexOutOfRangeOrAlreadyOpenIsIgnored() {
            this.show();
            this.clickFile();
            final int[] rewrite = {5};
            this.record();
            this.bar.onEvent(SingleSelection.SELECT, -1, e -> e.setValue(rewrite[0]));
            MenuWindowEventsTest.this.move(60, 7);
            rewrite[0] = 0;
            MenuWindowEventsTest.this.key(Key.RIGHT);
            assertEquals(List.of("open 5", "open 0"), MenuWindowEventsTest.this.heard);
            assertTrue(this.file.isOpen());
            assertEquals(0, this.bar.selectedIndex());
        }

        @Test
        void codeAndClosingByTitleFireNoRequest() {
            this.show();
            this.record();
            this.edit.open(new Rect(40, 0, 40, 15));
            assertTrue(this.edit.isOpen());
            this.edit.close();
            this.clickFile();
            MenuWindowEventsTest.this.test.click(20, 7);
            assertFalse(this.file.isOpen());
            assertEquals(-1, this.bar.selectedIndex());
            assertEquals(List.of("open 0", "opened -1 0", "opened 0 -1"), MenuWindowEventsTest.this.heard);
        }

        @Test
        void anAncestorHearsAndCanRefuseTheRequest() {
            this.show();
            final List<Component<?>> targets = new ArrayList<>();
            this.panel.onEvent(SingleSelection.SELECT, e -> {
                targets.add(e.target());
                e.cancel();
            });
            this.clickFile();
            assertEquals(List.of(this.bar), targets);
            assertFalse(this.file.isOpen());
        }

        @Test
        void anAncestorHearsTheReport() {
            this.show();
            final List<Integer> opened = new ArrayList<>();
            this.panel.onEvent(SingleSelection.SELECTED, e -> opened.add(e.value()));
            this.clickFile();
            assertEquals(List.of(0), opened);
        }
    }

    @Nested
    class Windows {
        private Window window(final String title) {
            final List<String> heard = MenuWindowEventsTest.this.heard;
            return MenuWindowEventsTest.this.ui.window(title)
                    .onEvent(Closable.CLOSE, e -> heard.add("close"))
                    .onEvent(Closable.CLOSED, e -> heard.add("closed"));
        }

        private Overlay dialog(final Window window) {
            MenuWindowEventsTest.this.test.show(MenuWindowEventsTest.this.ui.absolutePanel());
            window.add(MenuWindowEventsTest.this.ui.button("OK"));
            return MenuWindowEventsTest.this.ui.overlays().dialog(window);
        }

        private AbsolutePanel inPanel(final Window window) {
            window.setSize(100, 60);
            final AbsolutePanel panel = MenuWindowEventsTest.this.ui.absolutePanel().add(window, 0, 0);
            MenuWindowEventsTest.this.test.show(panel);
            return panel;
        }

        private void clickClose(final Window window) {
            final Rect bounds = window.bounds();
            MenuWindowEventsTest.this.test.click(bounds.right() - 7, bounds.y() + 5);
        }

        @Test
        void acceptingTheCloseButtonCloses() {
            final Window window = this.window("Title");
            this.inPanel(window);
            this.clickClose(window);
            assertEquals(List.of("close", "closed"), MenuWindowEventsTest.this.heard);
        }

        @Test
        void theCloseButtonIsNotAComponent() {
            final Window window = MenuWindowEventsTest.this.ui.window("Title");
            final Button ok = MenuWindowEventsTest.this.ui.button("OK");
            window.add(ok);
            this.inPanel(window);
            assertEquals(List.of(ok), window.children());
        }

        @Test
        void cancelingTheCloseButtonKeepsADialogOpen() {
            final Window window = this.window("Title").onEvent(Closable.CLOSE, e -> e.cancel());
            final Overlay overlay = this.dialog(window);
            this.clickClose(window);
            assertEquals(List.of("close"), MenuWindowEventsTest.this.heard);
            assertTrue(overlay.isOpen());
        }

        @Test
        void acceptingEscapeClosesADialog() {
            final Overlay overlay = this.dialog(this.window("Title"));
            assertTrue(MenuWindowEventsTest.this.key(Key.ESCAPE));
            assertEquals(List.of("close", "closed"), MenuWindowEventsTest.this.heard);
            assertFalse(overlay.isOpen());
        }

        @Test
        void cancelingEscapeKeepsADialogOpenAndUsesThePress() {
            final Overlay overlay = this.dialog(this.window("Title").onEvent(Closable.CLOSE, e -> e.cancel()));
            assertTrue(MenuWindowEventsTest.this.key(Key.ESCAPE));
            assertEquals(List.of("close"), MenuWindowEventsTest.this.heard);
            assertTrue(overlay.isOpen());
        }

        @Test
        void codeFiresNoRequestButReports() {
            final Window window = this.window("Title");
            final Overlay overlay = this.dialog(window);
            window.close();
            assertEquals(List.of("closed"), MenuWindowEventsTest.this.heard);
            assertFalse(overlay.isOpen());
        }

        @Test
        void anAncestorHearsAndCanRefuseTheRequestAndHearsTheReport() {
            final Window window = MenuWindowEventsTest.this.ui.window("Title");
            final AbsolutePanel panel = this.inPanel(window);
            final List<String> heard = MenuWindowEventsTest.this.heard;
            final boolean[] refuse = {true};
            panel.onEvent(Closable.CLOSE, e -> {
                heard.add("close");
                if (refuse[0]) {
                    e.cancel();
                }
            }).onEvent(Closable.CLOSED, e -> heard.add("closed " + (e.control() == window)));
            this.clickClose(window);
            refuse[0] = false;
            this.clickClose(window);
            assertEquals(List.of("close", "close", "closed true"), heard);
        }

        @Test
        void escapeOnAScreensWindowGoesToTheEngineUnlessSomeoneListens() {
            final Window window = MenuWindowEventsTest.this.ui.window("Title");
            final Button ok = MenuWindowEventsTest.this.ui.button("OK");
            window.add(ok);
            final AbsolutePanel panel = this.inPanel(window);
            ok.requestFocus();
            assertFalse(MenuWindowEventsTest.this.key(Key.ESCAPE));
            panel.onEvent(Closable.CLOSED, e -> MenuWindowEventsTest.this.heard.add("closed"));
            assertTrue(MenuWindowEventsTest.this.key(Key.ESCAPE));
            assertEquals(List.of("closed"), MenuWindowEventsTest.this.heard);
        }
    }

    @Nested
    class MessageBoxes {
        private final List<MessageBox.Result> results = new ArrayList<>();
        private MessageBox box;
        private Overlay overlay;

        private void show() {
            final MenuWindowEventsTest outer = MenuWindowEventsTest.this;
            outer.test.show(outer.ui.absolutePanel());
            this.box = outer.ui.messageBox("Save", "Save?", MessageBox.Buttons.YES_NO_CANCEL)
                    .onEvent(MessageBox.ANSWER, e -> outer.heard.add("answer " + e.value()))
                    .onEvent(MessageBox.ANSWERED, e -> {
                        assertSame(e.value(), e.previous());
                        this.results.add(e.value());
                    });
            this.box.window().onEvent(Closable.CLOSED, e -> outer.heard.add("window closed"));
            this.overlay = outer.ui.overlays().dialog(this.box);
        }

        private void clickClose() {
            final Rect bounds = this.box.window().bounds();
            MenuWindowEventsTest.this.test.click(bounds.right() - 7, bounds.y() + 5);
        }

        @Test
        void acceptingAButtonReportsAndCloses() {
            this.show();
            // The first button is focused
            MenuWindowEventsTest.this.key(Key.ENTER);
            assertEquals(List.of("answer YES"), MenuWindowEventsTest.this.heard);
            assertEquals(List.of(MessageBox.Result.YES), this.results);
            assertFalse(this.overlay.isOpen());
        }

        @Test
        void cancelingAButtonKeepsTheBoxOpen() {
            this.show();
            final Subscription refuse = this.box.subscribe(MessageBox.ANSWER, 1, e -> e.cancel());
            MenuWindowEventsTest.this.key(Key.ENTER);
            assertEquals(List.of("answer YES"), MenuWindowEventsTest.this.heard);
            assertEquals(List.of(), this.results);
            assertTrue(this.overlay.isOpen());
            refuse.close();
            MenuWindowEventsTest.this.key(Key.ENTER);
            assertEquals(List.of(MessageBox.Result.YES), this.results);
            assertFalse(this.overlay.isOpen());
        }

        @Test
        void aRewrittenAnswerIsReportedEvenWithoutAButtonForIt() {
            this.show();
            this.box.onEvent(MessageBox.ANSWER, -1, e -> e.setValue(MessageBox.Result.CLOSE));
            MenuWindowEventsTest.this.key(Key.ENTER);
            assertEquals(List.of("answer CLOSE"), MenuWindowEventsTest.this.heard);
            assertEquals(List.of(MessageBox.Result.CLOSE), this.results);
            assertFalse(this.overlay.isOpen());
        }

        @Test
        void escapeAndTheCloseButtonAnswerTheDismissResult() {
            this.show();
            final Subscription refuse = this.box.subscribe(MessageBox.ANSWER, 1, e -> e.cancel());
            assertTrue(MenuWindowEventsTest.this.key(Key.ESCAPE));
            this.clickClose();
            assertEquals(List.of("answer CANCEL", "answer CANCEL"), MenuWindowEventsTest.this.heard);
            assertEquals(List.of(), this.results);
            assertTrue(this.overlay.isOpen());
            refuse.close();
            MenuWindowEventsTest.this.key(Key.ESCAPE);
            assertEquals(List.of("answer CANCEL", "answer CANCEL", "answer CANCEL", "window closed"),
                    MenuWindowEventsTest.this.heard);
            assertEquals(List.of(MessageBox.Result.CANCEL), this.results);
            assertFalse(this.overlay.isOpen());
        }

        @Test
        void aRewrittenDismissIsReported() {
            this.show();
            this.box.onEvent(MessageBox.ANSWER, -1, e -> e.setValue(MessageBox.Result.NO));
            this.clickClose();
            assertEquals(List.of(MessageBox.Result.NO), this.results);
            assertFalse(this.overlay.isOpen());
        }

        @Test
        void cancelingTheWindowsOwnRequestStopsTheDismissBeforeTheAnswer() {
            this.show();
            this.box.window().onEvent(Closable.CLOSE, e -> e.cancel());
            this.clickClose();
            assertEquals(List.of(), MenuWindowEventsTest.this.heard);
            assertTrue(this.overlay.isOpen());
        }

        @Test
        void escapeWithNothingFocusedStillAnswers() {
            final MenuWindowEventsTest outer = MenuWindowEventsTest.this;
            outer.test.show(outer.ui.absolutePanel());
            final MessageBox plain = outer.ui.messageBox("Save", "Save?", MessageBox.Buttons.YES_NO)
                    .onEvent(MessageBox.ANSWERED, e -> this.results.add(e.value()));
            final Overlay shown = outer.ui.overlays().dialog(plain);
            // A press on the title bar clears focus, so Escape goes to the dialog's content
            final Rect bounds = plain.window().bounds();
            outer.test.click(bounds.x() + 5, bounds.y() + 5);
            assertNull(outer.test.focused());
            assertTrue(outer.key(Key.ESCAPE));
            assertEquals(List.of(MessageBox.Result.NO), this.results);
            assertFalse(shown.isOpen());
        }

        @Test
        void closingTheWindowFromCodeFiresNoRequestButReportsTheDismissResult() {
            this.show();
            this.box.window().close();
            assertEquals(List.of("window closed"), MenuWindowEventsTest.this.heard);
            assertEquals(List.of(MessageBox.Result.CANCEL), this.results);
            assertFalse(this.overlay.isOpen());
        }

        @Test
        void anAncestorHearsTheBoxButNotItsPrivateWindow() {
            final MenuWindowEventsTest outer = MenuWindowEventsTest.this;
            final MessageBox embedded = outer.ui.messageBox("Save", "Save?", MessageBox.Buttons.OK_CANCEL);
            final AbsolutePanel panel = outer.ui.absolutePanel().add(embedded, 0, 0);
            outer.test.show(panel);
            final List<String> outside = new ArrayList<>();
            panel.onEvent(MessageBox.ANSWER, e -> outside.add("answer " + e.value() + " " + (e.target() == embedded)))
                    .onEvent(MessageBox.ANSWERED, e -> outside.add("answered " + e.value()))
                    .onEvent(Closable.CLOSE, e -> outside.add("window close"))
                    .onEvent(Closable.CLOSED, e -> outside.add("window closed"));
            embedded.onEvent(Closable.CLOSED, e -> outer.heard.add("box heard window closed"));

            final Rect bounds = embedded.window().bounds();
            outer.test.click(bounds.right() - 7, bounds.y() + 5);

            assertEquals(List.of("answer CANCEL true", "answered CANCEL"), outside);
            assertEquals(List.of("box heard window closed"), outer.heard);
        }
    }

    @Nested
    class Overlays {
        private final List<String> closed = new ArrayList<>();

        private Overlay popup(final String name) {
            final Box content = new Box(MenuWindowEventsTest.this.ui, 20, 20);
            final Overlay overlay = MenuWindowEventsTest.this.ui.overlays().popup(content, Rect.ZERO);
            return overlay.onEvent(Closable.CLOSED, e -> {
                assertSame(overlay, e.control());
                assertSame(content, e.target());
                assertFalse(e.target().isAttached());
                this.closed.add(name);
            });
        }

        @Test
        void closingFromCodeReportsOnce() {
            MenuWindowEventsTest.this.test.show(MenuWindowEventsTest.this.ui.absolutePanel());
            final Overlay overlay = this.popup("popup");
            overlay.close();
            overlay.close();
            assertEquals(List.of("popup"), this.closed);
        }

        @Test
        void escapeAndAPressOutsideReport() {
            MenuWindowEventsTest.this.test.show(MenuWindowEventsTest.this.ui.absolutePanel());
            this.popup("first");
            this.popup("second");
            assertTrue(MenuWindowEventsTest.this.key(Key.ESCAPE));
            assertEquals(List.of("second"), this.closed);
            MenuWindowEventsTest.this.test.click(200, 200);
            assertEquals(List.of("second", "first"), this.closed);
        }

        @Test
        void handlersRunByPriorityAndCanBeRemoved() {
            MenuWindowEventsTest.this.test.show(MenuWindowEventsTest.this.ui.absolutePanel());
            final Overlay overlay = this.popup("default");
            final Subscription removed = overlay.subscribe(Closable.CLOSED, 0, e -> this.closed.add("removed"));
            overlay.onEvent(Closable.CLOSED, -1, e -> this.closed.add("early"));
            removed.close();
            overlay.close();
            assertEquals(List.of("early", "default"), this.closed);
        }

        @Test
        void aHandlerAddedAfterClosingNeverRuns() {
            MenuWindowEventsTest.this.test.show(MenuWindowEventsTest.this.ui.absolutePanel());
            final Overlay overlay = this.popup("popup");
            overlay.close();
            overlay.onEvent(Closable.CLOSED, e -> this.closed.add("late"));
            overlay.close();
            assertEquals(List.of("popup"), this.closed);
        }
    }
}
