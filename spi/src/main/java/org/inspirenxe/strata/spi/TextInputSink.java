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

import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.input.TextInputKind;

/**
 * Where a host tells the engine that a text field gained or lost focus. Some engines deliver no typed characters
 * until told (SDL-based engines among them), and some show an on-screen keyboard or input method
 * window at the given area.
 *
 * <p>Core calls {@link #start(Rect, TextInputKind)}, which says what kind of field took focus. Its default forwards to
 * {@link #start(Rect)}, so a sink that ignores the kind implements only that.</p>
 *
 * <h2>Requirements for implementors</h2>
 * <ul>
 *     <li>{@link #start} and {@link #stop} may arrive repeatedly; treat a second start as moving the input
 *     area, and as changing the kind when focus moves from one field to another.</li>
 *     <li>After {@link #start}, the adapter must forward typed characters to {@link InputSink#charTyped}.</li>
 *     <li>For {@link TextInputKind#SECRET}, turn off input method composition, prediction and on-screen keyboard
 *     suggestions where the engine allows, and turn them back on for the next {@link TextInputKind#TEXT} start or
 *     {@link #stop}.</li>
 *     <li>Never log typed characters or drawn text.</li>
 * </ul>
 */
public interface TextInputSink {
    /**
     * A sink that ignores both calls, for engines that always deliver characters.
     */
    TextInputSink NONE = new TextInputSink() {
        @Override
        public void start(final Rect fieldArea) {
        }

        @Override
        public void stop() {
        }
    };

    /**
     * Called when a component that accepts text input gains focus.
     *
     * @param fieldArea the bounds of the whole field, in the host's UI units, for placing input method windows. Core
     *                  reads them as the field gains focus, and again when its screen comes back to the top of a
     *                  stack, but does not call again as the caret moves or the field is laid out anew.
     */
    void start(Rect fieldArea);

    /**
     * Called when a component that accepts text input gains focus, with the kind of text it takes. The default
     * forwards to {@link #start(Rect)}; override it to treat secrets differently.
     *
     * @param fieldArea the bounds of the whole field, in the host's UI units, as {@link #start(Rect)} says
     * @param kind the kind of text the field takes
     */
    default void start(final Rect fieldArea, final TextInputKind kind) {
        this.start(fieldArea);
    }

    /**
     * Called when focus leaves the last component that accepts text input.
     */
    void stop();
}
