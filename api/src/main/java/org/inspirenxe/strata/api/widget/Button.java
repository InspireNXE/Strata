package org.inspirenxe.strata.api.widget;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.geom.Insets;

/**
 * A push button with a text caption. Fires its actions on a left click, or on Enter or Space while focused.
 */
public interface Button extends Component<Button> {
    String STYLE_NORMAL = "button.normal";
    String STYLE_HOVERED = "button.hovered";
    String STYLE_PRESSED = "button.pressed";
    String STYLE_DISABLED = "button.disabled";
    String STYLE_FOCUS = "button.focus";
    String STYLE_TEXT = "button.text";
    String STYLE_TEXT_DISABLED = "button.text.disabled";

    String text();

    Button setText(String text);

    /**
     * Space between the caption and the button's edge.
     */
    Button setTextPadding(Insets padding);

    /**
     * Adds an action run on every press of the button.
     */
    Button onAction(Runnable action);

    /**
     * Runs the button's actions as if it were pressed. Does nothing while disabled.
     */
    void press();
}
