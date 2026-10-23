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
package org.inspirenxe.strata.core.control.collection;

import org.inspirenxe.strata.api.control.collection.ListView;
import org.inspirenxe.strata.api.event.input.CharEvent;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.core.text.Translations;

import java.util.Locale;
import java.util.function.IntFunction;

/**
 * The prefix a user types into a list, a table, a tree or a combo box to jump to an item by its name. Printable
 * characters build the prefix, which starts again once {@value ListView#STYLE_TYPEAHEAD_TIMEOUT} passes without one.
 * The search runs from the item after the current one for a prefix of one character, and from the current one for a
 * longer prefix, so typing on extends the match in place. A prefix of one character typed again and again cycles
 * through the items that start with it. Names are compared ignoring case in the translator's locale.
 */
final class TypeAhead {
    /**
     * What {@link #type} returns for a character it did not take, which the control leaves to its ancestors.
     */
    static final int NOT_TAKEN = -2;
    /**
     * What {@link #type} returns for a character it took that matched no item.
     */
    static final int NO_MATCH = -1;

    private final StringBuilder prefix = new StringBuilder();
    private long last;
    private boolean typed;
    private long timeout = ListView.DEFAULT_TYPEAHEAD_TIMEOUT;

    /**
     * Reads the timeout from a theme.
     */
    void applyTheme(final Theme theme) {
        this.timeout = Math.max(0L, (long) theme.metricOr(ListView.STYLE_TYPEAHEAD_TIMEOUT,
                ListView.DEFAULT_TYPEAHEAD_TIMEOUT));
    }

    /**
     * Forgets the prefix.
     */
    void reset() {
        this.prefix.setLength(0);
        this.typed = false;
    }

    /**
     * Adds a typed character to the prefix and finds the item it leads to.
     *
     * @param event the character
     * @param now the platform's time in milliseconds
     * @param current the current item, or -1
     * @param count how many items there are
     * @param names the name of each item
     * @return the item's index, {@link #NO_MATCH} when the character was taken but no item matches, or
     *     {@link #NOT_TAKEN} for a character with Control or Alt, a control character, or a space that would start a
     *     prefix
     */
    int type(final CharEvent event, final long now, final int current, final int count, final IntFunction<String> names) {
        final int codePoint = event.codePoint();
        if (event.modifiers().control() || event.modifiers().alt() || Character.isISOControl(codePoint)) {
            return NOT_TAKEN;
        }
        if (!this.typed || now - this.last > this.timeout) {
            this.prefix.setLength(0);
        }
        if (this.prefix.isEmpty() && Character.isWhitespace(codePoint)) {
            return NOT_TAKEN;
        }
        this.typed = true;
        this.last = now;
        this.prefix.appendCodePoint(codePoint);
        if (count == 0) {
            return NO_MATCH;
        }
        final Locale locale = Translations.translator().locale();
        final String typed = this.prefix.toString().toLowerCase(locale);
        final int first = typed.codePointAt(0);
        boolean repeated = true;
        for (int i = 0; i < typed.length(); i += Character.charCount(typed.codePointAt(i))) {
            if (typed.codePointAt(i) != first) {
                repeated = false;
                break;
            }
        }
        final String wanted = repeated ? typed.substring(0, Character.charCount(first)) : typed;
        final int start = repeated ? current + 1 : Math.max(0, current);
        for (int i = 0; i < count; i++) {
            final int index = Math.floorMod(start + i, count);
            if (names.apply(index).toLowerCase(locale).startsWith(wanted)) {
                return index;
            }
        }
        return NO_MATCH;
    }
}
