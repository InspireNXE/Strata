package org.inspirenxe.strata.api.control.button;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.geom.Insets;

import java.util.Objects;

/**
 * A push button with a text caption.
 *
 * <p>The button runs its actions when it is left-clicked, or when it has focus and Enter or Space is pressed (not
 * repeated). It consumes those inputs. A disabled button does neither and draws its disabled look.</p>
 *
 * <p>It is focusable and tabbable, shows the hand cursor, and has a minimum size of 20 by 20. Its desired size is
 * the caption plus the text padding (8 units left and right, 5 top and bottom by default). The caption is centered
 * in whatever size the button ends up.</p>
 *
 * <h2>Theme keys</h2>
 * <ul>
 *     <li>{@value #STYLE_NORMAL} (paint, required): the face when idle.</li>
 *     <li>{@value #STYLE_HOVERED} (paint, required): the face when hovered or focused.</li>
 *     <li>{@value #STYLE_PRESSED} (paint, required): the face while pressed with the pointer still over it.</li>
 *     <li>{@value #STYLE_DISABLED} (paint, required): the face when disabled.</li>
 *     <li>{@value #STYLE_FOCUS} (paint, optional): a one-unit ring drawn inside the edge while focused. No ring if
 *     missing.</li>
 *     <li>{@value #STYLE_TEXT} (text style, optional): the caption. Falls back to the default text style.</li>
 *     <li>{@value #STYLE_TEXT_DISABLED} (text style, optional): the caption when disabled. Falls back to
 *     {@value #STYLE_TEXT}.</li>
 * </ul>
 *
 * <pre>{@code
 * Button save = factory.button("Save").onAction(this::save);
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#button(String)}. Do not implement this
 * interface.</p>
 */
public interface Button extends Component<Button> {
    /**
     * Style key for the idle face (paint, required).
     */
    String STYLE_NORMAL = "button.normal";
    /**
     * Style key for the hovered or focused face (paint, required).
     */
    String STYLE_HOVERED = "button.hovered";
    /**
     * Style key for the pressed face (paint, required).
     */
    String STYLE_PRESSED = "button.pressed";
    /**
     * Style key for the disabled face (paint, required).
     */
    String STYLE_DISABLED = "button.disabled";
    /**
     * Style key for the focus ring (paint, optional).
     */
    String STYLE_FOCUS = "button.focus";
    /**
     * Style key for the caption (text style, optional).
     */
    String STYLE_TEXT = "button.text";
    /**
     * Style key for the caption when disabled (text style, optional).
     */
    String STYLE_TEXT_DISABLED = "button.text.disabled";

    /**
     * Returns the caption.
     *
     * @return the caption text
     */
    String text();

    /**
     * Replaces the caption. A new layout runs.
     *
     * @param text the caption, on one line
     * @return this button
     */
    Button setText(String text);

    /**
     * Sets the space between the caption and the button's edges, which with the caption gives the desired size.
     *
     * @param padding the padding; 8 left and right, 5 top and bottom by default
     * @return this button
     */
    Button setTextPadding(Insets padding);

    /**
     * Adds an action to run on every press. Actions run in the order they were added. An action may add more
     * actions; they run from the next press.
     *
     * @param action the action
     * @return this button
     */
    Button onAction(Runnable action);

    /**
     * Runs the button's actions as if it had been clicked. Does nothing while the button is disabled. Fires no
     * events.
     */
    void press();

    /**
     * A button's own state in a {@link org.inspirenxe.strata.api.component.ComponentSnapshot}. Whether it was
     * pressed or hovered is in the snapshot itself.
     *
     * @param text the caption; never null
     */
    record State(String text) {

        /**
         * Checks the text.
         */
        public State {
            Objects.requireNonNull(text, "text");
        }
    }
}
