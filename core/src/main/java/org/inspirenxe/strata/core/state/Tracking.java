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
package org.inspirenxe.strata.core.state;

import java.util.HashMap;
import java.util.Map;

/**
 * Records which states a source reads while a binding evaluates it. Frames nest per thread, and a read goes to the
 * innermost frame only. A read outside any frame records nothing, so code that calls {@code get()} elsewhere costs a
 * thread-local lookup and no more.
 */
final class Tracking {
    private static final ThreadLocal<Frame> CURRENT = new ThreadLocal<>();

    private Tracking() {
    }

    /**
     * Opens a frame on this thread. Close it with {@link #end} in a {@code finally}.
     *
     * @return the frame
     */
    static Frame begin() {
        final Frame frame = new Frame(Tracking.CURRENT.get());
        Tracking.CURRENT.set(frame);
        return frame;
    }

    /**
     * Closes a frame, making its parent current again.
     *
     * @param frame the frame {@link #begin} returned
     */
    static void end(final Frame frame) {
        if (frame.parent == null) {
            Tracking.CURRENT.remove();
        } else {
            Tracking.CURRENT.set(frame.parent);
        }
    }

    /**
     * Records that a state was read at a version, if a frame is open. The first read of a state in a frame counts.
     *
     * @param state the state
     * @param version its version before the value was read
     */
    static void read(final StateImpl<?> state, final long version) {
        final Frame frame = Tracking.CURRENT.get();
        if (frame != null) {
            frame.reads.putIfAbsent(state, version);
        }
    }

    /**
     * The states read while one source ran, each with the version it had when first read.
     */
    static final class Frame {
        private final Frame parent;
        private final Map<StateImpl<?>, Long> reads;

        Frame(final Frame parent) {
            this.parent = parent;
            this.reads = new HashMap<>();
        }

        Map<StateImpl<?>, Long> reads() {
            return this.reads;
        }
    }
}
