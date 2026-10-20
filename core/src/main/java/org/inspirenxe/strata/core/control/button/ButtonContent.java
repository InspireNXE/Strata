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

import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.visual.Visual;
import org.inspirenxe.strata.core.control.Rings;
import org.inspirenxe.strata.core.text.TextLine;

import java.util.Objects;
import java.util.Set;

/**
 * What {@link ButtonImpl} and {@link DropDownButtonImpl} share: the {@code button.*} keys they read, the caption,
 * the icon and the padding, how the caption and icon measure, and how they draw. Each button keeps one and calls it
 * from its own hooks, so the two measure alike and follow the same padding keys.
 *
 * <p>The caption is resolved during measure in three styles, normal, hovered and disabled, and the widest of the
 * three counts, so hovering, enabling or disabling needs no new layout. An icon is drawn {@link ButtonImpl#ICON_GAP}
 * units before the caption, and the two are centered together in the area they are given. The padding is the one set
 * with {@link #setTextPadding}, or else none for a link, the theme's {@value Button#STYLE_PADDING_ICON} for an icon
 * with no caption, and the theme's {@value Button#STYLE_PADDING} otherwise.</p>
 */
final class ButtonContent {
    /**
     * The keys every button requires: its four faces.
     */
    static final Set<String> REQUIRED = Set.of(Button.STYLE_NORMAL, Button.STYLE_HOVERED, Button.STYLE_PRESSED,
            Button.STYLE_DISABLED);
    private static final int DISABLED_ICON_TINT = 0xFF808080;

    private Text text;
    // Set with setTextPadding; null follows the theme
    private Insets textPadding;
    private Insets themePadding = ButtonImpl.DEFAULT_PADDING;
    private Insets themeIconPadding = ButtonImpl.DEFAULT_ICON_PADDING;
    private SpriteId iconSprite;
    private Visual iconVisual;
    private float iconWidth;
    private float iconHeight;

    // Theme
    Paint normal;
    Paint hovered;
    Paint pressed;
    Paint disabled;
    Paint focusRing;
    TextStyle textStyle;
    TextStyle disabledTextStyle;
    TextStyle linkTextStyle;
    TextStyle linkHoveredTextStyle;
    TextStyle linkDisabledTextStyle;

    // Computed during measure. All three captions are kept so a change of state needs no relayout
    TextLine caption;
    TextLine hoveredCaption;
    TextLine disabledCaption;
    private float captionWidth;
    private float captionHeight;

    /**
     * Creates the content of a button with a caption, no icon and the theme's padding.
     *
     * @param text the caption
     */
    ButtonContent(final Text text) {
        this.text = Objects.requireNonNull(text, "text");
        this.iconWidth = 16f;
        this.iconHeight = 16f;
        this.normal = Paint.NONE;
        this.hovered = Paint.NONE;
        this.pressed = Paint.NONE;
        this.disabled = Paint.NONE;
        this.focusRing = Paint.NONE;
        this.textStyle = TextStyle.DEFAULT;
        this.disabledTextStyle = TextStyle.DEFAULT;
        this.linkTextStyle = TextStyle.DEFAULT;
        this.linkHoveredTextStyle = TextStyle.DEFAULT;
        this.linkDisabledTextStyle = TextStyle.DEFAULT;
        this.caption = TextLine.EMPTY;
        this.hoveredCaption = this.caption;
        this.disabledCaption = this.caption;
    }

    // ------------------------------------------------------------------
    // Settings
    // ------------------------------------------------------------------

    Text text() {
        return this.text;
    }

    void setText(final Text text) {
        this.text = Objects.requireNonNull(text, "text");
    }

    void setTextPadding(final Insets padding) {
        this.textPadding = padding;
    }

    SpriteId iconSprite() {
        return this.iconSprite;
    }

    Visual iconVisual() {
        return this.iconVisual;
    }

    void setIcon(final SpriteId sprite) {
        this.iconSprite = Objects.requireNonNull(sprite, "sprite");
        this.iconVisual = null;
    }

    /**
     * Shows a visual at its natural size.
     */
    void setIcon(final Visual visual) {
        this.iconVisual = Objects.requireNonNull(visual, "visual");
        this.iconSprite = null;
        this.setIconSize(visual.size().width(), visual.size().height());
    }

    void clearIcon() {
        this.iconSprite = null;
        this.iconVisual = null;
    }

    float iconWidth() {
        return this.iconWidth;
    }

    float iconHeight() {
        return this.iconHeight;
    }

    /**
     * Sets the size the icon is drawn at.
     *
     * @throws IllegalArgumentException if either size is negative, infinite or not a number
     */
    void setIconSize(final float width, final float height) {
        if (!(width >= 0f) || !(height >= 0f) || Float.isInfinite(width) || Float.isInfinite(height)) {
            throw new IllegalArgumentException("Icon size must be finite and not negative: " + width + " x " + height);
        }
        this.iconWidth = width;
        this.iconHeight = height;
    }

    boolean hasIcon() {
        return this.iconSprite != null || this.iconVisual != null;
    }

    /**
     * Returns the padding to measure and draw with: the one set, or the default for what the button shows. Whether
     * the caption is empty is known from the last measure.
     */
    Insets padding(final boolean link) {
        if (this.textPadding != null) {
            return this.textPadding;
        }
        if (link) {
            return Insets.NONE;
        }
        return this.hasIcon() && this.caption.isEmpty() ? this.themeIconPadding : this.themePadding;
    }

    // ------------------------------------------------------------------
    // Theme and measuring
    // ------------------------------------------------------------------

    /**
     * Reads every {@code button.*} key, with the fallbacks {@link Button} documents.
     */
    void applyTheme(final Theme theme) {
        this.normal = theme.paintOr(Button.STYLE_NORMAL, Paint.NONE);
        this.hovered = theme.paintOr(Button.STYLE_HOVERED, this.normal);
        this.pressed = theme.paintOr(Button.STYLE_PRESSED, this.hovered);
        this.disabled = theme.paintOr(Button.STYLE_DISABLED, this.normal);
        this.focusRing = theme.paintOr(Button.STYLE_FOCUS, Paint.NONE);
        this.textStyle = theme.textStyleOr(Button.STYLE_TEXT, TextStyle.DEFAULT);
        this.disabledTextStyle = theme.textStyleOr(Button.STYLE_TEXT_DISABLED, this.textStyle);
        this.linkTextStyle = theme.textStyleOr(Button.STYLE_LINK_TEXT, this.textStyle);
        this.linkHoveredTextStyle = theme.textStyleOr(Button.STYLE_LINK_TEXT_HOVERED,
                this.linkTextStyle.withUnderline(true));
        this.linkDisabledTextStyle = theme.textStyleOr(Button.STYLE_LINK_TEXT_DISABLED, this.disabledTextStyle);
        this.themePadding = theme.insetsOr(Button.STYLE_PADDING, ButtonImpl.DEFAULT_PADDING);
        this.themeIconPadding = theme.insetsOr(Button.STYLE_PADDING_ICON, ButtonImpl.DEFAULT_ICON_PADDING);
    }

    /**
     * Resolves the caption in the button or link styles and measures it.
     */
    void measure(final TextMetrics metrics, final boolean link) {
        final TextStyle normalStyle = link ? this.linkTextStyle : this.textStyle;
        final TextStyle hoveredStyle = link ? this.linkHoveredTextStyle : this.textStyle;
        final TextStyle disabledStyle = link ? this.linkDisabledTextStyle : this.disabledTextStyle;
        this.caption = TextLine.of(this.text, normalStyle, metrics);
        this.hoveredCaption = TextLine.of(this.text, hoveredStyle, metrics);
        this.disabledCaption = TextLine.of(this.text, disabledStyle, metrics);
        this.captionWidth = Math.max(this.caption.width(),
                Math.max(this.hoveredCaption.width(), this.disabledCaption.width()));
        this.captionHeight = Math.max(this.caption.height(),
                Math.max(this.hoveredCaption.height(), this.disabledCaption.height()));
    }

    /**
     * Returns the width of the icon and caption together, as of the last measure.
     */
    float contentWidth() {
        final boolean icon = this.hasIcon();
        final boolean hasText = !this.caption.isEmpty();
        return (icon ? this.iconWidth : 0f) + (icon && hasText ? ButtonImpl.ICON_GAP : 0f)
                + (hasText ? this.captionWidth : 0f);
    }

    /**
     * Returns the height of the icon and caption together, as of the last measure. An icon with no caption counts
     * alone, so the line height does not.
     */
    float contentHeight() {
        final boolean icon = this.hasIcon();
        if (icon && this.caption.isEmpty()) {
            return this.iconHeight;
        }
        return Math.max(this.captionHeight, icon ? this.iconHeight : 0f);
    }

    // ------------------------------------------------------------------
    // Painting
    // ------------------------------------------------------------------

    /**
     * Returns the face for a state: disabled, pressed, lit (hovered or focused) or idle, in that order.
     */
    Paint face(final boolean enabled, final boolean pressed, final boolean lit) {
        if (!enabled) {
            return this.disabled;
        }
        if (pressed) {
            return this.pressed;
        }
        return lit ? this.hovered : this.normal;
    }

    /**
     * Draws the focus ring one unit inside the bounds, when the theme has one.
     */
    void paintFocusRing(final Painter painter, final Rect bounds) {
        if (this.focusRing != Paint.NONE) {
            Rings.paint(painter, this.focusRing, bounds);
        }
    }

    /**
     * Draws the icon and a caption centered together in an area: horizontally across it, and each vertically in it.
     *
     * @param painter what to draw with
     * @param left the area's left edge
     * @param top the area's top edge
     * @param width the area's width
     * @param height the area's height
     * @param enabled false to tint the icon gray
     * @param shown the caption to draw, one of the three this content measured
     */
    void paintContent(final Painter painter, final float left, final float top, final float width, final float height,
                      final boolean enabled, final TextLine shown) {
        final boolean icon = this.hasIcon();
        final boolean hasText = !this.caption.isEmpty();
        float x = left + (width - this.contentWidth()) * 0.5f;
        if (icon) {
            final Rect iconRect = new Rect(x, top + (height - this.iconHeight) * 0.5f, this.iconWidth, this.iconHeight);
            final int tint = enabled ? 0xFFFFFFFF : DISABLED_ICON_TINT;
            if (this.iconVisual != null) {
                painter.visual(this.iconVisual, iconRect, tint);
            } else {
                painter.sprite(this.iconSprite, iconRect, tint);
            }
            x += this.iconWidth + ButtonImpl.ICON_GAP;
        }
        if (hasText) {
            shown.draw(painter, x, top + (height - this.captionHeight) * 0.5f);
        }
    }
}
