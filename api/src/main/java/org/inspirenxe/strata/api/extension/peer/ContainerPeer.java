package org.inspirenxe.strata.api.extension.peer;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.geom.Insets;

import java.util.List;

/**
 * Strata's half of a {@link org.inspirenxe.strata.api.extension.CustomContainer}. Each method has the meaning of
 * the {@link org.inspirenxe.strata.api.component.Container} method of the same name, with setters returning
 * nothing. Not for use outside Strata.
 */
public interface ContainerPeer extends ComponentPeer {

    /**
     * See {@link org.inspirenxe.strata.api.component.Container#children()}.
     *
     * @return the live, unmodifiable child list
     */
    List<Component<?>> children();

    /**
     * See {@link org.inspirenxe.strata.api.component.Container#add(Component)}.
     *
     * @param child the child to add
     */
    void add(Component<?> child);

    /**
     * Adds a child that holds content the composite shows for its user. See
     * {@code CustomComposite.addContent(Component)}.
     *
     * @param child the child to add
     */
    void addContent(Component<?> child);

    /**
     * See {@link org.inspirenxe.strata.api.component.Container#remove(Component)}.
     *
     * @param child the child to remove
     */
    void remove(Component<?> child);

    /**
     * See {@link org.inspirenxe.strata.api.component.Container#clear()}.
     */
    void clear();

    /**
     * See {@link org.inspirenxe.strata.api.component.Container#padding()}.
     *
     * @return the padding
     */
    Insets padding();

    /**
     * See {@link org.inspirenxe.strata.api.component.Container#setPadding(Insets)}.
     *
     * @param padding the padding
     */
    void setPadding(Insets padding);

    /**
     * See {@link org.inspirenxe.strata.api.component.Container#clipsChildren()}.
     *
     * @return true if children are clipped
     */
    boolean clipsChildren();

    /**
     * See {@link org.inspirenxe.strata.api.component.Container#setClipChildren(boolean)}.
     *
     * @param clipChildren false to let children draw outside
     */
    void setClipChildren(boolean clipChildren);
}
