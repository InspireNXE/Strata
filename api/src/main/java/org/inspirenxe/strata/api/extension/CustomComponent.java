package org.inspirenxe.strata.api.extension;

import org.inspirenxe.strata.api.Ui;
import org.inspirenxe.strata.api.component.Component;
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
import org.inspirenxe.strata.api.geom.Insets;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.geom.Size;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;

import java.util.Objects;
import java.util.Set;

/**
 * Base for new component types. Subclass it, override the {@code protected} hooks, and implement your own
 * interfaces if other code should see the component by a type of its own:
 *
 * <pre>{@code
 * public interface ColorPicker extends Component<ColorPicker> { ... }
 *
 * final class ColorPickerImpl extends CustomComponent<ColorPicker> implements ColorPicker {
 *     ColorPickerImpl(final Ui ui) {
 *         super(ui);
 *     }
 *
 *     @Override
 *     protected Size measure(final MeasureContext context, final float width, final float height) { ... }
 * }
 * }</pre>
 *
 * <p>Every {@link Component} method here is {@code final} and delegates to a peer that Strata creates in the
 * constructor; the peer holds the bounds, state and handlers the rest of Strata relies on. Strata's own widgets and
 * containers are built on this class too.</p>
 *
 * @param <T> the type callers see the component as: the subclass itself, or an interface it implements
 */
public abstract class CustomComponent<T extends Component<T>> implements Component<T> {
    private final ComponentPeer peer;

    protected CustomComponent(final Ui ui) {
        this(ui, null);
    }

    /**
     * @param behavior decorates this component's hooks, or {@code null}
     */
    protected CustomComponent(final Ui ui, final ComponentBehavior<T> behavior) {
        this(ui, behavior, false);
    }

    @SuppressWarnings("this-escape")
    CustomComponent(final Ui ui, final ComponentBehavior<T> behavior, final boolean container) {
        Objects.requireNonNull(ui, "ui");
        final Hooks hooks = new Hooks();
        this.peer = container ? ui.createContainerPeer(this, hooks, behavior) : ui.createPeer(this, hooks, behavior);
    }

    /**
     * Strata's half of this component. For Strata's implementation; not for use by subclasses or callers.
     */
    public final ComponentPeer peer() {
        return this.peer;
    }

    @SuppressWarnings("unchecked")
    protected final T self() {
        return (T) this;
    }

    // ------------------------------------------------------------------
    // Hooks for subclasses
    // ------------------------------------------------------------------

    /**
     * Returns the natural size of the content within the given space (margin already removed). Containers must
     * measure their children here through {@link MeasureContext#measure}.
     */
    protected abstract Size measure(MeasureContext context, float availableWidth, float availableHeight);

    /**
     * Draws the component inside its bounds. The background is already drawn; a container's children are drawn
     * afterwards.
     */
    protected void paint(final Painter painter, final Rect bounds) {
    }

    /**
     * The style keys this component needs from a theme. A theme that lacks any of them is rejected, listing every
     * missing key.
     */
    protected Set<String> requiredStyleKeys() {
        return Set.of();
    }

    /**
     * Resolves paints and text styles. Called before the first layout and on every theme change.
     */
    protected void onApplyTheme(final Theme theme) {
    }

    /**
     * Whether focusing this component should start text input on the engine.
     */
    protected boolean acceptsTextInput() {
        return false;
    }

    protected void onPointerEnter(final PointerEvent event) {
    }

    protected void onPointerExit(final PointerEvent event) {
    }

    protected void onPointerMove(final PointerEvent event) {
    }

    protected void onPointerPress(final PointerButtonEvent event) {
    }

    protected void onPointerRelease(final PointerButtonEvent event) {
    }

    protected void onClick(final PointerButtonEvent event) {
    }

    protected void onDragStart(final DragEvent event) {
    }

    protected void onDrag(final DragEvent event) {
    }

    protected void onDragEnd(final DragEvent event) {
    }

    protected void onScroll(final ScrollEvent event) {
    }

    protected void onKey(final KeyEvent event) {
    }

    protected void onChar(final CharEvent event) {
    }

    protected void onFocusGained(final FocusEvent event) {
    }

    protected void onFocusLost(final FocusEvent event) {
    }

    /**
     * Default behavior for event types defined outside Strata's core set.
     */
    protected void onOtherEvent(final EventType<?> type, final Event event) {
    }

    // Overridden by CustomContainer
    void arrangeHook(final ArrangeContext context, final Rect bounds) {
    }

    void childRemovedHook(final Component<?> child) {
    }

    private void handleEvent(final EventType<?> type, final Event event) {
        if (type == UIEvents.POINTER_ENTER) {
            this.onPointerEnter((PointerEvent) event);
        } else if (type == UIEvents.POINTER_EXIT) {
            this.onPointerExit((PointerEvent) event);
        } else if (type == UIEvents.POINTER_MOVE) {
            this.onPointerMove((PointerEvent) event);
        } else if (type == UIEvents.POINTER_PRESS) {
            this.onPointerPress((PointerButtonEvent) event);
        } else if (type == UIEvents.POINTER_RELEASE) {
            this.onPointerRelease((PointerButtonEvent) event);
        } else if (type == UIEvents.CLICK) {
            this.onClick((PointerButtonEvent) event);
        } else if (type == UIEvents.DRAG_START) {
            this.onDragStart((DragEvent) event);
        } else if (type == UIEvents.DRAG) {
            this.onDrag((DragEvent) event);
        } else if (type == UIEvents.DRAG_END) {
            this.onDragEnd((DragEvent) event);
        } else if (type == UIEvents.SCROLL) {
            this.onScroll((ScrollEvent) event);
        } else if (type == UIEvents.KEY) {
            this.onKey((KeyEvent) event);
        } else if (type == UIEvents.CHAR) {
            this.onChar((CharEvent) event);
        } else if (type == UIEvents.FOCUS_GAINED) {
            this.onFocusGained((FocusEvent) event);
        } else if (type == UIEvents.FOCUS_LOST) {
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
        public void childRemoved(final Component<?> child) {
            CustomComponent.this.childRemovedHook(child);
        }
    }

    // ------------------------------------------------------------------
    // Component, delegated to the peer
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
    public final <E extends Event> Subscription on(final EventType<E> type, final Phase phase,
                                                   final EventHandler<? super E> handler, final Object owner) {
        return this.peer.on(type, phase, handler, owner);
    }

    @Override
    public final void removeHandlers(final Object owner) {
        this.peer.removeHandlers(owner);
    }

    @Override
    public String toString() {
        final String id = this.peer.id();
        return this.getClass().getSimpleName() + (id == null ? "" : "#" + id);
    }
}
