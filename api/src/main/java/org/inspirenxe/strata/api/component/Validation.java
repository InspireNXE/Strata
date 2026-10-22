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
package org.inspirenxe.strata.api.component;

import org.inspirenxe.strata.api.text.Text;

import java.util.Objects;

/**
 * Whether a control's value is acceptable, and why not, as a
 * {@link org.inspirenxe.strata.api.control.attribute.value.Validatable} control shows it. An error marks the value
 * as wrong, such as "abc" in a quantity field; a warning marks it as allowed but worth a look, such as a weak
 * password.
 *
 * <pre>{@code
 * box.setValidator(field -> field.text().isBlank() ? Validation.error(Text.of("Enter a name")) : Validation.VALID);
 * save.bind(Component.ENABLED, check.map(Validation::isValid));
 * }</pre>
 *
 * @param severity how serious it is
 * @param message what the user should read, such as "Enter a number from 1 to 64"; empty for {@link #VALID}
 */
public record Validation(Severity severity, Text message) {
    /**
     * Nothing to report.
     */
    public static final Validation VALID = new Validation(Severity.NONE, Text.empty());

    /**
     * Checks the arguments.
     */
    public Validation {
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(message, "message");
    }

    /**
     * Returns an error: the value is wrong.
     *
     * @param message why, as the user should read it
     * @return the validation
     */
    public static Validation error(final Text message) {
        return new Validation(Severity.ERROR, message);
    }

    /**
     * Returns a warning: the value is allowed but worth a look.
     *
     * @param message why, as the user should read it
     * @return the validation
     */
    public static Validation warning(final Text message) {
        return new Validation(Severity.WARNING, message);
    }

    /**
     * Returns whether the value may be used, which is anything but an error, so a form can enable its Save button
     * while its fields only warn.
     *
     * @return true unless the severity is {@link Severity#ERROR}
     */
    public boolean isValid() {
        return this.severity != Severity.ERROR;
    }

    /**
     * How serious a validation is.
     */
    public enum Severity {
        /**
         * Nothing to report.
         */
        NONE,
        /**
         * Allowed, but worth a look.
         */
        WARNING,
        /**
         * Wrong.
         */
        ERROR
    }
}
