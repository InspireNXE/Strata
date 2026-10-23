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
package org.inspirenxe.strata.core.text;

import org.inspirenxe.strata.api.text.PluralCategory;

import java.util.Locale;

/**
 * The cardinal plural rules of the Unicode CLDR for whole numbers, the part of them that counts need. The JDK has no
 * public plural rules, so core carries this table. Each language is matched by its ISO code, with Hebrew's old
 * {@code iw} and the {@code pt_PT} region as the only special cases, and a language with no rule here uses English's:
 * {@link PluralCategory#ONE} for 1 and {@link PluralCategory#OTHER} for the rest. The categories that only fractions
 * reach, such as Czech's {@code many}, never come out.
 */
public final class PluralRules {

    private PluralRules() {
    }

    /**
     * Returns the category of a count in a language.
     *
     * @param locale the language
     * @param count the count; a negative count takes the category of its absolute value
     * @return the category
     */
    public static PluralCategory select(final Locale locale, final long count) {
        // Long.MIN_VALUE has no absolute value, and MAX_VALUE falls in the same category in every rule here
        final long n = count == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(count);
        final long n10 = n % 10L;
        final long n100 = n % 100L;
        return switch (locale.getLanguage()) {
            // No plural forms
            case "ja", "zh", "ko", "th", "vi", "id", "ms", "lo", "my", "km", "yo", "ig", "jv", "su", "bo", "dz",
                 "ii", "kea", "ses", "sg", "to", "wo", "yue", "sah", "kde", "nqo", "osa", "jbo" -> PluralCategory.OTHER;
            // 0 and 1 are one, and whole millions are many
            case "fr" -> n <= 1L ? PluralCategory.ONE : millions(n);
            case "pt" -> "PT".equals(locale.getCountry())
                    ? n == 1L ? PluralCategory.ONE : millions(n)
                    : n <= 1L ? PluralCategory.ONE : millions(n);
            // 1 is one, and whole millions are many
            case "es", "it", "ca" -> n == 1L ? PluralCategory.ONE : millions(n);
            // 0 and 1 are one
            case "hy", "ff", "kab", "am", "as", "bn", "doi", "fa", "gu", "hi", "kn", "pcm", "zu", "ak", "bho",
                 "guw", "ln", "mg", "nso", "pa", "ti", "wa", "si" -> n <= 1L ? PluralCategory.ONE : PluralCategory.OTHER;
            // Ends in 1 but not in 11
            case "is", "mk" -> n10 == 1L && n100 != 11L ? PluralCategory.ONE : PluralCategory.OTHER;
            case "ru", "uk", "be" -> {
                if (n10 == 1L && n100 != 11L) {
                    yield PluralCategory.ONE;
                }
                yield n10 >= 2L && n10 <= 4L && (n100 < 12L || n100 > 14L) ? PluralCategory.FEW : PluralCategory.MANY;
            }
            case "pl" -> {
                if (n == 1L) {
                    yield PluralCategory.ONE;
                }
                yield n10 >= 2L && n10 <= 4L && (n100 < 12L || n100 > 14L) ? PluralCategory.FEW : PluralCategory.MANY;
            }
            case "cs", "sk" -> n == 1L ? PluralCategory.ONE
                    : n >= 2L && n <= 4L ? PluralCategory.FEW : PluralCategory.OTHER;
            case "hr", "sr", "bs", "sh" -> {
                if (n10 == 1L && n100 != 11L) {
                    yield PluralCategory.ONE;
                }
                yield n10 >= 2L && n10 <= 4L && (n100 < 12L || n100 > 14L) ? PluralCategory.FEW : PluralCategory.OTHER;
            }
            case "sl" -> switch ((int) n100) {
                case 1 -> PluralCategory.ONE;
                case 2 -> PluralCategory.TWO;
                case 3, 4 -> PluralCategory.FEW;
                default -> PluralCategory.OTHER;
            };
            case "lt" -> {
                if (n100 >= 11L && n100 <= 19L) {
                    yield PluralCategory.OTHER;
                }
                yield n10 == 1L ? PluralCategory.ONE : n10 >= 2L ? PluralCategory.FEW : PluralCategory.OTHER;
            }
            case "lv", "prg" -> {
                if (n10 == 0L || n100 >= 11L && n100 <= 19L) {
                    yield PluralCategory.ZERO;
                }
                yield n10 == 1L ? PluralCategory.ONE : PluralCategory.OTHER;
            }
            case "ro", "mo" -> {
                if (n == 1L) {
                    yield PluralCategory.ONE;
                }
                yield n == 0L || n100 >= 1L && n100 <= 19L ? PluralCategory.FEW : PluralCategory.OTHER;
            }
            case "ar", "ars" -> {
                if (n <= 2L) {
                    yield n == 0L ? PluralCategory.ZERO : n == 1L ? PluralCategory.ONE : PluralCategory.TWO;
                }
                yield n100 >= 3L && n100 <= 10L ? PluralCategory.FEW
                        : n100 >= 11L ? PluralCategory.MANY : PluralCategory.OTHER;
            }
            case "he", "iw" -> n == 1L ? PluralCategory.ONE : n == 2L ? PluralCategory.TWO : PluralCategory.OTHER;
            case "ga" -> {
                if (n == 1L) {
                    yield PluralCategory.ONE;
                }
                if (n == 2L) {
                    yield PluralCategory.TWO;
                }
                yield n >= 3L && n <= 6L ? PluralCategory.FEW
                        : n >= 7L && n <= 10L ? PluralCategory.MANY : PluralCategory.OTHER;
            }
            case "gd" -> {
                if (n == 1L || n == 11L) {
                    yield PluralCategory.ONE;
                }
                if (n == 2L || n == 12L) {
                    yield PluralCategory.TWO;
                }
                yield n >= 3L && n <= 19L ? PluralCategory.FEW : PluralCategory.OTHER;
            }
            case "cy" -> switch ((int) Math.min(n, 7L)) {
                case 0 -> PluralCategory.ZERO;
                case 1 -> PluralCategory.ONE;
                case 2 -> PluralCategory.TWO;
                case 3 -> PluralCategory.FEW;
                case 6 -> PluralCategory.MANY;
                default -> PluralCategory.OTHER;
            };
            case "mt" -> {
                if (n == 1L) {
                    yield PluralCategory.ONE;
                }
                if (n == 2L) {
                    yield PluralCategory.TWO;
                }
                yield n == 0L || n100 >= 3L && n100 <= 10L ? PluralCategory.FEW
                        : n100 >= 11L && n100 <= 19L ? PluralCategory.MANY : PluralCategory.OTHER;
            }
            default -> n == 1L ? PluralCategory.ONE : PluralCategory.OTHER;
        };
    }

    /**
     * Many for a whole number of millions, other for the rest.
     */
    private static PluralCategory millions(final long n) {
        return n != 0L && n % 1_000_000L == 0L ? PluralCategory.MANY : PluralCategory.OTHER;
    }
}
