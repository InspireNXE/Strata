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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Numbers as the translator's language writes them, for the spinner and the number converters, so the two show and
 * read a number the same way.
 *
 * <p>A number is written with the locale's digits, its decimal separator and its minus sign, without grouping and
 * without the marks some locales put around a sign. Reading takes the locale's digits and ASCII digits alike, the
 * locale's minus sign or an ASCII hyphen, and the locale's decimal separator; with {@code grouping} it also skips the
 * locale's grouping separator, and without it a {@code .} is a decimal separator in every language, since numeric
 * keypads type one.</p>
 *
 * <p>Instances are immutable. {@link #current()} keeps the last one and makes another only when the translator's
 * locale or the translations' version changed, so controls can call it from every measure.</p>
 */
public final class Numbers {
    private static final Pattern PLAIN = Pattern.compile("-?(\\d+(\\.\\d*)?|\\.\\d+)");
    private static volatile Numbers cached;

    private final Locale locale;
    private final long version;
    private final char zero;
    private final char separator;
    private final char minus;
    private final char grouping;
    private final String infinity;

    private Numbers(final Locale locale, final long version) {
        this.locale = locale;
        this.version = version;
        final DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(locale);
        this.zero = symbols.getZeroDigit();
        this.separator = symbols.getDecimalSeparator();
        this.minus = symbols.getMinusSign();
        this.grouping = symbols.getGroupingSeparator();
        this.infinity = symbols.getInfinity();
    }

    /**
     * Returns the numbers of the translator's locale, made again only when the locale or the translations' version
     * changed. A translator with no locale gives {@link Locale#ROOT}'s.
     *
     * @return the numbers
     */
    public static Numbers current() {
        final long version = Translations.version();
        final Locale given = Translations.translator().locale();
        final Locale locale = given == null ? Locale.ROOT : given;
        final Numbers last = Numbers.cached;
        if (last != null && last.version == version && last.locale.equals(locale)) {
            return last;
        }
        final Numbers made = new Numbers(locale, version);
        Numbers.cached = made;
        return made;
    }

    /**
     * Returns the numbers of a locale, uncached.
     *
     * @param locale the locale
     * @return the numbers
     */
    public static Numbers of(final Locale locale) {
        return new Numbers(Objects.requireNonNull(locale, "locale"), Long.MIN_VALUE);
    }

    /**
     * Returns the locale.
     *
     * @return the locale
     */
    public Locale locale() {
        return this.locale;
    }

    /**
     * Returns the locale's decimal separator.
     *
     * @return the separator
     */
    public char separator() {
        return this.separator;
    }

    /**
     * Returns the locale's minus sign.
     *
     * @return the sign
     */
    public char minus() {
        return this.minus;
    }

    /**
     * Returns the locale's zero, the first of its ten digits.
     *
     * @return the zero digit
     */
    public char zero() {
        return this.zero;
    }

    /**
     * Writes a number with exactly {@code decimals} digits after the separator, rounding half up. Negative zero is
     * written as zero.
     *
     * @param value the number
     * @param decimals the digits after the separator, 0 or more
     * @return the text
     */
    public String format(final double value, final int decimals) {
        return this.format(value, decimals, decimals);
    }

    /**
     * Writes a number with at least {@code minDecimals} and at most {@code maxDecimals} digits after the separator,
     * rounding half up and dropping trailing zeros down to the minimum. Negative zero is written as zero, and an
     * infinity as the locale's infinity sign.
     *
     * @param value the number, not NaN
     * @param minDecimals the fewest digits after the separator
     * @param maxDecimals the most digits after the separator, at least {@code minDecimals}
     * @return the text
     */
    public String format(final double value, final int minDecimals, final int maxDecimals) {
        if (Double.isInfinite(value)) {
            return value < 0d ? this.minus + this.infinity : this.infinity;
        }
        final BigDecimal rounded = BigDecimal.valueOf(value).setScale(maxDecimals, RoundingMode.HALF_UP)
                .stripTrailingZeros();
        // BigDecimal has no negative zero, so -0.0 comes out as 0
        final BigDecimal number = rounded.setScale(Math.max(minDecimals, rounded.scale()), RoundingMode.UNNECESSARY);
        return this.localize(number.toPlainString());
    }

    /**
     * Turns a number written in ASCII, with {@code -} and {@code .}, into the locale's digits and signs.
     */
    private String localize(final String plain) {
        final StringBuilder out = new StringBuilder(plain.length());
        for (int i = 0; i < plain.length(); i++) {
            final char c = plain.charAt(i);
            if (c >= '0' && c <= '9') {
                out.append((char) (this.zero + (c - '0')));
            } else if (c == '.') {
                out.append(this.separator);
            } else if (c == '-') {
                out.append(this.minus);
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    /**
     * Reads the whole of a text, spaces around it aside, as a number.
     *
     * @param text the text
     * @param grouping whether the locale's grouping separator may appear, and is skipped; without it a {@code .}
     *     is a decimal separator in every language
     * @return the number, or null if the text is empty or not wholly a number
     */
    public BigDecimal parse(final String text, final boolean grouping) {
        final String ascii = this.toAscii(text.strip(), grouping);
        if (ascii == null || !PLAIN.matcher(ascii).matches()) {
            return null;
        }
        return new BigDecimal(ascii.endsWith(".") ? ascii.substring(0, ascii.length() - 1) : ascii);
    }

    /**
     * Turns the locale's digits and signs into ASCII, or returns null at a character no number holds. Bidi marks
     * are dropped, and so is a leading plus sign.
     */
    private String toAscii(final String text, final boolean grouping) {
        final StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            final char c = text.charAt(i);
            if (c >= '0' && c <= '9') {
                out.append(c);
            } else if (c >= this.zero && c <= this.zero + 9) {
                out.append((char) ('0' + (c - this.zero)));
            } else if (c == this.separator) {
                out.append('.');
            } else if (grouping && (c == this.grouping
                    || Character.isSpaceChar(c) && Character.isSpaceChar(this.grouping))) {
                continue;
            } else if (!grouping && c == '.') {
                out.append('.');
            } else if (c == this.minus || c == '-' || c == '−') {
                out.append('-');
            } else if (c == '+' && out.isEmpty()) {
                continue;
            } else if (c == '‎' || c == '‏' || c == '؜') {
                continue;
            } else {
                return null;
            }
        }
        return out.toString();
    }

    /**
     * Returns whether a char is one of the digits a number may be typed with: ASCII or the locale's.
     *
     * @param c the char
     * @return whether it is a digit
     */
    public boolean isDigit(final char c) {
        return c >= '0' && c <= '9' || c >= this.zero && c <= this.zero + 9;
    }
}
