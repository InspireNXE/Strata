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

/**
 * A control or entry that shows one string of text as its caption, such as a button, a check box, a label, a tab or a
 * menu item.
 *
 * <p>Only Strata implements this interface.</p>
 *
 * @param <S> the control's own type, which the setter returns
 */
public interface HasText<S extends HasText<S>> {

    /**
     * Returns the text.
     *
     * @return the text, never null; empty when there is none
     */
    String text();

    /**
     * Replaces the text. The control lays out again if its size depends on it.
     *
     * @param text the text, or an empty string for none
     * @return this control
     */
    S setText(String text);
}
