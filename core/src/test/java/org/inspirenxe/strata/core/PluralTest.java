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

import org.inspirenxe.strata.api.text.PluralCategory;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.core.text.Translations;
import org.inspirenxe.strata.spi.Translator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Plural categories and plural text (design 4.49).
 */
class PluralTest {
    private static final Map<String, String> ENGLISH = Map.of(
            "listing.count.one", "%s listing",
            "listing.count.other", "%s listings");
    private static final Map<String, String> RUSSIAN = Map.of(
            "listing.count.one", "%s лот",
            "listing.count.few", "%s лота",
            "listing.count.many", "%s лотов",
            "listing.count.other", "%s лота");

    @AfterEach
    void resetTranslator() {
        Translations.setTranslator(Translator.NONE);
    }

    private static PluralCategory of(final String tag, final long count) {
        return PluralCategory.of(Locale.forLanguageTag(tag), count);
    }

    @Test
    void englishHasOneAndOther() {
        assertEquals(PluralCategory.ONE, of("en", 1));
        assertEquals(PluralCategory.OTHER, of("en", 0));
        assertEquals(PluralCategory.OTHER, of("en", 2));
        assertEquals(PluralCategory.OTHER, of("en", 21));
        assertEquals(PluralCategory.ONE, of("en", -1));
    }

    @Test
    void russianHasThreeForms() {
        assertEquals(PluralCategory.ONE, of("ru", 1));
        assertEquals(PluralCategory.ONE, of("ru", 21));
        assertEquals(PluralCategory.MANY, of("ru", 11));
        assertEquals(PluralCategory.FEW, of("ru", 2));
        assertEquals(PluralCategory.FEW, of("ru", 34));
        assertEquals(PluralCategory.MANY, of("ru", 12));
        assertEquals(PluralCategory.MANY, of("ru", 5));
        assertEquals(PluralCategory.MANY, of("ru", 0));
        assertEquals(PluralCategory.MANY, of("ru", 111));
    }

    @Test
    void arabicHasSixForms() {
        assertEquals(PluralCategory.ZERO, of("ar", 0));
        assertEquals(PluralCategory.ONE, of("ar", 1));
        assertEquals(PluralCategory.TWO, of("ar", 2));
        assertEquals(PluralCategory.FEW, of("ar", 3));
        assertEquals(PluralCategory.FEW, of("ar", 110));
        assertEquals(PluralCategory.MANY, of("ar", 11));
        assertEquals(PluralCategory.MANY, of("ar", 199));
        assertEquals(PluralCategory.OTHER, of("ar", 100));
        assertEquals(PluralCategory.OTHER, of("ar", 102));
    }

    @Test
    void otherLanguagesFollowTheirRules() {
        assertEquals(PluralCategory.ONE, of("fr", 0));
        assertEquals(PluralCategory.ONE, of("fr", 1));
        assertEquals(PluralCategory.OTHER, of("fr", 2));
        assertEquals(PluralCategory.MANY, of("fr", 1_000_000));
        assertEquals(PluralCategory.ONE, of("pt-BR", 0));
        assertEquals(PluralCategory.OTHER, of("pt-PT", 0));
        assertEquals(PluralCategory.MANY, of("es", 2_000_000));
        assertEquals(PluralCategory.OTHER, of("es", 2_000_001));
        assertEquals(PluralCategory.ONE, of("pl", 1));
        assertEquals(PluralCategory.FEW, of("pl", 22));
        assertEquals(PluralCategory.MANY, of("pl", 21));
        assertEquals(PluralCategory.FEW, of("cs", 4));
        assertEquals(PluralCategory.OTHER, of("cs", 5));
        assertEquals(PluralCategory.TWO, of("sl", 102));
        assertEquals(PluralCategory.TWO, of("he", 2));
        assertEquals(PluralCategory.TWO, of("iw", 2));
        assertEquals(PluralCategory.ZERO, of("lv", 10));
        assertEquals(PluralCategory.FEW, of("ro", 0));
        assertEquals(PluralCategory.FEW, of("lt", 9));
        assertEquals(PluralCategory.MANY, of("cy", 6));
        assertEquals(PluralCategory.OTHER, of("ja", 1));
        assertEquals(PluralCategory.OTHER, of("zh", 1));
        assertEquals(PluralCategory.ONE, of("hi", 0));
        assertEquals(PluralCategory.ONE, of("xx", 1));
        assertEquals(PluralCategory.OTHER, of("xx", 2));
        assertEquals(PluralCategory.MANY, of("ru", Long.MIN_VALUE));
    }

    @Test
    void pluralTextPicksTheTranslationForTheCount() {
        Translations.setTranslator(new LocaleTranslator(Locale.ENGLISH, ENGLISH));
        assertEquals("1 listing", Text.plural("listing.count", 1, 1).plain());
        assertEquals("3 listings", Text.plural("listing.count", 3, 3).plain());
        assertEquals("0 listings", Text.plural("listing.count", 0, 0).plain());
    }

    @Test
    void pluralTextFollowsTheLanguage() {
        final LocaleTranslator translator = new LocaleTranslator(Locale.ENGLISH, ENGLISH);
        Translations.setTranslator(translator);
        final Text text = Text.plural("listing.count", 5, 5);
        assertEquals("5 listings", text.plain());
        translator.set(Locale.forLanguageTag("ru"), RUSSIAN);
        assertEquals("5 лотов", text.plain());
        assertEquals("2 лота", Text.plural("listing.count", 2, 2).plain());
        assertEquals("21 лот", Text.plural("listing.count", 21, 21).plain());
    }

    @Test
    void aMissingCategoryFallsBackToOther() {
        Translations.setTranslator(new LocaleTranslator(Locale.forLanguageTag("ar"),
                Map.of("listing.count.one", "one", "listing.count.other", "%s other")));
        assertEquals("one", Text.plural("listing.count", 1, 1).plain());
        assertEquals("0 other", Text.plural("listing.count", 0, 0).plain());
        assertEquals("11 other", Text.plural("listing.count", 11, 11).plain());
    }

    @Test
    void aKeyWithNoTranslationShowsItsOtherKey() {
        Translations.setTranslator(new LocaleTranslator(Locale.ENGLISH));
        assertEquals("missing.count.other", Text.plural("missing.count", 1).plain());
    }

    @Test
    void aTranslatorCanGiveItsOwnCategories() {
        Translations.setTranslator(new Translator() {
            @Override
            public Optional<String> translate(final String key) {
                return Optional.ofNullable(Map.of("cats.few", "a few cats", "cats.other", "cats").get(key));
            }

            @Override
            public long version() {
                return 0L;
            }

            @Override
            public PluralCategory plural(final long count) {
                return count < 5 ? PluralCategory.FEW : PluralCategory.OTHER;
            }
        });
        assertEquals("a few cats", Text.plural("cats", 1).plain());
        assertEquals("cats", Text.plural("cats", 7).plain());
    }

    @Test
    void theDefaultRuleReadsTheTranslatorsLocale() {
        final Translator translator = new LocaleTranslator(Locale.forLanguageTag("ru"));
        assertEquals(PluralCategory.FEW, translator.plural(3));
        assertEquals(PluralCategory.MANY, translator.plural(5));
    }

    @Test
    void pluralTextIsEqualByKeyCountAndArguments() {
        assertEquals(Text.plural("a", 2, "x"), Text.plural("a", 2, "x"));
        assertNotEquals(Text.plural("a", 2, "x"), Text.plural("a", 3, "x"));
        assertNotEquals(Text.plural("a", 2, "x"), Text.translatable("a", "x"));
        assertThrows(NullPointerException.class, () -> Text.plural("a", 1, (Object) null));
    }
}
