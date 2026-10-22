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
import org.inspirenxe.strata.api.Strata;
import org.inspirenxe.strata.api.UiThread;
import org.inspirenxe.strata.api.control.button.CheckBox;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.event.cause.CauseFrame;
import org.inspirenxe.strata.api.event.cause.CauseKeys;
import org.inspirenxe.strata.api.event.cause.Origin;
import org.inspirenxe.strata.api.control.attribute.value.Toggle;
import org.inspirenxe.strata.api.event.input.InputEvents;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.inspirenxe.strata.api.screen.ScreenController;
import org.inspirenxe.strata.api.state.State;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.theme.Themes;
import org.inspirenxe.strata.core.frame.UiThreadImpl;
import org.inspirenxe.strata.core.host.PlatformScreens;
import org.inspirenxe.strata.spi.ScreenStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The UI thread (design 4.42): handing tasks over, which thread is current, and the checks on attached components.
 */
class UiThreadTest {

    /**
     * Runs an action on a new thread and returns what it threw, or null.
     */
    private static Throwable onOtherThread(final Runnable action) throws InterruptedException {
        final AtomicReference<Throwable> thrown = new AtomicReference<>();
        final Thread thread = new Thread(() -> {
            try {
                action.run();
            } catch (final Throwable t) {
                thrown.set(t);
            }
        }, "worker");
        thread.start();
        thread.join();
        return thrown.get();
    }

    @Nested
    class Tasks {
        @Test
        void hostsOfOnePlatformShareOneUiThread() {
            final TestPlatform platform = new TestPlatform();
            final ComponentFactory first = StrataCore.createScreenHost(platform, Themes.flat()).factory();
            final ComponentFactory second = StrataCore.createScreenHost(platform, Themes.flat()).factory();
            final ComponentFactory other = StrataCore.createScreenHost(new TestPlatform(), Themes.flat()).factory();
            assertSame(first.uiThread(), second.uiThread());
            assertNotSame(first.uiThread(), other.uiThread());
        }

        @Test
        void tasksWaitForTheNextFlushAndRunInOrder() throws InterruptedException {
            final TestHost host = new TestHost(200, 100);
            final List<String> ran = new ArrayList<>();
            final UiThread ui = host.factory().uiThread();
            assertEquals(null, onOtherThread(() -> {
                ui.execute(() -> ran.add("first"));
                ui.execute(() -> ran.add("second"));
            }));
            assertTrue(ran.isEmpty());
            host.host().flush();
            assertEquals(List.of("first", "second"), ran);
        }

        @Test
        void executeOnTheUiThreadQueuesToo() {
            final TestHost host = new TestHost(200, 100);
            host.host().flush();
            final List<String> ran = new ArrayList<>();
            host.factory().uiThread().execute(() -> ran.add("task"));
            ran.add("caller");
            host.host().flush();
            assertEquals(List.of("caller", "task"), ran);
        }

        @Test
        void aTaskQueuedByATaskWaitsForTheNextFlush() {
            final TestHost host = new TestHost(200, 100);
            final UiThread ui = host.factory().uiThread();
            final List<String> ran = new ArrayList<>();
            ui.execute(() -> {
                ran.add("outer");
                ui.execute(() -> ran.add("inner"));
            });
            host.host().flush();
            assertEquals(List.of("outer"), ran);
            host.host().flush();
            assertEquals(List.of("outer", "inner"), ran);
        }

        @Test
        void aTaskThatThrowsIsLoggedAndTheNextRuns() {
            final TestHost host = new TestHost(200, 100);
            final UiThread ui = host.factory().uiThread();
            final List<String> ran = new ArrayList<>();
            ui.execute(() -> {
                throw new IllegalStateException("boom");
            });
            ui.execute(() -> ran.add("next"));
            host.host().flush();
            assertEquals(List.of("next"), ran);
        }

        @Test
        void submitCompletesItsFuture() throws Exception {
            final TestHost host = new TestHost(200, 100);
            final UiThread ui = host.factory().uiThread();
            final CompletableFuture<String> done = ui.submit(() -> "value");
            final CompletableFuture<String> failed = ui.submit(() -> {
                throw new IllegalArgumentException("bad");
            });
            assertFalse(done.isDone());
            host.host().flush();
            assertEquals("value", done.get());
            final ExecutionException error = assertThrows(ExecutionException.class, failed::get);
            assertInstanceOf(IllegalArgumentException.class, error.getCause());
        }

        @Test
        void tasksRunWithNoFrameOpenUnlessTheyPushOne() {
            final TestHost host = new TestHost(200, 100);
            final ComponentFactory ui = host.factory();
            final CheckBox box = ui.checkBox(Text.of("Sold"));
            host.show(box);
            final List<Origin> origins = new ArrayList<>();
            box.onEvent(Toggle.TOGGLED, event -> origins.add(event.origin()));
            final Object seller = new Object();
            // Inside a user frame, as from a click handler; the task still runs with none
            try (CauseFrame _ = ui.causes().pushFrame().put(CauseKeys.ORIGIN, new Origin.User(null))) {
                ui.uiThread().execute(() -> box.setSelected(true));
                ui.uiThread().execute(() -> {
                    try (CauseFrame _ = ui.causes().pushFrame().put(CauseKeys.ORIGIN, new Origin.Remote(seller))) {
                        box.setSelected(false);
                    }
                });
            }
            host.host().flush();
            assertEquals(List.of(Origin.CODE, new Origin.Remote(seller)), origins);
        }

        @Test
        void aPlatformExecutorRunsTheTasks() {
            final TestPlatform platform = new TestPlatform();
            final List<Runnable> handedOver = new ArrayList<>();
            platform.setUiExecutor(handedOver::add);
            final TestHost host = new TestHost(platform, Themes.flat(), 200, 100);
            final List<String> ran = new ArrayList<>();
            host.factory().uiThread().execute(() -> ran.add("task"));
            host.factory().uiThread().execute(() -> {
                throw new IllegalStateException("logged, not thrown");
            });
            host.host().flush();
            assertTrue(ran.isEmpty(), "core does not run tasks the engine's executor took");
            assertEquals(2, handedOver.size());
            handedOver.forEach(Runnable::run);
            assertEquals(List.of("task"), ran);
        }
    }

    @Nested
    class Current {
        @Test
        void theFirstThreadToDriveAHostIsTheUiThread() throws InterruptedException {
            final TestHost host = new TestHost(200, 100);
            final UiThread ui = host.factory().uiThread();
            assertFalse(ui.isCurrent(), "unknown until a host is driven");
            ui.checkCurrent();
            host.host().flush();
            assertTrue(ui.isCurrent());
            final AtomicBoolean elsewhere = new AtomicBoolean(true);
            assertEquals(null, onOtherThread(() -> elsewhere.set(ui.isCurrent())));
            assertFalse(elsewhere.get());
            assertInstanceOf(IllegalStateException.class, onOtherThread(ui::checkCurrent));
        }

        @Test
        void inputRecordsTheThreadToo() {
            final TestHost host = new TestHost(200, 100);
            host.host().pointerLeft();
            assertTrue(host.factory().uiThread().isCurrent());
        }

        @Test
        void aPlatformCheckDecides() {
            final TestPlatform platform = new TestPlatform();
            final AtomicBoolean onUi = new AtomicBoolean(false);
            platform.setUiThreadCheck(onUi::get);
            final TestHost host = new TestHost(platform, Themes.flat(), 200, 100);
            final UiThread ui = host.factory().uiThread();
            host.host().flush();
            assertFalse(ui.isCurrent(), "the platform's check, not the thread that flushed");
            assertThrows(IllegalStateException.class, ui::checkCurrent);
            onUi.set(true);
            assertTrue(ui.isCurrent());
            ui.checkCurrent();
        }
    }

    @Nested
    class Checks {
        @Test
        void changesToAnAttachedComponentThrowOffTheUiThread() throws InterruptedException {
            final TestHost host = new TestHost(200, 100);
            final ComponentFactory ui = host.factory();
            final StackPanel panel = ui.stackPanel();
            final Label label = ui.label(Text.of("Price"));
            panel.add(label);
            host.show(panel);

            assertInstanceOf(IllegalStateException.class, onOtherThread(() -> label.setWidth(40f)));
            assertInstanceOf(IllegalStateException.class, onOtherThread(() -> label.setText(Text.of("Cost"))));
            assertInstanceOf(IllegalStateException.class,
                    onOtherThread(() -> label.onEvent(InputEvents.CLICK, event -> { })));
            assertInstanceOf(IllegalStateException.class,
                    onOtherThread(() -> label.subscribe(InputEvents.CLICK, 0, event -> { })));
            assertInstanceOf(IllegalStateException.class,
                    onOtherThread(() -> label.bind(Label.TEXT, State.of(Text.of("Bound")))));
            assertInstanceOf(IllegalStateException.class, onOtherThread(() -> panel.remove(label)));
            assertInstanceOf(IllegalStateException.class, onOtherThread(() -> panel.add(ui.label(Text.of("New")))));
            assertInstanceOf(IllegalStateException.class, onOtherThread(() -> ui.overlays().toast(Text.of("Hi"))));
            assertEquals(1, panel.children().size(), "nothing changed");
        }

        @Test
        void overlaysCloseAndTakeHandlersOnTheUiThreadOnly() throws InterruptedException {
            final TestHost host = new TestHost(200, 100);
            final ComponentFactory ui = host.factory();
            host.show(ui.label(Text.of("Base")));
            final var popup = ui.overlays().popup(ui.label(Text.of("Menu")), host.factory().label().bounds());
            assertInstanceOf(IllegalStateException.class, onOtherThread(popup::close));
            assertInstanceOf(IllegalStateException.class,
                    onOtherThread(() -> popup.onEvent(org.inspirenxe.strata.api.overlay.Overlay.CLOSED, event -> { })));
            assertTrue(popup.isOpen());
        }

        @Test
        void aTreeBuiltOnAnotherThreadIsAddedOnTheUiThread() throws InterruptedException {
            final TestHost host = new TestHost(200, 100);
            final ComponentFactory ui = host.factory();
            final StackPanel panel = ui.stackPanel();
            host.show(panel);
            final AtomicReference<StackPanel> built = new AtomicReference<>();
            assertEquals(null, onOtherThread(() -> {
                final StackPanel row = ui.stackPanel();
                row.add(ui.label(Text.of("Ana")));
                row.add(ui.label(Text.of("Ben")).setWidth(30f));
                row.onEvent(InputEvents.CLICK, event -> { });
                built.set(row);
            }));
            panel.add(built.get());
            host.host().layout();
            assertEquals(2, built.get().children().size());
        }

        @Test
        void aStateMayBeSetFromAnyThread() throws InterruptedException {
            final TestHost host = new TestHost(200, 100);
            final State<Boolean> sold = State.of(false);
            final CheckBox box = host.factory().checkBox(Text.of("Sold")).bind(Toggle.SELECTED, sold);
            host.show(box);
            assertEquals(null, onOtherThread(() -> sold.set(true)));
            host.host().flush();
            assertTrue(box.isSelected());
        }
    }

    @Nested
    class Process {
        @AfterEach
        void removeController() {
            StrataCore.setScreenController(null);
        }

        @Test
        void throwsUntilAPlatformIsKnown() {
            final IllegalStateException error = assertThrows(IllegalStateException.class, Strata::uiThread);
            assertEquals(PlatformScreens.NO_UI_THREAD, error.getMessage());
        }

        @Test
        void aStackHandedOverNamesItsPlatform() {
            final TestPlatform platform = new TestPlatform();
            final ScreenStack stack = StrataCore.createScreenStack(platform, Themes.flat());
            StrataCore.setScreenController(stack);
            assertSame(UiThreadImpl.of(platform), Strata.uiThread());
        }

        @Test
        void anAdapterControllerNamesItsPlatform() {
            final TestPlatform platform = new TestPlatform();
            final ScreenController controller = (screen, theme) -> {
                throw new UnsupportedOperationException();
            };
            StrataCore.setScreenController(controller);
            assertThrows(IllegalStateException.class, Strata::uiThread);
            StrataCore.setScreenController(controller, platform);
            assertSame(UiThreadImpl.of(platform), Strata.uiThread());
            assertSame(controller, Strata.screens());
            StrataCore.clearScreenController();
            assertThrows(IllegalStateException.class, Strata::uiThread);
            assertThrows(IllegalStateException.class, Strata::screens);
        }
    }
}
