package org.inspirenxe.strata.api.control.button;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.ValueEvent;
import org.inspirenxe.strata.api.event.ValueRequestEvent;

import java.util.Objects;

/**
 * A switch that is on or off: a track with a knob that sits at the left when off and at the right when on, with a
 * caption to its right.
 *
 * <p>A left click anywhere on it flips it, and so does Enter, keypad Enter or Space while it has focus (not
 * repeated). It consumes those inputs. Each flip by the user fires {@link #TOGGLE}, and every change fires
 * {@link #TOGGLED}. A disabled switch does neither and draws its disabled look.</p>
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
 * ToggleSwitch pvp = factory.toggleSwitch("PvP").setOn(settings.pvp())
 *         .onEvent(ToggleSwitch.TOGGLED, event -> settings.setPvp(event.value()));
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
     * Fired when the user flips the switch by click or key, before it changes. The value is the state the switch
     * is about to take: true to turn it on. Canceling it leaves the switch as it is. A handler may set another
     * value, which the switch applies like {@link #setOn}; null is ignored. Fires only for the user's actions.
     */
    EventType<ValueRequestEvent<ToggleSwitch, Boolean>> TOGGLE = EventType.valueRequest("toggle_switch.toggle");
    /**
     * Fired after the switch was turned on or off. The value is the new state and the previous value the old one.
     * Fires for changes made from code too.
     */
    EventType<ValueEvent<ToggleSwitch, Boolean>> TOGGLED = EventType.valueReport("toggle_switch.toggled");

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
     * Turns the switch on or off. Fires {@link #TOGGLED} if the state changed. Works while disabled too.
     *
     * @param on true to turn it on
     * @return this switch
     */
    ToggleSwitch setOn(boolean on);

    /**
     * Flips the switch as if it had been clicked, firing {@link #TOGGLED} but not {@link #TOGGLE}. Does nothing
     * while disabled.
     */
    void toggle();

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
