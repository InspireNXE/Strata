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
 * Layout containers and the values that control placement.
 *
 * <p>Strata ships six containers. Each measures its children, then gives each one a slot; a child places itself
 * in its slot by its own {@link org.inspirenxe.strata.api.layout.HorizontalAlignment} and
 * {@link org.inspirenxe.strata.api.layout.VerticalAlignment}.</p>
 * <ul>
 *     <li>{@link org.inspirenxe.strata.api.layout.AnchorPanel}: every child gets the whole content area, so children
 *     overlap and anchor themselves to its edges or center.</li>
 *     <li>{@link org.inspirenxe.strata.api.layout.StackPanel}: children follow one another in a row or a column.</li>
 *     <li>{@link org.inspirenxe.strata.api.layout.GridPanel}: children sit in the cells of rows and columns sized
 *     automatically, fixed, or by weight.</li>
 *     <li>{@link org.inspirenxe.strata.api.layout.WrapPanel}: children follow one another and wrap onto a new row
 *     or column when the current one is full.</li>
 *     <li>{@link org.inspirenxe.strata.api.layout.AbsolutePanel}: children sit at fixed positions.</li>
 *     <li>{@link org.inspirenxe.strata.api.layout.ScrollPanel}: children larger than the panel scroll.</li>
 * </ul>
 * <p>A {@link org.inspirenxe.strata.api.layout.Spacer} is an empty component that keeps a gap, or takes the room
 * left in a stack panel.</p>
 * <p>To write another layout, extend {@link org.inspirenxe.strata.api.extension.CustomContainer}.</p>
 */
package org.inspirenxe.strata.api.layout;
