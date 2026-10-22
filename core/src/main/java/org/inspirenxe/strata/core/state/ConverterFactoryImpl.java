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
package org.inspirenxe.strata.core.state;

import org.inspirenxe.strata.api.state.Conversion;
import org.inspirenxe.strata.api.state.Converter;
import org.inspirenxe.strata.api.state.ConverterFactory;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.core.text.Translations;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Locale;

/**
 * Core's {@link ConverterFactory}. Its converters read the locale of the current translator on every call, so a
 * change of language applies to the next conversion. They format with a {@link DecimalFormat} of the locale without
 * grouping, and parse with one that accepts grouping, the whole text after stripping spaces, as a
 * {@link BigDecimal}, so a long number is not rounded through a {@code double} before it is checked.
 */
public final class ConverterFactoryImpl implements ConverterFactory {
    /**
     * The one factory.
     */
    public static final ConverterFactoryImpl INSTANCE = new ConverterFactoryImpl();
    /**
     * The most digits after the separator a decimal converter keeps.
     */
    public static final int MAX_DECIMALS = 15;

    private ConverterFactoryImpl() {
    }

    @Override
    public Converter<Integer, String> integer(final int min, final int max) {
        if (min > max) {
            throw new IllegalArgumentException("Min " + min + " is greater than max " + max);
        }
        return new IntegerConverter(min, max);
    }

    @Override
    public Converter<Double, String> decimal(final double min, final double max, final int decimals) {
        if (Double.isNaN(min) || Double.isNaN(max) || min > max) {
            throw new IllegalArgumentException("Not a range: " + min + " to " + max);
        }
        if (decimals < 0 || decimals > MAX_DECIMALS) {
            throw new IllegalArgumentException("Decimals must be from 0 to " + MAX_DECIMALS + ": " + decimals);
        }
        return new DecimalConverter(min, max, decimals);
    }

    private static Locale locale() {
        final Locale locale = Translations.translator().locale();
        return locale == null ? Locale.ROOT : locale;
    }

    private static DecimalFormat format(final Locale locale) {
        final NumberFormat format = NumberFormat.getNumberInstance(locale);
        return format instanceof final DecimalFormat decimal ? decimal
                : new DecimalFormat("#,##0.###", DecimalFormatSymbols.getInstance(locale));
    }

    /**
     * Formats a number without grouping, with between {@code minDecimals} and {@code maxDecimals} digits after the
     * separator, rounding half up.
     */
    private static String print(final Locale locale, final double value, final int minDecimals,
                                final int maxDecimals) {
        final DecimalFormat format = format(locale);
        format.setGroupingUsed(false);
        format.setMinimumFractionDigits(minDecimals);
        format.setMaximumFractionDigits(maxDecimals);
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format.format(value);
    }

    /**
     * Parses the whole of a text, spaces around it aside, as a number in a locale.
     *
     * @return the number, or null if the text is empty or not wholly a finite number
     */
    static BigDecimal parse(final String text, final Locale locale) {
        final String stripped = text.strip();
        if (stripped.isEmpty()) {
            return null;
        }
        final DecimalFormat format = format(locale);
        format.setParseBigDecimal(true);
        final ParsePosition position = new ParsePosition(0);
        final Number number = format.parse(stripped, position);
        if (position.getIndex() != stripped.length() || position.getErrorIndex() >= 0) {
            return null;
        }
        // Infinity and NaN come back as a Double
        return number instanceof final BigDecimal decimal ? decimal : null;
    }

    /**
     * Whole numbers from {@code min} to {@code max}.
     */
    private record IntegerConverter(int min, int max) implements Converter<Integer, String> {
        @Override
        public String toView(final Integer model) {
            return model == null ? "" : print(locale(), model, 0, 0);
        }

        @Override
        public Conversion<Integer> toModel(final String view) {
            final Locale locale = locale();
            final BigDecimal number = view == null ? null : parse(view, locale);
            if (number == null || number.signum() != 0 && number.stripTrailingZeros().scale() > 0
                    || number.compareTo(BigDecimal.valueOf(this.min)) < 0
                    || number.compareTo(BigDecimal.valueOf(this.max)) > 0) {
                return Conversion.invalid(this.message(locale));
            }
            return Conversion.valid(number.intValueExact());
        }

        private Text message(final Locale locale) {
            if (this.min == Integer.MIN_VALUE && this.max == Integer.MAX_VALUE) {
                return Text.translatable("strata.validation.integer");
            }
            return Text.translatable("strata.validation.integer_range", print(locale, this.min, 0, 0),
                    print(locale, this.max, 0, 0));
        }
    }

    /**
     * Finite numbers from {@code min} to {@code max}, kept to {@code decimals} digits after the separator.
     */
    private record DecimalConverter(double min, double max, int decimals) implements Converter<Double, String> {
        @Override
        public String toView(final Double model) {
            return model == null ? "" : print(locale(), model, this.decimals, this.decimals);
        }

        @Override
        public Conversion<Double> toModel(final String view) {
            final Locale locale = locale();
            final BigDecimal number = view == null ? null : parse(view, locale);
            if (number == null) {
                return Conversion.invalid(this.message(locale));
            }
            final double value = number.setScale(this.decimals, RoundingMode.HALF_UP).doubleValue();
            if (!Double.isFinite(value) || value < this.min || value > this.max) {
                return Conversion.invalid(this.message(locale));
            }
            // No negative zero, so "-0" and "0" are the same model value
            return Conversion.valid(value == 0d ? 0d : value);
        }

        private Text message(final Locale locale) {
            if (this.min == Double.NEGATIVE_INFINITY && this.max == Double.POSITIVE_INFINITY) {
                return Text.translatable("strata.validation.decimal");
            }
            return Text.translatable("strata.validation.decimal_range", print(locale, this.min, 0, this.decimals),
                    print(locale, this.max, 0, this.decimals));
        }
    }
}
