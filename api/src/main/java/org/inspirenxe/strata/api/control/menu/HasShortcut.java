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
package org.inspirenxe.strata.api.control.menu;

import org.inspirenxe.strata.api.text.Text;

/**
 * A menu entry that shows a shortcut hint at its right, such as {@code "Ctrl+C"}: a {@link MenuItem} or a
 * {@link CheckMenuItem}. The hint is only text and binds no key.
 *
 * <p>Do not implement this interface.</p>
 *
 * @param <S> the entry's own type, which the setters return
 */
public interface HasShortcut<S extends HasShortcut<S>> {
    /**
     * Returns the shortcut hint.
     *
     * @return the hint, never null; empty when there is none, which is the default
     */
    Text shortcut();

    /**
     * Sets the hint drawn at the right of the entry, such as {@code "Ctrl+C"}. It is only text and binds no key. The
     * menu lays out again.
     *
     * @param shortcut the hint, or empty text for none
     * @return this entry
     */
    S setShortcut(Text shortcut);

    /**
     * Does the same as {@link #setShortcut(Text)} with literal text.
     *
     * @param shortcut the hint, or an empty string for none
     * @return this entry
     */
    default S setShortcut(final String shortcut) {
        return this.setShortcut(Text.of(shortcut));
    }
}
