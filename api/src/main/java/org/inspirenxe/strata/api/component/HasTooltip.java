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
package org.inspirenxe.strata.api.component;

/**
 * A component that can show a tooltip. Every container and nearly every control has one.
 *
 * <p>While the pointer rests on a component, the host shows the tooltip of the deepest component under it that has
 * one set, looking through its ancestors, so a tooltip on a panel covers what is inside it.</p>
 *
 * <p>To give a custom component a tooltip, extend {@link org.inspirenxe.strata.api.extension.CustomComponent} and
 * declare {@code implements HasTooltip<Self>}: the class already has these methods. Do not implement this interface
 * any other way.</p>
 *
 * @param <S> the component's own type, which the setters return
 */
public interface HasTooltip<S extends HasTooltip<S>> {

    /**
     * Returns the tooltip.
     *
     * @return the tooltip, or {@code null} for none
     */
    Tooltip tooltip();

    /**
     * Sets what the host shows near the pointer while it rests on this component or a descendant that sets none.
     *
     * @param tooltip the tooltip, or {@code null} for none
     * @return this component
     */
    S setTooltip(Tooltip tooltip);

    /**
     * Sets a plain-text tooltip. Line breaks ({@code \n}) start new lines.
     *
     * @param text the text, or {@code null} for no tooltip
     * @return this component
     */
    default S setTooltip(final String text) {
        return this.setTooltip(text == null ? null : Tooltip.text(text));
    }
}
