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
package org.inspirenxe.strata.api.text;

import org.inspirenxe.strata.api.Strata;

import java.util.Locale;

/**
 * The plural forms a language tells apart, as the Unicode CLDR names them. English uses {@link #ONE} for 1 and
 * {@link #OTHER} for every other count; Russian adds {@link #FEW} and {@link #MANY}, and Arabic uses all six.
 * {@link Text#plural} picks a translation by the category of a count.
 */
public enum PluralCategory {
    /**
     * The form for zero in the languages that have one, such as Arabic and Latvian.
     */
    ZERO,
    /**
     * The singular, which some languages also use for other counts, such as 21 in Russian.
     */
    ONE,
    /**
     * The dual, such as 2 in Arabic, Hebrew and Slovenian.
     */
    TWO,
    /**
     * The paucal, such as 2 to 4 in Czech and 3 to 10 in Arabic.
     */
    FEW,
    /**
     * The form for many, such as 5 to 20 in Russian and 11 to 99 in Arabic.
     */
    MANY,
    /**
     * Every count the other categories leave, and the only category of languages without plurals.
     */
    OTHER;

    /**
     * Returns the suffix a translation key takes for this category, the name in lower case.
     *
     * @return {@code zero}, {@code one}, {@code two}, {@code few}, {@code many} or {@code other}
     */
    public String suffix() {
        return switch (this) {
            case ZERO -> "zero";
            case ONE -> "one";
            case TWO -> "two";
            case FEW -> "few";
            case MANY -> "many";
            case OTHER -> "other";
        };
    }

    /**
     * Returns the category of a whole count in a language, by Strata's cardinal rules from the Unicode CLDR. A
     * negative count takes the category of its absolute value. A language Strata has no rule for uses English's.
     *
     * @param locale the language
     * @param count the count
     * @return the category
     */
    public static PluralCategory of(final Locale locale, final long count) {
        return Strata.texts().pluralCategory(locale, count);
    }
}
