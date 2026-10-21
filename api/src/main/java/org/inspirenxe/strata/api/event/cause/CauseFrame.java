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

/**
 * One frame of a {@link CauseStack}, open from {@link CauseStack#pushFrame()} until {@link #close()}. Objects pushed
 * and values put through it are part of every cause taken while it is open, and go away when it closes. Use it in a
 * {@code try}-with-resources block, so it closes on every path:
 *
 * <pre>{@code
 * try (CauseFrame frame = ui.causes().pushFrame().put(CauseKeys.ORIGIN, new Origin.Remote(buyer))) {
 *     listings.setItems(update.listings());
 * }
 * }</pre>
 *
 * <p>Close frames in the reverse order they were opened. Closing a frame that is not the top one closes the frames
 * above it first, and closing a frame again does nothing. A frame belongs to the thread its stack belongs to, the UI
 * thread.</p>
 *
 * <p>Strata and adapters implement this interface.</p>
 */
public interface CauseFrame extends AutoCloseable {

    /**
     * Adds an object to the front of the chain.
     *
     * @param cause the object
     * @return this frame
     * @throws IllegalStateException if the frame was closed
     */
    CauseFrame push(Object cause);

    /**
     * Puts a value into the context, replacing this frame's earlier value for the key. It hides the value an outer
     * frame put for the key until this frame closes.
     *
     * @param <V> the value type
     * @param key the key
     * @param value the value
     * @return this frame
     * @throws IllegalStateException if the frame was closed
     */
    <V> CauseFrame put(CauseKey<V> key, V value);

    /**
     * Closes the frame, removing what it pushed and put.
     */
    @Override
    void close();
}
