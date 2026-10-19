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
package org.inspirenxe.strata.api.input;

/**
 * A modifier key that may be held during input. The left and right keys of each kind count as the same modifier,
 * which is what shortcut matching needs; use {@link Key} to tell them apart.
 */
public enum Modifier {
    /** Either Shift key. */
    SHIFT,
    /** Either Control key. */
    CONTROL,
    /** Either Alt key, including AltGr. */
    ALT,
    /** Either Super key: the Windows key, or Command on macOS. */
    SUPER;

    final int bit() {
        return 1 << this.ordinal();
    }
}
