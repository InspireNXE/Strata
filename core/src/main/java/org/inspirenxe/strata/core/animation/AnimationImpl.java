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

import org.inspirenxe.strata.api.animation.Animation;
import org.inspirenxe.strata.api.animation.Easing;
import org.inspirenxe.strata.api.event.cause.CauseFrame;
import org.inspirenxe.strata.api.event.cause.CauseKeys;
import org.inspirenxe.strata.api.event.cause.Origin;
import org.inspirenxe.strata.core.state.Binding;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.DoubleConsumer;

/**
 * One animation of an {@link AnimatorImpl}. Each step opens a cause frame that pushes the animation and puts
 * {@code Origin.CODE}, and every step but the last also puts {@link Binding#IN_MOTION}, so bindings hand themselves
 * here and are written back once the animation ends or stops.
 */
final class AnimationImpl implements Animation, Binding.Deferral {
    private final AnimatorImpl owner;
    private final long start;
    private final long duration;
    private final Easing easing;
    private final DoubleConsumer step;
    private final Object target;
    private final String keyName;
    private boolean running;
    private boolean finished;
    private List<Runnable> finishedActions;
    private List<Binding<?>> deferred;

    AnimationImpl(final AnimatorImpl owner, final long start, final long duration, final Easing easing,
                  final DoubleConsumer step, final Object target, final String keyName) {
        this.owner = owner;
        this.start = start;
        this.duration = duration;
        this.easing = easing;
        this.step = step;
        this.target = target;
        this.keyName = keyName;
        this.running = true;
    }

    Object target() {
        return this.target;
    }

    String keyName() {
        return this.keyName;
    }

    /**
     * Steps to where the clock says the animation is, ending it once its time is up.
     */
    void stepAt(final long now) {
        final long gone = now - this.start;
        if (this.duration <= 0L || gone >= this.duration) {
            this.complete();
            return;
        }
        final float t = Math.max(0f, (float) ((double) gone / this.duration));
        this.write(this.easing.apply(t), false);
    }

    private void write(final double fraction, final boolean last) {
        try (CauseFrame frame = this.owner.causes().pushFrame().push(this).put(CauseKeys.ORIGIN, Origin.CODE)) {
            if (!last) {
                frame.put(Binding.IN_MOTION, this);
            }
            this.step.accept(fraction);
        }
    }

    /**
     * Ends the animation: takes it out of the running ones, runs the last step, writes back the bindings it holds and
     * runs the finished actions.
     */
    private void complete() {
        this.running = false;
        this.owner.ended(this);
        try {
            this.write(1d, true);
        } finally {
            this.finished = true;
            this.writeBack();
        }
        final List<Runnable> actions = this.finishedActions;
        this.finishedActions = null;
        if (actions != null) {
            for (final Runnable action : actions) {
                action.run();
            }
        }
    }

    private void writeBack() {
        final List<Binding<?>> bindings = this.deferred;
        this.deferred = null;
        if (bindings != null) {
            for (final Binding<?> binding : bindings) {
                binding.writeBack();
            }
        }
    }

    @Override
    public void defer(final Binding<?> binding) {
        if (this.deferred == null) {
            this.deferred = new ArrayList<>(1);
        }
        for (final Binding<?> held : this.deferred) {
            if (held == binding) {
                return;
            }
        }
        this.deferred.add(binding);
    }

    @Override
    public boolean isRunning() {
        return this.running;
    }

    @Override
    public Animation finish() {
        this.owner.uiThread().checkCurrent();
        if (this.running) {
            this.complete();
        }
        return this;
    }

    @Override
    public Animation onFinished(final Runnable action) {
        Objects.requireNonNull(action, "action");
        this.owner.uiThread().checkCurrent();
        if (this.finished) {
            action.run();
        } else if (this.running) {
            if (this.finishedActions == null) {
                this.finishedActions = new ArrayList<>(1);
            }
            this.finishedActions.add(action);
        }
        return this;
    }

    /**
     * Stops the animation where it is. The bindings it holds are written back; the finished actions never run.
     */
    @Override
    public void unsubscribe() {
        if (!this.running) {
            return;
        }
        this.owner.uiThread().checkCurrent();
        this.running = false;
        this.finishedActions = null;
        this.owner.ended(this);
        this.writeBack();
    }

    @Override
    public String toString() {
        return "Animation[" + (this.keyName != null ? this.keyName : "run") + ", " + this.duration / 1_000_000L
                + " ms]";
    }
}
