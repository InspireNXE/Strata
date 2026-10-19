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
package org.inspirenxe.strata.api.text;

import java.util.List;

/**
 * Styles parts of a text field's text, for syntax coloring, highlighted matches, colored chat input and the like.
 * Set one with {@link org.inspirenxe.strata.api.control.attribute.text.TextInput#setHighlighter}.
 *
 * <pre>{@code
 * box.setHighlighter((text, base) -> {
 *     final List<StyleRun> runs = new ArrayList<>();
 *     final int at = text.indexOf("TODO");
 *     if (at >= 0) {
 *         runs.add(new StyleRun(at, at + 4, base.withColor(0xFFFFD54F).withBold(true)));
 *     }
 *     return runs;
 * });
 * }</pre>
 *
 * <p>Requirements for implementations:</p>
 * <ul>
 *     <li>Return runs in order of {@code start}, not overlapping, inside the text. The field ignores a run that
 *     breaks any of these rules and draws its range in {@code base}. Text no run covers is drawn in
 *     {@code base}.</li>
 *     <li>Derive styles from {@code base}, the style the field would draw its text in, so highlighted text keeps what
 *     the theme gives the field, such as a shadow.</li>
 *     <li>Be a pure function of its arguments. The field calls it when its text or its base style changes, from code
 *     or the user, caches the runs, and never calls it while painting.</li>
 *     <li>Run on the UI thread, like the field, and return quickly: it runs on every edit.</li>
 * </ul>
 */
@FunctionalInterface
public interface TextHighlighter {
    /**
     * Styles nothing: the field draws all its text in its own style.
     */
    TextHighlighter NONE = (text, base) -> List.of();

    /**
     * Returns the runs to draw in their own styles.
     *
     * @param text the field's whole text
     * @param base the style the field draws text in, from its theme and state
     * @return the runs, in order and not overlapping; may be empty, must not be null
     */
    List<StyleRun> highlight(String text, TextStyle base);
}
