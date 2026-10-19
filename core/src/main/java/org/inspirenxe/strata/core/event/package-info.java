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
 * The event classes core creates, and their delivery.
 *
 * <p>{@link org.inspirenxe.strata.core.event.EventRouter} delivers each event to its target and, for bubbling
 * types, up through the ancestors. Each component keeps its handlers in an
 * {@link org.inspirenxe.strata.core.event.EventDispatcher}, made on its first subscription.</p>
 */
package org.inspirenxe.strata.core.event;
