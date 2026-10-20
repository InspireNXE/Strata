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
 * Extending Strata: new components, new layouts, and changes to existing components.
 *
 * <p>Pick the lightest way that does the job:</p>
 * <ul>
 *     <li>Change a built-in through its events: cancel or rewrite its requests, react to its reports, draw over
 *     it with {@code Component.PAINTED} or change its size with {@code Component.MEASURED}. See
 *     {@link org.inspirenxe.strata.api.event}.</li>
 *     <li>Compose: put built-in components inside a container and expose the group through your own
 *     interface.</li>
 *     <li>Write a new component: extend {@link org.inspirenxe.strata.api.extension.CustomComponent}, or
 *     {@link org.inspirenxe.strata.api.extension.CustomContainer} for a new layout. Your class can implement your own
 *     interface, so other code sees it as a type of its own, and declare and fire events of its own with
 *     {@link org.inspirenxe.strata.api.extension.CustomComponent}'s {@code request} and {@code report}.</li>
 * </ul>
 * <p>Do not implement the component interfaces ({@code Component}, {@code Button} and so on) directly. Such a class
 * has no Strata implementation behind it, and containers reject it.</p>
 *
 * <p>Strata's own components are not built on these classes. A component that is an instance of
 * {@link org.inspirenxe.strata.api.extension.CustomComponent} was written outside Strata, by you or another library,
 * and everything {@link org.inspirenxe.strata.api.ComponentFactory} makes is Strata's. Both kinds share the same tree,
 * layout and events, and either can hold the other.</p>
 *
 * <p>The {@code peer} subpackage is plumbing between the base classes and Strata's implementation. Developers do
 * not use it.</p>
 */
package org.inspirenxe.strata.api.extension;
