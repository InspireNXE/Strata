package org.inspirenxe.strata.api.control.attribute.value;

/**
 * A {@link Ranged} control whose value moves in steps, such as a slider or a spinner.
 *
 * <p>Only Strata implements this interface.</p>
 *
 * @param <S> the control's own type, which the setters return
 */
public interface Stepped<S extends Stepped<S>> extends Ranged<S> {

    /**
     * Returns the step.
     *
     * @return the step, or 0 when the control allows none and the value may take any number in the range
     */
    double step();

    /**
     * Sets the step. The control rounds the value to it again.
     *
     * @param step the step, or 0 for none where the control allows that
     * @return this control
     * @throws IllegalArgumentException if the step is negative or not finite, or 0 where the control needs a step
     */
    S setStep(double step);
}
