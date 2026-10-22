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
import org.inspirenxe.strata.api.extension.peer.ContainerPeer;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.core.paint.CanvasPainter;
import org.inspirenxe.strata.spi.Canvas;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Core's half of a container or composite: the child list, the drawing order, padding and clipping. A new container
 * has no padding and clips its children to its {@code childClip}, which is its bounds unless the component says
 * otherwise. While clipping, a child whose bounds miss the clip entirely is not painted.
 *
 * <p>Children are kept in layout order twice, as nodes for core and as public components for callers; layout, focus
 * and Tab follow that order. The drawing order is kept apart in an array sorted by z index and then by a sequence
 * number: each child gets the next number above every sibling's when it is added or raised, and the next one below
 * every sibling's when it is lowered. The array is sorted again, in place, before the first paint or hit test after a
 * child is added, removed, raised or lowered or changes z index, so painting and hit testing allocate nothing while
 * the children stay the same. A move in the layout order leaves the drawing order alone.</p>
 */
public final class ContainerNode extends Node implements ContainerPeer {
    private static final Node[] NO_NODES = new Node[0];

    private final List<Node> childNodes;
    private final List<Component<?>> children;
    private final List<Component<?>> childrenView;
    private Insets padding;
    // The padding the container has while no padding is set; a window's comes from its theme
    private Insets defaultPadding;
    private boolean paddingSet;
    private boolean clipChildren;
    private final boolean composite;

    // The drawing order, bottom first, in the first paintCount entries; sorted again when paintOrderValid is false
    private Node[] paintOrder;
    private int paintCount;
    private boolean paintOrderValid;
    // The last sequence numbers handed out at the top and at the bottom
    private long topSequence;
    private long bottomSequence;

    /**
     * Creates the node behind a container or composite. {@link ComponentFactoryImpl#createContainerPeer} and
     * {@link BuiltInComposite} call this.
     *
     * @param ui the factory that owns the container
     * @param facade the public container this node implements
     * @param hooks the container's protected hooks
     * @param composite true if the children are private parts, false if callers add and remove them
     */
    ContainerNode(final ComponentFactoryImpl ui, final Component<?> facade, final NodeHooks hooks,
                  final boolean composite) {
        super(ui, facade, hooks);
        this.composite = composite;
        this.childNodes = new ArrayList<>();
        this.children = new ArrayList<>();
        this.childrenView = Collections.unmodifiableList(this.children);
        this.padding = Insets.NONE;
        this.defaultPadding = Insets.NONE;
        this.clipChildren = true;
        this.paintOrder = NO_NODES;
        this.paintOrderValid = true;
    }

    /**
     * Returns the child nodes in layout order, the same order as {@link #children()}.
     *
     * @return an unmodifiable view that follows later changes
     */
    public List<Node> childNodes() {
        return Collections.unmodifiableList(this.childNodes);
    }

    @Override
    public boolean isComposite() {
        return this.composite;
    }

    @Override
    public List<Component<?>> children() {
        return this.childrenView;
    }

    @Override
    public void add(final Component<?> child) {
        this.insert(this.childNodes.size(), child);
    }

    /**
     * Adds a child at a place in the layout order, which is also the tree order Tab follows. The child is drawn over
     * its siblings with the same z index.
     *
     * @param index where the child goes: 0 for the first, the number of children for the last
     * @param child the child
     * @throws IndexOutOfBoundsException if the index is negative or greater than the number of children
     * @throws IllegalArgumentException if the child already has a parent, this container included, is a host's
     *     content, would create a cycle, or was made by another factory
     * @throws org.inspirenxe.strata.api.theme.ThemeException if this container is attached and the root's theme
     *     lacks a style key the child's tree requires; the child is not added
     */
    @Override
    public void insert(final int index, final Component<?> child) {
        Objects.checkIndex(index, this.childNodes.size() + 1);
        final Node node = this.factory().nodeOf(child);
        if (node.parentNode() != null) {
            throw new IllegalArgumentException(child + " already belongs to " + node.parentNode().facade());
        }
        if (node == this || this.isDescendantOf(node)) {
            throw new IllegalArgumentException("Adding " + child + " to " + this.facade() + " would create a cycle");
        }
        if (node.root() != null) {
            throw new IllegalArgumentException(child + " is the content of a host and cannot be added to a container");
        }
        final Root root = this.root();
        if (root != null) {
            // Before anything changes, so a theme that lacks a key leaves the container as it was
            root.checkTheme(node);
        }
        this.childNodes.add(index, node);
        this.children.add(index, node.facade());
        node.setParent(this);
        node.setPaintSequence(++this.topSequence);
        this.paintOrderValid = false;
        if (root != null) {
            root.attachTree(node);
        }
        this.invalidateLayout();
    }

    @Override
    public void addContent(final Component<?> child) {
        this.add(child);
        this.factory().nodeOf(child).setContent(true);
    }

    /**
     * Adds content, as {@link #addContent(Component)} does, at a place in the layout order.
     *
     * @param index where the child goes: 0 for the first, the number of children for the last
     * @param child the child
     * @throws IndexOutOfBoundsException if the index is negative or greater than the number of children
     * @throws IllegalArgumentException if the child already has a parent, is a host's content, would create a
     *     cycle, or was made by another factory
     * @throws org.inspirenxe.strata.api.theme.ThemeException as for {@link #insert(int, Component)}
     */
    public void insertContent(final int index, final Component<?> child) {
        this.insert(index, child);
        this.factory().nodeOf(child).setContent(true);
    }

    @Override
    public void move(final Component<?> child, final int index) {
        final int from = this.requireChild(child);
        Objects.checkIndex(index, this.childNodes.size());
        if (from == index) {
            return;
        }
        final Node node = this.childNodes.remove(from);
        this.childNodes.add(index, node);
        this.children.remove(from);
        this.children.add(index, node.facade());
        this.invalidateLayout();
        final Root root = this.root();
        if (root != null) {
            // Tab follows the layout order
            root.treeChanged();
        }
    }

    @Override
    public int indexOf(final Component<?> child) {
        for (int i = 0; i < this.children.size(); i++) {
            if (this.children.get(i) == child) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Draws a child over its siblings with the same z index. Does nothing if it is already the last child added or
     * raised. Marks nothing for layout.
     *
     * @param child the child
     * @throws IllegalArgumentException if the component is not a child of this container
     */
    @Override
    public void raise(final Component<?> child) {
        final Node node = this.childNodes.get(this.requireChild(child));
        if (node.paintSequence() != this.topSequence) {
            node.setPaintSequence(++this.topSequence);
            this.paintOrderValid = false;
        }
    }

    @Override
    public void lower(final Component<?> child) {
        final Node node = this.childNodes.get(this.requireChild(child));
        if (node.paintSequence() != this.bottomSequence) {
            node.setPaintSequence(--this.bottomSequence);
            this.paintOrderValid = false;
        }
    }

    @Override
    public List<Component<?>> paintOrder() {
        this.sortPaintOrder();
        final List<Component<?>> order = new ArrayList<>(this.paintCount);
        for (int i = 0; i < this.paintCount; i++) {
            order.add(this.paintOrder[i].facade());
        }
        return Collections.unmodifiableList(order);
    }

    /**
     * Makes the drawing order be sorted again before the next paint or hit test, after a child's z index changed.
     */
    void paintOrderChanged() {
        this.paintOrderValid = false;
    }

    @Override
    public void remove(final Component<?> child) {
        final int index = this.indexOf(child);
        if (index < 0) {
            return;
        }
        final Node node = this.childNodes.remove(index);
        this.children.remove(index);
        this.paintOrderValid = false;
        this.hooks().childRemoved(node.facade());
        final Root root = this.root();
        if (root != null) {
            root.detachTree(node);
        }
        node.setParent(null);
        node.setContent(false);
        this.invalidateLayout();
    }

    @Override
    public void clear() {
        for (final Component<?> child : List.copyOf(this.children)) {
            this.remove(child);
        }
    }

    @Override
    public Insets padding() {
        return this.padding;
    }

    @Override
    public void setPadding(final Insets padding) {
        this.paddingSet = padding != null;
        this.padding = padding != null ? padding : this.defaultPadding;
        this.invalidateLayout();
    }

    /**
     * Sets the padding the container has while none is set with {@link #setPadding}, as a window does from its
     * theme. It takes effect now unless a padding is set.
     *
     * @param padding the default padding
     */
    public void setDefaultPadding(final Insets padding) {
        this.defaultPadding = Objects.requireNonNull(padding, "padding");
        if (!this.paddingSet && !this.padding.equals(padding)) {
            this.padding = padding;
            this.invalidateLayout();
        }
    }

    @Override
    public boolean clipsChildren() {
        return this.clipChildren;
    }

    @Override
    public void setClipChildren(final boolean clipChildren) {
        this.clipChildren = clipChildren;
    }

    @Override
    protected void paintChildren(final CanvasPainter painter) {
        if (this.childNodes.isEmpty()) {
            return;
        }
        this.sortPaintOrder();
        final Node[] order = this.paintOrder;
        final int count = this.paintCount;
        if (!this.clipChildren) {
            for (int i = 0; i < count; i++) {
                order[i].paint(painter);
            }
            return;
        }
        final Rect clip = this.hooks().childClip();
        final Canvas canvas = painter.canvas();
        canvas.pushClip(clip);
        try {
            for (int i = 0; i < count; i++) {
                final Node child = order[i];
                if (child.bounds().intersects(clip)) {
                    child.paint(painter);
                }
            }
        } finally {
            canvas.popClip();
        }
    }

    /**
     * Children are tested from the top of the drawing order down. A clipping container's children can only be hit
     * inside its child clip, and nothing can be hit outside its bounds.
     */
    @Override
    public Node hitTest(final float x, final float y) {
        if (!this.isVisible() || !this.isEnabled()) {
            return null;
        }
        final boolean inside = this.bounds().contains(x, y);
        if (this.clipChildren && (!inside || !this.hooks().childClip().contains(x, y))) {
            return inside ? this : null;
        }
        this.sortPaintOrder();
        for (int i = this.paintCount - 1; i >= 0; i--) {
            final Node hit = this.paintOrder[i].hitTest(x, y);
            if (hit != null) {
                return hit;
            }
        }
        return inside ? this : null;
    }

    /**
     * Sorts the drawing order again if a change made it stale: copies the children into the array, growing it only
     * when it is too small, and sorts them in place by insertion, which is stable and allocates nothing. Children
     * added at the end and never raised are already in order, so that case takes one pass.
     */
    private void sortPaintOrder() {
        if (this.paintOrderValid) {
            return;
        }
        this.paintOrderValid = true;
        final int count = this.childNodes.size();
        if (this.paintOrder.length < count) {
            this.paintOrder = new Node[Math.max(count, this.paintOrder.length * 2)];
        } else if (count < this.paintCount) {
            // Let go of removed children
            Arrays.fill(this.paintOrder, count, this.paintCount, null);
        }
        final Node[] order = this.paintOrder;
        for (int i = 0; i < count; i++) {
            final Node node = this.childNodes.get(i);
            int j = i - 1;
            while (j >= 0 && ContainerNode.drawsAfter(order[j], node)) {
                order[j + 1] = order[j];
                j--;
            }
            order[j + 1] = node;
        }
        this.paintCount = count;
    }

    private static boolean drawsAfter(final Node a, final Node b) {
        if (a.zIndex() != b.zIndex()) {
            return a.zIndex() > b.zIndex();
        }
        return a.paintSequence() > b.paintSequence();
    }

    private int requireChild(final Component<?> child) {
        Objects.requireNonNull(child, "child");
        final int index = this.indexOf(child);
        if (index < 0) {
            throw new IllegalArgumentException(child + " is not a child of " + this.facade());
        }
        return index;
    }

    private boolean isDescendantOf(final Node candidate) {
        ContainerNode node = this.parentNode();
        while (node != null) {
            if (node == candidate) {
                return true;
            }
            node = node.parentNode();
        }
        return false;
    }
}
