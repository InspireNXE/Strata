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
 * The component model: {@link org.inspirenxe.strata.api.component.Component} and
 * {@link org.inspirenxe.strata.api.component.Container}.
 *
 * <p>A Strata UI is a tree of components. Containers own their children, lay them out and draw them; leaves draw
 * themselves. Components come from a {@link org.inspirenxe.strata.api.ComponentFactory}, or from subclassing
 * {@link org.inspirenxe.strata.api.extension.CustomComponent}.</p>
 *
 * <h2>Rules that hold for every component</h2>
 * <ul>
 *     <li>Call components only from the engine's UI thread. Strata does no locking.</li>
 *     <li>A component belongs to the factory that made it and can only join trees built by that factory.</li>
 *     <li>A component has at most one parent. Remove it from its parent before adding it elsewhere.</li>
 *     <li>Setters take effect on the next frame. Changes that affect size schedule a new layout; you never call
 *     layout yourself.</li>
 *     <li>Setters return the component, so calls chain.</li>
 * </ul>
 */
package org.inspirenxe.strata.api.component;
