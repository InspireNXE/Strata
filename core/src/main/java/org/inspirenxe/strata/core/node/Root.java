package org.inspirenxe.strata.core.node;

import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.event.EventTypes;
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
 * Owns one screen's node tree: layout, painting, and the hover, press, drag, click-count and focus state machines.
 * A {@code ScreenHostImpl} drives it. The input methods return true when Strata used the input, so the host can
 * pass unused input back to the engine.
 *
 * <p>Pointer rules: hover goes to the deepest visible, enabled node under the pointer. A press focuses the pressed
 * node or its nearest focusable ancestor and clears focus if there is none. Each button is tracked on its own. A
 * drag starts once the pointer moves more than {@link #DRAG_THRESHOLD} units from the press on either axis, and its
 * events go to the pressed node wherever the pointer is. A release fires a click only when it ends no drag and lands
 * inside the pressed node, which must still be enabled.</p>
 *
 * <p>Keyboard rules: key events go to the focused node, or the content when nothing has focus. An unhandled Tab
 * press or repeat without Ctrl or Alt moves focus, backwards with Shift. Typed characters go only to the focused
 * node. When the platform does not send key repeats, the root makes them with a {@link KeyRepeater}; each repeat
 * goes to whatever has focus when it fires.</p>
 *
 * <p>Every method must be called on the UI thread.</p>
 */
public final class Root {
    /**
     * How far, in UI units along either axis, the pointer must move with a button held before a drag starts.
     */
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

    /**
     * Creates an empty root of size 0 by 0.
     *
     * @param platform the host's platform, for text metrics, time, text input and capabilities
     * @param theme the starting theme
     * @param ui the host's factory
     * @throws NullPointerException if {@code platform} or {@code theme} is null
     */
    public Root(final Platform platform, final Theme theme, final ComponentFactoryImpl ui) {
        this.platform = Objects.requireNonNull(platform, "platform");
        this.theme = Objects.requireNonNull(theme, "theme");
        this.layoutPass = new LayoutPass(ui, platform.textMetrics());
        this.painter = new CanvasPainter();
        this.clickCounter = new ClickCounter();
        this.keyRepeater = new KeyRepeater();
        this.presses = new EnumMap<>(MouseButton.class);
        this.layoutRequested = true;
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

    // ------------------------------------------------------------------
    // Tree
    // ------------------------------------------------------------------

    /**
     * Replaces the tree. The theme is checked against every node of the new tree first, so a theme that lacks a
     * required key fails before anything changes. The old tree is detached, which clears its hover, press and
     * focus state.
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
     * Changes the theme and applies it to every node. Nothing changes if the new theme lacks a key the tree
     * requires.
     *
     * @param theme the new theme
     * @throws NullPointerException if {@code theme} is null
     * @throws ThemeException if the theme lacks a style key the tree requires
     */
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

    /**
     * Lays out the whole tree if anything requested it since the last layout: the content is measured with the
     * root's size and arranged into it. Rendering and pointer input call this first, so bounds are never stale.
     */
    public void layout() {
        if (!this.layoutRequested || this.content == null) {
            return;
        }
        this.layoutRequested = false;
        this.content.measure(this.layoutPass, this.width, this.height);
        this.content.arrange(this.layoutPass, new Rect(0, 0, this.width, this.height));
    }

    /**
     * Draws one frame: fires a key repeat if one is due, lays out if needed, paints the tree, and asks the canvas
     * for the cursor of the hovered node (or its nearest ancestor that sets one, falling back to the arrow).
     *
     * @param canvas the frame's canvas
     */
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

    /**
     * Handles a pointer move: starts or continues drags, updates hover, and fires a move event at the hovered node.
     *
     * @param x the pointer's x, in UI units
     * @param y the pointer's y, in UI units
     * @param modifiers the modifiers held
     * @return true if a drag is in progress or the pointer is over a node
     */
    public boolean pointerMoved(final float x, final float y, final Modifiers modifiers) {
        this.pointerX = x;
        this.pointerY = y;

        boolean dragging = false;
        for (final Map.Entry<MouseButton, Press> entry : this.presses.entrySet()) {
            final Press press = entry.getValue();
            final Node target = press.target;
            if (!press.dragging && (Math.abs(x - press.startX) > DRAG_THRESHOLD || Math.abs(y - press.startY) > DRAG_THRESHOLD)) {
                press.dragging = true;
                target.fire(EventTypes.DRAG_START, new DragEventImpl(target.facade(), press.startX, press.startY, modifiers,
                        entry.getKey(), press.clicks, press.startX, press.startY, 0f, 0f));
            }
            if (press.dragging && target.root() == this) {
                target.fire(EventTypes.DRAG, new DragEventImpl(target.facade(), x, y, modifiers, entry.getKey(),
                        press.clicks, press.startX, press.startY, x - press.lastX, y - press.lastY));
                dragging = true;
            }
            press.lastX = x;
            press.lastY = y;
        }

        this.updateHover(x, y, modifiers);
        if (this.hovered != null) {
            this.hovered.fire(EventTypes.POINTER_MOVE, new PointerEventImpl(this.hovered.facade(), x, y, modifiers));
        }
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
     * @return for a press, true if it hit a node; for a release, true if a press of that button was being tracked
     */
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
        target.fire(EventTypes.POINTER_PRESS, new PointerButtonEventImpl(target.facade(), x, y, modifiers, button, clicks));
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

        target.fire(EventTypes.POINTER_RELEASE, new PointerButtonEventImpl(target.facade(), x, y, modifiers, button, press.clicks));
        if (press.dragging) {
            target.fire(EventTypes.DRAG_END, new DragEventImpl(target.facade(), x, y, modifiers, button, press.clicks,
                    press.startX, press.startY, x - press.lastX, y - press.lastY));
        } else if (target.isEnabled() && target.bounds().contains(x, y)) {
            target.fire(EventTypes.CLICK, new PointerButtonEventImpl(target.facade(), x, y, modifiers, button, press.clicks));
        }
        this.updateHover(x, y, modifiers);
        return true;
    }

    /**
     * Handles a scroll by firing a scroll event at the hovered node, which bubbles until something handles it.
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
        this.updateHover(x, y, modifiers);
        if (this.hovered == null) {
            return false;
        }
        return this.hovered.fire(EventTypes.SCROLL,
                new ScrollEventImpl(this.hovered.facade(), x, y, modifiers, scrollX, scrollY, kind)).isHandled();
    }

    /**
     * Handles the pointer leaving the host's area: the hovered node gets an exit event and hover clears. Presses
     * stay tracked, so a drag can continue when the pointer comes back.
     */
    public void pointerLeft() {
        if (this.hovered != null) {
            final Node old = this.hovered;
            this.hovered = null;
            old.setHovered(false);
            old.fire(EventTypes.POINTER_EXIT, new PointerEventImpl(old.facade(), this.pointerX, this.pointerY, Modifiers.NONE));
        }
    }

    /**
     * Returns the node under the pointer.
     *
     * @return the hovered node, or {@code null} if the pointer is over nothing or outside the host
     */
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
            previous.fire(EventTypes.POINTER_EXIT, new PointerEventImpl(previous.facade(), x, y, modifiers));
        }
        if (next != null) {
            next.setHovered(true);
            next.fire(EventTypes.POINTER_ENTER, new PointerEventImpl(next.facade(), x, y, modifiers));
        }
    }

    // ------------------------------------------------------------------
    // Keyboard
    // ------------------------------------------------------------------

    /**
     * Handles a key event from the engine. See the class description for where it goes and how Tab works.
     *
     * @param key the physical key
     * @param action whether the key went down, repeated or went up
     * @param modifiers the modifiers held
     * @return true if a node handled the key or it moved focus
     */
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
        if (target.fire(EventTypes.KEY, new KeyEventImpl(target.facade(), key, action, modifiers)).isHandled()) {
            return true;
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
        return this.focused.fire(EventTypes.CHAR, new CharEventImpl(this.focused.facade(), codePoint, modifiers)).isHandled();
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
     * enabled, sorted by tab index and then by tree order. The list is cached until the tree changes.
     *
     * @return an unmodifiable list, possibly empty
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
            previous.fire(EventTypes.FOCUS_LOST, new FocusEventImpl(previous.facade(), next == null ? null : next.facade()));
        }
        if (next != null) {
            next.setFocused(true);
            if (next.acceptsTextInput()) {
                this.platform.textInput().start(next.bounds());
            }
            next.fire(EventTypes.FOCUS_GAINED, new FocusEventImpl(next.facade(), previous == null ? null : previous.facade()));
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
