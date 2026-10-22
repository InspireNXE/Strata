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
package org.inspirenxe.strata.api.state;

import org.inspirenxe.strata.api.Strata;

import java.util.Objects;
import java.util.function.Function;

/**
 * Common {@link Converter}s, for binding a field's text to a state of another type with {@link PropertyKey#convert}.
 *
 * <pre>{@code
 * quantity.bind(TextInput.TEXT.convert(Converters.integer(1, 64)), order.quantity);
 * price.bind(TextInput.TEXT.convert(Converters.decimal(0, 1_000_000, 2)), order.price);
 * }</pre>
 *
 * <p>The number converters format and parse in the locale of the platform's translator, read on each call, so "2,5"
 * is two and a half where the decimal separator is a comma. They format as a
 * {@link org.inspirenxe.strata.api.control.field.Spinner} shows its value, in the language's digits and signs without
 * grouping, and parse with or without grouping, ignore spaces around the number, and turn a null model into empty text. Their messages are the translation keys
 * {@code strata.validation.integer}, {@code strata.validation.integer_range}, {@code strata.validation.decimal} and
 * {@code strata.validation.decimal_range}, which have English in Strata's core, such as "Enter a whole number from 1
 * to 64".</p>
 */
public final class Converters {

    private Converters() {
    }

    /**
     * Returns a converter between whole numbers and text that takes any {@code int}. Empty text, text that is not a
     * whole number and numbers outside the range of {@code int} are invalid, with the message "Enter a whole number".
     *
     * @return the converter
     * @throws IllegalStateException if Strata's core is not on the class path
     */
    public static Converter<Integer, String> integer() {
        return Strata.converters().integer(Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    /**
     * Returns a converter between whole numbers and text that takes a range. Empty text, text that is not a whole
     * number and numbers outside the range are invalid, with the message "Enter a whole number from {@code min} to
     * {@code max}".
     *
     * @param min the lowest value accepted
     * @param max the highest value accepted
     * @return the converter
     * @throws IllegalArgumentException if {@code min} is greater than {@code max}
     * @throws IllegalStateException if Strata's core is not on the class path
     */
    public static Converter<Integer, String> integer(final int min, final int max) {
        return Strata.converters().integer(min, max);
    }

    /**
     * Returns a converter between numbers and text that takes any finite number. The model's value is shown with
     * exactly {@code decimals} digits after the separator, and parsed text is rounded half up to that many. Empty text
     * and text that is not a number are invalid, with the message "Enter a number".
     *
     * @param decimals how many digits to show and keep after the decimal separator, from 0 to 15
     * @return the converter
     * @throws IllegalArgumentException if {@code decimals} is negative or above 15
     * @throws IllegalStateException if Strata's core is not on the class path
     */
    public static Converter<Double, String> decimal(final int decimals) {
        return Strata.converters().decimal(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, decimals);
    }

    /**
     * Returns a converter between numbers and text that takes a range, as {@link #decimal(int)} does. A number
     * outside the range after rounding is invalid too, and every invalid text has the message "Enter a number from
     * {@code min} to {@code max}".
     *
     * @param min the lowest value accepted
     * @param max the highest value accepted
     * @param decimals how many digits to show and keep after the decimal separator, from 0 to 15
     * @return the converter
     * @throws IllegalArgumentException if {@code min} is greater than {@code max}, either is NaN, or
     *     {@code decimals} is negative or above 15
     * @throws IllegalStateException if Strata's core is not on the class path
     */
    public static Converter<Double, String> decimal(final double min, final double max, final int decimals) {
        return Strata.converters().decimal(min, max, decimals);
    }

    /**
     * Returns a converter from two functions, which is never invalid. Use it where every value of the control has a
     * value in the model, such as a combo box's item and the id the model keeps.
     *
     * <pre>{@code
     * final Converter<String, Text> name = Converters.of(Text::of, Text::plain);
     * }</pre>
     *
     * @param <M> the type of the model's value
     * @param <V> the type of the control's value
     * @param toView turns the model's value into the control's
     * @param toModel turns the control's value into the model's
     * @return the converter
     */
    public static <M, V> Converter<M, V> of(final Function<? super M, ? extends V> toView,
                                            final Function<? super V, ? extends M> toModel) {
        Objects.requireNonNull(toView, "toView");
        Objects.requireNonNull(toModel, "toModel");
        return new Converter<>() {
            @Override
            public V toView(final M model) {
                return toView.apply(model);
            }

            @Override
            public Conversion<M> toModel(final V view) {
                return Conversion.valid(toModel.apply(view));
            }
        };
    }
}
