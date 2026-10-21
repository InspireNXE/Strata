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
import org.inspirenxe.strata.api.component.HasTooltip;
import org.inspirenxe.strata.api.component.Tooltip;
import org.inspirenxe.strata.api.control.attribute.item.Activatable;
import org.inspirenxe.strata.api.control.attribute.item.Selection;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.collection.ComboBox;
import org.inspirenxe.strata.api.control.collection.ListView;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.control.menu.Menu;
import org.inspirenxe.strata.api.control.pane.NavigationHeader;
import org.inspirenxe.strata.api.control.pane.NavigationItem;
import org.inspirenxe.strata.api.control.pane.NavigationView;
import org.inspirenxe.strata.api.control.pane.NavigationView.DisplayMode;
import org.inspirenxe.strata.api.control.pane.NavigationView.Expansion;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.inspirenxe.strata.api.layout.VerticalAlignment;
import org.inspirenxe.strata.api.overlay.Overlay;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.ThemeException;
import org.inspirenxe.strata.api.theme.Themes;
import org.inspirenxe.strata.core.control.pane.NavigationViewImpl;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The navigation view: its entries and their indexes, picking, pages, expanding items, the display modes and the pane
 * over the content, the menu of children, the keyboard, its theme keys and its state.
 *
 * <p>Text in the test platform is 6 units per code point and 9 per line, so an item is 19 tall with the default
 * padding of 6 and 5. The pane's padding is 2, so the first row of a view with no title starts at (2, 2) and is 116
 * wide in the default pane of 120. The column of icons is 23 wide, and the bar of the minimal mode 23 tall.</p>
 */
class NavigationViewTest {
    private static final int SELECTED = 0xFF3A4D6E;
    private static final int INDICATOR = 0xFF6A9FFF;

    private final TestHost test = new TestHost(400, 300);
    private final ComponentFactory ui = this.test.factory();
    private final List<String> heard = new ArrayList<>();
    private NavigationView view;

    /**
     * Shows a view with General, Sound and Video, then a separator, a header and Account with Profile and Privacy as
     * children, and Sign out in the footer. Every event is logged.
     */
    private NavigationView show() {
        this.view = this.ui.navigationView()
                .addItem(this.ui.navigationItem("General"))
                .addItem(this.ui.navigationItem("Sound"))
                .addItem(this.ui.navigationItem("Video"))
                .addSeparator()
                .addHeader("People")
                .addItem(this.ui.navigationItem("Account")
                        .addItem(this.ui.navigationItem("Profile"))
                        .addItem(this.ui.navigationItem("Privacy")))
                .addFooterItem(this.ui.navigationItem("Sign out").setSelectable(false));
        this.log(this.view);
        this.test.show(this.view);
        return this.view;
    }

    private void log(final NavigationView target) {
        target.onEvent(Activatable.ACTIVATE, event -> this.heard.add("activate? " + event.value()))
                .onEvent(Activatable.ACTIVATED, event -> this.heard.add("activated " + event.value()))
                .onEvent(Selection.SELECT, event -> this.heard.add("select? " + event.value()))
                .onEvent(Selection.SELECTED, event -> this.heard.add("selected " + event.value()))
                .onEvent(NavigationView.EXPAND, event -> this.heard.add("expand? " + event.value()))
                .onEvent(NavigationView.EXPANDED, event -> this.heard.add("expanded " + event.value()))
                .onEvent(NavigationView.EXPAND_ITEM, event -> this.heard.add("expand item? "
                        + event.value().item().text().plain() + " " + event.value().expanded()))
                .onEvent(NavigationView.ITEM_EXPANDED, event -> this.heard.add("item expanded "
                        + event.value().item().text().plain() + " " + event.value().expanded()));
    }

    private NavigationItem item(final String text) {
        return this.view.allItems().stream().filter(item -> item.text().plain().equals(text)).findFirst().orElseThrow();
    }

    /**
     * Clicks the middle of the text of a row, given its top in the default layout with no title: rows are 19 tall
     * from y 2.
     */
    private void clickRow(final int row) {
        this.test.click(40, 2 + row * 19 + 9);
    }

    private void layout() {
        this.test.host().layout();
    }

    private DrawLog render() {
        final DrawLog canvas = new DrawLog();
        this.test.host().render(canvas);
        return canvas;
    }

    private boolean key(final Key key) {
        return this.test.host().key(key, KeyAction.PRESS, Modifiers.NONE);
    }

    private List<Overlay> overlays() {
        return this.ui.overlays().open();
    }

    @Nested
    class Entries {
        @Test
        void countItemsDepthFirstThenTheFooter() {
            NavigationViewTest.this.show();
            assertEquals(List.of("General", "Sound", "Video", "Account", "Profile", "Privacy", "Sign out"),
                    NavigationViewTest.this.view.allItems().stream().map(item -> item.text().plain()).toList());
            assertEquals(6, NavigationViewTest.this.view.items().size());
            assertTrue(NavigationViewTest.this.view.items().get(4) instanceof NavigationHeader);
            assertEquals(1, NavigationViewTest.this.view.footerItems().size());
        }

        @Test
        void belongToOneViewOrItemAtATime() {
            NavigationViewTest.this.show();
            final NavigationItem general = NavigationViewTest.this.item("General");
            final NavigationView other = NavigationViewTest.this.ui.navigationView();
            assertThrows(IllegalArgumentException.class, () -> other.addItem(general));
            assertThrows(IllegalArgumentException.class, () -> other.addFooterItem(general));
            final NavigationItem account = NavigationViewTest.this.item("Account");
            assertThrows(IllegalArgumentException.class, () -> account.addItem(general));
            assertThrows(IllegalArgumentException.class, () -> account.addItem(account));
            final NavigationItem profile = NavigationViewTest.this.item("Profile");
            final NavigationItem loose = NavigationViewTest.this.ui.navigationItem("Loose");
            loose.addItem(NavigationViewTest.this.ui.navigationItem("Inner"));
            assertThrows(IllegalArgumentException.class, () -> profile.addItem(account));
            // An item that Strata did not make
            final NavigationItem foreign = (NavigationItem) Proxy.newProxyInstance(
                    NavigationViewTest.class.getClassLoader(), new Class<?>[] {NavigationItem.class},
                    (proxy, method, args) -> null);
            assertThrows(IllegalArgumentException.class, () -> other.addItem(foreign));
            assertThrows(IllegalArgumentException.class, () -> loose.addItem(foreign));
            assertThrows(IndexOutOfBoundsException.class, () -> other.addItem(5, loose));
        }

        @Test
        void addAtAPlaceAndRemove() {
            NavigationViewTest.this.show();
            final NavigationItem news = NavigationViewTest.this.ui.navigationItem("News");
            NavigationViewTest.this.view.addItem(0, news);
            assertEquals("News", NavigationViewTest.this.view.allItems().getFirst().text().plain());
            NavigationViewTest.this.view.removeItem(news);
            assertEquals("General", NavigationViewTest.this.view.allItems().getFirst().text().plain());
            // A removed entry can join another view
            NavigationViewTest.this.ui.navigationView().addItem(news);
            NavigationViewTest.this.view.clearItems();
            assertEquals(List.of(), NavigationViewTest.this.view.items());
            assertEquals(1, NavigationViewTest.this.view.allItems().size());
            NavigationViewTest.this.view.removeFooterItem(NavigationViewTest.this.item("Sign out"));
            assertEquals(List.of(), NavigationViewTest.this.view.allItems());
        }

        @Test
        void reportAShiftedOrRemovedSelection() {
            NavigationViewTest.this.show().select(1);
            NavigationViewTest.this.heard.clear();
            NavigationViewTest.this.view.addItem(0, NavigationViewTest.this.ui.navigationItem("News"));
            assertEquals(List.of("selected 2"), NavigationViewTest.this.heard);
            NavigationViewTest.this.view.removeItem(NavigationViewTest.this.item("Sound"));
            assertEquals(List.of("selected 2", "selected -1"), NavigationViewTest.this.heard);
            assertEquals(Optional.empty(), NavigationViewTest.this.view.selected());
        }

        @Test
        void redrawWhenTheyChange() {
            NavigationViewTest.this.show();
            NavigationViewTest.this.item("Sound").setText("Audio").setBadge(3);
            NavigationViewTest.this.layout();
            final List<String> lines = NavigationViewTest.this.render().lines("text");
            assertTrue(lines.contains("text Audio 21,26 #FFE0E0E0"));
            assertTrue(lines.contains("text 3 104,26 #FFFFFFFF"));
            assertThrows(IllegalArgumentException.class, () -> NavigationViewTest.this.item("Video").setBadge(-1));
        }
    }

    @Nested
    class Picking {
        @Test
        void aClickActivatesThenSelects() {
            NavigationViewTest.this.show();
            NavigationViewTest.this.clickRow(1);
            assertEquals(List.of("activate? 1", "select? 1", "selected 1", "activated 1"), NavigationViewTest.this.heard);
            assertEquals(1, NavigationViewTest.this.view.selectedIndex());
            assertSame(NavigationViewTest.this.item("Sound"), NavigationViewTest.this.view.selected().orElseThrow());
            // Clicking the selected item activates it again and requests no selection
            NavigationViewTest.this.heard.clear();
            NavigationViewTest.this.clickRow(1);
            assertEquals(List.of("activate? 1", "activated 1"), NavigationViewTest.this.heard);
        }

        @Test
        void theKeyboardRingFollowsAClick() {
            NavigationViewTest.this.show();
            NavigationViewTest.this.clickRow(1);
            final NavigationViewImpl impl = (NavigationViewImpl) NavigationViewTest.this.view;
            assertSame(NavigationViewTest.this.item("Sound"), impl.cursorItem());
            NavigationViewTest.this.clickRow(2);
            assertSame(NavigationViewTest.this.item("Video"), impl.cursorItem());
            NavigationViewTest.this.key(Key.UP);
            assertSame(NavigationViewTest.this.item("Sound"), impl.cursorItem());
        }

        @Test
        void anItemThatIsNotSelectableOnlyActivates() {
            NavigationViewTest.this.show();
            // Sign out sits in the footer at the bottom of the 300 tall view: 300 - 2 - 19
            NavigationViewTest.this.test.click(40, 288);
            assertEquals(List.of("activate? 6", "activated 6"), NavigationViewTest.this.heard);
            assertEquals(-1, NavigationViewTest.this.view.selectedIndex());
        }

        @Test
        void aDisabledItemCannotBePickedButShowsItsTooltip() {
            NavigationViewTest.this.show();
            NavigationViewTest.this.item("Sound").setEnabled(false).setTooltip(Tooltip.text("Not yet"));
            NavigationViewTest.this.layout();
            NavigationViewTest.this.clickRow(1);
            assertEquals(List.of(), NavigationViewTest.this.heard);
            NavigationViewTest.this.test.host().pointerMoved(40, 30, Modifiers.NONE);
            assertTrue(NavigationViewTest.this.test.host().root().tooltip().isPresent());
        }

        @Test
        void cancelingActivationStopsEverything() {
            NavigationViewTest.this.show().onEvent(Activatable.ACTIVATE, event -> event.cancel());
            NavigationViewTest.this.clickRow(0);
            assertEquals(List.of("activate? 0"), NavigationViewTest.this.heard);
            assertEquals(-1, NavigationViewTest.this.view.selectedIndex());
        }

        @Test
        void cancelingSelectionStillActivates() {
            NavigationViewTest.this.show().onEvent(Selection.SELECT, event -> event.cancel());
            NavigationViewTest.this.clickRow(0);
            assertEquals(List.of("activate? 0", "select? 0", "activated 0"), NavigationViewTest.this.heard);
            assertEquals(-1, NavigationViewTest.this.view.selectedIndex());
        }

        @Test
        void rewritingActivationPicksAnotherItem() {
            NavigationViewTest.this.show().onEvent(Activatable.ACTIVATE, event -> event.setValue(2));
            NavigationViewTest.this.clickRow(0);
            assertEquals(2, NavigationViewTest.this.view.selectedIndex());
        }

        @Test
        void selectingFromCodeOnlyReports() {
            NavigationViewTest.this.show().select(2);
            assertEquals(List.of("selected 2"), NavigationViewTest.this.heard);
            NavigationViewTest.this.view.selectItem(NavigationViewTest.this.item("General"));
            NavigationViewTest.this.view.clearSelection();
            assertEquals(List.of("selected 2", "selected 0", "selected -1"), NavigationViewTest.this.heard);
            assertThrows(IndexOutOfBoundsException.class, () -> NavigationViewTest.this.view.select(7));
        }

        @Test
        void withoutActivateOnClickASingleClickOnlySelects() {
            NavigationViewTest.this.show().setActivateOnClick(false);
            NavigationViewTest.this.clickRow(1);
            assertEquals(List.of("select? 1", "selected 1"), NavigationViewTest.this.heard);
            NavigationViewTest.this.heard.clear();
            // The second click of a double click activates
            NavigationViewTest.this.clickRow(1);
            assertEquals(List.of("activate? 1", "activated 1"), NavigationViewTest.this.heard);
        }

        @Test
        void drawsTheSelectedItemAndItsIndicator() {
            NavigationViewTest.this.show().select(0);
            final List<String> lines = NavigationViewTest.this.render().lines();
            assertTrue(lines.contains("fill 2,2 116x19 #FF3A4D6E"));
            assertTrue(lines.contains("fill 2,2 3x19 #FF6A9FFF"));
            assertEquals(SELECTED, ((Paint.Solid) Themes.flat().paint(NavigationView.STYLE_ITEM_SELECTED)
                    .orElseThrow()).argb());
            assertEquals(INDICATOR, ((Paint.Solid) Themes.flat().paint(NavigationView.STYLE_INDICATOR)
                    .orElseThrow()).argb());
        }
    }

    @Nested
    class Pages {
        @Test
        void areBuiltOnFirstSelectionAndKept() {
            final AtomicInteger built = new AtomicInteger();
            final NavigationView view = NavigationViewTest.this.ui.navigationView()
                    .addItem("General", ui -> {
                        built.incrementAndGet();
                        return ui.textBox();
                    })
                    .addItem("Sound", ui -> ui.label("Sound page"));
            NavigationViewTest.this.test.show(view);
            assertEquals(0, built.get());
            view.select(0);
            NavigationViewTest.this.layout();
            final TextBox general = (TextBox) NavigationViewTest.this.test.tabOrder().get(1);
            assertEquals(new Rect(120, 0, 120, 20), general.bounds());
            general.setText("kept");
            view.select(1);
            assertFalse(general.isAttached());
            view.select(0);
            assertTrue(general.isAttached());
            assertEquals("kept", general.text());
            assertEquals(1, built.get());
        }

        @Test
        void theContentShowsWhileTheItemHasNoPage() {
            final Label empty = NavigationViewTest.this.ui.label("Pick a section");
            final NavigationView view = NavigationViewTest.this.ui.navigationView()
                    .addItem(NavigationViewTest.this.ui.navigationItem("Plain"))
                    .addItem("Paged", ui -> ui.label("Page"))
                    .setContent(empty);
            NavigationViewTest.this.test.show(view);
            assertTrue(empty.isAttached());
            assertEquals(Optional.of(empty), view.content());
            view.select(1);
            assertFalse(empty.isAttached());
            view.select(0);
            assertTrue(empty.isAttached());
            final Label other = NavigationViewTest.this.ui.label("Other");
            view.setContent(other);
            assertFalse(empty.isAttached());
            assertTrue(other.isAttached());
        }

        @Test
        void areDiscardedWhenTheItemLeavesOrGetsANewPage() {
            final AtomicInteger built = new AtomicInteger();
            final NavigationView view = NavigationViewTest.this.ui.navigationView()
                    .addItem("Paged", ui -> ui.label("Page " + built.incrementAndGet()));
            NavigationViewTest.this.test.show(view);
            view.select(0);
            final NavigationItem paged = view.allItems().getFirst();
            paged.setPage(ui -> ui.label("New " + built.incrementAndGet()));
            assertEquals(2, built.get());
            view.removeItem(paged);
            view.addItem(paged);
            view.select(0);
            assertEquals(3, built.get());
        }

        @Test
        void passTheirEventsOutOfTheView() {
            final Button save = NavigationViewTest.this.ui.button("Save");
            final NavigationView view = NavigationViewTest.this.ui.navigationView().addItem("General", ui -> save);
            final StackPanel screen = NavigationViewTest.this.ui.stackPanel().add(view);
            screen.onEvent(Button.PRESSED, event -> NavigationViewTest.this.heard.add("pressed"));
            NavigationViewTest.this.test.show(screen);
            view.select(0);
            save.press();
            assertEquals(List.of("pressed"), NavigationViewTest.this.heard);
        }

        @Test
        void refuseComponentsAlreadyInATree() {
            final NavigationView view = NavigationViewTest.this.ui.navigationView();
            final Label placed = NavigationViewTest.this.ui.label("Placed");
            NavigationViewTest.this.ui.stackPanel().add(placed);
            assertThrows(IllegalArgumentException.class, () -> view.setContent(placed));
            assertThrows(IllegalArgumentException.class, () -> view.setPaneHeader(placed));
            assertThrows(IllegalArgumentException.class, () -> view.setPaneFooter(placed));
            final TestHost other = new TestHost(100, 100);
            assertThrows(IllegalArgumentException.class, () -> view.setContent(other.factory().label("Other")));
        }
    }

    @Nested
    class Expanding {
        @Test
        void theArrowAsksAndShowsTheChildren() {
            NavigationViewTest.this.show();
            // Account is the sixth row: 3 items, the separator of 9, the header, at y 2 + 57 + 9 + 19 = 87
            NavigationViewTest.this.test.click(112, 96);
            assertEquals(List.of("expand item? Account true", "item expanded Account true"), NavigationViewTest.this.heard);
            assertTrue(NavigationViewTest.this.item("Account").isExpanded());
            NavigationViewTest.this.layout();
            assertTrue(NavigationViewTest.this.render().lines("text").contains("text Profile 34,111 #FFE0E0E0"));
            assertEquals(-1, NavigationViewTest.this.view.selectedIndex());
        }

        @Test
        void aClickElsewhereSelectsASelectableParentAndExpandsAnother() {
            NavigationViewTest.this.show();
            NavigationViewTest.this.test.click(40, 96);
            assertEquals(3, NavigationViewTest.this.view.selectedIndex());
            assertFalse(NavigationViewTest.this.item("Account").isExpanded());
            NavigationViewTest.this.item("Account").setSelectable(false);
            NavigationViewTest.this.heard.clear();
            NavigationViewTest.this.test.click(40, 96);
            assertEquals(List.of("activate? 3", "expand item? Account true", "item expanded Account true",
                    "activated 3"), NavigationViewTest.this.heard);
        }

        @Test
        void expandingFromCodeReports() {
            NavigationViewTest.this.show();
            NavigationViewTest.this.item("Account").setExpanded(true).setExpanded(true);
            assertEquals(List.of("item expanded Account true"), NavigationViewTest.this.heard);
        }

        @Test
        void cancelingKeepsTheItemAsItIs() {
            NavigationViewTest.this.show().onEvent(NavigationView.EXPAND_ITEM, event -> event.cancel());
            NavigationViewTest.this.test.click(112, 96);
            assertFalse(NavigationViewTest.this.item("Account").isExpanded());
        }

        @Test
        void aCollapsedParentDrawsItsSelectedChildsSelection() {
            NavigationViewTest.this.show().selectItem(NavigationViewTest.this.item("Profile"));
            assertTrue(NavigationViewTest.this.render().lines().contains("fill 2,87 116x19 #FF3A4D6E"));
            NavigationViewTest.this.item("Account").setExpanded(true);
            NavigationViewTest.this.layout();
            final List<String> lines = NavigationViewTest.this.render().lines();
            assertFalse(lines.contains("fill 2,87 116x19 #FF3A4D6E"));
            assertTrue(lines.contains("fill 2,106 116x19 #FF3A4D6E"));
        }
    }

    @Nested
    class Modes {
        @Test
        void autoFollowsTheWidth() {
            NavigationViewTest.this.show();
            assertEquals(DisplayMode.AUTO, NavigationViewTest.this.view.displayMode());
            assertEquals(DisplayMode.EXPANDED, NavigationViewTest.this.view.shownMode());
            NavigationViewTest.this.test.host().resize(300, 300);
            NavigationViewTest.this.layout();
            assertEquals(DisplayMode.COMPACT, NavigationViewTest.this.view.shownMode());
            NavigationViewTest.this.test.host().resize(200, 300);
            NavigationViewTest.this.layout();
            assertEquals(DisplayMode.MINIMAL, NavigationViewTest.this.view.shownMode());
            NavigationViewTest.this.view.setAutoWidths(150, 100);
            NavigationViewTest.this.layout();
            assertEquals(DisplayMode.EXPANDED, NavigationViewTest.this.view.shownMode());
            assertEquals(150f, NavigationViewTest.this.view.compactBelow());
            assertEquals(100f, NavigationViewTest.this.view.minimalBelow());
            assertThrows(IllegalArgumentException.class, () -> NavigationViewTest.this.view.setAutoWidths(100, 150));
            assertThrows(IllegalArgumentException.class, () -> NavigationViewTest.this.view.setAutoWidths(-1, -2));
            assertThrows(IllegalArgumentException.class,
                    () -> NavigationViewTest.this.view.setAutoWidths(Float.POSITIVE_INFINITY, 1));
        }

        @Test
        void expandedPutsThePaneBesideTheContent() {
            final Label page = NavigationViewTest.this.ui.label("Page");
            final NavigationView view = NavigationViewTest.this.ui.navigationView().setContent(page).setPaneWidth(100);
            NavigationViewTest.this.test.show(view);
            assertEquals(new Rect(0, 0, 400, 300), view.bounds());
            assertEquals(new Rect(100, 0, 24, 9), page.bounds());
            assertThrows(IllegalArgumentException.class, () -> view.setPaneWidth(-1));
            assertEquals(100f, view.paneWidth());
        }

        @Test
        void thePaneStacksItsPartsTopToBottom() {
            final TextBox search = NavigationViewTest.this.ui.textBox();
            search.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);
            final org.inspirenxe.strata.api.control.display.Avatar avatar = NavigationViewTest.this.ui.avatar()
                    .setName("Ana Lima");
            NavigationViewTest.this.show().setTitle("Settings").setPaneHeader(search).setPaneFooter(avatar);
            NavigationViewTest.this.layout();
            // The title row is 19 tall from y 2, the search box 20 tall below it, and the items scroll below that
            assertEquals(new Rect(2, 21, 116, 20), search.bounds());
            assertTrue(NavigationViewTest.this.render().lines("text").contains("text General 21,46 #FFE0E0E0"));
            // The avatar sits at the foot, under Sign out
            assertEquals(new Rect(2, 274, 24, 24), avatar.bounds());
            assertTrue(NavigationViewTest.this.render().lines("text").contains("text Sign out 21,260 #FFE0E0E0"));
            assertEquals(Optional.of(avatar), NavigationViewTest.this.view.paneFooter());
            assertEquals("Settings", NavigationViewTest.this.view.title().plain());
        }

        @Test
        void compactShowsAColumnOfIcons() {
            NavigationViewTest.this.show().setDisplayMode(DisplayMode.COMPACT);
            final Label page = NavigationViewTest.this.ui.label("Page");
            NavigationViewTest.this.view.setContent(page).setPaneHeader(NavigationViewTest.this.ui.textBox());
            NavigationViewTest.this.layout();
            assertEquals(DisplayMode.COMPACT, NavigationViewTest.this.view.shownMode());
            assertEquals(new Rect(23, 0, 24, 9), page.bounds());
            final List<String> lines = NavigationViewTest.this.render().lines();
            // No text is drawn but the content's; the toggle's three lines sit in the top cell
            assertEquals(List.of("text Page 23,0 #FFE0E0E0"), NavigationViewTest.this.render().lines("text"));
            assertTrue(lines.contains("fill 8,8 7x1 #FFE0E0E0"));
            // The pane header is out of the tree, so only the view takes Tab
            assertEquals(List.of(NavigationViewTest.this.view), NavigationViewTest.this.test.tabOrder());
            // An item's text is its tooltip
            NavigationViewTest.this.test.host().pointerMoved(10, 30, Modifiers.NONE);
            final Component<?> hovered = NavigationViewTest.this.test.host().root().hovered().facade();
            assertEquals(Tooltip.text(NavigationViewTest.this.item("General").text()), ((HasTooltip<?>) hovered).tooltip());
        }

        @Test
        void minimalShowsABar() {
            final Label page = NavigationViewTest.this.ui.label("Page");
            NavigationViewTest.this.show().setTitle("Settings").setContent(page).setDisplayMode(DisplayMode.MINIMAL);
            NavigationViewTest.this.layout();
            assertEquals(new Rect(0, 23, 24, 9), page.bounds());
            assertEquals(List.of("text Settings 25,7 #FFFFFFFF", "text Page 0,23 #FFE0E0E0"),
                    NavigationViewTest.this.render().lines("text"));
        }

        @Test
        void theToggleOpensThePaneOverTheContent() {
            NavigationViewTest.this.show().setDisplayMode(DisplayMode.COMPACT);
            NavigationViewTest.this.layout();
            NavigationViewTest.this.test.click(10, 10);
            assertEquals(List.of("expand? true", "expanded true"), NavigationViewTest.this.heard);
            assertTrue(NavigationViewTest.this.view.isExpanded());
            final List<Overlay> open = NavigationViewTest.this.overlays();
            assertEquals(1, open.size());
            assertEquals(new Rect(0, 0, 120, 300), open.getFirst().content().bounds());
            // The full pane shows over the content, its toggle in the same place
            assertTrue(NavigationViewTest.this.render().lines("text").contains("text General 21,26 #FFE0E0E0"));
            NavigationViewTest.this.test.click(10, 10);
            assertFalse(NavigationViewTest.this.view.isExpanded());
            assertEquals(List.of("expand? true", "expanded true", "expand? false", "expanded false"),
                    NavigationViewTest.this.heard);
        }

        @Test
        void thePaneOverTheContentClosesOnAPressOutsideEscapeAndAPick() {
            NavigationViewTest.this.show().setDisplayMode(DisplayMode.COMPACT);
            NavigationViewTest.this.layout();
            NavigationViewTest.this.view.setExpanded(true);
            NavigationViewTest.this.test.click(300, 150);
            assertFalse(NavigationViewTest.this.view.isExpanded());

            NavigationViewTest.this.view.setExpanded(true);
            assertTrue(NavigationViewTest.this.key(Key.ESCAPE));
            assertFalse(NavigationViewTest.this.view.isExpanded());

            NavigationViewTest.this.view.setExpanded(true);
            NavigationViewTest.this.heard.clear();
            // The pane's toggle row is 19 tall, so General is at y 21
            NavigationViewTest.this.test.click(40, 30);
            assertEquals(List.of("activate? 0", "select? 0", "selected 0", "activated 0", "expanded false"),
                    NavigationViewTest.this.heard);
            assertFalse(NavigationViewTest.this.view.isExpanded());
            // The pane is back beside the content as a column, first among the view's children
            assertTrue(NavigationViewTest.this.overlays().isEmpty());
            assertSame(NavigationViewTest.this.view, NavigationViewTest.this.test.focused());
        }

        @Test
        void cancelingExpandKeepsThePaneClosed() {
            NavigationViewTest.this.show().setDisplayMode(DisplayMode.MINIMAL)
                    .onEvent(NavigationView.EXPAND, event -> event.cancel());
            NavigationViewTest.this.layout();
            NavigationViewTest.this.test.click(10, 10);
            assertFalse(NavigationViewTest.this.view.isExpanded());
            assertEquals(List.of("expand? true"), NavigationViewTest.this.heard);
        }

        @Test
        void thePaneOverTheContentFollowsTheViewAndClosesWhenTheModeChanges() {
            NavigationViewTest.this.show().setDisplayMode(DisplayMode.AUTO);
            NavigationViewTest.this.test.host().resize(300, 300);
            NavigationViewTest.this.layout();
            NavigationViewTest.this.view.setExpanded(true);
            NavigationViewTest.this.test.host().resize(300, 200);
            NavigationViewTest.this.layout();
            assertEquals(new Rect(0, 0, 120, 200), NavigationViewTest.this.overlays().getFirst().content().bounds());
            NavigationViewTest.this.heard.clear();
            NavigationViewTest.this.test.host().resize(500, 200);
            NavigationViewTest.this.layout();
            assertEquals(DisplayMode.EXPANDED, NavigationViewTest.this.view.shownMode());
            assertFalse(NavigationViewTest.this.view.isExpanded());
            assertEquals(List.of("expanded false"), NavigationViewTest.this.heard);
            assertTrue(NavigationViewTest.this.overlays().isEmpty());
        }

        @Test
        void thePaneDoesNotOpenOverTheContentWhenExpanded() {
            NavigationViewTest.this.show().setExpanded(true);
            assertFalse(NavigationViewTest.this.view.isExpanded());
            assertEquals(List.of(), NavigationViewTest.this.heard);
            assertTrue(NavigationViewTest.this.view.isToggleShown());
        }

        @Test
        void theColumnOpensChildrenInAMenu() {
            NavigationViewTest.this.show().setDisplayMode(DisplayMode.COMPACT);
            NavigationViewTest.this.layout();
            // In the column the header is hidden: toggle 19, three items, the separator, then Account at y 87
            NavigationViewTest.this.item("Account").setSelectable(false);
            NavigationViewTest.this.test.click(10, 95);
            final List<Overlay> open = NavigationViewTest.this.overlays();
            assertEquals(1, open.size());
            final Menu menu = (Menu) open.getFirst().content();
            assertEquals(2, menu.items().size());
            assertEquals(new Rect(23, 87, menu.bounds().width(), menu.bounds().height()), menu.bounds());
            NavigationViewTest.this.heard.clear();
            NavigationViewTest.this.key(Key.DOWN);
            NavigationViewTest.this.key(Key.ENTER);
            assertEquals(List.of("activate? 4", "select? 4", "selected 4", "activated 4"), NavigationViewTest.this.heard);
            assertTrue(NavigationViewTest.this.overlays().isEmpty());
        }

        @Test
        void minimalOpensThePaneWithEnter() {
            NavigationViewTest.this.show().setDisplayMode(DisplayMode.MINIMAL);
            NavigationViewTest.this.layout();
            NavigationViewTest.this.view.requestFocus();
            assertTrue(NavigationViewTest.this.key(Key.ENTER));
            assertTrue(NavigationViewTest.this.view.isExpanded());
            NavigationViewTest.this.key(Key.DOWN);
            assertSame(NavigationViewTest.this.item("General"),
                    ((NavigationViewImpl) NavigationViewTest.this.view).cursorItem());
        }

        @Test
        void hidingTheToggleLeavesItOut() {
            NavigationViewTest.this.show().setDisplayMode(DisplayMode.COMPACT).setToggleShown(false);
            NavigationViewTest.this.layout();
            assertFalse(NavigationViewTest.this.view.isToggleShown());
            assertFalse(NavigationViewTest.this.render().lines().contains("fill 8,8 7x1 #FFE0E0E0"));
            // Without the top row, General is the first cell
            NavigationViewTest.this.test.click(10, 10);
            assertEquals(0, NavigationViewTest.this.view.selectedIndex());
        }
    }

    @Nested
    class Keyboard {
        @Test
        void theCursorSkipsWhatCannotBePicked() {
            NavigationViewTest.this.show();
            NavigationViewTest.this.item("Video").setEnabled(false);
            NavigationViewTest.this.view.requestFocus();
            final NavigationViewImpl impl = (NavigationViewImpl) NavigationViewTest.this.view;
            assertSame(NavigationViewTest.this.item("General"), impl.cursorItem());
            NavigationViewTest.this.key(Key.DOWN);
            NavigationViewTest.this.key(Key.DOWN);
            assertSame(NavigationViewTest.this.item("Account"), impl.cursorItem());
            NavigationViewTest.this.key(Key.END);
            assertSame(NavigationViewTest.this.item("Sign out"), impl.cursorItem());
            NavigationViewTest.this.key(Key.HOME);
            assertSame(NavigationViewTest.this.item("General"), impl.cursorItem());
            NavigationViewTest.this.key(Key.UP);
            assertSame(NavigationViewTest.this.item("General"), impl.cursorItem());
        }

        @Test
        void enterAndSpacePickTheCursorsItem() {
            NavigationViewTest.this.show().requestFocus();
            NavigationViewTest.this.key(Key.DOWN);
            assertTrue(NavigationViewTest.this.key(Key.SPACE));
            assertEquals(1, NavigationViewTest.this.view.selectedIndex());
            NavigationViewTest.this.key(Key.UP);
            assertTrue(NavigationViewTest.this.key(Key.ENTER));
            assertEquals(0, NavigationViewTest.this.view.selectedIndex());
        }

        @Test
        void rightExpandsAndLeftCollapsesOrGoesUp() {
            NavigationViewTest.this.show().requestFocus();
            final NavigationViewImpl impl = (NavigationViewImpl) NavigationViewTest.this.view;
            NavigationViewTest.this.key(Key.DOWN);
            NavigationViewTest.this.key(Key.DOWN);
            NavigationViewTest.this.key(Key.DOWN);
            assertSame(NavigationViewTest.this.item("Account"), impl.cursorItem());
            assertTrue(NavigationViewTest.this.key(Key.RIGHT));
            assertTrue(NavigationViewTest.this.item("Account").isExpanded());
            NavigationViewTest.this.key(Key.DOWN);
            assertSame(NavigationViewTest.this.item("Profile"), impl.cursorItem());
            NavigationViewTest.this.key(Key.LEFT);
            assertSame(NavigationViewTest.this.item("Account"), impl.cursorItem());
            NavigationViewTest.this.key(Key.LEFT);
            assertFalse(NavigationViewTest.this.item("Account").isExpanded());
        }

        @Test
        void theCursorStartsOnTheSelectedItemAndIsRinged() {
            NavigationViewTest.this.show().select(2);
            NavigationViewTest.this.view.requestFocus();
            assertSame(NavigationViewTest.this.item("Video"), ((NavigationViewImpl) NavigationViewTest.this.view).cursorItem());
            assertTrue(NavigationViewTest.this.render().lines().contains("fill 2,40 116x1 #FF6A9FFF"));
        }

        @Test
        void keysFromThePageAreLeftAlone() {
            final TextBox field = NavigationViewTest.this.ui.textBox();
            final NavigationView view = NavigationViewTest.this.ui.navigationView()
                    .addItem(NavigationViewTest.this.ui.navigationItem("One"))
                    .addItem(NavigationViewTest.this.ui.navigationItem("Two"))
                    .setContent(field);
            NavigationViewTest.this.test.show(view);
            field.requestFocus();
            NavigationViewTest.this.key(Key.DOWN);
            assertNull(((NavigationViewImpl) view).cursorItem());
        }

        @Test
        void tabGoesFromTheViewToThePaneHeaderThenTheContent() {
            final TextBox search = NavigationViewTest.this.ui.textBox();
            final TextBox page = NavigationViewTest.this.ui.textBox();
            final NavigationView view = NavigationViewTest.this.ui.navigationView()
                    .setContent(page)
                    .setPaneHeader(search)
                    .setDisplayMode(DisplayMode.MINIMAL);
            NavigationViewTest.this.test.show(view);
            view.setExpanded(true);
            view.setExpanded(false);
            view.setDisplayMode(DisplayMode.EXPANDED);
            NavigationViewTest.this.layout();
            assertEquals(List.of(view, search, page), NavigationViewTest.this.test.tabOrder());
            assertEquals(Optional.of(search), view.paneHeader());
        }
    }

    @Nested
    class Theming {
        @Test
        void requiresTheSelectedPaint() {
            final Theme theme = Theme.builder("bare")
                    .paint(org.inspirenxe.strata.api.layout.ScrollPanel.STYLE_THUMB, Paint.solid(0xFF000000))
                    .build();
            final TestHost host = new TestHost(new TestPlatform(), theme, 100, 100);
            final ThemeException error = assertThrows(ThemeException.class,
                    () -> host.show(host.factory().navigationView()));
            assertEquals(Set.of(NavigationView.STYLE_ITEM_SELECTED), error.missingKeys());
        }

        @Test
        void fallsBackToTheListsHoverAndTheComboBoxsPopup() {
            NavigationViewTest.this.show().setDisplayMode(DisplayMode.COMPACT);
            NavigationViewTest.this.layout();
            NavigationViewTest.this.view.setExpanded(true);
            NavigationViewTest.this.test.host().pointerMoved(40, 30, Modifiers.NONE);
            final List<String> lines = NavigationViewTest.this.render().lines();
            // The overlay draws nav.pane, which the flat theme sets
            assertTrue(lines.contains("fill 0,0 120x300 #FF1F2024"));
            assertTrue(lines.contains("fill 2,21 116x19 #FF2E3136"));

            final Theme.Builder noPane = Theme.builder("no pane");
            Themes.flat().paints().forEach((key, paint) -> {
                if (!key.equals(NavigationView.STYLE_PANE)) {
                    noPane.paint(key, paint);
                }
            });
            Themes.flat().textStyles().forEach(noPane::textStyle);
            NavigationViewTest.this.test.host().setTheme(noPane.build());
            NavigationViewTest.this.layout();
            assertTrue(NavigationViewTest.this.render().lines().contains("fill 0,0 120x300 #FF16171A"));
            assertEquals(0xFF16171A, ((Paint.Solid) Themes.flat().paint(ComboBox.STYLE_POPUP).orElseThrow()).argb());
            assertEquals(0xFF2E3136, ((Paint.Solid) Themes.flat().paint(ListView.STYLE_ROW_HOVERED).orElseThrow()).argb());
        }

        @Test
        void drawsSeparatorsAndHeaders() {
            NavigationViewTest.this.show();
            final DrawLog canvas = NavigationViewTest.this.render();
            // The separator under Video, 4 below its row's top and inset by the item padding
            assertTrue(canvas.lines().contains("fill 8,63 104x1 #FF3C3F44"));
            // The header in the disabled text box color
            assertTrue(canvas.lines("text").contains("text People 8,73 #FF707070"));
        }
    }

    @Test
    void aDisabledViewClosesItsPaneAndTakesNoFocus() {
        this.show().setDisplayMode(DisplayMode.COMPACT);
        this.layout();
        this.view.setExpanded(true);
        this.view.setEnabled(false);
        assertFalse(this.view.isExpanded());
        assertEquals(List.of(), this.test.tabOrder());
        this.view.setExpanded(true);
        assertFalse(this.view.isExpanded());
    }

    @Test
    void snapshotsItsState() {
        this.show().select(1);
        final List<ComponentSnapshot> snapshots = new ArrayList<>();
        this.view.onEvent(Activatable.ACTIVATE, event -> snapshots.add(event.snapshot()));
        this.clickRow(0);
        assertEquals(new NavigationView.State(1, DisplayMode.EXPANDED, false), snapshots.getFirst().state());
        assertNotNull(new Expansion(this.item("Account"), true).item());
        assertNotSame(this.item("General"), this.item("Sound"));
    }
}
