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
package org.inspirenxe.strata.core.control.display;

import org.inspirenxe.strata.api.Strata;
import org.inspirenxe.strata.api.component.Tooltip;
import org.inspirenxe.strata.api.control.display.Avatar;
import org.inspirenxe.strata.api.event.input.PointerEvent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.visual.Visual;
import org.inspirenxe.strata.core.control.Badge;
import org.inspirenxe.strata.core.node.BuiltInComponent;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;
import org.inspirenxe.strata.core.text.TextLine;
import org.inspirenxe.strata.core.text.Translations;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * The core {@link Avatar}, a component with no children. Measuring works out the initials, makes their
 * {@link TextLine} and measures the badge; painting works out the square, the initials' scale and place and the
 * badge's rectangle when the bounds change, and keeps them until the next change, so a frame allocates nothing.
 *
 * <p>The value's icon and name are read from its appearance when the value is set. The initials are read again on
 * every measure, so a translated name follows the language, since the host lays every component out again when the
 * translations change.</p>
 */
public final class AvatarImpl extends BuiltInComponent<Avatar> implements Avatar {
    private static final Set<String> REQUIRED = Set.of(STYLE_BACKGROUND);
    private static final int WHITE = 0xFFFFFFFF;

    // What the avatar shows
    private Object value;
    private Visual valueIcon;
    private Text valueName;
    private Tooltip valueTooltip;
    private SpriteId sprite;
    private Visual visual;
    private Text name;
    private String initials;
    private boolean group;
    private float size;
    private final Badge badge;
    private Visual badgeVisual;

    // Theme
    private Paint background;
    private Paint frame;
    private Paint placeholder;
    private Paint groupPlaceholder;
    private TextStyle initialsStyle;

    // Computed during measure
    private TextLine initialsLine;
    private float lineHeight;

    // Worked out for the last bounds painted
    private Rect placedFor;
    private Rect square;
    private Rect badgeRect;
    private Rect badgeVisualRect;
    private float scale;
    private float initialsX;
    private float initialsY;

    // Set only while paint runs, for drawInitials
    private final Runnable drawInitials;
    private Painter painter;

    /**
     * Creates an avatar that shows its placeholder, {@link #DEFAULT_SIZE} on a side.
     *
     * @param ui the factory that owns the avatar
     */
    public AvatarImpl(final ComponentFactoryImpl ui) {
        super(ui);
        this.name = Text.empty();
        this.size = DEFAULT_SIZE;
        this.badge = new Badge();
        this.background = Paint.NONE;
        this.frame = Paint.NONE;
        this.placeholder = Paint.NONE;
        this.groupPlaceholder = Paint.NONE;
        this.initialsStyle = TextStyle.DEFAULT;
        this.initialsLine = TextLine.EMPTY;
        this.scale = 1f;
        this.drawInitials = this::drawInitials;
    }

    /**
     * Works out initials from a name: the first letter of the first word and the first letter of the last word,
     * upper cased for a locale. Words are runs between whitespace or space characters, and words with no letter are
     * skipped. Characters are whole code points; upper casing may turn one into several, as German {@code ß} becomes
     * {@code SS}.
     *
     * @param name the name's characters
     * @param locale the locale to upper case for
     * @return the initials: none, one or two letters
     */
    public static String initialsOf(final String name, final Locale locale) {
        int first = -1;
        int last = -1;
        int i = 0;
        while (i < name.length()) {
            int codePoint = name.codePointAt(i);
            if (AvatarImpl.isSpace(codePoint)) {
                i += Character.charCount(codePoint);
                continue;
            }
            int letter = -1;
            while (i < name.length()) {
                codePoint = name.codePointAt(i);
                if (AvatarImpl.isSpace(codePoint)) {
                    break;
                }
                if (letter < 0 && Character.isLetter(codePoint)) {
                    letter = codePoint;
                }
                i += Character.charCount(codePoint);
            }
            if (letter >= 0) {
                if (first < 0) {
                    first = letter;
                } else {
                    last = letter;
                }
            }
        }
        if (first < 0) {
            return "";
        }
        final String start = Character.toString(first).toUpperCase(locale);
        return last < 0 ? start : start + Character.toString(last).toUpperCase(locale);
    }

    private static boolean isSpace(final int codePoint) {
        return Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint);
    }

    // ------------------------------------------------------------------
    // What it shows
    // ------------------------------------------------------------------

    @Override
    public Optional<Object> value() {
        return Optional.ofNullable(this.value);
    }

    @Override
    public Avatar setValue(final Object value) {
        this.value = value;
        this.valueIcon = value == null ? null : Strata.appearances().iconOf(value).orElse(null);
        this.valueName = value == null ? null : Strata.appearances().nameOf(value);
        this.showValueTooltip();
        this.invalidateLayout();
        return this;
    }

    /**
     * Shows the tooltip of the value just set: it replaces the tooltip shown when the value has one, and otherwise
     * removes the previous value's tooltip and leaves a tooltip set with {@code setTooltip} in place.
     */
    private void showValueTooltip() {
        final Tooltip next = this.value == null ? null : Strata.appearances().tooltipOf(this.value).orElse(null);
        if (next != null || this.tooltip().orElse(null) == this.valueTooltip) {
            this.setTooltip(next);
        }
        this.valueTooltip = next;
    }

    /**
     * Asks the value for its tooltip again while the one shown is the value's.
     */
    @Override
    protected void onPointerEnter(final PointerEvent event) {
        if (this.value != null && this.tooltip().orElse(null) == this.valueTooltip) {
            this.showValueTooltip();
        }
    }

    @Override
    public Optional<SpriteId> sprite() {
        return Optional.ofNullable(this.sprite);
    }

    @Override
    public Avatar setSprite(final SpriteId sprite) {
        this.sprite = sprite;
        this.visual = null;
        return this;
    }

    @Override
    public Optional<Visual> visual() {
        return Optional.ofNullable(this.visual);
    }

    @Override
    public Avatar setVisual(final Visual visual) {
        this.visual = visual;
        this.sprite = null;
        return this;
    }

    @Override
    public Text name() {
        return this.name;
    }

    @Override
    public Avatar setName(final Text name) {
        this.name = name == null ? Text.empty() : name;
        this.invalidateLayout();
        return this;
    }

    @Override
    public String initials() {
        if (this.initials != null) {
            return this.initials;
        }
        final Text from = !this.name.isEmpty() ? this.name : this.valueName;
        return from == null ? "" : AvatarImpl.initialsOf(from.plain(), Translations.translator().locale());
    }

    @Override
    public Avatar setInitials(final String initials) {
        this.initials = initials;
        this.invalidateLayout();
        return this;
    }

    @Override
    public boolean isGroup() {
        return this.group;
    }

    @Override
    public Avatar setGroup(final boolean group) {
        this.group = group;
        return this;
    }

    @Override
    public float avatarSize() {
        return this.size;
    }

    @Override
    public Avatar setAvatarSize(final float size) {
        if (!(size >= 0f) || Float.isInfinite(size)) {
            throw new IllegalArgumentException("Avatar size must be finite and not negative: " + size);
        }
        this.size = size;
        this.invalidateLayout();
        return this;
    }

    @Override
    public int badge() {
        return this.badge.count();
    }

    @Override
    public Avatar setBadge(final int count) {
        if (count < 0) {
            throw new IllegalArgumentException("A badge count cannot be negative: " + count);
        }
        if (count != this.badge.count()) {
            this.badge.setCount(count);
            this.invalidateLayout();
        }
        return this;
    }

    @Override
    public Optional<Visual> badgeVisual() {
        return Optional.ofNullable(this.badgeVisual);
    }

    @Override
    public Avatar setBadgeVisual(final Visual visual) {
        this.badgeVisual = visual;
        return this;
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
        this.frame = theme.paintOr(STYLE_FRAME, Paint.NONE);
        this.placeholder = theme.paintOr(STYLE_PLACEHOLDER, Paint.NONE);
        this.groupPlaceholder = theme.paintOr(STYLE_PLACEHOLDER_GROUP, this.placeholder);
        this.initialsStyle = theme.textStyleOr(STYLE_INITIALS, TextStyle.DEFAULT);
        this.badge.applyTheme(theme);
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        final TextMetrics metrics = context.text();
        final String shown = this.initials();
        this.initialsLine = shown.isEmpty() ? TextLine.EMPTY : TextLine.of(Text.of(shown), this.initialsStyle, metrics);
        this.lineHeight = metrics.lineHeight(this.initialsStyle);
        this.badge.measure(metrics);
        this.placedFor = null;
        return new Size(this.size, this.size);
    }

    /**
     * Works out the square, the initials' scale and place, and the badges' rectangles for the bounds.
     */
    private void place(final Rect bounds) {
        final float side = Math.max(0f, Math.min(bounds.width(), bounds.height()));
        final Rect box = new Rect(bounds.x() + (bounds.width() - side) * 0.5f, bounds.y() + (bounds.height() - side) * 0.5f,
                side, side);
        this.square = box;
        this.scale = this.lineHeight > 0f ? Math.max(1f, (float) Math.floor(side * 0.4f / this.lineHeight)) : 1f;
        this.initialsX = (box.x() + (side - this.initialsLine.width() * this.scale) * 0.5f) / this.scale;
        this.initialsY = (box.y() + (side - this.initialsLine.height() * this.scale) * 0.5f) / this.scale;
        this.badgeRect = this.badge.cornerAt(box.right(), box.bottom());
        final float third = side / 3f;
        this.badgeVisualRect = new Rect(box.right() - third, box.bottom() - third, third, third);
        this.placedFor = bounds;
    }

    @Override
    protected void paint(final Painter painter, final Rect bounds) {
        if (!bounds.equals(this.placedFor)) {
            this.place(bounds);
        }
        final Rect box = this.square;
        this.background.paint(painter, box);
        if (this.visual != null) {
            painter.visual(this.visual, box, WHITE);
        } else if (this.sprite != null) {
            painter.sprite(this.sprite, box, WHITE);
        } else if (this.valueIcon != null) {
            painter.visual(this.valueIcon, box, WHITE);
        } else if (!this.initialsLine.isEmpty()) {
            if (this.scale == 1f) {
                this.initialsLine.draw(painter, this.initialsX, this.initialsY);
            } else {
                this.painter = painter;
                try {
                    painter.scaled(this.scale, this.drawInitials);
                } finally {
                    this.painter = null;
                }
            }
        } else {
            (this.group ? this.groupPlaceholder : this.placeholder).paint(painter, box);
        }
        this.frame.paint(painter, box);
        if (this.badgeVisual != null) {
            painter.visual(this.badgeVisual, this.badgeVisualRect, WHITE);
        } else {
            this.badge.paint(painter, this.badgeRect);
        }
    }

    /**
     * Runs inside the initials' scale during {@link #paint}; a field-held method reference, so painting allocates no
     * lambda per frame.
     */
    private void drawInitials() {
        this.initialsLine.draw(this.painter, this.initialsX, this.initialsY);
    }

    @Override
    protected Object snapshotState() {
        return new Avatar.State(this.value, this.name, this.initials(), this.group, this.badge.count());
    }
}
