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
package org.inspirenxe.strata.api.control.display;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.HasCursor;
import org.inspirenxe.strata.api.component.HasTooltip;
import org.inspirenxe.strata.api.control.attribute.text.HasText;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.text.TextOverflow;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;

import java.util.List;
import java.util.Objects;

/**
 * Text that the user reads but does not edit.
 *
 * <p>The text breaks into lines at each {@code \n}. With {@linkplain #setWrap(boolean) wrapping} on, it also breaks
 * between words to fit the width it is offered, and breaks a single word that is wider than a whole line between
 * characters. The label's desired size is its widest line by the number of lines times the line height, all times
 * the {@linkplain #setTextScale text scale}.</p>
 *
 * <p>The text is {@link Text}: its plain parts are drawn in the label's {@linkplain #style() style}, and styled
 * parts change that style, such as a white item name followed by a gray count. Styled parts wrap and break like plain
 * text.</p>
 *
 * <p>With {@link TextOverflow#ELLIPSIS}, a line wider than the label's bounds is cut short and ends in
 * {@code ...}, and the label asks for no more width than it is offered.</p>
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
 * Label row = factory.label("").setSpans(List.of(
 *         new TextSpan("Diamond", white), new TextSpan(" x 64", gray)));
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#label(String)}. Do not implement this
 * interface.</p>
 */
public interface Label extends Component<Label>, HasCursor<Label>, HasTooltip<Label>, HasText<Label> {
    /**
     * Style key for the label's text style.
     */
    String STYLE_TEXT = "label.text";

    /**
     * Returns the text.
     *
     * @return the text, possibly with {@code \n} line breaks
     */
    Text text();

    /**
     * Replaces the text. Its plain parts are drawn in the label's {@linkplain #style() style}, and styled parts
     * change that style; see {@link Text}. A new layout runs if it changed.
     *
     * @param text the new text; {@code \n} starts a new line
     * @return this label
     */
    Label setText(Text text);

    /**
     * Returns the text scale.
     *
     * @return the factor text is drawn and measured at; 1 by default
     */
    float textScale();

    /**
     * Draws the text larger or smaller than the font's size, such as 0.8 for small print or 2 for a banner. The
     * label measures at the scaled size.
     *
     * @param scale the factor
     * @return this label
     * @throws IllegalArgumentException if the scale is not greater than 0 or not finite
     */
    Label setTextScale(float scale);

    /**
     * Returns what happens to a line wider than the label.
     *
     * @return the overflow; {@link TextOverflow#VISIBLE} by default
     */
    TextOverflow overflow();

    /**
     * Sets what happens to a line wider than the label's bounds.
     *
     * @param overflow the overflow
     * @return this label
     */
    Label setOverflow(TextOverflow overflow);

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
    record State(Text text, TextStyle style, boolean wraps, HorizontalAlignment textAlignment) {

        /**
         * Checks the fields that must not be null.
         */
        public State {
            Objects.requireNonNull(text, "text");
            Objects.requireNonNull(textAlignment, "textAlignment");
        }
    }
}
