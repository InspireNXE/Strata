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
package org.inspirenxe.strata.api.control.button;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.Focusable;
import org.inspirenxe.strata.api.component.HasCursor;
import org.inspirenxe.strata.api.component.HasTooltip;
import org.inspirenxe.strata.api.control.attribute.text.HasText;
import org.inspirenxe.strata.api.control.attribute.value.Toggle;
import org.inspirenxe.strata.api.text.Text;

import java.util.Objects;

/**
 * A switch that is on or off: a track with a knob that sits at the left when off and at the right when on, with a
 * caption to its right.
 *
 * <p>A left click anywhere on it flips it, and so does Enter, keypad Enter or Space while it has focus (not
 * repeated). It consumes those inputs. Each flip by the user fires {@link Toggle#TOGGLE}, and every change fires
 * {@link Toggle#TOGGLED}. A disabled switch does neither and draws its disabled look.</p>
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
 * ToggleSwitch pvp = factory.toggleSwitch("PvP").setSelected(settings.pvp())
 *         .onEvent(Toggle.TOGGLED, event -> settings.setPvp(event.value()));
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#toggleSwitch(String)}. Do not implement this
 * interface.</p>
 */
public interface ToggleSwitch extends Component<ToggleSwitch>, Focusable<ToggleSwitch>, HasCursor<ToggleSwitch>,
        HasTooltip<ToggleSwitch>, HasText<ToggleSwitch>, Toggle<ToggleSwitch> {
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
     * Style key for the track's size around a box a line tall and two lines wide (insets, optional): the track is
     * that box grown by these insets. Defaults to 4 at the sides and 2 at the top and bottom.
     */
    String STYLE_TRACK_PADDING = "toggle.track.padding";
    /**
     * Style key for the space between the knob and the track's edges (insets, optional). The knob is square, as
     * tall as the track less this top and bottom, and sits this left in from the track's left end when off and
     * this right in from its right end when on. Defaults to 2 on every side.
     */
    String STYLE_KNOB_PADDING = "toggle.knob.padding";

    /**
     * Returns the caption.
     *
     * @return the caption text
     */
    Text text();

    /**
     * Replaces the caption. A new layout runs.
     *
     * @param text the caption, on one line; may be empty for a switch alone
     * @return this switch
     */
    ToggleSwitch setText(Text text);

    /**
     * A switch's own state in a {@link org.inspirenxe.strata.api.component.ComponentSnapshot}.
     *
     * @param selected whether the switch was {@linkplain #isSelected() on}
     * @param text the caption; never null
     */
    record State(boolean selected, Text text) {

        /**
         * Checks the text.
         */
        public State {
            Objects.requireNonNull(text, "text");
        }
    }
}
