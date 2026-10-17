package org.inspirenxe.strata.core.node;

import org.inspirenxe.strata.api.component.ComponentSnapshot;
import org.inspirenxe.strata.api.component.Container;
import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.component.Tooltip;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.layout.VerticalAlignment;
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
import org.inspirenxe.strata.api.event.EventTypes;
import org.inspirenxe.strata.api.extension.ComponentBehavior;
import org.inspirenxe.strata.api.extension.peer.ComponentHooks;
import org.inspirenxe.strata.api.extension.peer.ComponentPeer;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.Environment;
import org.inspirenxe.strata.api.extension.Next;
import org.inspirenxe.strata.api.geom.Insets;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.geom.Size;
import org.inspirenxe.strata.api.theme.Border;
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
 * {@link #facade()}, a {@link CustomComponent} whose {@code final} methods delegate here. Core reaches the facade's
 * protected hooks only through {@link ComponentHooks}, wrapped by the component's {@link ComponentBehavior} when it
 * has one.
 *
 * <p>A new node is visible, enabled, not focusable and not tabbable, with tab index 0, no cursor, no margin, no
 * size limits, no fixed size, left and top alignment, no offset, no background, no border and no tooltip.</p>
 *
 * <p>Layout is incremental. A node measured again with the same available space, with nothing inside it changed
 * since, keeps its last desired size without calling its hook. A node arranged again into the same slot, with
 * nothing inside it changed since, keeps its bounds without arranging its children. {@link #invalidateLayout()}
 * marks the node and every ancestor as changed.</p>
 *
 * <p>Every method must be called on the UI thread.</p>
 */
public class Node implements ComponentPeer {
    /**
     * The value of a fixed width or height that has not been set.
     */
    protected static final float UNSET = Float.NaN;

    private final ComponentFactoryImpl ui;
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
    private float offsetX;
    private float offsetY;
    private Paint background;
    private Border border;
    private Tooltip tooltip;

    // Layout outputs, and what they were computed from
    private float desiredWidth;
    private float desiredHeight;
    private Rect bounds;
    private boolean measureDirty;
    private boolean arrangeDirty;
    private float lastAvailableWidth;
    private float lastAvailableHeight;
    private Rect lastSlot;

    /**
     * Creates the node behind a component. Only {@link ComponentFactoryImpl#createPeer} and subclasses call this.
     *
     * @param ui the factory that owns the component
     * @param facade the public component this node implements
     * @param hooks the component's protected hooks
     * @param behavior the decorator around those hooks, or {@code null} for none
     */
    public Node(final ComponentFactoryImpl ui, final CustomComponent<?> facade, final ComponentHooks hooks,
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
        this.border = Border.NONE;
        this.bounds = Rect.ZERO;
        this.measureDirty = true;
        this.arrangeDirty = true;
        this.lastAvailableWidth = Float.NaN;
        this.lastAvailableHeight = Float.NaN;
    }

    /**
     * Returns the factory that made this component. A component can only join trees in that factory's host.
     *
     * @return the factory
     */
    public final ComponentFactoryImpl factory() {
        return this.ui;
    }

    /**
     * Returns the public component this node implements. Events and callers see this object, never the node.
     *
     * @return the component
     */
    public final CustomComponent<?> facade() {
        return this.facade;
    }

    /**
     * Returns the component's protected hooks, without its behavior.
     *
     * @return the hooks
     */
    protected final ComponentHooks hooks() {
        return this.hooks;
    }

    // ------------------------------------------------------------------
    // Layout
    // ------------------------------------------------------------------

    /**
     * Measures the component and stores its desired size. The hook gets the available space minus the margin,
     * replaced by the fixed size where one is set and capped by the maximum size. The result is replaced by the
     * fixed size where one is set, then clamped to the minimum and maximum. A hidden component measures as 0 by 0
     * without calling its hook. Nothing is measured if nothing changed since the last measure with the same space.
     *
     * @param pass the layout context
     * @param availableWidth the width the parent offers, margin included; may be infinite
     * @param availableHeight the height the parent offers, margin included; may be infinite
     */
    @SuppressWarnings("unchecked")
    public final void measure(final LayoutPass pass, final float availableWidth, final float availableHeight) {
        if (!this.measureDirty && availableWidth == this.lastAvailableWidth && availableHeight == this.lastAvailableHeight) {
            return;
        }
        this.lastAvailableWidth = availableWidth;
        this.lastAvailableHeight = availableHeight;
        this.measureDirty = false;
        // A new desired size can change the bounds even in the same slot
        this.arrangeDirty = true;
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

    /**
     * Places the component in a slot and arranges its children. The slot is shrunk by the margin; the component
     * then takes its desired size, or the whole inner slot on an axis set to stretch with no fixed size (still
     * clamped), and is aligned inside, then moved by its offset. A hidden component gets an empty rectangle at the
     * slot's origin. Nothing is arranged if nothing changed since the last arrange into the same slot.
     *
     * @param pass the layout context
     * @param slot the space the parent assigns, margin included, in the host's coordinates
     */
    public final void arrange(final LayoutPass pass, final Rect slot) {
        if (!this.arrangeDirty && slot.equals(this.lastSlot)) {
            return;
        }
        this.arrangeDirty = false;
        this.lastSlot = slot;
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

        this.bounds = new Rect(x + this.offsetX, y + this.offsetY, width, height);
        this.hooks.arrange(pass, this.bounds);
    }

    /**
     * Returns the slot the parent gave in the last arrange, margin included.
     *
     * @return the slot, or {@link Rect#ZERO} before the first arrange
     */
    public final Rect slot() {
        return this.lastSlot == null ? Rect.ZERO : this.lastSlot;
    }

    /**
     * Arranges the component again in the slot it last had, as {@link #invalidateArrangement()} asks. Does nothing
     * if it was never arranged.
     *
     * @param pass the layout context
     */
    final void rearrange(final LayoutPass pass) {
        if (this.lastSlot != null) {
            this.arrangeDirty = true;
            this.arrange(pass, this.lastSlot);
        }
    }

    @Override
    public final void invalidateLayout() {
        for (Node node = this; node != null; node = node.parent) {
            node.measureDirty = true;
            node.arrangeDirty = true;
        }
        if (this.root != null) {
            this.root.requestLayout();
        }
    }

    @Override
    public final void invalidateArrangement() {
        if (this.root != null) {
            this.root.requestArrangement(this);
        }
    }

    /**
     * Returns whether this component, or something inside it, changed in a way that needs a new measure.
     *
     * @return true until the next measure
     */
    public final boolean isLayoutDirty() {
        return this.measureDirty;
    }

    @Override
    public final float desiredWidth() {
        return this.desiredWidth;
    }

    @Override
    public final float desiredHeight() {
        return this.desiredHeight;
    }

    /**
     * Returns the desired width plus the left and right margin.
     *
     * @return the outer width, or 0 when hidden
     */
    public final float outerWidth() {
        return this.visible ? this.desiredWidth + this.margin.horizontal() : 0f;
    }

    /**
     * Returns the desired height plus the top and bottom margin.
     *
     * @return the outer height, or 0 when hidden
     */
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

    /**
     * Paints the component: its background and border, then its paint hook (through its behavior if it has one),
     * then its children, then its over-children hook. Hidden components and components with empty bounds paint
     * nothing.
     *
     * @param painter the painter for this frame
     */
    @SuppressWarnings("unchecked")
    public final void paint(final CanvasPainter painter) {
        if (!this.visible || this.bounds.isEmpty()) {
            return;
        }
        this.background.paint(painter, this.bounds);
        this.border.paint(painter, this.bounds);
        if (this.behavior == null) {
            this.hooks.paint(painter, this.bounds);
        } else {
            final Rect paintBounds = this.bounds;
            this.behavior.paint(this.facade, painter, paintBounds, () -> this.hooks.paint(painter, paintBounds));
        }
        this.paintChildren(painter);
        this.hooks.paintOverChildren(painter, this.bounds);
    }

    /**
     * Paints the children after the component itself. A plain node has none; {@link ContainerNode} overrides this.
     *
     * @param painter the painter for this frame
     */
    protected void paintChildren(final CanvasPainter painter) {
    }

    /**
     * Finds the deepest visible, enabled node at a point. A disabled or hidden node hides its whole subtree from
     * the pointer.
     *
     * @param x the x, in the host's coordinates
     * @param y the y, in the host's coordinates
     * @return the node hit, or {@code null} if the point misses this node
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
        return this.dispatcher.subscribe(type, phase, handler, owner, false);
    }

    @Override
    public final <E extends Event> Subscription onWithSnapshot(final EventType<E> type, final Phase phase,
                                                               final EventHandler<? super E> handler,
                                                               final Object owner) {
        if (this.dispatcher == null) {
            this.dispatcher = new EventDispatcher();
        }
        return this.dispatcher.subscribe(type, phase, handler, owner, true);
    }

    /**
     * Records the component's current state, its own only, with the component's
     * {@code CustomComponent.snapshotState} as the {@linkplain ComponentSnapshot#state() state}. The behavior, if
     * any, is not consulted. {@link EventRouter} calls this before an event that has snapshots reaches any handler.
     *
     * @return a new snapshot
     */
    public final ComponentSnapshot snapshot() {
        return new ComponentSnapshot(this.facade, this.id, this.visible, this.enabled, this.focusable, this.tabbable,
                this.tabIndex, this.cursor, this.hovered, this.pressed, this.focused, this.margin, this.minWidth,
                this.maxWidth, this.minHeight, this.maxHeight, this.horizontalAlignment, this.verticalAlignment,
                this.background, this.bounds, this.desiredWidth, this.desiredHeight, this.hooks.snapshotState());
    }

    @Override
    public final void removeHandlers(final Object owner) {
        if (this.dispatcher != null) {
            this.dispatcher.removeOwner(owner);
        }
    }

    /**
     * Returns this component's handlers. The dispatcher is created on the first subscription.
     *
     * @return the dispatcher, or {@code null} if nothing ever subscribed
     */
    public final EventDispatcher dispatcherOrNull() {
        return this.dispatcher;
    }

    /**
     * Delivers an event aimed at this node through {@link EventRouter}, bubbling if its type does.
     *
     * @param <E> the event class
     * @param type the event type
     * @param event the event, whose target must be this node's facade
     * @return the same event, so the caller can check whether it was canceled
     */
    public final <E extends EventImpl> E fire(final EventType<? super E> type, final E event) {
        return EventRouter.dispatch(type, event, this);
    }

    /**
     * Runs this component's default behavior for an event. With a behavior, the matching behavior hook runs and
     * gets the component's own handling as its {@code next}; event types without a behavior hook go straight to
     * the component.
     *
     * @param type the event type
     * @param event the event, of the class that {@code type} describes
     */
    @SuppressWarnings("unchecked")
    public final void defaultAction(final EventType<?> type, final Event event) {
        if (this.behavior == null) {
            this.hooks.handle(type, event);
            return;
        }
        final Next next = () -> this.hooks.handle(type, event);
        if (type == EventTypes.POINTER_ENTER) {
            this.behavior.onPointerEnter(this.facade, (PointerEvent) event, next);
        } else if (type == EventTypes.POINTER_EXIT) {
            this.behavior.onPointerExit(this.facade, (PointerEvent) event, next);
        } else if (type == EventTypes.POINTER_MOVE) {
            this.behavior.onPointerMove(this.facade, (PointerEvent) event, next);
        } else if (type == EventTypes.POINTER_PRESS) {
            this.behavior.onPointerPress(this.facade, (PointerButtonEvent) event, next);
        } else if (type == EventTypes.POINTER_RELEASE) {
            this.behavior.onPointerRelease(this.facade, (PointerButtonEvent) event, next);
        } else if (type == EventTypes.CLICK) {
            this.behavior.onClick(this.facade, (PointerButtonEvent) event, next);
        } else if (type == EventTypes.DRAG_START) {
            this.behavior.onDragStart(this.facade, (DragEvent) event, next);
        } else if (type == EventTypes.DRAG) {
            this.behavior.onDrag(this.facade, (DragEvent) event, next);
        } else if (type == EventTypes.DRAG_END) {
            this.behavior.onDragEnd(this.facade, (DragEvent) event, next);
        } else if (type == EventTypes.SCROLL) {
            this.behavior.onScroll(this.facade, (ScrollEvent) event, next);
        } else if (type == EventTypes.KEY) {
            this.behavior.onKey(this.facade, (KeyEvent) event, next);
        } else if (type == EventTypes.CHAR) {
            this.behavior.onChar(this.facade, (CharEvent) event, next);
        } else if (type == EventTypes.FOCUS_GAINED) {
            this.behavior.onFocusGained(this.facade, (FocusEvent) event, next);
        } else if (type == EventTypes.FOCUS_LOST) {
            this.behavior.onFocusLost(this.facade, (FocusEvent) event, next);
        } else {
            next.proceed();
        }
    }

    // ------------------------------------------------------------------
    // Theme
    // ------------------------------------------------------------------

    /**
     * Returns the style keys the component cannot draw without. The host checks the whole tree against its theme
     * before showing it.
     *
     * @return the required keys, possibly empty
     */
    public final Set<String> requiredStyleKeys() {
        return this.hooks.requiredStyleKeys();
    }

    /**
     * Gives the component a theme and marks its layout dirty, since styles can change sizes.
     *
     * @param theme the theme
     */
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

    /**
     * Returns the node of the container holding this component.
     *
     * @return the parent node, or {@code null} for a root's content or a component not in any container
     */
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

    /**
     * Returns the root of the screen this component is shown in.
     *
     * @return the root, or {@code null} while the component is not attached to a shown tree
     */
    public final Root root() {
        return this.root;
    }

    @Override
    public final boolean isAttached() {
        return this.root != null;
    }

    @Override
    public final Environment environment() {
        return this.ui.environment();
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

    /**
     * Returns whether the component takes typed text. The root turns the engine's text input on while such a
     * component has focus.
     *
     * @return true for text-entry components
     */
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
    public final boolean isHoverWithin() {
        return this.root != null && this.root.isHoverWithin(this);
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
    public final float offsetX() {
        return this.offsetX;
    }

    @Override
    public final float offsetY() {
        return this.offsetY;
    }

    @Override
    public final void setOffset(final float offsetX, final float offsetY) {
        if (this.offsetX != offsetX || this.offsetY != offsetY) {
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.invalidateArrangement();
        }
    }

    @Override
    public final Border border() {
        return this.border;
    }

    @Override
    public final void setBorder(final Border border) {
        this.border = Objects.requireNonNull(border, "border");
    }

    @Override
    public final Tooltip tooltip() {
        return this.tooltip;
    }

    @Override
    public final void setTooltip(final Tooltip tooltip) {
        this.tooltip = tooltip;
        if (this.root != null) {
            this.root.tooltipChanged(this);
        }
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
