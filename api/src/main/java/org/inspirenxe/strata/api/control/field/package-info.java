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
/**
 * Controls that edit a value: text, a number, a color, a date or a time:
 * {@link org.inspirenxe.strata.api.control.field.TextBox},
 * {@link org.inspirenxe.strata.api.control.field.PasswordTextBox},
 * {@link org.inspirenxe.strata.api.control.field.TextArea}, {@link org.inspirenxe.strata.api.control.field.Spinner},
 * {@link org.inspirenxe.strata.api.control.field.Slider},
 * {@link org.inspirenxe.strata.api.control.field.ColorPicker},
 * {@link org.inspirenxe.strata.api.control.field.CalendarView},
 * {@link org.inspirenxe.strata.api.control.field.DatePicker},
 * {@link org.inspirenxe.strata.api.control.field.TimePicker} and
 * {@link org.inspirenxe.strata.api.control.field.DateTimePicker}.
 *
 * <p>Make them with a {@link org.inspirenxe.strata.api.ComponentFactory}. Each control lists the theme style
 * keys it reads as {@code STYLE_*} constants and says which are required, and its events as constants too:
 * a request for each change the user can make, such as {@code Button.PRESS}, and a report for each change
 * from any source, such as {@code Button.PRESSED}.</p>
 */
package org.inspirenxe.strata.api.control.field;
