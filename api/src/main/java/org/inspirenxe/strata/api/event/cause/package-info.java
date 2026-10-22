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
 * Causes: who or what led to an event.
 *
 * <p>Every event has a {@link org.inspirenxe.strata.api.event.cause.Cause}, a chain of the objects a change passed
 * through and a context of keyed values. The most asked value is the
 * {@linkplain org.inspirenxe.strata.api.event.cause.Origin origin}: the user at this screen, another person through
 * the server, a data binding, or plain code. {@code Event.origin()} reads it without making the whole cause, and
 * {@code Event.cause()} makes the cause only when a handler asks for it.</p>
 *
 * <p>Causes come from a {@link org.inspirenxe.strata.api.event.cause.CauseStack} of open frames. Hosts open one for
 * each input they dispatch, a control acting on another pushes itself, such as a radio group deselecting a button,
 * and code that applies a change for someone else opens its own. On an engine with a cause stack of its own, the
 * adapter can back Strata's stack with the engine's.</p>
 */
package org.inspirenxe.strata.api.event.cause;
