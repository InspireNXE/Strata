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
package org.inspirenxe.strata.api.component;

import org.inspirenxe.strata.api.event.EventTarget;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.Subscription;
import org.inspirenxe.strata.api.event.component.LifecycleEvent;
import org.inspirenxe.strata.api.event.component.MeasureEvent;
import org.inspirenxe.strata.api.event.component.PaintEvent;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.layout.VerticalAlignment;
import org.inspirenxe.strata.api.state.PropertyKey;
import org.inspirenxe.strata.api.state.Source;
import org.inspirenxe.strata.api.state.State;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * An element of a Strata UI tree.
 *
 * <p>Do not implement this interface. Get components from a {@link org.inspirenxe.strata.api.ComponentFactory}, or
 * extend {@link org.inspirenxe.strata.api.extension.CustomComponent} to make a new kind. A class that implements
 * this interface directly has no Strata implementation behind it; adding one to a container throws
 * {@link IllegalArgumentException}.</p>
 *
 * <h2>Layout</h2>
 * <p>Layout runs in two passes before a frame is drawn, and only after something changed. First each component is
 * <em>measured</em>: its parent offers some space and the component reports the size it wants, its
 * {@linkplain #desiredWidth() desired size}. Then each component is <em>arranged</em>: its parent gives it a slot,
 * and the component places itself in the slot by its {@linkplain #margin() margin},
 * {@linkplain #horizontalAlignment() alignment} and size limits. The result is its {@linkplain #bounds() bounds}.
 * A component with {@code STRETCH} alignment fills its slot; with any other alignment it keeps its desired size and
 * may overflow a small slot, in which case a clipping parent cuts it off.</p>
 *
 * <p>Desired sizes exclude the margin. A fixed size set with {@link #setWidth}/{@link #setHeight} replaces the
 * measured size. Every size is clamped to the {@linkplain #minWidth() minimum} and {@linkplain #maxWidth()
 * maximum}.</p>
 *
 * <h2>Drawing order</h2>
 * <p>A parent draws its children by {@linkplain #zIndex() z index} and then in the order they were added, raised or
 * lowered, and the pointer hits them in the opposite order. See {@link #setZIndex} and {@link Container#paintOrder()}.
 * The drawing order is kept apart from the order of {@link Container#children()}, which layout, focus and Tab
 * follow.</p>
 *
 * <h2>What only some components have</h2>
 * <p>This interface holds what every component needs. The rest comes from smaller interfaces a component implements
 * when it fits: {@link Focusable} for keyboard focus and tab order, {@link HasCursor} and {@link HasTooltip}, and
 * {@link Container} for children, background and border.</p>
 *
 * <h2>Events</h2>
 * <p>Everything a component does reaches code as an event, registered with
 * {@link #onEvent(EventType, EventHandler)}: input (see {@link org.inspirenxe.strata.api.event.input.InputEvents}), a
 * control's own requests and reports, and drawing and sizing ({@link #PAINT}, {@link #PAINTED},
 * {@link #MEASURED}). For each component an input event reaches, the handlers run in priority order, then the
 * component's own default behavior (a button pressing, a text box inserting a character) unless a handler canceled
 * the event. A handler can read the state components had when the event fired; see {@link ComponentSnapshot}.</p>
 *
 * <h2>Binding</h2>
 * <p>A property can follow a {@link State} or a computed {@link Source} instead of being set by hand, with
 * {@link #bind}. The properties are named by {@link PropertyKey} constants beside the events of the attribute that
 * has them, such as {@link #ENABLED} here.</p>
 *
 * <h2>Threading</h2>
 * <p>Call components only from the engine's UI thread.</p>
 *
 * @param <T> the type callers see the component as, so setters can return it for chaining
 */
public interface Component<T extends Component<T>> extends EventTarget<T>, Enableable<T> {

    /**
     * Fired before the component draws itself. Canceling it skips the component's own drawing; its
     * children still draw. See {@link PaintEvent}.
     */
    EventType<PaintEvent.Pre> PAINT = EventType.direct("paint", PaintEvent.Pre.class);

    /**
     * Fired after the component and its children have drawn, so handlers draw on top. See {@link PaintEvent}.
     */
    EventType<PaintEvent> PAINTED = EventType.direct("painted", PaintEvent.class);

    /**
     * Fired after the component measured its content, so a handler can change the size it asks for. See
     * {@link MeasureEvent}.
     */
    EventType<MeasureEvent> MEASURED = EventType.direct("measured", MeasureEvent.class);

    /**
     * Fired when the component joins a tree a host is showing: when it is added to an attached container or
     * composite, when it is in a screen's tree as the screen opens or is rebuilt, and when it is in an overlay's
     * content as the overlay opens. Every component of the subtree that joins hears it, each parent before its
     * children, once the subtree's {@linkplain #bind bindings} have set their properties. A kept component that joins
     * again, such as a page a navigation view shows again, hears it again. See {@link LifecycleEvent}.
     */
    EventType<LifecycleEvent> ATTACHED = EventType.direct("attached", LifecycleEvent.class);

    /**
     * Fired when the component leaves a tree a host is showing: when it or an ancestor is removed from an attached
     * container or composite, as an expander does with its content when it collapses and a navigation view with the
     * page it stops showing, when its overlay closes, and when its screen closes or is rebuilt. Every component of the
     * subtree that leaves hears it, children before their parent, while it is still attached and before its bindings
     * stop. See {@link LifecycleEvent}.
     */
    EventType<LifecycleEvent> DETACHED = EventType.direct("detached", LifecycleEvent.class);

    /**
     * Whether the component is enabled, as {@link #isEnabled()} and {@link #setEnabled} say. Binds one way.
     */
    PropertyKey<Component<?>, Boolean> ENABLED = PropertyKey.of("component.enabled", Component::isEnabled,
            Component::setEnabled);

    /**
     * Whether the component is shown, as {@link #isVisible()} and {@link #setVisible} say. Binds one way.
     */
    PropertyKey<Component<?>, Boolean> VISIBLE = PropertyKey.of("component.visible", Component::isVisible,
            Component::setVisible);

    // Tree

    /**
     * Returns the component this component was added to: a {@link Container} such as a panel, or a composite control
     * that holds it as content or as one of its own parts, as an expander holds its content and a navigation view its
     * page. Code that needs something from its parent should ask for that capability, an attribute or
     * {@code Container}, with {@link #parent(Class)} or {@link #ancestor(Class)}, and not assume the parent's type.
     *
     * @return the parent, or empty for a screen's root component, an overlay's content or a component not added
     *     anywhere
     */
    Optional<Component<?>> parent();

    /**
     * Returns the parent if it is an instance of the given type. The type is usually a raw interface, such as
     * {@code Container.class} or an attribute like {@code Openable.class}, so the result's own type argument is not
     * checked.
     *
     * @param type the type the parent must have
     * @param <P> the type
     * @return the parent as that type, or empty when there is no parent or it is not an instance of {@code type}
     */
    <P> Optional<P> parent(Class<P> type);

    /**
     * Returns the nearest ancestor that is an instance of the given type, looking at the parent first, then its
     * parent, and so on up to the root. Composites count as ancestors, so a component on a navigation view's page
     * finds the navigation view. The type is usually a raw interface, such as {@code Container.class} or an attribute
     * like {@code Openable.class}, so the result's own type argument is not checked.
     *
     * @param type the type the ancestor must have
     * @param <A> the type
     * @return the nearest ancestor of that type, or empty when none has it
     */
    <A> Optional<A> ancestor(Class<A> type);

    /**
     * Returns whether the component is in a tree that a host is showing. Hover, focus and press state only change
     * while attached, and the theme is only applied once attached. {@link #ATTACHED} and {@link #DETACHED} say when
     * this changes.
     *
     * @return true while attached to a host
     */
    boolean isAttached();

    /**
     * Runs something for as long as the component is attached, every time it is attached. The supplier is called
     * when the component is attached, and at once if it is attached now, and the subscription it returns is removed
     * when the component is detached. A component that is attached again calls the supplier again, so
     *
     * <pre>{@code
     * chart.whileAttached(() -> prices.subscribe(chart::add));
     * }</pre>
     *
     * <p>listens to a feed whenever the chart shows and stops whenever it goes, however often that happens. Each call
     * adds one more thing to run, and nothing takes it away again; it costs nothing while the component is detached.
     * The supplier runs from an {@link #ATTACHED} handler and the subscription is removed from a {@link #DETACHED}
     * handler, both at the default priority.</p>
     *
     * @param start starts what to run and returns how to stop it; must not return null
     * @return this component
     * @throws NullPointerException if {@code start} returns null
     */
    default T whileAttached(final Supplier<? extends Subscription> start) {
        Objects.requireNonNull(start, "start");
        final Subscription[] running = new Subscription[1];
        if (this.isAttached()) {
            running[0] = Objects.requireNonNull(start.get(), "start returned null");
        }
        this.onEvent(DETACHED, event -> {
            final Subscription stop = running[0];
            running[0] = null;
            if (stop != null) {
                stop.unsubscribe();
            }
        });
        return this.onEvent(ATTACHED, event -> {
            if (running[0] == null) {
                running[0] = Objects.requireNonNull(start.get(), "start returned null");
            }
        });
    }

    // State

    /**
     * Returns whether the component is visible.
     *
     * @return true unless hidden with {@code setVisible(false)}
     */
    boolean isVisible();

    /**
     * Shows or hides the component. A hidden component is not drawn, cannot be hovered or focused, and takes no
     * space in layout. Hiding the focused component, or one of its ancestors, clears focus.
     *
     * @param visible false to hide
     * @return this component
     */
    T setVisible(boolean visible);

    /**
     * Returns whether the component is enabled.
     *
     * @return true unless disabled with {@code setEnabled(false)}
     */
    @Override
    boolean isEnabled();

    /**
     * Enables or disables the component. A disabled component is still drawn (built-in controls draw a disabled
     * look) and still takes space, but it gets no pointer or keyboard events and cannot be focused, and neither can
     * anything inside it. It still stops the pointer: a press or click on it, or on anything inside it, goes no
     * further, so the container behind it does not act either, and its tooltip shows while the pointer rests on it.
     * A scroll over it goes to its nearest enabled ancestor. Disabling the focused component, or one of its
     * ancestors, clears focus.
     *
     * @param enabled false to disable
     * @return this component
     */
    @Override
    T setEnabled(boolean enabled);

    /**
     * Returns whether the component and every container or composite it is inside are enabled, so that it can take
     * input. An overlay's content counts only the components inside the overlay.
     *
     * @return true unless this component or one of its ancestors is disabled
     */
    default boolean isEffectivelyEnabled() {
        for (Optional<Component<?>> component = Optional.of(this); component.isPresent();
             component = component.get().parent()) {
            if (!component.get().isEnabled()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns whether the component and every container or composite it is inside are visible. It says nothing of
     * whether the tree is shown at all; see {@link #isAttached()}.
     *
     * @return true unless this component or one of its ancestors is hidden
     */
    default boolean isEffectivelyVisible() {
        for (Optional<Component<?>> component = Optional.of(this); component.isPresent();
             component = component.get().parent()) {
            if (!component.get().isVisible()) {
                return false;
            }
        }
        return true;
    }

    // Layout

    /**
     * Returns the margin.
     *
     * @return the space kept clear around the component; {@link Insets#NONE} by default
     */
    Insets margin();

    /**
     * Sets the space kept clear around the component, outside its bounds. Parents add the margin to the
     * component's desired size when they make room for it.
     *
     * @param margin the margin
     * @return this component
     */
    T setMargin(Insets margin);

    /**
     * Sets the same margin on all four sides.
     *
     * @param all the margin on every side
     * @return this component
     */
    default T setMargin(final float all) {
        return this.setMargin(Insets.of(all));
    }

    /**
     * Sets one margin for the left and right sides and another for the top and bottom.
     *
     * @param horizontal the margin on the left and right
     * @param vertical the margin on the top and bottom
     * @return this component
     */
    default T setMargin(final float horizontal, final float vertical) {
        return this.setMargin(Insets.of(horizontal, vertical));
    }

    /**
     * Sets the margin on each side.
     *
     * @param left the margin on the left
     * @param top the margin on the top
     * @param right the margin on the right
     * @param bottom the margin on the bottom
     * @return this component
     */
    default T setMargin(final float left, final float top, final float right, final float bottom) {
        return this.setMargin(new Insets(left, top, right, bottom));
    }

    /**
     * Returns the minimum width.
     *
     * @return the minimum width; 0 by default
     */
    float minWidth();

    /**
     * Returns the maximum width.
     *
     * @return the maximum width; {@link Float#POSITIVE_INFINITY} by default
     */
    float maxWidth();

    /**
     * Returns the minimum height.
     *
     * @return the minimum height; 0 by default
     */
    float minHeight();

    /**
     * Returns the maximum height.
     *
     * @return the maximum height; {@link Float#POSITIVE_INFINITY} by default
     */
    float maxHeight();

    /**
     * Sets the smallest size the component may take. Applies to measured sizes, fixed sizes and stretching alike.
     * Where a minimum is larger than the maximum, the minimum wins.
     *
     * @param minWidth the minimum width; 0 or more and finite
     * @param minHeight the minimum height; 0 or more and finite
     * @return this component
     * @throws IllegalArgumentException if either value is negative, infinite or NaN
     */
    T setMinSize(float minWidth, float minHeight);

    /**
     * Sets the largest size the component may take. Applies to measured sizes, fixed sizes and stretching alike.
     * The maximum also limits the space offered to the component when it is measured, so text wraps within it.
     * Where the minimum is larger than a maximum, the minimum wins.
     *
     * @param maxWidth the maximum width; {@link Float#POSITIVE_INFINITY} for no limit
     * @param maxHeight the maximum height; {@link Float#POSITIVE_INFINITY} for no limit
     * @return this component
     * @throws IllegalArgumentException if either value is negative or NaN
     */
    T setMaxSize(float maxWidth, float maxHeight);

    /**
     * Fixes the width, replacing the measured width and overriding {@code STRETCH}. The minimum and maximum still
     * apply.
     *
     * @param width the width, 0 or more and finite, or {@link Float#NaN} to go back to the measured width
     * @return this component
     * @throws IllegalArgumentException if the width is negative or infinite
     */
    T setWidth(float width);

    /**
     * Fixes the height, replacing the measured height and overriding {@code STRETCH}. The minimum and maximum still
     * apply.
     *
     * @param height the height, 0 or more and finite, or {@link Float#NaN} to go back to the measured height
     * @return this component
     * @throws IllegalArgumentException if the height is negative or infinite
     */
    T setHeight(float height);

    /**
     * Fixes both the width and the height; see {@link #setWidth} and {@link #setHeight}.
     *
     * @param width the width, or {@link Float#NaN} for the measured width
     * @param height the height, or {@link Float#NaN} for the measured height
     * @return this component
     * @throws IllegalArgumentException if either value is negative or infinite
     */
    default T setSize(final float width, final float height) {
        this.setWidth(width);
        return this.setHeight(height);
    }

    /**
     * Returns how the component places itself horizontally in its slot.
     *
     * @return the horizontal alignment; {@link HorizontalAlignment#LEFT} by default
     */
    HorizontalAlignment horizontalAlignment();

    /**
     * Returns how the component places itself vertically in its slot.
     *
     * @return the vertical alignment; {@link VerticalAlignment#TOP} by default
     */
    VerticalAlignment verticalAlignment();

    /**
     * Sets how the component places itself in the slot its parent gives it.
     *
     * @param horizontal the horizontal alignment
     * @param vertical the vertical alignment
     * @return this component
     */
    T setAlignment(HorizontalAlignment horizontal, VerticalAlignment vertical);

    /**
     * Sets both alignments from an anchor, for example {@link Anchor#CENTER} or {@link Anchor#FILL}.
     *
     * @param anchor the anchor
     * @return this component
     */
    default T setAnchor(final Anchor anchor) {
        return this.setAlignment(anchor.horizontal(), anchor.vertical());
    }

    /**
     * Returns where the component sits, in the host's coordinates, as of the last layout. Before the first layout
     * this is {@link Rect#ZERO}. A hidden component has an empty rectangle.
     *
     * @return the bounds
     */
    Rect bounds();

    /**
     * Returns the width the component asked for in the last layout, margin excluded.
     *
     * @return the desired width
     */
    float desiredWidth();

    /**
     * Returns the height the component asked for in the last layout, margin excluded.
     *
     * @return the desired height
     */
    float desiredHeight();

    // Drawing order

    /**
     * Returns the component's z index among its siblings.
     *
     * @return the z index; 0 by default
     */
    int zIndex();

    /**
     * Sets where the component is drawn among its siblings. A parent draws its children by z index, lowest first,
     * and the pointer hits them in the opposite order, so a child with a higher z index is drawn over its siblings
     * and takes presses where they overlap. Children with the same z index are drawn in the order they were added,
     * unless {@link Container#raise} or {@link Container#lower} changed it. The z index orders siblings only: a
     * child is never drawn over its parent's siblings, whatever its z index.
     *
     * <p>Layout, focus and Tab order do not change: they follow the order of {@link Container#children()}. A change
     * redraws but lays nothing out again.</p>
     *
     * @param zIndex the z index; may be negative
     * @return this component
     */
    T setZIndex(int zIndex);

    // Binding

    /**
     * Keeps a property of this component in step with a source while the component is attached.
     *
     * <pre>{@code
     * music.bind(Toggle.SELECTED, model.music);               // two way: a State and a key with a report
     * save.bind(Component.ENABLED, model::canSave);            // one way: a computed source
     * }</pre>
     *
     * <p>When the component is attached, the binding reads the source and sets the property, and from then on sets
     * it again whenever a state the source read changes. Those updates happen on the UI thread before the next frame
     * is laid out, or when the host is flushed. Setting the property works as the control's setter does: it reports
     * the change and fires no request. A value equal to the property's current one is not set.</p>
     *
     * <p>When the source is a {@link State} and the key has {@linkplain PropertyKey#reports() reports}, the binding is
     * two way. A change the user makes still asks the control's request first, so a handler can refuse or rewrite it,
     * and the value the control accepted is written to the state when the control reports it. A report the binding
     * itself caused is not written back, and a value equal to the state's is not written, so a binding cannot loop. Nor
     * is a report that fires after attach before the binding first read its source, so the state wins over what the
     * control held, such as the first item a combo box selects when its items arrive. A value the control adjusts as
     * the binding sets it, such as a slider clamping to its range, stays adjusted in the control and is not written
     * back either.</p>
     *
     * <p>A binding lives as long as the component holds it. It follows its sources only while the component is
     * attached: it starts on attach and lets go of every state on detach, so a closed screen leaks nothing and there is
     * nothing to unsubscribe. A component holds one binding per {@linkplain PropertyKey#name() key name}; binding the
     * same key again replaces the earlier binding. On attach a component sets its bound properties in the order they
     * were first bound, so bind a list's items before its selection.</p>
     *
     * @param <V> the type of the property's value
     * @param key the property, such as {@link #ENABLED} or {@code Toggle.SELECTED}
     * @param source the source, a {@link State} or a lambda that computes from states
     * @return this component
     */
    <V> T bind(PropertyKey<? super T, V> key, Source<V> source);

    // Events
}
