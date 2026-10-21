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

import org.inspirenxe.strata.api.text.TextMetrics;

/**
 * Everything a host needs from the engine besides drawing. An adapter passes one to
 * {@code StrataCore.createScreenHost} or {@code StrataCore.createScreenStack}. Hosts may share a platform: a screen
 * stack gives its own to the host of every screen in it.
 *
 * <h2>Requirements for implementors</h2>
 * <ul>
 *     <li>Return the same objects, or objects that behave the same, every time: hosts may keep what they get.</li>
 *     <li>{@link #capabilities()} must not change during the platform's life.</li>
 *     <li>Calls come from the UI thread only.</li>
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
     * Returns where hosts look up themes named by id.
     *
     * @return the theme source, or {@link ThemeSource#NONE} if the engine has no themes or theme files of its own
     */
    ThemeSource themes();
}
