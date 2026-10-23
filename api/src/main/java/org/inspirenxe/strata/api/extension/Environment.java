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
package org.inspirenxe.strata.api.extension;

import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.transfer.Transfer;

import java.util.Optional;

/**
 * What a component may ask of the engine outside layout: text measurement, time, the clipboard, the held
 * modifiers and the display's density. Get it with {@link CustomComponent#environment()}.
 *
 * <p>It comes from the host, so tests can drive time and the clipboard, and a component written against it works
 * on any engine. Do not read the system clock or AWT clipboard directly in a component.</p>
 *
 * <p>Strata provides this object. Do not implement this interface.</p>
 */
public interface Environment {

    /**
     * Returns the engine's text measurement, the same one layout uses.
     *
     * @return the text metrics
     */
    TextMetrics text();

    /**
     * Returns the host's clock, for blinking carets and other timing. Only differences between two readings mean
     * anything; it is not the time of day.
     *
     * @return milliseconds on a clock that never goes backward
     */
    long millis();

    /**
     * Reads text from the clipboard: the {@link org.inspirenxe.strata.api.transfer.DataFormat#TEXT} of
     * {@link #clipboard()}.
     *
     * @return the clipboard text, or empty if it holds none or the engine has no clipboard
     */
    Optional<String> clipboardText();

    /**
     * Puts text on the clipboard, as {@link #setClipboard} does with {@link Transfer#text}. Engines without a
     * clipboard ignore it.
     *
     * @param text the text
     */
    void setClipboardText(String text);

    /**
     * Reads the clipboard in every format it holds. While the engine's clipboard still holds the text of the last
     * transfer this process {@linkplain #setClipboard set}, it is that whole transfer, local formats included;
     * otherwise it is what the engine reads, which is at least the text.
     *
     * @return the clipboard's content, or empty if it holds nothing or the engine has no clipboard
     */
    Optional<Transfer> clipboard();

    /**
     * Puts a transfer on the clipboard. The engine gets every format but the
     * {@linkplain org.inspirenxe.strata.api.transfer.DataFormat#isLocal() local} ones, in what it can hold, which
     * is at least the text; the whole transfer is kept for {@link #clipboard()} in this process. So rows copied as
     * text and as objects paste back as objects in the application and as text everywhere else.
     *
     * @param transfer what to put on the clipboard
     */
    void setClipboard(Transfer transfer);

    /**
     * Returns the modifier keys held, as of the latest input the host received.
     *
     * @return the held modifiers; {@link Modifiers#NONE} before any input
     */
    Modifiers modifiers();

    /**
     * Returns how many device pixels one UI unit covers on the host's display now, so a component can draw a line
     * one device pixel wide ({@code 1 / pixelsPerUnit()} units) or pick a more detailed image. It changes when the
     * window moves to another display or the user zooms, and the host then lays every component out again.
     *
     * @return device pixels per UI unit, greater than 0; 1 when the engine does not say
     */
    float pixelsPerUnit();
}
