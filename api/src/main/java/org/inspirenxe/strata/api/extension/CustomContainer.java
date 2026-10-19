package org.inspirenxe.strata.api.extension;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.Container;
import org.inspirenxe.strata.api.geom.Insets;
import org.inspirenxe.strata.api.geom.Rect;

import java.util.List;

/**
 * Base class for new layouts: a {@link CustomComposite} whose children callers add and remove. Strata's
 * {@code AnchorPanel}, {@code StackPanel}, {@code GridPanel}, {@code AbsolutePanel} and {@code ScrollPanel} are
 * built on it.
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
 * <p>Everything required of a {@link CustomComposite}, and:</p>
 * <ul>
 *     <li>Include the {@linkplain #padding() padding} yourself: subtract it from the space you offer children, add
 *     it to the size you return, and place children inside {@link #contentBox()}.</li>
 *     <li>Expect callers to add and remove children at any time outside layout.</li>
 * </ul>
 *
 * @param <T> the type callers see the container as
 */
public abstract class CustomContainer<T extends Container<T>> extends CustomComposite<T> implements Container<T> {

    /**
     * Creates the container and its peer.
     *
     * @param factory the factory of the host that will show this container
     */
    protected CustomContainer(final ComponentFactory factory) {
        super(factory);
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
