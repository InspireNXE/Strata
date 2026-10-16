package org.inspirenxe.strata.api.component;

import org.inspirenxe.strata.api.geom.Insets;

import java.util.List;

/**
 * A component with children. Later children are drawn on top and are hit first.
 *
 * <p><b>Not for implementation</b>; see {@link Component}.</p>
 */
public interface Container<T extends Container<T>> extends Component<T> {

    /**
     * The children in drawing order, as an unmodifiable live view.
     */
    List<Component<?>> children();

    /**
     * @throws IllegalArgumentException if the child already has a parent, would create a cycle, or was not made by
     *                                  the same {@link org.inspirenxe.strata.api.Ui} as this container
     */
    T add(Component<?> child);

    T remove(Component<?> child);

    T clear();

    Insets padding();

    T setPadding(Insets padding);

    default T setPadding(final float all) {
        return this.setPadding(Insets.of(all));
    }

    boolean clipsChildren();

    T setClipChildren(boolean clipChildren);
}
