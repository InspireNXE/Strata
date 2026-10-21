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
package org.inspirenxe.strata.api.event.cause;

import java.util.Optional;

/**
 * The frames whose objects and values make up the cause of every event fired while they are open. Get it from
 * {@code ComponentFactory.causes()}. Hosts push a frame for each input with a {@link Origin.User} origin, a data
 * binding pushes one with {@link Origin.Binding} while it writes, and code that applies a change for someone else
 * pushes its own:
 *
 * <pre>{@code
 * try (CauseFrame frame = ui.causes().pushFrame().put(CauseKeys.ORIGIN, new Origin.Remote(update.buyer()))) {
 *     listings.setItems(update.listings());       // SELECTED and the rest report the buyer as their origin
 * }
 * }</pre>
 *
 * <p>A stack belongs to the UI thread, which dispatches events. Work from another thread pushes its frame inside the
 * task it hands to the UI thread, where the change is applied.</p>
 *
 * <p>Strata implements this interface, and an adapter may implement it to back Strata's causes with the engine's
 * own, as an adapter on Sponge does with Sponge's.</p>
 */
public interface CauseStack {

    /**
     * Opens a frame on top of the stack. Close it when the change it covers is done.
     *
     * @return the new frame
     */
    CauseFrame pushFrame();

    /**
     * Returns the cause as the open frames make it now.
     *
     * @return the current cause; never null
     */
    Cause current();

    /**
     * Returns a value of the current context, without making a whole cause.
     *
     * @param <V> the value type
     * @param key the key
     * @return the value from the innermost frame that put one for the key, or empty
     */
    <V> Optional<V> context(CauseKey<V> key);

    /**
     * Returns the current origin, without making a whole cause.
     *
     * @return the origin the innermost frame put, or {@link Origin#CODE}
     */
    default Origin origin() {
        return this.context(CauseKeys.ORIGIN).orElse(Origin.CODE);
    }
}
