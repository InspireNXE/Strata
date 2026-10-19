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

/**
 * Measures text the way the engine draws it. Components get one from
 * {@link org.inspirenxe.strata.api.extension.MeasureContext#text()} or
 * {@link org.inspirenxe.strata.api.extension.Environment#text()}; engine adapters provide it through
 * {@code Platform.textMetrics()}.
 *
 * <h2>Requirements for implementors</h2>
 * <ul>
 *     <li>Match drawing: a span drawn at x must end at x plus its measured width, give or take a unit of rounding.
 *     Text boxes place the caret and selection from these numbers.</li>
 *     <li>Be pure: the same span gives the same result every time, with no side effects.</li>
 *     <li>{@link #width} is 0 for an empty span and never negative.</li>
 *     <li>Widths must not shrink as text grows: appending a code point to a span never makes it narrower. Text
 *     boxes rely on this to find the character under the pointer.</li>
 *     <li>{@link #lineHeight} is positive.</li>
 *     <li>Be fast, or cache: layout measures every label line and a text box measures each prefix of its text.</li>
 *     <li>Calls come from the UI thread only.</li>
 * </ul>
 */
public interface TextMetrics {

    /**
     * Measures how wide a span is drawn.
     *
     * @param span the span; a single line
     * @return the width in UI units; 0 for empty text
     */
    float width(TextSpan span);

    /**
     * Returns the height of one line of text, the distance from one line's top to the next line's top.
     *
     * @param style the style the line is drawn in
     * @return the line height in UI units, positive
     */
    float lineHeight(TextStyle style);
}
