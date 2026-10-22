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
 *
 * <p>States may be read and set from any thread. A change marks the bindings that read the state, and the host
 * brings their components up to date on the UI thread before it next lays out, or when it is flushed, so many
 * changes in one frame cost one update. A binding to a state with a key that has a report is two way: the user's
 * accepted changes go back into the state. Bindings that keep changing each other's states for too many rounds in
 * one flush, 100 in Strata's core, are taken for a cycle, logged as an error through {@link java.lang.System.Logger}, and dropped until one of
 * their states changes again.</p>
 *
 * <p>A {@link org.inspirenxe.strata.api.state.Converter} binds a property to a state of another type through
 * {@link org.inspirenxe.strata.api.state.PropertyKey#convert}, such as a text box's text to a number. Converting the
 * control's value back gives a {@link org.inspirenxe.strata.api.state.Conversion}, which says why when the value has
 * no model value. {@link org.inspirenxe.strata.api.state.Converters} has converters for numbers.</p>
 *
 * <p>Code that acts on a change rather than showing it hears it two ways.
 * {@link org.inspirenxe.strata.api.ComponentFactory#watch} follows a source the way a binding does and calls on the UI
 * thread with the bindings, until its screen closes. {@link org.inspirenxe.strata.api.state.State#subscribe} calls at
 * once on the thread that set the state, for code outside any screen.</p>
 *
 * <p>{@link org.inspirenxe.strata.api.state.State#of} makes states through the
 * {@link org.inspirenxe.strata.api.state.StateFactory} that Strata's core provides.</p>
 */
package org.inspirenxe.strata.api.state;
