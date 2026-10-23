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
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.control.attribute.value.Ranged;
import org.inspirenxe.strata.api.control.field.Slider;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.Subscription;
import org.inspirenxe.strata.api.event.control.ValueEvent;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.screen.Screen;
import org.inspirenxe.strata.api.screen.ScreenHandle;
import org.inspirenxe.strata.api.state.PropertyKey;
import org.inspirenxe.strata.api.state.State;
import org.inspirenxe.strata.api.theme.Themes;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;
import org.inspirenxe.strata.core.state.StateImpl;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code Component.unbind}, {@code State.subscribe} and {@code ComponentFactory.watch} (design 4.43).
 */
class WatchingTest {
    private final TestHost test = new TestHost(300, 200);
    private final ComponentFactory ui = this.test.factory();

    private void flush() {
        this.test.host().flush();
    }

    private static int dependents(final State<?> state) {
        return ((StateImpl<?>) state).dependentCount();
    }

    @Nested
    class Unbinding {
        @Test
        void stopsFollowingTheStateBothWaysAndKeepsTheValue() {
            final State<Double> volume = State.of(30.0);
            final Slider slider = WatchingTest.this.ui.slider().setRange(0, 100).setStep(1);
            slider.bind(Ranged.VALUE, volume);
            WatchingTest.this.test.show(slider);
            assertEquals(30.0, slider.value());

            slider.unbind(Ranged.VALUE);
            assertEquals(30.0, slider.value());
            assertEquals(0, dependents(volume));

            volume.set(60.0);
            WatchingTest.this.flush();
            assertEquals(30.0, slider.value());

            slider.requestFocus();
            WatchingTest.this.test.host().key(Key.RIGHT, KeyAction.PRESS, Modifiers.NONE);
            assertEquals(31.0, slider.value());
            assertEquals(60.0, volume.get());
        }

        @Test
        void doesNothingWhenNotBoundAndAllowsBindingAgain() {
            final State<Double> volume = State.of(10.0);
            final Slider slider = WatchingTest.this.ui.slider().setRange(0, 100);
            WatchingTest.this.test.show(slider);
            assertSame(slider, slider.unbind(Ranged.VALUE));

            slider.bind(Ranged.VALUE, volume);
            assertEquals(10.0, slider.value());
            slider.unbind(Ranged.VALUE).bind(Ranged.VALUE, volume);
            volume.set(20.0);
            WatchingTest.this.flush();
            assertEquals(20.0, slider.value());
            assertEquals(1, dependents(volume));
        }

        @Test
        void worksBeforeTheComponentIsAttached() {
            final State<Double> volume = State.of(40.0);
            final Slider slider = WatchingTest.this.ui.slider().setRange(0, 100);
            slider.bind(Ranged.VALUE, volume).unbind(Ranged.VALUE);
            WatchingTest.this.test.show(slider);
            assertEquals(0.0, slider.value());
            assertEquals(0, dependents(volume));
        }

        @Test
        void removesTheBindingOfADevelopersOwnKey() {
            final State<Integer> stars = State.of(3);
            final Rating rating = new Rating(WatchingTest.this.ui);
            rating.bind(Rating.STARS, stars);
            WatchingTest.this.test.show(rating);
            assertEquals(3, rating.stars());

            rating.unbind(Rating.STARS);
            stars.set(5);
            WatchingTest.this.flush();
            assertEquals(3, rating.stars());
            rating.setStars(1);
            assertEquals(5, stars.get());
        }
    }

    @Nested
    class Subscribing {
        @Test
        void callsTheListenerAfterEachChangeWithTheStoredValue() {
            final State<String> name = State.of("Ana");
            final List<String> heard = new ArrayList<>();
            name.subscribe(value -> heard.add(value + "=" + name.get()));
            assertEquals(List.of(), heard);

            name.set("Ben");
            name.set("Ben");
            name.set(null);
            assertEquals(List.of("Ben=Ben", "null=null"), heard);
        }

        @Test
        void runsOnTheSettingThread() throws InterruptedException {
            final State<Integer> count = State.of(0);
            final AtomicReference<Thread> heardOn = new AtomicReference<>();
            count.subscribe(value -> heardOn.set(Thread.currentThread()));
            final Thread setter = new Thread(() -> count.set(1));
            setter.start();
            setter.join();
            assertSame(setter, heardOn.get());
        }

        @Test
        void stopsWhenClosedAndClosingTwiceLeavesTheOthers() {
            final State<Integer> count = State.of(0);
            final List<String> heard = new ArrayList<>();
            final java.util.function.Consumer<Integer> listener = value -> heard.add("a" + value);
            final Subscription first = count.subscribe(listener);
            count.subscribe(listener);
            count.set(1);
            first.unsubscribe();
            first.unsubscribe();
            count.set(2);
            assertEquals(List.of("a1", "a1", "a2"), heard);
        }

        @Test
        void listenersRunInOrderAfterBindingsAreMarked() {
            final State<Double> volume = State.of(10.0);
            final Slider slider = WatchingTest.this.ui.slider().setRange(0, 100).bind(Ranged.VALUE, volume);
            WatchingTest.this.test.show(slider);
            final List<String> heard = new ArrayList<>();
            volume.subscribe(value -> heard.add("first " + value));
            volume.subscribe(value -> heard.add("second " + value));
            volume.set(50.0);
            assertEquals(List.of("first 50.0", "second 50.0"), heard);
            // The binding waits for the frame, as before
            assertEquals(10.0, slider.value());
            WatchingTest.this.flush();
            assertEquals(50.0, slider.value());
        }
    }

    @Nested
    class Watching {
        @Test
        void callsWithTheBindingsOnceAFrameAndNotForTheFirstValue() {
            final State<Integer> count = State.of(1);
            final List<Integer> heard = new ArrayList<>();
            WatchingTest.this.test.show(WatchingTest.this.ui.label("x"));
            WatchingTest.this.ui.watch(count, heard::add);
            WatchingTest.this.flush();
            assertEquals(List.of(), heard);

            count.set(2);
            count.set(3);
            assertEquals(List.of(), heard);
            WatchingTest.this.test.host().layout();
            assertEquals(List.of(3), heard);
        }

        @Test
        void tracksComputedSourcesAndSkipsEqualValues() {
            final State<Integer> price = State.of(3);
            final State<Integer> balance = State.of(10);
            final List<Boolean> heard = new ArrayList<>();
            WatchingTest.this.test.show(WatchingTest.this.ui.label("x"));
            WatchingTest.this.ui.watch(() -> price.get() <= balance.get(), heard::add);

            balance.set(9);
            WatchingTest.this.flush();
            assertEquals(List.of(), heard);
            price.set(12);
            WatchingTest.this.flush();
            balance.set(20);
            WatchingTest.this.flush();
            assertEquals(List.of(false, true), heard);
        }

        @Test
        void aListenerMayChangeStatesThatBindingsFollow() {
            final State<Double> volume = State.of(10.0);
            final State<Double> mirror = State.of(0.0);
            final Slider slider = WatchingTest.this.ui.slider().setRange(0, 100).bind(Ranged.VALUE, mirror);
            WatchingTest.this.test.show(slider);
            WatchingTest.this.ui.watch(volume, mirror::set);
            volume.set(70.0);
            WatchingTest.this.flush();
            assertEquals(70.0, slider.value());
        }

        @Test
        void stopsWhenTheScreenCloses() {
            final State<Integer> count = State.of(1);
            final List<Integer> heard = new ArrayList<>();
            WatchingTest.this.test.host().open(new Screen() {
                @Override
                public Component<?> build(final ComponentFactory ui) {
                    ui.watch(count, heard::add);
                    return ui.label("x");
                }
            });
            assertEquals(1, dependents(count));
            WatchingTest.this.test.host().close();
            assertEquals(0, dependents(count));
            count.set(2);
            WatchingTest.this.flush();
            assertEquals(List.of(), heard);
        }

        @Test
        void aRebuildStopsTheOldBuildsWatchesAndKeepsTheNewOnes() {
            final State<Integer> count = State.of(1);
            final List<String> heard = new ArrayList<>();
            final ScreenHandle[] handle = new ScreenHandle[1];
            final int[] builds = new int[1];
            WatchingTest.this.test.host().open(new Screen() {
                @Override
                public Component<?> build(final ComponentFactory ui) {
                    final int build = ++builds[0];
                    ui.watch(count, value -> heard.add(build + ":" + value));
                    return ui.label("x");
                }

                @Override
                public void opened(final ScreenHandle opened) {
                    handle[0] = opened;
                }
            });
            handle[0].invalidate();
            WatchingTest.this.flush();
            assertEquals(1, dependents(count));
            count.set(2);
            WatchingTest.this.flush();
            assertEquals(List.of("2:2"), heard);
        }

        @Test
        void closingTheSubscriptionStopsIt() {
            final State<Integer> count = State.of(1);
            final List<Integer> heard = new ArrayList<>();
            WatchingTest.this.test.show(WatchingTest.this.ui.label("x"));
            final Subscription watch = WatchingTest.this.ui.watch(count, heard::add);
            watch.unsubscribe();
            assertEquals(0, dependents(count));
            count.set(5);
            WatchingTest.this.flush();
            assertEquals(List.of(), heard);
        }

        @Test
        void aSourceThatThrowsAtFirstIsDropped() {
            final State<Integer> count = State.of(1);
            WatchingTest.this.test.show(WatchingTest.this.ui.label("x"));
            assertThrows(IllegalStateException.class, () -> WatchingTest.this.ui.watch(() -> {
                count.get();
                throw new IllegalStateException("broken");
            }, value -> { }));
            assertEquals(0, dependents(count));
            assertTrue(WatchingTest.this.test.host().root().takeWatches().isEmpty());
        }

        @Test
        void needsAHost() {
            final ComponentFactoryImpl alone = new ComponentFactoryImpl(Themes::flat, new TestPlatform());
            assertThrows(IllegalStateException.class, () -> alone.watch(State.of(1), value -> { }));
        }
    }

    /**
     * A developer's control with a key of its own, written as the binding guide shows.
     */
    static final class Rating extends CustomComponent<Rating> {
        static final EventType<ValueEvent<Rating, Integer>> RATED = EventType.valueReport("rating.rated");
        static final PropertyKey<Rating, Integer> STARS = PropertyKey.of("rating.stars", Rating::stars,
                Rating::setStars, RATED);

        private int stars;

        Rating(final ComponentFactory ui) {
            super(ui);
        }

        int stars() {
            return this.stars;
        }

        Rating setStars(final int stars) {
            final int previous = this.stars;
            if (previous != stars) {
                this.stars = stars;
                this.report(RATED, stars, previous);
            }
            return this;
        }

        @Override
        protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
            return new Size(50, 10);
        }
    }
}
