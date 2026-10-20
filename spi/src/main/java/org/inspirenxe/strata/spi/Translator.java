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
 * Looks up translations for {@link org.inspirenxe.strata.api.text.Text#translatable translated text}. An engine
 * adapter gives core its translator with {@code StrataCore.setTranslator}; there is one for the whole process.
 *
 * <h2>Requirements for implementors</h2>
 * <ul>
 *     <li>{@link #translate} returns the pattern for a key in the current language, or empty when there is none.
 *     Core does the argument formatting, so return the pattern as written, with its {@code %s}, {@code %1$s} and
 *     {@code %%} untouched. Core falls back to its own English for Strata's keys and to the key itself for the
 *     rest.</li>
 *     <li>{@link #version()} changes whenever {@link #translate} may return something else, such as after the
 *     language changes or translations reload. Hosts compare it on every layout and render, and lay out every
 *     control again when it changes, so it must be cheap.</li>
 *     <li>Calls may come from any thread, since code can read text outside the UI thread. Store the version in a
 *     volatile field or an atomic.</li>
 * </ul>
 */
public interface Translator {
    /**
     * Translates nothing, so Strata's keys read in English and other keys read as themselves.
     */
    Translator NONE = new Translator() {
        @Override
        public Optional<String> translate(final String key) {
            return Optional.empty();
        }

        @Override
        public long version() {
            return 0L;
        }
    };

    /**
     * Returns the pattern for a key in the current language.
     *
     * @param key the key
     * @return the pattern, or empty if the key has no translation
     */
    Optional<String> translate(String key);

    /**
     * Returns a number that changes whenever translations may have changed.
     *
     * @return the version
     */
    long version();
}
