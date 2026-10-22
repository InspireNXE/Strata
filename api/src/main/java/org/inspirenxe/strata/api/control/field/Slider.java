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
import org.inspirenxe.strata.api.component.Focusable;
import org.inspirenxe.strata.api.component.HasContextMenu;
import org.inspirenxe.strata.api.component.HasCursor;
import org.inspirenxe.strata.api.component.HasTooltip;
import org.inspirenxe.strata.api.control.attribute.value.Ranged;
import org.inspirenxe.strata.api.control.attribute.value.Stepped;
import org.inspirenxe.strata.api.control.attribute.value.Validatable;
import org.inspirenxe.strata.api.layout.Orientation;

import java.util.function.DoubleFunction;

/**
 * A slider that picks a number from a range by dragging a thumb along a track. It runs across by default; a
 * {@linkplain #setOrientation vertical} slider runs up, with its minimum at the bottom.
 *
 * <h2>Values</h2>
 * <p>The value lies between {@link #min()} and {@link #max()}, 0 and 1 by default. With a {@linkplain #setStep
 * step}, values snap to {@code min + k * step}; the maximum is always reachable even when it is not on that grid.
 * Every way of changing the value clamps and snaps it, and {@link #CHANGED} fires only when the result differs from
 * the current value.</p>
 *
 * <h2>Input</h2>
 * <ul>
 *     <li>A left press anywhere on the slider moves the thumb's center to the pointer. Dragging then follows the
 *     pointer, once it has moved more than 4 units (the drag threshold), even outside the slider.</li>
 *     <li>While focused: Left and Down lower the value by the {@linkplain #keyStep() key step}, Right and Up raise it,
 *     Page Down and Page Up move by ten key steps, and Home and End go to the minimum and maximum. Held keys
 *     repeat. The keys are the same in both orientations, so Up raises a vertical slider and Right raises a
 *     horizontal one.</li>
 * </ul>
 * <p>The slider consumes the presses, drags and keys it acts on. A disabled slider ignores input and draws its
 * disabled look. It does not react to scrolling, so a scroll over a slider reaches its ancestors.</p>
 *
 * <p>It is focusable and tabbable and shows the hand cursor. A horizontal slider wants 150 by 20 units, and its thumb
 * is 8 units wide and as tall as the slider; the thumb's left edge moves from the slider's left edge (at the
 * minimum) to the slider's right edge minus the thumb width (at the maximum). A vertical slider wants 20 by 150
 * units, and its thumb is 8 units tall and as wide as the slider; the thumb's bottom edge moves from the slider's
 * bottom edge (at the minimum) to one thumb height below its top edge (at the maximum). An optional
 * {@linkplain #setLabel label} is drawn centered over the track, in either orientation, to show the setting. It is
 * drawn last, over the thumb, so its text style should stay readable on the thumb as well as the track; the flat
 * theme gives it a shadow.</p>
 *
 * <h2>Events</h2>
 * <ul>
 *     <li>{@link Ranged#CHANGE}: Fired when the user is about to change the value: a press or drag on the slider, or
 *     the arrow, Page Up, Page Down, Home and End keys while it has focus. During a drag it fires for each move that
 *     would change the value. The value is the new value, already clamped to the range and snapped to the step. Cancel
 *     the event to keep the current value, or set another value; it is clamped and snapped again as {@link #setValue}
 *     does, and must not be null or NaN. A press, drag or key that would leave the value as it is fires nothing. Fires
 *     only for the user's actions: {@link #setValue}, {@link #setRange} and {@link #setStep} fire none.</li>
 *     <li>{@link Ranged#CHANGED}: Fired after the value changed: by the user, by {@link #setValue}, or by
 *     {@link #setRange} or {@link #setStep} moving it. The value is the new value and {@code previous()} the value
 *     before; neither is null. During a drag it fires for each move that changes the value.</li>
 * </ul>
 *
 * <h2>Theme keys</h2>
 * <ul>
 *     <li>{@value #STYLE_TRACK} (paint, required): the track, the slider's whole bounds.</li>
 *     <li>{@value #STYLE_TRACK_FOCUSED} (paint, optional): the track while focused. Falls back to
 *     {@value #STYLE_TRACK}.</li>
 *     <li>{@value #STYLE_TRACK_DISABLED} (paint, optional): the track when disabled. Falls back to
 *     {@value #STYLE_TRACK}.</li>
 *     <li>{@value #STYLE_THUMB} (paint, required): the thumb.</li>
 *     <li>{@value #STYLE_THUMB_HOVERED} (paint, required): the thumb while the slider is hovered, focused or
 *     pressed.</li>
 *     <li>{@value #STYLE_THUMB_DISABLED} (paint, optional): the thumb when disabled. Falls back to
 *     {@value #STYLE_THUMB}.</li>
 *     <li>{@value #STYLE_TEXT} (text style, optional): the label. Falls back to the default text style.</li>
 *     <li>{@value #STYLE_TEXT_DISABLED} (text style, optional): the label when disabled. Falls back to
 *     {@value #STYLE_TEXT}.</li>
 *     <li>{@value #STYLE_PADDING} (insets, optional): the space between the slider's edges and its label, which
 *     centers inside it and is cut to it. Defaults to none.</li>
 * </ul>
 *
 * <pre>{@code
 * Slider volume = factory.slider()
 *         .setRange(0, 100)
 *         .setStep(1)
 *         .setValue(settings.volume())
 *         .setLabel(value -> "Volume: " + (int) value + "%")
 *         .onEvent(Slider.CHANGED, event -> settings.setVolume(event.value()));
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#slider()}. Do not implement this
 * interface.</p>
 */
public interface Slider extends Component<Slider>, Focusable<Slider>, HasCursor<Slider>, HasTooltip<Slider>,
        HasContextMenu<Slider>, Stepped<Slider>, Validatable<Slider> {
    /**
     * Style key for the track (paint, required).
     */
    String STYLE_TRACK = "slider.track";
    /**
     * Style key for the track while focused (paint, optional).
     */
    String STYLE_TRACK_FOCUSED = "slider.track.focused";
    /**
     * Style key for the track when disabled (paint, optional).
     */
    String STYLE_TRACK_DISABLED = "slider.track.disabled";
    /**
     * Style key for the track while the slider is enabled and its validation is an error, in place of the normal and
     * focused ones (paint, optional). Without it the slider draws its normal track with a one-unit outline in
     * {@value Validatable#STYLE_ERROR}.
     */
    String STYLE_INVALID = "slider.invalid";
    /**
     * Style key for the thumb (paint, required).
     */
    String STYLE_THUMB = "slider.thumb";
    /**
     * Style key for the thumb while hovered, focused or pressed (paint, required).
     */
    String STYLE_THUMB_HOVERED = "slider.thumb.hovered";
    /**
     * Style key for the thumb when disabled (paint, optional).
     */
    String STYLE_THUMB_DISABLED = "slider.thumb.disabled";
    /**
     * Style key for the label (text style, optional).
     */
    String STYLE_TEXT = "slider.text";
    /**
     * Style key for the label when disabled (text style, optional).
     */
    String STYLE_TEXT_DISABLED = "slider.text.disabled";
    /**
     * Style key for the space between the slider's edges and its label (insets, optional). The label centers in
     * the slider less this padding, and a label too wide is cut at the padding's left and right. Defaults to none.
     */
    String STYLE_PADDING = "slider.padding";
    /**
     * Style key for how fast a label too wide for the slider scrolls (metric, optional), in milliseconds per unit
     * of overflow for one sweep there and back. Defaults to 0, which cuts the label instead.
     */
    String STYLE_TEXT_SCROLL = "slider.text.scroll";
    /**
     * Style key for the shortest sweep of a scrolling label (metric, optional), in milliseconds. Defaults to 0.
     */
    String STYLE_TEXT_SCROLL_MINIMUM = "slider.text.scroll.minimum";
    /**
     * Style key for how far an arrow key moves the thumb of a slider with no step (metric, optional), in units of
     * the thumb's travel along the track. Defaults to 0, which moves the value by a hundredth of the range instead.
     */
    String STYLE_KEY_TRAVEL = "slider.key.travel";

    /**
     * Returns the value.
     *
     * @return the current value, between {@link #min()} and {@link #max()}
     */
    double value();

    /**
     * Sets the value, clamped to the range and snapped to the step. {@link #CHANGED} fires if the result differs
     * from the current value; {@link #CHANGE} does not. Works while disabled too.
     *
     * @param value the new value
     * @return this slider
     * @throws IllegalArgumentException if {@code value} is NaN
     */
    Slider setValue(double value);

    /**
     * Returns the smallest value.
     *
     * @return the minimum; 0 by default
     */
    double min();

    /**
     * Returns the largest value.
     *
     * @return the maximum; 1 by default
     */
    double max();

    /**
     * Sets the range. The current value is clamped and snapped into it, and {@link #CHANGED} fires if that changes
     * it.
     *
     * @param min the smallest value
     * @param max the largest value
     * @return this slider
     * @throws IllegalArgumentException if either bound is not finite, or {@code max} is not greater than
     *                                  {@code min}
     */
    Slider setRange(double min, double max);

    /**
     * Returns the step values snap to.
     *
     * @return the step, or 0 for none; 0 by default
     */
    double step();

    /**
     * Sets the step values snap to, measured from the minimum. The current value is snapped, and {@link #CHANGED}
     * fires if that changes it.
     *
     * @param step the step, or 0 for a continuous slider
     * @return this slider
     * @throws IllegalArgumentException if {@code step} is negative or not finite
     */
    Slider setStep(double step);

    /**
     * Returns how far one press of an arrow key moves the value.
     *
     * @return the step; with no step, the value that moves the thumb {@value #STYLE_KEY_TRAVEL} units when the
     *     theme sets that above 0 and the slider has been laid out longer than its thumb, and otherwise a
     *     hundredth of the range
     */
    double keyStep();

    /**
     * Returns which way the track runs.
     *
     * @return the orientation; {@link Orientation#HORIZONTAL} by default
     */
    Orientation orientation();

    /**
     * Sets which way the track runs. A horizontal slider has its minimum at the left, a vertical one at the bottom.
     * The value is kept, and a new layout runs so the desired size follows.
     *
     * @param orientation the orientation
     * @return this slider
     */
    Slider setOrientation(Orientation orientation);

    /**
     * Sets the text drawn centered over the track, computed from the value. It is recomputed whenever the value
     * changes. The slider's size does not depend on it; text wider than the slider is cut off at its edges.
     *
     * @param label turns the value into text, or {@code null} for no label
     * @return this slider
     */
    Slider setLabel(DoubleFunction<String> label);

    /**
     * Returns the label text for the current value.
     *
     * @return the label, or an empty string when there is no label
     */
    String labelText();

    /**
     * A slider's own state in a {@link org.inspirenxe.strata.api.component.ComponentSnapshot}.
     *
     * @param value the {@linkplain #value() value}
     * @param min the {@linkplain #min() minimum}
     * @param max the {@linkplain #max() maximum}
     * @param step the {@linkplain #step() step}, or 0 for none
     */
    record State(double value, double min, double max, double step) {
    }
}
