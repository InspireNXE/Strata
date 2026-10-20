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
import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.component.Focusable;
import org.inspirenxe.strata.api.component.Tooltip;
import org.inspirenxe.strata.api.event.Event;
import org.inspirenxe.strata.api.event.EventHandler;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.Subscription;
import org.inspirenxe.strata.api.event.input.InputEvents;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifier;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.input.ScrollKind;
import org.inspirenxe.strata.api.overlay.Overlay;
import org.inspirenxe.strata.api.overlay.Overlays;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.ThemeException;
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
import org.inspirenxe.strata.core.input.ClickCounter;
import org.inspirenxe.strata.core.input.KeyRepeater;
import org.inspirenxe.strata.core.paint.CanvasPainter;
import org.inspirenxe.strata.spi.Canvas;
import org.inspirenxe.strata.spi.Platform;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
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
 * it, and the tooltip is never hit.</p>
 *
 * <p>Pointer rules: hover goes to the deepest visible, enabled node under the pointer. A press first closes every
 * popup above the layer it lands on, and then goes nowhere if any closed. Otherwise it focuses the pressed node or
 * its nearest focusable ancestor and clears focus if there is none. Each button is tracked on its own. A drag starts
 * once the pointer moves more than {@link #DRAG_THRESHOLD} units from the press on either axis, and its events go to
 * the pressed node wherever the pointer is. A release fires a click only when it ends no drag and lands inside the
 * pressed node, which must still be enabled. After a scroll that a node handled, hover is worked out again, since
 * content may have moved under the pointer.</p>
 *
 * <p>Keyboard rules: key events go to the focused node, or else to the top dialog, or else to the content. An
 * unhandled Escape press closes the top overlay. An unhandled Tab press or repeat without Ctrl or Alt moves focus,
 * backwards with Shift, among the nodes of the top dialog or, with no dialog, of the content. Typed characters go
 * only to the focused node. When the platform does not send key repeats, the root makes them with a
 * {@link KeyRepeater}; each repeat goes to whatever has focus when it fires.</p>
 *
 * <p>Toasts are overlays placed down the top-right corner. Presses elsewhere and Escape leave them open; each closes
 * on a click that nothing in it consumed, or on the first frame rendered after its time runs out.</p>
 *
 * <p>Tooltips: the hovered node's tooltip, or its nearest ancestor's, shows {@link #TOOLTIP_OFFSET} units right of
 * and above the pointer, flipped left of it when it would leave the host on the right, kept inside the host. It
 * follows the pointer and hides while a drag is in progress.</p>
 *
 * <p>Every method must be called on the UI thread.</p>
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
    private final List<Layer> layers;
    private final List<Node> arrangeRequests;
    private final OverlaysImpl overlays;

    private Theme theme;
    private Node content;
    private float width;
    private float height;
    private boolean layoutRequested;

    private Node hovered;
    private Node focused;
    private List<Node> tabOrder;
    private float pointerX;
    private float pointerY;
    private Modifiers modifiers;

    private Node tooltipOwner;
    private Tooltip tooltipShown;
    private Node tooltipNode;

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
        this.layoutPass = new LayoutPass(ui, platform.textMetrics());
        this.painter = new CanvasPainter();
        this.clickCounter = new ClickCounter();
        this.keyRepeater = new KeyRepeater();
        this.presses = new EnumMap<>(MouseButton.class);
        this.layers = new ArrayList<>();
        this.arrangeRequests = new ArrayList<>();
        this.overlays = new OverlaysImpl();
        this.modifiers = Modifiers.NONE;
        this.layoutRequested = true;
        ui.bindRoot(this);
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

    // ------------------------------------------------------------------
    // Tree
    // ------------------------------------------------------------------

    /**
     * Replaces the tree, closing every overlay and the tooltip first. The theme is checked against every node of the
     * new tree before anything changes, so a theme that lacks a required key fails first. The old tree is detached,
     * which clears its hover, press and focus state.
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
        if (this.content != null) {
            this.detachTree(this.content);
        }
        this.content = content;
        if (content != null) {
            this.attachTree(content);
        }
        this.requestLayout();
    }

    /**
     * Changes the theme and applies it to every node, overlays and tooltip included. Nothing changes if the new
     * theme lacks a key the tree requires.
     *
     * @param theme the new theme
     * @throws ThemeException if the theme lacks a style key the tree or an overlay requires
     */
    public void setTheme(final Theme theme) {
        Objects.requireNonNull(theme, "theme");
        for (final Node top : this.tops()) {
            this.validateTheme(theme, top);
        }
        this.theme = theme;
        for (final Node top : this.tops()) {
            visit(top, node -> node.applyTheme(theme));
        }
        this.requestLayout();
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

    void attachTree(final Node subtree) {
        this.validateTheme(this.theme, subtree);
        visit(subtree, node -> node.attach(this));
        this.treeChanged();
    }

    void detachTree(final Node subtree) {
        visit(subtree, node -> {
            if (node == this.hovered) {
                this.hovered = null;
            }
            if (node == this.focused) {
                this.changeFocus(null);
            }
            if (node == this.tooltipOwner) {
                this.tooltipOwner = null;
            }
            this.presses.values().removeIf(press -> press.target == node);
            node.detach();
        });
        if (this.tooltipOwner == null && this.tooltipNode != null && this.tooltipNode != subtree) {
            this.closeTooltip();
        }
        this.treeChanged();
    }

    void treeChanged() {
        this.tabOrder = null;
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

    private static void visit(final Node node, final Consumer<Node> action) {
        action.accept(node);
        if (node instanceof ContainerNode container) {
            for (final Node child : List.copyOf(container.childNodes())) {
                visit(child, action);
            }
        }
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
            this.requestLayout();
        }
    }

    void requestLayout() {
        this.layoutRequested = true;
    }

    void requestArrangement(final Node node) {
        this.arrangeRequests.add(node);
        this.layoutRequested = true;
    }

    /**
     * Lays out every layer if anything requested it since the last layout: the content and dialogs are measured
     * with the root's size and arranged into it, popups are placed by their anchor, and the tooltip by the pointer.
     * Subtrees that did not change are skipped. Then nodes that asked only to be arranged again are arranged in their
     * last slot. Rendering and pointer input call this first, so bounds are never stale.
     */
    public void layout() {
        if (!this.layoutRequested) {
            return;
        }
        this.layoutRequested = false;
        final Rect area = new Rect(0, 0, this.width, this.height);
        if (this.content != null) {
            this.content.measure(this.layoutPass, this.width, this.height);
            this.content.arrange(this.layoutPass, area);
        }
        float toastY = Overlays.TOAST_MARGIN;
        for (final Layer layer : List.copyOf(this.layers)) {
            if (layer.isToast()) {
                toastY = layer.layoutToast(area, toastY);
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
        float x = this.pointerX + TOOLTIP_OFFSET;
        if (x + tooltipWidth > this.width) {
            x = this.pointerX - TOOLTIP_OFFSET - tooltipWidth;
        }
        float y = this.pointerY - TOOLTIP_OFFSET;
        x = Math.max(0f, Math.min(x, this.width - tooltipWidth));
        y = Math.max(0f, Math.min(y, this.height - tooltipHeight));
        node.arrange(this.layoutPass, new Rect(x, y, tooltipWidth, tooltipHeight));
    }

    /**
     * Draws one frame: fires a key repeat if one is due, lays out if needed, paints the content, each overlay and
     * the tooltip, and asks the canvas for the cursor of the hovered node (or its nearest ancestor that sets one,
     * falling back to the arrow).
     *
     * @param canvas the frame's canvas
     */
    public void render(final Canvas canvas) {
        this.pumpKeyRepeat();
        this.expireToasts();
        this.layout();
        final CanvasPainter frame = this.painter.begin(canvas);
        if (this.content != null) {
            this.content.paint(frame);
        }
        if (!this.layers.isEmpty()) {
            final Rect area = new Rect(0, 0, this.width, this.height);
            final Paint backdrop = this.theme.paintOr(Overlays.STYLE_BACKDROP, Paint.NONE);
            for (final Layer layer : this.layers) {
                canvas.nextLayer();
                if (layer.modal) {
                    backdrop.paint(frame, area);
                }
                layer.node.paint(frame);
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
        for (final Layer layer : List.copyOf(this.layers)) {
            if (layer.isToast() && now >= layer.expiresAt) {
                layer.close();
            }
        }
    }

    private void requestHoverCursor(final Canvas canvas) {
        for (Node node = this.hovered; node != null; node = node.parentNode()) {
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
            this.dispatchKey(key, KeyAction.REPEAT, this.keyRepeater.modifiers());
        }
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
        this.pointerX = x;
        this.pointerY = y;
        this.updateModifiers(modifiers, null, null);

        boolean dragging = false;
        for (final Map.Entry<MouseButton, Press> entry : this.presses.entrySet()) {
            final Press press = entry.getValue();
            final Node target = press.target;
            if (!press.dragging && (Math.abs(x - press.startX) > DRAG_THRESHOLD || Math.abs(y - press.startY) > DRAG_THRESHOLD)) {
                press.dragging = true;
                target.fire(InputEvents.DRAG_START, new DragEventImpl(target.facade(), press.startX, press.startY, modifiers,
                        entry.getKey(), press.clicks, press.startX, press.startY, 0f, 0f));
            }
            if (press.dragging && target.root() == this) {
                target.fire(InputEvents.DRAG, new DragEventImpl(target.facade(), x, y, modifiers, entry.getKey(),
                        press.clicks, press.startX, press.startY, x - press.lastX, y - press.lastY));
                dragging = true;
            }
            press.lastX = x;
            press.lastY = y;
        }

        this.updateHover(x, y, modifiers);
        if (this.hovered != null) {
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
        this.updateModifiers(modifiers, null, null);
        final boolean used = pressed ? this.pointerPressed(x, y, button, modifiers) : this.pointerReleased(x, y, button, modifiers);
        this.updateTooltip();
        return used;
    }

    private boolean pointerPressed(final float x, final float y, final MouseButton button, final Modifiers modifiers) {
        this.pointerX = x;
        this.pointerY = y;
        this.updateHover(x, y, modifiers);

        final Node target = this.hovered;
        final int pressedLayer = target == null ? -1 : this.layerOf(target);
        boolean dismissed = false;
        for (int i = this.layers.size() - 1; i > pressedLayer; i--) {
            final Layer layer = this.layers.get(i);
            if (layer.modal) {
                break;
            }
            if (layer.isToast()) {
                continue;
            }
            layer.close();
            dismissed = true;
        }
        if (dismissed) {
            this.updateHover(x, y, modifiers);
            return true;
        }

        if (target == null) {
            if (this.topDialog() != null) {
                return true;
            }
            this.changeFocus(null);
            return false;
        }

        this.changeFocus(this.focusableAncestor(target));

        final int clicks = this.clickCounter.press(target, button, x, y, this.platform.clock().millis());
        this.presses.put(button, new Press(target, clicks, x, y));
        target.setPressed(true);
        target.fire(InputEvents.POINTER_PRESS, new PointerButtonEventImpl(target.facade(), x, y, modifiers, button, clicks));
        return true;
    }

    private boolean pointerReleased(final float x, final float y, final MouseButton button, final Modifiers modifiers) {
        this.pointerX = x;
        this.pointerY = y;

        final Press press = this.presses.remove(button);
        if (press == null) {
            return false;
        }

        final Node target = press.target;
        if (this.presses.values().stream().noneMatch(other -> other.target == target)) {
            target.setPressed(false);
        }
        if (target.root() != this) {
            return true;
        }

        target.fire(InputEvents.POINTER_RELEASE, new PointerButtonEventImpl(target.facade(), x, y, modifiers, button, press.clicks));
        if (press.dragging) {
            target.fire(InputEvents.DRAG_END, new DragEventImpl(target.facade(), x, y, modifiers, button, press.clicks,
                    press.startX, press.startY, x - press.lastX, y - press.lastY));
        } else if (target.root() == this && target.isEnabled() && target.bounds().contains(x, y)) {
            target.fire(InputEvents.CLICK, new PointerButtonEventImpl(target.facade(), x, y, modifiers, button, press.clicks));
        }
        this.updateHover(x, y, modifiers);
        return true;
    }

    /**
     * Handles a scroll by firing a scroll event at the hovered node, which bubbles until something handles it. If
     * something did, layout runs and hover is worked out again.
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
        this.pointerX = x;
        this.pointerY = y;
        this.updateModifiers(modifiers, null, null);
        this.updateHover(x, y, modifiers);
        if (this.hovered == null) {
            return false;
        }
        final boolean handled = this.hovered.fire(InputEvents.SCROLL,
                new ScrollEventImpl(this.hovered.facade(), x, y, modifiers, scrollX, scrollY, kind)).isHandled();
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
        if (this.hovered != null) {
            final Node old = this.hovered;
            this.hovered = null;
            old.setHovered(false);
            old.fire(InputEvents.POINTER_EXIT, new PointerEventImpl(old.facade(), this.pointerX, this.pointerY, Modifiers.NONE));
        }
        this.updateTooltip();
    }

    /**
     * Returns the node under the pointer.
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

    private Node hitTest(final float x, final float y) {
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
        final Node next = this.hitTest(x, y);
        if (next == this.hovered) {
            return;
        }
        final Node previous = this.hovered;
        this.hovered = next;
        if (previous != null) {
            previous.setHovered(false);
            previous.fire(InputEvents.POINTER_EXIT, new PointerEventImpl(previous.facade(), x, y, modifiers));
        }
        if (next != null) {
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
                if (node.tooltip() != null) {
                    owner = node;
                    break;
                }
            }
        }
        if (owner == this.tooltipOwner && (owner == null || owner.tooltip() == this.tooltipShown)) {
            if (this.tooltipNode != null) {
                // Follow the pointer
                this.requestLayout();
            }
            return;
        }
        this.closeTooltip();
        if (owner == null) {
            return;
        }
        final Tooltip tooltip = owner.tooltip();
        final Node node = switch (tooltip) {
            case Tooltip.Lines lines -> this.ui.nodeOf(new TooltipView(this.ui, lines));
            case Tooltip.Custom custom -> this.ui.nodeOf(custom.builder().apply(this.ui));
        };
        if (node.parentNode() != null || node.root() != null) {
            throw new IllegalArgumentException("A tooltip builder must return a new component, not one already in a tree");
        }
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
     * @param modifiers the modifiers held
     * @return true if a node handled the key, it closed an overlay, or it moved focus
     */
    public boolean key(final Key key, final KeyAction action, final Modifiers modifiers) {
        this.updateModifiers(modifiers, key, action);
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
        if (target.fire(InputEvents.KEY, new KeyEventImpl(target.facade(), key, action, modifiers)).isHandled()) {
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
     * Handles a typed character by firing a char event at the focused node.
     *
     * @param codePoint the Unicode code point
     * @param modifiers the modifiers held
     * @return true if a node handled it; false when nothing has focus
     */
    public boolean charTyped(final int codePoint, final Modifiers modifiers) {
        if (this.focused == null) {
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
     * forward goes to the first node and backward to the last.
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
        if (index < 0) {
            next = backwards ? order.size() - 1 : 0;
        } else {
            next = Math.floorMod(index + (backwards ? -1 : 1), order.size());
        }
        this.changeFocus(order.get(next));
        return true;
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

    private void changeFocus(final Node next) {
        if (next == this.focused) {
            return;
        }
        final Node previous = this.focused;
        this.focused = next;

        if (previous != null) {
            previous.setFocused(false);
            if (previous.acceptsTextInput() && (next == null || !next.acceptsTextInput())) {
                this.platform.textInput().stop();
            }
            previous.fire(Focusable.FOCUS_LOST, new FocusEventImpl(previous.facade(),
                    next == null ? null : next.facade()));
        }
        if (next != null) {
            next.setFocused(true);
            if (next.acceptsTextInput()) {
                this.platform.textInput().start(next.bounds());
            }
            next.fire(Focusable.FOCUS_GAINED, new FocusEventImpl(next.facade(),
                    previous == null ? null : previous.facade()));
            for (Node ancestor = next.parentNode(); ancestor != null && this.focused == next; ancestor = ancestor.parentNode()) {
                ancestor.hooks().descendantFocused(next.facade());
            }
        }
    }

    // ------------------------------------------------------------------
    // Overlays
    // ------------------------------------------------------------------

    private Layer open(final Component<?> content, final Rect anchor, final boolean modal) {
        return this.open(content, anchor, modal, -1L);
    }

    private Layer open(final Component<?> content, final Rect anchor, final boolean modal, final long expiresAt) {
        final Node node = this.ui.nodeOf(content);
        if (node.parentNode() != null || node.root() != null) {
            throw new IllegalArgumentException(content + " is already part of a tree");
        }
        final Layer layer = new Layer(node, anchor, modal, expiresAt);
        // Remember focus before the dialog makes it unfocusable
        layer.previousFocus = modal ? this.focused : null;
        this.attachTree(node);
        this.layers.add(layer);
        this.treeChanged();
        this.requestLayout();
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
        final long expiresAt;
        final EventDispatcher handlers;
        Node previousFocus;
        boolean open;

        Layer(final Node node, final Rect anchor, final boolean modal, final long expiresAt) {
            this.node = node;
            this.anchor = anchor;
            this.modal = modal;
            this.expiresAt = expiresAt;
            this.handlers = new EventDispatcher();
            this.open = true;
        }

        void layout(final Rect area) {
            final LayoutPass pass = Root.this.layoutPass;
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
            return this.expiresAt >= 0L;
        }

        /**
         * Places a toast at its desired size against the right edge, with its top at {@code y}.
         *
         * @return where the next toast's top goes
         */
        float layoutToast(final Rect area, final float y) {
            final LayoutPass pass = Root.this.layoutPass;
            this.node.measure(pass, area.width(), area.height());
            final float toastWidth = this.node.outerWidth();
            final float toastHeight = this.node.outerHeight();
            final float x = Math.max(0f, area.width() - Overlays.TOAST_MARGIN - toastWidth);
            this.node.arrange(pass, new Rect(x, y, toastWidth, toastHeight));
            return y + toastHeight + Overlays.TOAST_MARGIN;
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
        public Overlay close() {
            if (!this.open) {
                return this;
            }
            this.open = false;
            Root.this.layers.remove(this);
            Root.this.detachTree(this.node);
            Root.this.requestLayout();
            if (this.modal && this.previousFocus != null && Root.this.canFocus(this.previousFocus)) {
                Root.this.changeFocus(this.previousFocus);
            }
            if (this.handlers.hasHandlers(Overlay.CLOSED)) {
                this.handlers.fire(Overlay.CLOSED, new ControlEventImpl<>(this.node.facade(), this));
            }
            this.handlers.clear();
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
            final Layer layer = Root.this.open(content, null, false, Root.this.platform.clock().millis() + millis);
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
