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
 * Controls that show something: {@link org.inspirenxe.strata.api.control.display.Label},
 * {@link org.inspirenxe.strata.api.control.display.Image},
 * {@link org.inspirenxe.strata.api.control.display.Avatar},
 * {@link org.inspirenxe.strata.api.control.display.InfoBar},
 * {@link org.inspirenxe.strata.api.control.display.ProgressBar},
 * {@link org.inspirenxe.strata.api.control.display.BusyIndicator} and
 * {@link org.inspirenxe.strata.api.control.display.Separator}. Only the info bar takes input, through its close
 * button and its action.
 *
 * <p>Make them with a {@link org.inspirenxe.strata.api.ComponentFactory}. Each control lists the theme style
 * keys it reads as {@code STYLE_*} constants and says which are required. The few events they fire, such as
 * {@code Closable.CLOSED} from an info bar or {@code Ranged.CHANGED} from a progress bar, are constants on the
 * attributes in {@code org.inspirenxe.strata.api.control.attribute}.</p>
 */
package org.inspirenxe.strata.api.control.display;
