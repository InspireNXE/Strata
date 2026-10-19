package org.inspirenxe.strata.api.control.attribute.item;

import java.util.List;
import java.util.function.Function;

/**
 * A control that shows a list of items, such as a list, a table, a tree, a combo box or a grid.
 *
 * <p>The control keeps a copy of the list it is given. Change what it shows with {@link #setItems}, or with
 * {@link #refresh} when the items themselves changed inside.</p>
 *
 * <p>Only Strata implements this interface.</p>
 *
 * @param <T> the type of the items
 * @param <S> the control's own type, which the setters return
 */
public interface ItemSource<T, S extends ItemSource<T, S>> {

    /**
     * Returns the items. For a tree these are its roots.
     *
     * @return an unmodifiable copy of the items
     */
    List<T> items();

    /**
     * Replaces the items. A control with a {@linkplain SingleSelection selection} keeps it on the item with the
     * selected item's {@linkplain #setKeyFunction key}, or clears it when no item has that key.
     *
     * @param items the items; the control keeps a copy
     * @return this control
     */
    S setItems(List<? extends T> items);

    /**
     * Shows the same items again, for when they changed inside.
     *
     * @return this control
     */
    S refresh();

    /**
     * Sets how items are told apart when {@link #setItems} keeps the selection, and when
     * {@link SingleSelection#selectItem} finds an item. By default an item is its own key, compared with
     * {@code equals}.
     *
     * @param keyFunction returns the key of an item; keys are compared with {@code equals}
     * @return this control
     */
    S setKeyFunction(Function<? super T, ?> keyFunction);
}
