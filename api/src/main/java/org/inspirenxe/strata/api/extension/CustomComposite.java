package org.inspirenxe.strata.api.extension;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.extension.peer.ContainerPeer;
import org.inspirenxe.strata.api.geometry.Rect;

import java.util.List;

/**
 * Base class for components built from other components that callers must not rearrange, such as a list made of
 * rows or a tab bar made of tabs. It has children like a container, but they are private: the component adds and
 * removes them through the protected methods here, and callers see an ordinary {@link Component}.
 * {@link CustomContainer} extends this class and makes the children public.
 *
 * <h2>Requirements for subclasses</h2>
 * <p>Everything required of a {@link CustomComponent}, and:</p>
 * <ul>
 *     <li>In {@link #measure}, measure every child, visible or not, with {@link MeasureContext#measure}. A child that
 *     is not measured keeps a stale size.</li>
 *     <li>In {@link #arrange}, give every child a slot with {@link ArrangeContext#arrange}, hidden children
 *     included. A child that is not arranged keeps stale bounds.</li>
 *     <li>Children may be added and removed during {@link #measure} or {@link #arrange}, which is how a list creates
 *     only the rows it shows. Measure a child added during arrange with {@link ArrangeContext#measure} before giving
 *     it a slot. Adding or removing a child schedules one more layout. Do not change anything else from layout.</li>
 *     <li>Keep per-child data in your own map, and drop it in {@link #onChildRemoved}.</li>
 *     <li>Leave drawing children to Strata. {@link #paint} draws under them and {@link #paintOverChildren} over
 *     them. While {@link #clipsChildren()} is true they are clipped to {@link #childClip()}.</li>
 * </ul>
 *
 * @param <T> the type callers see the component as
 */
public abstract class CustomComposite<T extends Component<T>> extends CustomComponent<T> {

    /**
     * Creates the component and its peer.
     *
     * @param factory the factory of the host that will show this component
     */
    protected CustomComposite(final ComponentFactory factory) {
        super(factory, true);
    }

    final ContainerPeer containerPeer() {
        return (ContainerPeer) this.peer();
    }

    // ------------------------------------------------------------------
    // Hooks for subclasses
    // ------------------------------------------------------------------

    /**
     * Gives every child its slot. Called after {@link #measure}, with the component's final bounds, and again with
     * the same bounds after {@link #invalidateArrangement()}.
     *
     * @param context child sizes and slot assignment
     * @param bounds the component's bounds, the same as {@link #bounds()}
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
     * Returns the rectangle children are clipped to and hit-tested against while {@link #clipsChildren()} is true. A
     * scroll panel returns its viewport, so nothing draws under its scroll bars. Called during painting and hit
     * testing; return a value computed during layout. The default is the bounds.
     *
     * @return the clip, in the host's coordinates
     */
    protected Rect childClip() {
        return this.bounds();
    }

    /**
     * Draws after the children, on top of them and outside their clip, for scroll bars, overlays and focus rings.
     * The default draws nothing.
     *
     * @param painter what to draw with; valid only during this call
     * @param bounds the component's bounds
     */
    protected void paintOverChildren(final Painter painter, final Rect bounds) {
    }

    /**
     * Called after focus moves to a component inside this one, on every ancestor of the newly focused component,
     * innermost first. A scroll panel uses it to bring the focused component into view. The default does nothing.
     *
     * @param descendant the component that gained focus
     */
    protected void onDescendantFocused(final Component<?> descendant) {
    }

    /**
     * Asks for this component to be arranged again in its last bounds before the next frame, without measuring
     * anything. Use it when children move but no size changes, as when scrolling. Changes that affect sizes need
     * {@link #invalidateLayout()} instead.
     */
    protected final void invalidateArrangement() {
        this.peer().invalidateArrangement();
    }

    // ------------------------------------------------------------------
    // Children
    // ------------------------------------------------------------------

    /**
     * Returns the children, in drawing order: later children draw on top and are hit first.
     *
     * @return a live, unmodifiable view
     */
    protected final List<Component<?>> childList() {
        return this.containerPeer().children();
    }

    /**
     * Adds a child at the end.
     *
     * @param child the child
     * @throws IllegalArgumentException if the child already has a parent, is a host's content, would create a
     *     cycle, or was made by another factory
     */
    protected final void addChild(final Component<?> child) {
        this.containerPeer().add(child);
    }

    /**
     * Adds a child that is content the composite shows for its user, such as a list's cell made by the user's cell
     * factory or the panels on either side of a split pane. The child is still private in every other way: callers
     * cannot reach it through this component.
     *
     * <p>The difference is in where control events go. Control events (requests and reports) of a composite's own
     * parts stop at the composite, so a table's inner list is never heard outside the table. Control events from
     * content added this way, and from anything inside it, pass the composite and every composite above it, so a
     * button in a table cell is heard by a handler around the table.</p>
     *
     * @param child the child
     * @throws IllegalArgumentException if the child already has a parent, is a host's content, would create a
     *     cycle, or was made by another factory
     */
    protected final void addContent(final Component<?> child) {
        this.containerPeer().addContent(child);
    }

    /**
     * Removes a child. Does nothing if it is not a child of this component.
     *
     * @param child the child
     */
    protected final void removeChild(final Component<?> child) {
        this.containerPeer().remove(child);
    }

    /**
     * Removes every child.
     */
    protected final void clearChildren() {
        this.containerPeer().clear();
    }

    /**
     * Returns whether children are clipped to {@link #childClip()}.
     *
     * @return true by default
     */
    protected boolean clipsChildren() {
        return this.containerPeer().clipsChildren();
    }

    /**
     * Sets whether children are clipped to {@link #childClip()}.
     *
     * @param clip false to let children draw and be hit outside it
     */
    protected final void setChildClipping(final boolean clip) {
        this.containerPeer().setClipChildren(clip);
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
    final Rect childClipHook() {
        return this.childClip();
    }

    @Override
    final void paintOverChildrenHook(final Painter painter, final Rect bounds) {
        this.paintOverChildren(painter, bounds);
    }

    @Override
    final void descendantFocusedHook(final Component<?> descendant) {
        this.onDescendantFocused(descendant);
    }
}
