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

import org.inspirenxe.strata.api.text.Text;

/**
 * A control that shows when a {@link Binding} through a converted key cannot convert its value, which Strata's
 * {@code Validatable} controls are. The binding tells it after each of the control's reports and after it writes the
 * state's value, on the UI thread.
 */
public interface ConversionTarget {

    /**
     * Shows or clears a binding's conversion error, which wins over the control's other validation while it lasts.
     *
     * @param message why the value does not convert, or {@code null} once it does
     */
    void setConversionError(Text message);
}
