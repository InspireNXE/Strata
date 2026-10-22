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
 * Input events: what the user does with the pointer and the keyboard, drags that carry a transfer, and focus moving.
 *
 * <p>The types are the constants in {@link org.inspirenxe.strata.api.event.input.InputEvents}, such as
 * {@code InputEvents.CLICK} and {@code InputEvents.KEY}. Input events are Pre events: canceling one skips the default
 * behavior of the components it has not reached yet, and the host reports it as consumed.</p>
 *
 * <p>Only Strata creates events. Do not implement these interfaces.</p>
 */
package org.inspirenxe.strata.api.event.input;
