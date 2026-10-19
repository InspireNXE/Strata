package org.inspirenxe.strata.api.control.attribute.item;

import org.inspirenxe.strata.api.event.EventTarget;

/**
 * A control that shows its entries as scrolling rows of one height, such as a list, a table or a tree.
 *
 * <p>Only Strata implements this interface.</p>
 *
 * @param <S> the control's own type, which the setters return
 */
public interface RowView<S extends RowView<S>> extends EventTarget<S> {
    /**
     * The row height of a new control, in UI units.
     */
    float DEFAULT_ROW_HEIGHT = 20f;

    /**
     * Returns the row height.
     *
     * @return the height of every row; {@value #DEFAULT_ROW_HEIGHT} by default
     */
    float rowHeight();

    /**
     * Sets the height of every row.
     *
     * @param height the height in UI units
     * @return this control
     * @throws IllegalArgumentException if the height is not greater than 0 or not finite
     */
    S setRowHeight(float height);

    /**
     * Returns the row spacing.
     *
     * @return the space between rows; 0 by default
     */
    float rowSpacing();

    /**
     * Sets the space between rows. No background is drawn in it.
     *
     * @param spacing the space in UI units
     * @return this control
     * @throws IllegalArgumentException if the spacing is negative or not finite
     */
    S setRowSpacing(float spacing);

    /**
     * Sets the text shown in the middle of the rows while there are none.
     *
     * @param placeholder the text, or {@code null} for none
     * @return this control
     */
    S setPlaceholder(String placeholder);

    /**
     * Returns the vertical offset.
     *
     * @return how far the rows are scrolled down, in UI units
     */
    float scrollY();

    /**
     * Scrolls to an offset, clamped to the rows.
     *
     * @param scrollY the vertical offset
     * @return this control
     */
    S setScrollY(float scrollY);

    /**
     * Scrolls the least distance that shows a row.
     *
     * @param index the row's index, as for {@link SingleSelection}
     * @return this control
     * @throws IndexOutOfBoundsException if the index is not a row's index
     */
    S scrollToIndex(int index);

    /**
     * Returns whether a press on the selected row deselects it.
     *
     * @return false by default
     */
    boolean isDeselectable();

    /**
     * Sets whether a press on the selected row deselects it.
     *
     * @param deselectable true to allow deselecting by pressing
     * @return this control
     */
    S setDeselectable(boolean deselectable);
}
