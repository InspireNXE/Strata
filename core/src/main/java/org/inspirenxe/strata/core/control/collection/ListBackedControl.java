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
package org.inspirenxe.strata.core.control.collection;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.control.attribute.item.Activatable;
import org.inspirenxe.strata.api.control.attribute.item.MultiSelection;
import org.inspirenxe.strata.api.control.attribute.item.RowView;
import org.inspirenxe.strata.api.control.attribute.item.SelectionMode;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.core.node.BuiltInComposite;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;

import java.util.Collection;
import java.util.List;

/**
 * The part a table and a tree share: both show their rows in a private {@link ListViewImpl}. This base makes the
 * list, routes its input and selection changes here, fires them as the control's own {@code SELECT},
 * {@code SELECT_MANY} and {@code ACTIVATE} requests and reports, and forwards the row and selection members the
 * list answers by itself. The subclass adds the list as a child where it belongs among its parts.
 *
 * @param <T> the type of the control's items
 * @param <E> the type of the list's rows
 * @param <S> the control's own type
 */
abstract class ListBackedControl<T, E, S extends Component<S> & RowView<S> & MultiSelection<T, S> & Activatable<S>>
        extends BuiltInComposite<S> implements RowView<S>, MultiSelection<T, S>, Activatable<S> {
    /**
     * The list that shows the rows.
     */
    final ListViewImpl<E> list;

    /**
     * Makes the list, which builds its rows with {@link #row}, and routes it here.
     *
     * @param ui the factory that owns the control
     */
    @SuppressWarnings("this-escape")
    ListBackedControl(final ComponentFactoryImpl ui) {
        super(ui);
        this.list = new ListViewImpl<>(ui, this::row);
        this.list.setAnchor(Anchor.FILL);
        this.setFocusDelegate(this.list);
        this.list.route(this::userSelect, this::userActivate, (index, previous) -> {
            if (this.reportsSelection()) {
                this.report(SELECTED, index, previous);
            }
        });
        this.list.routeMany((indexes, current) -> this.request(SELECT_MANY, indexes, set -> this.list.applyMany(set, current)),
                (indexes, previous) -> {
                    if (this.reportsSelection()) {
                        this.report(SELECTED_MANY, indexes, previous);
                    }
                });
    }

    /**
     * Makes the component for a row as it comes into view.
     */
    abstract Component<?> row(E row);

    /**
     * Returns whether a change of the list's selection is reported as it happens. A tree holds the reports back
     * while one change moves the selection several times, and reports once at the end.
     */
    boolean reportsSelection() {
        return true;
    }

    /**
     * Asks to select a row the user chose in the list.
     */
    private void userSelect(final int index) {
        this.request(SELECT, index, this.list::applySelect);
    }

    /**
     * Asks to activate a row the user activated in the list.
     */
    private void userActivate(final int index) {
        this.request(ACTIVATE, index, this::applyActivate);
    }

    /**
     * Reports the row a request ended with as activated. A null or an index outside the rows is ignored.
     */
    private void applyActivate(final Integer index) {
        if (Items.isIndex(index, this.list.items().size())) {
            this.report(ACTIVATED, index, index);
        }
    }

    // ------------------------------------------------------------------
    // RowView
    // ------------------------------------------------------------------

    @Override
    public float rowHeight() {
        return this.list.rowHeight();
    }

    @Override
    public S setRowHeight(final float height) {
        this.list.setRowHeight(height);
        return this.self();
    }

    @Override
    public float rowSpacing() {
        return this.list.rowSpacing();
    }

    @Override
    public S setRowSpacing(final float spacing) {
        this.list.setRowSpacing(spacing);
        return this.self();
    }

    @Override
    public Text placeholder() {
        return this.list.placeholder();
    }

    @Override
    public S setPlaceholder(final Text placeholder) {
        this.list.setPlaceholder(placeholder);
        return this.self();
    }

    @Override
    public float scrollY() {
        return this.list.scrollY();
    }

    @Override
    public S setScrollY(final float scrollY) {
        this.list.setScrollY(scrollY);
        return this.self();
    }

    @Override
    public S scrollToIndex(final int index) {
        this.list.scrollToIndex(index);
        return this.self();
    }

    @Override
    public boolean isDeselectable() {
        return this.list.isDeselectable();
    }

    @Override
    public S setDeselectable(final boolean deselectable) {
        this.list.setDeselectable(deselectable);
        return this.self();
    }

    @Override
    public S setActivateOnClick(final boolean onClick) {
        this.list.setActivateOnClick(onClick);
        return this.self();
    }

    // ------------------------------------------------------------------
    // Selection by index
    // ------------------------------------------------------------------

    @Override
    public int selectedIndex() {
        return this.list.selectedIndex();
    }

    @Override
    public S select(final int index) {
        this.list.select(index);
        return this.self();
    }

    @Override
    public SelectionMode selectionMode() {
        return this.list.selectionMode();
    }

    @Override
    public S setSelectionMode(final SelectionMode mode) {
        this.list.setSelectionMode(mode);
        return this.self();
    }

    @Override
    public List<Integer> selectedIndexes() {
        return this.list.selectedIndexes();
    }

    @Override
    public S selectIndexes(final Collection<Integer> indexes) {
        this.list.selectIndexes(indexes);
        return this.self();
    }

    @Override
    public S selectAll() {
        this.list.selectAll();
        return this.self();
    }
}
