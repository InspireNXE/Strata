package org.inspirenxe.strata.api.control.button;

import org.inspirenxe.strata.api.component.Component;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * A switch that is on or off: a track with a knob that sits at the left when off and at the right when on, with a
 * caption to its right.
 *
 * <p>A left click anywhere on it flips it, and so does Enter, keypad Enter or Space while it has focus (not
 * repeated). It consumes those inputs. A disabled switch does neither and draws its disabled look.</p>
 *
 * <p>It is focusable and tabbable and shows the hand cursor. The track is the caption's line height plus 4 units
 * tall and twice that wide. The knob is a square 2 units in from the track's edges. The caption sits 4 units right
 * of the track, centered on it vertically. The desired size is the track, the gap and the caption's width wide (just
 * the track when the caption is empty), and the taller of the track and the caption.</p>
 *
 * <h2>Theme keys</h2>
 * <ul>
 *     <li>{@value #STYLE_TRACK} (paint, required): the track when off.</li>
 *     <li>{@value #STYLE_TRACK_ON} (paint, required): the track when on.</li>
 *     <li>{@value #STYLE_TRACK_DISABLED} (paint, optional): the track when disabled. Falls back to
 *     {@value #STYLE_TRACK}.</li>
 *     <li>{@value #STYLE_KNOB} (paint, required): the knob.</li>
 *     <li>{@value #STYLE_KNOB_HOVERED} (paint, optional): the knob when hovered or focused. Falls back to
 *     {@value #STYLE_KNOB}.</li>
 *     <li>{@value #STYLE_KNOB_DISABLED} (paint, optional): the knob when disabled. Falls back to
 *     {@value #STYLE_KNOB}.</li>
 *     <li>{@value #STYLE_FOCUS} (paint, optional): a one-unit ring drawn inside the track's edge while focused.</li>
 *     <li>{@value #STYLE_TEXT} (text style, optional): the caption. Falls back to the default text style.</li>
 *     <li>{@value #STYLE_TEXT_DISABLED} (text style, optional): the caption when disabled. Falls back to
 *     {@value #STYLE_TEXT}.</li>
 * </ul>
 *
 * <pre>{@code
 * ToggleSwitch pvp = factory.toggleSwitch("PvP").setOn(settings.pvp()).onToggled(settings::setPvp);
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#toggleSwitch(String)}. Do not implement this
 * interface.</p>
 */
public interface ToggleSwitch extends Component<ToggleSwitch> {
    /**
     * Style key for the track when off (paint, required).
     */
    String STYLE_TRACK = "toggle.track";
    /**
     * Style key for the track when on (paint, required).
     */
    String STYLE_TRACK_ON = "toggle.track.on";
    /**
     * Style key for the track when disabled (paint, optional).
     */
    String STYLE_TRACK_DISABLED = "toggle.track.disabled";
    /**
     * Style key for the knob (paint, required).
     */
    String STYLE_KNOB = "toggle.knob";
    /**
     * Style key for the knob when hovered or focused (paint, optional).
     */
    String STYLE_KNOB_HOVERED = "toggle.knob.hovered";
    /**
     * Style key for the knob when disabled (paint, optional).
     */
    String STYLE_KNOB_DISABLED = "toggle.knob.disabled";
    /**
     * Style key for the focus ring (paint, optional).
     */
    String STYLE_FOCUS = "toggle.focus";
    /**
     * Style key for the caption (text style, optional).
     */
    String STYLE_TEXT = "toggle.text";
    /**
     * Style key for the caption when disabled (text style, optional).
     */
    String STYLE_TEXT_DISABLED = "toggle.text.disabled";

    /**
     * Returns the caption.
     *
     * @return the caption text
     */
    String text();

    /**
     * Replaces the caption. A new layout runs.
     *
     * @param text the caption, on one line; may be empty for a switch alone
     * @return this switch
     */
    ToggleSwitch setText(String text);

    /**
     * Returns whether the switch is on.
     *
     * @return true if on; false by default
     */
    boolean isOn();

    /**
     * Turns the switch on or off. Listeners run if the state changed. Works while disabled too.
     *
     * @param on true to turn it on
     * @return this switch
     */
    ToggleSwitch setOn(boolean on);

    /**
     * Flips the switch as if it had been clicked, and runs the listeners. Does nothing while disabled. Fires no
     * events.
     */
    void toggle();

    /**
     * Adds a listener told the new state after every change, by input, {@link #toggle()} or {@link #setOn}.
     * Listeners run in the order they were added.
     *
     * @param listener receives true when the switch turned on
     * @return this switch
     */
    ToggleSwitch onToggled(Consumer<Boolean> listener);

    /**
     * A switch's own state in a {@link org.inspirenxe.strata.api.component.ComponentSnapshot}.
     *
     * @param on whether the switch was {@linkplain #isOn() on}
     * @param text the caption; never null
     */
    record State(boolean on, String text) {

        /**
         * Checks the text.
         */
        public State {
            Objects.requireNonNull(text, "text");
        }
    }
}
