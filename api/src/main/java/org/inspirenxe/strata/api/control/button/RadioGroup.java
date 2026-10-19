package org.inspirenxe.strata.api.control.button;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * A set of {@link RadioButton}s of which at most one is selected. Each button stands for a value, and the group
 * reports the value of the selected button. The group is not a component: place its buttons anywhere in the tree,
 * in any layout.
 *
 * <p>A new group has no buttons and nothing selected. Selecting a button deselects the one before it. Up and Left
 * move the selection and focus to the previous enabled, visible button in the group, and Down and Right to the next
 * one, wrapping at the ends.</p>
 *
 * <pre>{@code
 * RadioGroup<Difficulty> difficulty = factory.radioGroup();
 * column.add(difficulty.add("Easy", Difficulty.EASY))
 *         .add(difficulty.add("Hard", Difficulty.HARD));
 * difficulty.select(settings.difficulty()).onSelectionChanged(value -> value.ifPresent(settings::setDifficulty));
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#radioGroup()}. Do not implement this
 * interface.</p>
 *
 * @param <T> the type of the values the buttons stand for
 */
public interface RadioGroup<T> {

    /**
     * Makes a new, unselected button in this group. The button is not added to any container.
     *
     * @param text the button's caption
     * @param value the value the button stands for; may be null
     * @return the new button
     */
    RadioButton add(String text, T value);

    /**
     * Returns the group's buttons in the order they were made.
     *
     * @return an unmodifiable list of the buttons
     */
    List<RadioButton> buttons();

    /**
     * Returns the value of the button at an index.
     *
     * @param index the button's index in {@link #buttons()}
     * @return the value, which may be null
     * @throws IndexOutOfBoundsException if there is no button at {@code index}
     */
    T value(int index);

    /**
     * Returns the selected button.
     *
     * @return the selected button, or empty if none is
     */
    Optional<RadioButton> selectedButton();

    /**
     * Returns the value of the selected button.
     *
     * @return the selected value, or empty if no button is selected or the selected button stands for null
     */
    Optional<T> selected();

    /**
     * Selects the first button that stands for a value, compared with {@link Object#equals}. Listeners run if the
     * selection changed. Works on disabled buttons too.
     *
     * @param value the value to select; may be null
     * @return this group
     * @throws IllegalArgumentException if no button stands for {@code value}
     */
    RadioGroup<T> select(T value);

    /**
     * Deselects every button. Listeners run if a button was selected.
     *
     * @return this group
     */
    RadioGroup<T> clearSelection();

    /**
     * Adds a listener told the selected value after every change, by input or by code. Listeners run in the order
     * they were added.
     *
     * @param listener receives the new value, or empty when nothing is selected or the button stands for null
     * @return this group
     */
    RadioGroup<T> onSelectionChanged(Consumer<Optional<T>> listener);
}
