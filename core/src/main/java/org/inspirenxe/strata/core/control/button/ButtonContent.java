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
import org.inspirenxe.strata.api.control.button.IconPlacement;
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
import org.inspirenxe.strata.core.control.Caption;
import org.inspirenxe.strata.core.control.RectCache;
import org.inspirenxe.strata.core.control.Rings;
import org.inspirenxe.strata.core.text.MnemonicLine;
import org.inspirenxe.strata.core.text.Mnemonics;
import org.inspirenxe.strata.core.text.TextLine;

import java.util.Objects;
import java.util.Set;

/**
 * What {@link ButtonImpl} and {@link DropDownButtonImpl} share: the {@code button.*} keys they read, the caption,
 * the icon and the padding, how the caption and icon measure, and how they draw. Each button keeps one and calls it
 * from its own hooks, so the two measure alike and follow the same padding keys.
 *
 * <p>The caption is resolved during measure in three styles, normal, hovered and disabled, and the widest of the
 * three counts, so hovering, enabling or disabling needs no new layout. The caption's mnemonic marker is taken out,
 * and each of the three is also kept with the mnemonic underlined, for drawing while mnemonics show. An icon is drawn {@link ButtonImpl#ICON_GAP}
 * units before the caption, or after, above or below it as its {@link IconPlacement} says, and the two are centered
 * together in the area they are given. The padding is the one set
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
    private IconPlacement placement = IconPlacement.START;
    private final Caption captionDrawer = new Caption();
    private final RectCache iconRect = new RectCache();
    private final RectCache captionArea = new RectCache();
    private final Rings.Ring ring = new Rings.Ring();

    // Theme
    Paint normal;
    Paint hovered;
    Paint pressed;
    Paint disabled;
    Paint focusRing;
    Paint defaultFace;
    TextStyle textStyle;
    TextStyle disabledTextStyle;
    TextStyle linkTextStyle;
    TextStyle linkHoveredTextStyle;
    TextStyle linkDisabledTextStyle;

    // Computed during measure. All three captions are kept so a change of state needs no relayout
    TextLine caption;
    TextLine hoveredCaption;
    TextLine disabledCaption;
    // The same three with the mnemonic underlined, drawn while mnemonics show
    private TextLine markedCaption;
    private TextLine markedHoveredCaption;
    private TextLine markedDisabledCaption;
    private int mnemonic = Mnemonics.NONE;
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
        this.defaultFace = Paint.NONE;
        this.textStyle = TextStyle.DEFAULT;
        this.disabledTextStyle = TextStyle.DEFAULT;
        this.linkTextStyle = TextStyle.DEFAULT;
        this.linkHoveredTextStyle = TextStyle.DEFAULT;
        this.linkDisabledTextStyle = TextStyle.DEFAULT;
        this.caption = TextLine.EMPTY;
        this.hoveredCaption = this.caption;
        this.disabledCaption = this.caption;
        this.markedCaption = this.caption;
        this.markedHoveredCaption = this.caption;
        this.markedDisabledCaption = this.caption;
    }

    /**
     * Returns the caption's mnemonic as of the last measure.
     *
     * @return the mnemonic in lower case, or {@link Mnemonics#NONE}
     */
    int mnemonic() {
        return this.mnemonic;
    }

    /**
     * Returns one of the three captions with its mnemonic underlined, for drawing while mnemonics show.
     *
     * @param line the caption, one of the three this content measured
     * @return the same caption marked
     */
    TextLine marked(final TextLine line) {
        if (line == this.caption) {
            return this.markedCaption;
        }
        if (line == this.hoveredCaption) {
            return this.markedHoveredCaption;
        }
        return line == this.disabledCaption ? this.markedDisabledCaption : line;
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

    IconPlacement placement() {
        return this.placement;
    }

    void setPlacement(final IconPlacement placement) {
        this.placement = Objects.requireNonNull(placement, "placement");
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
        this.defaultFace = theme.paintOr(Button.STYLE_DEFAULT, this.normal);
        this.textStyle = theme.textStyleOr(Button.STYLE_TEXT, TextStyle.DEFAULT);
        this.disabledTextStyle = theme.textStyleOr(Button.STYLE_TEXT_DISABLED, this.textStyle);
        this.linkTextStyle = theme.textStyleOr(Button.STYLE_LINK_TEXT, this.textStyle);
        this.linkHoveredTextStyle = theme.textStyleOr(Button.STYLE_LINK_TEXT_HOVERED,
                this.linkTextStyle.withUnderline(true));
        this.linkDisabledTextStyle = theme.textStyleOr(Button.STYLE_LINK_TEXT_DISABLED, this.disabledTextStyle);
        this.themePadding = theme.insetsOr(Button.STYLE_PADDING, ButtonImpl.DEFAULT_PADDING);
        this.themeIconPadding = theme.insetsOr(Button.STYLE_PADDING_ICON, ButtonImpl.DEFAULT_ICON_PADDING);
        this.captionDrawer.applyTheme(theme, Button.STYLE_CLIP, Button.STYLE_TEXT_SCROLL,
                Button.STYLE_TEXT_SCROLL_MINIMUM);
    }

    /**
     * Resolves the caption in the button or link styles and measures it.
     */
    void measure(final TextMetrics metrics, final boolean link) {
        final TextStyle normalStyle = link ? this.linkTextStyle : this.textStyle;
        final TextStyle hoveredStyle = link ? this.linkHoveredTextStyle : this.textStyle;
        final TextStyle disabledStyle = link ? this.linkDisabledTextStyle : this.disabledTextStyle;
        final MnemonicLine normalLine = MnemonicLine.of(this.text, normalStyle, metrics);
        final MnemonicLine hoveredLine = MnemonicLine.of(this.text, hoveredStyle, metrics);
        final MnemonicLine disabledLine = MnemonicLine.of(this.text, disabledStyle, metrics);
        this.caption = normalLine.plain();
        this.hoveredCaption = hoveredLine.plain();
        this.disabledCaption = disabledLine.plain();
        this.markedCaption = normalLine.marked();
        this.markedHoveredCaption = hoveredLine.marked();
        this.markedDisabledCaption = disabledLine.marked();
        this.mnemonic = normalLine.mnemonic();
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
        if (icon && hasText && this.placement.isVertical()) {
            return Math.max(this.iconWidth, this.captionWidth);
        }
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
        if (icon && this.placement.isVertical()) {
            return this.iconHeight + ButtonImpl.ICON_GAP + this.captionHeight;
        }
        return Math.max(this.captionHeight, icon ? this.iconHeight : 0f);
    }

    /**
     * Returns the caption's top below the content's top, which is 0 unless the icon is stacked above it.
     */
    private float captionOffset() {
        return this.hasIcon() && this.placement == IconPlacement.TOP ? this.iconHeight + ButtonImpl.ICON_GAP : 0f;
    }

    /**
     * Returns the caption's baseline in a button of a given height, as {@link #paintContent} places the caption.
     *
     * @param height the button's height
     * @param padding the padding
     * @return the baseline below the button's top, or NaN when there is no caption
     */
    float baseline(final float height, final Insets padding) {
        if (this.caption.isEmpty()) {
            return Float.NaN;
        }
        if (this.hasIcon() && this.placement.isVertical()) {
            return padding.top() + (height - padding.vertical() - this.contentHeight()) * 0.5f + this.captionOffset()
                    + this.caption.baseline();
        }
        return padding.top() + (height - padding.vertical() - this.captionHeight) * 0.5f + this.caption.baseline();
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
            this.ring.paint(painter, this.focusRing, bounds);
        }
    }

    /**
     * Draws the icon and a caption centered together in the bounds less the padding: horizontally across it, and
     * each vertically in it. The caption is drawn by a {@link Caption}, so one too wide is cut or scrolled as the
     * theme says; beside an icon, the icon starts at the clip area's left edge and the caption is cut after it. Right
     * to left the piece is mirrored: the icon sits at its right end and the caption before it.
     *
     * @param painter what to draw with
     * @param bounds the bounds to center in, less anything the button draws beside its content
     * @param padding the padding
     * @param enabled false to tint the icon gray
     * @param shown the caption to draw, one of the three this content measured
     * @param millis the platform's clock, for a scrolling caption
     * @param rightToLeft whether the button runs right to left
     * @return true if the caption slides, so the button should ask for another frame
     */
    boolean paintContent(final Painter painter, final Rect bounds, final Insets padding, final boolean enabled,
                         final TextLine shown, final long millis, final boolean rightToLeft) {
        final boolean icon = this.hasIcon();
        final boolean hasText = !this.caption.isEmpty();
        final float left = bounds.x() + padding.left();
        final float top = bounds.y() + padding.top();
        final float width = bounds.width() - padding.horizontal();
        final float height = bounds.height() - padding.vertical();
        if (!icon) {
            return hasText && this.captionDrawer.draw(painter, shown, bounds, padding,
                    top + (height - this.captionHeight) * 0.5f, millis);
        }
        final int tint = enabled ? 0xFFFFFFFF : DISABLED_ICON_TINT;
        if (!hasText) {
            this.paintIcon(painter, Caption.center(left, width, this.iconWidth),
                    top + (height - this.iconHeight) * 0.5f, tint);
            return false;
        }
        // Right to left, an icon before the caption sits at its right end, and one after it at its left
        IconPlacement placement = this.placement;
        if (rightToLeft && placement == IconPlacement.START) {
            placement = IconPlacement.END;
        } else if (rightToLeft && placement == IconPlacement.END) {
            placement = IconPlacement.START;
        }
        switch (placement) {
            case TOP, BOTTOM -> {
                final float blockTop = top + (height - this.contentHeight()) * 0.5f;
                final boolean above = placement == IconPlacement.TOP;
                this.paintIcon(painter, Caption.center(left, width, this.iconWidth),
                        above ? blockTop : blockTop + this.captionHeight + ButtonImpl.ICON_GAP, tint);
                return this.captionDrawer.draw(painter, shown, bounds, padding, blockTop + this.captionOffset(), millis);
            }
            case END -> {
                // The caption centers and is cut in the bounds less the icon and its gap, and the icon follows it
                final float lead = this.iconWidth + ButtonImpl.ICON_GAP;
                final Rect area = this.captionArea.at(bounds.x(), bounds.y(), Math.max(0f, bounds.width() - lead),
                        bounds.height());
                final float start = this.captionDrawer.start(area, padding, this.captionWidth);
                final float end = Math.min(start + this.captionWidth, this.captionDrawer.clipRight(area, padding));
                this.paintIcon(painter, end + ButtonImpl.ICON_GAP, top + (height - this.iconHeight) * 0.5f, tint);
                return this.captionDrawer.draw(painter, shown, area, padding, top + (height - this.captionHeight) * 0.5f,
                        millis);
            }
            default -> {
                final float contentWidth = this.contentWidth();
                this.paintIcon(painter, this.captionDrawer.start(bounds, padding, contentWidth),
                        top + (height - this.iconHeight) * 0.5f, tint);
                return this.captionDrawer.draw(painter, shown, bounds, padding, this.iconWidth + ButtonImpl.ICON_GAP,
                        contentWidth, top + (height - this.captionHeight) * 0.5f, millis);
            }
        }
    }

    private void paintIcon(final Painter painter, final float x, final float y, final int tint) {
        final Rect iconRect = this.iconRect.at(x, y, this.iconWidth, this.iconHeight);
        if (this.iconVisual != null) {
            painter.visual(this.iconVisual, iconRect, tint);
        } else {
            painter.sprite(this.iconSprite, iconRect, tint);
        }
    }
}
