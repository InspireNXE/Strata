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
package org.inspirenxe.strata.core.text;

import org.inspirenxe.strata.spi.Translator;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The process-wide translator that translated text reads through, with English for Strata's own keys.
 *
 * <p>A key is looked up in the {@linkplain #setTranslator translator} first, then in {@link #DEFAULTS}, and is shown
 * as itself if neither has it, with one warning per key. {@link #version()} changes when the translator is replaced
 * and whenever the translator's own version changes, and hosts lay out again when it does.</p>
 */
public final class Translations {
    /**
     * English for the keys Strata's controls use, so they read correctly where the engine has no translation.
     */
    public static final Map<String, String> DEFAULTS = Map.ofEntries(
            Map.entry("strata.button.ok", "OK"),
            Map.entry("strata.button.cancel", "Cancel"),
            Map.entry("strata.button.yes", "Yes"),
            Map.entry("strata.button.no", "No"),
            Map.entry("strata.button.close", "Close"),
            Map.entry("strata.password.show", "Show password"),
            Map.entry("strata.password.hide", "Hide password"),
            Map.entry("strata.date.placeholder", "Pick a date"),
            Map.entry("strata.time.placeholder", "Pick a time"),
            Map.entry("strata.datetime.placeholder", "Pick a date and time"),
            Map.entry("strata.calendar.title", "%1$s %2$s"),
            Map.entry("strata.calendar.decade", "%1$s - %2$s"));

    private static final System.Logger LOGGER = System.getLogger(Translations.class.getName());
    private static final Set<String> MISSING = ConcurrentHashMap.newKeySet();
    private static final AtomicLong REPLACED = new AtomicLong();
    private static volatile Translator translator = Translator.NONE;

    private Translations() {
    }

    /**
     * Returns the translator.
     *
     * @return the translator; {@link Translator#NONE} until one is set
     */
    public static Translator translator() {
        return Translations.translator;
    }

    /**
     * Replaces the translator, which every host notices on its next layout or render.
     *
     * @param translator the translator
     */
    public static void setTranslator(final Translator translator) {
        Translations.translator = Objects.requireNonNull(translator, "translator");
        Translations.REPLACED.incrementAndGet();
        Translations.MISSING.clear();
    }

    /**
     * Returns the pattern for a key: the translator's, then Strata's English, then the key itself.
     *
     * @param key the key
     * @return the pattern
     */
    public static String translate(final String key) {
        final String translated = Translations.translator.translate(key).orElse(null);
        if (translated != null) {
            return translated;
        }
        final String fallback = Translations.DEFAULTS.get(key);
        if (fallback != null) {
            return fallback;
        }
        if (Translations.MISSING.add(key)) {
            LOGGER.log(System.Logger.Level.WARNING, "No translation for " + key);
        }
        return key;
    }

    /**
     * Returns a number that changes whenever translated text may read differently.
     *
     * @return the version
     */
    public static long version() {
        return (Translations.REPLACED.get() << 40) ^ Translations.translator.version();
    }
}
