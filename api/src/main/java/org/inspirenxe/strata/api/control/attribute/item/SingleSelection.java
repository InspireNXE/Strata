package org.inspirenxe.strata.api.control.attribute.item;

import org.inspirenxe.strata.api.event.EventTarget;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.ValueEvent;
import org.inspirenxe.strata.api.event.ValueRequestEvent;

import java.util.Optional;

/**
 * A control in which at most one entry is selected, such as a list, a table, a tree, a combo box, a grid, a tab bar,
 * a menu bar or a radio group.
 *
 * <p>Indexes are into what the control shows: its items for a list, a combo box or a grid, the
 * {@linkplain org.inspirenxe.strata.api.control.collection.TableView#shownItems() sorted rows} of a table, the
 * {@linkplain org.inspirenxe.strata.api.control.collection.TreeView#shownItems() visible rows} of a tree, the tabs
 * of a tab bar, the menus of a menu bar and the buttons of a radio group. -1 means nothing is selected.
 * {@link #selected()} returns the entry at the selected index, as each control describes.</p>
 *
 * <p>Handle {@link #SELECT} to refuse or change what the user picks, and {@link #SELECTED} to react to every
 * change. Both carry indexes. One handler on a form hears every selection in it:</p>
 *
 * <pre>{@code
 * form.onEvent(SingleSelection.SELECTED, event -> updateButtons());
 * }</pre>
 *
 * <p>Only Strata implements this interface.</p>
 *
 * @param <T> the type of the entries
 * @param <S> the control's own type, which the setters return
 */
public interface SingleSelection<T, S extends SingleSelection<T, S>> extends EventTarget<S> {
    /**
     * Asked before the user changes the selection. The value is the index about to be selected, or -1 when the user
     * clears the selection. A handler may rewrite it to another index or to -1; the control ignores an index it does
     * not show. Fires only for the user's actions, and only when the selection would change. A control the user
     * selects in only through its parts, such as a radio group, does not fire it; its parts fire their own requests.
     */
    EventType<ValueRequestEvent<SingleSelection<?, ?>, Integer>> SELECT = EventType.valueRequest("selection.select");
    /**
     * Told after the selection changed. The value is the selected index, or -1, and the previous value the index
     * before. Fires for changes made from code too.
     */
    EventType<ValueEvent<SingleSelection<?, ?>, Integer>> SELECTED = EventType.valueReport("selection.selected");

    /**
     * Returns the selected index.
     *
     * @return the index of the selected entry, or -1 when nothing is selected
     */
    int selectedIndex();

    /**
     * Returns the selected entry.
     *
     * @return the entry at {@link #selectedIndex()}, or empty when nothing is selected or the entry is null
     */
    Optional<T> selected();

    /**
     * Selects the entry at an index. Fires {@link #SELECTED} if the selection changed, and never {@link #SELECT}.
     *
     * @param index the index, or -1 to clear the selection
     * @return this control
     * @throws IndexOutOfBoundsException if the index is not -1 and not an entry's index
     */
    S select(int index);

    /**
     * Selects the first entry equal to the given one, or with the same key on a control with an
     * {@linkplain ItemSource#setKeyFunction key function}. Clears the selection if there is none.
     *
     * @param item the entry to find; may be null where the control holds null entries
     * @return this control
     */
    S selectItem(T item);

    /**
     * Clears the selection, as {@code select(-1)} does.
     *
     * @return this control
     */
    default S clearSelection() {
        return this.select(-1);
    }
}
