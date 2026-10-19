package org.inspirenxe.strata.api.control.attribute.item;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.event.EventTarget;

import java.util.function.Function;

/**
 * A control that shows each item through a component built by a cell factory, such as a list, a tree or a grid.
 *
 * <p>A control may build cells only for the items in view and drop them when they scroll out, so keep nothing in a
 * cell that must outlive it; build it from the item. Events from inside a cell, such as a button's
 * {@code PRESSED}, pass the control and reach the containers around it.</p>
 *
 * <p>Only Strata implements this interface.</p>
 *
 * @param <T> the type of the items
 * @param <S> the control's own type, which the setter returns
 */
public interface CellSource<T, S extends CellSource<T, S>> extends EventTarget<S> {

    /**
     * Sets what builds the component for an item, and builds the cells in view again.
     *
     * @param cellFactory makes a new component for an item each time its cell is built
     * @return this control
     */
    S setCellFactory(Function<? super T, ? extends Component<?>> cellFactory);
}
