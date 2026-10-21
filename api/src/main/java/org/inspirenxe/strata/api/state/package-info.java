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
 * State and data binding.
 *
 * <p>A {@link org.inspirenxe.strata.api.state.State} holds a value in a plain model class. A
 * {@link org.inspirenxe.strata.api.state.Source} is a state or a lambda that computes from states. A component keeps
 * one of its properties in step with a source through
 * {@link org.inspirenxe.strata.api.component.Component#bind}, named by a
 * {@link org.inspirenxe.strata.api.state.PropertyKey} declared beside the property's events.</p>
 */
package org.inspirenxe.strata.api.state;
