package org.inspirenxe.strata.api.widget;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.HorizontalAlignment;
import org.inspirenxe.strata.api.text.TextStyle;

/**
 * Static text. Breaks at newlines and, with wrapping on, at the available width.
 */
public interface Label extends Component<Label> {
    String STYLE_TEXT = "label.text";

    String text();

    Label setText(String text);

    /**
     * The style in use: the one set explicitly, otherwise the theme's {@value #STYLE_TEXT}.
     */
    TextStyle style();

    /**
     * Overrides the theme's text style; {@code null} returns to it.
     */
    Label setStyle(TextStyle style);

    boolean wraps();

    Label setWrap(boolean wrap);

    HorizontalAlignment textAlignment();

    /**
     * How each line sits within the label's width. {@code STRETCH} acts like {@code LEFT}.
     */
    Label setTextAlignment(HorizontalAlignment textAlignment);
}
