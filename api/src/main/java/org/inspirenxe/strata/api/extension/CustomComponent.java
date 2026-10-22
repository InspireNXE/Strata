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
package org.inspirenxe.strata.api.extension;

import org.inspirenxe.strata.api.ComponentFactory;
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
import org.inspirenxe.strata.api.extension.peer.ComponentHooks;
import org.inspirenxe.strata.api.extension.peer.ComponentPeer;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.input.TextInputKind;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.layout.VerticalAlignment;
import org.inspirenxe.strata.api.state.PropertyKey;
import org.inspirenxe.strata.api.state.Source;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Border;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Base class for new kinds of component. Strata's own controls are not built on it, so a component that is an
 * instance of this class was written outside Strata.
 *
 * <p>Subclass it, implement {@link #measure}, and override the other {@code protected} hooks you need. To let other
 * code see the component as a type of your own, declare an interface that extends {@link Component} and implement
 * it here:</p>
 *
 * <pre>{@code
 * public interface ColorSwatch extends Component<ColorSwatch>, Focusable<ColorSwatch> {
 *     int color();
 * }
 *
 * final class ColorSwatchImpl extends CustomComponent<ColorSwatch> implements ColorSwatch {
 *     private int color = 0xFFFF0000;
 *
 *     ColorSwatchImpl(ComponentFactory factory) {
 *         super(factory);
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
 * <p>The methods of {@link Focusable}, {@link HasCursor} and {@link HasTooltip} are here too, so a subclass takes
 * part in focus, chooses a cursor or shows a tooltip just by declaring the interface, as {@code ColorSwatch} does
 * above. Strata ignores them on a subclass that does not declare the interface. A component that implements
 * {@link Focusable} starts out focusable and tabbable.</p>
 *
 * <h2>Requirements for subclasses</h2>
 * <ul>
 *     <li>Pass the factory of the host that will show the component; a component cannot move between hosts.</li>
 *     <li>{@link #measure} must be pure layout: compute and return a size, and cache what {@link #paint} needs. Do
 *     not change other components or call {@link #invalidateLayout()} from it.</li>
 *     <li>{@link #paint} draws inside the given bounds and must not change layout state. Painting happens every
 *     frame, so avoid allocating there.</li>
 *     <li>Call {@link #invalidateLayout()} whenever something that {@link #measure} or {@link #measureMinimum}
 *     reads changes.</li>
 *     <li>Read time, the clipboard and text sizes through {@link #environment()}, never from the system directly,
 *     so the component works on every engine and in tests.</li>
 *     <li>Event hooks run only while the component is attached, and a disabled or hidden component gets no new
 *     pointer or keyboard input, nor does one inside it. Consume the events you act on, so ancestors and the engine
 *     do not act on them too.</li>
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
        if (this instanceof Focusable<?>) {
            this.peer.setFocusable(true);
            this.peer.setTabbable(true);
        }
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
     * Returns the size the content needs within the given space. Strata has already removed the margin, put a fixed
     * size in place of the space offered on an axis that has one, and applied the maximum size; it applies the
     * minimum size and any fixed size to what you return. Containers must measure each child here with
     * {@link MeasureContext#measure}. Strata calls this during layout only when something changed: a component that
     * did not {@linkplain #invalidateLayout() invalidate its layout} and is offered the same space as last time keeps
     * its last size without this hook running.
     *
     * @param context text measurement and child measuring
     * @param availableWidth the width offered; may be {@link Float#POSITIVE_INFINITY}
     * @param availableHeight the height offered; may be {@link Float#POSITIVE_INFINITY}
     * @return the natural size of the content; a negative value counts as zero
     */
    protected abstract Size measure(MeasureContext context, float availableWidth, float availableHeight);

    /**
     * Returns the smallest size the content can work in. A parent that shares space among its children, such as a
     * stack panel with weighted children or a grid with weighted tracks, gives no child less than this, read through
     * {@link MeasureContext#minimumSize}. The component itself is not laid out larger because of it: {@link #measure}
     * may still be offered less, for example inside a fixed size. The default returns {@link Size#ZERO}, so only the
     * {@linkplain #setMinSize minimum size} counts.
     *
     * <p>Return what does not depend on the space offered: the longest word of wrapping text, a caption that cannot
     * shrink, or the sum of the children's minimums read with {@link MeasureContext#minimumSize}. Do not measure
     * children here. Strata raises the result to the minimum size, caps it by the maximum size and puts the fixed
     * size in its place on an axis that has one. It calls this hook when a parent first asks after the layout was
     * {@linkplain #invalidateLayout() invalidated}, which can come before or after {@link #measure}, so call
     * {@link #invalidateLayout()} whenever something it reads changes. It is not called while the component is
     * hidden.</p>
     *
     * @param context text measurement and the children's minimums
     * @return the smallest size of the content; a negative or non-finite value counts as zero
     */
    protected Size measureMinimum(final MeasureContext context) {
        return Size.ZERO;
    }

    /**
     * Returns the distance from the top of the component's bounds to the baseline of its first line of text, for
     * parents that line up their children's text ({@link VerticalAlignment#BASELINE}). Strata calls it during the
     * parent's layout, after {@link #measure}, for the component at its desired height, so work it out from what
     * {@code measure} cached and the text metrics' {@link org.inspirenxe.strata.api.text.TextMetrics#ascent ascent}:
     * a component that draws a line of text at a top of {@code y} has its baseline at {@code y} plus the ascent of
     * the line's style. It must not change layout state. It is not called while the component is hidden. The default
     * returns NaN, for no baseline, and a parent lining up baselines then puts the component's bottom on the line.
     *
     * @return the baseline in UI units from the top of the bounds, or NaN for none
     */
    protected float baseline() {
        return Float.NaN;
    }

    /**
     * Draws the component. The {@linkplain #background() background} and border are already drawn, and a
     * container's children are drawn after this returns, on top. The default draws nothing.
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
     * @param theme the host's theme, with the text style patches of the containers around the component applied
     */
    protected void onApplyTheme(final Theme theme) {
    }

    /**
     * Returns whether focusing this component should start text input on the engine. Return true for components
     * that take typed characters: some engines, SDL-based ones among them, send no
     * {@link org.inspirenxe.strata.api.event.input.CharEvent}s otherwise. The default is false.
     *
     * @return true if the component accepts typed text
     */
    protected boolean acceptsTextInput() {
        return false;
    }

    /**
     * Returns what kind of text this component takes, which Strata passes to the engine when the component gains
     * focus, so the engine can turn off input methods, prediction and suggestions for secrets. Read only while
     * {@link #acceptsTextInput()} is true. The default is {@link TextInputKind#TEXT}.
     *
     * @return the kind of text
     */
    protected TextInputKind textInputKind() {
        return TextInputKind.TEXT;
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
     * Default behavior for an event type that has no hook of its own here, such as
     * {@link InputEvents#MODIFIERS_CHANGED}, which reaches the screen's content and each overlay's. Control events,
     * the requests and reports controls fire, have no default behavior and never reach this hook. The default does
     * nothing.
     *
     * @param type the event's type
     * @param event the event
     */
    protected void onOtherEvent(final EventType<?> type, final Event event) {
    }

    /**
     * Returns this component's own state for a {@link ComponentSnapshot}, which stores it as
     * {@link ComponentSnapshot#state()}. Strata calls this only when a
     * {@link org.inspirenxe.strata.api.event.SnapshotEvent} fires and this component is captured, before any handler
     * runs.
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
     * The only way Strata reaches the protected hooks. Private, so nothing else can call them.
     */
    private final class Hooks implements ComponentHooks {
        @Override
        public Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
            return CustomComponent.this.measure(context, availableWidth, availableHeight);
        }

        @Override
        public Size measureMinimum(final MeasureContext context) {
            return CustomComponent.this.measureMinimum(context);
        }

        @Override
        public float baseline() {
            return CustomComponent.this.baseline();
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
        public TextInputKind textInputKind() {
            return CustomComponent.this.textInputKind();
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
    public final Component<?> parent() {
        return this.peer.parent();
    }

    @Override
    public final <P> Optional<P> parent(final Class<P> type) {
        return this.peer.parent(type);
    }

    @Override
    public final <A> Optional<A> ancestor(final Class<A> type) {
        return this.peer.ancestor(type);
    }

    @Override
    public final boolean isAttached() {
        return this.peer.isAttached();
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
    public final boolean isEffectivelyEnabled() {
        return Component.super.isEffectivelyEnabled();
    }

    @Override
    public final boolean isEffectivelyVisible() {
        return Component.super.isEffectivelyVisible();
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
    public final T setMargin(final float all) {
        return Component.super.setMargin(all);
    }

    @Override
    public final T setMargin(final float horizontal, final float vertical) {
        return Component.super.setMargin(horizontal, vertical);
    }

    @Override
    public final T setMargin(final float left, final float top, final float right, final float bottom) {
        return Component.super.setMargin(left, top, right, bottom);
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
    public final T setSize(final float width, final float height) {
        return Component.super.setSize(width, height);
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
    public final T setAnchor(final Anchor anchor) {
        return Component.super.setAnchor(anchor);
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
    public final int zIndex() {
        return this.peer.zIndex();
    }

    @Override
    public final T setZIndex(final int zIndex) {
        this.peer.setZIndex(zIndex);
        return this.self();
    }

    // ------------------------------------------------------------------
    // Focusable, HasCursor and HasTooltip, for subclasses that declare them. Strata ignores these on the rest.
    // ------------------------------------------------------------------

    /**
     * Returns whether the component can take keyboard focus. See {@link Focusable#isFocusable()}.
     *
     * @return true if focusable; always false unless the subclass implements {@link Focusable}
     */
    public final boolean isFocusable() {
        return this.peer.isFocusable();
    }

    /**
     * Sets whether the component can take keyboard focus. See {@link Focusable#setFocusable(boolean)}.
     *
     * @param focusable false to refuse focus
     * @return this component
     */
    public final T setFocusable(final boolean focusable) {
        this.peer.setFocusable(focusable);
        return this.self();
    }

    /**
     * Returns whether Tab and Shift+Tab stop at this component. See {@link Focusable#isTabbable()}.
     *
     * @return true if tabbable
     */
    public final boolean isTabbable() {
        return this.peer.isTabbable();
    }

    /**
     * Sets whether Tab and Shift+Tab stop at this component. See {@link Focusable#setTabbable(boolean)}.
     *
     * @param tabbable false to leave the component out of tab order
     * @return this component
     */
    public final T setTabbable(final boolean tabbable) {
        this.peer.setTabbable(tabbable);
        return this.self();
    }

    /**
     * Returns the tab index. See {@link Focusable#tabIndex()}.
     *
     * @return the tab index; 0 by default
     */
    public final int tabIndex() {
        return this.peer.tabIndex();
    }

    /**
     * Sets the tab index. See {@link Focusable#setTabIndex(int)}.
     *
     * @param tabIndex the tab index
     * @return this component
     */
    public final T setTabIndex(final int tabIndex) {
        this.peer.setTabIndex(tabIndex);
        return this.self();
    }

    /**
     * Returns whether this component has keyboard focus. See {@link Focusable#isFocused()}.
     *
     * @return true while focused
     */
    public final boolean isFocused() {
        return this.peer.isFocused();
    }

    /**
     * Asks the host to give this component keyboard focus. See {@link Focusable#requestFocus()}.
     *
     * @return true if the component now has focus; always false unless the subclass implements {@link Focusable}
     */
    public final boolean requestFocus() {
        return this.peer.requestFocus();
    }

    /**
     * Returns the cursor shown over this component. See {@link HasCursor#cursor()}.
     *
     * @return the cursor, or {@code null} to use the nearest ancestor's
     */
    public final CursorKind cursor() {
        return this.peer.cursor();
    }

    /**
     * Sets the cursor shown over this component. See {@link HasCursor#setCursor(CursorKind)}.
     *
     * @param cursor the cursor, or {@code null} to use the nearest ancestor's
     * @return this component
     */
    public final T setCursor(final CursorKind cursor) {
        this.peer.setCursor(cursor);
        return this.self();
    }

    /**
     * Returns the tooltip. See {@link HasTooltip#tooltip()}.
     *
     * @return the tooltip, or {@code null} for none
     */
    public final Tooltip tooltip() {
        return this.peer.tooltip();
    }

    /**
     * Sets the tooltip. See {@link HasTooltip#setTooltip(Tooltip)}.
     *
     * @param tooltip the tooltip, or {@code null} for none
     * @return this component
     */
    public final T setTooltip(final Tooltip tooltip) {
        this.peer.setTooltip(tooltip);
        return this.self();
    }

    // ------------------------------------------------------------------
    // State for the subclass's own use
    // ------------------------------------------------------------------

    /**
     * Returns the theme applied to this component, for reading styles outside {@link #onApplyTheme}.
     *
     * @return the host's theme, with the {@linkplain org.inspirenxe.strata.api.component.Container#setTextStyle text
     *     style patches} of the containers around the component applied, or {@code null} before the component is
     *     first attached
     */
    protected final Theme theme() {
        return this.peer.theme();
    }

    /**
     * Turns one of this component's own style states on or off, such as {@code "selected"} or {@code "warning"}, so
     * {@link #stylePaint} and {@link #styleText} pick the theme's look for it: with {@code "warning"} on, a lookup of
     * {@code fancy.card} finds {@code fancy.card.warning} when the theme has it. Strata sets {@code disabled},
     * {@code pressed}, {@code hovered} and {@code focused} on every component itself, so those names cannot be set
     * here. A state's place in a lookup is the order in which it was first set. Changing a state marks nothing for
     * layout; call {@link #invalidateLayout()} if the styles it picks change the component's size.
     *
     * @param state the state's name; any name but the four Strata sets
     * @param on whether the state is on
     * @throws IllegalArgumentException if the name is empty or one Strata sets, or the component already has 60
     *     states of its own
     */
    protected final void setStyleState(final String state, final boolean on) {
        this.peer.setStyleState(state, on);
    }

    /**
     * Looks up a paint from the theme for this component's style states. The lookup tries {@code key.disabled}
     * while the component is disabled, itself or through an ancestor, then {@code key.<state>} for each of the
     * component's own states that is on, in the order they were first set, then {@code key.pressed},
     * {@code key.hovered} and {@code key.focused} for those that are on, and last the key itself. The first key
     * the theme has wins, so with {@code "warning"} on, {@code fancy.card.warning} wins over {@code fancy.card},
     * and a disabled card shows {@code fancy.card.disabled}. The result for each combination of states is kept
     * until the theme changes, so calling this from {@link #paint} allocates nothing once each combination was
     * looked up.
     *
     * @param key the style key, such as {@code fancy.card}
     * @param fallback returned when the theme has none of the keys tried, or the component was never attached
     * @return the paint
     */
    protected final Paint stylePaint(final String key, final Paint fallback) {
        return this.peer.stylePaint(key, fallback);
    }

    /**
     * Looks up a text style from the theme for this component's style states, trying the keys in the order
     * {@link #stylePaint} does.
     *
     * @param key the style key, such as {@code fancy.card.text}
     * @param fallback returned when the theme has none of the keys tried, or the component was never attached
     * @return the text style
     */
    protected final TextStyle styleText(final String key, final TextStyle fallback) {
        return this.peer.styleText(key, fallback);
    }

    /**
     * Returns whether the pointer is over this component. Only the deepest component under the pointer counts as
     * hovered; its ancestors do not. A component that is disabled, or inside a disabled one, is never hovered.
     *
     * @return true while hovered
     */
    protected final boolean isHovered() {
        return this.peer.isHovered();
    }

    /**
     * Returns whether the pointer is over this component or any of its descendants, so a list can light up the row
     * whose label is hovered.
     *
     * @return true while this component or a descendant is hovered
     */
    protected final boolean isHoverWithin() {
        return this.peer.isHoverWithin();
    }

    /**
     * Returns whether a mouse button went down on this component and has not been released yet. Stays true while
     * the pointer is dragged away.
     *
     * @return true while pressed
     */
    protected final boolean isPressed() {
        return this.peer.isPressed();
    }

    /**
     * Returns the horizontal offset.
     *
     * @return how far the component is moved right of where layout puts it; 0 by default
     */
    protected float offsetX() {
        return this.peer.offsetX();
    }

    /**
     * Returns the vertical offset.
     *
     * @return how far the component is moved down from where layout puts it; 0 by default
     */
    protected float offsetY() {
        return this.peer.offsetY();
    }

    /**
     * Moves the component from where its parent's layout puts it, along with everything inside it. Siblings and the
     * parent's size do not change, so this suits dragging, such as moving a window by its title bar. A subclass may
     * make it public, the way {@link org.inspirenxe.strata.api.control.window.Window} exposes its offset.
     *
     * @param offsetX the distance to move right, in UI units; negative moves left
     * @param offsetY the distance to move down, in UI units; negative moves up
     * @return this component
     */
    protected T setOffset(final float offsetX, final float offsetY) {
        this.peer.setOffset(offsetX, offsetY);
        return this.self();
    }

    /**
     * Returns the background paint.
     *
     * @return the background; {@link Paint#NONE} by default
     */
    protected Paint background() {
        return this.peer.background();
    }

    /**
     * Sets the paint drawn over the component's bounds before anything else of it. Containers make this public.
     *
     * @param background the background, or {@link Paint#NONE} for none
     * @return this component
     */
    protected T setBackground(final Paint background) {
        this.peer.setBackground(background);
        return this.self();
    }

    /**
     * Returns the border.
     *
     * @return the border; {@link Border#NONE} by default
     */
    protected Border border() {
        return this.peer.border();
    }

    /**
     * Sets the lines drawn along the inside of the component's edges, over its background and under its content.
     * Containers make this public.
     *
     * @param border the border, or {@link Border#NONE} for none
     * @return this component
     */
    protected T setBorder(final Border border) {
        this.peer.setBorder(border);
        return this.self();
    }

    /**
     * Schedules a new layout before the next frame. Call it whenever something that {@link #measure} reads
     * changes. The component's own setters for size, margin and the like already do this.
     */
    protected final void invalidateLayout() {
        this.peer.invalidateLayout();
    }

    @Override
    public final <E extends Event> T onEvent(final EventType<E> type, final EventHandler<? super E> handler) {
        return this.onEvent(type, 0, handler);
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

    @Override
    public final <V> T bind(final PropertyKey<? super T, V> key, final Source<V> source) {
        this.peer.bind(key, source);
        return this.self();
    }

    /**
     * Asks whether the user may do something this component is about to do, by firing one of its request events
     * (a type made with {@link EventType#request}) through its path. Call it for the user's actions, from the
     * component's input handlers. It fires only when the current origin is {@code Origin.User}, which the host puts
     * for every input; for any other origin, such as code or a binding, it fires nothing and returns true. With no
     * handler for the type on the path, it fires nothing, allocates nothing and returns true.
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
     * unless a handler canceled the request. Call it for the user's actions. Like the other {@code request}, it
     * fires only when the current origin is {@code Origin.User}. Otherwise, or with no handler for the type on the
     * path, it applies the value at once without firing anything.
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
     * bubbling type, on any ancestor the event would reach. Use it to skip work that only an event needs.
     *
     * @param type the event type
     * @return true if firing the type would reach a handler
     */
    protected final boolean isHandled(final EventType<?> type) {
        return this.peer.isHandled(type);
    }

    /**
     * Returns the class name, for debugging.
     *
     * @return for example {@code ColorSwatchImpl}
     */
    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }
}
