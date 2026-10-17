package org.inspirenxe.strata.api.component;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.text.TextSpan;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * What a host shows near the pointer while it rests on a component; see {@link Component#setTooltip(Tooltip)}.
 *
 * <p>The host shows a tooltip as soon as the pointer is over its component, and moves it with the pointer: 12 units
 * right of and 12 units above the pointer, flipped to the left side when it would leave the host on the right, and
 * kept inside the host. Tooltips take no input. Text tooltips draw on the {@value #STYLE_BACKGROUND} paint with 4
 * units of padding, in the {@value #STYLE_TEXT} text style unless their spans carry their own.</p>
 *
 * <p>The {@link Text} and {@link Lines} kinds are drawn by Strata. Engine adapters can offer factory methods that
 * build tooltips from engine objects, such as an item's tooltip lines on Minecraft.</p>
 */
public sealed interface Tooltip {
    /**
     * Style key for the box behind a tooltip (paint, optional). The flat look is used if it is missing: a dark box
     * with a light edge.
     */
    String STYLE_BACKGROUND = "tooltip.background";
    /**
     * Style key for the text of a {@link Text} tooltip (text style, optional). Falls back to the default style.
     */
    String STYLE_TEXT = "tooltip.text";

    /**
     * Returns a tooltip of plain text in the theme's tooltip style.
     *
     * @param text the text; {@code \n} starts a new line
     * @return the tooltip
     */
    static Tooltip text(final String text) {
        return new Text(text);
    }

    /**
     * Returns a tooltip of styled lines, each made of spans that keep their own style.
     *
     * @param lines the lines, top to bottom
     * @return the tooltip
     */
    static Tooltip lines(final List<List<TextSpan>> lines) {
        return new Lines(lines);
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
     * Plain text in the theme's tooltip style.
     *
     * @param text the text; {@code \n} starts a new line
     */
    record Text(String text) implements Tooltip {
        /**
         * Checks the text.
         */
        public Text {
            Objects.requireNonNull(text, "text");
        }
    }

    /**
     * Styled lines.
     *
     * @param lines the lines, top to bottom, each a list of spans drawn left to right
     */
    record Lines(List<List<TextSpan>> lines) implements Tooltip {
        /**
         * Copies the lines, so later changes to the lists do not show.
         */
        public Lines {
            lines = lines.stream().map(List::copyOf).toList();
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
