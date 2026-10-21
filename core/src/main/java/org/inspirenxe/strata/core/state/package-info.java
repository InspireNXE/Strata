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
 * States, dependency tracking and bindings.
 *
 * <p>{@link org.inspirenxe.strata.core.state.StateImpl} holds a value and the bindings that depend on it. A
 * {@link org.inspirenxe.strata.core.state.Binding} records the states its source reads, and a root's
 * {@link org.inspirenxe.strata.core.state.BindingQueue} evaluates the dirty ones on the UI thread before layout.</p>
 */
package org.inspirenxe.strata.core.state;
