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
package org.inspirenxe.strata.core.control.button;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.control.attribute.popup.Popup;
import org.inspirenxe.strata.api.control.button.DropDownButton;
import org.inspirenxe.strata.api.event.input.KeyEvent;
import org.inspirenxe.strata.api.event.input.PointerButtonEvent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.visual.Visual;
import org.inspirenxe.strata.core.control.CorePopup;
import org.inspirenxe.strata.core.control.RectCache;
import org.inspirenxe.strata.core.control.Rings;
import org.inspirenxe.strata.core.control.collection.ComboBoxImpl;
import org.inspirenxe.strata.core.node.BuiltInComponent;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;

import java.util.Optional;
import java.util.Set;

/**
 * The core {@link DropDownButton}. It shares its keys, caption, icon and measuring with {@link ButtonImpl}, and adds
 * a down arrow, the one {@link ComboBoxImpl} draws, {@link ComboBoxImpl#ARROW_GAP} units after the content and
 * {@link ComboBoxImpl#ARROW_WIDTH} wide, against the right edge of the space inside the padding. The caption and
 * icon center in that space less the gap and the arrow. Its face is the disabled paint when disabled, the pressed
 * paint while open or while held under the pointer, the hovered paint while hovered or focused, and the normal paint
 * otherwise.
 *
 * <p>The button has no children: the popup is the developer's and lives in its own overlay while open. It holds the
 * popup as a {@link CorePopup.Holder}, checks {@link CorePopup#canOpen()} before it asks {@code OPEN}, opens it with
 * {@link CorePopup#openBelow}, and reports {@code CLOSED} when the popup tells it that it closed, if this button
 * opened it. A disabled button closes its popup.</p>
 */
public final class DropDownButtonImpl extends BuiltInComponent<DropDownButton> implements DropDownButton,
        CorePopup.Holder {
    private final ButtonContent content;
    private Popup<?> popup;
    private boolean open;

    // Theme
    private Paint arrow;
    // Kept between frames
    private final RectCache contentArea = new RectCache();
    private final Rings.Arrow arrowLines = new Rings.Arrow();

    /**
     * Creates a drop-down button with no popup.
     *
     * @param ui the factory that owns the button
     * @param text the caption
     */
    @SuppressWarnings("this-escape")
    public DropDownButtonImpl(final ComponentFactoryImpl ui, final Text text) {
        super(ui);
        this.content = new ButtonContent(text);
        this.arrow = Paint.NONE;
        this.setCursor(CursorKind.HAND);
        this.setMinSize(20f, 0f);
    }

    // ------------------------------------------------------------------
    // Caption and icon
    // ------------------------------------------------------------------

    @Override
    public Text text() {
        return this.content.text();
    }

    @Override
    public DropDownButton setText(final Text text) {
        this.content.setText(text);
        this.invalidateLayout();
        return this;
    }

    @Override
    public DropDownButton setTextPadding(final Insets padding) {
        this.content.setTextPadding(padding);
        this.invalidateLayout();
        return this;
    }

    @Override
    public SpriteId iconSprite() {
        return this.content.iconSprite();
    }

    @Override
    public Visual iconVisual() {
        return this.content.iconVisual();
    }

    @Override
    public DropDownButton setIcon(final SpriteId sprite) {
        this.content.setIcon(sprite);
        this.invalidateLayout();
        return this;
    }

    @Override
    public DropDownButton setIcon(final Visual visual) {
        this.content.setIcon(visual);
        this.invalidateLayout();
        return this;
    }

    @Override
    public DropDownButton clearIcon() {
        this.content.clearIcon();
        this.invalidateLayout();
        return this;
    }

    @Override
    public float iconWidth() {
        return this.content.iconWidth();
    }

    @Override
    public float iconHeight() {
        return this.content.iconHeight();
    }

    @Override
    public DropDownButton setIconSize(final float width, final float height) {
        this.content.setIconSize(width, height);
        this.invalidateLayout();
        return this;
    }

    // ------------------------------------------------------------------
    // The popup
    // ------------------------------------------------------------------

    @Override
    public Optional<Popup<?>> popup() {
        return Optional.ofNullable(this.popup);
    }

    @Override
    public DropDownButton setPopup(final Popup<?> popup) {
        if (popup == this.popup) {
            return this;
        }
        if (popup != null) {
            if (!(popup instanceof final CorePopup next) || !(popup instanceof final Component<?> component)) {
                throw new IllegalArgumentException(popup + " is not a Strata popup");
            }
            this.nodeOf(component);
            if (next.holder() != null) {
                throw new IllegalArgumentException(popup + " already belongs to another drop-down button");
            }
            next.setHolder(this);
        }
        final Popup<?> old = this.popup;
        if (old != null) {
            old.close();
            ((CorePopup) old).setHolder(null);
        }
        this.open = false;
        this.popup = popup;
        return this;
    }

    @Override
    public boolean isOpen() {
        return this.open;
    }

    @Override
    public DropDownButton open() {
        if (this.canOpen()) {
            this.openPopup(false);
        }
        return this;
    }

    @Override
    public DropDownButton close() {
        if (this.open) {
            this.popup.close();
        }
        return this;
    }

    @Override
    public void popupClosed() {
        if (this.open) {
            this.open = false;
            this.report(CLOSED);
        }
    }

    private boolean canOpen() {
        return !this.open && this.isAttached() && this.isEnabled() && this.popup instanceof final CorePopup core
                && core.canOpen();
    }

    /**
     * Opens the popup for the user if {@code OPEN} allows it. Asks nothing if the popup cannot open.
     */
    private void userOpen(final boolean fromKeyboard) {
        if (this.canOpen() && this.request(OPEN) && this.canOpen()) {
            this.openPopup(fromKeyboard);
        }
    }

    /**
     * Opens the popup below the button and reports {@code OPENED} if it opened.
     */
    private void openPopup(final boolean fromKeyboard) {
        ((CorePopup) this.popup).openBelow(this.bounds(), fromKeyboard);
        if (this.popup.isOpen()) {
            this.open = true;
            this.report(OPENED);
        }
    }

    @Override
    protected void onDisabled() {
        this.close();
    }

    // ------------------------------------------------------------------
    // Theme, layout and painting
    // ------------------------------------------------------------------

    @Override
    protected Set<String> requiredStyleKeys() {
        return ButtonContent.REQUIRED;
    }

    @Override
    protected void onApplyTheme(final Theme theme) {
        this.content.applyTheme(theme);
        this.arrow = theme.paintOr(STYLE_ARROW, Paint.NONE);
    }

    @Override
    protected Size measureMinimum(final MeasureContext context) {
        // The caption does not shrink, and measuring does not depend on the space offered
        return this.measure(context, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        this.content.measure(context.text(), false);
        final Insets padding = this.content.padding(false);
        return new Size(this.content.contentWidth() + ComboBoxImpl.ARROW_GAP + ComboBoxImpl.ARROW_WIDTH
                + padding.horizontal(), Math.max(ButtonImpl.MIN_HEIGHT,
                Math.max(this.content.contentHeight(), Rings.ARROW_SHORT) + padding.vertical()));
    }

    @Override
    protected void paint(final Painter painter, final Rect bounds) {
        final ButtonContent content = this.content;
        final boolean enabled = this.isEnabled();
        content.face(enabled, this.open || this.isPressed() && this.isHovered(), this.isHovered() || this.isFocused())
                .paint(painter, bounds);
        if (this.isFocused()) {
            content.paintFocusRing(painter, bounds);
        }
        final Insets padding = content.padding(false);
        final float innerY = bounds.y() + padding.top();
        final float innerHeight = bounds.height() - padding.vertical();
        // The content and its clip area end before the arrow
        content.paintContent(painter, this.contentArea.at(bounds.x(), bounds.y(),
                        bounds.width() - ComboBoxImpl.ARROW_GAP - ComboBoxImpl.ARROW_WIDTH, bounds.height()), padding,
                enabled, enabled ? content.caption : content.disabledCaption, this.environment().millis());

        final float arrowX = bounds.right() - padding.right() - ComboBoxImpl.ARROW_WIDTH;
        final float arrowY = innerY + (innerHeight - Rings.ARROW_SHORT) * 0.5f;
        if (enabled && this.arrow != Paint.NONE) {
            this.arrowLines.down(painter, this.arrow, arrowX, arrowY);
        } else {
            this.arrowLines.down(painter, (enabled ? content.textStyle : content.disabledTextStyle).color(), arrowX,
                    arrowY);
        }
    }

    // ------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------

    @Override
    protected void onClick(final PointerButtonEvent event) {
        if (event.button() != MouseButton.LEFT || !this.isEnabled()) {
            return;
        }
        if (this.open) {
            this.close();
        } else {
            this.userOpen(false);
        }
        event.consume();
    }

    @Override
    protected void onKey(final KeyEvent event) {
        if (event.action() != KeyAction.PRESS || !this.isEnabled()) {
            return;
        }
        final Key key = event.key();
        if (key.isEnter() || key == Key.SPACE || key == Key.DOWN && event.modifiers().alt()) {
            this.userOpen(true);
            event.consume();
        }
    }

    @Override
    protected Object snapshotState() {
        return new DropDownButton.State(this.content.text(), this.open);
    }
}
