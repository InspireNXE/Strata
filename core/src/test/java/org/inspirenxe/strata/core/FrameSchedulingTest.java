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
import org.inspirenxe.strata.api.component.Tooltip;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.button.CheckBox;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.control.display.ProgressBar;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.event.input.InputEvents;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.inspirenxe.strata.api.overlay.Overlay;
import org.inspirenxe.strata.api.screen.Screen;
import org.inspirenxe.strata.api.state.State;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.Themes;
import org.inspirenxe.strata.core.frame.FrameRequests;
import org.inspirenxe.strata.core.host.ScreenHostImpl;
import org.inspirenxe.strata.spi.ScreenStack;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Frames on demand (design 4.47): when a host needs a frame, when the requester runs, and the deadline each timed
 * change reports.
 */
class FrameSchedulingTest {
    private static final long MS = 1_000_000L;

    private final TestPlatform platform = new TestPlatform();
    private final TestHost test = new TestHost(this.platform, Themes.flat(), 300, 200);
    private final ScreenHostImpl host = this.test.host();
    private final ComponentFactory ui = this.test.factory();
    private final NullCanvas canvas = new NullCanvas();
    private final AtomicInteger requests = new AtomicInteger();

    private void render() {
        this.host.render(this.canvas);
    }

    /**
     * Shows a tree, draws it and starts counting requests from a clean host.
     */
    private void showClean(final Component<?> content) {
        this.test.show(content);
        this.render();
        this.host.setFrameRequester(this.requests::incrementAndGet);
        assertFalse(this.host.needsFrame(), "clean after a frame");
    }

    private long now() {
        return this.platform.clock().nanoTime();
    }

    /**
     * A gauge whose needle moves through a setter that changes no size, written as a developer would.
     */
    static final class Gauge extends CustomComponent<Gauge> {
        private float needle;

        Gauge(final ComponentFactory factory) {
            super(factory);
        }

        Gauge setNeedle(final float needle) {
            this.needle = needle;
            this.repaint();
            return this;
        }

        Gauge pulseIn(final long millis) {
            this.repaintAfter(millis);
            return this;
        }

        @Override
        protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
            return new Size(40, 10);
        }

        @Override
        protected void paint(final Painter painter, final Rect bounds) {
            painter.fill(new Rect(bounds.x(), bounds.y(), bounds.width() * this.needle, bounds.height()), 0xFFFFFFFF);
        }
    }

    @Nested
    class Requests {
        @Test
        void aNewHostNeedsAFrameAndAnIdleOneNeedsNone() {
            assertTrue(FrameSchedulingTest.this.host.needsFrame());
            FrameSchedulingTest.this.test.show(FrameSchedulingTest.this.ui.label(Text.of("Idle")));
            FrameSchedulingTest.this.render();
            assertFalse(FrameSchedulingTest.this.host.needsFrame());
            assertEquals(Long.MAX_VALUE, FrameSchedulingTest.this.host.nextFrameNanos(), "nothing timed");
        }

        @Test
        void aRequesterSetWhileDirtyRunsAtOnce() {
            final AtomicInteger ran = new AtomicInteger();
            FrameSchedulingTest.this.host.setFrameRequester(ran::incrementAndGet);
            assertEquals(1, ran.get());
        }

        @Test
        void theRequesterRunsOncePerChangeFromCleanToDirty() {
            final Label label = FrameSchedulingTest.this.ui.label(Text.of("Price"));
            FrameSchedulingTest.this.showClean(label);
            label.setText(Text.of("Cost"));
            label.setText(Text.of("Fee"));
            assertTrue(FrameSchedulingTest.this.host.needsFrame());
            assertEquals(1, FrameSchedulingTest.this.requests.get());
            FrameSchedulingTest.this.render();
            assertFalse(FrameSchedulingTest.this.host.needsFrame());
            label.setText(Text.of("Toll"));
            assertEquals(2, FrameSchedulingTest.this.requests.get());
        }

        @Test
        void inputAsksForAFrame() {
            FrameSchedulingTest.this.showClean(FrameSchedulingTest.this.ui.label(Text.of("Price")));
            FrameSchedulingTest.this.host.pointerMoved(5, 5, Modifiers.NONE);
            assertTrue(FrameSchedulingTest.this.host.needsFrame());
        }

        @Test
        void aReportFromCodeAsksForAFrame() {
            final CheckBox box = FrameSchedulingTest.this.ui.checkBox(Text.of("Sold"));
            FrameSchedulingTest.this.showClean(box);
            box.setSelected(true);
            assertTrue(FrameSchedulingTest.this.host.needsFrame());
        }

        @Test
        void aBackgroundChangeAsksForAFrame() {
            final StackPanel panel = FrameSchedulingTest.this.ui.stackPanel();
            FrameSchedulingTest.this.showClean(panel);
            panel.setBackground(Paint.solid(0xFF203040));
            assertTrue(FrameSchedulingTest.this.host.needsFrame());
        }

        @Test
        void overlaysAskWhenTheyOpenAndClose() {
            FrameSchedulingTest.this.showClean(FrameSchedulingTest.this.ui.label(Text.of("Base")));
            final Overlay popup = FrameSchedulingTest.this.ui.overlays().popup(FrameSchedulingTest.this.ui
                    .label(Text.of("Menu")), new Rect(0, 0, 10, 10));
            assertTrue(FrameSchedulingTest.this.host.needsFrame());
            FrameSchedulingTest.this.render();
            assertFalse(FrameSchedulingTest.this.host.needsFrame());
            popup.close();
            assertTrue(FrameSchedulingTest.this.host.needsFrame());
        }

        @Test
        void aComponentOfYourOwnRepaints() {
            final Gauge gauge = new Gauge(FrameSchedulingTest.this.ui);
            FrameSchedulingTest.this.showClean(gauge);
            gauge.setNeedle(0.5f);
            assertTrue(FrameSchedulingTest.this.host.needsFrame());
            assertEquals(1, FrameSchedulingTest.this.requests.get());
        }

        @Test
        void aStateSetOnAnotherThreadRunsTheRequesterThere() throws InterruptedException {
            final State<Text> price = State.of(Text.of("1"));
            final Label label = FrameSchedulingTest.this.ui.label().bind(Label.TEXT, price);
            FrameSchedulingTest.this.showClean(label);
            final AtomicReference<String> requestedOn = new AtomicReference<>();
            FrameSchedulingTest.this.host.setFrameRequester(() -> requestedOn.set(Thread.currentThread().getName()));
            final Thread network = new Thread(() -> price.set(Text.of("2")), "network");
            network.start();
            network.join();
            assertEquals("network", requestedOn.get());
            assertTrue(FrameSchedulingTest.this.host.needsFrame());
        }

        @Test
        void aTaskHandedOverAsksForAFrame() throws InterruptedException {
            FrameSchedulingTest.this.showClean(FrameSchedulingTest.this.ui.label(Text.of("Base")));
            final Thread worker = new Thread(() -> FrameSchedulingTest.this.ui.uiThread().execute(() -> { }));
            worker.start();
            worker.join();
            assertTrue(FrameSchedulingTest.this.host.needsFrame());
            FrameSchedulingTest.this.render();
            assertFalse(FrameSchedulingTest.this.host.needsFrame(), "the frame ran the task");
        }

        @Test
        void aChangeWhilePaintingAsksForTheNextFrame() {
            final Label label = FrameSchedulingTest.this.ui.label(Text.of("0"));
            FrameSchedulingTest.this.showClean(label);
            final AtomicInteger frames = new AtomicInteger();
            label.onEvent(Component.PAINTED, event -> {
                if (frames.incrementAndGet() == 1) {
                    label.setText(Text.of("1"));
                }
            });
            label.setText(Text.of("2"));
            FrameSchedulingTest.this.render();
            assertTrue(FrameSchedulingTest.this.host.needsFrame());
        }

        @Test
        void aBindingQueuedAfterTheFlushStillAsks() {
            final TestPlatform platform = new TestPlatform(false);
            final TestHost test = new TestHost(platform, Themes.flat(), 300, 200);
            final State<Text> count = State.of(Text.of("0"));
            final Label label = test.factory().label().bind(Label.TEXT, count);
            final TextBox box = test.factory().textBox();
            final StackPanel panel = test.factory().stackPanel();
            panel.add(label);
            panel.add(box);
            test.show(panel);
            box.requestFocus();
            // A repeat the root makes itself fires during the render, after the flush
            box.onEvent(InputEvents.KEY, event -> {
                if (event.action() == KeyAction.REPEAT) {
                    count.set(Text.of("repeated"));
                }
            });
            test.host().key(Key.RIGHT, KeyAction.PRESS, Modifiers.NONE);
            test.host().render(FrameSchedulingTest.this.canvas);
            platform.advanceMillis(1_000L);
            test.host().render(FrameSchedulingTest.this.canvas);
            assertTrue(test.host().needsFrame(), "the binding waits for the next frame");
            test.host().render(FrameSchedulingTest.this.canvas);
            assertEquals("repeated", label.text().plain());
        }
    }

    @Nested
    class Deadlines {
        @Test
        void repaintAfterReportsItsDeadlineUntilAFrameReachesIt() {
            final Gauge gauge = new Gauge(FrameSchedulingTest.this.ui);
            FrameSchedulingTest.this.showClean(gauge);
            gauge.pulseIn(250L).pulseIn(250L).pulseIn(400L);
            assertEquals(250L * MS, FrameSchedulingTest.this.host.nextFrameNanos(), "the earliest, counted once");
            FrameSchedulingTest.this.platform.advanceMillis(250L);
            FrameSchedulingTest.this.render();
            assertEquals(400L * MS, FrameSchedulingTest.this.host.nextFrameNanos());
            FrameSchedulingTest.this.platform.advanceMillis(150L);
            FrameSchedulingTest.this.render();
            assertEquals(Long.MAX_VALUE, FrameSchedulingTest.this.host.nextFrameNanos());
            assertFalse(FrameSchedulingTest.this.host.needsFrame());
        }

        @Test
        void theCaretAsksForEachBlink() {
            final TextBox box = FrameSchedulingTest.this.ui.textBox();
            FrameSchedulingTest.this.showClean(box);
            box.requestFocus();
            FrameSchedulingTest.this.render();
            final long first = FrameSchedulingTest.this.host.nextFrameNanos();
            assertTrue(first > FrameSchedulingTest.this.now() && first != Long.MAX_VALUE);
            FrameSchedulingTest.this.platform.advanceMillis((first - FrameSchedulingTest.this.now()) / MS);
            FrameSchedulingTest.this.render();
            assertEquals(first * 2, FrameSchedulingTest.this.host.nextFrameNanos(), "one blink later");
            box.setFocusable(false);
            FrameSchedulingTest.this.render();
            FrameSchedulingTest.this.platform.advanceMillis(first * 2 / MS);
            FrameSchedulingTest.this.render();
            assertEquals(Long.MAX_VALUE, FrameSchedulingTest.this.host.nextFrameNanos(), "no caret, no blink");
        }

        @Test
        void aWaitingTooltipReportsTheEndOfItsDelay() {
            FrameSchedulingTest.this.host.setTooltipDelay(300L);
            final Label label = FrameSchedulingTest.this.ui.label(Text.of("Price")).setTooltip(Tooltip.text("Tip"));
            FrameSchedulingTest.this.showClean(label);
            FrameSchedulingTest.this.platform.advanceMillis(100L);
            FrameSchedulingTest.this.host.pointerMoved(2, 2, Modifiers.NONE);
            FrameSchedulingTest.this.render();
            assertEquals(400L * MS, FrameSchedulingTest.this.host.nextFrameNanos());
        }

        @Test
        void aToastReportsItsExpiry() {
            FrameSchedulingTest.this.showClean(FrameSchedulingTest.this.ui.label(Text.of("Base")));
            FrameSchedulingTest.this.ui.overlays().toast(FrameSchedulingTest.this.ui.label(Text.of("Saved")), 2_000L);
            FrameSchedulingTest.this.render();
            assertEquals(2_000L * MS, FrameSchedulingTest.this.host.nextFrameNanos());
            FrameSchedulingTest.this.platform.advanceMillis(2_000L);
            FrameSchedulingTest.this.render();
            assertTrue(FrameSchedulingTest.this.ui.overlays().open().isEmpty());
        }

        @Test
        void aHeldKeyReportsItsNextRepeat() {
            final TestPlatform platform = new TestPlatform(false);
            final TestHost test = new TestHost(platform, Themes.flat(), 300, 200);
            final TextBox box = test.factory().textBox();
            test.show(box);
            box.requestFocus();
            test.host().key(Key.LEFT, KeyAction.PRESS, Modifiers.NONE);
            test.host().render(FrameSchedulingTest.this.canvas);
            assertEquals(400L * MS, test.host().nextFrameNanos(), "the first repeat comes before the caret blinks");
        }

        @Test
        void theBusyIndicatorAsksForItsNextStep() {
            FrameSchedulingTest.this.showClean(FrameSchedulingTest.this.ui.busyIndicator());
            final long next = FrameSchedulingTest.this.host.nextFrameNanos();
            assertTrue(next > 0L && next <= 1_000L * MS, "a step within a second: " + next);
        }

        @Test
        void anIndeterminateProgressBarAsksForItsNextStep() {
            final ProgressBar bar = FrameSchedulingTest.this.ui.progressBar().setIndeterminate(true);
            bar.setWidth(100f);
            FrameSchedulingTest.this.showClean(bar);
            final long next = FrameSchedulingTest.this.host.nextFrameNanos();
            assertTrue(next > 0L && next < Long.MAX_VALUE, "the segment moves: " + next);
            bar.setIndeterminate(false);
            FrameSchedulingTest.this.platform.advanceMillis(next / MS);
            FrameSchedulingTest.this.render();
            assertEquals(Long.MAX_VALUE, FrameSchedulingTest.this.host.nextFrameNanos(), "a still bar asks for nothing");
        }

        @Test
        void aSlidingCaptionAsksEveryFrame() {
            final Theme scrolling = Themes.flat().toBuilder().metric(Button.STYLE_TEXT_SCROLL, 500f)
                    .metric(Button.STYLE_TEXT_SCROLL_MINIMUM, 3000f).build();
            final TestHost test = new TestHost(new TestPlatform(), scrolling, 300, 200);
            final Button button = test.factory().button(Text.of("a caption far too wide for it"));
            button.setWidth(50f);
            test.show(button);
            test.host().render(FrameSchedulingTest.this.canvas);
            assertEquals(FrameRequests.CONTINUOUS_MILLIS * MS, test.host().nextFrameNanos());
        }

        @Test
        void aCalendarAsksForTheNextReadingOfToday() {
            FrameSchedulingTest.this.showClean(FrameSchedulingTest.this.ui.calendarView());
            assertEquals(60_000L * MS, FrameSchedulingTest.this.host.nextFrameNanos());
        }
    }

    @Nested
    class Stacks {
        private final TestPlatform platform = new TestPlatform();
        private final ScreenStack stack = StrataCore.createScreenStack(this.platform, Themes.flat());
        private final AtomicInteger requests = new AtomicInteger();

        Stacks() {
            this.stack.resize(300, 200);
        }

        @Test
        void aStackAsksWhenAScreenChangesOrIsPushed() {
            final Label[] label = new Label[1];
            this.stack.push(ui -> label[0] = ui.label(Text.of("Base")));
            this.stack.render(FrameSchedulingTest.this.canvas);
            this.stack.setFrameRequester(this.requests::incrementAndGet);
            assertFalse(this.stack.needsFrame());
            label[0].setText(Text.of("Changed"));
            assertTrue(this.stack.needsFrame());
            assertEquals(1, this.requests.get());
            this.stack.render(FrameSchedulingTest.this.canvas);
            assertFalse(this.stack.needsFrame());
            this.stack.push(ui -> ui.label(Text.of("Top")));
            assertTrue(this.stack.needsFrame());
            this.stack.render(FrameSchedulingTest.this.canvas);
            this.stack.pop();
            assertTrue(this.stack.needsFrame(), "the screen below shows again");
        }

        @Test
        void aHiddenScreenDoesNotCount() {
            this.stack.push(ui -> ui.textBox());
            final Screen cover = new Screen() {
                @Override
                public Component<?> build(final ComponentFactory factory) {
                    return factory.label(Text.of("Cover"));
                }

                @Override
                public boolean hidesBackground() {
                    return true;
                }
            };
            this.stack.push(cover);
            this.stack.render(FrameSchedulingTest.this.canvas);
            assertEquals(Long.MAX_VALUE, this.stack.nextFrameNanos());
            assertFalse(this.stack.needsFrame());
        }

        @Test
        void theEarliestDeadlineOfTheShownScreens() {
            this.stack.push(ui -> ui.busyIndicator());
            this.stack.push(ui -> ui.calendarView());
            this.stack.render(FrameSchedulingTest.this.canvas);
            final long next = this.stack.nextFrameNanos();
            assertTrue(next <= 1_000L * MS, "the busy indicator's step comes first: " + next);
        }
    }
}
