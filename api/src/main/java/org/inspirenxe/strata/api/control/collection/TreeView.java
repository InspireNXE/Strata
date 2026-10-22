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
package org.inspirenxe.strata.api.control.collection;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.HasCursor;
import org.inspirenxe.strata.api.component.HasTooltip;
import org.inspirenxe.strata.api.control.attribute.item.Activatable;
import org.inspirenxe.strata.api.control.attribute.item.CellSource;
import org.inspirenxe.strata.api.control.attribute.item.ItemSource;
import org.inspirenxe.strata.api.control.attribute.item.RowView;
import org.inspirenxe.strata.api.control.attribute.item.MultiSelection;
import org.inspirenxe.strata.api.control.attribute.item.Selection;
import org.inspirenxe.strata.api.control.attribute.item.SelectionMode;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.control.ValueEvent;
import org.inspirenxe.strata.api.event.control.ValueRequestEvent;
import org.inspirenxe.strata.api.text.Text;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Items in a hierarchy, each row indented under its parent, with an arrow to show or hide an item's children.
 *
 * <p>The tree asks a children function for an item's children when it needs them, so a large hierarchy costs only
 * what is expanded. The rows shown are the roots and, under each expanded item, its children, depth first. They live
 * in a {@link ListView}, which builds only the rows in view and scrolls, selects and activates the same way.</p>
 *
 * <p>Each row starts {@linkplain #setIndent indented} by its depth, then shows a 12-unit arrow column, then the cell.
 * The arrow points right for a collapsed item and down for an expanded one, and is missing for an item with no
 * children. Clicking the arrow flips the item. Right expands the selected item or, if it is expanded, selects its
 * first child; Left collapses it or, if it is collapsed, selects its parent. Those moves select as Up and Down do, so
 * in {@link SelectionMode#MULTIPLE} mode they ask {@link MultiSelection#SELECT_MANY} and Shift extends the range.
 * Up, Down, Page Up, Page Down, Home, End and Enter work as in a list.</p>
 *
 * <p>Items are told apart by their key, the item itself unless {@linkplain #setKeyFunction set} otherwise, and the
 * tree remembers which keys are expanded across {@link #setItems} calls.</p>
 *
 * <p>The tree fires its own events. The selection and activation events carry row indexes, so read the item with
 * {@code tree.shownItems().get(e.value())}. The row list is private to the tree: its {@link ListView} events never
 * reach anything outside the tree.</p>
 *
 * <h2>Events</h2>
 * <ul>
 *     <li>{@link Selection#SELECT}: Asked before the user changes the selection: a press on a row, the Up, Down,
 *     Page Up, Page Down, Home and End keys while focus is in the tree, Right while an expanded item is selected, which
 *     moves to its first child, or Left while an item that is not expanded is selected, which moves to its parent. It
 *     fires only when the selection would change. The value is the row of {@link #shownItems()} about to be selected.
 *     It is -1 when the user clears the selection of a {@linkplain #setDeselectable deselectable} tree. A handler
 *     may rewrite the value to another row or to -1; any other index outside the rows is ignored. In
 *     {@link SelectionMode#MULTIPLE} mode the user's changes ask {@link MultiSelection#SELECT_MANY} instead, as in a
 *     {@link ListView}. Fires only for the user's actions.</li>
 *     <li>{@link Selection#SELECTED}: Told after the selected row changed. The value is the selected row of
 *     {@link #shownItems()}, or -1 when nothing is selected, and the previous value is the row before. It fires once
 *     per change, also when expanding or collapsing moves the selected item to another row, and when the rows are
 *     worked out again and the item kept selected is no longer equal to the one before, in which case the value can
 *     equal the previous value. Fires for changes made from code too.</li>
 *     <li>{@link Activatable#ACTIVATE}: Asked before the user activates a row: a double press on a row, or Enter while
 *     focus is in the tree and a row is selected. The value is the row of {@link #shownItems()}. A handler may rewrite
 *     it to another row; an index outside the rows is ignored. Fires only for the user's actions.</li>
 *     <li>{@link Activatable#ACTIVATED}: Told after the user activated a row. The value is the row of
 *     {@link #shownItems()}, and the previous value is the same row. The tree has no method that activates a row from
 *     code.</li>
 * </ul>
 *
 * <h2>Theme keys</h2>
 * <p>The rows read the {@link ListView} keys. The arrows read this.</p>
 * <ul>
 *     <li>{@value #STYLE_ARROW} (paint, optional): the expand arrows. Falls back to a light gray.</li>
 * </ul>
 *
 * <pre>{@code
 * TreeView<Category> categories = factory.<Category>treeView(category -> factory.label(category.name()))
 *         .setChildren(Category::subcategories)
 *         .setItems(store.categories());
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#treeView}. Do not implement this
 * interface.</p>
 *
 * @param <T> the type of the items
 */
public interface TreeView<T> extends Component<TreeView<T>>, HasCursor<TreeView<T>>, HasTooltip<TreeView<T>>,
        ItemSource<T, TreeView<T>>, CellSource<T, TreeView<T>>,
        MultiSelection<T, TreeView<T>>, Activatable<TreeView<T>>, RowView<TreeView<T>> {
    /**
     * Style key for the expand arrows (paint, optional).
     */
    String STYLE_ARROW = "tree.arrow";
    /**
     * The width of the arrow column, in UI units.
     */
    float ARROW_WIDTH = 12f;
    /**
     * How far each level is indented by default, in UI units.
     */
    float DEFAULT_INDENT = 10f;
    /**
     * Asked before the user expands or collapses an item: a click on its arrow, or, while focus is in the tree, Right
     * on the selected item when it has children and is collapsed, or Left on the selected item when it is expanded.
     * The press that starts a click on the arrow also selects the row, through {@link #SELECT}. The value names the
     * item and whether it is about to be expanded. A handler may rewrite it, even to name another item, which must
     * be an item of this tree; the tree then applies it as {@link #setExpanded} does, and ignores a value whose item
     * is null. Fires only for the user's actions.
     */
    EventType<ValueRequestEvent<TreeView<?>, Expansion<?>>> EXPAND = EventType.valueRequest("tree_view.expand");
    /**
     * Told after an item was expanded or collapsed. The value names the item and its new state, and the previous
     * value the same item and its old state. {@link #expandAll()} and {@link #collapseAll()} fire it once for each
     * item that changed, and {@link #selectItem} once for each ancestor it expanded. {@link #collapseAll()} reports
     * only items it can still reach from the roots; it forgets other expanded keys without an event. Fires for
     * changes made from code too.
     */
    EventType<ValueEvent<TreeView<?>, Expansion<?>>> EXPANDED = EventType.valueReport("tree_view.expanded");

    /**
     * Returns the top-level items, the roots.
     *
     * @return an unmodifiable list of the roots
     */
    @Override
    List<T> items();

    /**
     * Replaces the top-level items, the roots. Expanded keys and the selection carry over to items with the same key,
     * as {@link ItemSource} describes.
     *
     * @param items the new roots, copied
     * @return this tree
     * @throws NullPointerException if a root is null
     */
    @Override
    TreeView<T> setItems(List<? extends T> items);

    /**
     * Sets how the tree finds an item's children. The rows are worked out again.
     *
     * @param children returns an item's children, in order; an empty list for a leaf. By default every item is a
     *     leaf. An item that would show under itself, because the children lead back to one of its ancestors, shows
     *     there as a leaf.
     * @return this tree
     */
    TreeView<T> setChildren(Function<? super T, ? extends List<? extends T>> children);

    /**
     * Replaces the cell factory. Every row is built again.
     *
     * @param cellFactory makes the component for an item's row, after the indent and arrow
     * @return this tree
     */
    TreeView<T> setCellFactory(Function<? super T, ? extends Component<?>> cellFactory);

    /**
     * Works out the rows again and builds them, for when the children of shown items changed.
     *
     * @return this tree
     */
    TreeView<T> refresh();

    /**
     * Returns whether an item is expanded.
     *
     * @param item the item
     * @return true if its key is expanded, whether or not the item is shown
     */
    boolean isExpanded(T item);

    /**
     * Expands or collapses an item. In {@link SelectionMode#SINGLE} mode, collapsing an item that holds the
     * selection moves the selection to it; in {@link SelectionMode#MULTIPLE} mode the rows it hides leave the
     * selection. {@link #EXPANDED} fires if the item changed.
     *
     * @param item the item
     * @param expanded true to show its children
     * @return this tree
     */
    TreeView<T> setExpanded(T item, boolean expanded);

    /**
     * Expands every item, asking the children function for the whole hierarchy.
     *
     * @return this tree
     */
    TreeView<T> expandAll();

    /**
     * Collapses every item, leaving only the roots shown. In {@link SelectionMode#SINGLE} mode a selection below the
     * roots moves to its root; in {@link SelectionMode#MULTIPLE} mode the hidden rows leave the selection.
     *
     * @return this tree
     */
    TreeView<T> collapseAll();

    /**
     * Returns the items shown, in order.
     *
     * @return an unmodifiable list of the shown items
     */
    List<T> shownItems();

    /**
     * Returns the depth of a shown item.
     *
     * @param index the item's index in {@link #shownItems()}
     * @return 0 for a root, 1 for its children, and so on
     * @throws IndexOutOfBoundsException if there is no such row
     */
    int depth(int index);

    /**
     * Returns how far each level is indented.
     *
     * @return the indent in UI units
     */
    float indent();

    /**
     * Sets how far each level is indented.
     *
     * @param indent the indent in UI units
     * @return this tree
     * @throws IllegalArgumentException if {@code indent} is negative or not finite
     */
    TreeView<T> setIndent(float indent);

    /**
     * Sets the text shown while there are no roots.
     *
     * @param placeholder the text, or empty for none
     * @return this tree
     */
    TreeView<T> setPlaceholder(Text placeholder);

    /**
     * Returns the selected row.
     *
     * @return the selected row of {@link #shownItems()}, or -1 when nothing is selected
     */
    @Override
    int selectedIndex();

    /**
     * Returns the selected item.
     *
     * @return the selected item, or empty when nothing is selected
     */
    @Override
    Optional<T> selected();

    /**
     * Selects a shown row and scrolls it into view.
     *
     * @param index the row of {@link #shownItems()}, or -1 to select nothing
     * @return this tree
     * @throws IndexOutOfBoundsException if the index is not -1 and not a shown row
     */
    @Override
    TreeView<T> select(int index);

    /**
     * Selects an item, expanding its ancestors so it shows, and scrolls it into view. The tree searches the
     * hierarchy from the roots for an item with the same key; nothing is selected if none is found.
     *
     * @param item the item, or null to select nothing
     * @return this tree
     */
    @Override
    TreeView<T> selectItem(T item);

    /**
     * An item and whether it is expanded.
     *
     * @param <T> the type of the items
     * @param item the item
     * @param expanded true if its children show
     */
    record Expansion<T>(T item, boolean expanded) {
    }
}
