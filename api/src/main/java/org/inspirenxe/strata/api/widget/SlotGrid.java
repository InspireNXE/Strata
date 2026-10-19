package org.inspirenxe.strata.api.widget;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.Tooltip;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A grid of square slots that each hold an item or nothing, such as an inventory or a hotbar.
 *
 * <p>There is one slot per entry in the items, null entries being empty slots, laid out left to right in rows of
 * {@linkplain #setColumns a set number of columns}. Each filled slot shows the component its cell factory makes for
 * the item, inset by one unit, and the tooltip its tooltip function gives. Slots are rebuilt when the items, the
 * cell factory or the tooltip function change.</p>
 *
 * <p>Clicking a slot with any button tells the click listeners, empty slots included, and consumes the click. A
 * selectable grid also selects the slot clicked with the left button. The grid is not focusable. The desired size is
 * the columns and rows of slots with the spacing between them.</p>
 *
 * <h2>Theme keys</h2>
 * <ul>
 *     <li>{@value #STYLE_SLOT} (paint, required): each slot, behind its content.</li>
 *     <li>{@value #STYLE_SLOT_HOVERED} (paint, optional): drawn over the slot under the pointer, in front of its
 *     content. Nothing if missing.</li>
 *     <li>{@value #STYLE_SLOT_SELECTED} (paint, optional): drawn over the selected slot, in front of its content.
 *     Falls back to {@value #STYLE_SLOT_HOVERED}.</li>
 * </ul>
 *
 * <pre>{@code
 * SlotGrid<Item> inventory = factory.<Item>slotGrid(item -> factory.image(item.icon()))
 *         .setColumns(9)
 *         .setTooltips(item -> Tooltip.text(item.name()))
 *         .setItems(player.inventory())
 *         .onSlotClicked(click -> this.take(click.index(), click.button()));
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#slotGrid}. Do not implement this
 * interface.</p>
 *
 * @param <T> the type of the items
 */
public interface SlotGrid<T> extends Component<SlotGrid<T>> {
    /**
     * Style key for each slot (paint, required).
     */
    String STYLE_SLOT = "slot.normal";
    /**
     * Style key drawn over the slot under the pointer (paint, optional).
     */
    String STYLE_SLOT_HOVERED = "slot.hovered";
    /**
     * Style key drawn over the selected slot (paint, optional).
     */
    String STYLE_SLOT_SELECTED = "slot.selected";
    /**
     * The size of a slot unless {@linkplain #setSlotSize set} otherwise, in UI units.
     */
    float DEFAULT_SLOT_SIZE = 18f;
    /**
     * The number of columns unless {@linkplain #setColumns set} otherwise.
     */
    int DEFAULT_COLUMNS = 9;

    /**
     * Returns the items, one per slot.
     *
     * @return an unmodifiable list of the items, null for an empty slot
     */
    List<T> items();

    /**
     * Replaces the items. The selection is kept if its index still exists, and cleared otherwise.
     *
     * @param items the items, one per slot, copied; null entries are empty slots
     * @return this grid
     */
    SlotGrid<T> setItems(List<? extends T> items);

    /**
     * Returns the item in a slot.
     *
     * @param index the slot index
     * @return the item, or null for an empty slot
     * @throws IndexOutOfBoundsException if there is no such slot
     */
    T item(int index);

    /**
     * Replaces the item in one slot and rebuilds only that slot.
     *
     * @param index the slot index
     * @param item the item, or null to empty the slot
     * @return this grid
     * @throws IndexOutOfBoundsException if there is no such slot
     */
    SlotGrid<T> setItem(int index, T item);

    /**
     * Returns how many slots there are.
     *
     * @return the number of items, empty slots included
     */
    int slotCount();

    /**
     * Replaces the cell factory and rebuilds the slots.
     *
     * @param cellFactory makes the component shown in a filled slot
     * @return this grid
     */
    SlotGrid<T> setCellFactory(Function<? super T, ? extends Component<?>> cellFactory);

    /**
     * Sets the tooltip of filled slots and rebuilds the slots.
     *
     * @param tooltips returns an item's tooltip, or null for none; a null function means no tooltips
     * @return this grid
     */
    SlotGrid<T> setTooltips(Function<? super T, Tooltip> tooltips);

    /**
     * Returns the number of columns.
     *
     * @return the columns; {@value #DEFAULT_COLUMNS} by default
     */
    int columns();

    /**
     * Sets the number of columns. A new layout runs.
     *
     * @param columns the number of slots per row
     * @return this grid
     * @throws IllegalArgumentException if {@code columns} is less than 1
     */
    SlotGrid<T> setColumns(int columns);

    /**
     * Returns the size of each slot.
     *
     * @return the slot size in UI units
     */
    float slotSize();

    /**
     * Sets the size of each slot. A new layout runs.
     *
     * @param size the width and height of a slot in UI units
     * @return this grid
     * @throws IllegalArgumentException if {@code size} is not greater than 0 or is infinite
     */
    SlotGrid<T> setSlotSize(float size);

    /**
     * Returns the space between slots.
     *
     * @return the spacing in UI units; 0 by default
     */
    float spacing();

    /**
     * Sets the space between slots, across and down. A new layout runs.
     *
     * @param spacing the spacing in UI units
     * @return this grid
     * @throws IllegalArgumentException if {@code spacing} is negative or infinite
     */
    SlotGrid<T> setSpacing(float spacing);

    /**
     * Returns whether a left click selects a slot.
     *
     * @return true if selectable; false by default
     */
    boolean isSelectable();

    /**
     * Makes left clicks select slots, or not.
     *
     * @param selectable true to select the slot clicked
     * @return this grid
     */
    SlotGrid<T> setSelectable(boolean selectable);

    /**
     * Returns the selected slot.
     *
     * @return the slot index, or -1 when none is selected
     */
    int selectedIndex();

    /**
     * Selects a slot, whether or not the grid is selectable. Listeners run if the selection changed.
     *
     * @param index the slot index, or -1 to select none
     * @return this grid
     * @throws IndexOutOfBoundsException if {@code index} is neither -1 nor a slot index
     */
    SlotGrid<T> select(int index);

    /**
     * Returns the slot under the pointer.
     *
     * @return the slot index, or -1 when the pointer is over no slot
     */
    int hoveredIndex();

    /**
     * Adds a listener told about every click on a slot.
     *
     * @param listener receives the click
     * @return this grid
     */
    SlotGrid<T> onSlotClicked(Consumer<Click<T>> listener);

    /**
     * Adds a listener told the selected slot after every change.
     *
     * @param listener receives the slot index, or -1 when none is selected
     * @return this grid
     */
    SlotGrid<T> onSelectionChanged(Consumer<Integer> listener);

    /**
     * A click on a slot.
     *
     * @param <T> the type of the items
     * @param index the slot index
     * @param item the item in the slot, or null for an empty slot
     * @param button the button clicked
     * @param modifiers the modifiers held; never null
     */
    record Click<T>(int index, T item, MouseButton button, Modifiers modifiers) {

        /**
         * Checks the button and modifiers.
         */
        public Click {
            Objects.requireNonNull(button, "button");
            Objects.requireNonNull(modifiers, "modifiers");
        }
    }
}
