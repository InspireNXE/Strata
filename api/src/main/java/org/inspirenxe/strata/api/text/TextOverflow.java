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
 * What a label does with a line wider than its bounds.
 */
public enum TextOverflow {
    /**
     * Draw the whole line, past the bounds if need be. A clipping parent cuts it off.
     */
    VISIBLE,
    /**
     * Cut the line between characters so it fits with {@code ...} after it, in the style of the last character
     * kept. A character is a grapheme cluster, so a flag, an emoji with its skin tone or a letter with its accents is
     * kept or left out whole. If not even the dots fit, nothing of the line is drawn.
     */
    ELLIPSIS
}
