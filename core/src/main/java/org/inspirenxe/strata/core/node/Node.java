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
import org.inspirenxe.strata.api.component.ComponentSnapshot;
import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.component.Focusable;
import org.inspirenxe.strata.api.component.HasCursor;
import org.inspirenxe.strata.api.component.HasTooltip;
import org.inspirenxe.strata.api.component.Tooltip;
import org.inspirenxe.strata.api.event.Event;
import org.inspirenxe.strata.api.event.EventHandler;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.Subscription;
import org.inspirenxe.strata.api.event.cause.CauseStack;
import org.inspirenxe.strata.api.event.cause.Origin;
import org.inspirenxe.strata.api.event.control.ControlEvent;
import org.inspirenxe.strata.api.event.control.RequestEvent;
import org.inspirenxe.strata.api.event.control.ValueEvent;
import org.inspirenxe.strata.api.event.control.ValueRequestEvent;
import org.inspirenxe.strata.api.extension.Environment;
import org.inspirenxe.strata.api.extension.peer.ComponentPeer;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.input.TextInputKind;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.layout.VerticalAlignment;
import org.inspirenxe.strata.api.state.PropertyKey;
import org.inspirenxe.strata.api.state.Source;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Border;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.core.event.ControlEventImpl;
import org.inspirenxe.strata.core.event.EventDispatcher;
import org.inspirenxe.strata.core.event.EventImpl;
import org.inspirenxe.strata.core.event.EventRouter;
import org.inspirenxe.strata.core.event.LifecycleEventImpl;
import org.inspirenxe.strata.core.event.MeasureEventImpl;
import org.inspirenxe.strata.core.event.PaintEventImpl;
import org.inspirenxe.strata.core.event.RequestEventImpl;
import org.inspirenxe.strata.core.event.ValueEventImpl;
import org.inspirenxe.strata.core.event.ValueRequestEventImpl;
import org.inspirenxe.strata.core.paint.CanvasPainter;
import org.inspirenxe.strata.core.state.Binding;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Core's half of every component: state, layout bookkeeping and event routing. The public object is the
 * {@link #facade()}, whose {@code final} {@link Component} methods forward here: a {@link BuiltInComponent} for
 * Strata's own components, or a developer's {@code CustomComponent}, for which this node is the peer. Core reaches the
 * facade's protected hooks only through the hooks object the facade hands its node.
 *
 * <p>A new node has no id, and is visible, enabled, not focusable and not tabbable, with tab index 0, no cursor, no
 * margin, no
 * size limits, no fixed size, left and top alignment, no offset, no background, no border, no tooltip and z index
 * 0.</p>
 *
 * <p>Layout is incremental. A node measured again with the same available space, with nothing inside it changed
 * since, keeps its last desired size without calling its hook. A node arranged again into the same slot, with
 * nothing inside it changed since, keeps its bounds without arranging its children. {@link #invalidateLayout()}
 * marks the node and every ancestor as changed.</p>
 *
 * <p>Every change that shows asks the host for a frame (design 4.47): layout invalidation through the root, and
 * {@link #repaint()} for the rest, which the background, border, cursor and z index setters and every report call.
 * {@link #repaintAfter(long)} registers a change the clock brings.</p>
 *
 * <p>Every method must be called on the UI thread. While the node is attached, its changes check that through
 * {@link #checkThread()}.</p>
 */
public class Node implements ComponentPeer {
    /**
     * The value of a fixed width or height that has not been set.
     */
    protected static final float UNSET = Float.NaN;

    private final ComponentFactoryImpl ui;
    private final Component<?> facade;
    private final NodeHooks hooks;

    private String id;
    private ContainerNode parent;
    private Root root;
    // Whether the attach hook and ATTACHED ran since the last detach, so attach and detach always come in pairs
    private boolean attachedFired;
    private EventDispatcher dispatcher;
    // By key name, made on the first bind
    private Map<String, Binding<?>> bindings;
    private PaintEventImpl paintEvent;
    private boolean content;
    private MeasureEventImpl measureEvent;
    private Theme theme;
    // Made on the first style state set or looked up
    private StyleStates styleStates;

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
    private Node focusDelegate;
    private Node focusOwner;

    // Layout inputs
    private Insets margin;
    // Set by setMargin; the theme's margin applies until then
    private boolean marginSet;
    private Function<Theme, Insets> themeMargin;
    private static final Theme NO_THEME = Theme.builder("strata:none").build();
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

    private float opacity;

    // Drawing order among siblings: the z index, then the sequence number the parent gave when adding or raising
    private int zIndex;
    private long paintSequence;

    // Layout outputs, and what they were computed from
    private float desiredWidth;
    private float desiredHeight;
    private Rect bounds;
    private boolean measureDirty;
    // The content minimum from the hook, kept until the layout is invalidated
    private float contentMinWidth;
    private float contentMinHeight;
    private boolean minimumDirty;
    private boolean arrangeDirty;
    private float lastAvailableWidth;
    private float lastAvailableHeight;
    private Rect lastSlot;

    /**
     * Creates the node behind a component. {@link ComponentFactoryImpl#createPeer}, {@link BuiltInComponent} and
     * {@link ContainerNode} call this.
     *
     * @param ui the factory that owns the component
     * @param facade the public component this node implements
     * @param hooks the component's protected hooks
     */
    Node(final ComponentFactoryImpl ui, final Component<?> facade, final NodeHooks hooks) {
        this.ui = Objects.requireNonNull(ui, "ui");
        this.facade = Objects.requireNonNull(facade, "facade");
        this.hooks = Objects.requireNonNull(hooks, "hooks");
        this.visible = true;
        this.enabled = true;
        this.opacity = 1f;
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
        this.minimumDirty = true;
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
     * Returns the cause stack of this node's factory, which its events read their cause from.
     *
     * @return the stack
     */
    public final CauseStack causes() {
        return this.ui.causes();
    }

    /**
     * Returns the public component this node implements. Events and callers see this object, never the node.
     *
     * @return the component
     */
    public final Component<?> facade() {
        return this.facade;
    }

    /**
     * Returns the component's protected hooks.
     *
     * @return the hooks
     */
    final NodeHooks hooks() {
        return this.hooks;
    }

    /**
     * Returns whether this node's children are a composite's private parts, whose control events stop here unless
     * they came out of content. See {@link org.inspirenxe.strata.core.event.EventRouter#next}.
     *
     * @return false for a leaf; {@link ContainerNode} answers for itself
     */
    public boolean isComposite() {
        return false;
    }

    // ------------------------------------------------------------------
    // Layout
    // ------------------------------------------------------------------

    /**
     * Measures the component and stores its desired size. The hook gets the available space minus the margin,
     * replaced by the fixed size where one is set and capped by the maximum size. The result is replaced by the
     * fixed size where one is set, then clamped to the minimum and maximum. A hidden component measures as 0 by 0
     * without calling its hook. Nothing is measured if nothing changed since the last measure with the same space.
     * If the hook or a {@link Component#MEASURED} handler throws, the component stays marked for measuring, so the
     * next layout tries again.
     *
     * @param pass the layout context
     * @param availableWidth the width the parent offers, margin included; may be infinite
     * @param availableHeight the height the parent offers, margin included; may be infinite
     */
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
        boolean done = false;
        try {
            this.measureVisible(pass, availableWidth, availableHeight);
            done = true;
        } finally {
            if (!done) {
                this.measureDirty = true;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void measureVisible(final LayoutPass pass, final float availableWidth, final float availableHeight) {
        float width = Float.isNaN(this.explicitWidth) ? availableWidth - this.margin.horizontal() : this.explicitWidth;
        float height = Float.isNaN(this.explicitHeight) ? availableHeight - this.margin.vertical() : this.explicitHeight;
        width = Math.max(0f, Math.min(width, this.maxWidth));
        height = Math.max(0f, Math.min(height, this.maxHeight));

        final float w = width;
        final float h = height;
        final Size content = this.hooks.measure(pass, w, h);
        float contentWidth = content.width();
        float contentHeight = content.height();
        final EventDispatcher handlers = this.dispatcher;
        if (handlers != null && handlers.hasHandlers(Component.MEASURED)) {
            if (this.measureEvent == null) {
                this.measureEvent = new MeasureEventImpl(this.facade);
                this.measureEvent.setCauses(this.ui.causes());
            }
            final MeasureEventImpl event = this.measureEvent.begin(pass, w, h, contentWidth, contentHeight);
            handlers.fire(Component.MEASURED, event);
            contentWidth = event.width();
            contentHeight = event.height();
        }
        this.desiredWidth = this.clampWidth(Float.isNaN(this.explicitWidth) ? contentWidth : this.explicitWidth);
        this.desiredHeight = this.clampHeight(Float.isNaN(this.explicitHeight) ? contentHeight : this.explicitHeight);
    }

    /**
     * Returns the smallest space the component can work in, margin included, as
     * {@link org.inspirenxe.strata.api.extension.MeasureContext#minimumSize} describes. The hook runs only when the
     * layout was invalidated since it last ran; if it throws, it runs again next time. A hidden component has a zero
     * minimum without calling its hook.
     *
     * @param pass the layout context
     * @return the outer minimum
     */
    public final Size minimumSize(final LayoutPass pass) {
        if (!this.visible) {
            return Size.ZERO;
        }
        if (this.minimumDirty) {
            final Size content = this.hooks.measureMinimum(pass);
            this.contentMinWidth = Float.isFinite(content.width()) ? Math.max(0f, content.width()) : 0f;
            this.contentMinHeight = Float.isFinite(content.height()) ? Math.max(0f, content.height()) : 0f;
            this.minimumDirty = false;
        }
        final float width = Float.isNaN(this.explicitWidth)
                ? Math.max(this.minWidth, Math.min(this.maxWidth, this.contentMinWidth))
                : this.clampWidth(this.explicitWidth);
        final float height = Float.isNaN(this.explicitHeight)
                ? Math.max(this.minHeight, Math.min(this.maxHeight, this.contentMinHeight))
                : this.clampHeight(this.explicitHeight);
        return new Size(width + this.margin.horizontal(), height + this.margin.vertical());
    }

    /**
     * Returns where the component's first baseline lies below the top of its margin, from its baseline hook and its
     * top margin, as {@link org.inspirenxe.strata.api.extension.MeasureContext#baseline} describes.
     *
     * @return the baseline in UI units, or NaN when the component is hidden, reports none or reports one that is
     *     not finite
     */
    public final float baseline() {
        if (!this.visible) {
            return Float.NaN;
        }
        final float baseline = this.hooks.baseline();
        return Float.isFinite(baseline) ? this.margin.top() + baseline : Float.NaN;
    }

    /**
     * Places the component in a slot and arranges its children. The slot is shrunk by the margin; the component
     * then takes its desired size, or the whole inner slot on an axis set to stretch with no fixed size (still
     * clamped), and is aligned inside, then moved by its offset. {@link VerticalAlignment#BASELINE} places it as
     * {@link VerticalAlignment#TOP} does; the parents that line up baselines move the slot instead. A hidden component gets an empty rectangle at the
     * slot's origin. Nothing is arranged if nothing changed since the last arrange into the same slot. If the hook
     * throws, the component stays marked for arranging, so the next layout tries again.
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
            case TOP, STRETCH, BASELINE -> inner.y();
            case MIDDLE -> inner.y() + (inner.height() - height) * 0.5f;
            case BOTTOM -> inner.bottom() - height;
        };

        this.bounds = new Rect(x + this.offsetX, y + this.offsetY, width, height);
        boolean done = false;
        try {
            this.hooks.arrange(pass, this.bounds);
            done = true;
        } finally {
            if (!done) {
                this.arrangeDirty = true;
            }
        }
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

    /**
     * Throws if this node is attached and the calling thread is not the UI thread. Every change that would corrupt a
     * shown tree from another thread calls this first: layout invalidation, and so every setter that lays out,
     * handler registration, binding, and adding, moving and removing children. A detached node is not checked, so a
     * tree can be built on another thread and added on the UI thread.
     *
     * @throws IllegalStateException if attached and called off the UI thread
     */
    protected final void checkThread() {
        final Root current = this.root;
        if (current != null) {
            current.checkThread();
        }
    }

    @Override
    public final void invalidateLayout() {
        this.checkThread();
        for (Node node = this; node != null; node = node.parent) {
            node.measureDirty = true;
            node.minimumDirty = true;
            node.arrangeDirty = true;
        }
        if (this.root != null) {
            this.root.requestLayout();
        }
    }

    /**
     * Asks the host for a frame, because the component's look changed and its layout did not. Does nothing while
     * detached: attaching asks for a frame anyway.
     */
    @Override
    public final void repaint() {
        final Root current = this.root;
        if (current != null) {
            current.repaint();
        }
    }

    /**
     * Tells the host that the component's look changes by itself after a time, so a host that renders on demand
     * draws a frame then. A delay of 0 is {@link #repaint()}. Does nothing while detached.
     *
     * @param millis the delay in milliseconds, by the platform's clock
     * @throws IllegalArgumentException if {@code millis} is negative
     * @throws IllegalStateException if attached and called off the UI thread
     */
    @Override
    public final void repaintAfter(final long millis) {
        if (millis < 0L) {
            throw new IllegalArgumentException("A repaint delay must not be negative: " + millis);
        }
        final Root current = this.root;
        if (current == null) {
            return;
        }
        current.checkThread();
        if (millis == 0L) {
            current.repaint();
        } else {
            current.repaintAt(current.nanoTime() + millis * 1_000_000L);
        }
    }

    @Override
    public final void invalidateArrangement() {
        this.checkThread();
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
     * Paints the component. If it has {@link Component#PAINT} handlers they run first, and canceling skips the
     * component's own drawing but still paints its children. Then come its background and border, its paint hook,
     * its children, its over-children hook, and last any {@link Component#PAINTED} handlers. Hidden components,
     * components with empty bounds and components with an opacity of 0 paint nothing. A component whose opacity is
     * between 0 and 1 paints inside a {@code Canvas.pushOpacity} of it. The paint event object is made once and
     * reused.
     *
     * @param painter the painter for this frame
     */
    public final void paint(final CanvasPainter painter) {
        if (!this.visible || this.bounds.isEmpty() || this.opacity <= 0f) {
            return;
        }
        if (this.opacity >= 1f) {
            this.paintOpaque(painter);
            return;
        }
        painter.canvas().pushOpacity(this.opacity);
        try {
            this.paintOpaque(painter);
        } finally {
            painter.canvas().popOpacity();
        }
    }

    @SuppressWarnings("unchecked")
    private void paintOpaque(final CanvasPainter painter) {
        final EventDispatcher handlers = this.dispatcher;
        boolean own = true;
        if (handlers != null && handlers.hasHandlers(Component.PAINT)) {
            final PaintEventImpl event = this.paintEvent().begin(painter, this.bounds);
            handlers.fire(Component.PAINT, event);
            own = !event.isCancelled();
        }
        if (own) {
            this.background.paint(painter, this.bounds);
            this.border.paint(painter, this.bounds);
            this.hooks.paint(painter, this.bounds);
        }
        this.paintChildren(painter);
        if (own) {
            this.hooks.paintOverChildren(painter, this.bounds);
        }
        if (handlers != null && handlers.hasHandlers(Component.PAINTED)) {
            handlers.fire(Component.PAINTED, this.paintEvent().begin(painter, this.bounds));
        }
    }

    private PaintEventImpl paintEvent() {
        if (this.paintEvent == null) {
            this.paintEvent = new PaintEventImpl(this.facade);
            this.paintEvent.setCauses(this.ui.causes());
        }
        return this.paintEvent;
    }

    /**
     * Paints the children after the component itself. A plain node has none; {@link ContainerNode} overrides this.
     *
     * @param painter the painter for this frame
     */
    protected void paintChildren(final CanvasPainter painter) {
    }

    /**
     * Finds the deepest visible node at a point. A hidden node hides its whole subtree from the pointer. A disabled
     * node, or one inside a disabled node, can be hit, so the pointer stops there: {@link Root} gives it no input
     * events and shows its tooltip.
     *
     * @param x the x, in the host's coordinates
     * @param y the y, in the host's coordinates
     * @return the node hit, or {@code null} if the point misses this node
     */
    public Node hitTest(final float x, final float y) {
        if (!this.visible || !this.bounds.contains(x, y)) {
            return null;
        }
        return this;
    }

    // ------------------------------------------------------------------
    // Events
    // ------------------------------------------------------------------

    /**
     * Adds a handler. Adding or removing a {@link Component#MEASURED} handler marks the component for measuring, since
     * the handler can change the desired size and a component is not measured again while nothing changed.
     */
    @Override
    public final <E extends Event> Subscription subscribe(final EventType<E> type, final int priority,
                                                          final EventHandler<? super E> handler) {
        this.checkThread();
        if (this.dispatcher == null) {
            this.dispatcher = new EventDispatcher();
        }
        final Subscription subscription = this.dispatcher.subscribe(type, priority, handler);
        if (type != Component.MEASURED) {
            return subscription;
        }
        this.invalidateLayout();
        return () -> {
            subscription.unsubscribe();
            this.invalidateLayout();
        };
    }

    /**
     * Binds a property, replacing any binding with the same key name. On an attached node the binding starts and
     * sets the property at once; otherwise it starts when the node is attached. See {@link Binding}.
     *
     * @param <V> the type of the property's value
     * @param key the property
     * @param source the source
     */
    @Override
    public final <V> void bind(final PropertyKey<?, V> key, final Source<V> source) {
        this.checkThread();
        final Binding<V> binding = new Binding<>(this.facade, key, source, this.ui::causes);
        if (this.bindings == null) {
            this.bindings = new LinkedHashMap<>();
        }
        final Binding<?> replaced = this.bindings.put(key.name(), binding);
        if (replaced != null) {
            replaced.detach();
        }
        if (this.root != null) {
            binding.attach(this.root.bindingQueue());
            binding.evaluate();
        }
    }

    /**
     * Removes the binding with the key's name, detaching it so no state refers to it. The property keeps its value.
     * Does nothing when no binding has that name.
     *
     * @param key the property
     */
    @Override
    public final void unbind(final PropertyKey<?, ?> key) {
        this.checkThread();
        if (this.bindings == null) {
            return;
        }
        final Binding<?> removed = this.bindings.remove(key.name());
        if (removed != null) {
            removed.detach();
        }
    }

    @Override
    public final boolean isHandled(final EventType<?> type) {
        // Control events stop at the first composite above, as EventRouter.next does
        final boolean input = EventRouter.isInput(type);
        for (Node node = this; node != null; node = type.bubbles() ? EventRouter.next(node, this, input) : null) {
            final EventDispatcher handlers = node.dispatcher;
            if (handlers != null && handlers.hasHandlers(type)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public final boolean request(final EventType<?> type) {
        Node.requireClass(type, RequestEvent.class);
        // Requests are the user's: from any other origin the change goes ahead without asking
        if (!this.isHandled(type) || !(this.ui.causes().origin() instanceof Origin.User)) {
            return true;
        }
        return !Node.dispatchControl(type, new RequestEventImpl<>(this.facade), this).isCancelled();
    }

    @Override
    @SuppressWarnings("unchecked")
    public final <V> void request(final EventType<?> type, final V value, final Consumer<? super V> apply) {
        Node.requireClass(type, ValueRequestEvent.class);
        Objects.requireNonNull(apply, "apply");
        if (!this.isHandled(type) || !(this.ui.causes().origin() instanceof Origin.User)) {
            apply.accept(value);
            return;
        }
        final ValueRequestEventImpl<?, V> event = Node.dispatchControl(type, new ValueRequestEventImpl<>(this.facade,
                value), this);
        if (!event.isCancelled()) {
            apply.accept(event.value());
        }
    }

    @Override
    public final void report(final EventType<?> type) {
        Node.requireClass(type, ControlEvent.class);
        // A report is a change, and most changes show
        this.repaint();
        if (this.isHandled(type)) {
            Node.dispatchControl(type, new ControlEventImpl<>(this.facade), this);
        }
    }

    @Override
    public final <V> void report(final EventType<?> type, final V value, final V previous) {
        Node.requireClass(type, ValueEvent.class);
        this.repaint();
        if (this.isHandled(type)) {
            Node.dispatchControl(type, new ValueEventImpl<>(this.facade, value, previous), this);
        }
    }

    /**
     * Delivers a request or report whose type was checked against the event's class by the caller.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <E extends EventImpl> E dispatchControl(final EventType<?> type, final E event, final Node target) {
        return (E) EventRouter.dispatch((EventType) type, event, target);
    }

    private static void requireClass(final EventType<?> type, final Class<?> eventClass) {
        Objects.requireNonNull(type, "type");
        if (type.eventClass() != eventClass) {
            throw new IllegalArgumentException(type + " is not a " + eventClass.getSimpleName() + " type");
        }
    }

    /**
     * Records the component's current state, its own only, with the component's {@code snapshotState} hook as the
     * {@linkplain ComponentSnapshot#state() state}. {@link EventRouter} calls this before an event that has snapshots
     * reaches any handler.
     *
     * @return a new snapshot
     */
    public final ComponentSnapshot snapshot() {
        return new ComponentSnapshot(this.facade, this.visible, this.enabled, this.hovered, this.pressed,
                this.focused, this.margin, this.minWidth, this.maxWidth, this.minHeight, this.maxHeight,
                this.horizontalAlignment, this.verticalAlignment, this.bounds, this.desiredWidth, this.desiredHeight,
                this.hooks.snapshotState());
    }

    /**
     * Returns whether this node was added to its parent as content, which control events from inside it carry past
     * every composite above. See {@link BuiltInComposite#addContent} and {@code CustomComposite.addContent}.
     *
     * @return true if added as content
     */
    public final boolean isContent() {
        return this.content;
    }

    final void setContent(final boolean content) {
        this.content = content;
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
     * Runs this component's default behavior for an input event: the matching {@code on...} hook of its facade.
     *
     * @param type the event type
     * @param event the event, of the class that {@code type} describes
     */
    public final void defaultAction(final EventType<?> type, final Event event) {
        this.hooks.handle(type, event);
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
     * Gives the component a theme and marks its layout dirty, since styles can change sizes. The component gets the
     * theme with the {@linkplain ContainerNode#childTheme text style patches} of its containers applied, which is the
     * theme itself when none sets one.
     *
     * @param theme the root's theme
     */
    public final void applyTheme(final Theme theme) {
        Objects.requireNonNull(theme, "theme");
        this.theme = this.parent != null ? this.parent.childTheme(theme) : theme;
        if (this.themeMargin != null && !this.marginSet) {
            this.margin = this.themeMargin.apply(this.theme);
        }
        this.hooks.applyTheme(this.theme);
        this.invalidateLayout();
    }

    @Override
    public final Theme theme() {
        return this.theme;
    }

    /**
     * Turns one of the component's own style states on or off. The node sets {@code disabled}, {@code pressed},
     * {@code hovered} and {@code focused} itself, from its own state. A change marks nothing for layout.
     *
     * @param state the state's name
     * @param on whether the state is on
     * @throws IllegalArgumentException if the name is empty or one of the four the node sets, or the component
     *     already has 60 states of its own
     */
    @Override
    public final void setStyleState(final String state, final boolean on) {
        this.styleStates().set(state, on);
    }

    /**
     * Looks up a paint for the component's style states, as {@link StyleStates} describes: {@code key.disabled}
     * while the component is disabled, itself or through an ancestor, then {@code key.<state>} for each of its own
     * states that is on, in the order they were first set, then {@code key.pressed}, {@code key.hovered} and
     * {@code key.focused}, and last the key. The result of each combination is kept until the theme changes.
     *
     * @param key the style key
     * @param fallback returned when the theme has none of the keys tried, or the node has no theme yet
     * @return the paint
     */
    @Override
    public final Paint stylePaint(final String key, final Paint fallback) {
        Objects.requireNonNull(key, "key");
        final Theme theme = this.theme;
        if (theme == null) {
            return fallback;
        }
        final StyleStates states = this.styleStates();
        return states.paint(theme, key, fallback, this.styleCombination(states));
    }

    /**
     * Looks up a text style for the component's style states, in the same order as {@link #stylePaint}.
     *
     * @param key the style key
     * @param fallback returned when the theme has none of the keys tried, or the node has no theme yet
     * @return the text style
     */
    @Override
    public final TextStyle styleText(final String key, final TextStyle fallback) {
        Objects.requireNonNull(key, "key");
        final Theme theme = this.theme;
        if (theme == null) {
            return fallback;
        }
        final StyleStates states = this.styleStates();
        return states.text(theme, key, fallback, this.styleCombination(states));
    }

    private StyleStates styleStates() {
        if (this.styleStates == null) {
            this.styleStates = new StyleStates();
        }
        return this.styleStates;
    }

    private long styleCombination(final StyleStates states) {
        return states.combination(!this.facade.isEffectivelyEnabled(), this.pressed, this.hovered, this.focused);
    }

    // ------------------------------------------------------------------
    // Tree
    // ------------------------------------------------------------------

    @Override
    public final Optional<String> id() {
        return Optional.ofNullable(this.id);
    }

    @Override
    public final void setId(final String id) {
        this.id = id;
    }

    /**
     * Returns the node of the container or composite holding this component.
     *
     * @return the parent node, or {@code null} for a root's content, an overlay's content or a component not added
     *     anywhere
     */
    public final ContainerNode parentNode() {
        return this.parent;
    }

    @Override
    public final Component<?> parent() {
        return this.parent == null ? null : this.parent.facade();
    }

    @Override
    public final <P> Optional<P> parent(final Class<P> type) {
        Objects.requireNonNull(type, "type");
        final Component<?> parent = this.parent();
        return type.isInstance(parent) ? Optional.of(type.cast(parent)) : Optional.empty();
    }

    @Override
    public final <A> Optional<A> ancestor(final Class<A> type) {
        Objects.requireNonNull(type, "type");
        for (ContainerNode node = this.parent; node != null; node = node.parentNode()) {
            final Component<?> facade = node.facade();
            if (type.isInstance(facade)) {
                return Optional.of(type.cast(facade));
            }
        }
        return Optional.empty();
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
        if (this.bindings != null) {
            for (final Binding<?> binding : this.bindings.values()) {
                binding.attach(root.bindingQueue());
                root.bindingAttached(binding);
            }
        }
    }

    /**
     * Runs the component's attach hook and then its {@link Component#ATTACHED} handlers, if it has any. Does nothing
     * while the node is detached, or if this already ran since the node was last {@linkplain #fireDetached()
     * detached}, so a subtree that a binding attached during another attach hears it once. {@link Root} calls this for
     * each node of a subtree it attached, parents first, after the bindings set their properties.
     */
    final void fireAttached() {
        if (this.root == null || this.attachedFired) {
            return;
        }
        this.attachedFired = true;
        this.hooks.attached();
        final EventDispatcher handlers = this.dispatcher;
        if (handlers != null && handlers.hasHandlers(Component.ATTACHED)) {
            final LifecycleEventImpl event = new LifecycleEventImpl(this.facade);
            event.setCauses(this.ui.causes());
            handlers.fire(Component.ATTACHED, event);
        }
    }

    /**
     * Runs the component's {@link Component#DETACHED} handlers, if it has any, and then its detach hook, which runs
     * even if a handler throws. Does nothing unless {@link #fireAttached()} ran since the last time, so every detach a
     * component hears follows an attach it heard. {@link Root} calls this for each node of a subtree it is about to
     * detach, children first, while the node is still attached.
     */
    final void fireDetached() {
        if (!this.attachedFired) {
            return;
        }
        this.attachedFired = false;
        try {
            final EventDispatcher handlers = this.dispatcher;
            if (handlers != null && handlers.hasHandlers(Component.DETACHED)) {
                final LifecycleEventImpl event = new LifecycleEventImpl(this.facade);
                event.setCauses(this.ui.causes());
                handlers.fire(Component.DETACHED, event);
            }
        } finally {
            this.hooks.detached();
        }
    }

    /**
     * Tells the component that the root let go of its tree for good. See {@link BuiltInComponent#onReleased()}.
     */
    final void released() {
        this.hooks.released();
    }

    final void detach() {
        if (this.bindings != null) {
            for (final Binding<?> binding : this.bindings.values()) {
                binding.detach();
            }
        }
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

    /**
     * Returns the kind of text the component takes, which the root passes to the engine when it starts text input.
     * Meaningful only while {@link #acceptsTextInput()} is true.
     *
     * @return the kind of text
     */
    public final TextInputKind textInputKind() {
        return this.hooks.textInputKind();
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

    /**
     * Returns the node that takes focus for this one, such as a table's inner list, which core's
     * {@code BuiltInComponent.setFocusDelegate} set.
     *
     * @return the delegate, or {@code null} when this node takes focus itself
     */
    public final Node focusDelegate() {
        return this.focusDelegate;
    }

    /**
     * Returns the node this one takes focus for, the reverse of {@link #focusDelegate()}. The root fires
     * {@code FOCUS_GAINED} and {@code FOCUS_LOST} on it too.
     *
     * @return the owner, or {@code null} when this node is no other node's delegate
     */
    public final Node focusOwner() {
        return this.focusOwner;
    }

    /**
     * Makes another node take focus for this one, and this node stop taking it itself.
     */
    final void setFocusDelegate(final Node delegate) {
        if (this.focusDelegate != null) {
            this.focusDelegate.focusOwner = null;
        }
        this.focusDelegate = delegate;
        delegate.focusOwner = this;
        this.focusable = false;
        this.tabbable = false;
        this.treeChanged();
    }

    // ------------------------------------------------------------------
    // Properties
    // ------------------------------------------------------------------

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
            this.hooks.enabledChanged();
            if (!enabled) {
                this.hooks.disabled();
            }
        }
    }

    @Override
    public final boolean isFocusable() {
        return this.focusable && this.facade instanceof Focusable<?>;
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
        return this.facade instanceof HasCursor<?> ? this.cursor : null;
    }

    @Override
    public final void setCursor(final CursorKind cursor) {
        this.cursor = cursor;
        // The host asks for the cursor as it renders
        this.repaint();
    }

    @Override
    public final Insets margin() {
        return this.margin;
    }

    @Override
    public final void setMargin(final Insets margin) {
        this.margin = Objects.requireNonNull(margin, "margin");
        this.marginSet = true;
        this.invalidateLayout();
    }

    /**
     * Makes the margin follow each theme the node gets, until {@link #setMargin} is called. The margin is read now,
     * from an empty theme while the node has none, so a detached node shows its default, and again whenever
     * {@link #applyTheme} runs.
     *
     * @param margin reads the margin from a theme
     */
    public final void setThemeMargin(final Function<Theme, Insets> margin) {
        this.themeMargin = Objects.requireNonNull(margin, "margin");
        if (!this.marginSet) {
            this.margin = margin.apply(this.theme != null ? this.theme : NO_THEME);
            this.invalidateLayout();
        }
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
        if (!(minWidth >= 0f) || !(minHeight >= 0f) || Float.isInfinite(minWidth) || Float.isInfinite(minHeight)) {
            throw new IllegalArgumentException("A minimum size must be finite and not negative: " + minWidth + " by "
                    + minHeight);
        }
        this.minWidth = minWidth;
        this.minHeight = minHeight;
        this.invalidateLayout();
    }

    @Override
    public final void setMaxSize(final float maxWidth, final float maxHeight) {
        if (!(maxWidth >= 0f) || !(maxHeight >= 0f)) {
            throw new IllegalArgumentException("A maximum size must not be negative or NaN: " + maxWidth + " by "
                    + maxHeight);
        }
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        this.invalidateLayout();
    }

    @Override
    public final void setWidth(final float width) {
        this.explicitWidth = fixedLength(width, "width");
        this.invalidateLayout();
    }

    @Override
    public final void setHeight(final float height) {
        this.explicitHeight = fixedLength(height, "height");
        this.invalidateLayout();
    }

    /**
     * Checks a fixed width or height: NaN, which means none, or a finite length that is not negative.
     */
    private static float fixedLength(final float length, final String name) {
        if (!Float.isNaN(length) && (length < 0f || Float.isInfinite(length))) {
            throw new IllegalArgumentException("A fixed " + name + " must be finite and not negative: " + length);
        }
        return length;
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
    public final float opacity() {
        return this.opacity;
    }

    @Override
    public final void setOpacity(final float opacity) {
        if (!(opacity >= 0f && opacity <= 1f)) {
            throw new IllegalArgumentException("An opacity must be from 0 to 1: " + opacity);
        }
        if (this.opacity != opacity) {
            this.opacity = opacity;
            this.repaint();
        }
    }

    @Override
    public final int zIndex() {
        return this.zIndex;
    }

    /**
     * Sets the z index. A change makes the parent sort its drawing order again before the next paint or hit test,
     * and marks nothing for layout.
     *
     * @param zIndex the z index; may be negative
     */
    @Override
    public final void setZIndex(final int zIndex) {
        if (this.zIndex != zIndex) {
            this.zIndex = zIndex;
            if (this.parent != null) {
                this.parent.paintOrderChanged();
            }
            this.repaint();
        }
    }

    /**
     * Returns the sequence number the parent gave this node when it was added, raised or lowered, which orders it
     * among siblings with the same z index.
     *
     * @return the sequence number; higher draws later
     */
    final long paintSequence() {
        return this.paintSequence;
    }

    final void setPaintSequence(final long paintSequence) {
        this.paintSequence = paintSequence;
    }

    @Override
    public final Border border() {
        return this.border;
    }

    @Override
    public final void setBorder(final Border border) {
        this.border = Objects.requireNonNull(border, "border");
        this.repaint();
    }

    @Override
    public final Tooltip tooltip() {
        return this.facade instanceof HasTooltip<?> ? this.tooltip : null;
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
        this.repaint();
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
