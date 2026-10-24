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
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.event.Subscription;
import org.inspirenxe.strata.api.screen.Screen;
import org.inspirenxe.strata.api.screen.ScreenHandle;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.theme.ThemeRef;
import org.inspirenxe.strata.api.theme.Themes;
import org.inspirenxe.strata.spi.ScreenHost;
import org.inspirenxe.strata.spi.ScreenStack;
import org.inspirenxe.strata.testing.ScreenTester;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Screens ask for repeating tasks through their handle; the platform's scheduler runs them, and core stops them when
 * the screen closes.
 */
class ScreenTaskTest {
    private final TestPlatform platform = new TestPlatform();

    /**
     * A screen that shows a count it refreshes every 100 ms, as a developer writes a polling screen.
     */
    static final class Ticker implements Screen {
        int runs;
        int runsWhenClosed = -1;
        int scheduledWhenClosed = -1;
        ScreenHandle handle;
        Subscription task;
        Label count;
        TestPlatform platform;

        @Override
        public Component<?> build(final ComponentFactory factory) {
            this.count = factory.label("0");
            return this.count;
        }

        @Override
        public void opened(final ScreenHandle opened) {
            this.handle = opened;
            this.task = opened.every(100L, () -> {
                this.runs++;
                this.count.setText(Text.of(Integer.toString(this.runs)));
            });
        }

        @Override
        public void closed() {
            this.runsWhenClosed = this.runs;
            if (this.platform != null) {
                this.scheduledWhenClosed = this.platform.scheduler().scheduled();
            }
        }
    }

    private ScreenHost host() {
        return StrataCore.createScreenHost(this.platform, Themes.flat());
    }

    @Test
    void aTaskRunsOnceForEachInterval() {
        final Ticker ticker = new Ticker();
        this.host().open(ticker);
        this.platform.advanceMillis(99L);
        assertEquals(0, ticker.runs, "the first run comes one interval after asking");
        this.platform.advanceMillis(1L);
        assertEquals(1, ticker.runs);
        this.platform.advanceMillis(250L);
        assertEquals(3, ticker.runs);
        assertEquals("3", ticker.count.text().plain(), "the task may change components");
    }

    @Test
    void closingTheSubscriptionStopsTheTask() {
        final Ticker ticker = new Ticker();
        this.host().open(ticker);
        this.platform.advanceMillis(100L);
        ticker.task.close();
        ticker.task.close();
        this.platform.advanceMillis(500L);
        assertEquals(1, ticker.runs);
        assertEquals(0, this.platform.scheduler().scheduled(), "the platform's subscription is closed too");
    }

    @Test
    void closingTheScreenStopsItsTasksBeforeItHearsClosed() {
        final Ticker ticker = new Ticker();
        ticker.platform = this.platform;
        final ScreenHost host = this.host();
        host.open(ticker);
        this.platform.advanceMillis(100L);
        host.close();
        assertEquals(0, ticker.scheduledWhenClosed, "closed() runs once no task is left");
        this.platform.advanceMillis(500L);
        assertEquals(1, ticker.runs);
        ticker.task.close();
    }

    @Test
    void showingAnotherScreenStopsTheFirstOnesTasks() {
        final Ticker first = new Ticker();
        final Ticker second = new Ticker();
        final ScreenHost host = this.host();
        host.open(first);
        host.open(second);
        this.platform.advanceMillis(200L);
        assertEquals(0, first.runs);
        assertEquals(2, second.runs);
    }

    @Test
    void aHandleWhoseOpeningEndedStartsNothing() {
        final Ticker ticker = new Ticker();
        final ScreenHost host = this.host();
        host.open(ticker);
        host.close();
        final int[] runs = new int[1];
        final Subscription late = ticker.handle.every(10L, () -> runs[0]++);
        this.platform.advanceMillis(100L);
        assertEquals(0, runs[0]);
        assertEquals(0, this.platform.scheduler().scheduled());
        late.close();
    }

    @Test
    void theTestKitRunsTasksAsItsClockAdvances() {
        final Ticker ticker = new Ticker();
        try (ScreenTester tester = ScreenTester.open(ticker)) {
            tester.advance(250L);
            assertEquals(2, ticker.runs);
            assertEquals("2", ticker.count.text().plain());
        }
        assertEquals(2, ticker.runsWhenClosed);
    }

    @Test
    void anIntervalOfZeroIsRefused() {
        final Ticker ticker = new Ticker();
        this.host().open(ticker);
        assertThrows(IllegalArgumentException.class, () -> ticker.handle.every(0L, () -> { }));
        assertThrows(IllegalArgumentException.class, () -> ticker.handle.every(-5L, () -> { }));
    }

    @Test
    void aCoveredScreenInAStackKeepsItsTasksUntilItCloses() {
        final ScreenStack stack = StrataCore.createScreenStack(this.platform, Themes.flat());
        stack.resize(200, 100);
        final Ticker below = new Ticker();
        final Ticker above = new Ticker();
        stack.push(below);
        below.handle.open(above);
        this.platform.advanceMillis(100L);
        assertEquals(1, below.runs, "a covered screen is still open");
        assertEquals(1, above.runs);
        stack.pop();
        stack.pop();
        this.platform.advanceMillis(500L);
        assertEquals(1, below.runs);
        assertEquals(1, above.runs);
    }

    @Test
    void aTaskAskedForBeforeAHandedOverOpeningStartsWhenItOpens() throws InterruptedException {
        final ScreenStack stack = StrataCore.createScreenStack(this.platform, Themes.flat());
        stack.resize(200, 100);
        // The test thread becomes the UI thread
        stack.push(ui -> ui.label(Text.of("Base")));
        stack.flush();
        final int[] runs = new int[2];
        final AtomicReference<Subscription> kept = new AtomicReference<>();
        final AtomicReference<Throwable> thrown = new AtomicReference<>();
        final Thread worker = new Thread(() -> {
            try {
                final ScreenHandle prices = stack.open(ui -> ui.label(Text.of("Prices")), ThemeRef.platform());
                kept.set(prices.every(100L, () -> runs[0]++));
                prices.every(100L, () -> runs[1]++).close();
            } catch (final Throwable t) {
                thrown.set(t);
            }
        }, "worker");
        worker.start();
        worker.join();
        assertNull(thrown.get());
        stack.flush();
        this.platform.advanceMillis(200L);
        assertEquals(2, runs[0], "the task started once the screen opened");
        assertEquals(0, runs[1], "a task closed before the screen opened never starts");
        kept.get().close();
        this.platform.advanceMillis(200L);
        assertEquals(2, runs[0]);
    }
}
