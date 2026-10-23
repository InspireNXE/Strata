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
package org.inspirenxe.strata.api.layout;

import java.util.Locale;
import java.util.Set;

/**
 * The direction a component's content runs in. A screen in Arabic or Hebrew runs right to left: rows start at the
 * right, the navigation pane sits on the right, text lines up on the right, and Left and Right swap where they move
 * through items. See {@link org.inspirenxe.strata.api.component.Component#setFlowDirection}.
 */
public enum FlowDirection {
    /**
     * Content starts at the left, as in English.
     */
    LEFT_TO_RIGHT,
    /**
     * Content starts at the right, as in Arabic and Hebrew.
     */
    RIGHT_TO_LEFT;

    // Scripts written right to left, by their ISO 15924 codes
    private static final Set<String> SCRIPTS = Set.of("Arab", "Hebr", "Thaa", "Syrc", "Nkoo", "Adlm", "Rohg",
            "Mand", "Samr", "Mend", "Yezi");
    // Languages whose usual script is written right to left
    private static final Set<String> LANGUAGES = Set.of("ar", "he", "iw", "fa", "ur", "yi", "ji", "ps", "sd", "ckb",
            "ug", "dv", "syr", "ks", "arc", "nqo", "mzn", "glk", "lrc", "bal", "prs");

    /**
     * Returns whether this is {@link #RIGHT_TO_LEFT}.
     *
     * @return true for right to left
     */
    public boolean isRightToLeft() {
        return this == RIGHT_TO_LEFT;
    }

    /**
     * Returns the direction a language is written in: right to left for a locale whose script, or whose language's
     * usual script when it names none, is written that way, such as Arabic, Hebrew, Persian and Urdu.
     *
     * @param locale the language
     * @return the direction
     */
    public static FlowDirection of(final Locale locale) {
        final String script = locale.getScript();
        if (!script.isEmpty()) {
            return SCRIPTS.contains(script) ? RIGHT_TO_LEFT : LEFT_TO_RIGHT;
        }
        return LANGUAGES.contains(locale.getLanguage()) ? RIGHT_TO_LEFT : LEFT_TO_RIGHT;
    }
}
