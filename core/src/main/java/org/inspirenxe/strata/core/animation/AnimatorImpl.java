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
package org.inspirenxe.strata.core.animation;

import org.inspirenxe.strata.api.UiThread;
import org.inspirenxe.strata.api.animation.Animation;
import org.inspirenxe.strata.api.animation.Animator;
import org.inspirenxe.strata.api.animation.Easing;
import org.inspirenxe.strata.api.animation.Interpolator;
import org.inspirenxe.strata.api.event.cause.CauseStack;
import org.inspirenxe.strata.api.state.PropertyKey;
import org.inspirenxe.strata.core.frame.FrameRequests;
import org.inspirenxe.strata.spi.Clock;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.DoubleConsumer;
import java.util.function.Supplier;

/**
 * The {@link Animator} of one root. The host calls {@link #step()} in every flush, between the tasks handed to the
 * UI thread and the bindings.
 *
 * <p>A step reads the clock once and steps every running animation with it, in the order they started. While any
 * still runs afterwards it registers a frame {@link FrameRequests#CONTINUOUS_MILLIS} later, and starting one asks for
 * a frame at once. Stepping allocates only what each step's cause frame and the animated values need.</p>
 *
 * <p>An animation whose step throws is stopped where it is, the others still step, and the first exception goes on
 * to the host's caller once they have.</p>
 */
public final class AnimatorImpl implements Animator {
    private final Supplier<Clock> clock;
    private final Supplier<CauseStack> causes;
    private final FrameRequests frames;
    private final UiThread uiThread;
    private final List<AnimationImpl> running;
    // The animations of the step in progress, reused so a step allocates no list
    private final List<AnimationImpl> stepping;

    /**
     * Creates an animator with nothing running.
     *
     * @param clock supplies the platform's clock
     * @param causes supplies the cause stack the steps open their frames on
     * @param frames the root's frame requests
     * @param uiThread the host's UI thread, which every call must be on
     */
    public AnimatorImpl(final Supplier<Clock> clock, final Supplier<CauseStack> causes, final FrameRequests frames,
                        final UiThread uiThread) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.causes = Objects.requireNonNull(causes, "causes");
        this.frames = Objects.requireNonNull(frames, "frames");
        this.uiThread = Objects.requireNonNull(uiThread, "uiThread");
        this.running = new ArrayList<>();
        this.stepping = new ArrayList<>();
    }

    @Override
    public Animation run(final long millis, final Easing easing, final DoubleConsumer step) {
        Objects.requireNonNull(step, "step");
        return this.start(millis, easing, step, null, null);
    }

    @Override
    public <C, V> Animation animate(final C target, final PropertyKey<? super C, V> key, final V to,
                                    final long millis, final Easing easing, final Interpolator<V> interpolator) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(interpolator, "interpolator");
        this.uiThread.checkCurrent();
        final V from = key.reader().apply(target);
        for (final AnimationImpl other : List.copyOf(this.running)) {
            if (other.target() == target && key.name().equals(other.keyName())) {
                other.unsubscribe();
            }
        }
        return this.start(millis, easing, fraction -> key.writer().accept(target,
                fraction == 1d ? to : interpolator.between(from, to, (float) fraction)), target, key.name());
    }

    @Override
    public <C> Animation animate(final C target, final PropertyKey<? super C, Double> key, final double to,
                                 final long millis, final Easing easing) {
        return this.animate(target, key, to, millis, easing, Interpolator.DOUBLE);
    }

    private AnimationImpl start(final long millis, final Easing easing, final DoubleConsumer step,
                                final Object target, final String keyName) {
        Objects.requireNonNull(easing, "easing");
        if (millis < 0L) {
            throw new IllegalArgumentException("An animation must not take negative time: " + millis);
        }
        this.uiThread.checkCurrent();
        final AnimationImpl animation = new AnimationImpl(this, this.clock.get().nanoTime(), millis * 1_000_000L,
                easing, step, target, keyName);
        this.running.add(animation);
        this.frames.request();
        return animation;
    }

    /**
     * Steps every running animation by the clock. Does nothing, and reads no clock, when none runs.
     *
     * @throws RuntimeException the first exception a step threw, after the other animations stepped
     */
    public void step() {
        if (this.running.isEmpty()) {
            return;
        }
        final long now = this.clock.get().nanoTime();
        this.stepping.addAll(this.running);
        RuntimeException failure = null;
        try {
            for (int i = 0; i < this.stepping.size(); i++) {
                final AnimationImpl animation = this.stepping.get(i);
                if (!animation.isRunning()) {
                    continue;
                }
                try {
                    animation.stepAt(now);
                } catch (final RuntimeException e) {
                    animation.unsubscribe();
                    if (failure == null) {
                        failure = e;
                    } else {
                        failure.addSuppressed(e);
                    }
                }
            }
        } finally {
            this.stepping.clear();
        }
        if (!this.running.isEmpty()) {
            this.frames.at(now + FrameRequests.CONTINUOUS_MILLIS * 1_000_000L);
        }
        if (failure != null) {
            throw failure;
        }
    }

    /**
     * Returns whether any animation runs.
     *
     * @return true while one runs
     */
    public boolean isAnimating() {
        return !this.running.isEmpty();
    }

    /**
     * Stops every running animation where it is, as closing each would, when the screen closes.
     */
    public void stopAll() {
        for (final AnimationImpl animation : List.copyOf(this.running)) {
            animation.unsubscribe();
        }
    }

    CauseStack causes() {
        return this.causes.get();
    }

    UiThread uiThread() {
        return this.uiThread;
    }

    void ended(final AnimationImpl animation) {
        this.running.remove(animation);
    }
}
