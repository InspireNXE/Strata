package org.inspirenxe.strata.core.control.window;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.control.window.Window;
import org.inspirenxe.strata.api.event.DragEvent;
import org.inspirenxe.strata.api.event.KeyEvent;
import org.inspirenxe.strata.api.event.PointerButtonEvent;
import org.inspirenxe.strata.api.event.PointerEvent;
import org.inspirenxe.strata.api.extension.ArrangeContext;
import org.inspirenxe.strata.api.extension.CustomContainer;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geom.Insets;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.geom.Size;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.overlay.Overlay;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;
import org.inspirenxe.strata.core.node.Node;

import java.util.Objects;
import java.util.Set;

/**
 * The core {@link Window}. The close button is an {@code x} at the right end of the title bar, {@link #CLOSE_WIDTH}
 * units wide and as tall as the bar. Dragging keeps the window's bounds inside the slot its parent last gave it. The
 * window asks for its largest child plus padding and the title bar, and at least the title's width plus room for
 * the close button.
 */
public final class WindowImpl extends CustomContainer<Window> implements Window {
    /**
     * The width of the close button's area, in UI units.
     */
    public static final float CLOSE_WIDTH = 12f;
    private static final Set<String> REQUIRED = Set.of(STYLE_BACKGROUND);

    private final ComponentFactory ui;
    private String title;
    private boolean closable;
    private boolean movable;

    // Pointer
    private boolean closePressed;
    private boolean closeHovered;
    private boolean dragging;
    private float dragOffsetX;
    private float dragOffsetY;

    // Theme
    private Paint background;
    private Paint titleBar;
    private TextStyle titleStyle;
    private TextStyle closeStyle;
    private TextStyle closeHoveredStyle;

    // Computed during measure
    private float titleHeight;
    private TextSpan titleSpan;
    private float titleWidth;
    private TextSpan closeSpan;
    private TextSpan closeHoveredSpan;
    private float closeWidth;

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
    public WindowImpl(final ComponentFactory ui, final String title) {
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
        this.titleSpan = TextSpan.of(title);
        this.closeSpan = TextSpan.of("x");
        this.closeHoveredSpan = this.closeSpan;
        this.setPadding(Insets.of(3f));
        this.setAnchor(Anchor.CENTER);
    }

    // ------------------------------------------------------------------
    // Window
    // ------------------------------------------------------------------

    @Override
    public String title() {
        return this.title;
    }

    @Override
    public Window setTitle(final String title) {
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
    public void close() {
        this.report(CLOSED);
        if (this.isAttached()) {
            this.ui.overlays().of(this).ifPresent(Overlay::close);
        }
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
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        final TextMetrics metrics = context.text();
        this.titleSpan = new TextSpan(this.title, this.titleStyle);
        this.titleWidth = metrics.width(this.titleSpan);
        this.titleHeight = metrics.lineHeight(this.titleStyle) + TITLE_PADDING * 2f;
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
        final float titleRoom = this.titleWidth + (this.closable ? (CLOSE_WIDTH + 1f) * 2f : 0f) + 8f;
        return new Size(Math.max(width + padding.horizontal(), titleRoom), height + padding.vertical() + this.titleHeight);
    }

    @Override
    protected void arrange(final ArrangeContext context, final Rect bounds) {
        final Rect area = new Rect(bounds.x(), bounds.y() + this.titleHeight, bounds.width(),
                Math.max(0f, bounds.height() - this.titleHeight)).shrink(this.padding());
        for (final Component<?> child : this.children()) {
            context.arrange(child, area);
        }
    }

    @Override
    protected Rect childClip() {
        final Rect bounds = this.bounds();
        return new Rect(bounds.x(), bounds.y() + this.titleHeight, bounds.width(), Math.max(0f, bounds.height() - this.titleHeight));
    }

    @Override
    protected void paint(final Painter painter, final Rect bounds) {
        this.background.paint(painter, bounds);
        final Rect bar = this.titleBarRect(bounds);
        this.titleBar.paint(painter, bar);
        final float textY = bounds.y() + TITLE_PADDING;
        painter.text(this.titleSpan, bounds.x() + (bounds.width() - this.titleWidth) * 0.5f, textY);
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
        if (event.button() != MouseButton.LEFT || !this.titleBarRect(this.bounds()).contains(event.x(), event.y())) {
            return;
        }
        if (this.closable && this.closeRect(this.bounds()).contains(event.x(), event.y())) {
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
    }

    @Override
    protected void onPointerMove(final PointerEvent event) {
        this.closeHovered = event.target() == this && this.closable
                && this.closeRect(this.bounds()).contains(event.x(), event.y());
    }

    @Override
    protected void onPointerExit(final PointerEvent event) {
        this.closeHovered = false;
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
}
