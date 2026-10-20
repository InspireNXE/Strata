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
package org.inspirenxe.strata.core.node;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.extension.ArrangeContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Rect;

import java.util.List;

/**
 * Base class of Strata's own controls that are built from private parts, the counterpart of the public
 * {@link org.inspirenxe.strata.api.extension.CustomComposite}. Its node is a {@link ContainerNode} whose children
 * callers cannot reach. Control events of a part added with {@link #addChild} stop here; those of content added with
 * {@link #addContent} pass on. {@link BuiltInContainer} extends it and makes the children public.
 *
 * <p>In {@link #measure} measure every child and in {@link #arrange} give every child a slot, hidden ones included,
 * as {@code CustomComposite} requires.</p>
 *
 * @param <T> the type callers see the component as
 */
public abstract class BuiltInComposite<T extends Component<T>> extends BuiltInComponent<T> {

    /**
     * Creates the component and its node, whose children are private parts.
     *
     * @param factory the factory of the host that will show this component
     */
    protected BuiltInComposite(final ComponentFactoryImpl factory) {
        super(factory, true, true);
    }

    /**
     * Creates the component and its container node.
     *
     * @param factory the factory of the host that will show this component
     * @param composite true if the children are private parts that stop control events
     */
    BuiltInComposite(final ComponentFactoryImpl factory, final boolean composite) {
        super(factory, true, composite);
    }

    /**
     * Returns the container node behind this component.
     *
     * @return the node
     */
    @Override
    protected final ContainerNode node() {
        return (ContainerNode) super.node();
    }

    // ------------------------------------------------------------------
    // Hooks for subclasses
    // ------------------------------------------------------------------

    /**
     * Gives every child its slot. Called after {@link #measure}, with the component's final bounds, and again with
     * the same bounds after {@link #invalidateArrangement()}.
     *
     * @param context child sizes and slot assignment
     * @param bounds the component's bounds
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
     * Returns the rectangle children are clipped to and hit-tested against while {@link #clipsChildren()} is true.
     * Called during painting and hit testing. The default is the bounds.
     *
     * @return the clip, in the host's coordinates
     */
    protected Rect childClip() {
        return this.bounds();
    }

    /**
     * Draws after the children, on top of them and outside their clip. The default draws nothing.
     *
     * @param painter what to draw with; valid only during this call
     * @param bounds the component's bounds
     */
    protected void paintOverChildren(final Painter painter, final Rect bounds) {
    }

    /**
     * Called after focus moves to a component inside this one, on every ancestor of it, innermost first. The default
     * does nothing.
     *
     * @param descendant the component that gained focus
     */
    protected void onDescendantFocused(final Component<?> descendant) {
    }

    /**
     * Asks for this component to be arranged again in its last bounds before the next frame, without measuring
     * anything.
     */
    protected final void invalidateArrangement() {
        this.node().invalidateArrangement();
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
        return this.node().children();
    }

    /**
     * Adds a private part at the end. Its control events, and those of anything inside it, stop at this component.
     *
     * @param child the child
     * @throws IllegalArgumentException if the child already has a parent, is a host's content, would create a
     *     cycle, or was made by another factory
     */
    protected final void addChild(final Component<?> child) {
        this.node().add(child);
    }

    /**
     * Adds a child that is content shown for the user, such as a cell from the user's cell factory. Its control
     * events, and those of anything inside it, pass this component and every composite above it.
     *
     * @param child the child
     * @throws IllegalArgumentException if the child already has a parent, is a host's content, would create a
     *     cycle, or was made by another factory
     */
    protected final void addContent(final Component<?> child) {
        this.node().addContent(child);
    }

    /**
     * Removes a child. Does nothing if it is not a child of this component.
     *
     * @param child the child
     */
    protected final void removeChild(final Component<?> child) {
        this.node().remove(child);
    }

    /**
     * Removes every child.
     */
    protected final void clearChildren() {
        this.node().clear();
    }

    /**
     * Returns whether children are clipped to {@link #childClip()}.
     *
     * @return true by default
     */
    protected boolean clipsChildren() {
        return this.node().clipsChildren();
    }

    /**
     * Sets whether children are clipped to {@link #childClip()}.
     *
     * @param clip false to let children draw and be hit outside it
     */
    protected final void setChildClipping(final boolean clip) {
        this.node().setClipChildren(clip);
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
