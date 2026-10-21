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

import org.inspirenxe.strata.api.state.PropertyKey;

import java.util.List;
import java.util.function.Function;

/**
 * A control that shows a list of items, such as a list, a table, a tree, a combo box or a grid.
 *
 * <p>The control keeps a copy of the list it is given. Change what it shows with {@link #setItems}, or with
 * {@link #refresh} when the items themselves changed inside.</p>
 *
 * <p>Do not implement this interface.</p>
 *
 * @param <T> the type of the items
 * @param <S> the control's own type, which the setters return
 */
public interface ItemSource<T, S extends ItemSource<T, S>> {

    /**
     * Returns the key for the items, as {@link #items()} and {@link #setItems} say. Binds one way: a change to the
     * bound list replaces the items as a whole with {@code setItems}, which keeps the selection by
     * {@linkplain #setKeyFunction key}. A list equal to the items the control holds is not set again. It is a
     * method because a constant cannot carry the item type, and is not named {@code items} because that is the
     * getter's name. Every call returns a key with the same name.
     *
     * <pre>{@code
     * offers.bind(ItemSource.itemsKey(), market.offers);       // a State<List<Offer>>
     * }</pre>
     *
     * @param <T> the type of the items
     * @return the key
     */
    static <T> PropertyKey<ItemSource<T, ?>, List<T>> itemsKey() {
        return PropertyKey.of("item_source.items", ItemSource::items, ItemSource::setItems);
    }

    /**
     * Returns the items. For a tree these are its roots.
     *
     * @return an unmodifiable copy of the items
     */
    List<T> items();

    /**
     * Replaces the items. A control with a {@linkplain Selection selection} keeps it as the control describes,
     * usually on the item with the selected item's {@linkplain #setKeyFunction key}.
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
     * {@link Selection#selectItem} finds an item. By default an item is its own key, compared with
     * {@code equals}.
     *
     * @param keyFunction returns the key of an item; keys are compared with {@code equals}
     * @return this control
     */
    S setKeyFunction(Function<? super T, ?> keyFunction);
}
