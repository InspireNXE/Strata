package org.inspirenxe.strata.core.node;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.peer.ComponentHooks;
import org.inspirenxe.strata.api.extension.peer.ContainerPeer;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.core.paint.CanvasPainter;
import org.inspirenxe.strata.spi.Canvas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Core's half of a container or composite: the child list, padding and clipping. Children are kept twice in the
 * same order, as nodes for core and as public components for callers. A new container has no padding and clips its
 * children to its {@code childClip}, which is its bounds unless the component says otherwise. While clipping, a
 * child whose bounds miss the clip entirely is not painted.
 */
public final class ContainerNode extends Node implements ContainerPeer {
    private final List<Node> childNodes;
    private final List<Component<?>> children;
    private final List<Component<?>> childrenView;
    private Insets padding;
    private boolean clipChildren;

    /**
     * Creates the node behind a container. Only {@link ComponentFactoryImpl#createContainerPeer} calls this.
     *
     * @param ui the factory that owns the container
     * @param facade the public container this node implements
     * @param hooks the container's protected hooks
     */
    public ContainerNode(final ComponentFactoryImpl ui, final CustomComponent<?> facade, final ComponentHooks hooks) {
        super(ui, facade, hooks);
        this.childNodes = new ArrayList<>();
        this.children = new ArrayList<>();
        this.childrenView = Collections.unmodifiableList(this.children);
        this.padding = Insets.NONE;
        this.clipChildren = true;
    }

    /**
     * Returns the child nodes, in the same order as {@link #children()}.
     *
     * @return an unmodifiable view that follows later changes
     */
    public List<Node> childNodes() {
        return Collections.unmodifiableList(this.childNodes);
    }

    @Override
    public List<Component<?>> children() {
        return this.childrenView;
    }

    @Override
    public void add(final Component<?> child) {
        final Node node = this.factory().nodeOf(child);
        if (node.parentNode() != null) {
            throw new IllegalArgumentException(child + " already belongs to " + node.parent());
        }
        if (node == this || this.isDescendantOf(node)) {
            throw new IllegalArgumentException("Adding " + child + " to " + this.facade() + " would create a cycle");
        }
        if (node.root() != null) {
            throw new IllegalArgumentException(child + " is the content of a host and cannot be added to a container");
        }
        this.childNodes.add(node);
        this.children.add(node.facade());
        node.setParent(this);
        final Root root = this.root();
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

    @Override
    public void remove(final Component<?> child) {
        final int index = this.children.indexOf(child);
        if (index < 0) {
            return;
        }
        final Node node = this.childNodes.remove(index);
        this.children.remove(index);
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
        this.padding = Objects.requireNonNull(padding, "padding");
        this.invalidateLayout();
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
        if (!this.clipChildren) {
            this.paintEach(painter);
            return;
        }
        final Rect clip = this.hooks().childClip();
        final Canvas canvas = painter.canvas();
        canvas.pushClip(clip);
        try {
            for (final Node child : this.childNodes) {
                if (child.bounds().intersects(clip)) {
                    child.paint(painter);
                }
            }
        } finally {
            canvas.popClip();
        }
    }

    private void paintEach(final CanvasPainter painter) {
        for (final Node child : this.childNodes) {
            child.paint(painter);
        }
    }

    /**
     * Children later in the list are drawn on top, so they are tested first. A clipping container's children can
     * only be hit inside its child clip, and nothing can be hit outside its bounds.
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
        for (int i = this.childNodes.size() - 1; i >= 0; i--) {
            final Node hit = this.childNodes.get(i).hitTest(x, y);
            if (hit != null) {
                return hit;
            }
        }
        return inside ? this : null;
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
