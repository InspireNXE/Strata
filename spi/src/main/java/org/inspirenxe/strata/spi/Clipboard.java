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

import java.util.Optional;

/**
 * The engine's text clipboard.
 *
 * <h2>Requirements for implementors</h2>
 * <ul>
 *     <li>Never throw: a clipboard that is busy, owned by another program or unavailable reads as empty and ignores
 *     writes.</li>
 *     <li>Text written with {@link #set} reads back unchanged from {@link #get} until something else changes the
 *     clipboard.</li>
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
     * @param text the text; never null
     */
    void set(String text);
}
