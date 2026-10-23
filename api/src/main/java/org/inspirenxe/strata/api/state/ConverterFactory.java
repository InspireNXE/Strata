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

/**
 * Makes the number {@link Converter}s, which read the translator's locale. {@link Converters} calls the factory that
 * {@link org.inspirenxe.strata.api.Strata} finds, so code rarely uses this interface directly. Implementations may be
 * called from any thread.
 *
 * <p>Strata's core implements this interface. Do not implement it.</p>
 */
public interface ConverterFactory {

    /**
     * See {@link Converters#integer(int, int)}; with both ends at the limits of {@code int} it is
     * {@link Converters#integer()}.
     *
     * @param min the lowest value accepted
     * @param max the highest value accepted
     * @return the converter
     * @throws IllegalArgumentException if {@code min} is greater than {@code max}
     */
    Converter<Integer, String> integer(int min, int max);

    /**
     * See {@link Converters#decimal(double, double, int)}; with infinite ends it is {@link Converters#decimal(int)}.
     *
     * @param min the lowest value accepted, or negative infinity for no limit
     * @param max the highest value accepted, or positive infinity for no limit
     * @param decimals how many digits to show and keep after the decimal separator
     * @return the converter
     * @throws IllegalArgumentException if {@code min} is greater than {@code max}, either is NaN, or
     *     {@code decimals} is negative or above 15
     */
    Converter<Double, String> decimal(double min, double max, int decimals);
}
