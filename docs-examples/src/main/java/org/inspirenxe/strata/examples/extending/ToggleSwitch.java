package org.inspirenxe.strata.examples.extending;

// --8<-- [start:interface]
import org.inspirenxe.strata.api.component.Component;

import java.util.function.Consumer;

/**
 * An on/off switch with a caption to its right: a track with a knob that sits left when off and right when on.
 */
public interface ToggleSwitch extends Component<ToggleSwitch> {
    /** Style key for the track when off (paint, required). */
    String STYLE_TRACK = "examplemod.switch.track";
    /** Style key for the track when on (paint, required). */
    String STYLE_TRACK_ON = "examplemod.switch.track.on";
    /** Style key for the knob (paint, required). */
    String STYLE_KNOB = "examplemod.switch.knob";
    /** Style key for the caption (text style, optional). */
    String STYLE_TEXT = "examplemod.switch.text";

    boolean isOn();

    ToggleSwitch setOn(boolean on);

    ToggleSwitch onChange(Consumer<Boolean> listener);

    /**
     * What a snapshot records about a switch.
     *
     * @param on whether it was on
     * @param text its caption
     */
    record State(boolean on, String text) {
    }
}
// --8<-- [end:interface]
