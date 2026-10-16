package org.inspirenxe.strata.api.extension;

import org.inspirenxe.strata.api.Ui;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.Container;
import org.inspirenxe.strata.api.geom.Insets;
import org.inspirenxe.strata.api.geom.Rect;

import java.util.List;

/**
 * Base for new container types: a {@link CustomComponent} that also measures and places children. Strata's own
 * {@code Panel}, {@code StackPanel}, {@code Grid} and {@code AbsolutePanel} are built on it.
 *
 * <p>In {@link #measure}, measure every child with {@link MeasureContext#measure}; in {@link #arrange}, give every
 * child a slot with {@link ArrangeContext#arrange}. Children are drawn after {@link #paint}, clipped to the bounds
 * unless clipping is turned off.</p>
 */
public abstract class CustomContainer<T extends Container<T>> extends CustomComponent<T> implements Container<T> {

    protected CustomContainer(final Ui ui) {
        this(ui, null);
    }

    protected CustomContainer(final Ui ui, final ComponentBehavior<T> behavior) {
        super(ui, behavior, true);
    }

    private ContainerPeer containerPeer() {
        return (ContainerPeer) this.peer();
    }

    /**
     * Places the children inside the final bounds.
     */
    protected abstract void arrange(ArrangeContext context, Rect bounds);

    /**
     * Lets subclasses drop per-child data when a child leaves.
     */
    protected void onChildRemoved(final Component<?> child) {
    }

    /**
     * The bounds minus padding: the area children are laid out in.
     */
    protected final Rect contentBox() {
        return this.bounds().shrink(this.padding());
    }

    @Override
    final void arrangeHook(final ArrangeContext context, final Rect bounds) {
        this.arrange(context, bounds);
    }

    @Override
    final void childRemovedHook(final Component<?> child) {
        this.onChildRemoved(child);
    }

    @Override
    public final List<Component<?>> children() {
        return this.containerPeer().children();
    }

    @Override
    public final T add(final Component<?> child) {
        this.containerPeer().add(child);
        return this.self();
    }

    @Override
    public final T remove(final Component<?> child) {
        this.containerPeer().remove(child);
        return this.self();
    }

    @Override
    public final T clear() {
        this.containerPeer().clear();
        return this.self();
    }

    @Override
    public final Insets padding() {
        return this.containerPeer().padding();
    }

    @Override
    public final T setPadding(final Insets padding) {
        this.containerPeer().setPadding(padding);
        return this.self();
    }

    @Override
    public final boolean clipsChildren() {
        return this.containerPeer().clipsChildren();
    }

    @Override
    public final T setClipChildren(final boolean clipChildren) {
        this.containerPeer().setClipChildren(clipChildren);
        return this.self();
    }
}
