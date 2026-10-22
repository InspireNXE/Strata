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
 * Turns a model's value into the value a control shows, and back, for binding a control to a state of another type,
 * such as a text box to a {@code State<Integer>}. The way back can fail, since the user can type anything.
 *
 * <pre>{@code
 * quantity.bind(TextInput.TEXT.convert(Converters.integer(1, 64)), order.quantity);
 * }</pre>
 *
 * <p>{@link Converters} has converters for numbers and a way to make one from two functions. A converter is called
 * on the UI thread, once per change. It should be a pure function of its argument, since a binding converts the same
 * value more than once to compare it.</p>
 *
 * @param <M> the type of the model's value, what the state holds
 * @param <V> the type of the control's value, what the property holds
 * @see PropertyKey#convert
 */
public interface Converter<M, V> {

    /**
     * Returns the model's value as the control shows it.
     *
     * @param model the model's value; null if the state holds null
     * @return the control's value; may be null only where the property accepts null
     */
    V toView(M model);

    /**
     * Returns the control's value as the model's, or why it cannot be one.
     *
     * @param view the control's value
     * @return a {@link Conversion.Valid} with the model's value, or a {@link Conversion.Invalid} with a message for
     *     the user
     */
    Conversion<M> toModel(V view);
}
