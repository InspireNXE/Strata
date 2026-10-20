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

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.text.Text;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * What a host shows near the pointer while it rests on a component; see {@link HasTooltip#setTooltip(Tooltip)}.
 *
 * <p>The host shows a tooltip once the pointer has rested on its component for the host's tooltip delay,
 * {@value #DEFAULT_DELAY_MILLIS} milliseconds unless the adapter sets another, or at once when the pointer moves
 * straight from another component's tooltip. It moves with the pointer: 12 units
 * right of and 12 units above the pointer, flipped to the left side when it would leave the host on the right, and
 * kept inside the host. Tooltips take no input. Text tooltips draw on the {@value #STYLE_BACKGROUND} paint inside
 * the {@value #STYLE_PADDING} insets, 4 units on every side by default. Their plain parts are drawn in the {@value #STYLE_TEXT} text style, and styled parts change
 * it; see {@link Text}.</p>
 *
 * <p>The {@link Lines} kind is drawn by Strata. Engine adapters can offer factory methods that
 * build tooltips from engine objects, such as the lines an engine shows for one of its items.</p>
 */
public sealed interface Tooltip {
    /**
     * Style key for the box behind a tooltip (paint, optional). The flat look is used if it is missing: a dark box
     * with a light edge.
     */
    String STYLE_BACKGROUND = "tooltip.background";
    /**
     * Style key for the text of a {@link Lines} tooltip (text style, optional). Falls back to the default style.
     */
    String STYLE_TEXT = "tooltip.text";
    /**
     * Style key for the space between a text tooltip's edges and its text (insets, optional). Defaults to 4 on
     * every side.
     */
    String STYLE_PADDING = "tooltip.padding";
    /**
     * How long, in milliseconds, the pointer rests on a component before its tooltip shows, unless the adapter sets
     * another delay.
     */
    long DEFAULT_DELAY_MILLIS = 500L;

    /**
     * Returns a tooltip of text.
     *
     * @param text the text; {@code \n} starts a new line
     * @return the tooltip
     */
    static Tooltip text(final Text text) {
        return new Lines(List.of(text));
    }

    /**
     * Returns a tooltip of literal text in the theme's tooltip style.
     *
     * @param text the text; {@code \n} starts a new line
     * @return the tooltip
     */
    static Tooltip text(final String text) {
        return Tooltip.text(Text.of(text));
    }

    /**
     * Returns a tooltip of lines.
     *
     * @param lines the lines, top to bottom; a {@code \n} inside one starts another
     * @return the tooltip
     */
    static Tooltip lines(final List<? extends Text> lines) {
        return new Lines(List.copyOf(lines));
    }

    /**
     * Returns a tooltip whose content is a component, built again each time the tooltip appears.
     *
     * @param builder makes the content with the host's factory
     * @return the tooltip
     */
    static Tooltip of(final Function<ComponentFactory, ? extends Component<?>> builder) {
        return new Custom(builder);
    }

    /**
     * Lines of text, resolved against the theme's tooltip style each time the tooltip appears.
     *
     * @param lines the lines, top to bottom; a {@code \n} inside one starts another
     */
    record Lines(List<Text> lines) implements Tooltip {
        /**
         * Copies the lines, so later changes to the list do not show.
         */
        public Lines {
            lines = List.copyOf(lines);
        }
    }

    /**
     * A component built when the tooltip appears. The host wraps it in nothing; give it its own background.
     *
     * @param builder makes the content with the host's factory
     */
    record Custom(Function<ComponentFactory, ? extends Component<?>> builder) implements Tooltip {
        /**
         * Checks the builder.
         */
        public Custom {
            Objects.requireNonNull(builder, "builder");
        }
    }
}
