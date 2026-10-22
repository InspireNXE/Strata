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
package org.inspirenxe.strata.core;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.animation.Animation;
import org.inspirenxe.strata.api.animation.Animator;
import org.inspirenxe.strata.api.animation.Easing;
import org.inspirenxe.strata.api.animation.Interpolator;
import org.inspirenxe.strata.api.control.attribute.value.Ranged;
import org.inspirenxe.strata.api.control.display.ProgressBar;
import org.inspirenxe.strata.api.control.field.Slider;
import org.inspirenxe.strata.api.event.cause.Origin;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.state.State;
import org.inspirenxe.strata.api.theme.Themes;
import org.inspirenxe.strata.core.frame.FrameRequests;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Animation (design 4.51): the animator's steps by the test platform's clock, property animations, causes, bindings,
 * frames, and the easings and interpolators.
 */
class AnimationTest {
    private static final long MS = 1_000_000L;

    private final TestPlatform platform = new TestPlatform();
    private final TestHost test = new TestHost(this.platform, Themes.flat(), 300, 200);
    private final ComponentFactory ui = this.test.factory();
    private final Animator animator = this.ui.animator();

    private void frame(final long afterMillis) {
        this.platform.advanceMillis(afterMillis);
        this.test.host().render(new NullCanvas());
    }

    @Nested
    class Runs {
        @Test
        void stepsByTheClockAndEndsOnExactlyOne() {
            AnimationTest.this.test.show(AnimationTest.this.ui.label());
            final List<Double> steps = new ArrayList<>();
            final AtomicInteger finished = new AtomicInteger();
            final Animation animation = AnimationTest.this.animator.run(200L, Easing.LINEAR, steps::add)
                    .onFinished(finished::incrementAndGet);
            AnimationTest.this.frame(50L);
            AnimationTest.this.frame(50L);
            assertEquals(List.of(0.25, 0.5), steps);
            assertTrue(animation.isRunning());
            AnimationTest.this.frame(150L);
            assertEquals(List.of(0.25, 0.5, 1.0), steps, "past the end, the last step is 1");
            assertFalse(animation.isRunning());
            assertEquals(1, finished.get());
            AnimationTest.this.frame(50L);
            assertEquals(3, steps.size());
        }

        @Test
        void theLastStepIsOneWhateverTheEasing() {
            AnimationTest.this.test.show(AnimationTest.this.ui.label());
            final List<Double> steps = new ArrayList<>();
            AnimationTest.this.animator.run(100L, t -> t * 0.5f, steps::add);
            AnimationTest.this.frame(50L);
            AnimationTest.this.frame(50L);
            assertEquals(List.of(0.25, 1.0), steps);
        }

        @Test
        void noTimeStepsOnceOnTheNextFrame() {
            AnimationTest.this.test.show(AnimationTest.this.ui.label());
            final List<Double> steps = new ArrayList<>();
            final Animation animation = AnimationTest.this.animator.run(0L, Easing.EASE_OUT, steps::add);
            assertTrue(steps.isEmpty());
            AnimationTest.this.frame(0L);
            assertEquals(List.of(1.0), steps);
            assertFalse(animation.isRunning());
        }

        @Test
        void finishJumpsToTheEndAndCloseStopsWhereItIs() {
            AnimationTest.this.test.show(AnimationTest.this.ui.label());
            final List<Double> steps = new ArrayList<>();
            final AtomicInteger finished = new AtomicInteger();
            final Animation jumped = AnimationTest.this.animator.run(200L, Easing.LINEAR, steps::add)
                    .onFinished(finished::incrementAndGet);
            jumped.finish();
            assertEquals(List.of(1.0), steps);
            assertEquals(1, finished.get());
            jumped.onFinished(finished::incrementAndGet);
            assertEquals(2, finished.get(), "an action added after the end runs at once");

            final List<Double> other = new ArrayList<>();
            final Animation closed = AnimationTest.this.animator.run(200L, Easing.LINEAR, other::add)
                    .onFinished(finished::incrementAndGet);
            AnimationTest.this.frame(100L);
            closed.close();
            AnimationTest.this.frame(100L);
            assertEquals(List.of(0.5), other);
            assertEquals(2, finished.get(), "a closed animation never finishes");
            closed.onFinished(finished::incrementAndGet);
            assertEquals(2, finished.get());
        }

        @Test
        void aNegativeDurationIsRefused() {
            assertThrows(IllegalArgumentException.class,
                    () -> AnimationTest.this.animator.run(-1L, Easing.LINEAR, fraction -> { }));
        }

        @Test
        void aStepThatThrowsStopsOnlyItsOwnAnimation() {
            AnimationTest.this.test.show(AnimationTest.this.ui.label());
            final Animation failing = AnimationTest.this.animator.run(200L, Easing.LINEAR, fraction -> {
                throw new IllegalStateException("broken step");
            });
            final List<Double> steps = new ArrayList<>();
            final Animation fine = AnimationTest.this.animator.run(200L, Easing.LINEAR, steps::add);
            AnimationTest.this.platform.advanceMillis(100L);
            assertThrows(IllegalStateException.class, () -> AnimationTest.this.test.host().flush());
            assertFalse(failing.isRunning());
            assertTrue(fine.isRunning());
            assertEquals(List.of(0.5), steps);
        }

        @Test
        void callsOffTheUiThreadThrow() throws InterruptedException {
            AnimationTest.this.test.show(AnimationTest.this.ui.label());
            final AtomicReference<Throwable> thrown = new AtomicReference<>();
            final Thread worker = new Thread(() -> {
                try {
                    AnimationTest.this.animator.run(100L, Easing.LINEAR, fraction -> { });
                } catch (final Throwable t) {
                    thrown.set(t);
                }
            });
            worker.start();
            worker.join();
            assertInstanceOf(IllegalStateException.class, thrown.get());
        }

        @Test
        void aClosedScreenStopsItsAnimations() {
            AnimationTest.this.test.show(AnimationTest.this.ui.label());
            final AtomicInteger finished = new AtomicInteger();
            final Animation animation = AnimationTest.this.animator.run(200L, Easing.LINEAR, fraction -> { })
                    .onFinished(finished::incrementAndGet);
            AnimationTest.this.test.host().close();
            assertFalse(animation.isRunning());
            AnimationTest.this.frame(300L);
            assertEquals(0, finished.get());
        }
    }

    @Nested
    class Properties {
        @Test
        void aPropertyMovesFromWhereItWasToItsEnd() {
            final ProgressBar bar = AnimationTest.this.ui.progressBar().setValue(0.2);
            AnimationTest.this.test.show(bar);
            AnimationTest.this.animator.animate(bar, Ranged.VALUE, 0.6, 100L, Easing.LINEAR);
            AnimationTest.this.frame(50L);
            assertEquals(0.4, bar.value(), 1e-6);
            AnimationTest.this.frame(50L);
            assertEquals(0.6, bar.value(), "the last step writes the end value itself");
        }

        @Test
        void aSecondAnimationOfTheSameKeyStopsTheFirst() {
            final ProgressBar bar = AnimationTest.this.ui.progressBar();
            AnimationTest.this.test.show(bar);
            final Animation first = AnimationTest.this.animator.animate(bar, Ranged.VALUE, 1.0, 100L, Easing.LINEAR);
            AnimationTest.this.frame(50L);
            final Animation second = AnimationTest.this.animator.animate(bar, Ranged.VALUE, 0.0, 100L, Easing.LINEAR);
            assertFalse(first.isRunning());
            AnimationTest.this.frame(50L);
            assertEquals(0.25, bar.value(), 1e-6, "from where the first stopped");
            assertTrue(second.isRunning());
        }

        @Test
        void stepsReportFromCodeWithTheAnimationOnTheChain() {
            final Slider slider = AnimationTest.this.ui.slider();
            AnimationTest.this.test.show(slider);
            final AtomicInteger requests = new AtomicInteger();
            final List<Origin> origins = new ArrayList<>();
            final List<Animation> chains = new ArrayList<>();
            slider.onEvent(Ranged.CHANGE, event -> requests.incrementAndGet());
            slider.onEvent(Ranged.CHANGED, event -> {
                origins.add(event.origin());
                event.cause().first(Animation.class).ifPresent(chains::add);
            });
            final Animation animation = AnimationTest.this.animator.animate(slider, Ranged.VALUE, 1.0, 100L,
                    Easing.LINEAR);
            AnimationTest.this.frame(50L);
            AnimationTest.this.frame(50L);
            assertEquals(0, requests.get());
            assertEquals(List.of(Origin.CODE, Origin.CODE), origins);
            assertEquals(List.of(animation, animation), chains);
        }

        @Test
        void aBoundStateHearsOnlyWhereTheAnimationEnds() {
            final State<Double> volume = State.of(0.0);
            final Slider slider = AnimationTest.this.ui.slider().bind(Ranged.VALUE, volume);
            AnimationTest.this.test.show(slider);
            AnimationTest.this.animator.animate(slider, Ranged.VALUE, 0.8, 100L, Easing.LINEAR);
            AnimationTest.this.frame(25L);
            AnimationTest.this.frame(25L);
            assertEquals(0.0, volume.get(), "nothing written while it moves");
            AnimationTest.this.frame(50L);
            assertEquals(0.8, volume.get());

            final Animation stopped = AnimationTest.this.animator.animate(slider, Ranged.VALUE, 0.0, 100L,
                    Easing.LINEAR);
            AnimationTest.this.frame(50L);
            assertEquals(0.8, volume.get());
            stopped.close();
            assertEquals(0.4, volume.get(), 1e-6, "a stopped animation leaves the state where it stopped");
        }

        @Test
        void insetsAndColorsInterpolate() {
            final AtomicReference<Integer> color = new AtomicReference<>(0xFF000000);
            final org.inspirenxe.strata.api.state.PropertyKey<AtomicReference<Integer>, Integer> key =
                    org.inspirenxe.strata.api.state.PropertyKey.of("test.color", AtomicReference::get,
                            AtomicReference::set);
            AnimationTest.this.test.show(AnimationTest.this.ui.label());
            AnimationTest.this.animator.animate(color, key, 0x80FF8000, 100L, Easing.LINEAR, Interpolator.ARGB);
            AnimationTest.this.frame(50L);
            assertEquals(0xC0804000, color.get());
            assertEquals(new Insets(2, 4, 6, 8),
                    Interpolator.INSETS.between(Insets.NONE, new Insets(4, 8, 12, 16), 0.5f));
            assertEquals(Insets.NONE, Interpolator.INSETS.between(new Insets(4, 4, 4, 4), new Insets(0, 0, 0, 0),
                    1.5f), "kept from going below 0");
        }
    }

    @Nested
    class Frames {
        @Test
        void aRunningAnimationAsksForFramesUntilItEnds() {
            final ProgressBar bar = AnimationTest.this.ui.progressBar();
            AnimationTest.this.test.show(bar);
            AnimationTest.this.frame(0L);
            assertFalse(AnimationTest.this.test.host().needsFrame());
            AnimationTest.this.animator.animate(bar, Ranged.VALUE, 1.0, 100L, Easing.EASE_IN_OUT);
            assertTrue(AnimationTest.this.test.host().needsFrame(), "starting asks at once");
            AnimationTest.this.frame(40L);
            assertEquals((40L + FrameRequests.CONTINUOUS_MILLIS) * MS, AnimationTest.this.test.host().nextFrameNanos());
            AnimationTest.this.frame(60L);
            assertEquals(Long.MAX_VALUE, AnimationTest.this.test.host().nextFrameNanos());
        }
    }

    @Nested
    class Easings {
        @Test
        void theCurvesKeepTheirEnds() {
            for (final Easing easing : List.of(Easing.LINEAR, Easing.EASE_IN, Easing.EASE_OUT, Easing.EASE_IN_OUT)) {
                assertEquals(0f, easing.apply(0f));
                assertEquals(1f, easing.apply(1f));
            }
        }

        @Test
        void theCurvesHaveTheirShapes() {
            assertTrue(Easing.EASE_IN.apply(0.25f) < 0.25f, "ease in starts slowly");
            assertTrue(Easing.EASE_OUT.apply(0.25f) > 0.25f, "ease out starts fast");
            assertEquals(0.5f, Easing.EASE_IN_OUT.apply(0.5f), 1e-4f, "ease in and out is symmetric");
            // CSS's ease-out at a quarter of the time
            assertEquals(0.3781f, Easing.EASE_OUT.apply(0.25f), 1e-3f);
            assertEquals(0.3f, Easing.cubicBezier(0f, 0f, 1f, 1f).apply(0.3f), 1e-3f, "a straight curve is linear");
        }

        @Test
        void aCurveMayOvershoot() {
            final Easing back = Easing.cubicBezier(0.34f, 1.56f, 0.64f, 1f);
            float highest = 0f;
            for (int i = 1; i < 100; i++) {
                highest = Math.max(highest, back.apply(i / 100f));
            }
            assertTrue(highest > 1f, "overshoots: " + highest);
        }

        @Test
        void aControlPointOutsideTheTimeIsRefused() {
            assertThrows(IllegalArgumentException.class, () -> Easing.cubicBezier(1.5f, 0f, 0.5f, 1f));
            assertThrows(IllegalArgumentException.class, () -> Easing.cubicBezier(0.5f, Float.NaN, 0.5f, 1f));
        }

        @Test
        void interpolatorsKeepTheirEnds() {
            assertSame(Insets.NONE, Interpolator.INSETS.between(Insets.NONE, new Insets(1, 1, 1, 1), 0f));
            assertEquals(0.3, Interpolator.DOUBLE.between(0.1, 0.3, 1f));
            assertEquals(2f, Interpolator.FLOAT.between(1f, 2f, 1f));
            assertEquals(0x12345678, Interpolator.ARGB.between(0x12345678, 0, 0f));
        }
    }
}
