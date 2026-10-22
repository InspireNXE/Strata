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

import org.inspirenxe.strata.api.transfer.DataFormat;
import org.inspirenxe.strata.api.transfer.Transfer;

import java.util.Optional;

/**
 * The engine's clipboard. An engine that holds only text implements {@link #get} and {@link #set}; one that holds
 * more formats, such as images, files or text in other MIME types, also overrides {@link #getTransfer} and
 * {@link #setTransfer}.
 *
 * <h2>Requirements for implementors</h2>
 * <ul>
 *     <li>Never throw: a clipboard that is busy, owned by another program or unavailable reads as empty and ignores
 *     writes.</li>
 *     <li>Text written with {@link #set} reads back unchanged from {@link #get} until something else changes the
 *     clipboard.</li>
 *     <li>{@link #get} answers with the {@link DataFormat#TEXT} of what {@link #setTransfer} wrote, so core can tell
 *     whether the clipboard still holds what it set: core keeps the whole transfer it set, local formats included,
 *     and hands it back while {@code get} still returns the text it returned right after the write.</li>
 * </ul>
 */
public interface Clipboard {
    /**
     * A clipboard that is always empty and ignores writes, for engines without one (or headless use).
     */
    Clipboard NONE = new Clipboard() {
        @Override
        public Optional<String> get() {
            return Optional.empty();
        }

        @Override
        public void set(final String text) {
        }
    };

    /**
     * Reads the clipboard's text.
     *
     * @return the text, or empty if the clipboard holds no text or cannot be read
     */
    Optional<String> get();

    /**
     * Puts text on the clipboard. Failures are ignored.
     *
     * @param text the text
     */
    void set(String text);

    /**
     * Reads every format the clipboard holds that the engine can read. Values may be
     * {@linkplain Transfer.Builder#supply supplied} when first read.
     *
     * <p>The default reads {@link #get} and returns its text as {@link DataFormat#TEXT}.</p>
     *
     * @return the clipboard's content, or empty if it holds nothing the engine can read
     */
    default Optional<Transfer> getTransfer() {
        return this.get().map(Transfer::text);
    }

    /**
     * Puts a transfer on the clipboard, replacing what it held, in every format the engine can hold. Core never
     * passes a {@linkplain DataFormat#isLocal() local} format. Failures are ignored.
     *
     * <p>The default puts the transfer's {@link DataFormat#TEXT} on the clipboard with {@link #set}, and leaves the
     * clipboard as it was for a transfer without text.</p>
     *
     * @param transfer what to put on the clipboard
     */
    default void setTransfer(final Transfer transfer) {
        transfer.get(DataFormat.TEXT).ifPresent(this::set);
    }
}
