package org.inspirenxe.strata.api.control.field;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.control.attribute.value.Ranged;
import org.inspirenxe.strata.api.control.attribute.value.Stepped;
import org.inspirenxe.strata.api.control.button.Button;

/**
 * A number in a text field, with buttons to step it up and down.
 *
 * <p>The field is a {@link TextBox} and takes its look and editing from it. It accepts only text that could become a
 * number in the range: digits, one decimal point when {@linkplain #decimals() decimals} are allowed, and a leading
 * minus sign when the minimum is negative. Typed text becomes the value on Enter and when the field loses focus; text
 * that is not a number goes back to the current value. Up and Down step by one step while the field has focus, and
 * Page Up and Page Down by ten. The wheel steps while the pointer is over the spinner. The buttons sit at the right
 * edge, {@value #BUTTON_WIDTH} units wide, the upper half stepping up and the lower half down.</p>
 *
 * <p>The field is a private part of the spinner: its {@link TextBox} events reach the spinner but nothing above it,
 * so handlers outside see only {@link #CHANGE} and {@link #CHANGED}.</p>
 *
 * <p>Every value is clamped to the range and rounded to the number of decimals. A new spinner counts whole numbers
 * from 0 to 100 in steps of 1, starting at 0, and asks for {@value #DEFAULT_WIDTH} units of width.</p>
 *
 * <h2>Events</h2>
 * <ul>
 *     <li>{@link Ranged#CHANGE}: Fired when the user is about to change the value: the step buttons, the wheel, the Up,
 *     Down, Page Up and Page Down keys, or typed text taken on Enter or when the field loses focus. A step takes any
 *     typed text first, so one step can fire this twice. The value is the new value, already clamped to the range and
 *     rounded to the decimals. Cancel the event to keep the current value, which the field then shows again, or set
 *     another value; it is clamped and rounded again as {@link #setValue} does, and must not be null or NaN. A change
 *     that would leave the value as it is fires nothing. Fires only for the user's actions: {@link #setValue},
 *     {@link #increment} and {@link #decrement} called from code fire none.</li>
 *     <li>{@link Ranged#CHANGED}: Fired after the value changed: by the user, by {@link #setValue}, {@link #increment}
 *     or {@link #decrement}, or by {@link #setRange} or {@link #setDecimals} moving it. The value is the new value and
 *     {@code previous()} the value before; neither is null. Fires for changes made from code too.</li>
 * </ul>
 *
 * <h2>Theme keys</h2>
 * <p>The field reads the {@link TextBox} keys. The buttons read these, and draw their arrows in the color of
 * {@value Button#STYLE_TEXT}, or {@value Button#STYLE_TEXT_DISABLED} when disabled.</p>
 * <ul>
 *     <li>{@value #STYLE_BUTTON} (paint, optional): each button. Falls back to {@value Button#STYLE_NORMAL}.</li>
 *     <li>{@value #STYLE_BUTTON_HOVERED} (paint, optional): the button under the pointer. Falls back to
 *     {@value Button#STYLE_HOVERED}.</li>
 *     <li>{@value #STYLE_BUTTON_DISABLED} (paint, optional): the buttons when disabled, or a button whose step
 *     would leave the range. Falls back to {@value Button#STYLE_DISABLED}.</li>
 * </ul>
 *
 * <pre>{@code
 * Spinner amount = factory.spinner().setRange(1, 64).setValue(1)
 *         .onEvent(Spinner.CHANGED, event -> this.amount = event.value().intValue());
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#spinner()}. Do not implement this
 * interface.</p>
 */
public interface Spinner extends Component<Spinner>, Stepped<Spinner> {
    /**
     * Style key for each step button (paint, optional).
     */
    String STYLE_BUTTON = "spinner.button";
    /**
     * Style key for the step button under the pointer (paint, optional).
     */
    String STYLE_BUTTON_HOVERED = "spinner.button.hovered";
    /**
     * Style key for a step button that cannot be used (paint, optional).
     */
    String STYLE_BUTTON_DISABLED = "spinner.button.disabled";
    /**
     * The width of the step buttons, in UI units.
     */
    float BUTTON_WIDTH = 12f;
    /**
     * The width a spinner asks for, in UI units.
     */
    float DEFAULT_WIDTH = 60f;

    /**
     * Returns the value.
     *
     * @return the value, within the range
     */
    double value();

    /**
     * Sets the value, clamped to the range and rounded to the number of decimals, and shows it in the field.
     * {@link #CHANGED} fires if the value changed; {@link #CHANGE} does not. Works while disabled too.
     *
     * @param value the new value
     * @return this spinner
     * @throws IllegalArgumentException if {@code value} is NaN
     */
    Spinner setValue(double value);

    /**
     * Returns the lowest value.
     *
     * @return the minimum; 0 by default
     */
    double min();

    /**
     * Returns the highest value.
     *
     * @return the maximum; 100 by default
     */
    double max();

    /**
     * Sets the range. The value is clamped into it, and {@link #CHANGED} fires if that changed it.
     *
     * @param min the lowest value
     * @param max the highest value
     * @return this spinner
     * @throws IllegalArgumentException if either bound is NaN or infinite, or {@code min} is greater than
     *     {@code max}
     */
    Spinner setRange(double min, double max);

    /**
     * Returns how far one step moves the value.
     *
     * @return the step; 1 by default
     */
    double step();

    /**
     * Sets how far one step moves the value.
     *
     * @param step the step
     * @return this spinner
     * @throws IllegalArgumentException if {@code step} is not greater than 0 or is infinite
     */
    Spinner setStep(double step);

    /**
     * Returns how many digits the value keeps after the decimal point.
     *
     * @return the number of decimals; 0 by default
     */
    int decimals();

    /**
     * Sets how many digits the value keeps after the decimal point. The value is rounded again, and
     * {@link #CHANGED} fires if that changed it.
     *
     * @param decimals the number of decimals, from 0 to 9
     * @return this spinner
     * @throws IllegalArgumentException if {@code decimals} is outside 0 to 9
     */
    Spinner setDecimals(int decimals);

    /**
     * Raises the value by one step, as the up button does, without firing {@link #CHANGE}. Does nothing while
     * disabled.
     */
    void increment();

    /**
     * Lowers the value by one step, as the down button does, without firing {@link #CHANGE}. Does nothing while
     * disabled.
     */
    void decrement();

    /**
     * A spinner's own state in a {@link org.inspirenxe.strata.api.component.ComponentSnapshot}.
     *
     * @param value the value
     * @param min the lowest value
     * @param max the highest value
     * @param step how far one step moves the value
     * @param decimals the number of decimals kept
     */
    record State(double value, double min, double max, double step, int decimals) {
    }
}
