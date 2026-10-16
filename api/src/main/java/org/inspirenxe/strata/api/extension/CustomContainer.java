package org.inspirenxe.strata.api.extension;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.Container;
import org.inspirenxe.strata.api.extension.peer.ContainerPeer;
import org.inspirenxe.strata.api.geom.Insets;
import org.inspirenxe.strata.api.geom.Rect;

import java.util.List;

/**
 * Base class for new layouts: a {@link CustomComponent} that also measures and places children. Strata's
 * {@code AnchorPanel}, {@code StackPanel}, {@code GridPanel} and {@code AbsolutePanel} are built on it.
 *
 * <pre>{@code
 * // Lays children out in a row from right to left
 * final class ReverseRow extends CustomContainer<ReverseRow> {
 *     ReverseRow(ComponentFactory factory) {
 *         super(factory);
 *     }
 *
 *     protected Size measure(MeasureContext context, float availableWidth, float availableHeight) {
 *         float width = 0, height = 0;
 *         for (Component<?> child : this.children()) {
 *             Size outer = context.measure(child, availableWidth, availableHeight);
 *             width += outer.width();
 *             height = Math.max(height, outer.height());
 *         }
 *         return new Size(width, height);
 *     }
 *
 *     protected void arrange(ArrangeContext context, Rect bounds) {
 *         float right = bounds.right();
 *         for (Component<?> child : this.children()) {
 *             Size outer = context.outerSize(child);
 *             right -= outer.width();
 *             context.arrange(child, new Rect(right, bounds.y(), outer.width(), outer.height()));
 *         }
 *     }
 * }
 * }</pre>
 *
 * <h2>Requirements for subclasses</h2>
 * <p>Everything required of a {@link CustomComponent}, and:</p>
 * <ul>
 *     <li>In {@link #measure}, measure every child, visible or not, with {@link MeasureContext#measure}. A child that
 *     is not measured keeps a stale size.</li>
 *     <li>In {@link #arrange}, give every child a slot with {@link ArrangeContext#arrange}, hidden children
 *     included. A child that is not arranged keeps stale bounds.</li>
 *     <li>Include the {@linkplain #padding() padding} yourself: subtract it from the space you offer children, add
 *     it to the size you return, and place children inside {@link #contentBox()}.</li>
 *     <li>Keep per-child data (such as a grid cell) in your own map, and drop it in {@link #onChildRemoved}.</li>
 *     <li>Leave drawing children to Strata. {@link #paint} draws under them; they are drawn on top, clipped to the
 *     bounds while {@link #clipsChildren()} is true.</li>
 * </ul>
 *
 * @param <T> the type callers see the container as
 */
public abstract class CustomContainer<T extends Container<T>> extends CustomComponent<T> implements Container<T> {

    /**
     * Creates the container and its peer.
     *
     * @param factory the factory of the host that will show this container
     * @throws NullPointerException if {@code factory} is null
     */
    protected CustomContainer(final ComponentFactory factory) {
        this(factory, null);
    }

    /**
     * Creates the container and its peer, decorated by a behavior.
     *
     * @param factory the factory of the host that will show this container
     * @param behavior wraps this container's hooks; {@code null} for none
     * @throws NullPointerException if {@code factory} is null
     */
    protected CustomContainer(final ComponentFactory factory, final ComponentBehavior<T> behavior) {
        super(factory, behavior, true);
    }

    private ContainerPeer containerPeer() {
        return (ContainerPeer) this.peer();
    }

    /**
     * Gives every child its slot. Called after {@link #measure}, with the container's final bounds.
     *
     * @param context child sizes and slot assignment
     * @param bounds the container's bounds, the same as {@link #bounds()}
     */
    protected abstract void arrange(ArrangeContext context, Rect bounds);

    /**
     * Called after a child is removed, to drop any data kept about it. The default does nothing.
     *
     * @param child the removed child
     */
    protected void onChildRemoved(final Component<?> child) {
    }

    /**
     * Returns the area children are laid out in: the bounds minus the padding. Valid during {@link #arrange} and
     * after layout.
     *
     * @return the content box
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

    // ------------------------------------------------------------------
    // Container, delegated to the peer. Documented on Container.
    // ------------------------------------------------------------------

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
