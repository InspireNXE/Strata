package org.inspirenxe.strata.api.extension;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.ComponentSnapshot;
import org.inspirenxe.strata.api.component.Container;
import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.component.Tooltip;
import org.inspirenxe.strata.api.event.CharEvent;
import org.inspirenxe.strata.api.event.ControlEvent;
import org.inspirenxe.strata.api.event.DragEvent;
import org.inspirenxe.strata.api.event.Event;
import org.inspirenxe.strata.api.event.EventHandler;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.EventTypes;
import org.inspirenxe.strata.api.event.FocusEvent;
import org.inspirenxe.strata.api.event.KeyEvent;
import org.inspirenxe.strata.api.event.PointerButtonEvent;
import org.inspirenxe.strata.api.event.PointerEvent;
import org.inspirenxe.strata.api.event.RequestEvent;
import org.inspirenxe.strata.api.event.ScrollEvent;
import org.inspirenxe.strata.api.event.Subscription;
import org.inspirenxe.strata.api.event.ValueEvent;
import org.inspirenxe.strata.api.event.ValueRequestEvent;
import org.inspirenxe.strata.api.extension.peer.ComponentHooks;
import org.inspirenxe.strata.api.extension.peer.ComponentPeer;
import org.inspirenxe.strata.api.geom.Insets;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.geom.Size;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.layout.VerticalAlignment;
import org.inspirenxe.strata.api.theme.Border;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;

import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Base class for new kinds of component. Strata's own controls are built on it too.
 *
 * <p>Subclass it, implement {@link #measure}, and override the other {@code protected} hooks you need. To let other
 * code see the component as a type of your own, declare an interface that extends {@link Component} and implement
 * it here:</p>
 *
 * <pre>{@code
 * public interface ColorPicker extends Component<ColorPicker> {
 *     int color();
 * }
 *
 * final class ColorPickerImpl extends CustomComponent<ColorPicker> implements ColorPicker {
 *     private int color = 0xFFFF0000;
 *
 *     ColorPickerImpl(ComponentFactory factory) {
 *         super(factory);
 *         this.setFocusable(true);
 *     }
 *
 *     public int color() {
 *         return this.color;
 *     }
 *
 *     protected Size measure(MeasureContext context, float availableWidth, float availableHeight) {
 *         return new Size(60, 20);
 *     }
 *
 *     protected void paint(Painter painter, Rect bounds) {
 *         painter.fill(bounds, this.color);
 *     }
 *
 *     protected void onClick(PointerButtonEvent event) {
 *         this.color = nextColor(this.color);
 *         event.consume();
 *     }
 * }
 * }</pre>
 *
 * <p>The constructor asks the factory for a <em>peer</em>, Strata's half of the component, which holds its bounds,
 * state and handlers. Every {@link Component} method is {@code final} here and goes to the peer, so the component
 * behaves like every other one in layout, focus and events.</p>
 *
 * <h2>Requirements for subclasses</h2>
 * <ul>
 *     <li>Pass the factory of the host that will show the component; a component cannot move between hosts.</li>
 *     <li>{@link #measure} must be pure layout: compute and return a size, and cache what {@link #paint} needs. Do
 *     not change other components or call {@link #invalidateLayout()} from it.</li>
 *     <li>{@link #paint} draws inside the given bounds and must not change layout state. Painting happens every
 *     frame, so avoid allocating there.</li>
 *     <li>Call {@link #invalidateLayout()} whenever something that {@link #measure} reads changes.</li>
 *     <li>Read time, the clipboard and text sizes through {@link #environment()}, never from the system directly,
 *     so the component works on every engine and in tests.</li>
 *     <li>Event hooks run only while the component is attached, and a disabled or hidden component gets no new
 *     pointer or keyboard input. Consume the events you act on, so ancestors and the engine do not act on them
 *     too.</li>
 *     <li>Hooks run on the UI thread. Exceptions thrown from them propagate to the engine's call into Strata.</li>
 * </ul>
 *
 * @param <T> the type callers see the component as: the subclass itself, or an interface it implements
 */
public abstract class CustomComponent<T extends Component<T>> implements Component<T> {
    private final ComponentPeer peer;

    /**
     * Creates the component and its peer.
     *
     * @param factory the factory of the host that will show this component
     */
    protected CustomComponent(final ComponentFactory factory) {
        this(factory, false);
    }

    @SuppressWarnings("this-escape")
    CustomComponent(final ComponentFactory factory, final boolean container) {
        Objects.requireNonNull(factory, "factory");
        final Hooks hooks = new Hooks();
        this.peer = container ? factory.createContainerPeer(this, hooks) : factory.createPeer(this, hooks);
    }

    /**
     * Returns Strata's half of this component. Strata's implementation uses it to find the component's state; do
     * not call it.
     *
     * @return the peer
     */
    public final ComponentPeer peer() {
        return this.peer;
    }

    /**
     * Returns this component as the type callers see, for returning from your own chaining setters.
     *
     * @return {@code this}
     */
    @SuppressWarnings("unchecked")
    protected final T self() {
        return (T) this;
    }

    /**
     * Returns text measurement, time and the clipboard, for use outside {@link #measure}: in event hooks and in
     * {@link #paint}. Works before the component is attached.
     *
     * @return the host's environment
     */
    protected final Environment environment() {
        return this.peer.environment();
    }

    // ------------------------------------------------------------------
    // Hooks for subclasses
    // ------------------------------------------------------------------

    /**
     * Returns the size the content needs within the given space. Strata has already removed the margin and applied
     * the maximum size; it applies the minimum size and any fixed size to what you return. Containers must measure
     * each child here with {@link MeasureContext#measure}.
     *
     * @param context text measurement and child measuring
     * @param availableWidth the width offered; may be {@link Float#POSITIVE_INFINITY}
     * @param availableHeight the height offered; may be {@link Float#POSITIVE_INFINITY}
     * @return the natural size of the content; a negative value counts as zero
     */
    protected abstract Size measure(MeasureContext context, float availableWidth, float availableHeight);

    /**
     * Draws the component. The {@linkplain #background() background} is already drawn, and a container's children
     * are drawn after this returns, on top. The default draws nothing.
     *
     * @param painter what to draw with; valid only during this call
     * @param bounds the component's bounds, the same as {@link #bounds()}
     */
    protected void paint(final Painter painter, final Rect bounds) {
    }

    /**
     * Returns the style keys this component needs from a theme. A host refuses to show the component with a theme
     * that lacks any of them, and lists every missing key. Read optional keys in {@link #onApplyTheme} with a
     * fallback instead of listing them here. The default needs none.
     *
     * @return the required keys; may be empty, must not be null
     */
    protected Set<String> requiredStyleKeys() {
        return Set.of();
    }

    /**
     * Reads paints and text styles from the theme. Called when the component is attached to a host, and again
     * whenever the host's theme changes, always before the next layout. The required keys are present. The default
     * does nothing.
     *
     * @param theme the host's theme
     */
    protected void onApplyTheme(final Theme theme) {
    }

    /**
     * Returns whether focusing this component should start text input on the engine. Return true for components
     * that take typed characters: some engines, SDL-based ones among them, send no
     * {@link org.inspirenxe.strata.api.event.CharEvent}s otherwise. The default is false.
     *
     * @return true if the component accepts typed text
     */
    protected boolean acceptsTextInput() {
        return false;
    }

    /**
     * Default behavior for {@link EventTypes#POINTER_ENTER}. The default does nothing.
     *
     * @param event the event
     */
    protected void onPointerEnter(final PointerEvent event) {
    }

    /**
     * Default behavior for {@link EventTypes#POINTER_EXIT}. The default does nothing.
     *
     * @param event the event
     */
    protected void onPointerExit(final PointerEvent event) {
    }

    /**
     * Default behavior for {@link EventTypes#POINTER_MOVE}. The default does nothing.
     *
     * @param event the event
     */
    protected void onPointerMove(final PointerEvent event) {
    }

    /**
     * Default behavior for {@link EventTypes#POINTER_PRESS}. The default does nothing.
     *
     * @param event the event
     */
    protected void onPointerPress(final PointerButtonEvent event) {
    }

    /**
     * Default behavior for {@link EventTypes#POINTER_RELEASE}. The default does nothing.
     *
     * @param event the event
     */
    protected void onPointerRelease(final PointerButtonEvent event) {
    }

    /**
     * Default behavior for {@link EventTypes#CLICK}. The default does nothing.
     *
     * @param event the event
     */
    protected void onClick(final PointerButtonEvent event) {
    }

    /**
     * Default behavior for {@link EventTypes#DRAG_START}. The default does nothing.
     *
     * @param event the event
     */
    protected void onDragStart(final DragEvent event) {
    }

    /**
     * Default behavior for {@link EventTypes#DRAG}. The default does nothing.
     *
     * @param event the event
     */
    protected void onDrag(final DragEvent event) {
    }

    /**
     * Default behavior for {@link EventTypes#DRAG_END}. The default does nothing.
     *
     * @param event the event
     */
    protected void onDragEnd(final DragEvent event) {
    }

    /**
     * Default behavior for {@link EventTypes#SCROLL}. The default does nothing.
     *
     * @param event the event
     */
    protected void onScroll(final ScrollEvent event) {
    }

    /**
     * Default behavior for {@link EventTypes#KEY}. The default does nothing.
     *
     * @param event the event
     */
    protected void onKey(final KeyEvent event) {
    }

    /**
     * Default behavior for {@link EventTypes#CHAR}. The default does nothing.
     *
     * @param event the event
     */
    protected void onChar(final CharEvent event) {
    }

    /**
     * Default behavior for {@link EventTypes#FOCUS_GAINED}. The default does nothing.
     *
     * @param event the event
     */
    protected void onFocusGained(final FocusEvent event) {
    }

    /**
     * Default behavior for {@link EventTypes#FOCUS_LOST}. The default does nothing.
     *
     * @param event the event
     */
    protected void onFocusLost(final FocusEvent event) {
    }

    /**
     * Default behavior for event types other than those in {@link EventTypes}. The default does nothing.
     *
     * @param type the event's type
     * @param event the event
     */
    protected void onOtherEvent(final EventType<?> type, final Event event) {
    }

    /**
     * Returns this component's own state for a {@link ComponentSnapshot}, which stores it as
     * {@link ComponentSnapshot#state()}. Strata calls this only when an event that has snapshots fires and this
     * component is captured, before any handler runs.
     *
     * <p>Return an immutable value, such as a record, that a handler can compare with the live component later.
     * Copy mutable data; returning a live list or builder makes the snapshot change with the component. Record only
     * this component's own state: a container must not include its children, which are captured separately. The
     * state Strata already keeps for every component (bounds, flags, margin and so on) is in the snapshot already.
     * Do not change any component from here. The default returns null.</p>
     *
     * @return the state, or null for none
     */
    protected Object snapshotState() {
        return null;
    }

    // Overridden by CustomComposite
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
        if (type == EventTypes.POINTER_ENTER) {
            this.onPointerEnter((PointerEvent) event);
        } else if (type == EventTypes.POINTER_EXIT) {
            this.onPointerExit((PointerEvent) event);
        } else if (type == EventTypes.POINTER_MOVE) {
            this.onPointerMove((PointerEvent) event);
        } else if (type == EventTypes.POINTER_PRESS) {
            this.onPointerPress((PointerButtonEvent) event);
        } else if (type == EventTypes.POINTER_RELEASE) {
            this.onPointerRelease((PointerButtonEvent) event);
        } else if (type == EventTypes.CLICK) {
            this.onClick((PointerButtonEvent) event);
        } else if (type == EventTypes.DRAG_START) {
            this.onDragStart((DragEvent) event);
        } else if (type == EventTypes.DRAG) {
            this.onDrag((DragEvent) event);
        } else if (type == EventTypes.DRAG_END) {
            this.onDragEnd((DragEvent) event);
        } else if (type == EventTypes.SCROLL) {
            this.onScroll((ScrollEvent) event);
        } else if (type == EventTypes.KEY) {
            this.onKey((KeyEvent) event);
        } else if (type == EventTypes.CHAR) {
            this.onChar((CharEvent) event);
        } else if (type == EventTypes.FOCUS_GAINED) {
            this.onFocusGained((FocusEvent) event);
        } else if (type == EventTypes.FOCUS_LOST) {
            this.onFocusLost((FocusEvent) event);
        } else {
            this.onOtherEvent(type, event);
        }
    }

    /**
     * The only way Strata reaches the protected hooks. Private, so nothing else can call them.
     */
    private final class Hooks implements ComponentHooks {
        @Override
        public Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
            return CustomComponent.this.measure(context, availableWidth, availableHeight);
        }

        @Override
        public void arrange(final ArrangeContext context, final Rect bounds) {
            CustomComponent.this.arrangeHook(context, bounds);
        }

        @Override
        public void paint(final Painter painter, final Rect bounds) {
            CustomComponent.this.paint(painter, bounds);
        }

        @Override
        public Set<String> requiredStyleKeys() {
            return CustomComponent.this.requiredStyleKeys();
        }

        @Override
        public void applyTheme(final Theme theme) {
            CustomComponent.this.onApplyTheme(theme);
        }

        @Override
        public boolean acceptsTextInput() {
            return CustomComponent.this.acceptsTextInput();
        }

        @Override
        public void handle(final EventType<?> type, final Event event) {
            CustomComponent.this.handleEvent(type, event);
        }

        @Override
        public Object snapshotState() {
            return CustomComponent.this.snapshotState();
        }

        @Override
        public void childRemoved(final Component<?> child) {
            CustomComponent.this.childRemovedHook(child);
        }

        @Override
        public Rect childClip() {
            return CustomComponent.this.childClipHook();
        }

        @Override
        public void paintOverChildren(final Painter painter, final Rect bounds) {
            CustomComponent.this.paintOverChildrenHook(painter, bounds);
        }

        @Override
        public void descendantFocused(final Component<?> descendant) {
            CustomComponent.this.descendantFocusedHook(descendant);
        }
    }

    // ------------------------------------------------------------------
    // Component, delegated to the peer. Documented on Component.
    // ------------------------------------------------------------------

    @Override
    public final Container<?> parent() {
        return this.peer.parent();
    }

    @Override
    public final boolean isAttached() {
        return this.peer.isAttached();
    }

    @Override
    public final Theme theme() {
        return this.peer.theme();
    }

    @Override
    public final String id() {
        return this.peer.id();
    }

    @Override
    public final T setId(final String id) {
        this.peer.setId(id);
        return this.self();
    }

    @Override
    public final boolean isVisible() {
        return this.peer.isVisible();
    }

    @Override
    public final T setVisible(final boolean visible) {
        this.peer.setVisible(visible);
        return this.self();
    }

    @Override
    public final boolean isEnabled() {
        return this.peer.isEnabled();
    }

    @Override
    public final T setEnabled(final boolean enabled) {
        this.peer.setEnabled(enabled);
        return this.self();
    }

    @Override
    public final boolean isFocusable() {
        return this.peer.isFocusable();
    }

    @Override
    public final T setFocusable(final boolean focusable) {
        this.peer.setFocusable(focusable);
        return this.self();
    }

    @Override
    public final boolean isTabbable() {
        return this.peer.isTabbable();
    }

    @Override
    public final T setTabbable(final boolean tabbable) {
        this.peer.setTabbable(tabbable);
        return this.self();
    }

    @Override
    public final int tabIndex() {
        return this.peer.tabIndex();
    }

    @Override
    public final T setTabIndex(final int tabIndex) {
        this.peer.setTabIndex(tabIndex);
        return this.self();
    }

    @Override
    public final CursorKind cursor() {
        return this.peer.cursor();
    }

    @Override
    public final T setCursor(final CursorKind cursor) {
        this.peer.setCursor(cursor);
        return this.self();
    }

    @Override
    public final boolean isHovered() {
        return this.peer.isHovered();
    }

    @Override
    public final boolean isHoverWithin() {
        return this.peer.isHoverWithin();
    }

    @Override
    public final boolean isPressed() {
        return this.peer.isPressed();
    }

    @Override
    public final boolean isFocused() {
        return this.peer.isFocused();
    }

    @Override
    public final boolean requestFocus() {
        return this.peer.requestFocus();
    }

    @Override
    public final Insets margin() {
        return this.peer.margin();
    }

    @Override
    public final T setMargin(final Insets margin) {
        this.peer.setMargin(margin);
        return this.self();
    }

    @Override
    public final float minWidth() {
        return this.peer.minWidth();
    }

    @Override
    public final float maxWidth() {
        return this.peer.maxWidth();
    }

    @Override
    public final float minHeight() {
        return this.peer.minHeight();
    }

    @Override
    public final float maxHeight() {
        return this.peer.maxHeight();
    }

    @Override
    public final T setMinSize(final float minWidth, final float minHeight) {
        this.peer.setMinSize(minWidth, minHeight);
        return this.self();
    }

    @Override
    public final T setMaxSize(final float maxWidth, final float maxHeight) {
        this.peer.setMaxSize(maxWidth, maxHeight);
        return this.self();
    }

    @Override
    public final T setWidth(final float width) {
        this.peer.setWidth(width);
        return this.self();
    }

    @Override
    public final T setHeight(final float height) {
        this.peer.setHeight(height);
        return this.self();
    }

    @Override
    public final HorizontalAlignment horizontalAlignment() {
        return this.peer.horizontalAlignment();
    }

    @Override
    public final VerticalAlignment verticalAlignment() {
        return this.peer.verticalAlignment();
    }

    @Override
    public final T setAlignment(final HorizontalAlignment horizontal, final VerticalAlignment vertical) {
        this.peer.setAlignment(horizontal, vertical);
        return this.self();
    }

    @Override
    public final float offsetX() {
        return this.peer.offsetX();
    }

    @Override
    public final float offsetY() {
        return this.peer.offsetY();
    }

    @Override
    public final T setOffset(final float offsetX, final float offsetY) {
        this.peer.setOffset(offsetX, offsetY);
        return this.self();
    }

    @Override
    public final Border border() {
        return this.peer.border();
    }

    @Override
    public final T setBorder(final Border border) {
        this.peer.setBorder(border);
        return this.self();
    }

    @Override
    public final Tooltip tooltip() {
        return this.peer.tooltip();
    }

    @Override
    public final T setTooltip(final Tooltip tooltip) {
        this.peer.setTooltip(tooltip);
        return this.self();
    }

    @Override
    public final Paint background() {
        return this.peer.background();
    }

    @Override
    public final T setBackground(final Paint background) {
        this.peer.setBackground(background);
        return this.self();
    }

    @Override
    public final Rect bounds() {
        return this.peer.bounds();
    }

    @Override
    public final float desiredWidth() {
        return this.peer.desiredWidth();
    }

    @Override
    public final float desiredHeight() {
        return this.peer.desiredHeight();
    }

    @Override
    public final void invalidateLayout() {
        this.peer.invalidateLayout();
    }

    @Override
    public final <E extends Event> T onEvent(final EventType<E> type, final int priority,
                                             final EventHandler<? super E> handler) {
        this.peer.subscribe(type, priority, handler);
        return this.self();
    }

    @Override
    public final <E extends Event> Subscription subscribe(final EventType<E> type, final int priority,
                                                          final EventHandler<? super E> handler) {
        return this.peer.subscribe(type, priority, handler);
    }

    /**
     * Asks whether the user may do something this component is about to do, by firing one of its request events
     * (a type made with {@link EventType#request}) through its path. Call it only for the user's actions, never for
     * changes made from code. With no handler for the type on the path, it fires nothing, allocates nothing and
     * returns true.
     *
     * @param type the request type
     * @return true if the component should go ahead, false if a handler canceled the request
     * @throws IllegalArgumentException if the type carries a value; use the other {@code request}
     */
    protected final boolean request(final EventType<? extends RequestEvent<?>> type) {
        return this.peer.request(type);
    }

    /**
     * Asks whether the user may make a change, and with which value, by firing one of this component's value
     * requests (a type made with {@link EventType#valueRequest}) through its path, then applies the final value
     * unless a handler canceled the request. Call it only for the user's actions. With no handler for the type on
     * the path, it applies the value at once without firing anything.
     *
     * @param <V> the value type
     * @param type the request type
     * @param value the value the user's action produced
     * @param apply makes the change with the final value; not called if the request was canceled
     */
    protected final <V> void request(final EventType<? extends ValueRequestEvent<?, V>> type, final V value,
                                     final Consumer<? super V> apply) {
        this.peer.request(type, value, apply);
    }

    /**
     * Reports that something happened, by firing one of this component's report events (a type made with
     * {@link EventType#report}) through its path. Call it after the fact, whether the user or code caused it. With
     * no handler for the type on the path, it fires nothing.
     *
     * @param type the report type
     * @throws IllegalArgumentException if the type carries a value; use the other {@code report}
     */
    protected final void report(final EventType<? extends ControlEvent<?>> type) {
        this.peer.report(type);
    }

    /**
     * Reports a change of value, by firing one of this component's value reports (a type made with
     * {@link EventType#valueReport}) through its path. Call it after the change, whether the user or code made it.
     * With no handler for the type on the path, it fires nothing.
     *
     * @param <V> the value type
     * @param type the report type
     * @param value the value after the change
     * @param previous the value before the change
     */
    protected final <V> void report(final EventType<? extends ValueEvent<?, V>> type, final V value,
                                    final V previous) {
        this.peer.report(type, value, previous);
    }

    /**
     * Returns whether a handler for a type is on this component's path: on the component itself or, for a
     * bubbling type, on any ancestor. Use it to skip work that only an event needs.
     *
     * @param type the event type
     * @return true if firing the type would reach a handler
     */
    protected final boolean isHandled(final EventType<?> type) {
        return this.peer.isHandled(type);
    }

    /**
     * Returns the class name and, if set, the id, for debugging.
     *
     * @return for example {@code ColorPickerImpl#accent}
     */
    @Override
    public String toString() {
        final String id = this.peer.id();
        return this.getClass().getSimpleName() + (id == null ? "" : "#" + id);
    }
}
