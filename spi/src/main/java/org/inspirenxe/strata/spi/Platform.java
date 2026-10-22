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
package org.inspirenxe.strata.spi;

import org.inspirenxe.strata.api.event.cause.CauseStack;
import org.inspirenxe.strata.api.event.cause.Origin;
import org.inspirenxe.strata.api.text.TextMetrics;

import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;

/**
 * Everything a host needs from the engine besides drawing. An adapter passes one to
 * {@code StrataCore.createScreenHost} or {@code StrataCore.createScreenStack}. Hosts may share a platform: a screen
 * stack gives its own to the host of every screen in it.
 *
 * <h2>Requirements for implementors</h2>
 * <ul>
 *     <li>Return the same objects, or objects that behave the same, every time: hosts may keep what they get.</li>
 *     <li>{@link #capabilities()} must not change during the platform's life.</li>
 *     <li>Calls come from the UI thread only, except {@link #uiExecutor()} and {@link #uiThreadCheck()}, which core
 *     reads once, on whichever thread first makes a host for the platform.</li>
 * </ul>
 * <p>Each method's return type lists its own requirements.</p>
 */
public interface Platform {

    /**
     * Returns the engine's text measurement, used by layout and by text editing.
     *
     * @return the text metrics; must match what {@link Canvas#text} draws
     */
    TextMetrics textMetrics();

    /**
     * Returns the clipboard text boxes copy to and paste from.
     *
     * @return the clipboard, or {@link Clipboard#NONE} if the engine has none
     */
    Clipboard clipboard();

    /**
     * Returns the clock used for click counting, caret blinking and synthesized key repeats.
     *
     * @return a monotonic clock, usually {@link Clock#SYSTEM}
     */
    Clock clock();

    /**
     * Returns where hosts announce that a text field gained or lost focus.
     *
     * @return the text input sink, or {@link TextInputSink#NONE} if the engine needs no signal
     */
    TextInputSink textInput();

    /**
     * Returns what the engine does for itself, so the host can fill the gaps.
     *
     * @return the capabilities; constant for the platform's life
     */
    Capabilities capabilities();

    /**
     * Returns how the engine's own controls behave, so Strata's can match them.
     *
     * @return the conventions; constant for the platform's life. {@link Conventions#DEFAULT} unless overridden
     */
    default Conventions conventions() {
        return Conventions.DEFAULT;
    }

    /**
     * Returns where hosts look up themes named by id.
     *
     * @return the theme source, or {@link ThemeSource#NONE} if the engine has no themes or theme files of its own
     */
    ThemeSource themes();

    /**
     * Returns the engine's own cause stack, when it has one Strata's causes should follow. Hosts then push their
     * input frames on it and hand it to developers through {@code ComponentFactory.causes()}.
     *
     * <p>A stack returned here must keep frames for the UI thread, close them in the order {@code CauseFrame}
     * states, and answer {@code current()} and {@code context} from the frames open at the time of the call. Strata
     * reads it only on the UI thread.</p>
     *
     * @return the engine's stack, or empty for Strata's own, one per thread; the default
     */
    default Optional<CauseStack> causes() {
        return Optional.empty();
    }

    /**
     * Returns the engine's object for the person at the screen, which hosts put in every {@link Origin.User} origin
     * so handlers can tell who acted, such as the signed-in user's account. Strata never reads it.
     *
     * @return the identity, or empty if the engine does not name its user; the default
     */
    default Optional<Object> user() {
        return Optional.empty();
    }

    /**
     * Returns the engine's executor for its UI thread, the thread that dispatches input to the platform's hosts and
     * renders them, such as Swing's {@code SwingUtilities::invokeLater} or an engine's main thread executor. Core
     * hands every task given to {@code UiThread.execute} and {@code submit} to it, wrapped so that a task that throws
     * is logged.
     *
     * <p>The executor must accept tasks from any thread, run them on the UI thread in the order given, and run a task
     * given on the UI thread after the current call into Strata returns, never inside it. Core reads this once per
     * platform.</p>
     *
     * @return the executor, or empty to have core queue tasks and run them at the start of the next
     *     {@code flush}, {@code layout} or {@code render} of any host of the platform; the default. Every adapter that
     *     renders runs them that way, so an adapter needs one only to run tasks while nothing renders
     */
    default Optional<Executor> uiExecutor() {
        return Optional.empty();
    }

    /**
     * Returns a test that answers whether the calling thread is the engine's UI thread, such as Swing's
     * {@code SwingUtilities::isEventDispatchThread}. Core calls it from {@code UiThread.isCurrent} and
     * {@code checkCurrent}, which run on every change to an attached component, so it must be cheap, must answer
     * from any thread, and must not block.
     *
     * <p>Without one, core takes the UI thread to be the thread that first dispatches input to, flushes, lays out or
     * renders a host of the platform, and until then treats every thread as passing {@code checkCurrent} and none as
     * current. Core reads this once per platform.</p>
     *
     * @return the test, or empty to let core record the thread; the default
     */
    default Optional<BooleanSupplier> uiThreadCheck() {
        return Optional.empty();
    }

    /**
     * Returns the engine's file dialogs, for {@code ComponentFactory.fileDialogs()}.
     *
     * @return the dialogs, or empty for an engine without them, the default; core then reports file dialogs as not
     *     supported and completes every request empty at once
     */
    default Optional<FileDialogSource> fileDialogs() {
        return Optional.empty();
    }
}
