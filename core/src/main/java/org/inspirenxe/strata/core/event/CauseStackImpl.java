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
package org.inspirenxe.strata.core.event;

import org.inspirenxe.strata.api.event.cause.Cause;
import org.inspirenxe.strata.api.event.cause.CauseFrame;
import org.inspirenxe.strata.api.event.cause.CauseKey;
import org.inspirenxe.strata.api.event.cause.CauseKeys;
import org.inspirenxe.strata.api.event.cause.CauseStack;
import org.inspirenxe.strata.api.event.cause.Origin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Strata's own cause stack, one per thread, used when the platform has none of its own. Frames are plain objects
 * kept in a list; a frame allocates its arrays only when something is pushed or put through it.
 * {@link #current()} copies the frames into a {@link CauseImpl} and keeps it until a frame changes, so asking
 * again costs nothing. {@link #context} and {@link #origin()} read the frames directly and make no cause.
 */
public final class CauseStackImpl implements CauseStack {
    private static final ThreadLocal<CauseStackImpl> THREAD = ThreadLocal.withInitial(CauseStackImpl::new);
    private static final Cause EMPTY = new CauseImpl(List.of(), Map.of());

    private final List<FrameImpl> frames;
    private Cause cached;

    private CauseStackImpl() {
        this.frames = new ArrayList<>();
        this.cached = CauseStackImpl.EMPTY;
    }

    /**
     * Returns the calling thread's stack, made on first use.
     *
     * @return the stack
     */
    public static CauseStackImpl forThread() {
        return CauseStackImpl.THREAD.get();
    }

    @Override
    public CauseFrame pushFrame() {
        final FrameImpl frame = new FrameImpl();
        this.frames.add(frame);
        return frame;
    }

    @Override
    public Cause current() {
        if (this.cached == null) {
            this.cached = this.build();
        }
        return this.cached;
    }

    @Override
    public <V> Optional<V> context(final CauseKey<V> key) {
        Objects.requireNonNull(key, "key");
        final Object value = this.find(key);
        return value == null ? Optional.empty() : Optional.of(key.type().cast(value));
    }

    @Override
    public Origin origin() {
        final Object origin = this.find(CauseKeys.ORIGIN);
        return origin == null ? Origin.CODE : (Origin) origin;
    }

    private Object find(final CauseKey<?> key) {
        for (int i = this.frames.size() - 1; i >= 0; i--) {
            final Object value = this.frames.get(i).get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Cause build() {
        final List<Object> chain = new ArrayList<>();
        final Map<CauseKey<?>, Object> context = new IdentityHashMap<>();
        for (int i = this.frames.size() - 1; i >= 0; i--) {
            final FrameImpl frame = this.frames.get(i);
            for (int c = frame.causeCount - 1; c >= 0; c--) {
                chain.add(frame.causes[c]);
            }
            for (int k = 0; k < frame.contextCount; k++) {
                context.putIfAbsent(frame.keys[k], frame.values[k]);
            }
        }
        if (chain.isEmpty() && context.isEmpty()) {
            return CauseStackImpl.EMPTY;
        }
        return new CauseImpl(Collections.unmodifiableList(chain), Collections.unmodifiableMap(context));
    }

    /**
     * Returns how many frames are open, for tests.
     *
     * @return the number of open frames
     */
    public int depth() {
        return this.frames.size();
    }

    private final class FrameImpl implements CauseFrame {
        private Object[] causes;
        private int causeCount;
        private CauseKey<?>[] keys;
        private Object[] values;
        private int contextCount;
        private boolean closed;

        @Override
        public CauseFrame push(final Object cause) {
            Objects.requireNonNull(cause, "cause");
            this.requireOpen();
            if (this.causes == null) {
                this.causes = new Object[2];
            } else if (this.causeCount == this.causes.length) {
                this.causes = Arrays.copyOf(this.causes, this.causeCount * 2);
            }
            this.causes[this.causeCount++] = cause;
            CauseStackImpl.this.cached = null;
            return this;
        }

        @Override
        public <V> CauseFrame put(final CauseKey<V> key, final V value) {
            Objects.requireNonNull(key, "key");
            key.type().cast(Objects.requireNonNull(value, "value"));
            this.requireOpen();
            for (int i = 0; i < this.contextCount; i++) {
                if (this.keys[i] == key) {
                    this.values[i] = value;
                    CauseStackImpl.this.cached = null;
                    return this;
                }
            }
            if (this.keys == null) {
                this.keys = new CauseKey<?>[6];
                this.values = new Object[6];
            } else if (this.contextCount == this.keys.length) {
                this.keys = Arrays.copyOf(this.keys, this.contextCount * 2);
                this.values = Arrays.copyOf(this.values, this.contextCount * 2);
            }
            this.keys[this.contextCount] = key;
            this.values[this.contextCount++] = value;
            CauseStackImpl.this.cached = null;
            return this;
        }

        private Object get(final CauseKey<?> key) {
            for (int i = 0; i < this.contextCount; i++) {
                if (this.keys[i] == key) {
                    return this.values[i];
                }
            }
            return null;
        }

        private void requireOpen() {
            if (this.closed) {
                throw new IllegalStateException("The cause frame was closed");
            }
        }

        @Override
        public void close() {
            if (this.closed) {
                return;
            }
            final List<FrameImpl> frames = CauseStackImpl.this.frames;
            final int at = frames.lastIndexOf(this);
            if (at < 0) {
                this.closed = true;
                return;
            }
            // Frames opened above this one and still open close with it
            for (int i = frames.size() - 1; i >= at; i--) {
                final FrameImpl frame = frames.remove(i);
                frame.closed = true;
                if (frame.causeCount > 0 || frame.contextCount > 0) {
                    CauseStackImpl.this.cached = null;
                }
            }
            this.closed = true;
        }
    }
}
