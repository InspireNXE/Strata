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

import java.util.Objects;

/**
 * Who caused a change, the {@link CauseKeys#ORIGIN} context value. The set is closed, so a handler can switch over
 * it:
 *
 * <pre>{@code
 * listings.onEvent(Activatable.ACTIVATED, event -> {
 *     if (event.origin() instanceof Origin.User user) {
 *         this.buy(event.value(), user);
 *     }
 * });
 * }</pre>
 *
 * <p>A control's requests fire only when the origin is {@link User}. Its reports fire for every origin and carry
 * it.</p>
 */
public sealed interface Origin {

    /**
     * A plain call from code, with no origin put. The origin of every event no frame gave one.
     */
    Code CODE = new Code();

    /**
     * The person at this screen, acting through input. Hosts put it for every input they dispatch.
     *
     * @param identity the adapter's object for the person, such as the local player's profile, or null if the
     *                 platform does not name its user; opaque to Strata
     */
    record User(Object identity) implements Origin {
    }

    /**
     * Another person, whose change reached this screen through a server or another channel. Put it in a frame
     * around the code that applies the change.
     *
     * @param identity the application's object for the person, such as a profile; opaque to Strata
     */
    record Remote(Object identity) implements Origin {
        /**
         * Checks the identity.
         */
        public Remote {
            Objects.requireNonNull(identity, "identity");
        }
    }

    /**
     * A data binding, writing a value from its source to a component.
     *
     * @param source the source the binding reads, the one given to {@code Component.bind}
     */
    record Binding(Object source) implements Origin {
        /**
         * Checks the source.
         */
        public Binding {
            Objects.requireNonNull(source, "source");
        }
    }

    /**
     * A plain call from code. Use {@link #CODE} rather than making new ones.
     */
    record Code() implements Origin {
    }
}
