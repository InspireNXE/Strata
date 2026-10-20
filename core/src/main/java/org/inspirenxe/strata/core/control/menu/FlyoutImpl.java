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
package org.inspirenxe.strata.core.control.menu;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.Focusable;
import org.inspirenxe.strata.api.control.collection.ComboBox;
import org.inspirenxe.strata.api.control.menu.Flyout;
import org.inspirenxe.strata.api.event.input.KeyEvent;
import org.inspirenxe.strata.api.extension.ArrangeContext;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.overlay.Overlay;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.core.control.CorePopup;
import org.inspirenxe.strata.core.node.BuiltInComposite;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;
import org.inspirenxe.strata.core.node.Node;
import org.inspirenxe.strata.core.node.Root;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The core {@link Flyout}. It owns the popup overlay it opens in, as {@link MenuImpl} does, and adds its content
 * with {@code addContent}, so the content's control events reach handlers on the flyout.
 *
 * <p>It measures its content with the space it is offered less its padding, and asks for the content's size plus
 * the padding, no larger than that space, which for a popup is the host. The content fills the flyout less its
 * padding. The background is the theme's {@value #STYLE_BACKGROUND}, or else its {@value ComboBox#STYLE_POPUP}, and
 * the padding its {@value #STYLE_PADDING}, or else {@link #DEFAULT_PADDING}.</p>
 *
 * <p>The flyout is focusable, so focus can rest on it when the content has nothing to focus, and not tabbable.
 * Opening it lays out its host's overlays at once and moves focus to the first node of the flyout's own tab order,
 * else to the flyout; the component that had focus before gets it back when the flyout closes, if it can still take
 * it. Tab and Shift+Tab that reach the flyout move through that tab order, wrapping, and are consumed even when it is
 * empty, so focus stays inside. Light dismiss is a setting of its overlay in the host's {@link Root}.</p>
 */
public final class FlyoutImpl extends BuiltInComposite<Flyout> implements Flyout, CorePopup {
    /**
     * The padding when the theme sets no {@value #STYLE_PADDING}: 4 on every side.
     */
    public static final Insets DEFAULT_PADDING = Insets.of(4f);

    private final ComponentFactoryImpl ui;
    private Component<?> content;
    private boolean lightDismiss;
    private Overlay popup;
    private Component<?> returnFocus;
    private Holder holder;

    // Theme
    private Paint background;
    private Insets padding = DEFAULT_PADDING;

    /**
     * Creates an empty, closed flyout with light dismiss on.
     *
     * @param ui the factory that owns the flyout
     */
    @SuppressWarnings("this-escape")
    public FlyoutImpl(final ComponentFactoryImpl ui) {
        super(ui);
        this.ui = ui;
        this.lightDismiss = true;
        this.background = Paint.NONE;
        this.setTabbable(false);
    }

    // ------------------------------------------------------------------
    // Content and settings
    // ------------------------------------------------------------------

    @Override
    public Optional<Component<?>> content() {
        return Optional.ofNullable(this.content);
    }

    @Override
    public Flyout setContent(final Component<?> content) {
        if (content == this.content) {
            return this;
        }
        if (content != null) {
            final Node node = this.nodeOf(content);
            if (node.parentNode() != null || node.root() != null) {
                throw new IllegalArgumentException(content + " is already part of a tree");
            }
        }
        final boolean refocus = this.isOpen() && this.hasFocusWithin();
        if (this.content != null) {
            this.removeChild(this.content);
        }
        this.content = content;
        if (content == null) {
            this.close();
        } else {
            this.addContent(content);
            if (refocus) {
                this.focusFirst();
            }
        }
        this.invalidateLayout();
        return this;
    }

    @Override
    public boolean isLightDismiss() {
        return this.lightDismiss;
    }

    @Override
    public Flyout setLightDismiss(final boolean dismiss) {
        this.lightDismiss = dismiss;
        if (this.popup != null && this.hostRoot() != null) {
            this.hostRoot().setLightDismiss(this.popup, dismiss);
        }
        return this;
    }

    // ------------------------------------------------------------------
    // Opening and closing
    // ------------------------------------------------------------------

    @Override
    public boolean isOpen() {
        return this.popup != null && this.popup.isOpen();
    }

    @Override
    public Flyout open(final Rect anchor) {
        Objects.requireNonNull(anchor, "anchor");
        if (!this.canOpen()) {
            return this;
        }
        final Root root = this.hostRoot();
        if (root == null) {
            // The factory has no host; this throws the same exception any overlay would
            this.ui.overlays();
            return this;
        }
        this.returnFocus = root.focused() == null ? null : root.focused().facade();
        this.popup = root.popup(this, anchor, this.lightDismiss);
        this.popup.onEvent(Overlay.CLOSED, event -> this.closed());
        // Lay the popup out now, so focus and keys find the content before the next frame
        root.layout();
        this.focusFirst();
        this.report(OPENED);
        return this;
    }

    @Override
    public Flyout openAt(final float x, final float y) {
        return this.open(new Rect(x, y, 0f, 0f));
    }

    @Override
    public Flyout close() {
        if (this.popup != null) {
            this.popup.close();
        }
        return this;
    }

    /**
     * Runs once the popup has closed, however it closed: gives focus back, reports {@code CLOSED} and tells the
     * holder.
     */
    private void closed() {
        this.popup = null;
        if (this.returnFocus != null) {
            final Component<?> focus = this.returnFocus;
            this.returnFocus = null;
            if (focus.isAttached() && focus instanceof final Focusable<?> focusable) {
                focusable.requestFocus();
            }
        }
        this.report(CLOSED);
        if (this.holder != null) {
            this.holder.popupClosed();
        }
    }

    /**
     * Focuses the first node of the flyout's tab order, or the flyout itself when there is none.
     */
    private void focusFirst() {
        final Root root = this.hostRoot();
        if (root == null || !this.isAttached()) {
            return;
        }
        final List<Node> order = root.tabOrderWithin(this.node());
        if (order.isEmpty() || !order.getFirst().requestFocus()) {
            this.requestFocus();
        }
    }

    private boolean hasFocusWithin() {
        final Root root = this.hostRoot();
        for (Node node = root == null ? null : root.focused(); node != null; node = node.parentNode()) {
            if (node == this.node()) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Held by a drop-down button
    // ------------------------------------------------------------------

    @Override
    public boolean canOpen() {
        return !this.isOpen() && this.content != null;
    }

    @Override
    public Holder holder() {
        return this.holder;
    }

    @Override
    public void setHolder(final Holder holder) {
        if (holder != null && this.holder != null && holder != this.holder) {
            throw new IllegalArgumentException(this + " already belongs to another drop-down button");
        }
        this.holder = holder;
    }

    @Override
    public void openBelow(final Rect anchor, final boolean fromKeyboard) {
        this.open(anchor);
    }

    // ------------------------------------------------------------------
    // Theme, layout and painting
    // ------------------------------------------------------------------

    @Override
    protected void onApplyTheme(final Theme theme) {
        this.background = theme.paintOr(STYLE_BACKGROUND, theme.paintOr(ComboBox.STYLE_POPUP, Paint.NONE));
        this.padding = theme.insetsOr(STYLE_PADDING, DEFAULT_PADDING);
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        final Insets padding = this.padding;
        float width = padding.horizontal();
        float height = padding.vertical();
        if (this.content != null) {
            final Size size = context.measure(this.content, Math.max(0f, availableWidth - padding.horizontal()),
                    Math.max(0f, availableHeight - padding.vertical()));
            width += size.width();
            height += size.height();
        }
        return new Size(Math.min(width, availableWidth), Math.min(height, availableHeight));
    }

    @Override
    protected void arrange(final ArrangeContext context, final Rect bounds) {
        if (this.content != null) {
            final Insets padding = this.padding;
            context.arrange(this.content, new Rect(bounds.x() + padding.left(), bounds.y() + padding.top(),
                    Math.max(0f, bounds.width() - padding.horizontal()), Math.max(0f, bounds.height() - padding.vertical())));
        }
    }

    @Override
    protected void paint(final Painter painter, final Rect bounds) {
        this.background.paint(painter, bounds);
    }

    // ------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------

    @Override
    protected void onKey(final KeyEvent event) {
        if (event.key() != Key.TAB || !event.action().isDown() || event.modifiers().control()
                || event.modifiers().alt()) {
            return;
        }
        final Root root = this.hostRoot();
        if (root != null && this.isOpen()) {
            final List<Node> order = root.tabOrderWithin(this.node());
            if (!order.isEmpty()) {
                final int index = order.indexOf(root.focused());
                final boolean backwards = event.modifiers().shift();
                final int next;
                if (index < 0) {
                    next = backwards ? order.size() - 1 : 0;
                } else {
                    next = Math.floorMod(index + (backwards ? -1 : 1), order.size());
                }
                order.get(next).requestFocus();
            }
        }
        event.consume();
    }

    @Override
    protected Object snapshotState() {
        return new Flyout.State(this.isOpen());
    }
}
