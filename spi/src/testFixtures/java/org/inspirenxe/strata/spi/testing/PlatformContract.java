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
package org.inspirenxe.strata.spi.testing;

import org.inspirenxe.strata.api.event.cause.CauseFrame;
import org.inspirenxe.strata.api.event.cause.CauseKey;
import org.inspirenxe.strata.api.event.cause.CauseStack;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.input.TextInputKind;
import org.inspirenxe.strata.api.event.Subscription;
import org.inspirenxe.strata.api.text.FontId;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.ThemeId;
import org.inspirenxe.strata.api.theme.Themes;
import org.inspirenxe.strata.api.transfer.DataFormat;
import org.inspirenxe.strata.api.transfer.Transfer;
import org.inspirenxe.strata.spi.Capabilities;
import org.inspirenxe.strata.spi.Clipboard;
import org.inspirenxe.strata.spi.Clock;
import org.inspirenxe.strata.spi.Conventions;
import org.inspirenxe.strata.spi.Platform;
import org.inspirenxe.strata.spi.Scheduler;
import org.inspirenxe.strata.spi.TextInputSink;
import org.inspirenxe.strata.spi.ThemeSource;
import org.inspirenxe.strata.spi.Translator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Checks a {@link Platform} against the requirements in its documentation. Extend it in an adapter's tests and
 * return the adapter's platform from {@link #platform()}. Override {@link #hasClipboard()} and
 * {@link #clockRunsOnItsOwn()} when the platform under test has no working clipboard (headless) or a clock the test
 * drives by hand, and {@link #translator()} to check the adapter's {@link Translator} too.
 *
 * <p>Besides the services every platform has, the checks cover the conventions and the theme source, which must
 * answer an id they do not know with nothing and keep their version while nothing changes; text sizes and fonts the
 * engine lacks; the scheduler, whose tasks must repeat until closed and never run after; and, when the platform
 * returns one, the engine's cause stack, whose frames must hide and restore values in order. Override
 * {@link #passTime} for a scheduler that runs only when the application drives it.</p>
 */
public abstract class PlatformContract {

    /**
     * Returns the platform under test. Called once per check.
     *
     * @return the adapter's platform
     */
    protected abstract Platform platform();

    /**
     * Returns whether the clipboard should keep what it is given. The default is true; return false for a platform
     * running headless or with {@link Clipboard#NONE}.
     *
     * @return true if text written to the clipboard must read back
     */
    protected boolean hasClipboard() {
        return true;
    }

    /**
     * Returns whether the clock advances in real time. The default is true; return false for a test clock.
     *
     * @return true if the clock should advance while the test sleeps
     */
    protected boolean clockRunsOnItsOwn() {
        return true;
    }

    /**
     * Lets time pass so the platform's scheduler can run what is due. The default sleeps, which suits a scheduler
     * with a timer or a thread of its own. Override it for a scheduler that runs only when the application drives
     * it, to move a test clock or run the loop that runs the tasks.
     *
     * @param millis about how long, in milliseconds
     * @throws InterruptedException if the test thread is interrupted while it sleeps
     */
    protected void passTime(final long millis) throws InterruptedException {
        Thread.sleep(millis);
    }

    /**
     * Returns the translator the adapter gives core with {@code StrataCore.setTranslator}, for the translator checks.
     * The default returns {@code null}, which skips them.
     *
     * @return the adapter's translator, or {@code null}
     */
    protected Translator translator() {
        return null;
    }

    @Test
    void providesEveryService() {
        final Platform platform = this.platform();
        assertNotNull(platform.textMetrics(), "textMetrics");
        assertNotNull(platform.clipboard(), "clipboard");
        assertNotNull(platform.clock(), "clock");
        assertNotNull(platform.textInput(), "textInput");
        assertNotNull(platform.capabilities(), "capabilities");
        assertNotNull(platform.scheduler(), "scheduler");
        assertNotNull(platform.conventions(), "conventions");
        assertNotNull(platform.themes(), "themes");
        assertNotNull(platform.causes(), "causes");
        assertNotNull(platform.user(), "user");
    }

    @Test
    void capabilitiesDoNotChange() {
        final Platform platform = this.platform();
        final Capabilities first = platform.capabilities();
        for (int i = 0; i < 10; i++) {
            assertEquals(first, platform.capabilities());
        }
    }

    @Test
    void theShorterCapabilitiesConstructorsDrawNoShapes() {
        final Capabilities two = new Capabilities(true, true);
        final Capabilities three = new Capabilities(true, true, true);
        assertFalse(two.slicesSprites(), "two arguments slice no sprites");
        assertFalse(two.drawsShapes(), "two arguments draw no shapes");
        assertFalse(three.drawsShapes(), "three arguments draw no shapes");
        assertEquals(new Capabilities(true, true, true, false), three);
    }

    @Test
    void aLargerSizeMeasuresLarger() {
        final TextMetrics metrics = this.platform().textMetrics();
        final TextStyle small = TextStyle.DEFAULT.withSize(10f);
        final TextStyle large = TextStyle.DEFAULT.withSize(20f);
        assertTrue(metrics.width(new TextSpan("Strata", large)) > metrics.width(new TextSpan("Strata", small)),
                "twice the size measures wider");
        assertTrue(metrics.lineHeight(large) > metrics.lineHeight(small), "twice the size has taller lines");
    }

    @Test
    void aFontTheEngineLacksMeasuresAsTheDefault() {
        final TextMetrics metrics = this.platform().textMetrics();
        final TextStyle missing = TextStyle.DEFAULT.withFont(FontId.of("strata_contract", "no_such_font"));
        assertEquals(metrics.width(new TextSpan("Strata", TextStyle.DEFAULT)),
                metrics.width(new TextSpan("Strata", missing)), 0.5f);
        assertEquals(metrics.lineHeight(TextStyle.DEFAULT), metrics.lineHeight(missing), 0.5f);
    }

    @Test
    void theMetricsVersionHoldsWhileNothingChanges() {
        final TextMetrics metrics = this.platform().textMetrics();
        final long first = metrics.version();
        for (int i = 0; i < 10; i++) {
            assertEquals(first, metrics.version());
        }
    }

    @Test
    void theSchedulerRefusesAnIntervalOfZero() {
        final Scheduler scheduler = this.platform().scheduler();
        assertThrows(IllegalArgumentException.class, () -> scheduler.every(0L, () -> { }));
    }

    @Test
    void aScheduledTaskRepeatsUntilClosed() throws InterruptedException {
        final AtomicInteger runs = new AtomicInteger();
        final Subscription subscription = this.platform().scheduler().every(10L, runs::incrementAndGet);
        for (int waited = 0; runs.get() < 2 && waited < 5_000; waited += 20) {
            this.passTime(20L);
        }
        assertTrue(runs.get() >= 2, "the task ran " + runs.get() + " times in five seconds");
        subscription.close();
        subscription.close();
        // A run already handed to the engine's queue may still land; none may come after it
        this.passTime(50L);
        final int stopped = runs.get();
        this.passTime(100L);
        assertEquals(stopped, runs.get(), "the task ran after its subscription closed");
    }

    @Test
    void aTaskClosedAtOnceNeverRuns() throws InterruptedException {
        final AtomicInteger runs = new AtomicInteger();
        this.platform().scheduler().every(10L, runs::incrementAndGet).close();
        this.passTime(100L);
        assertEquals(0, runs.get());
    }

    @Test
    void conventionsDoNotChange() {
        final Platform platform = this.platform();
        final Conventions first = platform.conventions();
        for (int i = 0; i < 10; i++) {
            assertEquals(first, platform.conventions());
        }
    }

    @Test
    void theThemeSourceAnswersAnUnknownIdWithNothing() {
        final ThemeSource themes = this.platform().themes();
        final ThemeId unknown = ThemeId.of("strata_contract", "no_such_theme");
        assertEquals(Optional.empty(), themes.base(unknown), "base");
        assertEquals(List.of(), themes.bases(unknown), "bases");
        assertEquals(List.of(), themes.documents(unknown), "documents");
    }

    @Test
    void theThemeSourceNamesARealDefaultTheme() {
        final ThemeSource themes = this.platform().themes();
        final ThemeId theme = themes.defaultTheme();
        assertNotNull(theme, "defaultTheme");
        assertNotEquals(Themes.PLATFORM, theme, "the default theme must not be the platform's placeholder id");
    }

    @Test
    void theThemeSourceVersionHoldsWhileNothingChanges() {
        final ThemeSource themes = this.platform().themes();
        final long first = themes.version();
        for (int i = 0; i < 10; i++) {
            assertEquals(first, themes.version());
        }
    }

    @Test
    void theEnginesCauseStackKeepsFramesInOrder() {
        final Optional<CauseStack> engine = this.platform().causes();
        assumeTrue(engine.isPresent(), "the platform has no cause stack of its own");
        final CauseStack stack = engine.get();
        final CauseKey<String> key = CauseKey.of("strata_contract:value", String.class);
        final Object marker = new Object();
        final Optional<String> before = stack.context(key);

        try (CauseFrame ignored = stack.pushFrame().put(key, "outer")) {
            assertEquals(Optional.of("outer"), stack.context(key));
            try (CauseFrame ignoredInner = stack.pushFrame().put(key, "inner").push(marker)) {
                assertEquals(Optional.of("inner"), stack.context(key), "an inner frame hides an outer one");
                assertEquals(Optional.of("inner"), stack.current().context(key), "the current cause agrees");
                assertEquals(Optional.of(marker), stack.current().root(), "the newest object is the direct cause");
            }
            assertEquals(Optional.of("outer"), stack.context(key), "closing the inner frame shows the outer again");
        }
        assertEquals(before, stack.context(key), "closing every frame removes their values");
    }

    @Test
    void theTranslatorAnswersAnUnknownKeyWithNothing() {
        final Translator translator = this.translator();
        assumeTrue(translator != null, "no translator given for the translator checks");
        assertEquals(Optional.empty(), translator.translate("strata_contract.no.such.key"));
        assertDoesNotThrow(() -> translator.translate("strata.date.month.1"), "Strata's own keys can be asked for");
        assertNotNull(translator.locale(), "locale");
    }

    @Test
    void theTranslatorVersionHoldsWhileNothingChanges() {
        final Translator translator = this.translator();
        assumeTrue(translator != null, "no translator given for the translator checks");
        final long first = translator.version();
        for (int i = 0; i < 10; i++) {
            assertEquals(first, translator.version());
        }
    }

    @Test
    void monospacedTextHasEqualWidthsWhenTheFontExists() {
        final Platform platform = this.platform();
        final TextStyle monospace = TextStyle.DEFAULT.withMonospace(true);
        final float narrow = platform.textMetrics().width(new TextSpan("iiii", monospace));
        final float wide = platform.textMetrics().width(new TextSpan("MMMM", monospace));
        assertTrue(narrow > 0f && wide > 0f, "monospaced text has a width");
        assumeTrue(platform.capabilities().monospaceFont(), "the engine has no monospaced font");
        assertEquals(narrow, wide, 0.01f, "every character of a monospaced font has the same width");
    }

    @Test
    void textMetricsAgreeBetweenCalls() {
        final Platform platform = this.platform();
        final TextSpan span = TextSpan.of("Strata");
        assertEquals(platform.textMetrics().width(span), platform.textMetrics().width(span), 0f);
    }

    @Test
    void clockNeverGoesBackward() {
        final Clock clock = this.platform().clock();
        long previous = clock.nanoTime();
        for (int i = 0; i < 10_000; i++) {
            final long now = clock.nanoTime();
            assertTrue(now >= previous, "clock went backward");
            previous = now;
        }
        // Compared as longs: assertEquals with a delta has no long form and would round both through float
        final long expected = clock.nanoTime() / 1_000_000L;
        final long millis = clock.millis();
        assertTrue(Math.abs(millis - expected) <= 1L,
                () -> "millis is nanoTime in milliseconds: expected " + expected + " but was " + millis);
    }

    @Test
    void wallClockGivesADateAndAZone() {
        final java.time.Clock wall = this.platform().clock().wall();
        assertNotNull(wall, "wall");
        assertNotNull(wall.getZone(), "the wall clock's zone");
        assertNotNull(wall.instant(), "the wall clock's instant");
        assertDoesNotThrow(() -> LocalDate.now(wall), "today can be read from the wall clock");
    }

    @Test
    void theDefaultWallClockIsTheSystemClockInTheDefaultZone() {
        final Clock clock = () -> 0L;
        final java.time.Clock wall = clock.wall();
        assertEquals(ZoneId.systemDefault(), wall.getZone(), "the default wall clock's zone");
        final long before = System.currentTimeMillis();
        final long read = wall.millis();
        final long after = System.currentTimeMillis();
        assertTrue(read >= before - 1000L && read <= after + 1000L, "the default wall clock reads the system time");
    }

    @Test
    void clockAdvancesInRealTime() throws InterruptedException {
        assumeTrue(this.clockRunsOnItsOwn(), "the platform's clock is driven by the test");
        final Clock clock = this.platform().clock();
        final long start = clock.millis();
        Thread.sleep(20L);
        assertTrue(clock.millis() - start >= 10L, "clock did not advance while sleeping");
    }

    @Test
    void clipboardNeverThrows() {
        final Clipboard clipboard = this.platform().clipboard();
        assertDoesNotThrow(() -> {
            clipboard.set("");
            clipboard.set("line one\nline two");
            clipboard.set("😀");
            clipboard.get();
        });
    }

    @Test
    void clipboardKeepsWhatItIsGiven() {
        assumeTrue(this.hasClipboard(), "the platform has no working clipboard here");
        final Clipboard clipboard = this.platform().clipboard();
        clipboard.set("strata clipboard check 😀");
        assertEquals(Optional.of("strata clipboard check 😀"), clipboard.get());
    }

    @Test
    void fileDialogsAreOptional() {
        assertNotNull(assertDoesNotThrow(() -> this.platform().fileDialogs()), "fileDialogs");
    }

    @Test
    void clipboardTransfersNeverThrow() {
        final Clipboard clipboard = this.platform().clipboard();
        assertDoesNotThrow(() -> {
            clipboard.setTransfer(Transfer.text("strata"));
            clipboard.setTransfer(Transfer.builder().add(DataFormat.TEXT, "a,b").add(DataFormat.mime("text/csv"), "a,b")
                    .build());
            clipboard.setTransfer(Transfer.empty());
            clipboard.getTransfer();
        });
    }

    @Test
    void clipboardTransfersKeepTheirText() {
        assumeTrue(this.hasClipboard(), "the platform has no working clipboard here");
        final Clipboard clipboard = this.platform().clipboard();
        clipboard.setTransfer(Transfer.builder().add(DataFormat.TEXT, "Ana	12").add(DataFormat.mime("text/csv"), "Ana,12")
                .build());
        assertEquals(Optional.of("Ana	12"), clipboard.get(), "get() reads the text setTransfer wrote");
        assertEquals(Optional.of("Ana	12"), clipboard.getTransfer().flatMap(transfer -> transfer.get(DataFormat.TEXT)),
                "getTransfer() holds the text");
    }

    @Test
    void textInputAcceptsRepeatedCalls() {
        final Platform platform = this.platform();
        assertDoesNotThrow(() -> {
            platform.textInput().start(new Rect(0, 0, 100, 20));
            platform.textInput().start(new Rect(10, 10, 100, 20));
            platform.textInput().stop();
            platform.textInput().stop();
        });
    }

    @Test
    void textInputAcceptsEveryKindInAnyOrder() {
        final Platform platform = this.platform();
        assertDoesNotThrow(() -> {
            platform.textInput().start(new Rect(0, 0, 100, 20), TextInputKind.SECRET);
            platform.textInput().start(new Rect(0, 30, 100, 20), TextInputKind.TEXT);
            platform.textInput().start(new Rect(0, 60, 100, 20), TextInputKind.SECRET);
            platform.textInput().stop();
            platform.textInput().start(new Rect(0, 0, 100, 20), TextInputKind.TEXT);
            platform.textInput().stop();
        });
    }

    @Test
    void theDefaultStartWithAKindForwardsToStart() {
        final List<Rect> started = new ArrayList<>();
        final TextInputSink sink = new TextInputSink() {
            @Override
            public void start(final Rect fieldArea) {
                started.add(fieldArea);
            }

            @Override
            public void stop() {
            }
        };
        sink.start(new Rect(1, 2, 3, 4), TextInputKind.SECRET);
        sink.start(new Rect(5, 6, 7, 8), TextInputKind.TEXT);
        assertEquals(List.of(new Rect(1, 2, 3, 4), new Rect(5, 6, 7, 8)), started);
    }
}
