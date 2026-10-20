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

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.event.input.KeyEvent;
import org.inspirenxe.strata.api.event.input.PointerButtonEvent;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.visual.Visual;
import org.inspirenxe.strata.core.text.TextLine;

import java.util.Objects;
import java.util.Set;

/**
 * The core {@link Button}. Its face is the disabled paint when disabled, the pressed paint while held under the
 * pointer, the hovered paint while hovered or focused, and the normal paint otherwise. A focused button also draws
 * a one-unit focus ring when the theme has one. The caption is centered.
 *
 * <p>The button is focusable and tabbable, shows a hand cursor, and is at least 20 by 20 units. Its desired size is
 * the caption plus 8 units of padding on the left and right and 5 on the top and bottom, unless that padding is
 * changed. Measuring takes the widest of the normal, hovered and disabled captions, so enabling, disabling or
 * hovering it needs no relayout.</p>
 *
 * <p>An icon is drawn {@link #ICON_GAP} units before the caption, and the two are centered together. The icon is
 * tinted half-bright gray while the button is disabled. With an icon and no caption the default padding is 2 units
 * on every side and the line height does not count, so a 16 by 16 icon gives 20 by 20.</p>
 *
 * <p>A link draws no face and no focus ring, has no default padding and no minimum size, and draws its caption in
 * the link text style. That is the hovered one while the link is hovered or focused, or else the link text with an
 * underline when the theme has no hovered link style.</p>
 */
public final class ButtonImpl extends CustomComponent<Button> implements Button {
    /**
     * The space between the icon and the caption, in UI units.
     */
    public static final float ICON_GAP = 4f;
    private static final int DISABLED_ICON_TINT = 0xFF808080;
    private static final Set<String> REQUIRED = Set.of(STYLE_NORMAL, STYLE_HOVERED, STYLE_PRESSED, STYLE_DISABLED);

    private Text text;
    private Insets textPadding;
    private SpriteId iconSprite;
    private Visual iconVisual;
    private float iconWidth;
    private float iconHeight;
    private boolean link;

    private Paint normal;
    private Paint hoveredPaint;
    private Paint pressedPaint;
    private Paint disabled;
    private Paint focusRing;
    private TextStyle textStyle;
    private TextStyle disabledTextStyle;
    private TextStyle linkTextStyle;
    private TextStyle linkHoveredTextStyle;
    private TextStyle linkDisabledTextStyle;

    // Computed during measure. Both captions are kept so enabling or disabling the button needs no relayout
    private TextLine caption;
    private TextLine disabledCaption;
    private TextLine hoveredCaption;
    private float captionWidth;
    private float captionHeight;

    /**
     * Creates a button.
     *
     * @param ui the factory that owns the button
     * @param text the caption
     */
    @SuppressWarnings("this-escape")
    public ButtonImpl(final ComponentFactory ui, final Text text) {
        super(ui);
        this.text = Objects.requireNonNull(text, "text");
        this.iconWidth = 16f;
        this.iconHeight = 16f;
        this.normal = Paint.NONE;
        this.hoveredPaint = Paint.NONE;
        this.pressedPaint = Paint.NONE;
        this.disabled = Paint.NONE;
        this.focusRing = Paint.NONE;
        this.textStyle = TextStyle.DEFAULT;
        this.disabledTextStyle = TextStyle.DEFAULT;
        this.linkTextStyle = TextStyle.DEFAULT;
        this.linkHoveredTextStyle = TextStyle.DEFAULT;
        this.linkDisabledTextStyle = TextStyle.DEFAULT;
        this.caption = TextLine.EMPTY;
        this.disabledCaption = this.caption;
        this.hoveredCaption = this.caption;
        this.setCursor(CursorKind.HAND);
        this.setMinSize(20f, 20f);
    }

    @Override
    public Text text() {
        return this.text;
    }

    @Override
    public Button setText(final Text text) {
        this.text = Objects.requireNonNull(text, "text");
        this.invalidateLayout();
        return this;
    }

    @Override
    public Button setTextPadding(final Insets padding) {
        this.textPadding = Objects.requireNonNull(padding, "padding");
        this.invalidateLayout();
        return this;
    }

    @Override
    public SpriteId iconSprite() {
        return this.iconSprite;
    }

    @Override
    public Visual iconVisual() {
        return this.iconVisual;
    }

    @Override
    public Button setIcon(final SpriteId sprite) {
        this.iconSprite = Objects.requireNonNull(sprite, "sprite");
        this.iconVisual = null;
        this.invalidateLayout();
        return this;
    }

    @Override
    public Button setIcon(final Visual visual) {
        this.iconVisual = Objects.requireNonNull(visual, "visual");
        this.iconSprite = null;
        return this.setIconSize(visual.size().width(), visual.size().height());
    }

    @Override
    public Button clearIcon() {
        this.iconSprite = null;
        this.iconVisual = null;
        this.invalidateLayout();
        return this;
    }

    @Override
    public float iconWidth() {
        return this.iconWidth;
    }

    @Override
    public float iconHeight() {
        return this.iconHeight;
    }

    @Override
    public Button setIconSize(final float width, final float height) {
        if (!(width >= 0f) || !(height >= 0f) || Float.isInfinite(width) || Float.isInfinite(height)) {
            throw new IllegalArgumentException("Icon size must be finite and not negative: " + width + " x " + height);
        }
        this.iconWidth = width;
        this.iconHeight = height;
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
            this.setMinSize(20f, 20f);
        }
        this.invalidateLayout();
        return this;
    }

    private boolean hasIcon() {
        return this.iconSprite != null || this.iconVisual != null;
    }

    /**
     * Returns the padding to measure with: the one set, or the default for what the button shows.
     */
    private Insets padding() {
        if (this.textPadding != null) {
            return this.textPadding;
        }
        if (this.link) {
            return Insets.NONE;
        }
        return this.hasIcon() && this.caption.isEmpty() ? Insets.of(2f, 2f) : Insets.of(8f, 5f);
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
        return REQUIRED;
    }

    @Override
    protected void onApplyTheme(final Theme theme) {
        this.normal = theme.paintOr(STYLE_NORMAL, Paint.NONE);
        this.hoveredPaint = theme.paintOr(STYLE_HOVERED, this.normal);
        this.pressedPaint = theme.paintOr(STYLE_PRESSED, this.hoveredPaint);
        this.disabled = theme.paintOr(STYLE_DISABLED, this.normal);
        this.focusRing = theme.paintOr(STYLE_FOCUS, Paint.NONE);
        this.textStyle = theme.textStyleOr(STYLE_TEXT, TextStyle.DEFAULT);
        this.disabledTextStyle = theme.textStyleOr(STYLE_TEXT_DISABLED, this.textStyle);
        this.linkTextStyle = theme.textStyleOr(STYLE_LINK_TEXT, this.textStyle);
        this.linkHoveredTextStyle = theme.textStyleOr(STYLE_LINK_TEXT_HOVERED, this.linkTextStyle.withUnderline(true));
        this.linkDisabledTextStyle = theme.textStyleOr(STYLE_LINK_TEXT_DISABLED, this.disabledTextStyle);
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        final TextMetrics metrics = context.text();
        final TextStyle normalStyle = this.link ? this.linkTextStyle : this.textStyle;
        final TextStyle hoveredStyle = this.link ? this.linkHoveredTextStyle : this.textStyle;
        final TextStyle disabledStyle = this.link ? this.linkDisabledTextStyle : this.disabledTextStyle;
        this.caption = TextLine.of(this.text, normalStyle, metrics);
        this.hoveredCaption = TextLine.of(this.text, hoveredStyle, metrics);
        this.disabledCaption = TextLine.of(this.text, disabledStyle, metrics);
        this.captionWidth = Math.max(this.caption.width(),
                Math.max(this.hoveredCaption.width(), this.disabledCaption.width()));
        this.captionHeight = Math.max(this.caption.height(),
                Math.max(this.hoveredCaption.height(), this.disabledCaption.height()));
        final boolean icon = this.hasIcon();
        final boolean iconOnly = icon && this.caption.isEmpty();
        final float contentWidth = (icon ? this.iconWidth : 0f) + (icon && !this.caption.isEmpty() ? ICON_GAP : 0f)
                + this.captionWidth;
        final float contentHeight = iconOnly ? this.iconHeight : Math.max(this.captionHeight, icon ? this.iconHeight : 0f);
        final Insets padding = this.padding();
        return new Size(contentWidth + padding.horizontal(), contentHeight + padding.vertical());
    }

    @Override
    protected void paint(final Painter painter, final Rect bounds) {
        final boolean enabled = this.isEnabled();
        final boolean lit = this.isHovered() || this.isFocused();
        if (!this.link) {
            final Paint face;
            if (!enabled) {
                face = this.disabled;
            } else if (this.isPressed() && this.isHovered()) {
                face = this.pressedPaint;
            } else if (lit) {
                face = this.hoveredPaint;
            } else {
                face = this.normal;
            }
            face.paint(painter, bounds);

            if (this.isFocused() && this.focusRing != Paint.NONE) {
                this.focusRing.paint(painter, new Rect(bounds.x(), bounds.y(), bounds.width(), 1f));
                this.focusRing.paint(painter, new Rect(bounds.x(), bounds.bottom() - 1f, bounds.width(), 1f));
                this.focusRing.paint(painter, new Rect(bounds.x(), bounds.y() + 1f, 1f, bounds.height() - 2f));
                this.focusRing.paint(painter, new Rect(bounds.right() - 1f, bounds.y() + 1f, 1f, bounds.height() - 2f));
            }
        }

        final boolean icon = this.hasIcon();
        final boolean hasText = !this.caption.isEmpty();
        final float contentWidth = (icon ? this.iconWidth : 0f) + (icon && hasText ? ICON_GAP : 0f)
                + (hasText ? this.captionWidth : 0f);
        float x = bounds.x() + (bounds.width() - contentWidth) * 0.5f;
        if (icon) {
            final Rect iconRect = new Rect(x, bounds.y() + (bounds.height() - this.iconHeight) * 0.5f, this.iconWidth,
                    this.iconHeight);
            final int tint = enabled ? 0xFFFFFFFF : DISABLED_ICON_TINT;
            if (this.iconVisual != null) {
                painter.visual(this.iconVisual, iconRect, tint);
            } else {
                painter.sprite(this.iconSprite, iconRect, tint);
            }
            x += this.iconWidth + ICON_GAP;
        }
        if (hasText) {
            final TextLine shown;
            if (!enabled) {
                shown = this.disabledCaption;
            } else if (this.link && lit) {
                shown = this.hoveredCaption;
            } else {
                shown = this.caption;
            }
            shown.draw(painter, x, bounds.y() + (bounds.height() - this.captionHeight) * 0.5f);
        }
    }

    @Override
    protected void onClick(final PointerButtonEvent event) {
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
        return new Button.State(this.text);
    }
}
