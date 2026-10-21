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
package org.inspirenxe.strata.api.control.field;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.HasCursor;
import org.inspirenxe.strata.api.component.HasTooltip;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.control.ValueEvent;
import org.inspirenxe.strata.api.event.control.ValueRequestEvent;

/**
 * Picks an ARGB color: a square for saturation and brightness, a strip for hue, an optional strip for alpha, and a
 * hex field.
 *
 * <h2>Values</h2>
 * <p>The color is an {@code int} in ARGB order, {@code 0xAARRGGBB}, as everywhere in Strata; opaque white
 * ({@code 0xFFFFFFFF}) by default. The picker also keeps the color's hue, saturation and brightness, so a gray or
 * black color keeps the hue the user last chose: dragging the brightness down to black and back up returns to the
 * same hue. {@link #setColor} works the hue out from the new color, and keeps the old hue when the new color has
 * none (a gray), and the old saturation too when it is black.</p>
 *
 * <h2>Input</h2>
 * <ul>
 *     <li>A left press in the square sets the saturation (left to right, 0 to 1) and the brightness (bottom to top, 0
 *     to 1) under the pointer, and dragging follows the pointer, even outside the square.</li>
 *     <li>A press or drag on the hue strip sets the hue, from red at the top through yellow, green, cyan, blue and
 *     magenta back to red at the bottom.</li>
 *     <li>A press or drag on the alpha strip, while it {@linkplain #setAlphaShown shows}, sets the alpha, opaque at the
 *     top and clear at the bottom.</li>
 *     <li>The hex field takes {@code RRGGBB} or {@code AARRGGBB}, with or without a leading {@code #}, on Enter or
 *     when it loses focus. Six digits keep the current alpha. Anything else puts the current color back. It shows
 *     {@code #RRGGBB}, or {@code #AARRGGBB} while the alpha strip shows.</li>
 * </ul>
 * <p>The picker is not focusable itself; keyboard users type into the hex field, which is focusable and tabbable. A
 * disabled picker ignores input and draws {@value #STYLE_DISABLED} over its square and strips.</p>
 *
 * <h2>Events</h2>
 * <ul>
 *     <li>{@link #PICK}: Asked before the user changes the color: a press or a drag in the square or on a
 *     strip, or a hex code taken from the field. During a drag it fires for each move that would change the color. The
 *     value is the new color. Cancel the event to keep the current color, or set another color, which then applies as
 *     {@link #setColor} does; the value must not be null. A press, drag or hex code that would leave the color as it
 *     is fires nothing, though a press or drag on the hue strip while the color is gray still moves the hue. Fires only
 *     for the user's actions: {@link #setColor} fires none.</li>
 *     <li>{@link #PICKED}: Told after the color changed, by the user or by {@link #setColor}. The value is the new
 *     color and {@code previous()} the color before.</li>
 * </ul>
 * <p>The picker has its own events because its color is an {@code int}, and a
 * {@link org.inspirenxe.strata.api.control.attribute.value.Ranged} value is a {@code double}.</p>
 *
 * <h2>Theme keys</h2>
 * <ul>
 *     <li>{@value #STYLE_FRAME} (paint, optional): drawn one unit larger than the square, each strip and the swatch,
 *     behind them. Nothing is drawn if it is missing.</li>
 *     <li>{@value #STYLE_MARKER} (paint, optional): the ring in the square and the bars on the strips. Falls back to
 *     white.</li>
 *     <li>{@value #STYLE_MARKER_OUTLINE} (paint, optional): a one-unit line around the markers, so they show on light
 *     and dark colors. Falls back to black.</li>
 *     <li>{@value #STYLE_DISABLED} (paint, optional): drawn over the square and the strips while disabled. Nothing is
 *     drawn if it is missing.</li>
 * </ul>
 * <p>The hex field uses the {@link TextBox} keys. Behind translucent colors, the alpha strip and the swatch draw a
 * gray and white checkerboard.</p>
 *
 * <h2>Size</h2>
 * <p>The square asks to be {@value #DEFAULT_SQUARE_SIZE} units on each side, and each strip is {@value #STRIP_WIDTH}
 * units wide, {@value #SPACING} units to its right. Below them, {@value #SPACING} units down, are a swatch of the
 * color, as tall as the hex field and as wide as it is tall, and the hex field filling the rest of the row. Given more
 * room, the square takes it.</p>
 *
 * <pre>{@code
 * ColorPicker tint = factory.colorPicker()
 *         .setAlphaShown(true)
 *         .setColor(settings.tint())
 *         .onEvent(ColorPicker.PICKED, event -> settings.setTint(event.value()));
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#colorPicker()}. Do not implement this
 * interface.</p>
 */
public interface ColorPicker extends Component<ColorPicker>, HasCursor<ColorPicker>, HasTooltip<ColorPicker> {
    /**
     * Style key for the frame behind the square, the strips and the swatch (paint, optional).
     */
    String STYLE_FRAME = "colorpicker.frame";
    /**
     * Style key for the markers (paint, optional).
     */
    String STYLE_MARKER = "colorpicker.marker";
    /**
     * Style key for the line around the markers (paint, optional).
     */
    String STYLE_MARKER_OUTLINE = "colorpicker.marker.outline";
    /**
     * Style key for the shade over the square and strips when disabled (paint, optional).
     */
    String STYLE_DISABLED = "colorpicker.disabled";
    /**
     * The side the square asks for, in UI units.
     */
    float DEFAULT_SQUARE_SIZE = 96f;
    /**
     * The width of the hue and alpha strips, in UI units.
     */
    float STRIP_WIDTH = 10f;
    /**
     * The space between the square, the strips and the row below them, in UI units.
     */
    float SPACING = 4f;

    /**
     * Asked before the user changes the color. The value is the new ARGB color. A handler may cancel the change or
     * set another color, which applies as {@link #setColor} does; the value must not be set to null. Fires only for the
     * user's actions.
     */
    EventType<ValueRequestEvent<ColorPicker, Integer>> PICK = EventType.valueRequest("color_picker.pick");
    /**
     * Told after the color changed, by the user or from code. The value is the new ARGB color and the previous value
     * the color before.
     */
    EventType<ValueEvent<ColorPicker, Integer>> PICKED = EventType.valueReport("color_picker.picked");

    /**
     * Returns the color.
     *
     * @return the color as {@code 0xAARRGGBB}
     */
    int color();

    /**
     * Sets the color, and the hue, saturation and brightness from it as the class description says. {@link #PICKED}
     * fires if the color differs from the current one; {@link #PICK} does not. Works while disabled too.
     *
     * @param argb the color as {@code 0xAARRGGBB}
     * @return this picker
     */
    ColorPicker setColor(int argb);

    /**
     * Returns the hue the square shows.
     *
     * @return the hue in degrees, from 0 to 360, both of which are red; {@link #setColor} gives less than 360
     */
    float hue();

    /**
     * Returns the saturation the square's marker is at.
     *
     * @return the saturation, from 0 to 1
     */
    float saturation();

    /**
     * Returns the brightness the square's marker is at.
     *
     * @return the brightness, from 0 (black) to 1
     */
    float brightness();

    /**
     * Returns whether the alpha strip shows.
     *
     * @return false by default
     */
    boolean isAlphaShown();

    /**
     * Shows or hides the alpha strip, and switches the hex field between {@code #RRGGBB} and {@code #AARRGGBB}.
     * Hiding the strip keeps the color's alpha; the user's changes then leave it as it is.
     *
     * @param shown true to let the user pick the alpha
     * @return this picker
     */
    ColorPicker setAlphaShown(boolean shown);

    /**
     * A color picker's own state in a {@link org.inspirenxe.strata.api.component.ComponentSnapshot}.
     *
     * @param color the {@linkplain #color() color}, as {@code 0xAARRGGBB}
     * @param hue the {@linkplain #hue() hue}, in degrees
     * @param saturation the {@linkplain #saturation() saturation}, from 0 to 1
     * @param brightness the {@linkplain #brightness() brightness}, from 0 to 1
     * @param alphaShown whether the alpha strip {@linkplain #isAlphaShown() showed}
     */
    record State(int color, float hue, float saturation, float brightness, boolean alphaShown) {
    }
}
