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
 * A Pre event: one that asks to do something, so a handler may refuse. Input events and a control's request events
 * are cancelable; Post events, which report something that already happened, are not.
 *
 * <p>Only Strata creates events. Do not implement this interface.</p>
 */
public interface Cancelable extends Event {

    /**
     * Refuses what the event asks for. For an input event, the default behavior of the current component and of
     * every ancestor the event still reaches is skipped. For a request event, the control does not make the change.
     * For {@link org.inspirenxe.strata.api.component.Component#PAINT}, the component skips its own drawing. Handlers
     * keep running, and the event keeps bubbling unless stopped too.
     */
    void cancel();

    /**
     * Returns whether the event was canceled.
     *
     * @return true after {@link #cancel()} or {@link #consume()}
     */
    boolean isCancelled();

    /**
     * Cancels the event and stops it: "handled here". Built-in controls consume the input they act on.
     */
    default void consume() {
        this.cancel();
        this.stopPropagation();
    }

    /**
     * Returns whether something canceled or stopped the event. The host reports such input to the engine as
     * consumed.
     *
     * @return true if canceled or stopped
     */
    default boolean isHandled() {
        return this.isCancelled() || this.isPropagationStopped();
    }
}
