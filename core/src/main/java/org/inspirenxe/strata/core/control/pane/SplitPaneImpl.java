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
package org.inspirenxe.strata.core.control.pane;

import org.inspirenxe.strata.core.layout.Flow;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.component.Focusable;
import org.inspirenxe.strata.api.component.HasCursor;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.display.Separator;
import org.inspirenxe.strata.api.control.pane.SplitPane;
import org.inspirenxe.strata.api.event.input.DragEvent;
import org.inspirenxe.strata.api.event.input.KeyEvent;
import org.inspirenxe.strata.api.event.input.PointerButtonEvent;
import org.inspirenxe.strata.api.extension.ArrangeContext;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.core.control.Adoption;
import org.inspirenxe.strata.core.control.Rings;
import org.inspirenxe.strata.core.node.BuiltInComponent;
import org.inspirenxe.strata.core.node.BuiltInComposite;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;

import java.util.Objects;
import java.util.Optional;

/**
 * The core {@link SplitPane}. The divider is a private child component, so it can show its own cursor and receive
 * the drag. Each side is arranged in its whole part, where its own alignment places it.
 *
 * <p>The divider takes focus for the pane, as {@code BuiltInComponent.setFocusDelegate} describes, so the pane is one
 * tab stop where the divider sits, between the two sides. Keys move it from the length of the first side as of the
 * last arrange.</p>
 *
 * <p>Each arrange works out the first side's length from the position and then keeps both sides within their
 * minimums. Under a keeping policy the pane holds the kept side's length in units, taken from the position the
 * first time it is arranged or after the position or the policy is set, and works the position out from it on each
 * later arrange. A collapsed side is removed as a child, as an expander removes its content, and put back in its
 * place when it is restored.</p>
 */
public final class SplitPaneImpl extends BuiltInComposite<SplitPane> implements SplitPane {
    private Orientation orientation;
    private final Divider divider;
    private Component<?> first;
    private Component<?> second;
    private float position;
    private float minFirst;
    private float minSecond;
    // The space shared between the sides as of the last arrange
    private float space;
    private ResizePolicy policy;
    // The kept side's length in units under KEEP_FIRST or KEEP_SECOND; NaN until an arrange works it out
    private float kept;
    private boolean collapsibleFirst;
    private boolean collapsibleSecond;
    private Side collapsed;

    /**
     * Creates an empty pane with the divider in the middle.
     *
     * @param ui the factory that owns the pane
     * @param orientation the direction of the split
     */
    @SuppressWarnings("this-escape")
    public SplitPaneImpl(final ComponentFactoryImpl ui, final Orientation orientation) {
        super(ui);
        this.orientation = Objects.requireNonNull(orientation, "orientation");
        this.position = 0.5f;
        this.minFirst = DEFAULT_MIN_SIZE;
        this.minSecond = DEFAULT_MIN_SIZE;
        this.policy = ResizePolicy.PROPORTIONAL;
        this.kept = Float.NaN;
        this.divider = new Divider(ui, this);
        this.addChild(this.divider);
        this.setFocusDelegate(this.divider);
    }

    private boolean horizontal() {
        return this.orientation == Orientation.HORIZONTAL;
    }

    @Override
    public Orientation orientation() {
        return this.orientation;
    }

    @Override
    public SplitPane setOrientation(final Orientation orientation) {
        this.orientation = Objects.requireNonNull(orientation, "orientation");
        this.divider.setCursor(this.horizontal() ? CursorKind.RESIZE_HORIZONTAL : CursorKind.RESIZE_VERTICAL);
        this.invalidateLayout();
        return this;
    }

    @Override
    public Optional<Component<?>> first() {
        return Optional.ofNullable(this.first);
    }

    @Override
    public SplitPane setFirst(final Component<?> first) {
        this.first = this.swap(this.first, first, true);
        return this;
    }

    @Override
    public Optional<Component<?>> second() {
        return Optional.ofNullable(this.second);
    }

    @Override
    public SplitPane setSecond(final Component<?> second) {
        this.second = this.swap(this.second, second, false);
        return this;
    }

    /**
     * Puts a new side in place of an old one, after checking that the pane can take it, so a side it cannot take
     * leaves the pane as it was.
     */
    private Component<?> swap(final Component<?> old, final Component<?> next, final boolean isFirst) {
        if (next == old) {
            return next;
        }
        if (next != null) {
            Adoption.require(this.node(), this.nodeOf(next));
        }
        // A collapsed side is not a child, so it is only swapped in the field until it is restored
        final boolean hidden = this.collapsed == (isFirst ? Side.FIRST : Side.SECOND);
        if (old != null && !hidden) {
            this.removeChild(old);
        }
        if (next != null && !hidden) {
            // The children go first side, divider, second side, so Tab stops at the divider between them
            if (isFirst) {
                this.insertContent(0, next);
            } else {
                this.addContent(next);
            }
        }
        this.invalidateLayout();
        return next;
    }

    @Override
    public float position() {
        return this.position;
    }

    @Override
    public SplitPane setPosition(final float position) {
        if (!(position >= 0f && position <= 1f)) {
            throw new IllegalArgumentException("Position must be from 0 to 1: " + position);
        }
        final float previous = this.position;
        if (position != previous) {
            this.position = position;
            this.keepFromPosition();
            this.invalidateArrangement();
            this.report(MOVED, position, previous);
        }
        return this;
    }

    /**
     * Works the kept side's length out from the position, under a keeping policy, or forgets it until the next
     * arrange when the pane has no space yet.
     */
    private void keepFromPosition() {
        if (this.policy == ResizePolicy.PROPORTIONAL || this.space <= 0f) {
            this.kept = Float.NaN;
        } else {
            this.kept = this.space * (this.policy == ResizePolicy.KEEP_FIRST ? this.position : 1f - this.position);
        }
    }

    @Override
    public ResizePolicy resizePolicy() {
        return this.policy;
    }

    @Override
    public SplitPane setResizePolicy(final ResizePolicy policy) {
        this.policy = Objects.requireNonNull(policy, "policy");
        this.keepFromPosition();
        this.invalidateLayout();
        return this;
    }

    @Override
    public boolean isCollapsible(final Side side) {
        return Objects.requireNonNull(side, "side") == Side.FIRST ? this.collapsibleFirst : this.collapsibleSecond;
    }

    @Override
    public SplitPane setCollapsible(final boolean first, final boolean second) {
        this.collapsibleFirst = first;
        this.collapsibleSecond = second;
        return this;
    }

    @Override
    public Optional<Side> collapsed() {
        return Optional.ofNullable(this.collapsed);
    }

    @Override
    public SplitPane setCollapsed(final Side side) {
        final Side previous = this.collapsed;
        if (side == previous) {
            return this;
        }
        if (previous != null) {
            this.collapsed = null;
            this.restore(previous);
        }
        this.collapsed = side;
        if (side != null) {
            final Component<?> component = side == Side.FIRST ? this.first : this.second;
            if (component != null) {
                this.removeChild(component);
            }
        }
        this.invalidateLayout();
        this.report(COLLAPSED, side, previous);
        return this;
    }

    /**
     * Puts a side back as a child in its place: the first before the divider, the second after it.
     */
    private void restore(final Side side) {
        if (side == Side.FIRST && this.first != null) {
            this.insertContent(0, this.first);
        } else if (side == Side.SECOND && this.second != null) {
            this.addContent(this.second);
        }
    }

    /**
     * Collapses a side for the user, or restores the collapsed one, through {@link SplitPane#COLLAPSE}: the first
     * collapsible side, or null while one is collapsed. Does nothing while neither side is collapsible.
     *
     * @return true if the pane asked
     */
    boolean userCollapse() {
        if (!this.collapsibleFirst && !this.collapsibleSecond) {
            return false;
        }
        final Side target = this.collapsed != null ? null : this.collapsibleFirst ? Side.FIRST : Side.SECOND;
        this.request(COLLAPSE, target, this::setCollapsed);
        return true;
    }

    /**
     * Restores a collapsed side for the user before a drag, through {@link SplitPane#COLLAPSE} with null.
     *
     * @return true if both sides show afterward, so the drag may go on
     */
    boolean userRestoreForDrag() {
        if (this.collapsed != null) {
            this.request(COLLAPSE, null, this::setCollapsed);
        }
        return this.collapsed == null;
    }

    @Override
    public SplitPane setMinSizes(final float first, final float second) {
        if (first < 0f || second < 0f || Float.isInfinite(first) || Float.isInfinite(second)) {
            throw new IllegalArgumentException("Minimum sizes must be finite and not negative: " + first + ", " + second);
        }
        this.minFirst = first;
        this.minSecond = second;
        this.invalidateLayout();
        return this;
    }

    /**
     * Moves the divider for the user so the first side is a length along the split, kept within the minimum sizes
     * when both fit. {@link SplitPane#MOVE} fires only if the divider would move.
     */
    void dragTo(final float firstLength) {
        if (this.space <= 0f) {
            return;
        }
        final float position = this.fit(firstLength);
        if (position != this.position) {
            this.request(MOVE, position, this::applyUserPosition);
        }
    }

    /**
     * Moves the divider to the final position of a {@link SplitPane#MOVE}, clamped to 0 to 1 and kept within the
     * minimum sizes when both fit. Null and NaN are ignored.
     */
    private void applyUserPosition(final Float position) {
        if (position == null || Float.isNaN(position) || this.space <= 0f) {
            return;
        }
        float fitted = Math.max(0f, Math.min(1f, position));
        if (this.minFirst + this.minSecond <= this.space) {
            fitted = Math.max(this.minFirst / this.space, Math.min((this.space - this.minSecond) / this.space, fitted));
        }
        this.setPosition(fitted);
    }

    /**
     * Turns a length of the first side into a position from 0 to 1, keeping both sides within their minimum sizes
     * when both fit.
     */
    private float fit(final float firstLength) {
        float length = firstLength;
        if (this.minFirst + this.minSecond <= this.space) {
            length = Math.max(this.minFirst, Math.min(this.space - this.minSecond, length));
        }
        return Math.max(0f, Math.min(1f, length / this.space));
    }

    /**
     * Returns the length of the first side for the last arrange's space: the space times the position, rounded down
     * to a whole unit, except that a product within a thousandth of a unit of a whole number counts as that number.
     * A position worked out from a length, as a drag does, comes back as that length although the division and the
     * multiplication in floats may leave the product a hair under it.
     */
    private float firstLength() {
        final double product = (double) this.space * this.position;
        final double whole = Math.rint(product);
        return (float) (Math.abs(product - whole) < 1e-3 ? whole : Math.floor(product));
    }

    /**
     * Returns the length the first side is laid out at: none or all the space while a side is collapsed, and
     * otherwise {@link #firstLength()} kept within both minimums when they fit. When they do not, the side the policy
     * keeps gets its minimum, or all the space if that is less: the second under {@code KEEP_SECOND}, the first
     * otherwise.
     */
    private float laidOutFirst() {
        if (this.collapsed != null) {
            return this.collapsed == Side.FIRST ? 0f : this.space;
        }
        if (this.minFirst + this.minSecond <= this.space) {
            return Math.max(this.minFirst, Math.min(this.space - this.minSecond, this.firstLength()));
        }
        if (this.policy == ResizePolicy.KEEP_SECOND) {
            return Math.max(0f, this.space - this.minSecond);
        }
        return Math.min(this.minFirst, this.space);
    }

    /**
     * Moves the divider for the user by a key: by a number of units along the split, or to an end.
     */
    private boolean keyMove(final KeyEvent event) {
        if (event.key().isEnter()) {
            return event.action() == KeyAction.PRESS && this.userCollapse();
        }
        if (this.collapsed != null) {
            return false;
        }
        final boolean across = this.horizontal();
        // Right to left the first side is at the right, so Left grows it
        final Key key = across ? Flow.key(event.key(), this.isRightToLeft()) : event.key();
        final float step = event.modifiers().shift() ? 1f : KEY_STEP;
        final float length = this.laidOutFirst();
        if (key == (across ? Key.LEFT : Key.UP)) {
            this.dragTo(length - step);
        } else if (key == (across ? Key.RIGHT : Key.DOWN)) {
            this.dragTo(length + step);
        } else if (key == Key.HOME) {
            this.dragTo(this.minFirst);
        } else if (key == Key.END) {
            this.dragTo(this.space - this.minSecond);
        } else {
            return false;
        }
        return true;
    }

    float firstStart() {
        return this.horizontal() ? this.bounds().x() : this.bounds().y();
    }

    // ------------------------------------------------------------------
    // Layout
    // ------------------------------------------------------------------

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        context.measure(this.divider, availableWidth, availableHeight);
        final Size a = this.first == null || this.collapsed == Side.FIRST ? Size.ZERO
                : context.measure(this.first, availableWidth, availableHeight);
        final Size b = this.second == null || this.collapsed == Side.SECOND ? Size.ZERO
                : context.measure(this.second, availableWidth, availableHeight);
        if (this.horizontal()) {
            return new Size(a.width() + DIVIDER_SIZE + b.width(), Math.max(a.height(), b.height()));
        }
        return new Size(Math.max(a.width(), b.width()), a.height() + DIVIDER_SIZE + b.height());
    }

    @Override
    protected void arrange(final ArrangeContext context, final Rect bounds) {
        final boolean across = this.horizontal();
        this.space = Math.max(0f, (across ? bounds.width() : bounds.height()) - DIVIDER_SIZE);
        if (this.policy != ResizePolicy.PROPORTIONAL && this.space > 0f) {
            if (Float.isNaN(this.kept)) {
                this.keepFromPosition();
            } else {
                // The divider stays where the kept side ends, so the position follows without a report
                final float share = Math.max(0f, Math.min(1f, this.kept / this.space));
                this.position = this.policy == ResizePolicy.KEEP_FIRST ? share : 1f - share;
            }
        }
        final float firstLength = this.laidOutFirst();
        final Rect firstRect;
        final Rect dividerRect;
        final Rect secondRect;
        if (across) {
            firstRect = new Rect(bounds.x(), bounds.y(), firstLength, bounds.height());
            dividerRect = new Rect(bounds.x() + firstLength, bounds.y(), DIVIDER_SIZE, bounds.height());
            secondRect = new Rect(dividerRect.right(), bounds.y(), Math.max(0f, bounds.right() - dividerRect.right()), bounds.height());
        } else {
            firstRect = new Rect(bounds.x(), bounds.y(), bounds.width(), firstLength);
            dividerRect = new Rect(bounds.x(), bounds.y() + firstLength, bounds.width(), DIVIDER_SIZE);
            secondRect = new Rect(bounds.x(), dividerRect.bottom(), bounds.width(), Math.max(0f, bounds.bottom() - dividerRect.bottom()));
        }
        if (this.first != null && this.collapsed != Side.FIRST) {
            context.measure(this.first, firstRect.width(), firstRect.height());
            context.arrange(this.first, firstRect);
        }
        context.arrange(this.divider, dividerRect);
        if (this.second != null && this.collapsed != Side.SECOND) {
            context.measure(this.second, secondRect.width(), secondRect.height());
            context.arrange(this.second, secondRect);
        }
    }

    /**
     * The divider. A drag moves it by the pointer, keeping the point grabbed under the pointer.
     */
    private static final class Divider extends BuiltInComponent<Divider> implements HasCursor<Divider>,
            Focusable<Divider> {
        private final SplitPaneImpl pane;
        private Paint paint;
        private Paint hovered;
        private Paint focus;
        private final Rings.Ring ring = new Rings.Ring();
        private float grab;
        private boolean dragging;

        @SuppressWarnings("this-escape")
        Divider(final ComponentFactoryImpl ui, final SplitPaneImpl pane) {
            super(ui);
            this.pane = pane;
            this.paint = Paint.NONE;
            this.hovered = Paint.NONE;
            this.focus = Paint.NONE;
            this.setAnchor(Anchor.FILL);
            this.setCursor(pane.horizontal() ? CursorKind.RESIZE_HORIZONTAL : CursorKind.RESIZE_VERTICAL);
        }

        @Override
        protected void onApplyTheme(final Theme theme) {
            this.paint = theme.paintOr(STYLE_DIVIDER, theme.paintOr(Separator.STYLE_LINE, Paint.NONE));
            this.hovered = theme.paintOr(STYLE_DIVIDER_HOVERED, this.paint);
            this.focus = theme.paintOr(STYLE_FOCUS, theme.paintOr(Button.STYLE_FOCUS, Paint.NONE));
        }

        @Override
        protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
            return new Size(DIVIDER_SIZE, DIVIDER_SIZE);
        }

        @Override
        protected void paint(final Painter painter, final Rect bounds) {
            (this.isHovered() || this.dragging ? this.hovered : this.paint).paint(painter, bounds);
            if (this.isFocused() && this.focus != Paint.NONE) {
                this.ring.paint(painter, this.focus, bounds);
            }
        }

        @Override
        protected void onKey(final KeyEvent event) {
            if (event.action().isDown() && !event.modifiers().control() && !event.modifiers().alt()
                    && this.pane.keyMove(event)) {
                event.consume();
            }
        }

        /**
         * Returns a point's place along the split as for left to right: right to left a side by side split counts
         * from the right, where its first side is.
         */
        private float along(final float x, final float y) {
            if (!this.pane.horizontal()) {
                return y;
            }
            return this.pane.isRightToLeft() ? Flow.mirrorX(x, this.pane.bounds()) : x;
        }

        /**
         * Returns where the divider starts along the split as for left to right.
         */
        private float start(final Rect bounds) {
            if (!this.pane.horizontal()) {
                return bounds.y();
            }
            return this.pane.isRightToLeft() ? Flow.mirrorX(bounds.right(), this.pane.bounds()) : bounds.x();
        }

        @Override
        protected void onPointerPress(final PointerButtonEvent event) {
            if (event.button() == MouseButton.LEFT) {
                final Rect bounds = this.bounds();
                this.grab = this.along(event.x(), event.y()) - this.start(bounds);
                event.consume();
            }
        }

        @Override
        protected void onClick(final PointerButtonEvent event) {
            if (event.button() == MouseButton.LEFT && event.clicks() == 2 && this.pane.userCollapse()) {
                event.consume();
            }
        }

        @Override
        protected void onDragStart(final DragEvent event) {
            if (event.button() == MouseButton.LEFT && !this.pane.userRestoreForDrag()) {
                return;
            }
            this.dragging = true;
            this.onDrag(event);
        }

        @Override
        protected void onDrag(final DragEvent event) {
            if (event.button() == MouseButton.LEFT && this.dragging) {
                this.pane.dragTo(this.along(event.x(), event.y()) - this.grab - this.pane.firstStart());
                event.consume();
            }
        }

        @Override
        protected void onDragEnd(final DragEvent event) {
            this.dragging = false;
        }
    }
}
