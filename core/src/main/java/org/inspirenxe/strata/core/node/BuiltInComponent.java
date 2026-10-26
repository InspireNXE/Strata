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

import org.inspirenxe.strata.api.animation.Animator;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.component.Focusable;
import org.inspirenxe.strata.api.component.HasContextMenu;
import org.inspirenxe.strata.api.component.HasCursor;
import org.inspirenxe.strata.api.component.HasTooltip;
import org.inspirenxe.strata.api.component.Tooltip;
import org.inspirenxe.strata.api.control.attribute.popup.Popup;
import org.inspirenxe.strata.api.event.Event;
import org.inspirenxe.strata.api.event.EventHandler;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.Subscription;
import org.inspirenxe.strata.api.event.control.ControlEvent;
import org.inspirenxe.strata.api.event.control.RequestEvent;
import org.inspirenxe.strata.api.event.control.ValueEvent;
import org.inspirenxe.strata.api.event.control.ValueRequestEvent;
import org.inspirenxe.strata.api.event.input.CharEvent;
import org.inspirenxe.strata.api.event.input.DragEvent;
import org.inspirenxe.strata.api.event.input.FocusEvent;
import org.inspirenxe.strata.api.event.input.InputEvents;
import org.inspirenxe.strata.api.event.input.KeyEvent;
import org.inspirenxe.strata.api.event.input.PointerButtonEvent;
import org.inspirenxe.strata.api.event.input.PointerEvent;
import org.inspirenxe.strata.api.event.input.ScrollEvent;
import org.inspirenxe.strata.api.event.input.TransferEvent;
import org.inspirenxe.strata.api.extension.ArrangeContext;
import org.inspirenxe.strata.api.extension.Environment;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.input.TextInputKind;
import org.inspirenxe.strata.api.layout.FlowDirection;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.layout.VerticalAlignment;
import org.inspirenxe.strata.api.state.PropertyKey;
import org.inspirenxe.strata.api.state.Source;
import org.inspirenxe.strata.api.theme.Border;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.core.layout.Flow;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Base class of Strata's own leaf controls, the counterpart of the public
 * {@link org.inspirenxe.strata.api.extension.CustomComponent} that developers extend. It offers the same hooks and
 * helpers under the same names, so a control reads the same either way, and adds what only core may use: its own
 * {@link #node()}, the nodes of other components through {@link #nodeOf}, the host's {@link #hostRoot()},
 * {@link #isFocusFromPress()}, {@link #activatesOnPress()}, and the {@link #onEnabledChanged()},
 * {@link #onDisabled()}, {@link #onReleased()} and {@link #onPressWithin} hooks.
 *
 * <p>The component holds no state of its own. Its constructor makes the {@link Node} behind it and hands it a private
 * {@link NodeHooks} that calls the protected hooks here; every {@link Component} method is {@code final} and forwards
 * to that node, the same node class a {@code CustomComponent}'s peer is. Nothing made from this class is an instance
 * of {@code CustomComponent}, so code can tell Strata's components from a developer's.</p>
 *
 * <p>The methods of {@link Focusable}, {@link HasCursor} and {@link HasTooltip} are here too, and the node ignores
 * them on a subclass that does not declare the interface. A subclass that implements {@link Focusable} starts out
 * focusable and tabbable, and one that keeps focus on a part, as a table does on its list, names it with
 * {@link #setFocusDelegate}.</p>
 *
 * @param <T> the type callers see the component as
 */
public abstract class BuiltInComponent<T extends Component<T>> implements Component<T> {
    private final Node node;

    /**
     * Creates the component and its node.
     *
     * @param factory the factory of the host that will show this component
     */
    protected BuiltInComponent(final ComponentFactoryImpl factory) {
        this(factory, false, false);
    }

    /**
     * Creates the component with a plain node, or with a {@link ContainerNode} for a composite or container.
     *
     * @param factory the factory of the host that will show this component
     * @param container true to make a container node
     * @param composite for a container node, true if its children are private parts that stop control events
     */
    @SuppressWarnings("this-escape")
    BuiltInComponent(final ComponentFactoryImpl factory, final boolean container, final boolean composite) {
        Objects.requireNonNull(factory, "factory");
        final Hooks hooks = new Hooks();
        this.node = container ? new ContainerNode(factory, this, hooks, composite) : new Node(factory, this, hooks);
        if (this instanceof Focusable<?>) {
            this.node.setFocusable(true);
            this.node.setTabbable(true);
        }
    }

    /**
     * Returns the node behind this component. {@link ComponentFactoryImpl#nodeOf} reads it without a cast.
     *
     * @return the node
     */
    protected Node node() {
        return this.node;
    }

    /**
     * Returns the node behind another component of this host, for reaching state its public type does not show,
     * such as the background of an inner list.
     *
     * @param component a component made by this component's factory or a developer's {@code CustomComponent}
     * @return its node
     * @throws IllegalArgumentException if the component has no node or belongs to another host
     */
    protected final Node nodeOf(final Component<?> component) {
        return this.node.factory().nodeOf(component);
    }

    /**
     * Returns the root of this component's host, whether or not this component is attached.
     *
     * @return the root, or {@code null} before the host made it
     */
    protected final Root hostRoot() {
        return this.node.factory().root();
    }

    /**
     * Returns the host's animator while this component is attached, for a control's own motion (design 4.51).
     *
     * @return the animator, or {@code null} while detached, so motion then jumps to its end
     */
    protected final Animator motionAnimator() {
        final Root root = this.node.root();
        return root == null ? null : root.animator();
    }

    /**
     * Returns whether the focus change being reported comes from a pointer press, so a focus hook can tell a click
     * from keyboard or code focus. See {@link Root#isFocusFromPress()}.
     *
     * @return true while a press moves focus to or from this component; false while it is detached
     */
    protected final boolean isFocusFromPress() {
        final Root root = this.node.root();
        return root != null && root.isFocusFromPress();
    }

    /**
     * Returns whether buttons act as the pointer goes down rather than on the click after it, as the platform's
     * {@link org.inspirenxe.strata.spi.Conventions#activateOnPress()} says. See {@link Root#activatesOnPress()}.
     *
     * @return true when buttons act on press; false while this component is detached
     */
    protected final boolean activatesOnPress() {
        final Root root = this.node.root();
        return root != null && root.activatesOnPress();
    }

    /**
     * Returns this component as the type callers see, for returning from chaining setters.
     *
     * @return {@code this}
     */
    @SuppressWarnings("unchecked")
    protected final T self() {
        return (T) this;
    }

    /**
     * Returns text measurement, time and the clipboard of the host. Works before the component is attached.
     *
     * @return the host's environment
     */
    protected final Environment environment() {
        return this.node.environment();
    }

    // ------------------------------------------------------------------
    // Hooks for subclasses
    // ------------------------------------------------------------------

    /**
     * Returns the size the content needs within the given space, as {@code CustomComponent.measure} does.
     *
     * @param context text measurement and child measuring
     * @param availableWidth the width offered, margin removed; may be {@link Float#POSITIVE_INFINITY}
     * @param availableHeight the height offered, margin removed; may be {@link Float#POSITIVE_INFINITY}
     * @return the natural size of the content
     */
    protected abstract Size measure(MeasureContext context, float availableWidth, float availableHeight);

    /**
     * Returns the smallest size the content can work in, as {@code CustomComponent.measureMinimum} does. The default
     * returns {@link Size#ZERO}.
     *
     * @param context text measurement and the children's minimums
     * @return the smallest size of the content
     */
    protected Size measureMinimum(final MeasureContext context) {
        return Size.ZERO;
    }

    /**
     * Returns the distance from the top of the bounds to the baseline of the first line of text, as
     * {@code CustomComponent.baseline} does. The default returns NaN, for no baseline.
     *
     * @return the baseline in UI units, or NaN for none
     */
    protected float baseline() {
        return Float.NaN;
    }

    /**
     * Draws the component over its background and border and under its children. The default draws nothing.
     *
     * @param painter what to draw with; valid only during this call
     * @param bounds the component's bounds
     */
    protected void paint(final Painter painter, final Rect bounds) {
    }

    /**
     * Returns the style keys the host's theme must have to show this component. The default needs none.
     *
     * @return the required keys
     */
    protected Set<String> requiredStyleKeys() {
        return Set.of();
    }

    /**
     * Reads paints and text styles from the theme, on attach and after every theme change, before the next layout.
     * The default does nothing.
     *
     * @param theme the host's theme, with the text style patches of the containers around the component applied
     */
    protected void onApplyTheme(final Theme theme) {
    }

    /**
     * Called when the component joins a tree a host is showing, every time it does, as
     * {@code CustomComponent.onAttached} is: after the theme is applied and the bindings set their properties, and
     * before its {@code ATTACHED} handlers. The default does nothing.
     */
    protected void onAttached() {
    }

    /**
     * Called when the component leaves a tree a host is showing, every time it does, as
     * {@code CustomComponent.onDetached} is: after its {@code DETACHED} handlers, while it is still attached. Unlike
     * {@link #onReleased()}, it runs for a kept component too, which may be attached again. The default does nothing.
     */
    protected void onDetached() {
    }

    /**
     * Returns whether focusing this component starts text input on the engine. The default is false.
     *
     * @return true if the component accepts typed text
     */
    protected boolean acceptsTextInput() {
        return false;
    }

    /**
     * Returns the kind of text the component takes, which the root passes to the engine when it starts text input.
     * Read only while {@link #acceptsTextInput()} is true. The default is {@link TextInputKind#TEXT}.
     *
     * @return the kind of text
     */
    protected TextInputKind textInputKind() {
        return TextInputKind.TEXT;
    }

    /**
     * Called after this component's own enabled flag changed, either way, so it can restyle what it caches or pass
     * the state on to its private parts. Enabling or disabling an ancestor does not call it. It runs before
     * {@link #onDisabled()}. The default does nothing.
     */
    protected void onEnabledChanged() {
    }

    /**
     * Called after this component itself was disabled, so it can hide what it shows. Disabling an ancestor instead
     * takes focus from the component. The default does nothing.
     */
    protected void onDisabled() {
    }

    /**
     * Called after the root let go of the tree this component is in and the component was not attached again: the
     * old content after {@link Root#setContent} replaced it, which the host does after {@code Screen.closed()} and on
     * a rebuild, and an overlay's content after the overlay closed. A component that holds secret text forgets it
     * here. The default does nothing.
     */
    protected void onReleased() {
    }

    /**
     * Called when a pointer button goes down on this component or on a component inside it, after focus moved and
     * before the press is delivered, on the pressed component and then on each ancestor up to the top of its layer.
     * It runs whatever handlers later do with the press, so a window raises itself here. The default does nothing.
     *
     * @param target the component the press is aimed at
     */
    protected void onPressWithin(final Component<?> target) {
    }

    /**
     * Called once a frame while a drag that carries a transfer holds the pointer over this component or a component
     * inside it, from the deepest component under the pointer up until one returns true. A control that scrolls
     * moves one step toward the edge of its viewport the pointer is near, as {@code EdgeScroll} works it out, and
     * returns true; the root then asks for the next frame, so scrolling goes on while the pointer stays still. The
     * default does nothing and returns false.
     *
     * @param x the pointer's x, in the host's coordinates
     * @param y the pointer's y, in the host's coordinates
     * @return true if the component scrolled
     */
    protected boolean onEdgeScroll(final float x, final float y) {
        return false;
    }

    /**
     * Default behavior for {@link InputEvents#POINTER_ENTER}. The default does nothing.
     *
     * @param event the event
     */
    protected void onPointerEnter(final PointerEvent event) {
    }

    /**
     * Default behavior for {@link InputEvents#POINTER_EXIT}. The default does nothing.
     *
     * @param event the event
     */
    protected void onPointerExit(final PointerEvent event) {
    }

    /**
     * Default behavior for {@link InputEvents#POINTER_MOVE}. The default does nothing.
     *
     * @param event the event
     */
    protected void onPointerMove(final PointerEvent event) {
    }

    /**
     * Default behavior for {@link InputEvents#POINTER_PRESS}. The default does nothing.
     *
     * @param event the event
     */
    protected void onPointerPress(final PointerButtonEvent event) {
    }

    /**
     * Default behavior for {@link InputEvents#POINTER_RELEASE}. The default does nothing.
     *
     * @param event the event
     */
    protected void onPointerRelease(final PointerButtonEvent event) {
    }

    /**
     * Default behavior for {@link InputEvents#CLICK}. The default does nothing.
     *
     * @param event the event
     */
    protected void onClick(final PointerButtonEvent event) {
    }

    /**
     * Default behavior for {@link InputEvents#DRAG_START}. The default does nothing.
     *
     * @param event the event
     */
    protected void onDragStart(final DragEvent event) {
    }

    /**
     * Default behavior for {@link InputEvents#DRAG}. The default does nothing.
     *
     * @param event the event
     */
    protected void onDrag(final DragEvent event) {
    }

    /**
     * Default behavior for {@link InputEvents#DRAG_END}. The default does nothing.
     *
     * @param event the event
     */
    protected void onDragEnd(final DragEvent event) {
    }

    /**
     * Default behavior for {@link InputEvents#DRAG_ENTER}. The default does nothing.
     *
     * @param event the event
     */
    protected void onDragEnter(final TransferEvent event) {
    }

    /**
     * Default behavior for {@link InputEvents#DRAG_OVER}. Accept the drag here with {@link TransferEvent#accept} to
     * take the drop. The default does nothing.
     *
     * @param event the event
     */
    protected void onDragOver(final TransferEvent event) {
    }

    /**
     * Default behavior for {@link InputEvents#DRAG_EXIT}. The default does nothing.
     *
     * @param event the event
     */
    protected void onDragExit(final TransferEvent event) {
    }

    /**
     * Default behavior for {@link InputEvents#DROP}. The default does nothing.
     *
     * @param event the event
     */
    protected void onDrop(final TransferEvent event) {
    }

    /**
     * Default behavior for {@link InputEvents#TRANSFER_DONE}, on the source of a drag. The default does nothing.
     *
     * @param event the event
     */
    protected void onTransferDone(final TransferEvent event) {
    }

    /**
     * Default behavior for {@link InputEvents#SCROLL}. The default does nothing.
     *
     * @param event the event
     */
    protected void onScroll(final ScrollEvent event) {
    }

    /**
     * Default behavior for {@link InputEvents#KEY}. The default does nothing.
     *
     * @param event the event
     */
    protected void onKey(final KeyEvent event) {
    }

    /**
     * Default behavior for {@link InputEvents#CHAR}. The default does nothing.
     *
     * @param event the event
     */
    protected void onChar(final CharEvent event) {
    }

    /**
     * Default behavior for {@link Focusable#FOCUS_GAINED}. The default does nothing.
     *
     * @param event the event
     */
    protected void onFocusGained(final FocusEvent event) {
    }

    /**
     * Default behavior for {@link Focusable#FOCUS_LOST}. The default does nothing.
     *
     * @param event the event
     */
    protected void onFocusLost(final FocusEvent event) {
    }

    /**
     * Default behavior for event types other than those in {@link InputEvents} and the focus events. The default
     * does nothing.
     *
     * @param type the event's type
     * @param event the event
     */
    protected void onOtherEvent(final EventType<?> type, final Event event) {
    }

    /**
     * Returns this component's own state for a snapshot, an immutable value without its children's state. Called
     * only when an event with snapshots captures this component. The default returns null.
     *
     * @return the state, or null for none
     */
    protected Object snapshotState() {
        return null;
    }

    // Overridden by BuiltInComposite
    void arrangeHook(final ArrangeContext context, final Rect bounds) {
    }

    void childRemovedHook(final Component<?> child) {
    }

    Rect childClipHook() {
        return this.bounds();
    }

    void paintOverChildrenHook(final Painter painter, final Rect bounds) {
    }

    void descendantFocusedHook(final Component<?> descendant) {
    }

    private void handleEvent(final EventType<?> type, final Event event) {
        if (type == InputEvents.POINTER_ENTER) {
            this.onPointerEnter((PointerEvent) event);
        } else if (type == InputEvents.POINTER_EXIT) {
            this.onPointerExit((PointerEvent) event);
        } else if (type == InputEvents.POINTER_MOVE) {
            this.onPointerMove((PointerEvent) event);
        } else if (type == InputEvents.POINTER_PRESS) {
            this.onPointerPress((PointerButtonEvent) event);
        } else if (type == InputEvents.POINTER_RELEASE) {
            this.onPointerRelease((PointerButtonEvent) event);
        } else if (type == InputEvents.CLICK) {
            this.onClick((PointerButtonEvent) event);
        } else if (type == InputEvents.DRAG_START) {
            this.onDragStart((DragEvent) event);
        } else if (type == InputEvents.DRAG) {
            this.onDrag((DragEvent) event);
        } else if (type == InputEvents.DRAG_END) {
            this.onDragEnd((DragEvent) event);
        } else if (type == InputEvents.DRAG_ENTER) {
            this.onDragEnter((TransferEvent) event);
        } else if (type == InputEvents.DRAG_OVER) {
            this.onDragOver((TransferEvent) event);
        } else if (type == InputEvents.DRAG_EXIT) {
            this.onDragExit((TransferEvent) event);
        } else if (type == InputEvents.DROP) {
            this.onDrop((TransferEvent) event);
        } else if (type == InputEvents.TRANSFER_DONE) {
            this.onTransferDone((TransferEvent) event);
        } else if (type == InputEvents.SCROLL) {
            this.onScroll((ScrollEvent) event);
        } else if (type == InputEvents.KEY) {
            this.onKey((KeyEvent) event);
        } else if (type == InputEvents.CHAR) {
            this.onChar((CharEvent) event);
        } else if (type == Focusable.FOCUS_GAINED) {
            this.onFocusGained((FocusEvent) event);
        } else if (type == Focusable.FOCUS_LOST) {
            this.onFocusLost((FocusEvent) event);
        } else {
            this.onOtherEvent(type, event);
        }
    }

    /**
     * The node's way into the protected hooks. Private, so nothing else can call them.
     */
    private final class Hooks implements NodeHooks {
        @Override
        public Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
            return BuiltInComponent.this.measure(context, availableWidth, availableHeight);
        }

        @Override
        public Size measureMinimum(final MeasureContext context) {
            return BuiltInComponent.this.measureMinimum(context);
        }

        @Override
        public float baseline() {
            return BuiltInComponent.this.baseline();
        }

        @Override
        public void arrange(final ArrangeContext context, final Rect bounds) {
            BuiltInComponent.this.arrangeHook(context, bounds);
        }

        @Override
        public void paint(final Painter painter, final Rect bounds) {
            BuiltInComponent.this.paint(painter, bounds);
        }

        @Override
        public Set<String> requiredStyleKeys() {
            return BuiltInComponent.this.requiredStyleKeys();
        }

        @Override
        public void applyTheme(final Theme theme) {
            BuiltInComponent.this.onApplyTheme(theme);
        }

        @Override
        public boolean acceptsTextInput() {
            return BuiltInComponent.this.acceptsTextInput();
        }

        @Override
        public TextInputKind textInputKind() {
            return BuiltInComponent.this.textInputKind();
        }

        @Override
        public void handle(final EventType<?> type, final Event event) {
            BuiltInComponent.this.handleEvent(type, event);
        }

        @Override
        public Object snapshotState() {
            return BuiltInComponent.this.snapshotState();
        }

        @Override
        public void childRemoved(final Component<?> child) {
            BuiltInComponent.this.childRemovedHook(child);
        }

        @Override
        public Rect childClip() {
            return BuiltInComponent.this.childClipHook();
        }

        @Override
        public void paintOverChildren(final Painter painter, final Rect bounds) {
            BuiltInComponent.this.paintOverChildrenHook(painter, bounds);
        }

        @Override
        public void descendantFocused(final Component<?> descendant) {
            BuiltInComponent.this.descendantFocusedHook(descendant);
        }

        @Override
        public void attached() {
            BuiltInComponent.this.onAttached();
        }

        @Override
        public void detached() {
            BuiltInComponent.this.onDetached();
        }

        @Override
        public void enabledChanged() {
            BuiltInComponent.this.onEnabledChanged();
        }

        @Override
        public void disabled() {
            BuiltInComponent.this.onDisabled();
        }

        @Override
        public void released() {
            BuiltInComponent.this.onReleased();
        }

        @Override
        public void pressedWithin(final Component<?> target) {
            BuiltInComponent.this.onPressWithin(target);
        }

        @Override
        public boolean edgeScroll(final float x, final float y) {
            return BuiltInComponent.this.onEdgeScroll(x, y);
        }
    }

    // ------------------------------------------------------------------
    // Component, forwarded to the node. Documented on Component.
    // ------------------------------------------------------------------

    @Override
    public final Optional<String> id() {
        return this.node.id();
    }

    @Override
    public final T setId(final String id) {
        this.node.setId(id);
        return this.self();
    }

    @Override
    public final Optional<Component<?>> parent() {
        return Optional.ofNullable(this.node.parent());
    }

    @Override
    public final <P> Optional<P> parent(final Class<P> type) {
        return this.node.parent(type);
    }

    @Override
    public final <A> Optional<A> ancestor(final Class<A> type) {
        return this.node.ancestor(type);
    }

    @Override
    public final boolean isAttached() {
        return this.node.isAttached();
    }

    @Override
    public final T whileAttached(final Supplier<? extends Subscription> start) {
        return Component.super.whileAttached(start);
    }

    @Override
    public final boolean isFocusWithin() {
        return this.node.isFocusWithin();
    }

    @Override
    public final boolean isVisible() {
        return this.node.isVisible();
    }

    @Override
    public final T setVisible(final boolean visible) {
        this.node.setVisible(visible);
        return this.self();
    }

    @Override
    public final boolean isHitTestVisible() {
        return this.node.isHitTestVisible();
    }

    @Override
    public final T setHitTestVisible(final boolean hitTestVisible) {
        this.node.setHitTestVisible(hitTestVisible);
        return this.self();
    }

    @Override
    public final boolean isEnabled() {
        return this.node.isEnabled();
    }

    @Override
    public final T setEnabled(final boolean enabled) {
        this.node.setEnabled(enabled);
        return this.self();
    }

    @Override
    public final Insets margin() {
        return this.node.margin();
    }

    @Override
    public final T setMargin(final Insets margin) {
        this.node.setMargin(margin);
        return this.self();
    }

    @Override
    public final float minWidth() {
        return this.node.minWidth();
    }

    @Override
    public final float maxWidth() {
        return this.node.maxWidth();
    }

    @Override
    public final float minHeight() {
        return this.node.minHeight();
    }

    @Override
    public final float maxHeight() {
        return this.node.maxHeight();
    }

    @Override
    public final T setMinSize(final float minWidth, final float minHeight) {
        this.node.setMinSize(minWidth, minHeight);
        return this.self();
    }

    @Override
    public final T setMaxSize(final float maxWidth, final float maxHeight) {
        this.node.setMaxSize(maxWidth, maxHeight);
        return this.self();
    }

    @Override
    public final T setWidth(final float width) {
        this.node.setWidth(width);
        return this.self();
    }

    @Override
    public final T setHeight(final float height) {
        this.node.setHeight(height);
        return this.self();
    }

    @Override
    public final HorizontalAlignment horizontalAlignment() {
        return this.node.horizontalAlignment();
    }

    @Override
    public final VerticalAlignment verticalAlignment() {
        return this.node.verticalAlignment();
    }

    @Override
    public final T setAlignment(final HorizontalAlignment horizontal, final VerticalAlignment vertical) {
        this.node.setAlignment(horizontal, vertical);
        return this.self();
    }

    @Override
    public final FlowDirection flowDirection() {
        return this.node.flowDirection();
    }

    @Override
    public final T setFlowDirection(final FlowDirection direction) {
        this.node.setFlowDirection(direction);
        return this.self();
    }

    /**
     * Returns whether the component's content runs right to left, as {@link #flowDirection()} says. Controls whose
     * parts sit at a side, or whose keys move one way, read this when they place, draw and handle them.
     *
     * @return true for {@link FlowDirection#RIGHT_TO_LEFT}
     */
    protected final boolean isRightToLeft() {
        return this.node.flowDirection() == FlowDirection.RIGHT_TO_LEFT;
    }

    /**
     * Returns a rectangle worked out as for left to right, mirrored inside the bounds while the component runs right
     * to left, as {@link Component#setFlowDirection} mirrors children. Left to right it comes back as it is.
     *
     * @param rect the rectangle, in the host's coordinates
     * @return the rectangle as drawn
     */
    protected final Rect flow(final Rect rect) {
        return this.isRightToLeft() ? Flow.mirror(rect, this.node.bounds()) : rect;
    }

    @Override
    public final Rect bounds() {
        return this.node.bounds();
    }

    @Override
    public final float desiredWidth() {
        return this.node.desiredWidth();
    }

    @Override
    public final float desiredHeight() {
        return this.node.desiredHeight();
    }

    @Override
    public final float opacity() {
        return this.node.opacity();
    }

    @Override
    public final T setOpacity(final float opacity) {
        this.node.setOpacity(opacity);
        return this.self();
    }

    @Override
    public final int zIndex() {
        return this.node.zIndex();
    }

    @Override
    public final T setZIndex(final int zIndex) {
        this.node.setZIndex(zIndex);
        return this.self();
    }

    @Override
    public final <E extends Event> T onEvent(final EventType<E> type, final int priority,
                                             final EventHandler<? super E> handler) {
        this.node.subscribe(type, priority, handler);
        return this.self();
    }

    @Override
    public final <E extends Event> Subscription subscribe(final EventType<E> type, final int priority,
                                                          final EventHandler<? super E> handler) {
        return this.node.subscribe(type, priority, handler);
    }

    @Override
    public final <V> T bind(final PropertyKey<? super T, V> key, final Source<V> source) {
        this.node.bind(key, source);
        return this.self();
    }

    @Override
    public final T unbind(final PropertyKey<? super T, ?> key) {
        this.node.unbind(key);
        return this.self();
    }

    // ------------------------------------------------------------------
    // Focusable, HasCursor and HasTooltip, for subclasses that declare them. The node ignores these on the rest.
    // ------------------------------------------------------------------

    /**
     * Makes one of this control's parts take focus for it, as a table's inner list does. From then on the
     * {@link Focusable} methods here work on the delegate: {@link #requestFocus()} focuses it, {@link #isFocused()}
     * is true while it has focus, the tab order holds it in place of this control, and a press on this control's other
     * parts focuses it. The root fires {@code FOCUS_GAINED} and {@code FOCUS_LOST} on this control too, after the
     * delegate, so its {@link #onFocusGained} and {@link #onFocusLost} run. This control's own node stops taking focus.
     *
     * @param delegate the part that takes focus, made by the same factory
     */
    protected final void setFocusDelegate(final Component<?> delegate) {
        this.node.setFocusDelegate(this.nodeOf(delegate));
    }

    /**
     * Returns the node the {@link Focusable} methods work on: the focus delegate, or this component's own node.
     */
    private Node focusNode() {
        final Node delegate = this.node.focusDelegate();
        return delegate != null ? delegate : this.node;
    }

    /**
     * Returns whether the component can take keyboard focus. See {@link Focusable#isFocusable()}.
     *
     * @return true if focusable; always false unless the subclass implements {@link Focusable}
     */
    public final boolean isFocusable() {
        return this.focusNode().isFocusable();
    }

    /**
     * Sets whether the component can take keyboard focus. See {@link Focusable#setFocusable(boolean)}.
     *
     * @param focusable false to refuse focus
     * @return this component
     */
    public final T setFocusable(final boolean focusable) {
        this.focusNode().setFocusable(focusable);
        return this.self();
    }

    /**
     * Returns whether Tab and Shift+Tab stop at this component. See {@link Focusable#isTabbable()}.
     *
     * @return true if tabbable
     */
    public final boolean isTabbable() {
        return this.focusNode().isTabbable();
    }

    /**
     * Sets whether Tab and Shift+Tab stop at this component. See {@link Focusable#setTabbable(boolean)}.
     *
     * @param tabbable false to leave the component out of tab order
     * @return this component
     */
    public final T setTabbable(final boolean tabbable) {
        this.focusNode().setTabbable(tabbable);
        return this.self();
    }

    /**
     * Returns the tab index. See {@link Focusable#tabIndex()}.
     *
     * @return the tab index; 0 by default
     */
    public final int tabIndex() {
        return this.focusNode().tabIndex();
    }

    /**
     * Sets the tab index. See {@link Focusable#setTabIndex(int)}.
     *
     * @param tabIndex the tab index
     * @return this component
     */
    public final T setTabIndex(final int tabIndex) {
        this.focusNode().setTabIndex(tabIndex);
        return this.self();
    }

    /**
     * Returns whether this component has keyboard focus that should be shown. See
     * {@link Focusable#isFocusVisible()}.
     *
     * @return true while focused and focus is shown
     */
    public final boolean isFocusVisible() {
        return this.node.isFocusVisible();
    }

    /**
     * Returns whether this component has keyboard focus. See {@link Focusable#isFocused()}.
     *
     * @return true while focused
     */
    public final boolean isFocused() {
        return this.focusNode().isFocused();
    }

    /**
     * Asks the host to give this component keyboard focus. See {@link Focusable#requestFocus()}.
     *
     * @return true if the component now has focus; always false unless the subclass implements {@link Focusable}
     */
    public final boolean requestFocus() {
        return this.focusNode().requestFocus();
    }

    /**
     * Returns the cursor shown over this component. See {@link HasCursor#cursor()}.
     *
     * @return the cursor, or empty to use the nearest ancestor's
     */
    public final Optional<CursorKind> cursor() {
        return Optional.ofNullable(this.node.cursor());
    }

    /**
     * Sets the cursor shown over this component. See {@link HasCursor#setCursor(CursorKind)}.
     *
     * @param cursor the cursor, or {@code null} to use the nearest ancestor's
     * @return this component
     */
    public final T setCursor(final CursorKind cursor) {
        this.node.setCursor(cursor);
        return this.self();
    }

    /**
     * Returns the tooltip. See {@link HasTooltip#tooltip()}.
     *
     * @return the tooltip, or empty for none
     */
    public final Optional<Tooltip> tooltip() {
        return Optional.ofNullable(this.node.tooltip());
    }

    /**
     * Sets the tooltip. See {@link HasTooltip#setTooltip(Tooltip)}.
     *
     * @param tooltip the tooltip
     * @return this component
     */
    public final T setTooltip(final Tooltip tooltip) {
        this.node.setTooltip(Objects.requireNonNull(tooltip, "tooltip"));
        return this.self();
    }

    /**
     * Removes the tooltip. See {@link HasTooltip#clearTooltip()}.
     *
     * @return this component
     */
    public final T clearTooltip() {
        this.node.setTooltip(null);
        return this.self();
    }

    /**
     * Returns the context menu. See {@link HasContextMenu#contextMenu()}.
     *
     * @return the popup, or empty for none
     */
    public final Optional<Popup<?>> contextMenu() {
        return Optional.ofNullable(this.node.contextMenu());
    }

    /**
     * Sets the context menu. See {@link HasContextMenu#setContextMenu(Popup)}.
     *
     * @param popup the popup, or {@code null} for none
     * @return this component
     */
    public final T setContextMenu(final Popup<?> popup) {
        this.node.setContextMenu(popup);
        return this.self();
    }

    // ------------------------------------------------------------------
    // State for the subclass's own use
    // ------------------------------------------------------------------

    /**
     * Returns the theme applied to this component, for reading styles outside {@link #onApplyTheme}.
     *
     * @return the host's theme, with the {@linkplain org.inspirenxe.strata.api.component.Container#setTextStyle text
     *     style patches} of the containers around the component applied, or empty before the component is
     *     first attached
     */
    protected final Optional<Theme> theme() {
        return Optional.ofNullable(this.node.theme());
    }

    /**
     * Turns one of this component's own style states on or off, as {@code CustomComponent.setStyleState} does.
     *
     * @param state the state's name; any name but {@code disabled}, {@code pressed}, {@code hovered},
     *              {@code focus_visible} and {@code focused}, which the node sets
     * @param on whether the state is on
     * @throws IllegalArgumentException if the name is empty or one the node sets
     */
    protected final void setStyleState(final String state, final boolean on) {
        this.node.setStyleState(state, on);
    }

    /**
     * Looks up a paint for this component's style states, as {@code CustomComponent.stylePaint} does.
     *
     * @param key the style key
     * @param fallback returned when the theme has none of the keys tried, or the component was never attached
     * @return the paint
     */
    protected final Paint stylePaint(final String key, final Paint fallback) {
        return this.node.stylePaint(key, fallback);
    }

    /**
     * Looks up a text style for this component's style states, as {@code CustomComponent.styleText} does.
     *
     * @param key the style key
     * @param fallback returned when the theme has none of the keys tried, or the component was never attached
     * @return the text style
     */
    protected final TextStyle styleText(final String key, final TextStyle fallback) {
        return this.node.styleText(key, fallback);
    }

    /**
     * Returns whether the pointer is over this component and not over a child of it. A component that is disabled,
     * or inside a disabled one, is never hovered.
     *
     * @return true while hovered
     */
    protected final boolean isHovered() {
        return this.node.isHovered();
    }

    /**
     * Returns whether the pointer is over this component or any of its descendants.
     *
     * @return true while this component or a descendant is hovered
     */
    protected final boolean isHoverWithin() {
        return this.node.isHoverWithin();
    }

    /**
     * Returns whether a mouse button went down on this component and has not been released yet.
     *
     * @return true while pressed
     */
    protected final boolean isPressed() {
        return this.node.isPressed();
    }

    /**
     * Returns the horizontal offset.
     *
     * @return how far the component is moved right of where layout puts it; 0 by default
     */
    protected float offsetX() {
        return this.node.offsetX();
    }

    /**
     * Returns the vertical offset.
     *
     * @return how far the component is moved down from where layout puts it; 0 by default
     */
    protected float offsetY() {
        return this.node.offsetY();
    }

    /**
     * Moves the component from where its parent's layout puts it, along with everything inside it, and arranges it
     * again. A subclass may make this public, as the window does.
     *
     * @param offsetX the distance to move right, in UI units
     * @param offsetY the distance to move down, in UI units
     * @return this component
     */
    protected T setOffset(final float offsetX, final float offsetY) {
        this.node.setOffset(offsetX, offsetY);
        return this.self();
    }

    /**
     * Returns the background paint.
     *
     * @return the background; {@link Paint#NONE} by default
     */
    protected Paint background() {
        return this.node.background();
    }

    /**
     * Sets the paint drawn over the component's bounds before anything else of it.
     *
     * @param background the background, or {@link Paint#NONE} for none
     * @return this component
     */
    protected T setBackground(final Paint background) {
        this.node.setBackground(background);
        return this.self();
    }

    /**
     * Returns the border.
     *
     * @return the border; {@link Border#NONE} by default
     */
    protected Border border() {
        return this.node.border();
    }

    /**
     * Sets the lines drawn along the inside of the component's edges, over its background and under its content.
     *
     * @param border the border, or {@link Border#NONE} for none
     * @return this component
     */
    protected T setBorder(final Border border) {
        this.node.setBorder(border);
        return this.self();
    }

    /**
     * Schedules a new layout before the next frame. Call it whenever something that {@link #measure} reads changes.
     */
    protected final void invalidateLayout() {
        this.node.invalidateLayout();
    }

    /**
     * Asks the host for a frame because the look changed and the layout did not, as
     * {@code CustomComponent.repaint()} does. Reports, layout and the root's hover, press and focus changes already
     * ask; call it for a change of look that none of them covers.
     */
    protected final void repaint() {
        this.node.repaint();
    }

    /**
     * Asks for the next frame the host's frame rate allows, as {@code CustomComponent.repaintNextFrame()} does, for a
     * look that moves with the clock such as a sliding caption.
     */
    protected final void repaintNextFrame() {
        this.node.repaintNextFrame();
    }

    /**
     * Tells the host that the look changes by itself after a time, as {@code CustomComponent.repaintAfter} does:
     * the caret's next blink or a step of a busy indicator. Call it from {@link #paint}, for the next change only.
     *
     * @param millis the delay in milliseconds, by the platform's clock; 0 is {@link #repaint()}
     * @throws IllegalArgumentException if {@code millis} is negative
     */
    protected final void repaintAfter(final long millis) {
        this.node.repaintAfter(millis);
    }

    /**
     * Fires a request without a value through this component's path, for the user's actions only. Fires nothing and
     * returns true when the current origin is not {@code Origin.User} or no handler for the type is on the path.
     *
     * @param type the request type
     * @return true if the component should go ahead, false if a handler canceled the request
     * @throws IllegalArgumentException if the type carries a value
     */
    protected final boolean request(final EventType<? extends RequestEvent<?>> type) {
        return this.node.request(type);
    }

    /**
     * Fires a value request through this component's path, for the user's actions only, then applies the final
     * value unless a handler canceled the request. Applies the value at once when the current origin is not
     * {@code Origin.User} or no handler for the type is on the path.
     *
     * @param <V> the value type
     * @param type the request type
     * @param value the value the user's action produced
     * @param apply makes the change with the final value; not called if the request was canceled
     */
    protected final <V> void request(final EventType<? extends ValueRequestEvent<?, V>> type, final V value,
                                     final Consumer<? super V> apply) {
        this.node.request(type, value, apply);
    }

    /**
     * Fires a report without a value through this component's path, after the fact. Fires nothing when no handler
     * for the type is on the path.
     *
     * @param type the report type
     * @throws IllegalArgumentException if the type carries a value
     */
    protected final void report(final EventType<? extends ControlEvent<?>> type) {
        this.node.report(type);
    }

    /**
     * Fires a value report through this component's path, after the change. Fires nothing when no handler for the
     * type is on the path.
     *
     * @param <V> the value type
     * @param type the report type
     * @param value the value after the change
     * @param previous the value before the change
     */
    protected final <V> void report(final EventType<? extends ValueEvent<?, V>> type, final V value,
                                    final V previous) {
        this.node.report(type, value, previous);
    }

    /**
     * Returns whether a handler for a type is on this component's path, so work only an event needs can be skipped.
     *
     * @param type the event type
     * @return true if firing the type would reach a handler
     */
    protected final boolean isHandled(final EventType<?> type) {
        return this.node.isHandled(type);
    }

    /**
     * Returns the class name, for debugging.
     *
     * @return for example {@code ButtonImpl}
     */
    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }
}
