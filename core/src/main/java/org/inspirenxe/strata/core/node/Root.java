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

import org.inspirenxe.strata.api.animation.Easing;
import org.inspirenxe.strata.api.animation.Interpolator;
import org.inspirenxe.strata.api.command.Command;
import org.inspirenxe.strata.api.command.Shortcuts;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.component.Focusable;
import org.inspirenxe.strata.api.component.Tooltip;
import org.inspirenxe.strata.api.control.attribute.popup.Closable;
import org.inspirenxe.strata.api.event.Event;
import org.inspirenxe.strata.api.event.EventHandler;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.Subscription;
import org.inspirenxe.strata.api.event.cause.CauseFrame;
import org.inspirenxe.strata.api.event.cause.CauseKeys;
import org.inspirenxe.strata.api.event.cause.Device;
import org.inspirenxe.strata.api.event.cause.Origin;
import org.inspirenxe.strata.api.event.input.InputEvents;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifier;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.input.ScrollKind;
import org.inspirenxe.strata.api.screen.Screen;
import org.inspirenxe.strata.api.overlay.Overlay;
import org.inspirenxe.strata.api.overlay.Overlays;
import org.inspirenxe.strata.api.state.Source;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.ThemeException;
import org.inspirenxe.strata.core.animation.AnimatorImpl;
import org.inspirenxe.strata.core.control.Commands;
import org.inspirenxe.strata.core.event.CharEventImpl;
import org.inspirenxe.strata.core.event.ControlEventImpl;
import org.inspirenxe.strata.core.event.DragEventImpl;
import org.inspirenxe.strata.core.event.EventDispatcher;
import org.inspirenxe.strata.core.event.FocusEventImpl;
import org.inspirenxe.strata.core.event.KeyEventImpl;
import org.inspirenxe.strata.core.event.ModifiersEventImpl;
import org.inspirenxe.strata.core.event.PointerButtonEventImpl;
import org.inspirenxe.strata.core.event.PointerEventImpl;
import org.inspirenxe.strata.core.event.ScrollEventImpl;
import org.inspirenxe.strata.core.frame.FrameRequests;
import org.inspirenxe.strata.core.frame.UiThreadImpl;
import org.inspirenxe.strata.core.input.ClickCounter;
import org.inspirenxe.strata.core.input.KeyRepeater;
import org.inspirenxe.strata.core.paint.CanvasPainter;
import org.inspirenxe.strata.core.text.InlineMetrics;
import org.inspirenxe.strata.core.state.Binding;
import org.inspirenxe.strata.core.state.BindingQueue;
import org.inspirenxe.strata.core.state.Watch;
import org.inspirenxe.strata.spi.Canvas;
import org.inspirenxe.strata.spi.Platform;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Owns one screen's node tree and its overlays: layout, painting, tooltips, and the hover, press, drag, click-count
 * and focus state machines. A {@code ScreenHostImpl} drives it. The input methods return true when Strata used the
 * input, so the host can pass unused input back to the engine.
 *
 * <p>Layers: the content is at the bottom, then each open overlay in the order it opened, then the tooltip. Each
 * draws after a {@link Canvas#nextLayer()}. A dialog draws the {@link Overlays#STYLE_BACKDROP} paint over everything
 * below it first. The pointer hits the topmost layer that has something under it; a dialog blocks every layer below
 * it, and the tooltip is never hit. {@link Overlay#raise()} moves a popup or toast up past the popups and toasts
 * above it, stopping below the first dialog, so a dialog stays above every overlay opened before it.</p>
 *
 * <p>Pointer rules: hover goes to the deepest visible node under the pointer. A disabled node, or one inside a
 * disabled node, can be that node: it is then the hovered node for its tooltip, but it is not marked hovered, hears
 * no pointer enter, exit or move, and shows the cursor of its nearest enabled ancestor. A press first closes every
 * popup above the layer it lands on, except those opened without light dismiss, and then goes nowhere if any
 * closed. A press on a disabled node goes no further either: it fires nothing, runs no hook, leaves focus alone and
 * uses up its release, so nothing behind the node acts. Otherwise the press focuses the pressed node or its nearest
 * focusable ancestor, where an ancestor whose {@linkplain Node#focusDelegate() focus delegate} can take focus counts
 * as that delegate, and clears focus if there is none, then tells the pressed node and each of its ancestors
 * through {@link NodeHooks#pressedWithin}, innermost first, and only then fires the press. Each button is tracked on
 * its own. A press that closed popups or hit a dialog's backdrop uses up its release too. A press of a button that
 * is still held, because the engine lost its release, first ends the old press with a release and no click. A drag
 * starts once the pointer moves more than {@link #DRAG_THRESHOLD} units from the press on either axis, and its
 * events go to the pressed node wherever the pointer is, carrying the press's button and click count in their cause.
 * A release fires a click only when it ends no drag and lands inside the pressed node, which must still be visible
 * and enabled, as must every ancestor. A scroll goes to the hovered node, or over a disabled node to its nearest
 * enabled ancestor. After a scroll that a node handled, hover is worked out again, since content may have moved
 * under the pointer; the first frame after the tree changes does the same while the pointer is inside the host, so
 * a node disabled or enabled under a still pointer loses or gains its hover then.</p>
 *
 * <p>Keyboard rules: key events go to the focused node, or else to the top dialog, or else to the content. An
 * unhandled press or repeat runs the first enabled command whose shortcut matches it, as {@link Shortcuts}
 * describes. An unhandled Escape press closes the top overlay that is not a toast. An unhandled Tab press or repeat without Ctrl
 * or Alt moves focus, backwards with Shift, among the nodes of the top dialog or, with no dialog, of the content.
 * Typed characters go only to the focused node. When the platform does not send key repeats, the root makes them
 * with a {@link KeyRepeater}; each repeat goes to whatever has focus when it fires.</p>
 *
 * <p>A popup opened next to an anchor takes its desired size below or above the anchor. A popup opened in an area
 * is measured with the area's size and arranged into it, where its own alignment places it, as a dialog is in the
 * whole host; the area is cut to the host, and core can move it while the popup is open.</p>
 *
 * <p>Toasts are overlays placed down the top-right corner. Presses elsewhere and Escape leave them open; each closes
 * on a click that nothing in it consumed, or on the first frame rendered after its time runs out. With the theme's
 * {@value Overlays#STYLE_TOAST_FADE} metric above 0, a toast's content fades in from an opacity of 0 as it opens, and
 * on the first frame rendered once less than that time is left it starts fading out, closing when the fade ends.</p>
 *
 * <p>Tooltips: the hovered node's tooltip, or its nearest ancestor's, shows whether or not they are enabled, so a
 * disabled control can say why it is disabled. Its corner is offset from the pointer's whole-unit position by the
 * theme's {@value Tooltip#STYLE_OFFSET_X} and {@value Tooltip#STYLE_OFFSET_Y}, {@link #TOOLTIP_OFFSET} units right of
 * and above it by default, flipped left of it when it would leave the host on the right, kept inside the host. It
 * shows on the first frame rendered once the pointer has stayed on its owner for the
 * {@linkplain #tooltipDelay tooltip delay}, or at once when the pointer moves to it from another tooltip's owner. It
 * follows the pointer and hides while a drag is in progress.</p>
 *
 * <p>Every method must be called on the UI thread. Opening and closing overlays and registering their handlers check
 * it through {@link UiThreadImpl#checkCurrent()}, as nodes do for their changes while attached.</p>
 */
public final class Root {
    /**
     * How far, in UI units along either axis, the pointer must move with a button held before a drag starts.
     */
    public static final float DRAG_THRESHOLD = 4f;
    /**
     * How far a tooltip sits from the pointer on each axis, in UI units.
     */
    public static final float TOOLTIP_OFFSET = 12f;

    private final Platform platform;
    private final ComponentFactoryImpl ui;
    private final LayoutPass layoutPass;
    private final CanvasPainter painter;
    private final ClickCounter clickCounter;
    private final KeyRepeater keyRepeater;
    private final Map<MouseButton, Press> presses;
    // Buttons whose press closed popups or hit a dialog's backdrop, so their release is swallowed too
    private final Set<MouseButton> swallowedReleases;
    private final List<Layer> layers;
    // The layers being painted, copied each frame so a paint handler can close an overlay
    private final List<Layer> paintLayers;
    private final List<Node> arrangeRequests;
    private final OverlaysImpl overlays;
    private final BindingQueue bindingQueue;
    // Bindings of nodes attached since attachTree last evaluated them
    private final List<Binding<?>> attachedBindings;
    // Watches made through the factory, stopped when the screen closes or rebuilds
    private final List<Watch<?>> watches;
    private final Origin.User user;
    private final UiThreadImpl uiThread;
    private final FrameRequests frames;
    private final AnimatorImpl animator;
    private final ShortcutsImpl shortcuts;
    // The visible command sources of the top layer that has keys, worked out when a shortcut is first looked for
    private List<Node> commandSources;
    private Node commandSourceScope;
    private final List<Command> commandScratch;
    private Screen screen;

    private Theme theme;
    private Node content;
    private float width;
    private float height;
    private float pixelsPerUnit = 1f;
    // The whole host, kept so neither layout nor painting allocates it
    private Rect area = Rect.ZERO;
    private boolean layoutRequested;

    private Node hovered;
    // Whether the hovered node and its ancestors are visible and enabled, so it was marked hovered and told it entered
    private boolean hoverLive;
    // Set when the tree changed, so the next frame works hover out again at the pointer's last position
    private boolean hoverStale;
    private boolean pointerInside;
    private Node focused;
    // The node a focus change in progress assigned but has not told yet; it never had focus to lose
    private Node focusArriving;
    private List<Node> tabOrder;
    private float pointerX;
    private float pointerY;
    private Modifiers modifiers;

    private Node tooltipOwner;
    private Tooltip tooltipShown;
    private Node tooltipNode;
    private long tooltipDelay;
    // The owner whose tooltip waits for the delay, and when the pointer reached it
    private Node tooltipPending;
    private long tooltipPendingSince;
    private boolean focusFromPress;

    /**
     * Creates an empty root of size 0 by 0, and binds it to its factory.
     *
     * @param platform the host's platform, for text metrics, time, text input and capabilities
     * @param theme the starting theme
     * @param ui the host's factory
     */
    public Root(final Platform platform, final Theme theme, final ComponentFactoryImpl ui) {
        this.platform = Objects.requireNonNull(platform, "platform");
        this.theme = Objects.requireNonNull(theme, "theme");
        this.ui = Objects.requireNonNull(ui, "ui");
        final InlineMetrics metrics = InlineMetrics.of(platform.textMetrics());
        this.layoutPass = new LayoutPass(ui, metrics);
        this.painter = new CanvasPainter(metrics);
        this.clickCounter = new ClickCounter();
        this.keyRepeater = new KeyRepeater();
        this.presses = new EnumMap<>(MouseButton.class);
        this.swallowedReleases = EnumSet.noneOf(MouseButton.class);
        this.layers = new ArrayList<>();
        this.paintLayers = new ArrayList<>();
        this.arrangeRequests = new ArrayList<>();
        this.overlays = new OverlaysImpl();
        this.user = new Origin.User(platform.user().orElse(null));
        this.uiThread = ui.uiThread();
        this.frames = new FrameRequests();
        this.uiThread.register(this.frames);
        this.bindingQueue = new BindingQueue(this.frames::request);
        this.animator = new AnimatorImpl(platform::clock, ui::causes, this.frames, this.uiThread);
        this.attachedBindings = new ArrayList<>();
        this.watches = new ArrayList<>();
        this.shortcuts = new ShortcutsImpl(ui);
        this.commandScratch = new ArrayList<>();
        this.modifiers = Modifiers.NONE;
        this.layoutRequested = true;
        this.tooltipDelay = -1L;
        ui.bindRoot(this);
    }

    /**
     * Returns the UI thread of the host's platform.
     *
     * @return the UI thread
     */
    public UiThreadImpl uiThread() {
        return this.uiThread;
    }

    /**
     * Returns the host's animator.
     *
     * @return the animator
     */
    public AnimatorImpl animator() {
        return this.animator;
    }

    /**
     * Returns whether the host needs a frame and the deadlines of its timed changes.
     *
     * @return the frame requests
     */
    public FrameRequests frames() {
        return this.frames;
    }

    /**
     * Throws unless the calling thread is the UI thread, as {@link UiThreadImpl#checkCurrent()} decides. Nodes call
     * this before they change while attached.
     *
     * @throws IllegalStateException off the UI thread
     */
    void checkThread() {
        this.uiThread.checkCurrent();
    }

    /**
     * Notes that the look changed without a new layout, so the host needs a frame.
     */
    void repaint() {
        this.frames.request();
    }

    /**
     * Registers a timed change, so {@link #nextFrameNanos()} reports a frame due then.
     *
     * @param nanos the platform clock's {@code nanoTime()} at which the look changes
     */
    void repaintAt(final long nanos) {
        this.frames.at(nanos);
    }

    /**
     * Returns the platform clock's reading.
     *
     * @return the clock's {@code nanoTime()}
     */
    long nanoTime() {
        return this.platform.clock().nanoTime();
    }

    /**
     * Returns when the next timed change is due: the earliest of the deadlines components registered, each toast's
     * expiry, the end of the delay of a tooltip waiting to show, and the next key repeat the root makes itself.
     * Allocates nothing.
     *
     * @return the platform clock's {@code nanoTime()} at which a frame is due, or {@link Long#MAX_VALUE} for none
     */
    public long nextFrameNanos() {
        long next = this.frames.earliest();
        for (int i = 0; i < this.layers.size(); i++) {
            final Layer layer = this.layers.get(i);
            if (layer.isToast() && !layer.fadingOut) {
                next = Math.min(next, layer.fadeOutAt() * 1_000_000L);
            }
        }
        if (this.tooltipPending != null) {
            next = Math.min(next, (this.tooltipPendingSince + this.tooltipDelay()) * 1_000_000L);
        }
        if (!this.platform.capabilities().sendsKeyRepeat()) {
            final long repeat = this.keyRepeater.nextFire();
            if (repeat != Long.MAX_VALUE) {
                next = Math.min(next, repeat * 1_000_000L);
            }
        }
        return next;
    }

    /**
     * Returns the theme every node in the tree uses.
     *
     * @return the theme
     */
    public Theme theme() {
        return this.theme;
    }

    /**
     * Returns the top node of the tree.
     *
     * @return the content, or {@code null} when the root is empty
     */
    public Node content() {
        return this.content;
    }

    /**
     * Returns the overlays of this root.
     *
     * @return the overlays
     */
    public Overlays overlays() {
        return this.overlays;
    }

    /**
     * Returns the modifiers held as of the latest input.
     *
     * @return the held modifiers
     */
    public Modifiers modifiers() {
        return this.modifiers;
    }

    /**
     * Returns how long the pointer rests on a component before its tooltip shows: the delay set with
     * {@link #setTooltipDelay}, else the theme's {@value Tooltip#STYLE_DELAY}, else
     * {@link Tooltip#DEFAULT_DELAY_MILLIS}.
     *
     * @return the delay in milliseconds
     */
    public long tooltipDelay() {
        if (this.tooltipDelay >= 0L) {
            return this.tooltipDelay;
        }
        final Theme theme = this.theme();
        return theme == null ? Tooltip.DEFAULT_DELAY_MILLIS
                : Math.max(0L, (long) theme.metricOr(Tooltip.STYLE_DELAY, Tooltip.DEFAULT_DELAY_MILLIS));
    }

    /**
     * Sets how long the pointer rests on a component before its tooltip shows, in place of the theme's. A tooltip
     * waiting to show uses the new delay.
     *
     * @param millis the delay in milliseconds; 0 shows tooltips at once
     * @throws IllegalArgumentException if {@code millis} is negative
     */
    public void setTooltipDelay(final long millis) {
        if (millis < 0L) {
            throw new IllegalArgumentException("Tooltip delay must not be negative: " + millis);
        }
        this.tooltipDelay = millis;
    }

    /**
     * Returns whether buttons act as the pointer's left button goes down on them rather than on the click after it,
     * from the platform's {@link org.inspirenxe.strata.spi.Conventions}.
     *
     * @return true when buttons act on press
     */
    public boolean activatesOnPress() {
        return this.platform.conventions().activateOnPress();
    }

    /**
     * Returns whether the focus change being reported comes from a pointer press. It is true only while focus
     * events fire for a press, which happens just before the press itself fires, so a focus handler can tell a
     * click from keyboard or code focus.
     *
     * @return true while a press moves focus
     */
    public boolean isFocusFromPress() {
        return this.focusFromPress;
    }

    // ------------------------------------------------------------------
    // Tree
    // ------------------------------------------------------------------

    /**
     * Replaces the tree, closing every overlay and the tooltip first. The theme is checked against every node of the
     * new tree before anything changes, so a theme that lacks a required key fails first. The old tree is detached,
     * which fires its {@link Component#DETACHED} events and clears its hover, press and focus state, the new one is
     * attached, which fires its {@link Component#ATTACHED} events, and then every component in the old tree that the
     * new tree did not take hears {@link BuiltInComponent#onReleased()}. A handler that throws on the way does not
     * stop any of this; its exception is thrown at the end.
     *
     * @param content the new top node, or {@code null} to empty the root
     * @throws IllegalArgumentException if {@code content} is already in a container or another root
     * @throws ThemeException if the theme lacks a style key the new tree requires
     */
    public void setContent(final Node content) {
        if (content != null) {
            if (content.parentNode() != null || content.root() != null) {
                throw new IllegalArgumentException(content.facade() + " is already part of another tree");
            }
            this.validateTheme(this.theme, content);
        }
        this.overlays.closeAll();
        this.closeTooltip();
        this.tooltipPending = null;
        final Node previous = this.content;
        // A DETACHED or ATTACHED handler that throws still leaves the new tree in place and the old one released
        try {
            if (previous != null) {
                this.detachTree(previous);
            }
        } finally {
            this.content = content;
            try {
                if (content != null) {
                    this.attachTree(content);
                }
            } finally {
                if (previous != null) {
                    release(previous);
                }
                this.requestLayout();
            }
        }
    }

    /**
     * Changes the theme and applies it to every node, overlays and tooltip included. Nothing changes if the new
     * theme lacks a key the tree requires.
     *
     * @param theme the new theme
     * @throws ThemeException if the theme lacks a style key the tree or an overlay requires
     */
    public void setTheme(final Theme theme) {
        this.checkTheme(theme);
        this.theme = theme;
        for (final Node top : this.tops()) {
            visit(top, node -> node.applyTheme(theme));
        }
        this.requestLayout();
    }

    /**
     * Checks a theme against every node this root holds, overlays and tooltip included, without changing anything,
     * so a caller that changes several roots can check them all first.
     *
     * @param theme the theme
     * @throws ThemeException if the theme lacks a style key the tree or an overlay requires
     */
    public void checkTheme(final Theme theme) {
        Objects.requireNonNull(theme, "theme");
        for (final Node top : this.tops()) {
            this.validateTheme(theme, top);
        }
    }

    /**
     * Checks this root's theme against a subtree about to be attached, so a container can refuse a child before it
     * changes anything.
     *
     * @throws ThemeException if the theme lacks a style key the subtree requires
     */
    void checkTheme(final Node subtree) {
        this.validateTheme(this.theme, subtree);
    }

    /**
     * Marks every node for measuring, so text is resolved again, as after the language changes.
     */
    public void invalidateText() {
        for (final Node top : this.tops()) {
            visit(top, Node::invalidateLayout);
        }
    }

    /**
     * The top node of every tree this root holds: the content, each overlay and the tooltip.
     */
    private List<Node> tops() {
        final List<Node> tops = new ArrayList<>();
        if (this.content != null) {
            tops.add(this.content);
        }
        for (final Layer layer : this.layers) {
            tops.add(layer.node);
        }
        if (this.tooltipNode != null) {
            tops.add(this.tooltipNode);
        }
        return tops;
    }

    /**
     * Attaches a subtree: checks the theme, attaches each node, sets the properties of the bindings that joined, and
     * then runs each node's attach hook and {@link Component#ATTACHED} handlers, parents first. A hook or handler that
     * throws does not stop the rest of the subtree from hearing it; the first failure is thrown once every node has.
     */
    void attachTree(final Node subtree) {
        this.validateTheme(this.theme, subtree);
        visit(subtree, node -> node.attach(this));
        this.treeChanged();
        this.evaluateAttachedBindings();
        final RuntimeException[] failure = new RuntimeException[1];
        visit(subtree, node -> {
            if (node.root() == this) {
                collect(failure, node::fireAttached);
            }
        });
        if (failure[0] != null) {
            throw failure[0];
        }
    }

    /**
     * Runs an action, keeping the first exception it throws in {@code failure} and adding later ones to it as
     * suppressed, so a walk over a tree goes on past a failing node.
     */
    private static void collect(final RuntimeException[] failure, final Runnable action) {
        try {
            action.run();
        } catch (final RuntimeException e) {
            if (failure[0] == null) {
                failure[0] = e;
            } else {
                failure[0].addSuppressed(e);
            }
        }
    }

    /**
     * Returns the queue of this root's dirty bindings.
     *
     * @return the queue
     */
    BindingQueue bindingQueue() {
        return this.bindingQueue;
    }

    /**
     * Notes a binding whose node was just attached, so {@link #attachTree} sets its property once the whole subtree
     * is attached.
     */
    void bindingAttached(final Binding<?> binding) {
        this.attachedBindings.add(binding);
    }

    /**
     * Sets the properties of the bindings attached since the last call, so a component shows its bound values as
     * soon as it joins the tree. Other dirty bindings wait for {@link #flushBindings()}. A binding that attaches
     * more nodes as it sets its property has their bindings evaluated by the nested {@code attachTree}.
     */
    private void evaluateAttachedBindings() {
        if (this.attachedBindings.isEmpty()) {
            return;
        }
        final List<Binding<?>> attached = List.copyOf(this.attachedBindings);
        this.attachedBindings.clear();
        for (final Binding<?> binding : attached) {
            binding.evaluate();
        }
    }

    /**
     * Evaluates every dirty binding until none is left, as {@link BindingQueue#flush()} describes, so the next
     * layout sees the bound values. The host calls this at the start of each layout and render.
     *
     * @return false if a binding cycle was reported and updates were dropped, true otherwise
     */
    public boolean flushBindings() {
        return this.bindingQueue.flush();
    }

    /**
     * Starts a watch over a source on this root's binding queue, as {@code ComponentFactory.watch} describes. The
     * source is read once now. If that throws, the watch is dropped and the exception goes on to the caller.
     *
     * @param <T> the type of the value
     * @param source the source
     * @param listener called with each new value
     * @return a subscription that stops the watch and forgets it
     */
    public <T> Subscription watch(final Source<T> source, final Consumer<? super T> listener) {
        this.checkThread();
        final Watch<T> watch = new Watch<>(source, listener);
        this.watches.add(watch);
        boolean started = false;
        try {
            watch.start(this.bindingQueue);
            started = true;
        } finally {
            if (!started) {
                watch.stop();
                this.watches.remove(watch);
            }
        }
        return () -> {
            watch.stop();
            this.watches.remove(watch);
        };
    }

    /**
     * Hands over the watches started so far and forgets them, so the host can stop them once a rebuilt screen has
     * started its own.
     *
     * @return the watches, still running
     */
    public List<Watch<?>> takeWatches() {
        final List<Watch<?>> taken = List.copyOf(this.watches);
        this.watches.clear();
        return taken;
    }

    /**
     * Stops every watch and forgets them, when the screen closes.
     */
    public void stopWatches() {
        for (final Watch<?> watch : this.takeWatches()) {
            watch.stop();
        }
    }

    /**
     * Detaches a subtree: first runs each node's {@link Component#DETACHED} handlers and detach hook, children first,
     * while the subtree is still attached, then clears the hover, focus, tooltip and press state that points into it
     * and detaches each node. A handler, hook or focus-lost handler that throws does not stop the rest of the subtree
     * from being detached; the first such failure is thrown once every node is detached.
     */
    void detachTree(final Node subtree) {
        final RuntimeException[] failure = new RuntimeException[1];
        visitChildrenFirst(subtree, node -> collect(failure, node::fireDetached));
        visit(subtree, node -> {
            if (node == this.hovered) {
                this.hovered = null;
                this.hoverLive = false;
            }
            if (node == this.focused) {
                collect(failure, () -> this.changeFocus(null));
            }
            if (node == this.tooltipOwner) {
                this.tooltipOwner = null;
            }
            if (node == this.tooltipPending) {
                this.tooltipPending = null;
            }
            this.presses.values().removeIf(press -> press.target == node);
            node.detach();
        });
        if (this.tooltipOwner == null && this.tooltipNode != null && this.tooltipNode != subtree) {
            this.closeTooltip();
        }
        this.treeChanged();
        if (failure[0] != null) {
            throw failure[0];
        }
    }

    void treeChanged() {
        this.frames.request();
        this.tabOrder = null;
        this.commandSources = null;
        // What is under the pointer may have changed though the pointer did not move
        this.hoverStale = true;
        if (this.focused != null && !this.canFocus(this.focused)) {
            this.changeFocus(null);
        }
    }

    private void validateTheme(final Theme theme, final Node subtree) {
        final Set<String> required = new LinkedHashSet<>();
        visit(subtree, node -> required.addAll(node.requiredStyleKeys()));
        final Set<String> missing = theme.missing(required);
        if (!missing.isEmpty()) {
            throw new ThemeException(theme, missing);
        }
    }

    /**
     * Tells every component in a tree this root let go of that it was released, skipping any that is attached
     * again, so a component holding secret text forgets it.
     */
    private static void release(final Node subtree) {
        visit(subtree, node -> {
            if (node.root() == null) {
                node.released();
            }
        });
    }

    private static void visit(final Node node, final Consumer<Node> action) {
        action.accept(node);
        if (node instanceof ContainerNode container) {
            for (final Node child : List.copyOf(container.childNodes())) {
                visit(child, action);
            }
        }
    }

    private static void visitChildrenFirst(final Node node, final Consumer<Node> action) {
        if (node instanceof ContainerNode container) {
            for (final Node child : List.copyOf(container.childNodes())) {
                visitChildrenFirst(child, action);
            }
        }
        action.accept(node);
    }

    /**
     * Returns the top node of the tree a node is in.
     */
    private static Node topOf(final Node node) {
        Node top = node;
        while (top.parentNode() != null) {
            top = top.parentNode();
        }
        return top;
    }

    /**
     * Returns which layer a node is in: -1 for the content, the overlay's index, or {@link Integer#MIN_VALUE} for
     * the tooltip and for nodes in no layer.
     */
    private int layerOf(final Node node) {
        final Node top = topOf(node);
        if (top == this.content) {
            return -1;
        }
        for (int i = 0; i < this.layers.size(); i++) {
            if (this.layers.get(i).node == top) {
                return i;
            }
        }
        return Integer.MIN_VALUE;
    }

    private Layer topDialog() {
        for (int i = this.layers.size() - 1; i >= 0; i--) {
            if (this.layers.get(i).modal) {
                return this.layers.get(i);
            }
        }
        return null;
    }

    private int topDialogIndex() {
        for (int i = this.layers.size() - 1; i >= 0; i--) {
            if (this.layers.get(i).modal) {
                return i;
            }
        }
        return -1;
    }

    // ------------------------------------------------------------------
    // Layout and rendering
    // ------------------------------------------------------------------

    /**
     * Sets the size of the area the tree fills. A change requests a new layout.
     *
     * @param width the width in UI units
     * @param height the height in UI units
     */
    public void resize(final float width, final float height) {
        if (this.width != width || this.height != height) {
            this.width = width;
            this.height = height;
            this.area = new Rect(0, 0, width, height);
            this.requestLayout();
        }
    }

    /**
     * Returns how many device pixels one UI unit covers.
     *
     * @return the density; 1 until set
     */
    public float pixelsPerUnit() {
        return this.pixelsPerUnit;
    }

    /**
     * Sets how many device pixels one UI unit covers. A change marks every node for measuring, as a change of
     * language does, and the painter draws with the new density from the next frame.
     *
     * @param pixelsPerUnit device pixels per UI unit
     * @throws IllegalArgumentException if the value is not finite and greater than 0
     */
    public void setPixelsPerUnit(final float pixelsPerUnit) {
        Root.checkPixelsPerUnit(pixelsPerUnit);
        if (pixelsPerUnit != this.pixelsPerUnit) {
            this.pixelsPerUnit = pixelsPerUnit;
            this.invalidateText();
            this.requestLayout();
        }
    }

    /**
     * Checks a density.
     *
     * @param pixelsPerUnit device pixels per UI unit
     * @throws IllegalArgumentException if the value is not finite and greater than 0
     */
    public static void checkPixelsPerUnit(final float pixelsPerUnit) {
        if (!(pixelsPerUnit > 0f) || Float.isInfinite(pixelsPerUnit)) {
            throw new IllegalArgumentException("Pixels per unit must be finite and greater than 0: " + pixelsPerUnit);
        }
    }

    /**
     * Measures the content in a given space without arranging it, for a host asked for its preferred size. Overlays
     * and the tooltip are left out. The content keeps the desired size from this measure until the next layout,
     * which this requests, measures it again at the root's own size.
     *
     * @param availableWidth the width offered; may be infinite
     * @param availableHeight the height offered; may be infinite
     * @return the content's outer size, margin included, or {@link Size#ZERO} with no content
     * @throws IllegalArgumentException if either value is negative or NaN
     */
    public Size preferredSize(final float availableWidth, final float availableHeight) {
        if (!(availableWidth >= 0f) || !(availableHeight >= 0f)) {
            throw new IllegalArgumentException("The space offered must not be negative or NaN: " + availableWidth
                    + " by " + availableHeight);
        }
        if (this.content == null) {
            return Size.ZERO;
        }
        this.requestLayout();
        this.content.measure(this.layoutPass, availableWidth, availableHeight);
        return new Size(this.content.outerWidth(), this.content.outerHeight());
    }

    void requestLayout() {
        this.layoutRequested = true;
        this.frames.request();
    }

    void requestArrangement(final Node node) {
        this.arrangeRequests.add(node);
        this.layoutRequested = true;
        this.frames.request();
    }

    /**
     * Lays out every layer if anything requested it since the last layout: the content and dialogs are measured
     * with the root's size and arranged into it, popups are placed by their anchor, and the tooltip by the pointer.
     * Subtrees that did not change are skipped. Then nodes that asked only to be arranged again are arranged in their
     * last slot. Rendering and pointer input call this first, so bounds are never stale. If a measure or arrange hook
     * throws, the layout stays requested, so the next call tries again.
     */
    public void layout() {
        if (!this.layoutRequested) {
            return;
        }
        this.layoutRequested = false;
        boolean done = false;
        try {
            this.layoutLayers();
            done = true;
        } finally {
            if (!done) {
                this.layoutRequested = true;
            }
        }
    }

    private void layoutLayers() {
        final Rect area = this.area;
        if (this.content != null) {
            this.content.measure(this.layoutPass, this.width, this.height);
            this.content.arrange(this.layoutPass, area);
        }
        final Theme toastTheme = this.theme();
        final float toastMargin = toastTheme == null ? Overlays.TOAST_MARGIN
                : Math.max(0f, toastTheme.metricOr(Overlays.STYLE_TOAST_MARGIN, Overlays.TOAST_MARGIN));
        float toastY = toastMargin;
        for (final Layer layer : List.copyOf(this.layers)) {
            if (layer.isToast()) {
                toastY = layer.layoutToast(area, toastY, toastMargin);
            } else {
                layer.layout(area);
            }
        }
        if (this.tooltipNode != null) {
            this.layoutTooltip();
        }
        while (!this.arrangeRequests.isEmpty()) {
            final List<Node> requests = List.copyOf(this.arrangeRequests);
            this.arrangeRequests.clear();
            for (final Node node : requests) {
                if (node.root() == this) {
                    node.rearrange(this.layoutPass);
                }
            }
        }
    }

    private void layoutTooltip() {
        final Node node = this.tooltipNode;
        node.measure(this.layoutPass, this.width, this.height);
        final float tooltipWidth = node.outerWidth();
        final float tooltipHeight = node.outerHeight();
        final Theme theme = this.theme();
        final float offsetX = theme == null ? TOOLTIP_OFFSET : theme.metricOr(Tooltip.STYLE_OFFSET_X, TOOLTIP_OFFSET);
        final float offsetY = theme == null ? -TOOLTIP_OFFSET : theme.metricOr(Tooltip.STYLE_OFFSET_Y, -TOOLTIP_OFFSET);
        // From the whole unit the pointer is in, so the box does not shift with sub-unit motion
        final float pointerX = (float) Math.floor(this.pointerX);
        final float pointerY = (float) Math.floor(this.pointerY);
        float x = pointerX + offsetX;
        if (x + tooltipWidth > this.width) {
            x = pointerX - offsetX - tooltipWidth;
        }
        float y = pointerY + offsetY;
        final Node owner = this.tooltipOwner;
        if (theme != null && owner != null && theme.metricOr(Tooltip.STYLE_BELOW, 0f) != 0f) {
            y = belowOwner(owner.bounds(), pointerY, tooltipHeight, this.height,
                    theme.metricOr(Tooltip.STYLE_BELOW_OVERLAP, 3f), theme.metricOr(Tooltip.STYLE_BELOW_GAP, 5f));
        }
        x = Math.max(0f, Math.min(x, this.width - tooltipWidth));
        y = Math.max(0f, Math.min(y, this.height - tooltipHeight));
        node.arrange(this.layoutPass, new Rect(x, y, tooltipWidth, tooltipHeight));
    }

    /**
     * Returns the top of a tooltip placed below its owner, or above it when there is no room below, as
     * {@link Tooltip#STYLE_BELOW} describes.
     */
    private static float belowOwner(final Rect owner, final float pointerY, final float boxHeight,
                                    final float hostHeight, final float overlap, final float gap) {
        final float height = owner.height();
        if (!(height > 0f)) {
            return owner.bottom() + gap;
        }
        if (owner.bottom() + height + boxHeight <= hostHeight - gap) {
            return pointerY + ease(Math.abs(pointerY + overlap - owner.y()), height, overlap, gap);
        }
        return pointerY - boxHeight - ease(Math.abs(pointerY + overlap - owner.bottom()), height, overlap, gap);
    }

    /**
     * Eases from {@code height - overlap} at a distance of 0 to {@code gap} at a distance of {@code height} or more,
     * rounded to a whole unit.
     */
    private static float ease(final float distance, final float height, final float overlap, final float gap) {
        final float t = Math.min(distance, height) / height;
        return Math.round((height - overlap) + t * (gap - (height - overlap)));
    }

    /**
     * Draws one frame: drops the deadlines it reaches, fires a key repeat if one is due, lays out if needed, works
     * hover out again if the tree changed since the pointer last moved and the pointer is inside the host, paints the
     * content, each overlay and the tooltip, and asks the canvas for the cursor of the hovered node (or its nearest
     * ancestor that sets one, falling back to the arrow). An overlay that a paint handler closes is not painted after
     * it closes.
     *
     * <p>Just before painting, the root forgets the changes that asked for this frame, since the frame shows them,
     * and asks again at once if a binding or a task is still waiting or a layout is still requested. Changes during
     * painting ask for the next frame.</p>
     *
     * @param canvas the frame's canvas
     */
    public void render(final Canvas canvas) {
        this.frames.prune(this.platform.clock().nanoTime());
        this.pumpKeyRepeat();
        this.expireToasts();
        this.showPendingTooltip();
        this.layout();
        if (this.hoverStale && this.pointerInside) {
            this.updateHover(this.pointerX, this.pointerY, this.modifiers);
            this.updateTooltip();
            this.layout();
        }
        this.hoverStale = false;
        this.frames.clear();
        if (this.layoutRequested || !this.bindingQueue.isEmpty() || this.uiThread.hasQueued()) {
            this.frames.request();
        }
        final CanvasPainter frame = this.painter.begin(canvas, this.platform.capabilities(), this.pixelsPerUnit);
        if (this.content != null) {
            this.content.paint(frame);
        }
        if (!this.layers.isEmpty()) {
            final Rect area = this.area;
            final Paint backdrop = this.theme.paintOr(Overlays.STYLE_BACKDROP, Paint.NONE);
            final List<Layer> painting = this.paintLayers;
            for (int i = 0; i < this.layers.size(); i++) {
                painting.add(this.layers.get(i));
            }
            try {
                for (int i = 0; i < painting.size(); i++) {
                    final Layer layer = painting.get(i);
                    if (!layer.open) {
                        continue;
                    }
                    canvas.nextLayer();
                    if (layer.modal) {
                        backdrop.paint(frame, area);
                    }
                    if (layer.clipped && layer.area != null) {
                        canvas.pushClip(layer.area.intersect(area));
                        try {
                            layer.node.paint(frame);
                        } finally {
                            canvas.popClip();
                        }
                    } else {
                        layer.node.paint(frame);
                    }
                }
            } finally {
                painting.clear();
            }
        }
        if (this.tooltipNode != null) {
            canvas.nextLayer();
            this.tooltipNode.paint(frame);
        }
        this.requestHoverCursor(canvas);
    }

    private void expireToasts() {
        if (this.layers.isEmpty()) {
            return;
        }
        final long now = this.platform.clock().millis();
        // Copied only when a toast is due, so a frame with overlays open allocates nothing here
        boolean due = false;
        for (int i = 0; i < this.layers.size() && !due; i++) {
            final Layer layer = this.layers.get(i);
            due = layer.isToast() && !layer.fadingOut && now >= layer.fadeOutAt();
        }
        if (!due) {
            return;
        }
        for (final Layer layer : List.copyOf(this.layers)) {
            if (layer.isToast() && !layer.fadingOut && now >= layer.fadeOutAt()) {
                if (layer.fadeMillis > 0L) {
                    // It closes when the fade ends, at its time or on the next frame after it
                    layer.fadingOut = true;
                    this.animator.animate(layer.node.facade(), Component.OPACITY, 0f,
                            Math.max(0L, layer.expiresAt - now), Easing.EASE_IN, Interpolator.FLOAT)
                            .onFinished(layer::close);
                } else {
                    layer.close();
                }
            }
        }
    }

    private void requestHoverCursor(final Canvas canvas) {
        // A disabled node shows the cursor of the nearest node that would take the pointer
        for (Node node = usableAncestor(this.hovered); node != null; node = node.parentNode()) {
            if (node.cursor() != null) {
                canvas.requestCursor(node.cursor());
                return;
            }
        }
        if (this.hovered != null) {
            canvas.requestCursor(CursorKind.ARROW);
        }
    }

    private void pumpKeyRepeat() {
        if (this.platform.capabilities().sendsKeyRepeat()) {
            return;
        }
        final Key key = this.keyRepeater.poll(this.platform.clock().millis());
        if (key != null) {
            try (CauseFrame _ = this.inputFrame(Device.KEYBOARD, this.keyRepeater.modifiers())) {
                this.dispatchKey(key, KeyAction.REPEAT, this.keyRepeater.modifiers());
            }
        }
    }

    /**
     * Tells the root which screen its host shows, for the {@link CauseKeys#SCREEN} context of input.
     *
     * @param screen the screen, or {@code null} when none is open
     */
    public void setScreen(final Screen screen) {
        this.screen = screen;
    }

    /**
     * Opens the frame every input runs in: the user as the origin, the device, the modifiers and the screen.
     */
    private CauseFrame inputFrame(final Device device, final Modifiers modifiers) {
        final CauseFrame frame = this.ui.causes().pushFrame()
                .put(CauseKeys.ORIGIN, this.user)
                .put(CauseKeys.DEVICE, device)
                .put(CauseKeys.MODIFIERS, modifiers);
        if (this.screen != null) {
            frame.put(CauseKeys.SCREEN, this.screen);
        }
        return frame;
    }

    // ------------------------------------------------------------------
    // Pointer
    // ------------------------------------------------------------------

    /**
     * Handles a pointer move: starts or continues drags, updates hover and the tooltip, and fires a move event at
     * the hovered node.
     *
     * @param x the pointer's x, in UI units
     * @param y the pointer's y, in UI units
     * @param modifiers the modifiers held
     * @return true if a drag is in progress or the pointer is over a node
     */
    public boolean pointerMoved(final float x, final float y, final Modifiers modifiers) {
        this.frames.request();
        try (CauseFrame _ = this.inputFrame(Device.MOUSE, modifiers)) {
            return this.handlePointerMoved(x, y, modifiers);
        }
    }

    private boolean handlePointerMoved(final float x, final float y, final Modifiers modifiers) {
        this.pointerX = x;
        this.pointerY = y;
        this.pointerInside = true;
        this.updateModifiers(modifiers, null, null);

        boolean dragging = false;
        for (final Map.Entry<MouseButton, Press> entry : this.presses.entrySet()) {
            final Press press = entry.getValue();
            final Node target = press.target;
            // Drags carry the button and click count of the press that started them, as presses and releases do
            try (CauseFrame _ = this.ui.causes().pushFrame().put(CauseKeys.BUTTON, entry.getKey())
                    .put(CauseKeys.CLICKS, press.clicks)) {
                if (!press.dragging && (Math.abs(x - press.startX) > DRAG_THRESHOLD || Math.abs(y - press.startY) > DRAG_THRESHOLD)) {
                    press.dragging = true;
                    target.fire(InputEvents.DRAG_START, new DragEventImpl(target.facade(), press.startX, press.startY,
                            modifiers, entry.getKey(), press.clicks, press.startX, press.startY, 0f, 0f));
                }
                if (press.dragging && target.root() == this) {
                    target.fire(InputEvents.DRAG, new DragEventImpl(target.facade(), x, y, modifiers, entry.getKey(),
                            press.clicks, press.startX, press.startY, x - press.lastX, y - press.lastY));
                    dragging = true;
                }
            }
            press.lastX = x;
            press.lastY = y;
        }

        this.updateHover(x, y, modifiers);
        if (this.hovered != null && this.hoverLive) {
            this.hovered.fire(InputEvents.POINTER_MOVE, new PointerEventImpl(this.hovered.facade(), x, y, modifiers));
        }
        this.updateTooltip();
        return dragging || this.hovered != null;
    }

    /**
     * Handles a button going down or up. See the class description for the rules.
     *
     * @param x the pointer's x, in UI units
     * @param y the pointer's y, in UI units
     * @param button the button
     * @param pressed true for a press, false for a release
     * @param modifiers the modifiers held
     * @return for a press, true if it hit a node, closed a popup or was blocked by a dialog; for a release, true if a
     *     press of that button was being tracked
     */
    public boolean pointerButton(final float x, final float y, final MouseButton button, final boolean pressed,
                                 final Modifiers modifiers) {
        this.frames.request();
        try (CauseFrame _ = this.inputFrame(Device.MOUSE, modifiers).put(CauseKeys.BUTTON, button)) {
            return this.handlePointerButton(x, y, button, pressed, modifiers);
        }
    }

    private boolean handlePointerButton(final float x, final float y, final MouseButton button,
                                        final boolean pressed, final Modifiers modifiers) {
        this.updateModifiers(modifiers, null, null);
        final boolean used = pressed ? this.pointerPressed(x, y, button, modifiers) : this.pointerReleased(x, y, button, modifiers);
        this.updateTooltip();
        return used;
    }

    private boolean pointerPressed(final float x, final float y, final MouseButton button, final Modifiers modifiers) {
        this.pointerX = x;
        this.pointerY = y;
        this.pointerInside = true;
        this.swallowedReleases.remove(button);
        final Press stale = this.presses.remove(button);
        if (stale != null) {
            // The engine lost the release of the button's last press, so end that press first
            this.endPress(stale, button, stale.lastX, stale.lastY, modifiers, false);
        }
        this.updateHover(x, y, modifiers);

        final Node target = this.hovered;
        final int pressedLayer = target == null ? -1 : this.layerOf(target);
        boolean dismissed = false;
        // From a copy, since closing one layer can close others through their handlers
        final List<Layer> layers = List.copyOf(this.layers);
        for (int i = layers.size() - 1; i > pressedLayer; i--) {
            final Layer layer = layers.get(i);
            if (layer.modal) {
                break;
            }
            if (!layer.open || layer.isToast() || !layer.lightDismiss) {
                continue;
            }
            layer.close();
            dismissed = true;
        }
        if (dismissed) {
            this.swallowedReleases.add(button);
            this.updateHover(x, y, modifiers);
            return true;
        }

        if (target == null) {
            if (this.topDialog() != null) {
                this.swallowedReleases.add(button);
                return true;
            }
            this.changeFocus(null);
            return false;
        }
        if (!isUsable(target)) {
            // A disabled node takes the press so nothing behind it acts, and does nothing with it
            this.swallowedReleases.add(button);
            return true;
        }

        this.focusFromPress = true;
        try {
            this.changeFocus(this.focusableAncestor(target));
        } finally {
            this.focusFromPress = false;
        }

        for (Node node = target; node != null; node = node.parentNode()) {
            node.hooks().pressedWithin(target.facade());
        }

        final int clicks = this.clickCounter.press(target, button, x, y, this.platform.clock().millis());
        this.presses.put(button, new Press(target, clicks, x, y));
        target.setPressed(true);
        try (CauseFrame _ = this.ui.causes().pushFrame().put(CauseKeys.CLICKS, clicks)) {
            target.fire(InputEvents.POINTER_PRESS, new PointerButtonEventImpl(target.facade(), x, y, modifiers, button,
                    clicks));
        }
        return true;
    }

    private boolean pointerReleased(final float x, final float y, final MouseButton button, final Modifiers modifiers) {
        this.pointerX = x;
        this.pointerY = y;
        this.pointerInside = true;

        final Press press = this.presses.remove(button);
        if (press == null) {
            // The release of a press that closed popups or hit a dialog's backdrop is used up with it
            return this.swallowedReleases.remove(button);
        }
        this.endPress(press, button, x, y, modifiers, true);
        this.updateHover(x, y, modifiers);
        return true;
    }

    /**
     * Ends a press that was taken out of the tracked presses: clears the target's pressed state unless another button
     * still holds it, fires the release, and then either ends the drag or, when {@code mayClick} is true and the
     * release lands inside the target while it and every ancestor are visible and enabled, fires a click.
     */
    private void endPress(final Press press, final MouseButton button, final float x, final float y,
                          final Modifiers modifiers, final boolean mayClick) {
        final Node target = press.target;
        if (this.presses.values().stream().noneMatch(other -> other.target == target)) {
            target.setPressed(false);
        }
        if (target.root() != this) {
            return;
        }

        try (CauseFrame _ = this.ui.causes().pushFrame().put(CauseKeys.CLICKS, press.clicks)) {
            target.fire(InputEvents.POINTER_RELEASE, new PointerButtonEventImpl(target.facade(), x, y, modifiers, button,
                    press.clicks));
            if (press.dragging) {
                target.fire(InputEvents.DRAG_END, new DragEventImpl(target.facade(), x, y, modifiers, button,
                        press.clicks, press.startX, press.startY, x - press.lastX, y - press.lastY));
            } else if (mayClick && target.root() == this && isUsable(target) && target.bounds().contains(x, y)) {
                target.fire(InputEvents.CLICK, new PointerButtonEventImpl(target.facade(), x, y, modifiers, button,
                        press.clicks));
            }
        }
    }

    /**
     * Returns whether a node and every ancestor are visible and enabled.
     */
    private static boolean isUsable(final Node node) {
        for (Node candidate = node; candidate != null; candidate = candidate.parentNode()) {
            if (!candidate.isVisible() || !candidate.isEnabled()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the node itself if it and every ancestor are visible and enabled, else its nearest ancestor that is,
     * which is the one just above the outermost disabled or hidden node.
     *
     * @return the node, an ancestor, or {@code null} for a {@code null} node or when no ancestor is usable
     */
    private static Node usableAncestor(final Node node) {
        Node usable = node;
        for (Node candidate = node; candidate != null; candidate = candidate.parentNode()) {
            if (!candidate.isVisible() || !candidate.isEnabled()) {
                usable = candidate.parentNode();
            }
        }
        return usable;
    }

    /**
     * Handles a scroll by firing a scroll event at the hovered node, or at its nearest enabled ancestor when it is
     * disabled or inside a disabled node, which bubbles until something handles it. If something did, layout runs
     * and hover is worked out again.
     *
     * @param x the pointer's x, in UI units
     * @param y the pointer's y, in UI units
     * @param scrollX the horizontal amount, positive toward the left
     * @param scrollY the vertical amount, positive when the wheel turns away from the user
     * @param kind the kind of device the scroll came from
     * @param modifiers the modifiers held
     * @return true if a node handled the scroll
     */
    public boolean scroll(final float x, final float y, final float scrollX, final float scrollY,
                          final ScrollKind kind, final Modifiers modifiers) {
        this.frames.request();
        try (CauseFrame _ = this.inputFrame(Device.MOUSE, modifiers)) {
            return this.handleScroll(x, y, scrollX, scrollY, kind, modifiers);
        }
    }

    private boolean handleScroll(final float x, final float y, final float scrollX, final float scrollY,
                                 final ScrollKind kind, final Modifiers modifiers) {
        this.pointerX = x;
        this.pointerY = y;
        this.pointerInside = true;
        this.updateModifiers(modifiers, null, null);
        this.updateHover(x, y, modifiers);
        final Node target = usableAncestor(this.hovered);
        if (target == null) {
            return false;
        }
        final boolean handled = target.fire(InputEvents.SCROLL,
                new ScrollEventImpl(target.facade(), x, y, modifiers, scrollX, scrollY, kind)).isHandled();
        if (handled) {
            this.layout();
            this.updateHover(x, y, modifiers);
            this.updateTooltip();
        }
        return handled;
    }

    /**
     * Handles the pointer leaving the host's area: the hovered node gets an exit event, hover clears and the
     * tooltip hides. Presses stay tracked, so a drag can continue when the pointer comes back.
     */
    public void pointerLeft() {
        this.frames.request();
        try (CauseFrame _ = this.inputFrame(Device.MOUSE, this.modifiers)) {
            this.handlePointerLeft();
        }
    }

    /**
     * Returns where the pointer was last seen, from the last pointer input; 0 before any.
     *
     * @return the x in root coordinates
     */
    public float pointerX() {
        return this.pointerX;
    }

    /**
     * Returns where the pointer was last seen, from the last pointer input; 0 before any.
     *
     * @return the y in root coordinates
     */
    public float pointerY() {
        return this.pointerY;
    }

    private void handlePointerLeft() {
        this.pointerInside = false;
        if (this.hovered != null) {
            final Node old = this.hovered;
            final boolean live = this.hoverLive;
            this.hovered = null;
            this.hoverLive = false;
            if (live) {
                old.setHovered(false);
                old.fire(InputEvents.POINTER_EXIT, new PointerEventImpl(old.facade(), this.pointerX, this.pointerY,
                        this.modifiers));
            }
        }
        this.updateTooltip();
    }

    /**
     * Returns the deepest visible node under the pointer. It may be disabled or inside a disabled node, and is then
     * not {@linkplain Node#isHovered() marked hovered}.
     *
     * @return the hovered node, or {@code null} if the pointer is over nothing or outside the host
     */
    public Node hovered() {
        return this.hovered;
    }

    /**
     * Returns whether the hovered node is a node or one of its descendants.
     *
     * @param node the node
     * @return true if the pointer is over the node or something inside it
     */
    public boolean isHoverWithin(final Node node) {
        for (Node candidate = this.hovered; candidate != null; candidate = candidate.parentNode()) {
            if (candidate == node) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the node the pointer would hit at a point: in the topmost overlay that has something there, where a
     * dialog blocks every layer below it, else in the content. The tooltip is never hit. The node may be disabled.
     *
     * @param x the x, in the host's coordinates
     * @param y the y, in the host's coordinates
     * @return the deepest visible node there, or {@code null} if nothing is there or a dialog's backdrop is
     */
    public Node hitTest(final float x, final float y) {
        for (int i = this.layers.size() - 1; i >= 0; i--) {
            final Layer layer = this.layers.get(i);
            final Node hit = layer.node.hitTest(x, y);
            if (hit != null) {
                return hit;
            }
            if (layer.modal) {
                return null;
            }
        }
        return this.content == null ? null : this.content.hitTest(x, y);
    }

    private void updateHover(final float x, final float y, final Modifiers modifiers) {
        this.layout();
        this.hoverStale = false;
        final Node next = this.hitTest(x, y);
        // A disabled node under the pointer is hovered for its tooltip but hears nothing
        final boolean live = next != null && isUsable(next);
        if (next == this.hovered && live == this.hoverLive) {
            return;
        }
        final Node previous = this.hovered;
        final boolean wasLive = this.hoverLive;
        this.hovered = next;
        this.hoverLive = live;
        if (previous != null && wasLive) {
            previous.setHovered(false);
            previous.fire(InputEvents.POINTER_EXIT, new PointerEventImpl(previous.facade(), x, y, modifiers));
        }
        if (live) {
            next.setHovered(true);
            next.fire(InputEvents.POINTER_ENTER, new PointerEventImpl(next.facade(), x, y, modifiers));
        }
    }

    // ------------------------------------------------------------------
    // Tooltips
    // ------------------------------------------------------------------

    void tooltipChanged(final Node node) {
        if (node == this.tooltipOwner || this.isHoverWithin(node)) {
            this.updateTooltip();
        }
    }

    private void updateTooltip() {
        Node owner = null;
        if (this.presses.values().stream().noneMatch(press -> press.dragging)) {
            for (Node node = this.hovered; node != null; node = node.parentNode()) {
                if (node.shownTooltip() != null) {
                    owner = node;
                    break;
                }
            }
        }
        if (owner == this.tooltipOwner && (owner == null || owner.shownTooltip() == this.tooltipShown)) {
            if (this.tooltipNode != null) {
                // Follow the pointer
                this.requestLayout();
            }
            if (owner == null) {
                this.tooltipPending = null;
            }
            return;
        }
        // Moving from one tooltip straight to another skips the delay
        final boolean warm = this.tooltipNode != null;
        this.closeTooltip();
        if (owner == null) {
            this.tooltipPending = null;
            return;
        }
        if (!warm && this.tooltipDelay() > 0L) {
            if (owner != this.tooltipPending) {
                this.tooltipPending = owner;
                this.tooltipPendingSince = this.platform.clock().millis();
            }
            return;
        }
        this.showTooltip(owner);
    }

    /**
     * Shows the waiting tooltip once its owner has been hovered for the delay.
     */
    private void showPendingTooltip() {
        final Node owner = this.tooltipPending;
        if (owner != null && owner.shownTooltip() != null
                && this.platform.clock().millis() - this.tooltipPendingSince >= this.tooltipDelay()) {
            this.showTooltip(owner);
        }
    }

    private void showTooltip(final Node owner) {
        this.tooltipPending = null;
        final Tooltip tooltip = owner.shownTooltip();
        final Node node = switch (tooltip) {
            case Tooltip.Lines lines -> this.ui.nodeOf(new TooltipView(this.ui, lines));
            case Tooltip.Custom custom -> this.ui.nodeOf(custom.builder().apply(this.ui));
        };
        if (node.parentNode() != null || node.root() != null) {
            throw new IllegalArgumentException("A tooltip builder must return a new component, not one already in a tree");
        }
        // Checked before the tooltip is recorded, so a theme that lacks a key leaves no tooltip behind
        this.validateTheme(this.theme, node);
        this.tooltipOwner = owner;
        this.tooltipShown = tooltip;
        this.tooltipNode = node;
        this.attachTree(node);
        this.requestLayout();
    }

    private void closeTooltip() {
        final Node node = this.tooltipNode;
        this.tooltipNode = null;
        this.tooltipOwner = null;
        this.tooltipShown = null;
        if (node != null) {
            this.detachTree(node);
        }
    }

    /**
     * Returns the component showing as the tooltip.
     *
     * @return the tooltip's content, or empty when no tooltip shows
     */
    public Optional<Component<?>> tooltip() {
        return Optional.ofNullable(this.tooltipNode).map(Node::facade);
    }

    // ------------------------------------------------------------------
    // Keyboard
    // ------------------------------------------------------------------

    /**
     * Handles a key event from the engine. See the class description for where it goes, how Escape closes
     * overlays and how Tab works.
     *
     * @param key the physical key
     * @param action whether the key went down, repeated or went up
     * @param modifiers the modifiers the engine reports as held; for a modifier key's own event the key event and its
     *     cause carry them with that modifier added on a press or repeat and taken out on a release, since engines
     *     differ on whether they include it
     * @return true if a node handled the key, it closed an overlay, or it moved focus
     */
    public boolean key(final Key key, final KeyAction action, final Modifiers modifiers) {
        this.frames.request();
        try (CauseFrame frame = this.inputFrame(Device.KEYBOARD, modifiers)) {
            this.updateModifiers(modifiers, key, action);
            frame.put(CauseKeys.MODIFIERS, this.modifiers);
            return this.handleKey(key, action, this.modifiers);
        }
    }

    private boolean handleKey(final Key key, final KeyAction action, final Modifiers modifiers) {
        if (!this.platform.capabilities().sendsKeyRepeat()) {
            this.keyRepeater.onKey(key, action, modifiers, this.platform.clock().millis());
        }
        return this.dispatchKey(key, action, modifiers);
    }

    private boolean dispatchKey(final Key key, final KeyAction action, final Modifiers modifiers) {
        final Layer dialog = this.topDialog();
        final Node target = this.focused != null ? this.focused : dialog != null ? dialog.node : this.content;
        if (target == null) {
            return false;
        }
        if (target.fire(InputEvents.KEY, new KeyEventImpl(target.facade(), key, action, modifiers,
                this.shortcutModifier())).isHandled()) {
            return true;
        }
        if (action.isDown() && this.runShortcut(target, key, modifiers)) {
            return true;
        }
        if (key == Key.ESCAPE && action == KeyAction.PRESS) {
            for (int i = this.layers.size() - 1; i >= 0; i--) {
                if (!this.layers.get(i).isToast()) {
                    this.layers.get(i).close();
                    return true;
                }
            }
        }
        if (key == Key.TAB && action.isDown() && !modifiers.control() && !modifiers.alt()) {
            return this.moveFocus(modifiers.shift());
        }
        return false;
    }

    /**
     * Returns the commands this root runs from their shortcuts.
     *
     * @return the shortcuts
     */
    public Shortcuts shortcuts() {
        return this.shortcuts;
    }

    /**
     * Returns the platform's shortcut modifier, from its conventions.
     *
     * @return the modifier that {@code KeyStroke.shortcut} strokes hold
     */
    public Modifier shortcutModifier() {
        return this.platform.conventions().shortcutModifier();
    }

    /**
     * Runs the first enabled command whose stroke matches a key nothing on the focus path consumed: those scoped to
     * the key's target or an ancestor, innermost first, then the host's own while no dialog is open, then those of
     * the visible command sources in the top dialog, or in the content while no dialog is open.
     */
    private boolean runShortcut(final Node target, final Key key, final Modifiers modifiers) {
        if (key.isModifier()) {
            return false;
        }
        final Modifier shortcutModifier = this.shortcutModifier();
        final Layer dialog = this.topDialog();
        Command command = this.shortcuts.matchScoped(target, key, modifiers, shortcutModifier);
        if (command == null && dialog == null) {
            command = this.shortcuts.matchHost(key, modifiers, shortcutModifier);
        }
        if (command == null) {
            command = this.matchCommandSources(dialog != null ? dialog.node : this.content, key, modifiers,
                    shortcutModifier);
        }
        if (command == null) {
            return false;
        }
        Commands.run(this.ui.causes(), command);
        return true;
    }

    private Command matchCommandSources(final Node scope, final Key key, final Modifiers modifiers,
                                        final Modifier shortcutModifier) {
        if (scope == null) {
            return null;
        }
        if (this.commandSources == null || this.commandSourceScope != scope) {
            final List<Node> sources = new ArrayList<>();
            visit(scope, node -> {
                if (node.facade() instanceof CommandSource) {
                    sources.add(node);
                }
            });
            this.commandSources = sources;
            this.commandSourceScope = scope;
        }
        for (final Node source : this.commandSources) {
            if (!isUsable(source)) {
                continue;
            }
            final List<Command> commands = this.commandScratch;
            commands.clear();
            try {
                ((CommandSource) source.facade()).collectCommands(commands);
                for (final Command command : commands) {
                    if (ShortcutsImpl.matches(command, key, modifiers, shortcutModifier)) {
                        return command;
                    }
                }
            } finally {
                commands.clear();
            }
        }
        return null;
    }

    /**
     * Handles a typed character by firing a char event at the focused node. A value that is not a Unicode code point,
     * or is a lone surrogate, is ignored.
     *
     * @param codePoint the Unicode code point
     * @param modifiers the modifiers held
     * @return true if a node handled it; false when nothing has focus or the code point was ignored
     */
    public boolean charTyped(final int codePoint, final Modifiers modifiers) {
        this.frames.request();
        try (CauseFrame _ = this.inputFrame(Device.KEYBOARD, modifiers)) {
            return this.handleCharTyped(codePoint, modifiers);
        }
    }

    private boolean handleCharTyped(final int codePoint, final Modifiers modifiers) {
        if (this.focused == null || !Character.isValidCodePoint(codePoint)
                || codePoint >= Character.MIN_SURROGATE && codePoint <= Character.MAX_SURROGATE) {
            return false;
        }
        return this.focused.fire(InputEvents.CHAR, new CharEventImpl(this.focused.facade(), codePoint, modifiers)).isHandled();
    }

    /**
     * Works out the held modifiers from an input's modifiers and, for a modifier key, the key itself, since engines
     * differ on whether a modifier key's own event includes it. Tells the content and each overlay when they change.
     */
    private void updateModifiers(final Modifiers reported, final Key key, final KeyAction action) {
        Modifiers next = reported;
        final Modifier own = key == null ? null : switch (key) {
            case LEFT_SHIFT, RIGHT_SHIFT -> Modifier.SHIFT;
            case LEFT_CONTROL, RIGHT_CONTROL -> Modifier.CONTROL;
            case LEFT_ALT, RIGHT_ALT -> Modifier.ALT;
            case LEFT_SUPER, RIGHT_SUPER -> Modifier.SUPER;
            default -> null;
        };
        if (own != null) {
            next = action.isDown() ? next.with(own) : next.without(own);
        }
        if (next.equals(this.modifiers)) {
            return;
        }
        this.modifiers = next;
        for (final Node top : this.tops()) {
            if (top != this.tooltipNode) {
                top.fire(InputEvents.MODIFIERS_CHANGED, new ModifiersEventImpl(top.facade(), next));
            }
        }
    }

    // ------------------------------------------------------------------
    // Focus
    // ------------------------------------------------------------------

    /**
     * Returns the node with keyboard focus.
     *
     * @return the focused node, or {@code null} if nothing has focus
     */
    public Node focused() {
        return this.focused;
    }

    boolean requestFocus(final Node node) {
        if (node != null && !this.canFocus(node)) {
            return false;
        }
        this.changeFocus(node);
        return true;
    }

    /**
     * Takes focus away from whatever has it, firing a focus-lost event.
     */
    public void clearFocus() {
        this.changeFocus(null);
    }

    /**
     * Moves focus to the next or previous node in {@link #tabOrder()}, wrapping at the ends. With nothing focused,
     * forward goes to the first node and backward to the last. A focused node that is not in the order, such as one
     * that is focusable but not tabbable, counts as if it were, at the place its tab index and tree position give it,
     * so Tab goes on from where focus is.
     *
     * @param backwards true to move to the previous node
     * @return true if focus moved; false if no node is tabbable
     */
    public boolean moveFocus(final boolean backwards) {
        final List<Node> order = this.tabOrder();
        if (order.isEmpty()) {
            return false;
        }
        final int index = this.focused == null ? -1 : order.indexOf(this.focused);
        final int next;
        if (index >= 0) {
            next = Math.floorMod(index + (backwards ? -1 : 1), order.size());
        } else {
            final int place = this.focused == null ? -1 : this.placeInOrder(order, this.focused);
            if (place < 0) {
                next = backwards ? order.size() - 1 : 0;
            } else {
                // place is how many nodes in the order come before the focused node
                next = backwards ? Math.floorMod(place - 1, order.size()) : place % order.size();
            }
        }
        this.changeFocus(order.get(next));
        return true;
    }

    /**
     * Returns how many nodes of a tab order come before a node that is not in it, by tab index and then tree order,
     * or -1 if the node is not in the scope the order was collected from.
     */
    private int placeInOrder(final List<Node> order, final Node node) {
        final Layer dialog = this.topDialog();
        final Node scope = dialog != null ? dialog.node : this.content;
        if (scope == null) {
            return -1;
        }
        final Map<Node, Integer> treeOrder = new IdentityHashMap<>();
        visit(scope, candidate -> treeOrder.put(candidate, treeOrder.size()));
        final Integer position = treeOrder.get(node);
        if (position == null) {
            return -1;
        }
        int place = 0;
        for (final Node candidate : order) {
            final int byIndex = Integer.compare(candidate.tabIndex(), node.tabIndex());
            if (byIndex < 0 || byIndex == 0 && treeOrder.get(candidate) < position) {
                place++;
            }
        }
        return place;
    }

    /**
     * Returns the nodes Tab moves through: every focusable, tabbable node whose ancestors are all visible and
     * enabled, in the top dialog or, with no dialog open, in the content, sorted by tab index and then by tree
     * order. The list is cached until the tree changes.
     *
     * @return an unmodifiable list, possibly empty
     */
    public List<Node> tabOrder() {
        if (this.tabOrder == null) {
            final List<Node> order = new ArrayList<>();
            final Layer dialog = this.topDialog();
            final Node scope = dialog != null ? dialog.node : this.content;
            if (scope != null) {
                this.collectTabbable(scope, order);
            }
            order.sort(Comparator.comparingInt(Node::tabIndex));
            this.tabOrder = List.copyOf(order);
        }
        return this.tabOrder;
    }

    /**
     * Returns the nodes Tab would move through inside one subtree, such as an open popup: every focusable, tabbable
     * node in it whose ancestors within it are visible and enabled, sorted by tab index and then by tree order. Not
     * cached.
     *
     * @param scope the top of the subtree, which counts too
     * @return a new list, possibly empty
     */
    public List<Node> tabOrderWithin(final Node scope) {
        final List<Node> order = new ArrayList<>();
        this.collectTabbable(scope, order);
        order.sort(Comparator.comparingInt(Node::tabIndex));
        return order;
    }

    private void collectTabbable(final Node node, final List<Node> out) {
        if (!node.isVisible() || !node.isEnabled()) {
            return;
        }
        if (node.isFocusable() && node.isTabbable()) {
            out.add(node);
        }
        if (node instanceof ContainerNode container) {
            for (final Node child : container.childNodes()) {
                this.collectTabbable(child, out);
            }
        }
    }

    private Node focusableAncestor(final Node node) {
        for (Node candidate = node; candidate != null; candidate = candidate.parentNode()) {
            if (this.canFocus(candidate)) {
                return candidate;
            }
            final Node delegate = candidate.focusDelegate();
            if (delegate != null && this.canFocus(delegate)) {
                return delegate;
            }
        }
        return null;
    }

    private boolean canFocus(final Node node) {
        if (node.root() != this || !node.isFocusable()) {
            return false;
        }
        for (Node candidate = node; candidate != null; candidate = candidate.parentNode()) {
            if (!candidate.isVisible() || !candidate.isEnabled()) {
                return false;
            }
        }
        final int layer = this.layerOf(node);
        return layer != Integer.MIN_VALUE && layer >= this.topDialogIndex();
    }

    /**
     * Moves focus. The old node loses focus and hears {@code FOCUS_LOST} first, then the new one gains it and hears
     * {@code FOCUS_GAINED}. A focus-lost handler may move focus again, as a field that refuses to let go does: that
     * change runs to the end, and this one stops there, so the node it was moving to never hears that it gained or
     * lost focus. A focus-lost handler that throws still lets the new node gain focus before the exception goes on.
     * A node that is another's {@linkplain Node#focusDelegate() focus delegate} has its owner hear each event right
     * after it, and the owner hears {@code FOCUS_GAINED} only while the delegate still has focus.
     */
    private void changeFocus(final Node next) {
        if (next == this.focused) {
            return;
        }
        this.frames.request();
        final Node previous = this.focused;
        // A node another change assigned but has not told yet never had focus, so it has none to lose
        final Node lost = previous == this.focusArriving ? null : previous;
        this.focused = next;
        this.focusArriving = next;
        try {
            if (lost != null) {
                lost.setFocused(false);
                if (lost.acceptsTextInput() && (next == null || !next.acceptsTextInput())) {
                    this.platform.textInput().stop();
                }
                lost.fire(Focusable.FOCUS_LOST, new FocusEventImpl(lost.facade(), next == null ? null : next.facade()));
                // The control a delegate takes focus for hears it too
                final Node owner = lost.focusOwner();
                if (owner != null) {
                    owner.fire(Focusable.FOCUS_LOST,
                            new FocusEventImpl(owner.facade(), next == null ? null : next.facade()));
                }
            }
        } finally {
            if (this.focused == next) {
                this.focusArriving = null;
                if (next != null) {
                    this.focusGained(next, lost);
                }
            }
        }
    }

    private void focusGained(final Node next, final Node lost) {
        next.setFocused(true);
        if (next.acceptsTextInput()) {
            this.platform.textInput().start(next.bounds(), next.textInputKind());
        }
        next.fire(Focusable.FOCUS_GAINED, new FocusEventImpl(next.facade(), lost == null ? null : lost.facade()));
        final Node owner = next.focusOwner();
        if (owner != null && this.focused == next) {
            owner.fire(Focusable.FOCUS_GAINED, new FocusEventImpl(owner.facade(), lost == null ? null : lost.facade()));
        }
        for (Node ancestor = next.parentNode(); ancestor != null && this.focused == next; ancestor = ancestor.parentNode()) {
            ancestor.hooks().descendantFocused(next.facade());
        }
    }

    /**
     * Stops the engine's text input if the focused node takes text, because another screen now covers this one and
     * takes the keyboard. {@link #resumeTextInput()} starts it again.
     */
    public void suspendTextInput() {
        if (this.focused != null && this.focused.acceptsTextInput()) {
            this.platform.textInput().stop();
        }
    }

    /**
     * Starts the engine's text input again if the focused node takes text, because this screen is on top again after
     * the screen that covered it closed.
     */
    public void resumeTextInput() {
        if (this.focused != null && this.focused.acceptsTextInput()) {
            this.platform.textInput().start(this.focused.bounds(), this.focused.textInputKind());
        }
    }

    // ------------------------------------------------------------------
    // Overlays
    // ------------------------------------------------------------------

    /**
     * Opens a popup as {@link Overlays#popup} does, choosing whether a press outside it closes it. Core's flyout
     * opens through here.
     *
     * @param content what to show; it must not be in a tree already
     * @param anchor the area to show it next to, in the host's coordinates
     * @param lightDismiss true to close it on a press outside, which then goes no further; false to leave it open
     *     and let the press through
     * @return the open popup
     * @throws IllegalArgumentException if the content is already in a tree or was made by another factory
     */
    public Overlay popup(final Component<?> content, final Rect anchor, final boolean lightDismiss) {
        final Layer layer = this.open(content, Objects.requireNonNull(anchor, "anchor"), false);
        layer.lightDismiss = lightDismiss;
        return layer;
    }

    /**
     * Opens a popup laid out in an area, as {@link Overlays#popupIn} does, choosing whether a press outside it closes
     * it. Core's flyout and navigation view open through here.
     *
     * @param content what to show; it must not be in a tree already
     * @param area the area to lay it out in, in the host's coordinates; cut to the host
     * @param lightDismiss true to close it on a press outside, which then goes no further; false to leave it open
     *     and let the press through
     * @return the open popup
     * @throws IllegalArgumentException if the content is already in a tree or was made by another factory
     */
    public Overlay popupIn(final Component<?> content, final Rect area, final boolean lightDismiss) {
        Objects.requireNonNull(area, "area");
        final Layer layer = this.open(content, null, false);
        layer.area = area;
        layer.lightDismiss = lightDismiss;
        return layer;
    }

    /**
     * Moves an open popup that was laid out in an area to another area, such as when the component it covers moved.
     * The popup is laid out again before the next frame.
     *
     * @param overlay a popup this root opened in an area
     * @param area the new area, in the host's coordinates; cut to the host
     * @throws IllegalArgumentException if this root did not open the overlay in an area
     */
    public void setPopupArea(final Overlay overlay, final Rect area) {
        Objects.requireNonNull(area, "area");
        if (!(overlay instanceof final Layer layer) || layer.root() != this || layer.area == null) {
            throw new IllegalArgumentException(overlay + " is not a popup this root opened in an area");
        }
        if (!layer.area.equals(area)) {
            layer.area = area;
            this.requestLayout();
        }
    }

    /**
     * Changes whether an open popup laid out in an area is drawn cut to that area, as a pane that slides in from
     * outside it is. The cut is the area less what lies outside the host.
     *
     * @param overlay a popup this root opened in an area
     * @param clipped true to cut its drawing to the area
     * @throws IllegalArgumentException if this root did not open the overlay in an area
     */
    public void setPopupClipped(final Overlay overlay, final boolean clipped) {
        if (!(overlay instanceof final Layer layer) || layer.root() != this || layer.area == null) {
            throw new IllegalArgumentException(overlay + " is not a popup this root opened in an area");
        }
        if (layer.clipped != clipped) {
            layer.clipped = clipped;
            this.frames.request();
        }
    }

    /**
     * Changes whether a press outside an open popup closes it.
     *
     * @param overlay a popup this root opened
     * @param lightDismiss true to close it on a press outside
     * @throws IllegalArgumentException if this root did not open the overlay
     */
    public void setLightDismiss(final Overlay overlay, final boolean lightDismiss) {
        if (!(overlay instanceof final Layer layer) || layer.root() != this) {
            throw new IllegalArgumentException(overlay + " is not an overlay of this root");
        }
        layer.lightDismiss = lightDismiss;
    }

    private Layer open(final Component<?> content, final Rect anchor, final boolean modal) {
        return this.open(content, anchor, modal, false, 0L);
    }

    private Layer open(final Component<?> content, final Rect anchor, final boolean modal, final boolean toast,
                       final long expiresAt) {
        this.checkThread();
        final Node node = this.ui.nodeOf(content);
        if (node.parentNode() != null || node.root() != null) {
            throw new IllegalArgumentException(content + " is already part of a tree");
        }
        // Checked first, so a theme that lacks a key opens nothing
        this.validateTheme(this.theme, node);
        final Layer layer = new Layer(node, anchor, modal, toast, expiresAt);
        // Remember focus before the dialog makes it unfocusable
        layer.previousFocus = modal ? this.focused : null;
        try {
            this.attachTree(node);
        } finally {
            // Open even when an ATTACHED handler threw, so closing it detaches the content again
            this.layers.add(layer);
            this.treeChanged();
            this.requestLayout();
        }
        if (modal) {
            this.layout();
            final List<Node> order = this.tabOrder();
            this.changeFocus(order.isEmpty() ? null : order.getFirst());
        }
        return layer;
    }

    /**
     * One open overlay.
     */
    private final class Layer implements Overlay {
        final Node node;
        final Rect anchor;
        final boolean modal;
        final boolean toast;
        // When a toast closes, by the platform's clock, whose zero point is arbitrary; unused for other layers
        final long expiresAt;
        final EventDispatcher handlers;
        Node previousFocus;
        boolean open;
        // Whether a press outside closes the layer; only popups can turn it off
        boolean lightDismiss;
        // The area a popup opened in an area is laid out in, or null
        Rect area;
        // Whether the layer is drawn cut to its area
        boolean clipped;
        // A toast's fade time, and whether its fade out has started
        long fadeMillis;
        boolean fadingOut;

        Layer(final Node node, final Rect anchor, final boolean modal, final boolean toast, final long expiresAt) {
            this.node = node;
            this.anchor = anchor;
            this.modal = modal;
            this.toast = toast;
            this.expiresAt = expiresAt;
            this.handlers = new EventDispatcher();
            this.open = true;
            this.lightDismiss = true;
        }

        Root root() {
            return Root.this;
        }

        void layout(final Rect area) {
            final LayoutPass pass = Root.this.layoutPass;
            if (this.area != null) {
                final Rect inside = this.area.intersect(area);
                this.node.measure(pass, inside.width(), inside.height());
                this.node.arrange(pass, inside);
                return;
            }
            this.node.measure(pass, area.width(), area.height());
            if (this.anchor == null) {
                this.node.arrange(pass, area);
                return;
            }
            final float popupWidth = this.node.outerWidth();
            final float popupHeight = this.node.outerHeight();
            final float x = Math.max(0f, Math.min(this.anchor.x(), area.width() - popupWidth));
            final float below = area.height() - this.anchor.bottom();
            final float above = this.anchor.y();
            final float y;
            if (popupHeight <= below || below >= above) {
                y = Math.max(0f, Math.min(this.anchor.bottom(), area.height() - popupHeight));
            } else {
                y = Math.max(0f, this.anchor.y() - popupHeight);
            }
            this.node.arrange(pass, new Rect(x, y, popupWidth, popupHeight));
        }

        boolean isToast() {
            return this.toast;
        }

        /**
         * Returns when a toast's fade out starts, by the platform's clock: its fade time before it expires, or when it
         * expires if it has no fade.
         */
        long fadeOutAt() {
            return this.expiresAt - this.fadeMillis;
        }

        /**
         * Places a toast at its desired size against the right edge, with its top at {@code y}.
         *
         * @return where the next toast's top goes
         */
        float layoutToast(final Rect area, final float y, final float margin) {
            final LayoutPass pass = Root.this.layoutPass;
            this.node.measure(pass, area.width(), area.height());
            final float toastWidth = this.node.outerWidth();
            final float toastHeight = this.node.outerHeight();
            final float x = Math.max(0f, area.width() - margin - toastWidth);
            this.node.arrange(pass, new Rect(x, y, toastWidth, toastHeight));
            return y + toastHeight + margin;
        }

        @Override
        public Component<?> content() {
            return this.node.facade();
        }

        @Override
        public boolean isModal() {
            return this.modal;
        }

        @Override
        public boolean isOpen() {
            return this.open;
        }

        @Override
        public Overlay raise() {
            if (!this.open || this.modal) {
                return this;
            }
            final List<Layer> layers = Root.this.layers;
            final int from = layers.indexOf(this);
            int to = from;
            while (to + 1 < layers.size() && !layers.get(to + 1).modal) {
                to++;
            }
            if (to != from) {
                layers.remove(from);
                layers.add(to, this);
                if (layers.stream().anyMatch(Layer::isToast)) {
                    // Toasts are placed down the corner in layer order
                    Root.this.requestLayout();
                }
            }
            return this;
        }

        /**
         * Closes the layer. A focus handler that throws while the content is detached does not stop the rest: the
         * content is still released, focus still goes back, and {@code CLOSED} still fires before the exception goes
         * on.
         */
        @Override
        public Overlay close() {
            if (!this.open) {
                return this;
            }
            Root.this.checkThread();
            this.open = false;
            Root.this.layers.remove(this);
            try {
                Root.this.detachTree(this.node);
            } finally {
                release(this.node);
                Root.this.requestLayout();
                try {
                    if (this.modal && this.previousFocus != null && Root.this.canFocus(this.previousFocus)) {
                        Root.this.changeFocus(this.previousFocus);
                    }
                    if (this.handlers.hasHandlers(Overlay.CLOSED)) {
                        final ControlEventImpl<Closable<?>> event = new ControlEventImpl<>(this.node.facade(), this);
                        event.setCauses(Root.this.ui.causes());
                        this.handlers.fire(Overlay.CLOSED, event);
                    }
                } finally {
                    this.handlers.clear();
                }
            }
            return this;
        }

        @Override
        public <E extends Event> Overlay onEvent(final EventType<E> type, final int priority,
                                                 final EventHandler<? super E> handler) {
            this.subscribe(type, priority, handler);
            return this;
        }

        @Override
        public <E extends Event> Subscription subscribe(final EventType<E> type, final int priority,
                                                        final EventHandler<? super E> handler) {
            Root.this.checkThread();
            return this.handlers.subscribe(type, priority, handler);
        }
    }

    /**
     * The {@link Overlays} of this root.
     */
    private final class OverlaysImpl implements Overlays {
        @Override
        public Overlay popup(final Component<?> content, final Rect anchor) {
            return Root.this.open(content, Objects.requireNonNull(anchor, "anchor"), false);
        }

        @Override
        public Overlay popupIn(final Component<?> content, final Rect area) {
            return Root.this.popupIn(content, area, true);
        }

        @Override
        public Overlay dialog(final Component<?> content) {
            return Root.this.open(content, null, true);
        }

        @Override
        public Overlay toast(final Text message) {
            return this.toast(new ToastView(Root.this.ui, Objects.requireNonNull(message, "message")), DEFAULT_TOAST_MILLIS);
        }

        @Override
        public Overlay toast(final Component<?> content, final long millis) {
            if (millis <= 0L) {
                throw new IllegalArgumentException("A toast must show for some time: " + millis);
            }
            final long now = Root.this.platform.clock().millis();
            final Layer layer = Root.this.open(content, null, false, true, now + millis);
            final Theme theme = Root.this.theme();
            final long fade = Math.min(millis / 2L, Math.max(0L, (long) theme.metricOr(STYLE_TOAST_FADE, 0f)));
            if (fade > 0L) {
                layer.fadeMillis = fade;
                content.setOpacity(0f);
                Root.this.animator.animate(content, Component.OPACITY, 1f, fade, Easing.EASE_OUT, Interpolator.FLOAT);
            }
            content.onEvent(InputEvents.CLICK, Integer.MAX_VALUE, event -> {
                if (!event.isCancelled()) {
                    layer.close();
                }
            });
            return layer;
        }

        @Override
        public Optional<Overlay> of(final Component<?> component) {
            final Node top = topOf(Root.this.ui.nodeOf(component));
            for (final Layer layer : Root.this.layers) {
                if (layer.node == top) {
                    return Optional.of(layer);
                }
            }
            return Optional.empty();
        }

        @Override
        public List<Overlay> open() {
            return List.copyOf(Root.this.layers);
        }

        @Override
        public void closeAll() {
            while (!Root.this.layers.isEmpty()) {
                Root.this.layers.getLast().close();
            }
        }
    }

    private static final class Press {
        final Node target;
        final int clicks;
        final float startX;
        final float startY;
        float lastX;
        float lastY;
        boolean dragging;

        Press(final Node target, final int clicks, final float startX, final float startY) {
            this.target = target;
            this.clicks = clicks;
            this.startX = startX;
            this.startY = startY;
            this.lastX = startX;
            this.lastY = startY;
        }
    }
}
