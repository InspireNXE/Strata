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
package org.inspirenxe.strata.core.control.window;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.control.window.Window;
import org.inspirenxe.strata.api.event.input.DragEvent;
import org.inspirenxe.strata.api.event.input.KeyEvent;
import org.inspirenxe.strata.api.event.input.PointerButtonEvent;
import org.inspirenxe.strata.api.event.input.PointerEvent;
import org.inspirenxe.strata.api.extension.ArrangeContext;
import org.inspirenxe.strata.api.extension.CustomContainer;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.overlay.Overlay;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;
import org.inspirenxe.strata.core.node.Node;
import org.inspirenxe.strata.core.text.TextLine;

import java.util.Objects;
import java.util.Set;

/**
 * The core {@link Window}. The close button is an {@code x} at the right end of the title bar, {@link #CLOSE_WIDTH}
 * units wide and as tall as the bar. Dragging keeps the window's bounds inside the slot its parent last gave it. The
 * window asks for its largest child plus padding and the title bar, and at least the title's width plus room for
 * the close button.
 *
 * <p>A press on the close button closes; otherwise a press on a part that resizes starts a resize, and a press
 * elsewhere on the title bar starts a move. A resize works from the bounds and pointer position at the press, so
 * each move of the drag works out the size afresh rather than adding up steps. The grip's six squares are placed
 * during arrange, so painting allocates nothing.</p>
 */
public final class WindowImpl extends CustomContainer<Window> implements Window {
    /**
     * The width of the close button's area, in UI units.
     */
    public static final float CLOSE_WIDTH = 12f;
    private static final Set<String> REQUIRED = Set.of(STYLE_BACKGROUND);
    private static final int LEFT = 1;
    private static final int TOP = 2;
    private static final int RIGHT = 4;
    private static final int BOTTOM = 8;

    private final ComponentFactory ui;
    private Text title;
    private boolean closable;
    private boolean movable;
    private boolean resizable;
    private boolean edgeResizable;

    // Pointer
    private boolean closePressed;
    private boolean closeHovered;
    private boolean dragging;
    private float dragOffsetX;
    private float dragOffsetY;
    private int resizeEdges;
    private Rect resizeStart;
    private Size size;
    private boolean gripHovered;
    private boolean resizeCursor;

    // Theme
    private Paint background;
    private Paint titleBar;
    private TextStyle titleStyle;
    private TextStyle closeStyle;
    private TextStyle closeHoveredStyle;
    private Paint gripPaint;
    private Paint gripHoveredPaint;

    // Computed during measure
    private float titleHeight;
    private TextLine titleSpan;
    private float titleWidth;
    private TextSpan closeSpan;
    private TextSpan closeHoveredSpan;
    private float closeWidth;
    private float titleRoom;

    // Placed during arrange
    private final Rect[] gripDots;

    /**
     * Runs in place of {@link #close()} once the user's {@link #CLOSE} request was accepted, for a control that holds
     * this window privately, or {@code null} to close at once.
     */
    private Runnable closeHook;

    /**
     * Creates an empty, centered window with 3 units of padding.
     *
     * @param ui the factory that owns the window
     * @param title the title
     */
    @SuppressWarnings("this-escape")
    public WindowImpl(final ComponentFactory ui, final Text title) {
        super(ui);
        this.ui = ui;
        this.title = Objects.requireNonNull(title, "title");
        this.closable = true;
        this.movable = true;
        this.background = Paint.NONE;
        this.titleBar = Paint.NONE;
        this.titleStyle = TextStyle.DEFAULT;
        this.closeStyle = TextStyle.DEFAULT;
        this.closeHoveredStyle = TextStyle.DEFAULT;
        this.gripPaint = Paint.NONE;
        this.gripHoveredPaint = Paint.NONE;
        this.gripDots = new Rect[6];
        this.titleSpan = TextLine.EMPTY;
        this.closeSpan = TextSpan.of("x");
        this.closeHoveredSpan = this.closeSpan;
        this.setPadding(3f);
        this.setAnchor(Anchor.CENTER);
    }

    // ------------------------------------------------------------------
    // Window
    // ------------------------------------------------------------------

    @Override
    public Text title() {
        return this.title;
    }

    @Override
    public Window setTitle(final Text title) {
        this.title = Objects.requireNonNull(title, "title");
        this.invalidateLayout();
        return this;
    }

    @Override
    public boolean isClosable() {
        return this.closable;
    }

    @Override
    public Window setClosable(final boolean closable) {
        this.closable = closable;
        this.invalidateLayout();
        return this;
    }

    @Override
    public boolean isMovable() {
        return this.movable;
    }

    @Override
    public Window setMovable(final boolean movable) {
        this.movable = movable;
        return this;
    }

    @Override
    public boolean isResizable() {
        return this.resizable;
    }

    @Override
    public Window setResizable(final boolean resizable) {
        this.resizable = resizable;
        if (!resizable) {
            this.endResize();
        }
        return this;
    }

    @Override
    public boolean isEdgeResizable() {
        return this.edgeResizable;
    }

    @Override
    public Window setEdgeResizable(final boolean edgeResizable) {
        this.edgeResizable = edgeResizable;
        return this;
    }

    @Override
    public Window close() {
        this.report(CLOSED);
        if (this.isAttached()) {
            this.ui.overlays().of(this).ifPresent(Overlay::close);
        }
        return this;
    }

    /**
     * Sets what a close by the user does once its {@link #CLOSE} request was accepted, in place of {@link #close()}.
     * A control that holds this window privately uses it to ask its own question first, and calls {@link #close()}
     * itself when the window should close.
     *
     * @param hook what to run, or {@code null} to close at once
     */
    void setUserCloseHook(final Runnable hook) {
        this.closeHook = hook;
    }

    /**
     * Closes the window for the user: fires a {@link #CLOSE} request and, unless a handler canceled it, runs the hook
     * set by {@link #setUserCloseHook} or {@link #close()}.
     */
    private void userClose() {
        if (!this.request(CLOSE)) {
            return;
        }
        if (this.closeHook != null) {
            this.closeHook.run();
        } else {
            this.close();
        }
    }

    private Rect titleBarRect(final Rect bounds) {
        return new Rect(bounds.x(), bounds.y(), bounds.width(), this.titleHeight);
    }

    private Rect closeRect(final Rect bounds) {
        return new Rect(bounds.right() - CLOSE_WIDTH - 1f, bounds.y(), CLOSE_WIDTH, this.titleHeight);
    }

    /**
     * Returns the edges a press at a point would resize, as a mask of {@link #LEFT}, {@link #TOP}, {@link #RIGHT} and
     * {@link #BOTTOM}: the right and bottom for the grip, and with edge resizing each edge within {@link #EDGE_SIZE}
     * of the point, plus the next edge where the point is within {@link #GRIP_SIZE} of it.
     */
    private int edgesAt(final float x, final float y) {
        final Rect bounds = this.bounds();
        if (!this.resizable || !bounds.contains(x, y)) {
            return 0;
        }
        if (x >= bounds.right() - GRIP_SIZE && y >= bounds.bottom() - GRIP_SIZE) {
            return RIGHT | BOTTOM;
        }
        if (!this.edgeResizable) {
            return 0;
        }
        int edges = 0;
        if (x < bounds.x() + EDGE_SIZE || x >= bounds.right() - EDGE_SIZE) {
            edges |= x < bounds.x() + EDGE_SIZE ? LEFT : RIGHT;
            if (y < bounds.y() + GRIP_SIZE) {
                edges |= TOP;
            } else if (y >= bounds.bottom() - GRIP_SIZE) {
                edges |= BOTTOM;
            }
        }
        if (y < bounds.y() + EDGE_SIZE || y >= bounds.bottom() - EDGE_SIZE) {
            edges |= y < bounds.y() + EDGE_SIZE ? TOP : BOTTOM;
            if (x < bounds.x() + GRIP_SIZE) {
                edges |= LEFT;
            } else if (x >= bounds.right() - GRIP_SIZE) {
                edges |= RIGHT;
            }
        }
        return edges;
    }

    private static CursorKind cursorFor(final int edges) {
        final boolean horizontal = (edges & (LEFT | RIGHT)) != 0;
        final boolean vertical = (edges & (TOP | BOTTOM)) != 0;
        if (horizontal && vertical) {
            return CursorKind.RESIZE_ALL;
        }
        if (horizontal) {
            return CursorKind.RESIZE_HORIZONTAL;
        }
        return vertical ? CursorKind.RESIZE_VERTICAL : null;
    }

    /**
     * Shows the resize cursor and the hovered grip for the edges under the pointer, or clears the cursor this window
     * set when there are none. Keeps both during a resize.
     */
    private void hoverEdges(final int edges) {
        final int shown = this.resizeStart != null ? this.resizeEdges : edges;
        this.gripHovered = shown != 0;
        if (shown != 0) {
            this.setCursor(cursorFor(shown));
            this.resizeCursor = true;
        } else if (this.resizeCursor) {
            this.setCursor(null);
            this.resizeCursor = false;
        }
    }

    private void endResize() {
        this.resizeStart = null;
        this.resizeEdges = 0;
    }

    /**
     * Resizes for a move of the resize drag: works out the size from the bounds at the press and how far the pointer
     * moved, asks {@link #RESIZE} if it differs from the current size, and applies the final value.
     */
    private void resizeBy(final float dx, final float dy) {
        final Rect start = this.resizeStart;
        float width = start.width();
        float height = start.height();
        if ((this.resizeEdges & LEFT) != 0) {
            width -= dx;
        } else if ((this.resizeEdges & RIGHT) != 0) {
            width += dx;
        }
        if ((this.resizeEdges & TOP) != 0) {
            height -= dy;
        } else if ((this.resizeEdges & BOTTOM) != 0) {
            height += dy;
        }
        final Size proposed = this.limit(width, height);
        if (proposed.equals(this.size)) {
            return;
        }
        this.request(RESIZE, proposed, this::applyResize);
    }

    /**
     * Applies the final size of a {@link #RESIZE}, kept within the limits, with the edges opposite the dragged ones
     * where they were at the press. Null and NaN are ignored.
     */
    private void applyResize(final Size requested) {
        if (requested == null || Float.isNaN(requested.width()) || Float.isNaN(requested.height())
                || this.resizeStart == null) {
            return;
        }
        final Size next = this.limit(requested.width(), requested.height());
        final Size previous = this.size;
        if (next.equals(previous)) {
            return;
        }
        final Rect start = this.resizeStart;
        final float x = (this.resizeEdges & LEFT) != 0 ? start.right() - next.width() : start.x();
        final float y = (this.resizeEdges & TOP) != 0 ? start.bottom() - next.height() : start.y();
        // Where layout will put a window of the new size before its offset, so the offset can land it at x, y
        final Rect inner = this.slotInner();
        final float baseX = switch (this.horizontalAlignment()) {
            case LEFT, STRETCH -> inner.x();
            case CENTER -> inner.x() + (inner.width() - next.width()) * 0.5f;
            case RIGHT -> inner.right() - next.width();
        };
        final float baseY = switch (this.verticalAlignment()) {
            case TOP, STRETCH -> inner.y();
            case MIDDLE -> inner.y() + (inner.height() - next.height()) * 0.5f;
            case BOTTOM -> inner.bottom() - next.height();
        };
        this.size = next;
        this.setSize(next.width(), next.height());
        this.setOffset(x - baseX, y - baseY);
        this.report(RESIZED, next, previous);
    }

    /**
     * Keeps a size within the window's minimum and maximum, the room in its slot on the sides being dragged, and the
     * floor of the title bar's width and the bar's height plus the grip. An axis no dragged edge touches keeps its
     * size at the press. The minimum wins over the room.
     */
    private Size limit(final float width, final float height) {
        final Rect start = this.resizeStart;
        final Rect inner = this.slotInner();
        final float roomWidth = (this.resizeEdges & LEFT) != 0 ? start.right() - inner.x() : inner.right() - start.x();
        final float roomHeight = (this.resizeEdges & TOP) != 0 ? start.bottom() - inner.y() : inner.bottom() - start.y();
        final float minWidth = Math.max(this.minWidth(), this.titleRoom);
        final float minHeight = Math.max(this.minHeight(), this.titleHeight + GRIP_SIZE);
        final float w = (this.resizeEdges & (LEFT | RIGHT)) == 0
                ? start.width()
                : Math.max(minWidth, Math.min(width, Math.min(this.maxWidth(), roomWidth)));
        final float h = (this.resizeEdges & (TOP | BOTTOM)) == 0
                ? start.height()
                : Math.max(minHeight, Math.min(height, Math.min(this.maxHeight(), roomHeight)));
        return new Size(w, h);
    }

    private Rect slotInner() {
        return ((ComponentFactoryImpl) this.ui).nodeOf(this).slot().shrink(this.margin());
    }

    // ------------------------------------------------------------------
    // Theme, layout and painting
    // ------------------------------------------------------------------

    @Override
    protected Set<String> requiredStyleKeys() {
        return REQUIRED;
    }

    @Override
    protected void onApplyTheme(final Theme theme) {
        this.background = theme.paintOr(STYLE_BACKGROUND, Paint.NONE);
        this.titleBar = theme.paintOr(STYLE_TITLE_BAR, Paint.NONE);
        this.titleStyle = theme.textStyleOr(STYLE_TITLE_TEXT, TextStyle.DEFAULT);
        this.closeStyle = theme.textStyleOr(STYLE_CLOSE, this.titleStyle);
        this.closeHoveredStyle = theme.textStyleOr(STYLE_CLOSE_HOVERED, this.closeStyle);
        this.gripPaint = theme.paintOr(STYLE_GRIP, Paint.NONE);
        this.gripHoveredPaint = theme.paintOr(STYLE_GRIP_HOVERED, this.gripPaint);
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        final TextMetrics metrics = context.text();
        this.titleSpan = TextLine.of(this.title, this.titleStyle, metrics);
        this.titleWidth = this.titleSpan.width();
        this.titleHeight = this.titleSpan.height() + TITLE_PADDING * 2f;
        this.closeSpan = new TextSpan("x", this.closeStyle);
        this.closeHoveredSpan = new TextSpan("x", this.closeHoveredStyle);
        this.closeWidth = metrics.width(this.closeSpan);

        final Insets padding = this.padding();
        final float innerWidth = availableWidth - padding.horizontal();
        final float innerHeight = availableHeight - padding.vertical() - this.titleHeight;
        float width = 0f;
        float height = 0f;
        for (final Component<?> child : this.children()) {
            final Size outer = context.measure(child, innerWidth, innerHeight);
            width = Math.max(width, outer.width());
            height = Math.max(height, outer.height());
        }
        this.titleRoom = this.titleWidth + (this.closable ? (CLOSE_WIDTH + 1f) * 2f : 0f) + 8f;
        return new Size(Math.max(width + padding.horizontal(), this.titleRoom), height + padding.vertical() + this.titleHeight);
    }

    @Override
    protected void arrange(final ArrangeContext context, final Rect bounds) {
        final Rect area = new Rect(bounds.x(), bounds.y() + this.titleHeight, bounds.width(),
                Math.max(0f, bounds.height() - this.titleHeight)).shrink(this.padding());
        for (final Component<?> child : this.children()) {
            context.arrange(child, area);
        }
        // Six squares in a triangle against the corner, one unit in from the edges
        final float originX = bounds.right() - 9f;
        final float originY = bounds.bottom() - 9f;
        int dot = 0;
        for (int row = 0; row < 3; row++) {
            for (int column = 2 - row; column < 3; column++) {
                this.gripDots[dot++] = new Rect(originX + column * 3f, originY + row * 3f, 2f, 2f);
            }
        }
    }

    @Override
    protected Rect childClip() {
        final Rect bounds = this.bounds();
        return new Rect(bounds.x(), bounds.y() + this.titleHeight, bounds.width(), Math.max(0f, bounds.height() - this.titleHeight));
    }

    @Override
    protected void paintOverChildren(final Painter painter, final Rect bounds) {
        if (!this.resizable || this.gripDots[0] == null) {
            return;
        }
        // A pointer that left from a child over the grip sends the window no exit, so check that it is still inside
        final boolean hovered = this.resizeStart != null || this.gripHovered && this.isHoverWithin();
        final Paint paint = hovered ? this.gripHoveredPaint : this.gripPaint;
        for (final Rect dot : this.gripDots) {
            paint.paint(painter, dot);
        }
    }

    @Override
    protected void paint(final Painter painter, final Rect bounds) {
        this.background.paint(painter, bounds);
        final Rect bar = this.titleBarRect(bounds);
        this.titleBar.paint(painter, bar);
        final float textY = bounds.y() + TITLE_PADDING;
        this.titleSpan.draw(painter, bounds.x() + (bounds.width() - this.titleWidth) * 0.5f, textY);
        if (this.closable) {
            final Rect close = this.closeRect(bounds);
            painter.text(this.closeHovered ? this.closeHoveredSpan : this.closeSpan,
                    close.x() + (close.width() - this.closeWidth) * 0.5f, textY);
        }
    }

    // ------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------

    @Override
    protected void onPointerPress(final PointerButtonEvent event) {
        this.closePressed = false;
        this.dragging = false;
        this.endResize();
        if (event.button() != MouseButton.LEFT) {
            return;
        }
        final Rect bounds = this.bounds();
        final boolean onClose = this.closable && this.closeRect(bounds).contains(event.x(), event.y());
        final int edges = onClose ? 0 : this.edgesAt(event.x(), event.y());
        if (edges != 0) {
            this.resizeEdges = edges;
            this.resizeStart = bounds;
            this.size = new Size(bounds.width(), bounds.height());
            this.hoverEdges(edges);
            event.consume();
            return;
        }
        if (!this.titleBarRect(bounds).contains(event.x(), event.y())) {
            return;
        }
        if (onClose) {
            this.closePressed = true;
        } else if (this.movable) {
            this.dragging = true;
            this.dragOffsetX = this.offsetX();
            this.dragOffsetY = this.offsetY();
        }
        event.consume();
    }

    @Override
    protected void onClick(final PointerButtonEvent event) {
        if (this.closePressed && this.closeRect(this.bounds()).contains(event.x(), event.y())) {
            this.closePressed = false;
            event.consume();
            this.userClose();
        }
    }

    @Override
    protected void onDragStart(final DragEvent event) {
        this.onDrag(event);
    }

    @Override
    protected void onDrag(final DragEvent event) {
        if (this.resizeStart != null && event.button() == MouseButton.LEFT) {
            this.resizeBy(event.x() - event.startX(), event.y() - event.startY());
            event.consume();
            return;
        }
        if (!this.dragging) {
            return;
        }
        final Node node = ((ComponentFactoryImpl) this.ui).nodeOf(this);
        final Rect slot = node.slot();
        final Rect bounds = this.bounds();
        final float baseX = bounds.x() - this.offsetX();
        final float baseY = bounds.y() - this.offsetY();
        float x = this.dragOffsetX + event.x() - event.startX();
        float y = this.dragOffsetY + event.y() - event.startY();
        x = Math.max(slot.x() - baseX, Math.min(x, slot.right() - bounds.width() - baseX));
        y = Math.max(slot.y() - baseY, Math.min(y, slot.bottom() - bounds.height() - baseY));
        this.setOffset(x, y);
        event.consume();
    }

    @Override
    protected void onDragEnd(final DragEvent event) {
        this.dragging = false;
        if (this.resizeStart != null) {
            this.endResize();
            this.hoverEdges(this.edgesAt(event.x(), event.y()));
        }
    }

    @Override
    protected void onPointerRelease(final PointerButtonEvent event) {
        if (this.resizeStart != null && event.button() == MouseButton.LEFT) {
            this.endResize();
            this.hoverEdges(this.edgesAt(event.x(), event.y()));
        }
    }

    @Override
    protected void onPointerMove(final PointerEvent event) {
        this.closeHovered = event.target() == this && this.closable
                && this.closeRect(this.bounds()).contains(event.x(), event.y());
        if (this.resizable || this.resizeCursor) {
            this.hoverEdges(this.closeHovered ? 0 : this.edgesAt(event.x(), event.y()));
        }
    }

    @Override
    protected void onPointerExit(final PointerEvent event) {
        this.closeHovered = false;
        if (this.resizeStart == null) {
            this.hoverEdges(0);
        }
    }

    @Override
    protected void onKey(final KeyEvent event) {
        this.escape(event);
    }

    /**
     * Closes the window for the user on an Escape press while it is closable, if a handler for {@link #CLOSE} or
     * {@link #CLOSED} is on its path or it is shown in an overlay, and consumes the press. Ignores every other key
     * event. A control that holds this window privately calls this for Escape presses that reach the control without
     * passing through the window.
     *
     * @param event the key event
     */
    void escape(final KeyEvent event) {
        if (event.key() != Key.ESCAPE || event.action() != KeyAction.PRESS || !this.closable) {
            return;
        }
        // A screen's own window that nobody listens to leaves Escape to the engine, which closes the screen
        if (this.isHandled(CLOSE) || this.isHandled(CLOSED)
                || this.isAttached() && this.ui.overlays().of(this).isPresent()) {
            event.consume();
            this.userClose();
        }
    }

    @Override
    public float offsetX() {
        return super.offsetX();
    }

    @Override
    public float offsetY() {
        return super.offsetY();
    }

    @Override
    public Window setOffset(final float offsetX, final float offsetY) {
        return super.setOffset(offsetX, offsetY);
    }
}
