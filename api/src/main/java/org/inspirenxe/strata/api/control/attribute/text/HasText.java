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
package org.inspirenxe.strata.api.control.attribute.text;

import org.inspirenxe.strata.api.state.PropertyKey;
import org.inspirenxe.strata.api.text.Text;

/**
 * A control or entry that shows text as its caption, such as a button, a checkbox, a label, a tab or a menu item.
 * The text may be styled or translated; see {@link Text}.
 *
 * <p>Do not implement this interface.</p>
 *
 * @param <S> the control's own type, which the setter returns
 */
public interface HasText<S extends HasText<S>> {
    /**
     * The text, as {@link #text()} and {@link #setText(Text)} say. Binds one way. Only components bind, so an
     * entry such as a tab or a menu item takes no binding.
     */
    PropertyKey<HasText<?>, Text> TEXT = PropertyKey.of("has_text.text", HasText::text, HasText::setText);

    /**
     * Returns the text.
     *
     * @return the text, never null; empty when there is none
     */
    Text text();

    /**
     * Replaces the text. The control lays out again if its size depends on it.
     *
     * @param text the text, or {@link Text#empty()} for none
     * @return this control
     */
    S setText(Text text);

    /**
     * Replaces the text with literal text in the control's own style.
     *
     * @param text the text, or an empty string for none
     * @return this control
     */
    default S setText(final String text) {
        return this.setText(Text.of(text));
    }
}
