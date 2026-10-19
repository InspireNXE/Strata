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

import java.util.Objects;

/**
 * A range of a text field's text drawn in its own style, as a {@link TextHighlighter} returns it.
 *
 * <p>The range is in {@code char} indexes into the field's text, half open: {@code start} is the first index in
 * the run and {@code end} the first index after it. Derive {@code style} from the field's style the highlighter is
 * given, with the {@code with} methods or {@link TextStyle#merge(TextStyle, TextStyle.Property,
 * TextStyle.Property...)}, so the run keeps what the theme gives the field.</p>
 *
 * @param start the first index in the run
 * @param end the first index after the run
 * @param style how the run is drawn
 */
public record StyleRun(int start, int end, TextStyle style) {

    /**
     * Checks the arguments.
     *
     * @throws IllegalArgumentException if {@code start} is negative or {@code end} is before {@code start}
     */
    public StyleRun {
        Objects.requireNonNull(style, "style");
        if (start < 0 || end < start) {
            throw new IllegalArgumentException("Bad run " + start + ".." + end);
        }
    }
}
