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
import org.inspirenxe.strata.api.component.ComponentSnapshot;
import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.control.attribute.popup.Closable;
import org.inspirenxe.strata.api.control.attribute.popup.Openable;
import org.inspirenxe.strata.api.control.attribute.popup.Popup;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.button.DropDownButton;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.control.menu.Flyout;
import org.inspirenxe.strata.api.control.menu.Menu;
import org.inspirenxe.strata.api.control.menu.MenuBar;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifier;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.layout.AbsolutePanel;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.ThemeException;
import org.inspirenxe.strata.api.theme.Themes;
import org.inspirenxe.strata.api.visual.Visual;
import org.inspirenxe.strata.spi.Canvas;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The drop-down button: how it measures and draws, how it opens and follows its popup, its keys, focus, the rule of
 * one button per popup, and disabling. Text in the test platform is 6 units per code point and 9 per line, so a
 * button captioned "Sort" is 24 for the caption, 4 for the gap, 7 for the arrow and 16 for the padding: 51 by 20.
 */
class DropDownButtonTest {
    private static final SpriteId ICON = SpriteId.of("test", "icon");
    private static final int TEXT = 0xFFF0F0F0;
    private static final int TEXT_DISABLED = 0xFF808080;
    private static final int NORMAL = 0xFF3C3F44;
    private static final int PRESSED = 0xFF2B2D31;
    private static final int DISABLED = 0xFF2A2A2A;

    private final TestHost test = new TestHost(300, 200);
    private final ComponentFactory ui = this.test.factory();
    private final List<String> heard = new ArrayList<>();

    /**
     * Shows a button at (10, 10), with another plain button at (200, 10) to take focus and presses.
     */
    private AbsolutePanel show(final DropDownButton button) {
        final AbsolutePanel panel = this.ui.absolutePanel()
                .add(button, 10, 10)
                .add(this.ui.button("Other").onEvent(Button.PRESSED, event -> this.heard.add("other pressed")), 200, 10);
        this.test.show(panel);
        return panel;
    }

    private Menu menu() {
        return this.ui.menu()
                .addItem("One", () -> this.heard.add("one"))
                .addItem("Two", () -> this.heard.add("two"));
    }

    /**
     * Logs the button's and a popup's open and close events.
     */
    private DropDownButton logged(final DropDownButton button) {
        button.onEvent(Openable.OPEN, event -> this.heard.add("button open?"))
                .onEvent(Openable.OPENED, event -> this.heard.add("button opened"))
                .onEvent(Closable.CLOSE, event -> this.heard.add("button close?"))
                .onEvent(Closable.CLOSED, event -> this.heard.add("button closed"));
        return button;
    }

    private Menu logged(final Menu menu) {
        return menu.onEvent(Openable.OPENED, event -> this.heard.add("menu opened"))
                .onEvent(Closable.CLOSED, event -> this.heard.add("menu closed"));
    }

    private void clickButton() {
        this.test.click(20, 20);
    }

    private boolean key(final Key key) {
        return this.key(key, Modifiers.NONE);
    }

    private boolean key(final Key key, final Modifiers modifiers) {
        return this.test.host().key(key, KeyAction.PRESS, modifiers);
    }

    private Recording render() {
        final Recording canvas = new Recording();
        this.test.host().render(canvas);
        return canvas;
    }

    @Nested
    class Measuring {
        @Test
        void isAButtonWithRoomForTheArrow() {
            final DropDownButton button = DropDownButtonTest.this.ui.dropDownButton("Sort");
            final Button plain = DropDownButtonTest.this.ui.button("Sort");
            DropDownButtonTest.this.test.show(DropDownButtonTest.this.ui.absolutePanel().add(button, 0, 0).add(plain, 0, 50));
            assertEquals(new Rect(0, 0, 51, 20), button.bounds());
            assertEquals(plain.bounds().width() + 4 + 7, button.bounds().width());
        }

        @Test
        void anIconComesBeforeTheCaption() {
            final DropDownButton both = DropDownButtonTest.this.ui.dropDownButton("Sort").setIcon(ICON);
            final DropDownButton iconOnly = DropDownButtonTest.this.ui.dropDownButton().setIcon(ICON);
            DropDownButtonTest.this.test.show(DropDownButtonTest.this.ui.absolutePanel().add(both, 0, 0).add(iconOnly, 0, 50));
            assertEquals(new Rect(0, 0, 16 + 4 + 24 + 4 + 7 + 16, 26), both.bounds());
            // The icon padding is 2 on every side, and the line height does not count
            assertEquals(new Rect(0, 50, 2 + 16 + 4 + 7 + 2, 20), iconOnly.bounds());
        }

        @Test
        void followsTheButtonPaddingKeysAndSetPadding() {
            final Theme theme = Themes.flat().toBuilder().insets(Button.STYLE_PADDING, Insets.of(10f, 6f)).build();
            final TestHost host = new TestHost(new TestPlatform(), theme, 300, 200);
            final DropDownButton button = host.factory().dropDownButton("Sort");
            host.show(host.factory().absolutePanel().add(button, 0, 0));
            assertEquals(new Rect(0, 0, 24 + 4 + 7 + 20, 21), button.bounds());
            button.setTextPadding(2f);
            host.host().layout();
            assertEquals(new Rect(0, 0, 24 + 4 + 7 + 4, 20), button.bounds());
        }

        @Test
        void isFocusableWithAHandCursor() {
            final DropDownButton button = DropDownButtonTest.this.ui.dropDownButton("Sort");
            assertTrue(button.isFocusable());
            assertTrue(button.isTabbable());
            assertEquals(CursorKind.HAND, button.cursor());
        }

        @Test
        void needsTheButtonFaces() {
            final TestHost host = new TestHost(new TestPlatform(), Theme.builder("bare").build(), 200, 200);
            final ThemeException error = assertThrows(ThemeException.class,
                    () -> host.show(host.factory().dropDownButton("Sort")));
            assertEquals(Set.of(Button.STYLE_NORMAL, Button.STYLE_HOVERED, Button.STYLE_PRESSED, Button.STYLE_DISABLED),
                    error.missingKeys());
        }
    }

    @Nested
    class Drawing {
        @Test
        void drawsTheCaptionCenteredAndTheArrowAtTheRight() {
            DropDownButtonTest.this.show(DropDownButtonTest.this.ui.dropDownButton("Sort"));
            final Recording canvas = DropDownButtonTest.this.render();
            assertEquals(new Fill(new Rect(10, 10, 51, 20), NORMAL), canvas.fills.getFirst());
            // Inside the padding less the gap and the arrow, which is exactly the caption's width
            assertEquals(new Drawn("Sort", 18, 15.5f), canvas.texts.getFirst());
            // The combo box's arrow: four shrinking lines, 7 by 4, against the right padding
            assertEquals(List.of(new Fill(new Rect(46, 18, 7, 1), TEXT), new Fill(new Rect(47, 19, 5, 1), TEXT),
                    new Fill(new Rect(48, 20, 3, 1), TEXT), new Fill(new Rect(49, 21, 1, 1), TEXT)),
                    canvas.fillsAfter(1, 4));
        }

        @Test
        void aWideButtonCentersTheCaptionLeftOfTheArrow() {
            final DropDownButton button = DropDownButtonTest.this.ui.dropDownButton("Sort");
            button.setWidth(101);
            DropDownButtonTest.this.show(button);
            final Recording canvas = DropDownButtonTest.this.render();
            assertEquals(new Drawn("Sort", 18 + 25, 15.5f), canvas.texts.getFirst());
            assertEquals(new Fill(new Rect(10 + 101 - 8 - 7, 18, 7, 1), TEXT), canvas.fillsAfter(1, 1).getFirst());
        }

        @Test
        void theArrowKeyPaintsTheArrow() {
            final Theme theme = Themes.flat().toBuilder().paint(DropDownButton.STYLE_ARROW, Paint.solid(0xFF00FF00)).build();
            final TestHost host = new TestHost(new TestPlatform(), theme, 300, 200);
            final DropDownButton button = host.factory().dropDownButton("Sort");
            host.show(host.factory().absolutePanel().add(button, 10, 10));
            final Recording canvas = new Recording();
            host.host().render(canvas);
            assertEquals(new Fill(new Rect(46, 18, 7, 1), 0xFF00FF00), canvas.fillsAfter(1, 1).getFirst());
            // Disabled, the arrow takes the disabled caption's color whatever the key says
            button.setEnabled(false);
            final Recording disabled = new Recording();
            host.host().render(disabled);
            assertEquals(new Fill(new Rect(46, 18, 7, 1), TEXT_DISABLED), disabled.fillsAfter(1, 1).getFirst());
        }

        @Test
        void aDisabledButtonDrawsItsDisabledFaceCaptionAndArrow() {
            final DropDownButton button = DropDownButtonTest.this.ui.dropDownButton("Sort");
            DropDownButtonTest.this.show(button.setEnabled(false));
            final Recording canvas = DropDownButtonTest.this.render();
            assertEquals(new Fill(new Rect(10, 10, 51, 20), DISABLED), canvas.fills.getFirst());
            assertEquals(TEXT_DISABLED, canvas.styles.getFirst());
            assertEquals(new Fill(new Rect(46, 18, 7, 1), TEXT_DISABLED), canvas.fillsAfter(1, 1).getFirst());
        }

        @Test
        void anOpenButtonDrawsThePressedFace() {
            final DropDownButton button = DropDownButtonTest.this.ui.dropDownButton("Sort", DropDownButtonTest.this.menu());
            DropDownButtonTest.this.show(button);
            button.open();
            final Recording canvas = DropDownButtonTest.this.render();
            assertEquals(new Fill(new Rect(10, 10, 51, 20), PRESSED), canvas.fills.getFirst());
        }
    }

    @Nested
    class Opening {
        @Test
        void aClickAsksThenOpensThePopupBelowAndReports() {
            final Menu menu = DropDownButtonTest.this.logged(DropDownButtonTest.this.menu());
            final DropDownButton button = DropDownButtonTest.this.logged(DropDownButtonTest.this.ui.dropDownButton("Sort", menu));
            DropDownButtonTest.this.show(button);
            DropDownButtonTest.this.clickButton();
            assertEquals(List.of("button open?", "menu opened", "button opened"), DropDownButtonTest.this.heard);
            assertTrue(button.isOpen());
            assertTrue(menu.isOpen());
            assertEquals(new Rect(10, 30, 60, 34), menu.bounds());
        }

        @Test
        void cancelingOpenKeepsThePopupClosed() {
            final Menu menu = DropDownButtonTest.this.logged(DropDownButtonTest.this.menu());
            final DropDownButton button = DropDownButtonTest.this.logged(DropDownButtonTest.this.ui.dropDownButton("Sort", menu));
            button.onEvent(Openable.OPEN, event -> event.cancel());
            DropDownButtonTest.this.show(button);
            DropDownButtonTest.this.clickButton();
            DropDownButtonTest.this.key(Key.ENTER);
            assertEquals(List.of("button open?", "button open?"), DropDownButtonTest.this.heard);
            assertFalse(button.isOpen());
            assertFalse(menu.isOpen());
            assertTrue(DropDownButtonTest.this.ui.overlays().open().isEmpty());
        }

        @Test
        void aMenuOpensAtLeastAsWideAsTheButtonForThatOpeningOnly() {
            final Menu menu = DropDownButtonTest.this.menu();
            final DropDownButton button = DropDownButtonTest.this.ui.dropDownButton("Sort", menu);
            button.setWidth(120);
            DropDownButtonTest.this.show(button);
            DropDownButtonTest.this.clickButton();
            assertEquals(120, menu.bounds().width());
            assertEquals(0f, menu.minWidth());
            menu.close();
            menu.openAt(10, 100);
            DropDownButtonTest.this.test.host().layout();
            assertEquals(60, menu.bounds().width());
        }

        @Test
        void aMenusOwnMinimumWidthStillApplies() {
            final Menu menu = DropDownButtonTest.this.menu().setMinWidth(150);
            final DropDownButton button = DropDownButtonTest.this.ui.dropDownButton("Sort", menu);
            DropDownButtonTest.this.show(button);
            DropDownButtonTest.this.clickButton();
            assertEquals(150, menu.bounds().width());
            assertEquals(150f, menu.minWidth());
        }

        @Test
        void aFlyoutOpensAtItsContentSize() {
            final Flyout flyout = DropDownButtonTest.this.ui.flyout(DropDownButtonTest.this.ui.label("Hello"));
            final DropDownButton button = DropDownButtonTest.this.ui.dropDownButton("Color", flyout);
            button.setWidth(120);
            DropDownButtonTest.this.show(button);
            DropDownButtonTest.this.clickButton();
            assertTrue(button.isOpen());
            assertEquals(new Rect(10, 30, 30 + 8, 9 + 8), flyout.bounds());
        }

        @Test
        void withNothingToOpenItAsksNothingAndStaysClosed() {
            final DropDownButton none = DropDownButtonTest.this.logged(DropDownButtonTest.this.ui.dropDownButton("None"));
            final DropDownButton emptyMenu = DropDownButtonTest.this.logged(
                    DropDownButtonTest.this.ui.dropDownButton("Empty", DropDownButtonTest.this.ui.menu()));
            final DropDownButton emptyFlyout = DropDownButtonTest.this.logged(
                    DropDownButtonTest.this.ui.dropDownButton("Flyout", DropDownButtonTest.this.ui.flyout()));
            DropDownButtonTest.this.test.show(DropDownButtonTest.this.ui.absolutePanel()
                    .add(none, 0, 0).add(emptyMenu, 0, 50).add(emptyFlyout, 0, 100));
            for (final DropDownButton button : List.of(none, emptyMenu, emptyFlyout)) {
                DropDownButtonTest.this.test.click(button.bounds().x() + 5, button.bounds().y() + 5);
                button.requestFocus();
                DropDownButtonTest.this.key(Key.ENTER);
                button.open();
                assertFalse(button.isOpen());
            }
            assertEquals(List.of(), DropDownButtonTest.this.heard);
        }

        @Test
        void openingFromCodeAsksNothing() {
            final Menu menu = DropDownButtonTest.this.logged(DropDownButtonTest.this.menu());
            final DropDownButton button = DropDownButtonTest.this.logged(DropDownButtonTest.this.ui.dropDownButton("Sort", menu));
            DropDownButtonTest.this.show(button);
            button.open();
            button.open();
            assertTrue(button.isOpen());
            button.close();
            button.close();
            assertEquals(List.of("menu opened", "button opened", "menu closed", "button closed"),
                    DropDownButtonTest.this.heard);
            assertFalse(menu.isOpen());
        }

        @Test
        void aButtonThatIsNotShownDoesNotOpen() {
            final Menu menu = DropDownButtonTest.this.menu();
            final DropDownButton button = DropDownButtonTest.this.ui.dropDownButton("Sort", menu);
            button.open();
            assertFalse(button.isOpen());
            assertFalse(menu.isOpen());
        }
    }

    @Nested
    class FollowingThePopup {
        @Test
        void theButtonReportsClosedAfterThePopupForEveryClose() {
            final Menu menu = DropDownButtonTest.this.logged(DropDownButtonTest.this.menu());
            final DropDownButton button = DropDownButtonTest.this.logged(DropDownButtonTest.this.ui.dropDownButton("Sort", menu));
            DropDownButtonTest.this.show(button);
            final List<String> heard = DropDownButtonTest.this.heard;

            // Closed from code on the popup
            button.open();
            heard.clear();
            menu.close();
            assertEquals(List.of("menu closed", "button closed"), heard);
            assertFalse(button.isOpen());

            // Escape in the menu
            button.open();
            heard.clear();
            DropDownButtonTest.this.key(Key.ESCAPE);
            assertEquals(List.of("menu closed", "button closed"), heard);

            // A choice: the menus close, then the action runs
            button.open();
            heard.clear();
            DropDownButtonTest.this.test.click(20, 30 + 2 + 15 + 7);
            assertEquals(List.of("menu closed", "button closed", "two"), heard);
            assertFalse(button.isOpen());
        }

        @Test
        void aPressOutsideClosesThePopupAndGoesNoFurther() {
            final Menu menu = DropDownButtonTest.this.logged(DropDownButtonTest.this.menu());
            final DropDownButton button = DropDownButtonTest.this.logged(DropDownButtonTest.this.ui.dropDownButton("Sort", menu));
            DropDownButtonTest.this.show(button);
            button.open();
            DropDownButtonTest.this.heard.clear();
            DropDownButtonTest.this.test.click(210, 15);
            assertEquals(List.of("menu closed", "button closed"), DropDownButtonTest.this.heard);
            assertFalse(button.isOpen());
        }

        @Test
        void aPressOnTheOpenButtonClosesItWithoutOpeningAgain() {
            final Menu menu = DropDownButtonTest.this.logged(DropDownButtonTest.this.menu());
            final DropDownButton button = DropDownButtonTest.this.logged(DropDownButtonTest.this.ui.dropDownButton("Sort", menu));
            DropDownButtonTest.this.show(button);
            DropDownButtonTest.this.clickButton();
            DropDownButtonTest.this.clickButton();
            assertEquals(List.of("button open?", "menu opened", "button opened", "menu closed", "button closed"),
                    DropDownButtonTest.this.heard);
            assertFalse(menu.isOpen());
        }

        @Test
        void aClickOnTheOpenButtonClosesAFlyoutThatStaysOnPressesOutside() {
            final Flyout flyout = DropDownButtonTest.this.ui.flyout(DropDownButtonTest.this.ui.label("Hello"))
                    .setLightDismiss(false);
            final DropDownButton button = DropDownButtonTest.this.logged(DropDownButtonTest.this.ui.dropDownButton("Color", flyout));
            DropDownButtonTest.this.show(button);
            DropDownButtonTest.this.clickButton();
            DropDownButtonTest.this.clickButton();
            assertEquals(List.of("button open?", "button opened", "button closed"), DropDownButtonTest.this.heard);
            assertFalse(flyout.isOpen());
        }

        @Test
        void aPopupOpenedElsewhereDoesNotOpenTheButton() {
            final Menu menu = DropDownButtonTest.this.menu();
            final DropDownButton button = DropDownButtonTest.this.logged(DropDownButtonTest.this.ui.dropDownButton("Sort", menu));
            DropDownButtonTest.this.show(button);
            menu.openAt(100, 100);
            assertFalse(button.isOpen());
            menu.close();
            assertEquals(List.of(), DropDownButtonTest.this.heard);
        }

        @Test
        void neverAsksClose() {
            final DropDownButton button = DropDownButtonTest.this.logged(
                    DropDownButtonTest.this.ui.dropDownButton("Sort", DropDownButtonTest.this.menu()));
            DropDownButtonTest.this.show(button);
            DropDownButtonTest.this.clickButton();
            DropDownButtonTest.this.key(Key.ESCAPE);
            DropDownButtonTest.this.clickButton();
            DropDownButtonTest.this.clickButton();
            assertFalse(DropDownButtonTest.this.heard.contains("button close?"));
        }
    }

    @Nested
    class Keys {
        @Test
        void enterKeypadEnterSpaceAndAltDownOpen() {
            final Menu menu = DropDownButtonTest.this.menu();
            final DropDownButton button = DropDownButtonTest.this.ui.dropDownButton("Sort", menu);
            DropDownButtonTest.this.show(button);
            final Key[] keys = {Key.ENTER, Key.KP_ENTER, Key.SPACE, Key.DOWN};
            for (final Key key : keys) {
                button.requestFocus();
                assertTrue(DropDownButtonTest.this.key(key, key == Key.DOWN ? Modifiers.of(Modifier.ALT) : Modifiers.NONE));
                assertTrue(button.isOpen(), key + " opens");
                menu.close();
            }
        }

        @Test
        void otherKeysAndRepeatsDoNotOpen() {
            final DropDownButton button = DropDownButtonTest.this.logged(
                    DropDownButtonTest.this.ui.dropDownButton("Sort", DropDownButtonTest.this.menu()));
            DropDownButtonTest.this.show(button);
            button.requestFocus();
            DropDownButtonTest.this.key(Key.DOWN);
            DropDownButtonTest.this.test.host().key(Key.ENTER, KeyAction.REPEAT, Modifiers.NONE);
            DropDownButtonTest.this.test.host().key(Key.SPACE, KeyAction.RELEASE, Modifiers.NONE);
            assertFalse(button.isOpen());
            assertEquals(List.of(), DropDownButtonTest.this.heard);
        }

        @Test
        void aMenuOpenedByAKeyHighlightsItsFirstItem() {
            final DropDownButton button = DropDownButtonTest.this.ui.dropDownButton("Sort", DropDownButtonTest.this.menu());
            DropDownButtonTest.this.show(button);
            button.requestFocus();
            DropDownButtonTest.this.key(Key.ENTER);
            DropDownButtonTest.this.key(Key.ENTER);
            assertEquals(List.of("one"), DropDownButtonTest.this.heard);
            assertFalse(button.isOpen());
        }

        @Test
        void aMenuOpenedByAClickHighlightsNothing() {
            final DropDownButton button = DropDownButtonTest.this.ui.dropDownButton("Sort", DropDownButtonTest.this.menu());
            DropDownButtonTest.this.show(button);
            DropDownButtonTest.this.clickButton();
            DropDownButtonTest.this.key(Key.ENTER);
            assertEquals(List.of(), DropDownButtonTest.this.heard);
            assertTrue(button.isOpen());
        }
    }

    @Nested
    class Focus {
        @Test
        void theMenuTakesFocusAndGivesItBackToTheButton() {
            final Menu menu = DropDownButtonTest.this.menu();
            final DropDownButton button = DropDownButtonTest.this.ui.dropDownButton("Sort", menu);
            DropDownButtonTest.this.show(button);
            DropDownButtonTest.this.clickButton();
            assertSame(menu, DropDownButtonTest.this.test.focused());
            DropDownButtonTest.this.key(Key.ESCAPE);
            assertSame(button, DropDownButtonTest.this.test.focused());

            DropDownButtonTest.this.clickButton();
            DropDownButtonTest.this.test.click(20, 30 + 2 + 7);
            assertEquals(List.of("one"), DropDownButtonTest.this.heard);
            assertSame(button, DropDownButtonTest.this.test.focused());
        }

        @Test
        void aFlyoutFocusesItsContentAndGivesFocusBack() {
            final TextBox box = DropDownButtonTest.this.ui.textBox();
            final Flyout flyout = DropDownButtonTest.this.ui.flyout(box);
            final DropDownButton button = DropDownButtonTest.this.ui.dropDownButton("Find", flyout);
            DropDownButtonTest.this.show(button);
            button.requestFocus();
            DropDownButtonTest.this.key(Key.SPACE);
            assertSame(box, DropDownButtonTest.this.test.focused());
            DropDownButtonTest.this.key(Key.ESCAPE);
            assertFalse(flyout.isOpen());
            assertFalse(button.isOpen());
            assertSame(button, DropDownButtonTest.this.test.focused());
        }
    }

    @Nested
    class OnePopupPerButton {
        @Test
        void aPopupAnotherButtonHoldsIsRefused() {
            final Menu menu = DropDownButtonTest.this.menu();
            final Flyout flyout = DropDownButtonTest.this.ui.flyout(DropDownButtonTest.this.ui.label("Hi"));
            final DropDownButton first = DropDownButtonTest.this.ui.dropDownButton("First", menu);
            final DropDownButton second = DropDownButtonTest.this.ui.dropDownButton("Second");
            assertThrows(IllegalArgumentException.class, () -> second.setPopup(menu));
            DropDownButtonTest.this.ui.dropDownButton("Third", flyout);
            assertThrows(IllegalArgumentException.class, () -> second.setPopup(flyout));
            assertEquals(Optional.empty(), second.popup());

            // Once freed, it can move
            first.setPopup(null);
            second.setPopup(menu);
            assertSame(menu, second.popup().orElseThrow());
            // Setting the same popup again changes nothing
            second.setPopup(menu);
            assertSame(menu, second.popup().orElseThrow());
        }

        @Test
        void aMenuThatOpensFromAMenuOrABarIsRefused() {
            final Menu submenu = DropDownButtonTest.this.menu();
            final Menu barMenu = DropDownButtonTest.this.menu();
            DropDownButtonTest.this.ui.menu().addSubmenu("More", submenu);
            final MenuBar bar = DropDownButtonTest.this.ui.menuBar().addMenu("File", barMenu);
            final DropDownButton button = DropDownButtonTest.this.ui.dropDownButton("Sort");
            assertThrows(IllegalArgumentException.class, () -> button.setPopup(submenu));
            assertThrows(IllegalArgumentException.class, () -> button.setPopup(barMenu));
            assertEquals(1, bar.menus().size());
        }

        @Test
        void aHeldMenuCannotBecomeASubmenuOrABarMenu() {
            final Menu menu = DropDownButtonTest.this.menu();
            DropDownButtonTest.this.ui.dropDownButton("Sort", menu);
            assertThrows(IllegalArgumentException.class, () -> DropDownButtonTest.this.ui.menu().addSubmenu("More", menu));
            assertThrows(IllegalArgumentException.class, () -> DropDownButtonTest.this.ui.menuBar().addMenu("File", menu));
        }

        @Test
        void aPopupFromAnotherFactoryIsRefused() {
            final TestHost other = new TestHost(100, 100);
            final DropDownButton button = DropDownButtonTest.this.ui.dropDownButton("Sort");
            assertThrows(IllegalArgumentException.class, () -> button.setPopup(other.factory().menu()));
        }

        @Test
        void replacingAnOpenPopupClosesItAndReportsClosed() {
            final Menu menu = DropDownButtonTest.this.logged(DropDownButtonTest.this.menu());
            final DropDownButton button = DropDownButtonTest.this.logged(DropDownButtonTest.this.ui.dropDownButton("Sort", menu));
            DropDownButtonTest.this.show(button);
            button.open();
            DropDownButtonTest.this.heard.clear();
            final Menu other = DropDownButtonTest.this.menu();
            button.setPopup(other);
            assertEquals(List.of("menu closed", "button closed"), DropDownButtonTest.this.heard);
            assertFalse(button.isOpen());
            assertFalse(menu.isOpen());
            // The old menu is free again
            DropDownButtonTest.this.ui.dropDownButton("Again", menu);
            button.open();
            assertTrue(other.isOpen());
        }
    }

    @Nested
    class Disabled {
        @Test
        void takesNoFocusAndDoesNotOpen() {
            final Menu menu = DropDownButtonTest.this.menu();
            final DropDownButton button = DropDownButtonTest.this.logged(DropDownButtonTest.this.ui.dropDownButton("Sort", menu));
            DropDownButtonTest.this.show(button.setEnabled(false));
            assertFalse(button.requestFocus());
            DropDownButtonTest.this.clickButton();
            button.open();
            assertFalse(button.isOpen());
            assertFalse(menu.isOpen());
            assertEquals(List.of(), DropDownButtonTest.this.heard);
        }

        @Test
        void disablingAnOpenButtonClosesItsPopup() {
            final Menu menu = DropDownButtonTest.this.logged(DropDownButtonTest.this.menu());
            final DropDownButton button = DropDownButtonTest.this.logged(DropDownButtonTest.this.ui.dropDownButton("Sort", menu));
            DropDownButtonTest.this.show(button);
            button.open();
            DropDownButtonTest.this.heard.clear();
            button.setEnabled(false);
            assertEquals(List.of("menu closed", "button closed"), DropDownButtonTest.this.heard);
            assertFalse(button.isOpen());
        }
    }

    @Nested
    class Settings {
        @Test
        void iconsAreSpritesOrVisuals() {
            final DropDownButton button = DropDownButtonTest.this.ui.dropDownButton("Sort").setIcon(ICON);
            assertSame(ICON, button.iconSprite());
            assertEquals(16f, button.iconWidth());
            final Visual visual = () -> new Size(10f, 12f);
            button.setIcon(visual);
            assertSame(visual, button.iconVisual());
            assertEquals(null, button.iconSprite());
            assertEquals(10f, button.iconWidth());
            assertEquals(12f, button.iconHeight());
            button.clearIcon();
            assertEquals(null, button.iconVisual());
            assertThrows(IllegalArgumentException.class, () -> button.setIconSize(-1, 4));
        }

        @Test
        void menusAndFlyoutsArePopups() {
            DropDownButtonTest.this.show(DropDownButtonTest.this.ui.dropDownButton("Sort"));
            final List<Popup<?>> popups = List.of(DropDownButtonTest.this.menu(),
                    DropDownButtonTest.this.ui.flyout(DropDownButtonTest.this.ui.label("Hi")));
            for (final Popup<?> popup : popups) {
                popup.openAt(50, 50);
                assertTrue(popup.isOpen());
                popup.close();
                assertFalse(popup.isOpen());
            }
        }

        @Test
        void theSnapshotHoldsTheCaptionAndWhetherItWasOpen() {
            final DropDownButton button = DropDownButtonTest.this.ui.dropDownButton("Sort", DropDownButtonTest.this.menu());
            final List<ComponentSnapshot> seen = new ArrayList<>();
            button.onEvent(Openable.OPEN, event -> seen.add(event.snapshot()));
            DropDownButtonTest.this.show(button);
            DropDownButtonTest.this.clickButton();
            assertEquals(new DropDownButton.State(Text.of("Sort"), false),
                    seen.getFirst().state());
        }
    }

    /**
     * A fill and its color.
     */
    record Fill(Rect rect, int argb) {
    }

    /**
     * Text drawn and where.
     */
    record Drawn(String text, float x, float y) {
    }

    /**
     * Records fills with their colors, and text with its position and color.
     */
    static final class Recording implements Canvas {
        final List<Fill> fills = new ArrayList<>();
        final List<Drawn> texts = new ArrayList<>();
        final List<Integer> styles = new ArrayList<>();

        /**
         * Returns {@code count} fills from {@code from} on.
         */
        List<Fill> fillsAfter(final int from, final int count) {
            return this.fills.subList(from, from + count);
        }

        @Override
        public void fill(final Rect rect, final int argb) {
            this.fills.add(new Fill(rect, argb));
        }

        @Override
        public void fillGradient(final Rect rect, final int topArgb, final int bottomArgb) {
        }

        @Override
        public void sprite(final SpriteId sprite, final Rect rect, final int tint) {
        }

        @Override
        public void nineSlice(final SpriteId sprite, final Rect rect, final Insets border, final int tint) {
        }

        @Override
        public void text(final TextSpan span, final float x, final float y) {
            this.texts.add(new Drawn(span.text(), x, y));
            this.styles.add(span.style().color());
        }

        @Override
        public void pushClip(final Rect rect) {
        }

        @Override
        public void popClip() {
        }

        @Override
        public void pushTranslation(final float dx, final float dy) {
        }

        @Override
        public void pushScale(final float scale) {
        }

        @Override
        public void popTransform() {
        }

        @Override
        public void visual(final Visual visual, final Rect rect, final int tint) {
        }

        @Override
        public void nextLayer() {
        }

        @Override
        public void requestCursor(final CursorKind cursor) {
        }
    }
}
