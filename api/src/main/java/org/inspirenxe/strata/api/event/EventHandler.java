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
package org.inspirenxe.strata.api.event;

/**
 * Code that reacts to an event, registered with {@link EventTarget#onEvent} or {@link EventTarget#subscribe}. Runs on
 * the UI thread. A lambda or method reference fits, and one handler may be registered on several targets.
 *
 * <p>An exception thrown by a handler is not caught: the handlers after it for the same event do not run, and the
 * exception propagates out of the input call or setter that caused the event, so handle your own errors.</p>
 *
 * @param <E> the event interface
 */
@FunctionalInterface
public interface EventHandler<E extends Event> {

    /**
     * Reacts to an event.
     *
     * @param event the event; valid only during this call
     */
    void handle(E event);
}
