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
 * Attributes that controls share, grouped by kind in the packages below this one.
 *
 * <p>An attribute is a small interface for one capability, such as
 * {@link org.inspirenxe.strata.api.control.attribute.value.Toggle} for anything on or off or
 * {@link org.inspirenxe.strata.api.control.attribute.item.SingleSelection} for anything with one selected entry. A
 * control implements every attribute that fits it, so code written against an attribute works for each control that has
 * it, and the same thing has the same name on every control.</p>
 *
 * <ul>
 *     <li>{@code text}: {@code HasText}, {@code Titled}, {@code TextEntry} and {@code TextInput}.</li>
 *     <li>{@code value}: {@code Toggle}, {@code Ranged}, {@code Stepped}, {@code HasDate} and {@code HasTime}.</li>
 *     <li>{@code item}: {@code ItemSource}, {@code CellSource}, {@code Selection}, {@code SingleSelection},
 *     {@code MultiSelection}, {@code Activatable} and {@code RowView}.</li>
 *     <li>{@code popup}: {@code Closable}, {@code Openable} and {@code Popup}.</li>
 *     <li>{@code command}: {@code HasCommand}.</li>
 * </ul>
 *
 * <p>An attribute owns the events of its capability. {@code Toggle.TOGGLED} fires for a checkbox, a switch and a radio
 * button alike, so one handler on a container hears all of them. Each attribute takes the control's own type as
 * {@code S}, so its setters return that type and chains keep going. Attributes with events extend
 * {@link org.inspirenxe.strata.api.event.EventTarget}; those without do not, so entries that are not event targets,
 * such as a menu item or a table column, can have them too.</p>
 *
 * <p>Do not implement these interfaces. Code may hold controls by them and call them.</p>
 */
package org.inspirenxe.strata.api.control.attribute;
