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
package org.inspirenxe.strata.api.control.attribute.value;

import org.inspirenxe.strata.api.component.Validation;
import org.inspirenxe.strata.api.event.EventTarget;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.control.ValueEvent;
import org.inspirenxe.strata.api.state.PropertyKey;

import java.util.function.Function;

/**
 * A control whose value can be marked wrong, with a message for the user: the text fields, the spinner, the slider,
 * the combo box, the check box, the date and time pickers and the color picker.
 *
 * <p>A control's {@link #validation()} comes from three places. {@link #setValidation} sets it from code. A
 * {@linkplain #setValidator validator} works it out after each change of the control's value. A binding through a
 * {@linkplain PropertyKey#convert converted} key sets an error with the conversion's message while the value does not
 * convert, and takes it away when it converts again or the state changes; that error wins over the other two, since
 * the value they would check has no model value. Every change of the validation reports {@link #VALIDATED}; there is
 * no request.</p>
 *
 * <p>An enabled control with an error draws its theme's invalid face in place of its normal one, such as
 * {@code textbox.invalid}, or, where the theme has none or the control has no such key, its normal face with a
 * one-unit outline in {@value #STYLE_ERROR}. A warning draws the outline in {@value #STYLE_WARNING}. A disabled
 * control draws as it always does. While the validation has a severity and a message, the control's tooltip shows the
 * message first, ahead of a tooltip set in code.</p>
 *
 * <p>Bind {@link #VALIDATION} to a state to bring the validations of a form's fields together:</p>
 *
 * <pre>{@code
 * final State<Validation> quantityCheck = State.of(Validation.VALID);
 * quantity.bind(TextInput.TEXT.convert(Converters.integer(1, 64)), order.quantity)
 *         .bind(Validatable.VALIDATION, quantityCheck);
 * save.bind(Component.ENABLED, quantityCheck.map(Validation::isValid));
 * hint.bind(HasText.TEXT, quantityCheck.map(Validation::message));
 * }</pre>
 *
 * <p>Do not implement this interface.</p>
 *
 * @param <S> the control's own type, which the setters return
 */
public interface Validatable<S extends Validatable<S>> extends EventTarget<S> {
    /**
     * Style key for the outline of a control whose validation is an error and whose theme has no invalid face for it
     * (paint, optional). Defaults to a solid red.
     */
    String STYLE_ERROR = "validation.error";
    /**
     * Style key for the outline of a control whose validation is a warning (paint, optional). Defaults to a solid
     * amber.
     */
    String STYLE_WARNING = "validation.warning";
    /**
     * Told after the validation changed, from any source. The value is the new validation and the previous value
     * the one before; neither is null.
     */
    EventType<ValueEvent<Validatable<?>, Validation>> VALIDATED = EventType.valueReport("validatable.validated");
    /**
     * The validation, as {@link #validation()} and {@link #setValidation} say. Binds two ways through
     * {@link #VALIDATED}, so a bound state hears every change, and a value set from the state is set as
     * {@code setValidation} sets it.
     */
    PropertyKey<Validatable<?>, Validation> VALIDATION = PropertyKey.of("validatable.validation",
            Validatable::validation, Validatable::setValidation, VALIDATED);

    /**
     * Returns the control's validation: a binding's conversion error if there is one, else what was set last, by
     * {@link #setValidation} or by the validator.
     *
     * @return the validation; {@link Validation#VALID} by default
     */
    Validation validation();

    /**
     * Sets the validation from code. A binding's conversion error still wins while it lasts, and the validator, if
     * there is one, replaces this the next time the value changes. Reports {@link #VALIDATED} if the validation the
     * control shows changed.
     *
     * @param validation the validation
     * @return this control
     */
    S setValidation(Validation validation);

    /**
     * Sets a function that works out the validation from the control, asked at once and then after each change of
     * the control's value, from the user or from code, once the change has been reported.
     *
     * <pre>{@code
     * name.setValidator(box -> box.isEmpty() ? Validation.error(Text.of("Enter a name")) : Validation.VALID);
     * }</pre>
     *
     * @param validator the validator, which must not return null, or {@code null} to stop validating; the
     *     validation then stays as it is
     * @return this control
     */
    S setValidator(Function<? super S, Validation> validator);
}
