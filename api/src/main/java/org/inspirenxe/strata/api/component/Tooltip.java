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
 * <p>The host shows a tooltip once the pointer has rested on its component for the host's tooltip delay, or at
 * once when the pointer moves straight from another component's tooltip. The delay is the one the adapter sets,
 * else the theme's {@value #STYLE_DELAY}, else {@value #DEFAULT_DELAY_MILLIS} milliseconds. It moves with the
 * pointer: its top-left corner sits {@value #STYLE_OFFSET_X} units right of and {@value #STYLE_OFFSET_Y} units below
 * the pointer's whole-unit position (12 right and 12 above by default), flipped to the left side when it would
 * leave the host on the right, and kept inside the host. It hides while a drag is in progress. A tooltip draws
 * above the screen and every overlay, and takes no input.</p>
 *
 * <p>There are two kinds. {@link Lines}, made by {@link #text(Text)}, {@link #text(String)} and {@link #lines},
 * is drawn by Strata: on the {@value #STYLE_BACKGROUND} paint inside the {@value #STYLE_PADDING} insets, 4 units on
 * every side by default, with its plain parts in the {@value #STYLE_TEXT} text style and styled parts changing it
 * (see {@link Text}). {@link Custom}, made by {@link #of}, is a component built afresh each time the tooltip shows,
 * drawn with no box around it. Engine adapters can offer factory methods that build tooltips from engine objects,
 * such as the lines an engine shows for one of its items.</p>
 */
public sealed interface Tooltip {
    /**
     * Style key for the box behind a {@link Lines} tooltip (paint, optional). If it is missing, the box is a dark
     * fill with a one-unit purple edge.
     */
    String STYLE_BACKGROUND = "tooltip.background";
    /**
     * Style key for the text of a {@link Lines} tooltip (text style, optional). Falls back to the default style.
     */
    String STYLE_TEXT = "tooltip.text";
    /**
     * Style key for the space between a {@link Lines} tooltip's edges and its text (insets, optional). Defaults to
     * 4 on every side.
     */
    String STYLE_PADDING = "tooltip.padding";
    /**
     * Style key for a second layer drawn over the {@value #STYLE_BACKGROUND} of a {@link Lines} tooltip, such as
     * an edge (paint, optional). Defaults to none.
     */
    String STYLE_FRAME = "tooltip.frame";
    /**
     * Style key for how far the background and frame of a {@link Lines} tooltip reach past its box (insets,
     * optional), for paints whose images have a margin of their own. The box, which is placed and kept inside the
     * host, is the text and its padding. Defaults to none.
     */
    String STYLE_OUTSET = "tooltip.outset";
    /**
     * Style key for how long the pointer rests on a component before its tooltip shows (metric, optional), in
     * milliseconds. A delay the adapter sets wins. Defaults to {@value #DEFAULT_DELAY_MILLIS}.
     */
    String STYLE_DELAY = "tooltip.delay";
    /**
     * Style key for where a tooltip's left edge sits, right of the pointer (metric, optional), in UI units. When
     * flipped to the left side, its right edge sits this far left of the pointer. Defaults to 12.
     */
    String STYLE_OFFSET_X = "tooltip.offset.x";
    /**
     * Style key for where a tooltip's top edge sits, below the pointer (metric, optional), in UI units; a negative
     * value puts it above. Defaults to -12.
     */
    String STYLE_OFFSET_Y = "tooltip.offset.y";
    /**
     * Style key for placing a tooltip below its component rather than beside the pointer (metric, optional): any
     * value but 0 turns it on. Defaults to 0. The left edge still follows {@value #STYLE_OFFSET_X}. With the
     * pointer at height {@code p}, the component's top at {@code t}, its bottom at {@code b} and its height
     * {@code h}, the box's top is {@code p + e(|p + o - t|)}, where {@code e(d)} eases from {@code h - o} at a
     * distance of 0 to {@code g} at a distance of {@code h} or more and rounds to a whole unit, {@code o} is
     * {@value #STYLE_BELOW_OVERLAP} and {@code g} is {@value #STYLE_BELOW_GAP}. So the box sits just below the
     * component and moves a little with the pointer. When the host has no room for the box below
     * {@code b + h}, less {@code g}, the box's bottom goes to {@code p - e(|p + o - b|)} instead, above the pointer.
     */
    String STYLE_BELOW = "tooltip.below";
    /**
     * Style key for the overlap of a tooltip placed below its component (metric, optional), in UI units. See
     * {@value #STYLE_BELOW}. Defaults to 3.
     */
    String STYLE_BELOW_OVERLAP = "tooltip.below.overlap";
    /**
     * Style key for the gap of a tooltip placed below its component (metric, optional), in UI units. See
     * {@value #STYLE_BELOW}. Defaults to 5.
     */
    String STYLE_BELOW_GAP = "tooltip.below.gap";
    /**
     * How long, in milliseconds, the pointer rests on a component before its tooltip shows, unless the adapter or
     * the theme sets another delay.
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
