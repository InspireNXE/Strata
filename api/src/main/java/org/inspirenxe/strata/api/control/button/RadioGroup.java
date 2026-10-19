package org.inspirenxe.strata.api.control.button;

import org.inspirenxe.strata.api.control.attribute.item.SingleSelection;
import org.inspirenxe.strata.api.control.attribute.value.Toggle;
import org.inspirenxe.strata.api.event.EventTarget;

import java.util.List;
import java.util.Optional;

/**
 * A set of {@link RadioButton}s of which at most one is selected. Each button stands for a value, and the group
 * reports the value of the selected button. The group is not a component: place its buttons anywhere in the tree,
 * in any layout.
 *
 * <p>A new group has no buttons and nothing selected. Selecting a button deselects the one before it. Up and Left
 * move the selection and focus to the previous enabled, visible button in the group, and Down and Right to the next
 * one, wrapping at the ends.</p>
 *
 * <p>The group is an {@link EventTarget} of its own for {@link SingleSelection#SELECTED}, which carries the index
 * of the selected button in {@link #buttons()}. The event's {@linkplain org.inspirenxe.strata.api.event.Event#target()
 * target} is the newly selected button, or the deselected one when the selection was cleared. Its events reach only
 * the handlers registered on the group, and do not bubble through the tree its buttons are in. The group never fires
 * {@link SingleSelection#SELECT}; to refuse a selection, handle {@link Toggle#TOGGLE} on the buttons or on a
 * container around them.</p>
 *
 * <pre>{@code
 * RadioGroup<Difficulty> difficulty = factory.radioGroup();
 * column.add(difficulty.add("Easy", Difficulty.EASY))
 *         .add(difficulty.add("Hard", Difficulty.HARD));
 * difficulty.selectItem(settings.difficulty())
 *         .onEvent(SingleSelection.SELECTED, event -> difficulty.selected().ifPresent(settings::setDifficulty));
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#radioGroup()}. Do not implement this
 * interface.</p>
 *
 * @param <T> the type of the values the buttons stand for
 */
public interface RadioGroup<T> extends SingleSelection<T, RadioGroup<T>> {
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
     * Returns the index of the selected button.
     *
     * @return the selected button's index in {@link #buttons()}, or -1 if none is
     */
    @Override
    int selectedIndex();

    /**
     * Returns the value of the selected button.
     *
     * @return the selected value, or empty if no button is selected or the selected button stands for null
     */
    @Override
    Optional<T> selected();

    /**
     * Selects the button at an index, or deselects every button for -1. Fires {@link Toggle#TOGGLED} on each button
     * whose state changed, then {@link SingleSelection#SELECTED} if the selection changed. Works on disabled
     * buttons too.
     *
     * @param index the button's index in {@link #buttons()}, or -1
     * @return this group
     * @throws IndexOutOfBoundsException if the index is not -1 and not a button's index
     */
    @Override
    RadioGroup<T> select(int index);

    /**
     * Selects the first button that stands for a value, compared with {@link Object#equals}, or deselects every
     * button if none does. Fires the events {@link #select(int)} does. Works on disabled buttons too.
     *
     * @param value the value to select; may be null
     * @return this group
     */
    @Override
    RadioGroup<T> selectItem(T value);
}
