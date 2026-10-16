package org.inspirenxe.strata.core.node;

import org.inspirenxe.strata.api.component.Container;
import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.component.HorizontalAlignment;
import org.inspirenxe.strata.api.component.VerticalAlignment;
import org.inspirenxe.strata.api.event.CharEvent;
import org.inspirenxe.strata.api.event.DragEvent;
import org.inspirenxe.strata.api.event.Event;
import org.inspirenxe.strata.api.event.EventHandler;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.FocusEvent;
import org.inspirenxe.strata.api.event.KeyEvent;
import org.inspirenxe.strata.api.event.Phase;
import org.inspirenxe.strata.api.event.PointerButtonEvent;
import org.inspirenxe.strata.api.event.PointerEvent;
import org.inspirenxe.strata.api.event.ScrollEvent;
import org.inspirenxe.strata.api.event.Subscription;
import org.inspirenxe.strata.api.event.UIEvents;
import org.inspirenxe.strata.api.extension.ComponentBehavior;
import org.inspirenxe.strata.api.extension.ComponentHooks;
import org.inspirenxe.strata.api.extension.ComponentPeer;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.Next;
import org.inspirenxe.strata.api.geom.Insets;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.geom.Size;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.core.event.EventDispatcher;
import org.inspirenxe.strata.core.event.EventImpl;
import org.inspirenxe.strata.core.event.EventRouter;
import org.inspirenxe.strata.core.paint.CanvasPainter;

import java.util.Objects;
import java.util.Set;

/**
 * Core's half of every component: state, layout bookkeeping and event routing. The public object is the
 * {@link #facade()}, a {@link CustomComponent} whose {@code final} methods delegate here; its protected hooks are
 * reached through {@link ComponentHooks}, wrapped by the optional {@link ComponentBehavior}.
 */
public class Node implements ComponentPeer {
    protected static final float UNSET = Float.NaN;

    private final CoreUi ui;
    private final CustomComponent<?> facade;
    private final ComponentHooks hooks;
    @SuppressWarnings("rawtypes")
    private final ComponentBehavior behavior;

    private ContainerNode parent;
    private Root root;
    private EventDispatcher dispatcher;
    private Theme theme;

    private String id;
    private boolean visible;
    private boolean enabled;
    private boolean focusable;
    private boolean tabbable;
    private int tabIndex;
    private CursorKind cursor;

    // State the root maintains
    private boolean hovered;
    private boolean pressed;
    private boolean focused;

    // Layout inputs
    private Insets margin;
    private float minWidth;
    private float maxWidth;
    private float minHeight;
    private float maxHeight;
    private float explicitWidth;
    private float explicitHeight;
    private HorizontalAlignment horizontalAlignment;
    private VerticalAlignment verticalAlignment;
    private Paint background;

    // Layout outputs
    private float desiredWidth;
    private float desiredHeight;
    private Rect bounds;
    private boolean layoutDirty;

    public Node(final CoreUi ui, final CustomComponent<?> facade, final ComponentHooks hooks,
                final ComponentBehavior<?> behavior) {
        this.ui = Objects.requireNonNull(ui, "ui");
        this.facade = Objects.requireNonNull(facade, "facade");
        this.hooks = Objects.requireNonNull(hooks, "hooks");
        this.behavior = behavior;
        this.visible = true;
        this.enabled = true;
        this.margin = Insets.NONE;
        this.minWidth = 0f;
        this.maxWidth = Float.POSITIVE_INFINITY;
        this.minHeight = 0f;
        this.maxHeight = Float.POSITIVE_INFINITY;
        this.explicitWidth = UNSET;
        this.explicitHeight = UNSET;
        this.horizontalAlignment = HorizontalAlignment.LEFT;
        this.verticalAlignment = VerticalAlignment.TOP;
        this.background = Paint.NONE;
        this.bounds = Rect.ZERO;
        this.layoutDirty = true;
    }

    public final CoreUi ui() {
        return this.ui;
    }

    public final CustomComponent<?> facade() {
        return this.facade;
    }

    protected final ComponentHooks hooks() {
        return this.hooks;
    }

    // ------------------------------------------------------------------
    // Layout
    // ------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    public final void measure(final LayoutPass pass, final float availableWidth, final float availableHeight) {
        if (!this.visible) {
            this.desiredWidth = 0f;
            this.desiredHeight = 0f;
            return;
        }

        float width = Float.isNaN(this.explicitWidth) ? availableWidth - this.margin.horizontal() : this.explicitWidth;
        float height = Float.isNaN(this.explicitHeight) ? availableHeight - this.margin.vertical() : this.explicitHeight;
        width = Math.max(0f, Math.min(width, this.maxWidth));
        height = Math.max(0f, Math.min(height, this.maxHeight));

        final float w = width;
        final float h = height;
        final Size content = this.behavior == null
                ? this.hooks.measure(pass, w, h)
                : this.behavior.measure(this.facade, pass, w, h, () -> this.hooks.measure(pass, w, h));
        this.desiredWidth = this.clampWidth(Float.isNaN(this.explicitWidth) ? content.width() : this.explicitWidth);
        this.desiredHeight = this.clampHeight(Float.isNaN(this.explicitHeight) ? content.height() : this.explicitHeight);
    }

    public final void arrange(final LayoutPass pass, final Rect slot) {
        this.layoutDirty = false;
        if (!this.visible) {
            this.bounds = new Rect(slot.x(), slot.y(), 0f, 0f);
            return;
        }

        final Rect inner = slot.shrink(this.margin);

        final float width = this.horizontalAlignment == HorizontalAlignment.STRETCH && Float.isNaN(this.explicitWidth)
                ? this.clampWidth(inner.width())
                : this.desiredWidth;
        final float height = this.verticalAlignment == VerticalAlignment.STRETCH && Float.isNaN(this.explicitHeight)
                ? this.clampHeight(inner.height())
                : this.desiredHeight;

        final float x = switch (this.horizontalAlignment) {
            case LEFT, STRETCH -> inner.x();
            case CENTER -> inner.x() + (inner.width() - width) * 0.5f;
            case RIGHT -> inner.right() - width;
        };
        final float y = switch (this.verticalAlignment) {
            case TOP, STRETCH -> inner.y();
            case MIDDLE -> inner.y() + (inner.height() - height) * 0.5f;
            case BOTTOM -> inner.bottom() - height;
        };

        this.bounds = new Rect(x, y, width, height);
        this.hooks.arrange(pass, this.bounds);
    }

    @Override
    public final void invalidateLayout() {
        Node node = this;
        while (node != null && !node.layoutDirty) {
            node.layoutDirty = true;
            node = node.parent;
        }
        if (this.root != null) {
            this.root.requestLayout();
        }
    }

    public final boolean isLayoutDirty() {
        return this.layoutDirty;
    }

    @Override
    public final float desiredWidth() {
        return this.desiredWidth;
    }

    @Override
    public final float desiredHeight() {
        return this.desiredHeight;
    }

    public final float outerWidth() {
        return this.visible ? this.desiredWidth + this.margin.horizontal() : 0f;
    }

    public final float outerHeight() {
        return this.visible ? this.desiredHeight + this.margin.vertical() : 0f;
    }

    @Override
    public final Rect bounds() {
        return this.bounds;
    }

    private float clampWidth(final float width) {
        return Math.max(this.minWidth, Math.min(this.maxWidth, width));
    }

    private float clampHeight(final float height) {
        return Math.max(this.minHeight, Math.min(this.maxHeight, height));
    }

    // ------------------------------------------------------------------
    // Painting and hit testing
    // ------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    public final void paint(final CanvasPainter painter) {
        if (!this.visible || this.bounds.isEmpty()) {
            return;
        }
        this.background.paint(painter, this.bounds);
        if (this.behavior == null) {
            this.hooks.paint(painter, this.bounds);
        } else {
            final Rect paintBounds = this.bounds;
            this.behavior.paint(this.facade, painter, paintBounds, () -> this.hooks.paint(painter, paintBounds));
        }
        this.paintChildren(painter);
    }

    protected void paintChildren(final CanvasPainter painter) {
    }

    /**
     * Returns the deepest enabled, visible node at the point, or {@code null}.
     */
    public Node hitTest(final float x, final float y) {
        if (!this.visible || !this.enabled || !this.bounds.contains(x, y)) {
            return null;
        }
        return this;
    }

    // ------------------------------------------------------------------
    // Events
    // ------------------------------------------------------------------

    @Override
    public final <E extends Event> Subscription on(final EventType<E> type, final Phase phase,
                                                   final EventHandler<? super E> handler, final Object owner) {
        if (this.dispatcher == null) {
            this.dispatcher = new EventDispatcher();
        }
        return this.dispatcher.subscribe(type, phase, handler, owner);
    }

    @Override
    public final void removeHandlers(final Object owner) {
        if (this.dispatcher != null) {
            this.dispatcher.removeOwner(owner);
        }
    }

    public final EventDispatcher dispatcherOrNull() {
        return this.dispatcher;
    }

    /**
     * Delivers an event targeted at this node, bubbling if its type does.
     */
    public final <E extends EventImpl> E fire(final EventType<? super E> type, final E event) {
        return EventRouter.dispatch(type, event, this);
    }

    /**
     * Runs this component's default behavior for an event, through its behavior if it has one.
     */
    @SuppressWarnings("unchecked")
    public final void defaultAction(final EventType<?> type, final Event event) {
        if (this.behavior == null) {
            this.hooks.handle(type, event);
            return;
        }
        final Next next = () -> this.hooks.handle(type, event);
        if (type == UIEvents.POINTER_ENTER) {
            this.behavior.onPointerEnter(this.facade, (PointerEvent) event, next);
        } else if (type == UIEvents.POINTER_EXIT) {
            this.behavior.onPointerExit(this.facade, (PointerEvent) event, next);
        } else if (type == UIEvents.POINTER_MOVE) {
            this.behavior.onPointerMove(this.facade, (PointerEvent) event, next);
        } else if (type == UIEvents.POINTER_PRESS) {
            this.behavior.onPointerPress(this.facade, (PointerButtonEvent) event, next);
        } else if (type == UIEvents.POINTER_RELEASE) {
            this.behavior.onPointerRelease(this.facade, (PointerButtonEvent) event, next);
        } else if (type == UIEvents.CLICK) {
            this.behavior.onClick(this.facade, (PointerButtonEvent) event, next);
        } else if (type == UIEvents.DRAG_START) {
            this.behavior.onDragStart(this.facade, (DragEvent) event, next);
        } else if (type == UIEvents.DRAG) {
            this.behavior.onDrag(this.facade, (DragEvent) event, next);
        } else if (type == UIEvents.DRAG_END) {
            this.behavior.onDragEnd(this.facade, (DragEvent) event, next);
        } else if (type == UIEvents.SCROLL) {
            this.behavior.onScroll(this.facade, (ScrollEvent) event, next);
        } else if (type == UIEvents.KEY) {
            this.behavior.onKey(this.facade, (KeyEvent) event, next);
        } else if (type == UIEvents.CHAR) {
            this.behavior.onChar(this.facade, (CharEvent) event, next);
        } else if (type == UIEvents.FOCUS_GAINED) {
            this.behavior.onFocusGained(this.facade, (FocusEvent) event, next);
        } else if (type == UIEvents.FOCUS_LOST) {
            this.behavior.onFocusLost(this.facade, (FocusEvent) event, next);
        } else {
            next.proceed();
        }
    }

    // ------------------------------------------------------------------
    // Theme
    // ------------------------------------------------------------------

    public final Set<String> requiredStyleKeys() {
        return this.hooks.requiredStyleKeys();
    }

    public final void applyTheme(final Theme theme) {
        this.theme = Objects.requireNonNull(theme, "theme");
        this.hooks.applyTheme(theme);
        this.invalidateLayout();
    }

    @Override
    public final Theme theme() {
        return this.theme;
    }

    // ------------------------------------------------------------------
    // Tree
    // ------------------------------------------------------------------

    public final ContainerNode parentNode() {
        return this.parent;
    }

    @Override
    public final Container<?> parent() {
        return this.parent == null ? null : (Container<?>) this.parent.facade();
    }

    final void setParent(final ContainerNode parent) {
        this.parent = parent;
    }

    public final Root root() {
        return this.root;
    }

    @Override
    public final boolean isAttached() {
        return this.root != null;
    }

    final void attach(final Root root) {
        this.root = root;
        if (root.theme() != null) {
            this.applyTheme(root.theme());
        }
    }

    final void detach() {
        this.root = null;
        this.hovered = false;
        this.pressed = false;
        this.focused = false;
    }

    @Override
    public final boolean requestFocus() {
        return this.root != null && this.root.requestFocus(this);
    }

    public final boolean acceptsTextInput() {
        return this.hooks.acceptsTextInput();
    }

    // ------------------------------------------------------------------
    // Root-maintained state
    // ------------------------------------------------------------------

    @Override
    public final boolean isHovered() {
        return this.hovered;
    }

    final void setHovered(final boolean hovered) {
        this.hovered = hovered;
    }

    @Override
    public final boolean isPressed() {
        return this.pressed;
    }

    final void setPressed(final boolean pressed) {
        this.pressed = pressed;
    }

    @Override
    public final boolean isFocused() {
        return this.focused;
    }

    final void setFocused(final boolean focused) {
        this.focused = focused;
    }

    // ------------------------------------------------------------------
    // Properties
    // ------------------------------------------------------------------

    @Override
    public final String id() {
        return this.id;
    }

    @Override
    public final void setId(final String id) {
        this.id = id;
    }

    @Override
    public final boolean isVisible() {
        return this.visible;
    }

    @Override
    public final void setVisible(final boolean visible) {
        if (this.visible != visible) {
            this.visible = visible;
            this.invalidateLayout();
            this.treeChanged();
        }
    }

    @Override
    public final boolean isEnabled() {
        return this.enabled;
    }

    @Override
    public final void setEnabled(final boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            this.treeChanged();
        }
    }

    @Override
    public final boolean isFocusable() {
        return this.focusable;
    }

    @Override
    public final void setFocusable(final boolean focusable) {
        this.focusable = focusable;
        this.treeChanged();
    }

    @Override
    public final boolean isTabbable() {
        return this.tabbable;
    }

    @Override
    public final void setTabbable(final boolean tabbable) {
        this.tabbable = tabbable;
        this.treeChanged();
    }

    @Override
    public final int tabIndex() {
        return this.tabIndex;
    }

    @Override
    public final void setTabIndex(final int tabIndex) {
        this.tabIndex = tabIndex;
        this.treeChanged();
    }

    @Override
    public final CursorKind cursor() {
        return this.cursor;
    }

    @Override
    public final void setCursor(final CursorKind cursor) {
        this.cursor = cursor;
    }

    @Override
    public final Insets margin() {
        return this.margin;
    }

    @Override
    public final void setMargin(final Insets margin) {
        this.margin = Objects.requireNonNull(margin, "margin");
        this.invalidateLayout();
    }

    @Override
    public final float minWidth() {
        return this.minWidth;
    }

    @Override
    public final float maxWidth() {
        return this.maxWidth;
    }

    @Override
    public final float minHeight() {
        return this.minHeight;
    }

    @Override
    public final float maxHeight() {
        return this.maxHeight;
    }

    @Override
    public final void setMinSize(final float minWidth, final float minHeight) {
        this.minWidth = minWidth;
        this.minHeight = minHeight;
        this.invalidateLayout();
    }

    @Override
    public final void setMaxSize(final float maxWidth, final float maxHeight) {
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        this.invalidateLayout();
    }

    @Override
    public final void setWidth(final float width) {
        this.explicitWidth = width;
        this.invalidateLayout();
    }

    @Override
    public final void setHeight(final float height) {
        this.explicitHeight = height;
        this.invalidateLayout();
    }

    @Override
    public final HorizontalAlignment horizontalAlignment() {
        return this.horizontalAlignment;
    }

    @Override
    public final VerticalAlignment verticalAlignment() {
        return this.verticalAlignment;
    }

    @Override
    public final void setAlignment(final HorizontalAlignment horizontal, final VerticalAlignment vertical) {
        this.horizontalAlignment = Objects.requireNonNull(horizontal, "horizontal");
        this.verticalAlignment = Objects.requireNonNull(vertical, "vertical");
        this.invalidateLayout();
    }

    @Override
    public final Paint background() {
        return this.background;
    }

    @Override
    public final void setBackground(final Paint background) {
        this.background = Objects.requireNonNull(background, "background");
    }

    private void treeChanged() {
        if (this.root != null) {
            this.root.treeChanged();
        }
    }

    @Override
    public String toString() {
        return "Node[" + this.facade + "]";
    }
}
