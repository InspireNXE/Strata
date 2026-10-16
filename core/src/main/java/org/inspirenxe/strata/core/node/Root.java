package org.inspirenxe.strata.core.node;

import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.event.UIEvents;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.input.ScrollKind;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.ThemeException;
import org.inspirenxe.strata.core.event.CharEventImpl;
import org.inspirenxe.strata.core.event.DragEventImpl;
import org.inspirenxe.strata.core.event.FocusEventImpl;
import org.inspirenxe.strata.core.event.KeyEventImpl;
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
import java.util.Set;
import java.util.function.Consumer;

/**
 * Owns one node tree: layout, painting, and the hover, press, drag, click-count and focus state machines. A host
 * drives it; input methods return true when Strata consumed the input.
 */
public final class Root {
    public static final float DRAG_THRESHOLD = 4f;

    private final Platform platform;
    private final LayoutPass layoutPass;
    private final CanvasPainter painter;
    private final ClickCounter clickCounter;
    private final KeyRepeater keyRepeater;
    private final Map<MouseButton, Press> presses;

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

    public Root(final Platform platform, final Theme theme, final CoreUi ui) {
        this.platform = Objects.requireNonNull(platform, "platform");
        this.theme = Objects.requireNonNull(theme, "theme");
        this.layoutPass = new LayoutPass(ui, platform.textMetrics());
        this.painter = new CanvasPainter();
        this.clickCounter = new ClickCounter();
        this.keyRepeater = new KeyRepeater();
        this.presses = new EnumMap<>(MouseButton.class);
        this.layoutRequested = true;
    }

    public Theme theme() {
        return this.theme;
    }

    public Node content() {
        return this.content;
    }

    // ------------------------------------------------------------------
    // Tree
    // ------------------------------------------------------------------

    /**
     * Replaces the tree. The theme is checked against every node first, so a bad theme fails before anything
     * changes.
     */
    public void setContent(final Node content) {
        if (content != null) {
            if (content.parentNode() != null || content.root() != null) {
                throw new IllegalArgumentException(content.facade() + " is already part of another tree");
            }
            this.validateTheme(this.theme, content);
        }
        if (this.content != null) {
            this.detachTree(this.content);
        }
        this.content = content;
        if (content != null) {
            this.attachTree(content);
        }
        this.requestLayout();
    }

    public void setTheme(final Theme theme) {
        Objects.requireNonNull(theme, "theme");
        if (this.content != null) {
            this.validateTheme(theme, this.content);
        }
        this.theme = theme;
        if (this.content != null) {
            visit(this.content, node -> node.applyTheme(theme));
        }
        this.requestLayout();
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
            this.presses.values().removeIf(press -> press.target == node);
            node.detach();
        });
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
            for (final Node child : container.childNodes()) {
                visit(child, action);
            }
        }
    }

    // ------------------------------------------------------------------
    // Layout and rendering
    // ------------------------------------------------------------------

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

    public void layout() {
        if (!this.layoutRequested || this.content == null) {
            return;
        }
        this.layoutRequested = false;
        this.content.measure(this.layoutPass, this.width, this.height);
        this.content.arrange(this.layoutPass, new Rect(0, 0, this.width, this.height));
    }

    public void render(final Canvas canvas) {
        this.pumpKeyRepeat();
        this.layout();
        if (this.content == null) {
            return;
        }
        this.content.paint(this.painter.begin(canvas));
        this.requestHoverCursor(canvas);
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

    public boolean pointerMoved(final float x, final float y, final Modifiers modifiers) {
        this.pointerX = x;
        this.pointerY = y;

        boolean dragging = false;
        for (final Map.Entry<MouseButton, Press> entry : this.presses.entrySet()) {
            final Press press = entry.getValue();
            final Node target = press.target;
            if (!press.dragging && (Math.abs(x - press.startX) > DRAG_THRESHOLD || Math.abs(y - press.startY) > DRAG_THRESHOLD)) {
                press.dragging = true;
                target.fire(UIEvents.DRAG_START, new DragEventImpl(target.facade(), press.startX, press.startY, modifiers,
                        entry.getKey(), press.clicks, press.startX, press.startY, 0f, 0f));
            }
            if (press.dragging && target.root() == this) {
                target.fire(UIEvents.DRAG, new DragEventImpl(target.facade(), x, y, modifiers, entry.getKey(),
                        press.clicks, press.startX, press.startY, x - press.lastX, y - press.lastY));
                dragging = true;
            }
            press.lastX = x;
            press.lastY = y;
        }

        this.updateHover(x, y, modifiers);
        if (this.hovered != null) {
            this.hovered.fire(UIEvents.POINTER_MOVE, new PointerEventImpl(this.hovered.facade(), x, y, modifiers));
        }
        return dragging || this.hovered != null;
    }

    public boolean pointerButton(final float x, final float y, final MouseButton button, final boolean pressed,
                                 final Modifiers modifiers) {
        return pressed ? this.pointerPressed(x, y, button, modifiers) : this.pointerReleased(x, y, button, modifiers);
    }

    private boolean pointerPressed(final float x, final float y, final MouseButton button, final Modifiers modifiers) {
        this.pointerX = x;
        this.pointerY = y;
        this.updateHover(x, y, modifiers);

        final Node target = this.hovered;
        if (target == null) {
            this.changeFocus(null);
            return false;
        }

        this.changeFocus(this.focusableAncestor(target));

        final int clicks = this.clickCounter.press(target, button, x, y, this.platform.clock().millis());
        this.presses.put(button, new Press(target, clicks, x, y));
        target.setPressed(true);
        target.fire(UIEvents.POINTER_PRESS, new PointerButtonEventImpl(target.facade(), x, y, modifiers, button, clicks));
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

        target.fire(UIEvents.POINTER_RELEASE, new PointerButtonEventImpl(target.facade(), x, y, modifiers, button, press.clicks));
        if (press.dragging) {
            target.fire(UIEvents.DRAG_END, new DragEventImpl(target.facade(), x, y, modifiers, button, press.clicks,
                    press.startX, press.startY, x - press.lastX, y - press.lastY));
        } else if (target.isEnabled() && target.bounds().contains(x, y)) {
            target.fire(UIEvents.CLICK, new PointerButtonEventImpl(target.facade(), x, y, modifiers, button, press.clicks));
        }
        this.updateHover(x, y, modifiers);
        return true;
    }

    public boolean scroll(final float x, final float y, final float scrollX, final float scrollY,
                          final ScrollKind kind, final Modifiers modifiers) {
        this.updateHover(x, y, modifiers);
        if (this.hovered == null) {
            return false;
        }
        return this.hovered.fire(UIEvents.SCROLL,
                new ScrollEventImpl(this.hovered.facade(), x, y, modifiers, scrollX, scrollY, kind)).isHandled();
    }

    public void pointerLeft() {
        if (this.hovered != null) {
            final Node old = this.hovered;
            this.hovered = null;
            old.setHovered(false);
            old.fire(UIEvents.POINTER_EXIT, new PointerEventImpl(old.facade(), this.pointerX, this.pointerY, Modifiers.NONE));
        }
    }

    public Node hovered() {
        return this.hovered;
    }

    private void updateHover(final float x, final float y, final Modifiers modifiers) {
        this.layout();
        final Node next = this.content == null ? null : this.content.hitTest(x, y);
        if (next == this.hovered) {
            return;
        }
        final Node previous = this.hovered;
        this.hovered = next;
        if (previous != null) {
            previous.setHovered(false);
            previous.fire(UIEvents.POINTER_EXIT, new PointerEventImpl(previous.facade(), x, y, modifiers));
        }
        if (next != null) {
            next.setHovered(true);
            next.fire(UIEvents.POINTER_ENTER, new PointerEventImpl(next.facade(), x, y, modifiers));
        }
    }

    // ------------------------------------------------------------------
    // Keyboard
    // ------------------------------------------------------------------

    public boolean key(final Key key, final KeyAction action, final Modifiers modifiers) {
        if (!this.platform.capabilities().sendsKeyRepeat()) {
            this.keyRepeater.onKey(key, action, modifiers, this.platform.clock().millis());
        }
        return this.dispatchKey(key, action, modifiers);
    }

    private boolean dispatchKey(final Key key, final KeyAction action, final Modifiers modifiers) {
        final Node target = this.focused != null ? this.focused : this.content;
        if (target == null) {
            return false;
        }
        if (target.fire(UIEvents.KEY, new KeyEventImpl(target.facade(), key, action, modifiers)).isHandled()) {
            return true;
        }
        if (key == Key.TAB && action.isDown() && !modifiers.control() && !modifiers.alt()) {
            return this.moveFocus(modifiers.shift());
        }
        return false;
    }

    public boolean charTyped(final int codePoint, final Modifiers modifiers) {
        if (this.focused == null) {
            return false;
        }
        return this.focused.fire(UIEvents.CHAR, new CharEventImpl(this.focused.facade(), codePoint, modifiers)).isHandled();
    }

    // ------------------------------------------------------------------
    // Focus
    // ------------------------------------------------------------------

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

    public void clearFocus() {
        this.changeFocus(null);
    }

    /**
     * Moves focus to the next (or previous) tabbable node, wrapping at the ends.
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
     * Visible, enabled, focusable and tabbable nodes, by tab index and then tree order. Cached until the tree
     * changes.
     */
    public List<Node> tabOrder() {
        if (this.tabOrder == null) {
            final List<Node> order = new ArrayList<>();
            if (this.content != null) {
                this.collectTabbable(this.content, order);
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
        return true;
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
            previous.fire(UIEvents.FOCUS_LOST, new FocusEventImpl(previous.facade(), next == null ? null : next.facade()));
        }
        if (next != null) {
            next.setFocused(true);
            if (next.acceptsTextInput()) {
                this.platform.textInput().start(next.bounds());
            }
            next.fire(UIEvents.FOCUS_GAINED, new FocusEventImpl(next.facade(), previous == null ? null : previous.facade()));
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
