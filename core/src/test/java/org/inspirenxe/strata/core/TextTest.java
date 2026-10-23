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

import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.control.window.MessageBox;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.core.text.Translations;
import org.inspirenxe.strata.spi.Translator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link Text}: how it resolves, what each operation does, and how translated text follows the translator.
 */
class TextTest {
    private static final TextStyle BASE = TextStyle.DEFAULT.withShadow(true);
    private static final int GOLD = 0xFFFFAA00;
    private static final int RED = 0xFFFF5555;

    @AfterEach
    void resetTranslator() {
        Translations.setTranslator(Translator.NONE);
    }

    private static TextSpan span(final String text, final TextStyle style) {
        return new TextSpan(text, style);
    }

    /**
     * A translator backed by a map that can change, with a version that moves when it does.
     */
    private static final class MapTranslator implements Translator {
        private volatile Map<String, String> entries;
        private final AtomicLong version = new AtomicLong();

        MapTranslator(final Map<String, String> entries) {
            this.entries = entries;
        }

        void set(final Map<String, String> entries) {
            this.entries = entries;
            this.version.incrementAndGet();
        }

        @Override
        public Optional<String> translate(final String key) {
            return Optional.ofNullable(this.entries.get(key));
        }

        @Override
        public long version() {
            return this.version.get();
        }
    }

    @Nested
    class Making {
        @Test
        void literalTextTakesTheBaseUnlessItHasAStyle() {
            assertEquals(List.of(span("Ana", BASE)), Text.of("Ana").resolve(BASE));
            final TextStyle fixed = TextStyle.DEFAULT.withColor(GOLD);
            assertEquals(List.of(span("Ben", fixed)), Text.of("Ben", fixed).resolve(BASE));
        }

        @Test
        void emptyTextResolvesToNoSpans() {
            assertEquals(List.of(), Text.empty().resolve(BASE));
            assertTrue(Text.of("").isEmpty());
        }

        @Test
        void partsJoinAndAdjacentSpansOfOneStyleMerge() {
            final Text text = Text.of("Ana").append(", ").append(Text.of("Ben"));
            assertEquals(List.of(span("Ana, Ben", BASE)), text.resolve(BASE));
            assertEquals("Ana, Ben", text.plain());
        }

        @Test
        void joinPutsTheSeparatorBetweenParts() {
            final Text names = Text.join(Text.of(", "), List.of(Text.of("Ana"), Text.of("Ben"), Text.of("Cleo")));
            assertEquals("Ana, Ben, Cleo", names.plain());
            assertEquals("", Text.join(Text.of(", "), List.of()).plain());
        }

        @Test
        void theBuilderAddsStyledParts() {
            final Text text = Text.builder().append("Price: ").color("64", GOLD).bold(" each").build();
            assertEquals(List.of(span("Price: ", BASE), span("64", BASE.withColor(GOLD)), span(" each", BASE.withBold(true))),
                    text.resolve(BASE));
        }

        @Test
        void textBuiltTheSameWayIsEqual() {
            assertEquals(Text.of("Ana").bold(), Text.of("Ana").bold());
            assertEquals(Text.translatable("shop.price", 3), Text.translatable("shop.price", 3));
            assertNotEquals(Text.of("Ana"), Text.of("Ana").bold());
        }
    }

    @Nested
    class Styles {
        @Test
        void changingTheBaseKeepsWhatPartsSetThemselves() {
            final Text price = Text.of("Price ").append(Text.of("64").color(GOLD)).color(RED);
            assertEquals(List.of(span("Price ", BASE.withColor(RED)), span("64", BASE.withColor(GOLD))), price.resolve(BASE));
        }

        @Test
        void styleChangesTheBaseWithAFunction() {
            final Text text = Text.of("Ana").style(style -> style.withItalic(true).withShadow(false));
            assertEquals(List.of(span("Ana", BASE.withItalic(true).withShadow(false))), text.resolve(BASE));
        }

        @Test
        void mapSpansChangesEveryResolvedSpan() {
            final Text price = Text.of("Price ").append(Text.of("64").color(GOLD))
                    .mapSpans(span -> span.withStyle(span.style().withColor(RED)));
            assertEquals(List.of(span("Price 64", BASE.withColor(RED))), price.resolve(BASE));
        }

        @Test
        void styleMatchesRestylesMatchesAcrossParts() {
            final Text text = Text.of("Diamond ").append(Text.of("Sword").color(GOLD))
                    .styleMatches(Pattern.compile("nd sw", Pattern.CASE_INSENSITIVE), style -> style.withBold(true));
            assertEquals(List.of(span("Diamo", BASE), span("nd ", BASE.withBold(true)),
                    span("Sw", BASE.withColor(GOLD).withBold(true)), span("ord", BASE.withColor(GOLD))), text.resolve(BASE));
        }
    }

    @Nested
    class Operations {
        @Test
        void replaceFitsTheReplacementToWhereItLands() {
            final Text text = Text.of("Hi ").append(Text.of("NAME").color(GOLD)).replace("NAME", Text.of("Ana").bold());
            assertEquals(List.of(span("Hi ", BASE), span("Ana", BASE.withColor(GOLD).withBold(true))), text.resolve(BASE));
            assertEquals("Ana and Ana", Text.of("x and x").replace("x", "Ana").plain());
            assertThrows(IllegalArgumentException.class, () -> Text.of("x").replace("", "y"));
        }

        @Test
        void trimAndNormalizeWorkAcrossParts() {
            final Text text = Text.of("  Ana \n").append(Text.of("\t Ben  ").bold());
            assertEquals("Ana \n\t Ben", text.trim().plain());
            assertEquals(" Ana Ben ", text.normalizeWhitespace().plain());
        }

        @Test
        void splitsKeepEachPartsStyle() {
            final Text text = Text.of("one two\n").append(Text.of("three").bold());
            final List<Text> lines = text.splitByLines();
            assertEquals(2, lines.size());
            assertEquals("one two", lines.get(0).plain());
            assertEquals(List.of(span("three", BASE.withBold(true))), lines.get(1).resolve(BASE));
            assertEquals(List.of("one", "two", "three"), text.splitByWords().stream().map(Text::plain).toList());
        }
    }

    @Nested
    class Translation {
        @Test
        void aMissingKeyShowsTheKey() {
            assertEquals("shop.title", Text.translatable("shop.title").plain());
        }

        @Test
        void strataKeysFallBackToEnglish() {
            assertEquals("Cancel", Text.translatable("strata.button.cancel").plain());
        }

        @Test
        void argumentsFillThePatternWithTheirOwnStyles() {
            Translations.setTranslator(new MapTranslator(Map.of("shop.price", "Price: %s each (%2$s%%)", "shop.swap", "%2$s %1$s")));
            final Text price = Text.translatable("shop.price", Text.of("64").color(GOLD), 5);
            assertEquals(List.of(span("Price: ", BASE), span("64", BASE.withColor(GOLD)), span(" each (5%)", BASE)),
                    price.resolve(BASE));
            assertEquals("Ben Ana", Text.translatable("shop.swap", "Ana", "Ben").plain());
        }

        @Test
        void aSpecifierWithNoArgumentStaysAsWritten() {
            Translations.setTranslator(new MapTranslator(Map.of("shop.count", "%s of %s")));
            assertEquals("3 of %s", Text.translatable("shop.count", 3).plain());
        }

        @Test
        void operationsOnTranslatedTextFollowTheLanguage() {
            final MapTranslator translator = new MapTranslator(Map.of("shop.greet", "Hello NAME"));
            Translations.setTranslator(translator);
            final Text greeting = Text.translatable("shop.greet").replace("NAME", "Ana");
            assertEquals("Hello Ana", greeting.plain());
            translator.set(Map.of("shop.greet", "Hallo NAME"));
            assertEquals("Hallo Ana", greeting.plain());
        }

        @Test
        void labelsLayOutAgainWhenTheLanguageChanges() {
            final MapTranslator translator = new MapTranslator(Map.of("shop.title", "Shop"));
            Translations.setTranslator(translator);
            final TestHost test = new TestHost(200, 100);
            final Label label = test.factory().label(Text.translatable("shop.title"));
            test.show(label);
            assertEquals(4 * 6f, label.desiredWidth());
            translator.set(Map.of("shop.title", "Laden!"));
            test.host().layout();
            assertEquals(6 * 6f, label.desiredWidth());
        }

        @Test
        void messageBoxButtonsAreTranslated() {
            Translations.setTranslator(new MapTranslator(Map.of("strata.button.yes", "Ja")));
            final TestHost test = new TestHost(400, 300);
            final MessageBox box = test.factory().messageBox(Text.of("Delete?"), Text.of("Sure?"), MessageBox.Buttons.YES_NO);
            test.show(box);
            final NullCanvas canvas = new NullCanvas();
            test.host().render(canvas);
            final List<String> drawn = canvas.texts().stream().map(TextSpan::text).toList();
            assertTrue(drawn.contains("Ja"), drawn.toString());
            assertTrue(drawn.contains("No"), drawn.toString());
        }
    }

    @Nested
    class Patterns {
        @Test
        void aPositionalIndexTooLongForAnIntIsShownAsWritten() {
            Translations.setTranslator(new MapTranslator(Map.of("test.total", "Total %99999999999$s")));

            assertEquals("Total %99999999999$s", Text.translatable("test.total", 3).plain());
        }
    }
}
