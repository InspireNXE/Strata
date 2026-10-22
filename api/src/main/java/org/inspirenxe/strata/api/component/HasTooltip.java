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

import org.inspirenxe.strata.api.state.PropertyKey;
import org.inspirenxe.strata.api.text.Text;

import java.util.Optional;

/**
 * A component or entry that can show a tooltip. Every container and nearly every control has one.
 *
 * <p>While the pointer rests on a component, the host shows the tooltip of the deepest component under it that has
 * one set, looking through its ancestors, so a tooltip on a panel covers what is inside it. A disabled component
 * shows its tooltip too, so it can say why it is disabled.</p>
 *
 * <p>Two entries have one too, a {@link org.inspirenxe.strata.api.control.pane.Tab} and a
 * {@link org.inspirenxe.strata.api.control.pane.NavigationItem}, which their control shows while the pointer rests
 * on them. The interface has no events, so entries can have it.</p>
 *
 * <p>To give a custom component a tooltip, extend {@link org.inspirenxe.strata.api.extension.CustomComponent} and
 * declare {@code implements HasTooltip<Self>}: the class already has these methods. Do not implement this interface
 * any other way.</p>
 *
 * @param <S> the component's own type, which the setters return
 */
public interface HasTooltip<S extends HasTooltip<S>> {
    /**
     * The tooltip, as {@link #tooltip()} and {@link #setTooltip(Tooltip)} say, null for none. Binds one way.
     */
    PropertyKey<HasTooltip<?>, Tooltip> TOOLTIP = PropertyKey.of("has_tooltip.tooltip",
            target -> target.tooltip().orElse(null), (target, tooltip) -> {
                if (tooltip == null) {
                    target.clearTooltip();
                } else {
                    target.setTooltip(tooltip);
                }
            });

    /**
     * Returns the tooltip.
     *
     * @return the tooltip, or empty for none
     */
    Optional<Tooltip> tooltip();

    /**
     * Sets what the host shows near the pointer while it rests on this component or a descendant that sets none.
     *
     * @param tooltip the tooltip; {@link #clearTooltip()} removes it
     * @return this component
     */
    S setTooltip(Tooltip tooltip);

    /**
     * Removes the tooltip.
     *
     * @return this component
     */
    S clearTooltip();

    /**
     * Sets a tooltip of text. Line breaks ({@code \n}) start new lines.
     *
     * @param text the text
     * @return this component
     */
    default S setTooltip(final Text text) {
        return this.setTooltip(Tooltip.text(text));
    }

    /**
     * Sets a tooltip of literal text. Line breaks ({@code \n}) start new lines.
     *
     * @param text the text
     * @return this component
     */
    default S setTooltip(final String text) {
        return this.setTooltip(Tooltip.text(text));
    }
}
