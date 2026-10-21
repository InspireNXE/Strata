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

import org.inspirenxe.strata.spi.Translator;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.Month;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * What dates and times look like in the current language: month and weekday names, the first day of the week, the
 * order and separator of a date's parts, the 12 or 24 hour clock, and the names of the halves of the day.
 *
 * <p>Each piece is read from the translator first, under the keys below, and if it has none, from {@code java.time}'s
 * data for the translator's {@linkplain Translator#locale() locale}. Unlike other translated text, these keys skip
 * {@link Translations#DEFAULTS}, whose English would hide the locale's data, and a missing key is not logged. A value
 * that does not parse is ignored and the locale's data is used.</p>
 * <ul>
 *     <li>{@code strata.date.month.1} to {@code .12}: full month names, from
 *     {@link Month#getDisplayName} in the stand-alone full style.</li>
 *     <li>{@code strata.date.month.short.1} to {@code .12}: short month names, in the stand-alone short style.</li>
 *     <li>{@code strata.date.weekday.short.1} to {@code .7}: short weekday names, Monday being 1 as in
 *     {@link DayOfWeek}, in the stand-alone short style.</li>
 *     <li>{@code strata.date.first_weekday}: the first day of the week as a number from 1 to 7, from
 *     {@link WeekFields#of(Locale)}.</li>
 *     <li>{@code strata.date.order}: {@code dmy}, {@code mdy} or {@code ymd} (any order of the three letters is
 *     taken), from the locale's short date pattern.</li>
 *     <li>{@code strata.date.separator}: what goes between a date's parts, from the short date pattern, such as
 *     {@code /} or {@code .}; {@code /} when the pattern has none.</li>
 *     <li>{@code strata.time.clock}: {@code 12} or {@code 24}, from whether the short time pattern counts hours to
 *     12.</li>
 *     <li>{@code strata.time.am} and {@code strata.time.pm}: the names of the halves of the day, from the {@code a}
 *     pattern.</li>
 * </ul>
 * <p>The separator between hours, minutes and seconds comes from the short time pattern, {@code :} when it has none,
 * and has no key.</p>
 *
 * <p>{@link #current()} keeps the last result and makes a new one only when the locale or
 * {@link Translations#version()} changed, so controls can call it from every measure. Instances are immutable and may
 * be used from any thread.</p>
 */
public final class DateFormats {
    private static volatile DateFormats cached;

    private final Locale locale;
    private final long version;
    private final String[] months;
    private final String[] shortMonths;
    private final String[] weekdays;
    private final DayOfWeek firstDayOfWeek;
    private final List<Part> order;
    private final String separator;
    private final String timeSeparator;
    private final boolean twelveHour;
    private final String am;
    private final String pm;

    private DateFormats(final Translator translator, final Locale locale, final long version) {
        this.locale = locale;
        this.version = version;
        this.months = new String[12];
        this.shortMonths = new String[12];
        for (final Month month : Month.values()) {
            final int index = month.getValue();
            this.months[index - 1] = DateFormats.key(translator, "strata.date.month." + index)
                    .orElseGet(() -> DateFormats.name(month, TextStyle.FULL_STANDALONE, TextStyle.FULL, locale));
            this.shortMonths[index - 1] = DateFormats.key(translator, "strata.date.month.short." + index)
                    .orElseGet(() -> DateFormats.name(month, TextStyle.SHORT_STANDALONE, TextStyle.SHORT, locale));
        }
        this.weekdays = new String[7];
        for (final DayOfWeek day : DayOfWeek.values()) {
            final int index = day.getValue();
            this.weekdays[index - 1] = DateFormats.key(translator, "strata.date.weekday.short." + index)
                    .orElseGet(() -> {
                        final String standalone = day.getDisplayName(TextStyle.SHORT_STANDALONE, locale);
                        return DateFormats.isNumber(standalone) ? day.getDisplayName(TextStyle.SHORT, locale) : standalone;
                    });
        }
        this.firstDayOfWeek = DateFormats.key(translator, "strata.date.first_weekday")
                .flatMap(DateFormats::weekday)
                .orElseGet(() -> WeekFields.of(locale).getFirstDayOfWeek());

        final String datePattern = DateTimeFormatterBuilder.getLocalizedDateTimePattern(FormatStyle.SHORT, null,
                IsoChronology.INSTANCE, locale);
        this.order = DateFormats.key(translator, "strata.date.order")
                .flatMap(DateFormats::order)
                .orElseGet(() -> DateFormats.patternOrder(datePattern));
        this.separator = DateFormats.key(translator, "strata.date.separator")
                .filter(value -> !value.isEmpty())
                .orElseGet(() -> DateFormats.patternSeparator(datePattern, "dMLyuY", "/"));

        final String timePattern = DateTimeFormatterBuilder.getLocalizedDateTimePattern(null, FormatStyle.SHORT,
                IsoChronology.INSTANCE, locale);
        this.twelveHour = DateFormats.key(translator, "strata.time.clock")
                .map(String::trim)
                .filter(value -> value.equals("12") || value.equals("24"))
                .map(value -> value.equals("12"))
                .orElseGet(() -> DateFormats.unquoted(timePattern).indexOf('h') >= 0
                        || DateFormats.unquoted(timePattern).indexOf('K') >= 0);
        this.timeSeparator = DateFormats.patternSeparator(timePattern, "hHkKms", ":");
        final DateTimeFormatter half = DateTimeFormatter.ofPattern("a", locale);
        this.am = DateFormats.key(translator, "strata.time.am").filter(value -> !value.isBlank())
                .orElseGet(() -> half.format(LocalTime.of(9, 0)));
        this.pm = DateFormats.key(translator, "strata.time.pm").filter(value -> !value.isBlank())
                .orElseGet(() -> half.format(LocalTime.of(21, 0)));
    }

    /**
     * Returns the formats for the current translator, made again only when its locale or the translations' version
     * changed.
     *
     * @return the formats
     */
    public static DateFormats current() {
        final Translator translator = Translations.translator();
        final long version = Translations.version();
        final Locale locale = Objects.requireNonNull(translator.locale(), "The translator's locale");
        final DateFormats last = DateFormats.cached;
        if (last != null && last.version == version && last.locale.equals(locale)) {
            return last;
        }
        final DateFormats made = new DateFormats(translator, locale, version);
        DateFormats.cached = made;
        return made;
    }

    /**
     * Makes the formats a translator gives, without caching them.
     *
     * @param translator the translator whose keys and locale to read
     * @return the formats
     */
    public static DateFormats of(final Translator translator) {
        return new DateFormats(translator, Objects.requireNonNull(translator.locale(), "The translator's locale"),
                translator.version());
    }

    /**
     * Returns the locale the formats were read for.
     *
     * @return the locale
     */
    public Locale locale() {
        return this.locale;
    }

    /**
     * Returns a month's full name.
     *
     * @param month the month
     * @return the name, such as {@code October}
     */
    public String monthName(final Month month) {
        return this.months[month.getValue() - 1];
    }

    /**
     * Returns a month's short name.
     *
     * @param month the month
     * @return the name, such as {@code Oct}
     */
    public String shortMonthName(final Month month) {
        return this.shortMonths[month.getValue() - 1];
    }

    /**
     * Returns a weekday's short name.
     *
     * @param day the day
     * @return the name, such as {@code Tue}
     */
    public String shortWeekdayName(final DayOfWeek day) {
        return this.weekdays[day.getValue() - 1];
    }

    /**
     * Returns the day weeks start on.
     *
     * @return the day
     */
    public DayOfWeek firstDayOfWeek() {
        return this.firstDayOfWeek;
    }

    /**
     * Returns the order of a date's parts.
     *
     * @return the day, the month and the year, each once, in the order they show
     */
    public List<Part> order() {
        return this.order;
    }

    /**
     * Returns what goes between a date's parts.
     *
     * @return the separator, never empty
     */
    public String separator() {
        return this.separator;
    }

    /**
     * Returns what goes between hours, minutes and seconds.
     *
     * @return the separator, never empty
     */
    public String timeSeparator() {
        return this.timeSeparator;
    }

    /**
     * Returns whether hours count to 12.
     *
     * @return true for a 12 hour clock, false for 24
     */
    public boolean isTwelveHour() {
        return this.twelveHour;
    }

    /**
     * Returns the name of the first half of the day.
     *
     * @return the name, such as {@code AM}
     */
    public String am() {
        return this.am;
    }

    /**
     * Returns the name of the second half of the day.
     *
     * @return the name, such as {@code PM}
     */
    public String pm() {
        return this.pm;
    }

    /**
     * A part of a date.
     */
    public enum Part {
        /**
         * The day of the month.
         */
        DAY,
        /**
         * The month.
         */
        MONTH,
        /**
         * The year.
         */
        YEAR
    }

    private static Optional<String> key(final Translator translator, final String key) {
        return translator.translate(key).filter(value -> !value.isEmpty());
    }

    private static String name(final Month month, final TextStyle style, final TextStyle fallback, final Locale locale) {
        final String name = month.getDisplayName(style, locale);
        return DateFormats.isNumber(name) ? month.getDisplayName(fallback, locale) : name;
    }

    private static boolean isNumber(final String text) {
        return !text.isEmpty() && text.chars().allMatch(Character::isDigit);
    }

    private static Optional<DayOfWeek> weekday(final String value) {
        try {
            final int day = Integer.parseInt(value.trim());
            return day >= 1 && day <= 7 ? Optional.of(DayOfWeek.of(day)) : Optional.empty();
        } catch (final NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    private static Optional<List<Part>> order(final String value) {
        final String letters = value.trim().toLowerCase(Locale.ROOT);
        if (letters.length() != 3 || letters.indexOf('d') < 0 || letters.indexOf('m') < 0 || letters.indexOf('y') < 0) {
            return Optional.empty();
        }
        final Part[] parts = new Part[3];
        for (int i = 0; i < 3; i++) {
            parts[i] = switch (letters.charAt(i)) {
                case 'd' -> Part.DAY;
                case 'm' -> Part.MONTH;
                default -> Part.YEAR;
            };
        }
        return Optional.of(List.of(parts));
    }

    /**
     * Returns the pattern with quoted text replaced by spaces, so letters inside quotes are not taken for fields.
     */
    private static String unquoted(final String pattern) {
        final StringBuilder out = new StringBuilder(pattern.length());
        boolean quoted = false;
        for (int i = 0; i < pattern.length(); i++) {
            final char c = pattern.charAt(i);
            if (c == '\'') {
                quoted = !quoted;
                out.append(' ');
            } else {
                out.append(quoted ? ' ' : c);
            }
        }
        return out.toString();
    }

    private static List<Part> patternOrder(final String pattern) {
        final String plain = DateFormats.unquoted(pattern);
        final int day = DateFormats.firstOf(plain, "d");
        final int month = DateFormats.firstOf(plain, "ML");
        final int year = DateFormats.firstOf(plain, "yuY");
        if (day < 0 || month < 0 || year < 0) {
            return List.of(Part.DAY, Part.MONTH, Part.YEAR);
        }
        if (year < month && year < day) {
            return month < day ? List.of(Part.YEAR, Part.MONTH, Part.DAY) : List.of(Part.YEAR, Part.DAY, Part.MONTH);
        }
        if (month < day) {
            return year < day ? List.of(Part.MONTH, Part.YEAR, Part.DAY) : List.of(Part.MONTH, Part.DAY, Part.YEAR);
        }
        return year < month ? List.of(Part.DAY, Part.YEAR, Part.MONTH) : List.of(Part.DAY, Part.MONTH, Part.YEAR);
    }

    private static int firstOf(final String pattern, final String letters) {
        for (int i = 0; i < pattern.length(); i++) {
            if (letters.indexOf(pattern.charAt(i)) >= 0) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the text between the first field of a pattern and the next, trimmed, or a space when only spaces are
     * there, or the fallback when nothing is.
     */
    private static String patternSeparator(final String pattern, final String fields, final String fallback) {
        final String plain = DateFormats.unquoted(pattern);
        final int start = DateFormats.firstOf(plain, fields);
        if (start < 0) {
            return fallback;
        }
        int end = start;
        while (end < plain.length() && plain.charAt(end) == plain.charAt(start)) {
            end++;
        }
        int next = end;
        while (next < plain.length() && fields.indexOf(plain.charAt(next)) < 0
                && !Character.isLetter(plain.charAt(next))) {
            next++;
        }
        if (next >= plain.length() || fields.indexOf(plain.charAt(next)) < 0) {
            return fallback;
        }
        // Take the separator from the pattern itself, so quoted literals keep their text
        final String between = pattern.substring(end, next).replace("'", "");
        final String trimmed = between.strip();
        if (!trimmed.isEmpty()) {
            return trimmed;
        }
        return between.isEmpty() ? fallback : " ";
    }
}
