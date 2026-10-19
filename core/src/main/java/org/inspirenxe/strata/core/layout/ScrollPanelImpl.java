package org.inspirenxe.strata.core.layout;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.event.input.DragEvent;
import org.inspirenxe.strata.api.event.input.KeyEvent;
import org.inspirenxe.strata.api.event.input.PointerButtonEvent;
import org.inspirenxe.strata.api.event.input.PointerEvent;
import org.inspirenxe.strata.api.event.input.ScrollEvent;
import org.inspirenxe.strata.api.extension.ArrangeContext;
import org.inspirenxe.strata.api.extension.CustomContainer;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.layout.ScrollPanel;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;

import java.util.Objects;
import java.util.Set;

/**
 * The core {@link ScrollPanel}. An {@link ScrollPolicy#AUTO} axis measures its children once without its scroll
 * bar and again with it only if the first pass overflowed; arranging checks again against the real bounds and shows
 * the bar if the content overflows them. Offsets set before the first layout are kept and clamped by it.
 */
public final class ScrollPanelImpl extends CustomContainer<ScrollPanel> implements ScrollPanel {
    private static final Set<String> REQUIRED = Set.of(STYLE_THUMB);
    private static final float GUTTER = BAR_WIDTH + BAR_GAP;

    private final ScrollTrack verticalBar;
    private final ScrollTrack horizontalBar;
    private ScrollPolicy horizontalPolicy;
    private ScrollPolicy verticalPolicy;
    private float rate;
    private float scrollX;
    private float scrollY;
    private float maxScrollX;
    private float maxScrollY;
    private boolean arranged;

    // Layout results
    private float contentWidth;
    private float contentHeight;
    private boolean verticalShown;
    private boolean horizontalShown;
    private Rect viewport;

    // Pointer
    private int dragAxis;
    private float dragGrab;
    private boolean thumbHovered;

    // Theme
    private Paint thumb;
    private Paint thumbHoveredPaint;
    private Paint track;

    /**
     * Creates an empty panel that scrolls vertically.
     *
     * @param ui the factory that owns the panel
     */
    public ScrollPanelImpl(final ComponentFactory ui) {
        super(ui);
        // Keys bubble here from focused content; the panel takes focus itself only when asked to
        this.setFocusable(false);
        this.verticalBar = new ScrollTrack(true);
        this.horizontalBar = new ScrollTrack(false);
        this.horizontalPolicy = ScrollPolicy.NEVER;
        this.verticalPolicy = ScrollPolicy.AUTO;
        this.rate = DEFAULT_SCROLL_RATE;
        this.viewport = Rect.ZERO;
        this.thumb = Paint.NONE;
        this.thumbHoveredPaint = Paint.NONE;
        this.track = Paint.NONE;
    }

    // ------------------------------------------------------------------
    // ScrollPanel
    // ------------------------------------------------------------------

    @Override
    public float scrollX() {
        return this.scrollX;
    }

    @Override
    public float scrollY() {
        return this.scrollY;
    }

    @Override
    public float maxScrollX() {
        return this.maxScrollX;
    }

    @Override
    public float maxScrollY() {
        return this.maxScrollY;
    }

    @Override
    public ScrollPanel setScroll(final float x, final float y) {
        this.scrollTo(x, y);
        return this;
    }

    @Override
    public ScrollPanel scrollBy(final float dx, final float dy) {
        this.scrollTo(this.scrollX + dx, this.scrollY + dy);
        return this;
    }

    /**
     * Moves to an offset, clamped once laid out, and fires {@link ScrollPanel#SCROLLED} on a change. The states are
     * only made when a handler is on the path.
     *
     * @return true if the offset changed
     */
    private boolean scrollTo(final float x, final float y) {
        final float nextX = this.arranged ? clamp(x, this.maxScrollX) : Math.max(0f, x);
        final float nextY = this.arranged ? clamp(y, this.maxScrollY) : Math.max(0f, y);
        if (nextX == this.scrollX && nextY == this.scrollY) {
            return false;
        }
        final State previous = this.isHandled(SCROLLED) ? this.state() : null;
        this.scrollX = nextX;
        this.scrollY = nextY;
        this.invalidateArrangement();
        if (previous != null) {
            this.report(SCROLLED, this.state(), previous);
        }
        return true;
    }

    @Override
    public ScrollPanel scrollToVisible(final Rect area) {
        final Rect view = this.viewport;
        float x = this.scrollX;
        float y = this.scrollY;
        if (area.y() < view.y()) {
            y -= view.y() - area.y();
        } else if (area.bottom() > view.bottom()) {
            y += Math.min(area.bottom() - view.bottom(), area.y() - view.y());
        }
        if (area.x() < view.x()) {
            x -= view.x() - area.x();
        } else if (area.right() > view.right()) {
            x += Math.min(area.right() - view.right(), area.x() - view.x());
        }
        this.scrollTo(x, y);
        return this;
    }

    @Override
    public ScrollPanel scrollToVisible(final Component<?> descendant) {
        return this.scrollToVisible(descendant.bounds());
    }

    @Override
    public ScrollPolicy horizontalPolicy() {
        return this.horizontalPolicy;
    }

    @Override
    public ScrollPolicy verticalPolicy() {
        return this.verticalPolicy;
    }

    @Override
    public ScrollPanel setPolicy(final ScrollPolicy horizontal, final ScrollPolicy vertical) {
        this.horizontalPolicy = Objects.requireNonNull(horizontal, "horizontal");
        this.verticalPolicy = Objects.requireNonNull(vertical, "vertical");
        this.invalidateLayout();
        return this;
    }

    @Override
    public float scrollRate() {
        return this.rate;
    }

    @Override
    public ScrollPanel setScrollRate(final float unitsPerNotch) {
        if (!(unitsPerNotch > 0f) || Float.isInfinite(unitsPerNotch)) {
            throw new IllegalArgumentException("Scroll rate must be finite and greater than 0: " + unitsPerNotch);
        }
        this.rate = unitsPerNotch;
        return this;
    }

    @Override
    public Rect viewport() {
        return this.viewport;
    }

    private State state() {
        return new State(this.scrollX, this.scrollY, this.maxScrollX, this.maxScrollY);
    }

    // ------------------------------------------------------------------
    // Theme and layout
    // ------------------------------------------------------------------

    @Override
    protected Set<String> requiredStyleKeys() {
        return REQUIRED;
    }

    @Override
    protected void onApplyTheme(final Theme theme) {
        this.thumb = theme.paintOr(STYLE_THUMB, Paint.NONE);
        this.thumbHoveredPaint = theme.paintOr(STYLE_THUMB_HOVERED, this.thumb);
        this.track = theme.paintOr(STYLE_TRACK, Paint.NONE);
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        final Insets padding = this.padding();
        final float innerWidth = availableWidth - padding.horizontal();
        final float innerHeight = availableHeight - padding.vertical();
        boolean vertical = this.verticalPolicy == ScrollPolicy.ALWAYS;
        boolean horizontal = this.horizontalPolicy == ScrollPolicy.ALWAYS;

        this.measureChildren(context, innerWidth, innerHeight, vertical, horizontal);
        if (this.verticalPolicy == ScrollPolicy.AUTO && this.contentHeight > innerHeight - (horizontal ? GUTTER : 0f)) {
            vertical = true;
            this.measureChildren(context, innerWidth, innerHeight, vertical, horizontal);
        }
        if (this.horizontalPolicy == ScrollPolicy.AUTO && this.contentWidth > innerWidth - (vertical ? GUTTER : 0f)) {
            horizontal = true;
            this.measureChildren(context, innerWidth, innerHeight, vertical, horizontal);
        }
        this.verticalShown = vertical;
        this.horizontalShown = horizontal;

        float width = this.contentWidth + padding.horizontal() + (vertical ? GUTTER : 0f);
        float height = this.contentHeight + padding.vertical() + (horizontal ? GUTTER : 0f);
        if (this.horizontalPolicy != ScrollPolicy.NEVER && !Float.isInfinite(availableWidth)) {
            width = Math.min(width, availableWidth);
        }
        if (this.verticalPolicy != ScrollPolicy.NEVER && !Float.isInfinite(availableHeight)) {
            height = Math.min(height, availableHeight);
        }
        return new Size(width, height);
    }

    private void measureChildren(final MeasureContext context, final float innerWidth, final float innerHeight,
                                 final boolean vertical, final boolean horizontal) {
        final float childWidth = this.horizontalPolicy == ScrollPolicy.NEVER
                ? innerWidth - (vertical ? GUTTER : 0f) : Float.POSITIVE_INFINITY;
        final float childHeight = this.verticalPolicy == ScrollPolicy.NEVER
                ? innerHeight - (horizontal ? GUTTER : 0f) : Float.POSITIVE_INFINITY;
        float width = 0f;
        float height = 0f;
        for (final Component<?> child : this.children()) {
            final Size outer = context.measure(child, childWidth, childHeight);
            width = Math.max(width, outer.width());
            height = Math.max(height, outer.height());
        }
        this.contentWidth = width;
        this.contentHeight = height;
    }

    @Override
    protected void arrange(final ArrangeContext context, final Rect bounds) {
        final Rect content = this.contentBox();
        boolean vertical = this.verticalShown;
        boolean horizontal = this.horizontalShown;
        if (this.verticalPolicy == ScrollPolicy.AUTO && !vertical
                && this.contentHeight > content.height() - (horizontal ? GUTTER : 0f)) {
            vertical = true;
        }
        if (this.horizontalPolicy == ScrollPolicy.AUTO && !horizontal
                && this.contentWidth > content.width() - (vertical ? GUTTER : 0f)) {
            horizontal = true;
        }
        this.verticalShown = vertical;
        this.horizontalShown = horizontal;

        this.viewport = new Rect(content.x(), content.y(),
                Math.max(0f, content.width() - (vertical ? GUTTER : 0f)),
                Math.max(0f, content.height() - (horizontal ? GUTTER : 0f)));
        final float extentWidth = this.horizontalPolicy == ScrollPolicy.NEVER
                ? this.viewport.width() : Math.max(this.contentWidth, this.viewport.width());
        final float extentHeight = this.verticalPolicy == ScrollPolicy.NEVER
                ? this.viewport.height() : Math.max(this.contentHeight, this.viewport.height());
        this.maxScrollX = Math.max(0f, extentWidth - this.viewport.width());
        this.maxScrollY = Math.max(0f, extentHeight - this.viewport.height());
        this.scrollX = clamp(this.scrollX, this.maxScrollX);
        this.scrollY = clamp(this.scrollY, this.maxScrollY);
        this.arranged = true;

        final Rect slot = new Rect(this.viewport.x() - Math.round(this.scrollX), this.viewport.y() - Math.round(this.scrollY),
                extentWidth, extentHeight);
        for (final Component<?> child : this.children()) {
            context.arrange(child, slot);
        }

        this.verticalBar.update(new Rect(content.right() - BAR_WIDTH, content.y(), BAR_WIDTH, this.viewport.height()),
                this.viewport.height(), extentHeight);
        this.horizontalBar.update(new Rect(content.x(), content.bottom() - BAR_WIDTH, this.viewport.width(), BAR_WIDTH),
                this.viewport.width(), extentWidth);
    }

    @Override
    protected Rect childClip() {
        return this.viewport;
    }

    @Override
    protected void paintOverChildren(final Painter painter, final Rect bounds) {
        if (this.verticalShown) {
            this.verticalBar.paint(painter, this.scrollY, this.maxScrollY, this.track,
                    this.thumbHovered && this.dragAxis != 2 || this.dragAxis == 1 ? this.thumbHoveredPaint : this.thumb);
        }
        if (this.horizontalShown) {
            this.horizontalBar.paint(painter, this.scrollX, this.maxScrollX, this.track,
                    this.thumbHovered && this.dragAxis != 1 || this.dragAxis == 2 ? this.thumbHoveredPaint : this.thumb);
        }
    }

    // ------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------

    @Override
    protected void onScroll(final ScrollEvent event) {
        float dx = -event.scrollX() * this.rate;
        float dy = -event.scrollY() * this.rate;
        if (this.verticalPolicy == ScrollPolicy.NEVER) {
            if (dx == 0f) {
                dx = dy;
            }
            dy = 0f;
        }
        if (this.horizontalPolicy == ScrollPolicy.NEVER) {
            dx = 0f;
        }
        if (this.scrollTo(this.scrollX + dx, this.scrollY + dy)) {
            event.consume();
        }
    }

    @Override
    protected void onKey(final KeyEvent event) {
        if (!event.action().isDown() || this.verticalPolicy == ScrollPolicy.NEVER
                || event.modifiers().control() || event.modifiers().alt()) {
            return;
        }
        final float page = Math.max(this.rate, this.viewport.height() - this.rate);
        final boolean moved = switch (event.key()) {
            case UP -> this.scrollTo(this.scrollX, this.scrollY - this.rate);
            case DOWN -> this.scrollTo(this.scrollX, this.scrollY + this.rate);
            case PAGE_UP -> this.scrollTo(this.scrollX, this.scrollY - page);
            case PAGE_DOWN -> this.scrollTo(this.scrollX, this.scrollY + page);
            case HOME -> this.scrollTo(this.scrollX, 0f);
            case END -> this.scrollTo(this.scrollX, this.maxScrollY);
            default -> false;
        };
        if (moved) {
            event.consume();
        }
    }

    @Override
    protected void onPointerPress(final PointerButtonEvent event) {
        this.dragAxis = 0;
        if (event.button() != MouseButton.LEFT) {
            return;
        }
        final float x = event.x();
        final float y = event.y();
        if (this.verticalShown && this.verticalBar.track().contains(x, y)) {
            final Rect thumbRect = this.verticalBar.thumb(this.scrollY, this.maxScrollY);
            if (thumbRect.contains(x, y)) {
                this.dragAxis = 1;
                this.dragGrab = y - thumbRect.y();
            } else {
                final float page = Math.max(this.rate, this.viewport.height() - this.rate);
                this.scrollTo(this.scrollX, this.scrollY + (y < thumbRect.y() ? -page : page));
            }
            event.consume();
        } else if (this.horizontalShown && this.horizontalBar.track().contains(x, y)) {
            final Rect thumbRect = this.horizontalBar.thumb(this.scrollX, this.maxScrollX);
            if (thumbRect.contains(x, y)) {
                this.dragAxis = 2;
                this.dragGrab = x - thumbRect.x();
            } else {
                final float page = Math.max(this.rate, this.viewport.width() - this.rate);
                this.scrollTo(this.scrollX + (x < thumbRect.x() ? -page : page), this.scrollY);
            }
            event.consume();
        }
    }

    @Override
    protected void onDragStart(final DragEvent event) {
        this.onDrag(event);
    }

    @Override
    protected void onDrag(final DragEvent event) {
        if (this.dragAxis == 1) {
            this.scrollTo(this.scrollX, this.verticalBar.offsetForThumbAt(event.y() - this.dragGrab, this.maxScrollY));
            event.consume();
        } else if (this.dragAxis == 2) {
            this.scrollTo(this.horizontalBar.offsetForThumbAt(event.x() - this.dragGrab, this.maxScrollX), this.scrollY);
            event.consume();
        }
    }

    @Override
    protected void onDragEnd(final DragEvent event) {
        this.dragAxis = 0;
    }

    @Override
    protected void onPointerRelease(final PointerButtonEvent event) {
        if (event.button() == MouseButton.LEFT && event.target() == this) {
            this.dragAxis = 0;
        }
    }

    @Override
    protected void onPointerMove(final PointerEvent event) {
        this.thumbHovered = event.target() == this
                && (this.verticalShown && this.verticalBar.thumb(this.scrollY, this.maxScrollY).contains(event.x(), event.y())
                || this.horizontalShown && this.horizontalBar.thumb(this.scrollX, this.maxScrollX).contains(event.x(), event.y()));
    }

    @Override
    protected void onPointerExit(final PointerEvent event) {
        this.thumbHovered = false;
    }

    @Override
    protected void onDescendantFocused(final Component<?> descendant) {
        this.scrollToVisible(descendant);
    }

    @Override
    protected Object snapshotState() {
        return this.state();
    }

    private static float clamp(final float value, final float max) {
        return Math.max(0f, Math.min(value, max));
    }
}
