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
 * Controls the user presses or toggles: {@link org.inspirenxe.strata.api.control.button.Button},
 * {@link org.inspirenxe.strata.api.control.button.DropDownButton},
 * {@link org.inspirenxe.strata.api.control.button.CycleButton},
 * {@link org.inspirenxe.strata.api.control.button.CheckBox},
 * {@link org.inspirenxe.strata.api.control.button.RadioButton},
 * {@link org.inspirenxe.strata.api.control.button.RadioGroup} and
 * {@link org.inspirenxe.strata.api.control.button.ToggleSwitch}.
 *
 * <p>Make them with a {@link org.inspirenxe.strata.api.ComponentFactory}. Each control lists the theme style
 * keys it reads as {@code STYLE_*} constants and says which are required, and the events particular to it as
 * constants too: a request for each change the user can make, such as {@code Button.PRESS}, and a report for each
 * change from any source, such as {@code Button.PRESSED}. Events that several controls share, such as
 * {@code Toggle.TOGGLED}, are constants on the attributes in {@code org.inspirenxe.strata.api.control.attribute}.</p>
 */
package org.inspirenxe.strata.api.control.button;
