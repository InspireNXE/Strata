/**
 * Layout containers and the values that control placement.
 *
 * <p>Strata ships five containers. Each measures its children, then gives each one a slot; a child places itself
 * in its slot by its own {@link org.inspirenxe.strata.api.layout.HorizontalAlignment} and
 * {@link org.inspirenxe.strata.api.layout.VerticalAlignment}.</p>
 * <ul>
 *     <li>{@link org.inspirenxe.strata.api.layout.AnchorPanel}: every child gets the whole content area, so children
 *     overlap and anchor themselves to its edges or center.</li>
 *     <li>{@link org.inspirenxe.strata.api.layout.StackPanel}: children follow one another in a row or a column.</li>
 *     <li>{@link org.inspirenxe.strata.api.layout.GridPanel}: children sit in the cells of rows and columns sized
 *     automatically, fixed, or by weight.</li>
 *     <li>{@link org.inspirenxe.strata.api.layout.AbsolutePanel}: children sit at fixed positions.</li>
 *     <li>{@link org.inspirenxe.strata.api.layout.ScrollPanel}: children larger than the panel scroll.</li>
 * </ul>
 * <p>To write another layout, extend {@link org.inspirenxe.strata.api.extension.CustomContainer}.</p>
 */
package org.inspirenxe.strata.api.layout;
