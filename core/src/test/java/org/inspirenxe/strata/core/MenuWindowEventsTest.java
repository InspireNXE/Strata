package org.inspirenxe.strata.core;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.menu.Menu;
import org.inspirenxe.strata.api.control.menu.MenuBar;
import org.inspirenxe.strata.api.control.window.MessageBox;
import org.inspirenxe.strata.api.control.window.Window;
import org.inspirenxe.strata.api.event.Subscription;
import org.inspirenxe.strata.api.geom.Rect;
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
import java.util.Optional;

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
                    .addItem("Off", () -> outer.heard.add("off"))
                    .setItemEnabled(5, false);
            this.menu.openAt(10, 10);
            outer.test.host().layout();
        }

        private void clickTwo() {
            MenuWindowEventsTest.this.test.click(30, 10 + 2 + 15 + 5 + 7);
        }

        @Test
        void acceptingAChoiceClosesRunsTheActionAndReports() {
            this.show();
            final List<String> heard = MenuWindowEventsTest.this.heard;
            this.menu.onEvent(Menu.CHOOSE, e -> heard.add("choose " + e.value()))
                    .onEvent(Menu.CLOSED, e -> heard.add("closed"))
                    .onEvent(Menu.CHOSEN, e -> heard.add("chosen " + e.previous() + " " + e.value()));
            this.clickTwo();
            assertEquals(List.of("choose 2", "closed", "two", "chosen 2 2"), heard);
            assertFalse(this.menu.isOpen());
        }

        @Test
        void acceptingAKeyFlipsACheckItem() {
            this.show();
            final List<Integer> chosen = new ArrayList<>();
            this.menu.onEvent(Menu.CHOSEN, e -> chosen.add(e.value()));
            MenuWindowEventsTest.this.key(Key.UP);
            MenuWindowEventsTest.this.key(Key.UP);
            MenuWindowEventsTest.this.key(Key.SPACE);
            assertEquals(List.of("check true"), MenuWindowEventsTest.this.heard);
            assertEquals(List.of(3), chosen);
            assertTrue(this.menu.isItemChecked(3));
            assertFalse(this.menu.isOpen());
        }

        @Test
        void cancelingKeepsTheMenuOpenAndTheItemUnchanged() {
            this.show();
            final List<Integer> requested = new ArrayList<>();
            final List<Integer> chosen = new ArrayList<>();
            this.menu.onEvent(Menu.CHOOSE, e -> {
                requested.add(e.value());
                e.cancel();
            }).onEvent(Menu.CHOSEN, e -> chosen.add(e.value()));
            MenuWindowEventsTest.this.key(Key.UP);
            MenuWindowEventsTest.this.key(Key.UP);
            assertTrue(MenuWindowEventsTest.this.key(Key.ENTER));
            MenuWindowEventsTest.this.test.click(30, 10 + 2 + 7);
            assertEquals(List.of(3, 0), requested);
            assertEquals(List.of(), chosen);
            assertEquals(List.of(), MenuWindowEventsTest.this.heard);
            assertFalse(this.menu.isItemChecked(3));
            assertTrue(this.menu.isOpen());
        }

        @Test
        void aRewrittenIndexRunsThatItem() {
            this.show();
            final List<Integer> chosen = new ArrayList<>();
            this.menu.onEvent(Menu.CHOOSE, e -> e.setValue(0)).onEvent(Menu.CHOSEN, e -> chosen.add(e.value()));
            this.clickTwo();
            assertEquals(List.of("one"), MenuWindowEventsTest.this.heard);
            assertEquals(List.of(0), chosen);
            assertFalse(this.menu.isOpen());
        }

        @Test
        void aRewrittenIndexThatCannotBeChosenIsIgnored() {
            this.show();
            final int[] rewrite = {0};
            final List<Integer> chosen = new ArrayList<>();
            this.menu.onEvent(Menu.CHOOSE, e -> e.setValue(rewrite[0])).onEvent(Menu.CHOSEN, e -> chosen.add(e.value()));
            // Out of range, a separator, a submenu item and a disabled item
            for (final int index : new int[] {-1, 99, 1, 4, 5}) {
                rewrite[0] = index;
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
            this.menu.onEvent(Menu.CHOOSE, e -> heard.add("menu " + e.value()));
            this.more.onEvent(Menu.CHOOSE, e -> heard.add("more " + e.value()))
                    .onEvent(Menu.CHOSEN, e -> heard.add("more chosen " + e.value()));
            MenuWindowEventsTest.this.key(Key.UP);
            MenuWindowEventsTest.this.key(Key.ENTER);
            assertTrue(this.more.isOpen());
            MenuWindowEventsTest.this.key(Key.ENTER);
            assertEquals(List.of("more 0", "deep", "more chosen 0"), heard);
            assertFalse(this.menu.isOpen());
        }

        @Test
        void codeFiresNoRequestButReportsClosing() {
            this.show();
            final List<String> heard = MenuWindowEventsTest.this.heard;
            this.menu.onEvent(Menu.CHOOSE, e -> heard.add("choose"))
                    .onEvent(Menu.CHOSEN, e -> heard.add("chosen"))
                    .onEvent(Menu.CLOSED, e -> heard.add("closed"));
            this.menu.setItemChecked(3, true);
            this.menu.close();
            this.menu.close();
            assertEquals(List.of("closed"), heard);
            assertFalse(this.menu.isOpen());
        }

        @Test
        void closingByTheUserReportsEachMenuOfTheChain() {
            this.show();
            final List<String> heard = MenuWindowEventsTest.this.heard;
            this.menu.onEvent(Menu.CLOSED, e -> heard.add("menu"));
            this.more.onEvent(Menu.CLOSED, e -> heard.add("more"));
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
            this.bar.onEvent(MenuBar.OPEN, e -> heard.add("open " + e.value()))
                    .onEvent(MenuBar.OPENED, e -> heard.add("opened " + e.previous() + " " + e.value()));
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
            assertEquals(Optional.of(0), this.bar.openIndex());
            assertEquals(List.of("open 0", "opened -1 0", "open 1", "opened 0 1", "open 0", "opened 1 0"),
                    MenuWindowEventsTest.this.heard);
        }

        @Test
        void cancelingLeavesTheOpenMenuAsItIs() {
            this.show();
            this.clickFile();
            this.record();
            this.bar.onEvent(MenuBar.OPEN, 1, e -> e.cancel());
            MenuWindowEventsTest.this.move(60, 7);
            MenuWindowEventsTest.this.key(Key.LEFT);
            assertEquals(List.of("open 1", "open 1"), MenuWindowEventsTest.this.heard);
            assertTrue(this.file.isOpen());
            assertFalse(this.edit.isOpen());
            assertEquals(Optional.of(0), this.bar.openIndex());
        }

        @Test
        void cancelingTheFirstOpenLeavesEverythingClosed() {
            this.show();
            this.bar.onEvent(MenuBar.OPEN, e -> e.cancel());
            this.clickFile();
            assertFalse(this.file.isOpen());
            assertEquals(Optional.empty(), this.bar.openIndex());
        }

        @Test
        void aRewrittenIndexOpensThatMenu() {
            this.show();
            this.record();
            this.bar.onEvent(MenuBar.OPEN, -1, e -> e.setValue(1));
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
            this.bar.onEvent(MenuBar.OPEN, -1, e -> e.setValue(rewrite[0]));
            MenuWindowEventsTest.this.move(60, 7);
            rewrite[0] = 0;
            MenuWindowEventsTest.this.key(Key.RIGHT);
            assertEquals(List.of("open 5", "open 0"), MenuWindowEventsTest.this.heard);
            assertTrue(this.file.isOpen());
            assertEquals(Optional.of(0), this.bar.openIndex());
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
            assertEquals(Optional.empty(), this.bar.openIndex());
            assertEquals(List.of("open 0", "opened -1 0"), MenuWindowEventsTest.this.heard);
        }

        @Test
        void anAncestorHearsAndCanRefuseTheRequest() {
            this.show();
            final List<Component<?>> targets = new ArrayList<>();
            this.panel.onEvent(MenuBar.OPEN, e -> {
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
            this.panel.onEvent(MenuBar.OPENED, e -> opened.add(e.value()));
            this.clickFile();
            assertEquals(List.of(0), opened);
        }
    }

    @Nested
    class Windows {
        private Window window(final String title) {
            final List<String> heard = MenuWindowEventsTest.this.heard;
            return MenuWindowEventsTest.this.ui.window(title)
                    .onEvent(Window.CLOSE, e -> heard.add("close"))
                    .onEvent(Window.CLOSED, e -> heard.add("closed"));
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
            final Window window = this.window("Title").onEvent(Window.CLOSE, e -> e.cancel());
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
            final Overlay overlay = this.dialog(this.window("Title").onEvent(Window.CLOSE, e -> e.cancel()));
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
            panel.onEvent(Window.CLOSE, e -> {
                heard.add("close");
                if (refuse[0]) {
                    e.cancel();
                }
            }).onEvent(Window.CLOSED, e -> heard.add("closed " + (e.control() == window)));
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
            panel.onEvent(Window.CLOSED, e -> MenuWindowEventsTest.this.heard.add("closed"));
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
            this.box.window().onEvent(Window.CLOSED, e -> outer.heard.add("window closed"));
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
            this.box.window().onEvent(Window.CLOSE, e -> e.cancel());
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
                    .onEvent(Window.CLOSE, e -> outside.add("window close"))
                    .onEvent(Window.CLOSED, e -> outside.add("window closed"));
            embedded.onEvent(Window.CLOSED, e -> outer.heard.add("box heard window closed"));

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
            return overlay.onEvent(Overlay.CLOSED, e -> {
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
            final Subscription removed = overlay.subscribe(Overlay.CLOSED, 0, e -> this.closed.add("removed"));
            overlay.onEvent(Overlay.CLOSED, -1, e -> this.closed.add("early"));
            removed.close();
            overlay.close();
            assertEquals(List.of("early", "default"), this.closed);
        }

        @Test
        void aHandlerAddedAfterClosingNeverRuns() {
            MenuWindowEventsTest.this.test.show(MenuWindowEventsTest.this.ui.absolutePanel());
            final Overlay overlay = this.popup("popup");
            overlay.close();
            overlay.onEvent(Overlay.CLOSED, e -> this.closed.add("late"));
            overlay.close();
            assertEquals(List.of("popup"), this.closed);
        }
    }
}
