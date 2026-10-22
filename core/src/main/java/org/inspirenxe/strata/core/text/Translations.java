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

import org.inspirenxe.strata.api.text.PluralCategory;
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
            Map.entry("strata.calendar.decade", "%1$s - %2$s"),
            Map.entry("strata.validation.integer", "Enter a whole number"),
            Map.entry("strata.validation.integer_range", "Enter a whole number from %1$s to %2$s"),
            Map.entry("strata.validation.decimal", "Enter a number"),
            Map.entry("strata.validation.decimal_range", "Enter a number from %1$s to %2$s"),
            Map.entry("strata.edit.undo", "Undo"),
            Map.entry("strata.edit.redo", "Redo"),
            Map.entry("strata.edit.cut", "Cut"),
            Map.entry("strata.edit.copy", "Copy"),
            Map.entry("strata.edit.paste", "Paste"),
            Map.entry("strata.edit.delete", "Delete"),
            Map.entry("strata.edit.select_all", "Select all"),
            Map.entry("strata.modifier.control", "Ctrl"),
            Map.entry("strata.modifier.alt", "Alt"),
            Map.entry("strata.modifier.shift", "Shift"),
            Map.entry("strata.modifier.super", "Super"),
            Map.entry("strata.modifier.separator", "+"),
            Map.entry("strata.key.unknown", "?"),
            Map.entry("strata.key.space", "Space"),
            Map.entry("strata.key.escape", "Esc"),
            Map.entry("strata.key.enter", "Enter"),
            Map.entry("strata.key.tab", "Tab"),
            Map.entry("strata.key.backspace", "Backspace"),
            Map.entry("strata.key.insert", "Ins"),
            Map.entry("strata.key.delete", "Del"),
            Map.entry("strata.key.left", "Left"),
            Map.entry("strata.key.right", "Right"),
            Map.entry("strata.key.up", "Up"),
            Map.entry("strata.key.down", "Down"),
            Map.entry("strata.key.page_up", "PgUp"),
            Map.entry("strata.key.page_down", "PgDn"),
            Map.entry("strata.key.home", "Home"),
            Map.entry("strata.key.end", "End"),
            Map.entry("strata.key.caps_lock", "Caps Lock"),
            Map.entry("strata.key.num_lock", "Num Lock"),
            Map.entry("strata.key.scroll_lock", "Scroll Lock"),
            Map.entry("strata.key.print_screen", "PrtSc"),
            Map.entry("strata.key.pause", "Pause"),
            Map.entry("strata.key.keypad", "Num %1$s"),
            Map.entry("strata.key.left_shift", "Shift"),
            Map.entry("strata.key.right_shift", "Shift"),
            Map.entry("strata.key.left_control", "Ctrl"),
            Map.entry("strata.key.right_control", "Ctrl"),
            Map.entry("strata.key.left_alt", "Alt"),
            Map.entry("strata.key.right_alt", "Alt"),
            Map.entry("strata.key.left_super", "Super"),
            Map.entry("strata.key.right_super", "Super"),
            Map.entry("strata.key.menu", "Menu"));

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
        final String found = Translations.lookup(key);
        if (found != null) {
            return found;
        }
        if (Translations.MISSING.add(key)) {
            LOGGER.log(System.Logger.Level.WARNING, "No translation for " + key);
        }
        return key;
    }

    /**
     * Returns the pattern for a key from the translator, then Strata's English, without warning when neither has it.
     *
     * @param key the key
     * @return the pattern, or null when there is none
     */
    public static String lookup(final String key) {
        final String translated = Translations.translator.translate(key).orElse(null);
        return translated != null ? translated : Translations.DEFAULTS.get(key);
    }

    /**
     * Returns the pattern for a count: the translation of {@code key.<category>} for the translator's
     * {@linkplain Translator#plural category} of the count, else of {@code key.other}, else {@code key.other} itself
     * with one warning. A translator that returns null for the category, or throws, counts as {@code other}.
     *
     * @param key the key without its category
     * @param count the count
     * @return the pattern
     */
    public static String translatePlural(final String key, final long count) {
        PluralCategory category;
        try {
            category = Translations.translator.plural(count);
        } catch (final RuntimeException e) {
            LOGGER.log(System.Logger.Level.WARNING, "The translator failed to give the plural of " + count, e);
            category = null;
        }
        if (category != null && category != PluralCategory.OTHER) {
            final String found = Translations.lookup(key + '.' + category.suffix());
            if (found != null) {
                return found;
            }
        }
        return Translations.translate(key + '.' + PluralCategory.OTHER.suffix());
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
