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
 * A handler registered with {@link EventTarget#subscribe}, which can be removed before its target is discarded.
 * Removing a component from its tree does not remove its handlers; close the subscription.
 */
@FunctionalInterface
public interface Subscription extends AutoCloseable {

    /**
     * Removes the handler. Calling it again does nothing.
     */
    void unsubscribe();

    /**
     * Same as {@link #unsubscribe()}, so a subscription works in try-with-resources.
     */
    @Override
    default void close() {
        this.unsubscribe();
    }
}
