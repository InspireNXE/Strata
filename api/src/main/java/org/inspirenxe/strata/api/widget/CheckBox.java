package org.inspirenxe.strata.api.widget;

import org.inspirenxe.strata.api.component.Component;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * A box that is checked or not, with a caption to its right.
 *
 * <p>A left click anywhere on the check box toggles it, and so does Enter, keypad Enter or Space while it has focus
 * (not repeated). It consumes those inputs. A disabled check box does neither and draws its disabled look.</p>
 *
 * <p>It is focusable and tabbable and shows the hand cursor. The box is square, {@linkplain #setBoxSize sized} to the
 * caption's line height plus 8 units by default, which is 17 with a 9-unit font.
 * The caption sits 4 units right of the box, centered on it vertically. The desired size is the box, the gap and the
 * caption's width wide (just the box when the caption is empty), and the taller of the box and the caption.</p>
 *
 * <h2>Theme keys</h2>
 * <p>The box paints cover the whole box. Themes built from images draw the check mark into
 * the checked paints; themes built from colors draw it with {@value #STYLE_MARK} instead.</p>
 * <ul>
 *     <li>{@value #STYLE_NORMAL} (paint, required): the box when unchecked.</li>
 *     <li>{@value #STYLE_HOVERED} (paint, required): the box when unchecked and hovered or focused.</li>
 *     <li>{@value #STYLE_CHECKED} (paint, required): the box when checked.</li>
 *     <li>{@value #STYLE_CHECKED_HOVERED} (paint, required): the box when checked and hovered or focused.</li>
 *     <li>{@value #STYLE_DISABLED} (paint, optional): the box when unchecked and disabled. Falls back to
 *     {@value #STYLE_NORMAL}.</li>
 *     <li>{@value #STYLE_CHECKED_DISABLED} (paint, optional): the box when checked and disabled. Falls back to
 *     {@value #STYLE_CHECKED}.</li>
 *     <li>{@value #STYLE_MARK} (paint, optional): drawn over the box while checked, inset by a fifth of the box size.
 *     Nothing if missing.</li>
 *     <li>{@value #STYLE_MARK_DISABLED} (paint, optional): the mark when disabled. Falls back to
 *     {@value #STYLE_MARK}.</li>
 *     <li>{@value #STYLE_FOCUS} (paint, optional): a one-unit ring drawn inside the box's edge while focused. No ring
 *     if missing.</li>
 *     <li>{@value #STYLE_TEXT} (text style, optional): the caption. Falls back to the default text style.</li>
 *     <li>{@value #STYLE_TEXT_DISABLED} (text style, optional): the caption when disabled. Falls back to
 *     {@value #STYLE_TEXT}.</li>
 * </ul>
 *
 * <pre>{@code
 * CheckBox music = factory.checkBox("Music")
 *         .setChecked(settings.music())
 *         .onCheckedChanged(settings::setMusic);
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#checkBox(String)}. Do not implement this
 * interface.</p>
 */
public interface CheckBox extends Component<CheckBox> {
    /**
     * Style key for the unchecked box (paint, required).
     */
    String STYLE_NORMAL = "checkbox.normal";
    /**
     * Style key for the unchecked box when hovered or focused (paint, required).
     */
    String STYLE_HOVERED = "checkbox.hovered";
    /**
     * Style key for the checked box (paint, required).
     */
    String STYLE_CHECKED = "checkbox.checked";
    /**
     * Style key for the checked box when hovered or focused (paint, required).
     */
    String STYLE_CHECKED_HOVERED = "checkbox.checked.hovered";
    /**
     * Style key for the unchecked box when disabled (paint, optional).
     */
    String STYLE_DISABLED = "checkbox.disabled";
    /**
     * Style key for the checked box when disabled (paint, optional).
     */
    String STYLE_CHECKED_DISABLED = "checkbox.checked.disabled";
    /**
     * Style key for the check mark drawn over a checked box (paint, optional).
     */
    String STYLE_MARK = "checkbox.mark";
    /**
     * Style key for the check mark when disabled (paint, optional).
     */
    String STYLE_MARK_DISABLED = "checkbox.mark.disabled";
    /**
     * Style key for the focus ring (paint, optional).
     */
    String STYLE_FOCUS = "checkbox.focus";
    /**
     * Style key for the caption (text style, optional).
     */
    String STYLE_TEXT = "checkbox.text";
    /**
     * Style key for the caption when disabled (text style, optional).
     */
    String STYLE_TEXT_DISABLED = "checkbox.text.disabled";

    /**
     * Returns the caption.
     *
     * @return the caption text
     */
    String text();

    /**
     * Replaces the caption. A new layout runs.
     *
     * @param text the caption, on one line; may be empty for a box alone
     * @return this check box
     */
    CheckBox setText(String text);

    /**
     * Returns whether the box is checked.
     *
     * @return true if checked; false by default
     */
    boolean isChecked();

    /**
     * Checks or unchecks the box. Listeners run if the state changed. Works while disabled too, so code can keep a
     * disabled check box in step with its setting.
     *
     * @param checked true to check the box
     * @return this check box
     */
    CheckBox setChecked(boolean checked);

    /**
     * Flips the box as if it had been clicked, and runs the listeners. Does nothing while disabled. Fires no events.
     */
    void toggle();

    /**
     * Returns the size of the square box.
     *
     * @return the box size in UI units: the one set with {@link #setBoxSize}, otherwise the caption's line height
     *         plus 8 as of the last layout
     */
    float boxSize();

    /**
     * Fixes the size of the square box. A new layout runs.
     *
     * @param size the box size in UI units, or {@link Float#NaN} for the caption's line height plus 8
     * @return this check box
     * @throws IllegalArgumentException if {@code size} is negative or infinite
     */
    CheckBox setBoxSize(float size);

    /**
     * Adds a listener told the new state after every change, by input, {@link #toggle()} or {@link #setChecked}.
     * Listeners run in the order they were added.
     *
     * @param listener receives true when the box became checked
     * @return this check box
     */
    CheckBox onCheckedChanged(Consumer<Boolean> listener);

    /**
     * A check box's own state in a {@link org.inspirenxe.strata.api.component.ComponentSnapshot}.
     *
     * @param checked whether the box was {@linkplain #isChecked() checked}
     * @param text the caption; never null
     */
    record State(boolean checked, String text) {

        /**
         * Checks the text.
         */
        public State {
            Objects.requireNonNull(text, "text");
        }
    }
}
