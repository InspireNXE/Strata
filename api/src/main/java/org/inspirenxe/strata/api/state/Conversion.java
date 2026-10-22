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

import org.inspirenxe.strata.api.text.Text;

import java.util.Objects;

/**
 * What a {@link Converter} made of a control's value: the model's value, or why there is none, such as "abc" in a
 * field for a quantity.
 *
 * <pre>{@code
 * switch (converter.toModel(field.text())) {
 *     case Conversion.Valid<Integer>(Integer quantity) -> order.setQuantity(quantity);
 *     case Conversion.Invalid<Integer>(Text message) -> hint.setText(message);
 * }
 * }</pre>
 *
 * @param <M> the type of the model's value
 */
public sealed interface Conversion<M> {

    /**
     * Returns a valid conversion.
     *
     * @param <M> the type of the model's value
     * @param value the model's value; may be null where the model accepts null
     * @return the conversion
     */
    static <M> Conversion<M> valid(final M value) {
        return new Valid<>(value);
    }

    /**
     * Returns a failed conversion.
     *
     * @param <M> the type the model's value would have had
     * @param message why the value cannot be converted, as the user should read it
     * @return the conversion
     */
    static <M> Conversion<M> invalid(final Text message) {
        return new Invalid<>(message);
    }

    /**
     * Returns whether the conversion gave a value.
     *
     * @return true for {@link Valid}
     */
    default boolean isValid() {
        return this instanceof Valid<M>;
    }

    /**
     * A conversion that gave a value.
     *
     * @param <M> the type of the model's value
     * @param value the model's value; may be null where the model accepts null
     */
    record Valid<M>(M value) implements Conversion<M> {
    }

    /**
     * A conversion that failed.
     *
     * @param <M> the type the model's value would have had
     * @param message why the value cannot be converted, as the user should read it, such as "Enter a number from 1
     *     to 64"
     */
    record Invalid<M>(Text message) implements Conversion<M> {
        /**
         * Checks the message.
         */
        public Invalid {
            Objects.requireNonNull(message, "message");
        }
    }
}
