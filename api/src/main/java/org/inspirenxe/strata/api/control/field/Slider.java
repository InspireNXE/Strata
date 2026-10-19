package org.inspirenxe.strata.api.control.field;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.ValueEvent;
import org.inspirenxe.strata.api.event.ValueRequestEvent;

import java.util.function.DoubleFunction;

/**
 * A horizontal slider that picks a number from a range by dragging a thumb along a track.
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
 *     repeat.</li>
 * </ul>
 * <p>The slider consumes the presses, drags and keys it acts on. A disabled slider ignores input and draws its
 * disabled look. It does not react to scrolling, so a scroll over a slider reaches its ancestors.</p>
 *
 * <p>It is focusable and tabbable and shows the hand cursor. Its desired size is 150 by 20 units. The thumb is 8 units
 * wide and as tall as the slider; its left edge moves from the slider's left edge (at the minimum) to the slider's
 * right edge minus the thumb width (at the maximum). An optional {@linkplain #setLabel label} is drawn centered over
 * the track to show the setting.</p>
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
public interface Slider extends Component<Slider> {
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
     * Fired when the user is about to change the value: a press or drag on the slider, or the arrow, Page Up, Page
     * Down, Home and End keys while it has focus. During a drag it fires for each move that would change the value.
     * The value is the new value, already clamped to the range and snapped to the step. Cancel the event to keep the
     * current value, or set another value; it is clamped and snapped again as {@link #setValue} does, and must not be
     * null or NaN. A press, drag or key that would leave the value as it is fires nothing. Fires only for the user's
     * actions: {@link #setValue}, {@link #setRange} and {@link #setStep} fire none.
     */
    EventType<ValueRequestEvent<Slider, Double>> CHANGE = EventType.valueRequest("slider.change");
    /**
     * Fired after the value changed: by the user, by {@link #setValue}, or by {@link #setRange} or {@link #setStep}
     * moving it. The value is the new value and {@code previous()} the value before; neither is null. During a drag
     * it fires for each move that changes the value. Fires for changes made from code too.
     */
    EventType<ValueEvent<Slider, Double>> CHANGED = EventType.valueReport("slider.changed");

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
     * @return the step, or a hundredth of the range when there is no step
     */
    double keyStep();

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
