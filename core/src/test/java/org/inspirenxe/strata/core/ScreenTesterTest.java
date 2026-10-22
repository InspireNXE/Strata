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
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.event.input.InputEvents;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.Modifier;
import org.inspirenxe.strata.api.layout.ScrollPanel;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.inspirenxe.strata.api.screen.Screen;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;
import org.inspirenxe.strata.core.text.Translations;
import org.inspirenxe.strata.spi.Translator;
import org.inspirenxe.strata.testing.Matching;
import org.inspirenxe.strata.testing.ScreenTester;
import org.inspirenxe.strata.testing.TesterOptions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The test kit as an application uses it: {@link ScreenTester} opens a screen, finds its parts, drives it with
 * simulated input and reads what it shows.
 */
class ScreenTesterTest {

    /**
     * A shop screen as an application writes it: a quantity, a Buy button and a total.
     */
    static final class ShopScreen implements Screen {
        final List<String> log = new ArrayList<>();
        private final double price;

        ShopScreen(final double price) {
            this.price = price;
        }

        @Override
        public Component<?> build(final ComponentFactory factory) {
            final TextBox quantity = factory.textBox().setId("quantity");
            final Label total = factory.label("0.00").setId("total");
            final Button buy = factory.button("Buy").onEvent(Button.PRESSED, event -> {
                final int count = quantity.text().isEmpty() ? 0 : Integer.parseInt(quantity.text());
                total.setText(String.format(Locale.ROOT, "%.2f", count * this.price));
            });
            final Button cancel = factory.button("Cancel").setEnabled(false);
            return factory.stackPanel().setSpacing(4).setPadding(8).add(quantity).add(buy).add(cancel).add(total);
        }

        @Override
        public void closed() {
            this.log.add("closed");
        }
    }

    @Test
    void aTestTypesAQuantityClicksBuyAndReadsTheTotal() {
        try (ScreenTester tester = ScreenTester.open(new ShopScreen(9.99))) {
            tester.click(tester.byId("quantity", TextBox.class)).type("3")
                    .click(tester.find(Button.class, Matching.text("Buy")));

            assertEquals("29.97", tester.byId("total", Label.class).text().plain());
            assertTrue(tester.drawnText().contains("29.97"), "the last frame drew the new total");
        }
    }

    @Test
    void closingTheTesterClosesTheScreenOnce() {
        final ShopScreen shop = new ShopScreen(1);
        final ScreenTester tester = ScreenTester.open(shop);

        tester.close();
        tester.close();

        assertEquals(List.of("closed"), shop.log);
        assertThrows(IllegalStateException.class, tester::frame);
    }

    @Nested
    class Finding {
        @Test
        void findFailsWithWhatItSawWhenNothingMatches() {
            try (ScreenTester tester = ScreenTester.open(new ShopScreen(1))) {
                final AssertionError error = assertThrows(AssertionError.class,
                        () -> tester.find(Button.class, Matching.text("Sell")));

                assertTrue(error.getMessage().contains("\"Buy\""), error.getMessage());
                assertTrue(error.getMessage().contains("\"Cancel\""), error.getMessage());
            }
        }

        @Test
        void findFailsWhenMoreThanOneMatches() {
            try (ScreenTester tester = ScreenTester.open(new ShopScreen(1))) {
                final AssertionError error = assertThrows(AssertionError.class,
                        () -> tester.find(Button.class, button -> true));

                assertTrue(error.getMessage().startsWith("2 components match"), error.getMessage());
            }
        }

        @Test
        void findAllKeepsTreeOrderAndMatchingCombines() {
            try (ScreenTester tester = ScreenTester.open(new ShopScreen(1))) {
                final List<Button> buttons = tester.findAll(Button.class, button -> true);
                assertEquals(List.of("Buy", "Cancel"), buttons.stream().map(button -> button.text().plain()).toList());

                assertEquals("Buy", tester.find(Button.class, Matching.enabled()).text().plain());
            }
        }

        @Test
        void byIdFailsForAnIdNobodyHas() {
            try (ScreenTester tester = ScreenTester.open(new ShopScreen(1))) {
                assertThrows(AssertionError.class, () -> tester.byId("price", Label.class));
                assertThrows(AssertionError.class, () -> tester.byId("quantity", Label.class));
            }
        }

        @Test
        void findLooksInOpenOverlays() {
            try (ScreenTester tester = ScreenTester.open(new ShopScreen(1))) {
                final Label hint = tester.factory().label("Out of stock").setId("hint");
                tester.factory().overlays().popup(hint, new Rect(0, 0, 10, 10));
                tester.frame();

                assertSame(hint, tester.byId("hint", Label.class));
                assertEquals(1, tester.overlays().size());
                assertTrue(tester.drawnText().contains("Out of stock"));
            }
        }
    }

    @Nested
    class Refusing {
        @Test
        void aDisabledButtonCannotBeClicked() {
            try (ScreenTester tester = ScreenTester.open(new ShopScreen(1))) {
                final AssertionError error = assertThrows(AssertionError.class,
                        () -> tester.click(tester.find(Button.class, Matching.text("Cancel"))));

                assertTrue(error.getMessage().endsWith("it is disabled"), error.getMessage());
            }
        }

        @Test
        void aComponentInsideAHiddenOneCannotBeClicked() {
            final Screen screen = factory -> factory.stackPanel()
                    .add(factory.stackPanel().setVisible(false).add(factory.button("Buy")));
            try (ScreenTester tester = ScreenTester.open(screen)) {
                final AssertionError error = assertThrows(AssertionError.class,
                        () -> tester.click(tester.find(Button.class, Matching.text("Buy"))));

                assertTrue(error.getMessage().contains("is inside hidden StackPanel"), error.getMessage());
            }
        }

        @Test
        void aButtonUnderADialogCannotBeClicked() {
            try (ScreenTester tester = ScreenTester.open(new ShopScreen(1))) {
                final Button buy = tester.find(Button.class, Matching.text("Buy"));
                tester.factory().overlays().dialog(tester.factory().label("Busy"));

                final AssertionError error = assertThrows(AssertionError.class, () -> tester.click(buy));

                assertTrue(error.getMessage().contains("covered by") || error.getMessage().contains("backdrop"),
                        error.getMessage());
            }
        }

        @Test
        void aButtonScrolledOutOfViewCannotBeClicked() {
            final Screen screen = factory -> {
                final StackPanel column = factory.stackPanel().add(new Block(factory, 100, 300))
                        .add(factory.button("Far"));
                final ScrollPanel scroll = factory.scrollPanel();
                scroll.add(column);
                scroll.setHeight(100);
                return factory.stackPanel().add(scroll);
            };
            try (ScreenTester tester = ScreenTester.open(screen)) {
                final Button far = tester.find(Button.class, Matching.text("Far"));
                final AssertionError error = assertThrows(AssertionError.class, () -> tester.click(far));
                assertTrue(error.getMessage().contains("clipped out of view by ScrollPanel"), error.getMessage());

                tester.scroll(tester.find(ScrollPanel.class, any -> true), -40);
                tester.click(far);
            }
        }

        @Test
        void aComponentNotOnTheScreenCannotBeClicked() {
            try (ScreenTester tester = ScreenTester.open(new ShopScreen(1))) {
                final Button loose = tester.factory().button("Loose");

                final AssertionError error = assertThrows(AssertionError.class, () -> tester.click(loose));

                assertTrue(error.getMessage().endsWith("it is not on this screen"), error.getMessage());
            }
        }
    }

    @Nested
    class Input {
        private final List<String> heard = new ArrayList<>();

        private Screen block() {
            return factory -> factory.stackPanel().add(new Block(factory, 50, 50)
                    .onEvent(InputEvents.CLICK, event -> this.heard.add(event.button() + " x" + event.clicks()))
                    .onEvent(InputEvents.DRAG_START, event -> this.heard.add("drag start"))
                    .onEvent(InputEvents.DRAG_END, event -> this.heard.add("drag end")));
        }

        @Test
        void twoClicksAreTwoSingleClicksAndADoubleClickCountsTwo() {
            try (ScreenTester tester = ScreenTester.open(this.block())) {
                final Block block = tester.find(Block.class, any -> true);

                tester.click(block).click(block).doubleClick(block).rightClick(block);

                assertEquals(List.of("LEFT x1", "LEFT x1", "LEFT x1", "LEFT x2", "RIGHT x1"), this.heard);
            }
        }

        @Test
        void aDragStartsPastTheThresholdAndEnds() {
            try (ScreenTester tester = ScreenTester.open(this.block())) {
                tester.drag(tester.find(Block.class, any -> true), 30, 0);

                assertEquals(List.of("drag start", "drag end"), this.heard);
            }
        }

        @Test
        void pressMovesFocusWithTabAndHoldsModifiers() {
            try (ScreenTester tester = ScreenTester.open(new ShopScreen(1))) {
                final TextBox quantity = tester.byId("quantity", TextBox.class);
                tester.press(Key.TAB);
                assertEquals(Optional.of(quantity), tester.focused());

                tester.type("12").press(Key.A, Modifier.CONTROL).type("7");

                assertEquals("7", quantity.text());
            }
        }

        @Test
        void advanceMovesTheClockSoATooltipShows() {
            final Screen screen = factory -> factory.stackPanel().add(factory.button("Buy").setTooltip("Buys it"));
            try (ScreenTester tester = ScreenTester.open(screen)) {
                tester.rightClick(tester.find(Button.class, Matching.text("Buy")));
                assertTrue(!tester.drawnText().contains("Buys it"), "the clock stands still");

                tester.advance(5_000);

                assertTrue(tester.drawnText().contains("Buys it"), String.valueOf(tester.drawnText()));
            }
        }
    }

    @Nested
    class Options {
        @Test
        void theSizeThemeAndWallClockComeFromTheOptions() {
            final ZonedDateTime now = ZonedDateTime.parse("2027-02-28T09:30Z");
            final LocalDate[] today = new LocalDate[1];
            final Screen screen = factory -> {
                today[0] = LocalDate.now(((ComponentFactoryImpl) factory).platform().clock().wall());
                return factory.stackPanel().setAnchor(org.inspirenxe.strata.api.layout.Anchor.FILL).setId("root");
            };
            try (ScreenTester tester = ScreenTester.open(screen, TesterOptions.DEFAULT.withSize(320, 200).withNow(now))) {
                assertEquals(new Rect(0, 0, 320, 200), tester.byId("root", StackPanel.class).bounds());
                assertEquals(LocalDate.of(2027, 2, 28), today[0]);
            }
        }

        @Test
        void theTranslatorHoldsWhileTheTesterIsOpen() {
            final Translator before = Translations.translator();
            final Translator french = new LocaleTranslator(Locale.FRANCE, Map.of("shop.buy", "Acheter"));
            final Screen screen = factory -> factory.stackPanel().add(factory.button(Text.translatable("shop.buy")));
            try (ScreenTester tester = ScreenTester.open(screen, TesterOptions.DEFAULT.withTranslator(french))) {
                assertTrue(tester.drawnText().contains("Acheter"), String.valueOf(tester.drawnText()));
            }
            assertSame(before, Translations.translator());
        }

        @Test
        void aSizeThatIsNotPositiveIsRefused() {
            assertThrows(IllegalArgumentException.class, () -> TesterOptions.DEFAULT.withSize(0, 10));
            assertThrows(IllegalArgumentException.class, () -> TesterOptions.DEFAULT.withSize(10, Float.POSITIVE_INFINITY));
        }
    }

    /**
     * A plain block of a fixed size, written on the public extension point.
     */
    static final class Block extends CustomComponent<Block> {
        private final float width;
        private final float height;

        Block(final ComponentFactory factory, final float width, final float height) {
            super(factory);
            this.width = width;
            this.height = height;
        }

        @Override
        protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
            return new Size(this.width, this.height);
        }
    }
}
