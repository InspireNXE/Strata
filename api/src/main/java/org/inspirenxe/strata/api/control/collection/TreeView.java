package org.inspirenxe.strata.api.control.collection;

import org.inspirenxe.strata.api.component.Component;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
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
 * first child; Left collapses it or, if it is collapsed, selects its parent. Up, Down, Page Up, Page Down, Home, End
 * and Enter work as in a list.</p>
 *
 * <p>Items are told apart by their key, the item itself unless {@linkplain #setKeyFunction set} otherwise, and the
 * tree remembers which keys are expanded across {@link #setRoots} calls.</p>
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
 *         .setRoots(store.categories());
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#treeView}. Do not implement this
 * interface.</p>
 *
 * @param <T> the type of the items
 */
public interface TreeView<T> extends Component<TreeView<T>> {
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
     * Returns the top-level items.
     *
     * @return an unmodifiable list of the roots
     */
    List<T> roots();

    /**
     * Replaces the top-level items. Expanded keys and the selection carry over to items with the same key.
     *
     * @param roots the new roots, copied
     * @return this tree
     */
    TreeView<T> setRoots(List<? extends T> roots);

    /**
     * Sets how the tree finds an item's children. The rows are worked out again.
     *
     * @param children returns an item's children, in order; an empty list for a leaf. By default every item is a
     *     leaf.
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
     * Sets what identifies an item.
     *
     * @param keyFunction returns an item's key, compared with {@link Object#equals}
     * @return this tree
     */
    TreeView<T> setKeyFunction(Function<? super T, ?> keyFunction);

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
     * Expands or collapses an item. Collapsing an item that holds the selection moves the selection to it.
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
     * Collapses every item, leaving only the roots shown. A selection below the roots moves to its root.
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
     * @throws IllegalArgumentException if {@code indent} is negative or infinite
     */
    TreeView<T> setIndent(float indent);

    /**
     * Sets the height of every row.
     *
     * @param height the row height in UI units
     * @return this tree
     * @throws IllegalArgumentException if {@code height} is not greater than 0 or is infinite
     */
    TreeView<T> setRowHeight(float height);

    /**
     * Sets the text shown while there are no roots.
     *
     * @param placeholder the text, or null for none
     * @return this tree
     */
    TreeView<T> setPlaceholder(String placeholder);

    /**
     * Returns the selected item.
     *
     * @return the selected item, or empty when nothing is selected
     */
    Optional<T> selected();

    /**
     * Selects an item, expanding its ancestors so it shows, and scrolls it into view. The tree searches the
     * hierarchy from the roots for an item with the same key; nothing is selected if none is found.
     *
     * @param item the item, or null to select nothing
     * @return this tree
     */
    TreeView<T> select(T item);

    /**
     * Adds a listener told the selected item after every change.
     *
     * @param listener receives the selected item, or empty when nothing is selected
     * @return this tree
     */
    TreeView<T> onSelectionChanged(Consumer<Optional<T>> listener);

    /**
     * Adds a listener told when a row is activated, by a double click or Enter.
     *
     * @param listener receives the activated item
     * @return this tree
     */
    TreeView<T> onActivate(Consumer<T> listener);
}
