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
 *     <li>{@code text}: {@code HasText}, {@code Titled} and {@code TextInput}.</li>
 *     <li>{@code value}: {@code Toggle}, {@code Ranged} and {@code Stepped}.</li>
 *     <li>{@code item}: {@code ItemSource}, {@code CellSource}, {@code SingleSelection}, {@code Activatable} and
 *     {@code RowView}.</li>
 *     <li>{@code popup}: {@code Closable} and {@code Openable}.</li>
 * </ul>
 *
 * <p>An attribute owns the events of its capability. {@code Toggle.TOGGLED} fires for a check box, a switch and a radio
 * button alike, so one handler on a container hears all of them. Each attribute takes the control's own type as
 * {@code S}, so its setters return that type and chains keep going.</p>
 *
 * <p>Only Strata implements these interfaces. Code may hold controls by them and call them.</p>
 */
package org.inspirenxe.strata.api.control.attribute;
