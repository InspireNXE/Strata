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
package org.inspirenxe.strata.core.frame;

import org.inspirenxe.strata.spi.ScreenHost;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Whether a host, or a stack of them, needs a frame, and the deadlines of the timed changes components registered.
 *
 * <p>{@link #request()} may be called from any thread. It turns the flag on and, when the flag was off, runs the
 * requester the adapter set, so the requester runs once per change from clean to dirty. A new instance starts dirty,
 * since nothing has been drawn yet.</p>
 *
 * <p>Deadlines are {@code Clock.nanoTime()} readings kept in a small array on the UI thread. Equal deadlines are
 * kept once, and {@link #prune} drops those a frame has reached, so registering the same deadline on every paint, as
 * a blinking caret does, allocates nothing.</p>
 *
 * <p>The frame rate keeps frames apart: {@link #earliestAllowed()} is one interval after the start of the last frame,
 * and {@link #atNextFrame(long)} registers the next frame something that moves with the clock needs. The rate is
 * {@link ScreenHost#DEFAULT_FRAME_RATE} until set.</p>
 */
public final class FrameRequests {
    private final AtomicBoolean dirty;
    private volatile Runnable requester;
    private long[] deadlines;
    private int count;
    private volatile int rate;
    private volatile long intervalNanos;
    private long lastFrame;
    private boolean framed;

    /**
     * Creates a dirty instance with no requester, no deadlines and the default frame rate.
     */
    public FrameRequests() {
        this.dirty = new AtomicBoolean(true);
        this.deadlines = new long[8];
        this.setRate(ScreenHost.DEFAULT_FRAME_RATE);
    }

    /**
     * Checks a frame rate.
     *
     * @param framesPerSecond the rate
     * @throws IllegalArgumentException if the rate is outside {@link ScreenHost#MIN_FRAME_RATE} to
     *     {@link ScreenHost#MAX_FRAME_RATE}
     */
    public static void checkRate(final int framesPerSecond) {
        if (framesPerSecond < ScreenHost.MIN_FRAME_RATE || framesPerSecond > ScreenHost.MAX_FRAME_RATE) {
            throw new IllegalArgumentException("A frame rate must be from " + ScreenHost.MIN_FRAME_RATE + " to "
                    + ScreenHost.MAX_FRAME_RATE + ": " + framesPerSecond);
        }
    }

    /**
     * Sets the frame rate. Any thread may call this.
     *
     * @param framesPerSecond frames a second
     * @throws IllegalArgumentException if the rate is out of range, as {@link #checkRate} says
     */
    public void setRate(final int framesPerSecond) {
        checkRate(framesPerSecond);
        this.rate = framesPerSecond;
        this.intervalNanos = (1_000_000_000L + framesPerSecond / 2) / framesPerSecond;
    }

    /**
     * Returns the frame rate.
     *
     * @return frames a second
     */
    public int rate() {
        return this.rate;
    }

    /**
     * Returns the time between two frames at the frame rate: one second divided by the rate, rounded.
     *
     * @return the interval in nanoseconds
     */
    public long intervalNanos() {
        return this.intervalNanos;
    }

    /**
     * Notes that a frame starts now. Call on the UI thread, as each frame begins.
     *
     * @param now the clock's reading at the start of the frame
     */
    public void frameStarted(final long now) {
        this.lastFrame = now;
        this.framed = true;
    }

    /**
     * Returns the earliest time the next frame may start: one interval after the start of the last frame. Call on
     * the UI thread.
     *
     * @return the clock's {@code nanoTime()}, or {@link Long#MIN_VALUE} before the first frame
     */
    public long earliestAllowed() {
        return this.framed ? this.lastFrame + this.intervalNanos : Long.MIN_VALUE;
    }

    /**
     * Registers a deadline one interval from now, for something that moves with the clock and needs the next frame
     * the rate allows. Call on the UI thread.
     *
     * @param now the clock's reading
     */
    public void atNextFrame(final long now) {
        this.at(now + this.intervalNanos);
    }

    /**
     * Sets what runs when a frame becomes needed, and runs it now if one already is. Any thread may call this.
     *
     * @param requester the requester, or {@code null} for none
     */
    public void setRequester(final Runnable requester) {
        this.requester = requester;
        if (requester != null && this.dirty.get()) {
            requester.run();
        }
    }

    /**
     * Notes that something changed, and runs the requester if nothing had before. Any thread may call this.
     */
    public void request() {
        if (this.dirty.compareAndSet(false, true)) {
            final Runnable current = this.requester;
            if (current != null) {
                current.run();
            }
        }
    }

    /**
     * Returns whether something changed since the last {@link #clear()}.
     *
     * @return true if a frame is needed
     */
    public boolean needsFrame() {
        return this.dirty.get();
    }

    /**
     * Forgets the changes so far, as a frame about to paint them does. A change after this call requests again.
     */
    public void clear() {
        this.dirty.set(false);
    }

    /**
     * Registers a deadline. Call on the UI thread.
     *
     * @param nanos the {@code Clock.nanoTime()} at which a frame is due
     */
    public void at(final long nanos) {
        for (int i = 0; i < this.count; i++) {
            if (this.deadlines[i] == nanos) {
                return;
            }
        }
        if (this.count == this.deadlines.length) {
            this.deadlines = Arrays.copyOf(this.deadlines, this.count * 2);
        }
        this.deadlines[this.count++] = nanos;
    }

    /**
     * Drops the deadlines a frame starting now serves. Call on the UI thread.
     *
     * @param now the clock's reading at the start of the frame
     */
    public void prune(final long now) {
        int kept = 0;
        for (int i = 0; i < this.count; i++) {
            if (this.deadlines[i] > now) {
                this.deadlines[kept++] = this.deadlines[i];
            }
        }
        this.count = kept;
    }

    /**
     * Returns the earliest deadline. Call on the UI thread.
     *
     * @return the deadline, or {@link Long#MAX_VALUE} if none is registered
     */
    public long earliest() {
        long earliest = Long.MAX_VALUE;
        for (int i = 0; i < this.count; i++) {
            earliest = Math.min(earliest, this.deadlines[i]);
        }
        return earliest;
    }
}
