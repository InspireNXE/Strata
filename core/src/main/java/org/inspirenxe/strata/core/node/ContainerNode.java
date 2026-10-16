package org.inspirenxe.strata.core.node;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.extension.ComponentBehavior;
import org.inspirenxe.strata.api.extension.ComponentHooks;
import org.inspirenxe.strata.api.extension.ContainerPeer;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.geom.Insets;
import org.inspirenxe.strata.core.paint.CanvasPainter;
import org.inspirenxe.strata.spi.Canvas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Core's half of a container: the child list, padding and clipping. Children are kept twice, as nodes for core and
 * as facades for callers, in the same order.
 */
public final class ContainerNode extends Node implements ContainerPeer {
    private final List<Node> childNodes;
    private final List<Component<?>> children;
    private final List<Component<?>> childrenView;
    private Insets padding;
    private boolean clipChildren;

    public ContainerNode(final CoreUi ui, final CustomComponent<?> facade, final ComponentHooks hooks,
                         final ComponentBehavior<?> behavior) {
        super(ui, facade, hooks, behavior);
        this.childNodes = new ArrayList<>();
        this.children = new ArrayList<>();
        this.childrenView = Collections.unmodifiableList(this.children);
        this.padding = Insets.NONE;
        this.clipChildren = true;
    }

    public List<Node> childNodes() {
        return Collections.unmodifiableList(this.childNodes);
    }

    @Override
    public List<Component<?>> children() {
        return this.childrenView;
    }

    @Override
    public void add(final Component<?> child) {
        final Node node = this.ui().nodeOf(child);
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
        final Canvas canvas = painter.canvas();
        canvas.pushClip(this.bounds());
        try {
            this.paintEach(painter);
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
     * Children later in the list are drawn on top, so they are tested first. A child outside a clipping
     * container's bounds cannot be hit, because the container's own bounds are checked first.
     */
    @Override
    public Node hitTest(final float x, final float y) {
        if (!this.isVisible() || !this.isEnabled()) {
            return null;
        }
        final boolean inside = this.bounds().contains(x, y);
        if (!inside && this.clipChildren) {
            return null;
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
