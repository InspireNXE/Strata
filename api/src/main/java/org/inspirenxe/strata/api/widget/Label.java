package org.inspirenxe.strata.api.widget;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.text.TextStyle;

import java.util.Objects;

/**
 * Text that the user reads but does not edit.
 *
 * <p>The text breaks into lines at each {@code \n}. With {@linkplain #setWrap(boolean) wrapping} on, it also breaks
 * between words to fit the width it is offered, and breaks a single word that is wider than a whole line between
 * characters. The label's desired size is its widest line by the number of lines times the line height.</p>
 *
 * <p>A label is not focusable and has no default behavior for input, so clicks and keys pass through to its
 * ancestors.</p>
 *
 * <h2>Theme keys</h2>
 * <ul>
 *     <li>{@value #STYLE_TEXT} (text style, optional): the text style used unless one is set with
 *     {@link #setStyle}. Falls back to {@link TextStyle#DEFAULT}.</li>
 * </ul>
 *
 * <pre>{@code
 * Label title = factory.label("Settings")
 *         .setStyle(TextStyle.DEFAULT.withBold(true));
 * Label help = factory.label(longText).setWrap(true);
 * help.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#label(String)}. Do not implement this
 * interface.</p>
 */
public interface Label extends Component<Label> {
    /**
     * Style key for the label's text style.
     */
    String STYLE_TEXT = "label.text";

    /**
     * Returns the text.
     *
     * @return the text, possibly with {@code \n} line breaks
     */
    String text();

    /**
     * Replaces the text. A new layout runs if it changed.
     *
     * @param text the new text; {@code \n} starts a new line
     * @return this label
     * @throws NullPointerException if {@code text} is null
     */
    Label setText(String text);

    /**
     * Returns the style the text is drawn in.
     *
     * @return the style set with {@link #setStyle}, otherwise the theme's {@value #STYLE_TEXT}
     */
    TextStyle style();

    /**
     * Overrides the theme's text style for this label.
     *
     * @param style the style, or {@code null} to go back to the theme's
     * @return this label
     */
    Label setStyle(TextStyle style);

    /**
     * Returns whether the text wraps to the available width.
     *
     * @return false by default
     */
    boolean wraps();

    /**
     * Turns word wrapping on or off. A wrapping label needs a width to wrap to: give it a fixed or maximum width,
     * or {@link HorizontalAlignment#STRETCH} it inside a parent that has one. Offered unlimited width, it wraps only
     * at {@code \n}.
     *
     * @param wrap true to wrap
     * @return this label
     */
    Label setWrap(boolean wrap);

    /**
     * Returns how each line sits within the label's width.
     *
     * @return the text alignment; {@link HorizontalAlignment#LEFT} by default
     */
    HorizontalAlignment textAlignment();

    /**
     * Sets how each line sits within the label's width. It only shows when the label is wider than its text, for
     * example when stretched. {@link HorizontalAlignment#STRETCH} acts like {@link HorizontalAlignment#LEFT}.
     *
     * @param textAlignment the alignment
     * @return this label
     * @throws NullPointerException if {@code textAlignment} is null
     */
    Label setTextAlignment(HorizontalAlignment textAlignment);

    /**
     * A label's own state in a {@link org.inspirenxe.strata.api.component.ComponentSnapshot}.
     *
     * @param text the text; never null
     * @param style the {@linkplain #style() style} the text was drawn in, or null if none was set and no theme was
     *              applied yet
     * @param wraps whether the text {@linkplain #wraps() wraps}
     * @param textAlignment how lines are {@linkplain #textAlignment() aligned}; never null
     */
    record State(String text, TextStyle style, boolean wraps, HorizontalAlignment textAlignment) {

        /**
         * Checks the fields that must not be null.
         *
         * @throws NullPointerException if {@code text} or {@code textAlignment} is null
         */
        public State {
            Objects.requireNonNull(text, "text");
            Objects.requireNonNull(textAlignment, "textAlignment");
        }
    }
}
