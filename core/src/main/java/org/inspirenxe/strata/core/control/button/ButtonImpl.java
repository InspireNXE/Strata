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

import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.control.button.Button;
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
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.visual.Visual;
import org.inspirenxe.strata.core.node.BuiltInComponent;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;
import org.inspirenxe.strata.core.text.TextLine;

import java.util.Set;

/**
 * The core {@link Button}. Its face is the disabled paint when disabled, the pressed paint while held under the
 * pointer, the hovered paint while hovered or focused, and the normal paint otherwise. A focused button also draws
 * a one-unit focus ring when the theme has one. The caption is centered.
 *
 * <p>The button is focusable and tabbable, shows a hand cursor, is at least 20 units wide, and measures at least 20
 * tall, though a smaller height set on it applies. Its desired size is
 * the caption plus its padding: the one set with {@link #setTextPadding}, or else the theme's
 * {@value #STYLE_PADDING}, or else {@link #DEFAULT_PADDING}. Measuring takes the widest of the normal, hovered and
 * disabled captions, so enabling, disabling or hovering it needs no relayout.</p>
 *
 * <p>An icon is drawn {@link #ICON_GAP} units before the caption, and the two are centered together in the button
 * less its padding. The icon is tinted half-bright gray while the button is disabled. With an icon and no caption
 * the padding is the theme's {@value #STYLE_PADDING_ICON}, or else {@link #DEFAULT_ICON_PADDING}, and the line
 * height does not count, so a 16 by 16 icon gives 20 by 20.</p>
 *
 * <p>A link draws no face and no focus ring, has no default padding and no minimum size, and draws its caption in
 * the link text style. While the link is hovered or focused it uses the hovered link style, which is the link text
 * style underlined when the theme sets none.</p>
 *
 * <p>The keys, the caption and the icon, and how they measure and draw, live in a helper that
 * {@link DropDownButtonImpl} shares, so the two buttons look and measure alike.</p>
 */
public final class ButtonImpl extends BuiltInComponent<Button> implements Button {
    /**
     * The space between the icon and the caption, in UI units.
     */
    public static final float ICON_GAP = 4f;
    /**
     * The padding around a caption when the theme sets no {@value #STYLE_PADDING}: 8 at the sides and 5 at the top
     * and bottom.
     */
    public static final Insets DEFAULT_PADDING = Insets.of(8f, 5f);
    /**
     * The padding around an icon with no caption when the theme sets no {@value #STYLE_PADDING_ICON}: 2 on every
     * side.
     */
    public static final Insets DEFAULT_ICON_PADDING = Insets.of(2f);
    /**
     * The least height a framed button, a cycle button, a drop-down button and the one-line text fields measure,
     * in UI units. A smaller height set on one applies.
     */
    public static final float MIN_HEIGHT = 20f;

    private final ButtonContent content;
    private boolean link;

    /**
     * Creates a button.
     *
     * @param ui the factory that owns the button
     * @param text the caption
     */
    @SuppressWarnings("this-escape")
    public ButtonImpl(final ComponentFactoryImpl ui, final Text text) {
        super(ui);
        this.content = new ButtonContent(text);
        this.setCursor(CursorKind.HAND);
        this.setMinSize(20f, 0f);
    }

    @Override
    public Text text() {
        return this.content.text();
    }

    @Override
    public Button setText(final Text text) {
        this.content.setText(text);
        this.invalidateLayout();
        return this;
    }

    @Override
    public Button setTextPadding(final Insets padding) {
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
    public Button setIcon(final SpriteId sprite) {
        this.content.setIcon(sprite);
        this.invalidateLayout();
        return this;
    }

    @Override
    public Button setIcon(final Visual visual) {
        this.content.setIcon(visual);
        this.invalidateLayout();
        return this;
    }

    @Override
    public Button clearIcon() {
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
    public Button setIconSize(final float width, final float height) {
        this.content.setIconSize(width, height);
        this.invalidateLayout();
        return this;
    }

    @Override
    public boolean isLink() {
        return this.link;
    }

    @Override
    public Button setLink(final boolean link) {
        this.link = link;
        if (link) {
            this.setMinSize(0f, 0f);
        } else {
            this.setMinSize(20f, 0f);
        }
        this.invalidateLayout();
        return this;
    }

    @Override
    public void press() {
        if (this.isEnabled()) {
            this.report(PRESSED);
        }
    }

    /**
     * Presses the button for the user: fires {@link Button#PRESS}, then {@link Button#PRESSED} unless a handler
     * canceled the request. Does nothing while disabled.
     */
    private void userPress() {
        if (this.isEnabled() && this.request(PRESS)) {
            this.report(PRESSED);
        }
    }

    @Override
    protected Set<String> requiredStyleKeys() {
        return ButtonContent.REQUIRED;
    }

    @Override
    protected void onApplyTheme(final Theme theme) {
        this.content.applyTheme(theme);
    }

    @Override
    protected Size measureMinimum(final MeasureContext context) {
        // The caption does not shrink, and measuring does not depend on the space offered
        return this.measure(context, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        this.content.measure(context.text(), this.link);
        final Insets padding = this.content.padding(this.link);
        final float height = this.content.contentHeight() + padding.vertical();
        return new Size(this.content.contentWidth() + padding.horizontal(),
                this.link ? height : Math.max(MIN_HEIGHT, height));
    }

    @Override
    protected void paint(final Painter painter, final Rect bounds) {
        final ButtonContent content = this.content;
        final boolean enabled = this.isEnabled();
        final boolean lit = this.isHovered() || this.isFocused();
        if (!this.link) {
            content.face(enabled, this.isPressed() && this.isHovered(), lit).paint(painter, bounds);
            if (this.isFocused()) {
                content.paintFocusRing(painter, bounds);
            }
        }
        final TextLine shown;
        if (!enabled) {
            shown = content.disabledCaption;
        } else if (this.link && lit) {
            shown = content.hoveredCaption;
        } else {
            shown = content.caption;
        }
        // Centered in the button less its padding, which is the whole button when the padding is even
        content.paintContent(painter, bounds, content.padding(this.link), enabled, shown, this.environment().millis());
    }

    @Override
    protected void onPointerPress(final PointerButtonEvent event) {
        if (this.activatesOnPress()) {
            this.clicked(event);
        }
    }

    @Override
    protected void onClick(final PointerButtonEvent event) {
        if (!this.activatesOnPress()) {
            this.clicked(event);
        }
    }

    /**
     * Acts on a pointer press or click, whichever the platform's conventions say buttons act on.
     */
    private void clicked(final PointerButtonEvent event) {
        if (event.button() == MouseButton.LEFT) {
            this.userPress();
            event.consume();
        }
    }

    @Override
    protected void onKey(final KeyEvent event) {
        if (event.action() == KeyAction.PRESS && (event.key().isEnter() || event.key() == Key.SPACE)) {
            this.userPress();
            event.consume();
        }
    }

    @Override
    protected Object snapshotState() {
        return new Button.State(this.content.text());
    }
}
