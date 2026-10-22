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
package org.inspirenxe.strata.api.control.attribute.item;

import org.inspirenxe.strata.api.text.Text;

/**
 * A control that shows its entries as scrolling rows of one height, such as a list, a table or a tree.
 *
 * <p>Do not implement this interface.</p>
 *
 * @param <S> the control's own type, which the setters return
 */
public interface RowView<S extends RowView<S>> {
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
     * Returns the text shown in the middle of the rows while there are none.
     *
     * @return the placeholder; empty for none, which is the default
     */
    Text placeholder();

    /**
     * Sets the text shown in the middle of the rows while there are none.
     *
     * @param placeholder the text, or {@code null} or empty for none
     * @return this control
     */
    S setPlaceholder(Text placeholder);

    /**
     * Does the same as {@link #setPlaceholder(Text)} with literal text.
     *
     * @param placeholder the text, or {@code null} or empty for none
     * @return this control
     */
    default S setPlaceholder(final String placeholder) {
        return this.setPlaceholder(placeholder == null ? null : Text.of(placeholder));
    }

    /**
     * Returns the vertical offset. It reflects {@link #setScrollY} at once. A {@link #scrollToIndex}, a scroll from
     * the user and a change of the rows or the control's size show in it after the next layout, which also clamps
     * it to the rows.
     *
     * @return how far the rows are scrolled down, in UI units
     */
    float scrollY();

    /**
     * Scrolls to an offset. The offset is at least 0, and once the control has been laid out it is at most what the
     * current rows need, so an offset set right after the items change, or before the first layout, is kept. The
     * next layout clamps it to the rows in the control's size.
     *
     * @param scrollY the vertical offset
     * @return this control
     * @throws IllegalArgumentException if the offset is NaN
     */
    S setScrollY(float scrollY);

    /**
     * Scrolls the least distance that shows a row. The scroll happens at the next layout, once the control knows its
     * size; {@link #scrollY()} reports the new offset after it.
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
