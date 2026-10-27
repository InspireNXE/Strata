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

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.component.Focusable;
import org.inspirenxe.strata.api.component.HasCursor;
import org.inspirenxe.strata.api.component.HasTooltip;
import org.inspirenxe.strata.api.component.Tooltip;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.display.InfoBar;
import org.inspirenxe.strata.api.control.window.Window;
import org.inspirenxe.strata.api.event.input.KeyEvent;
import org.inspirenxe.strata.api.event.input.PointerButtonEvent;
import org.inspirenxe.strata.api.extension.ArrangeContext;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.layout.VerticalAlignment;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.core.control.Rings;
import org.inspirenxe.strata.core.node.BuiltInComponent;
import org.inspirenxe.strata.core.node.BuiltInComposite;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;
import org.inspirenxe.strata.core.node.Node;
import org.inspirenxe.strata.core.text.RichText;
import org.inspirenxe.strata.core.text.TextLine;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * The core {@link InfoBar}, a composite whose close button is a private child and whose action is content. The bar
 * paints its background, accent strip, icon, title and message itself, from {@link TextLine}s and places worked out
 * when it is measured and arranged, so a frame allocates nothing.
 *
 * <p>Measuring with a finite width first tries one line: the title and the message unwrapped, {@link #GAP} apart,
 * then the action at its desired size. If that is wider than the space inside the padding, beside the close button
 * and the icon, the title and the message wrap to the width of that column, one below the other, and the action is
 * measured with the column's width and goes {@link #GAP} below them; the bar then asks for the whole width it is
 * offered. With an infinite width nothing wraps: the bar takes one line unless the title or the message holds a line
 * break, and then the lines stack the same way in a column as wide as the widest of them and the action, and the
 * bar asks for that column with its chrome. The icon, a square one line of the message's style
 * tall, and the close button, {@link #CLOSE_WIDTH} wide and a line of its style tall, are centered on the line, or on
 * the first line when the text wraps.</p>
 *
 * <p>A closed bar measures zero, draws nothing and has no children. The close button draws the theme's
 * {@code button.focus} as a ring while focused, and its hovered style while hovered or focused.</p>
 */
public final class InfoBarImpl extends BuiltInComposite<InfoBar> implements InfoBar {
    /**
     * The padding when the theme sets no {@value #STYLE_PADDING}: 6 at the sides and 4 at the top and bottom.
     */
    public static final Insets DEFAULT_PADDING = new Insets(6f, 4f, 6f, 4f);
    /**
     * The space between the icon, the title, the message, the action and the close button, in UI units.
     */
    public static final float GAP = 4f;
    /**
     * The width of the close button, in UI units, as wide as a window's.
     */
    public static final float CLOSE_WIDTH = 12f;
    private static final Set<String> REQUIRED = Set.of(STYLE_BACKGROUND);
    private static final String[] BACKGROUNDS = {STYLE_BACKGROUND_INFO, STYLE_BACKGROUND_SUCCESS,
            STYLE_BACKGROUND_WARNING, STYLE_BACKGROUND_ERROR};
    private static final String[] ACCENTS = {STYLE_ACCENT_INFO, STYLE_ACCENT_SUCCESS, STYLE_ACCENT_WARNING,
            STYLE_ACCENT_ERROR};
    private static final String[] ICONS = {STYLE_ICON_INFO, STYLE_ICON_SUCCESS, STYLE_ICON_WARNING, STYLE_ICON_ERROR};

    private final ComponentFactoryImpl ui;
    private final CloseButton closeButton;
    private Severity severity;
    private Text title;
    private Text text;
    private Component<?> action;
    private boolean closable;
    private boolean iconShown;
    private boolean open;

    // Theme, one entry per severity
    private final Paint[] backgrounds;
    private final Paint[] accents;
    private final Paint[] icons;
    private TextStyle titleStyle;
    private TextStyle textStyle;
    private TextStyle closeStyle;
    private TextStyle closeHoveredStyle;
    private Paint focusRing;
    private Insets padding;

    // Worked out during measure, relative to the top-left of the space inside the accent strip and the padding
    private final List<TextLine> lines;
    private final List<Float> lineX;
    private final List<Float> lineY;
    private float iconSize;
    private float iconY;
    private float actionX;
    private float actionY;
    private float closeY;

    // Worked out during arrange, in the host's coordinates
    private float[] drawX;
    private float[] drawY;
    private Rect accentRect;
    private Rect iconRect;

    /**
     * Creates an open {@link Severity#INFO} bar with no title, message or action, and a close button.
     *
     * @param ui the factory that owns the bar
     */
    @SuppressWarnings("this-escape")
    public InfoBarImpl(final ComponentFactoryImpl ui) {
        super(ui);
        this.ui = ui;
        this.severity = Severity.INFO;
        this.title = Text.empty();
        this.text = Text.empty();
        this.closable = true;
        this.iconShown = true;
        this.open = true;
        this.backgrounds = new Paint[] {Paint.NONE, Paint.NONE, Paint.NONE, Paint.NONE};
        this.accents = new Paint[] {Paint.NONE, Paint.NONE, Paint.NONE, Paint.NONE};
        this.icons = new Paint[4];
        this.titleStyle = TextStyle.DEFAULT;
        this.textStyle = TextStyle.DEFAULT;
        this.closeStyle = TextStyle.DEFAULT;
        this.closeHoveredStyle = TextStyle.DEFAULT;
        this.focusRing = Paint.NONE;
        this.padding = DEFAULT_PADDING;
        this.lines = new ArrayList<>();
        this.lineX = new ArrayList<>();
        this.lineY = new ArrayList<>();
        this.drawX = new float[0];
        this.drawY = new float[0];
        this.accentRect = Rect.ZERO;
        this.iconRect = Rect.ZERO;
        this.closeButton = new CloseButton(ui, this);
        this.addChild(this.closeButton);
        this.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);
    }

    // ------------------------------------------------------------------
    // What it says
    // ------------------------------------------------------------------

    @Override
    public Severity severity() {
        return this.severity;
    }

    @Override
    public InfoBar setSeverity(final Severity severity) {
        this.severity = Objects.requireNonNull(severity, "severity");
        this.invalidateLayout();
        return this;
    }

    @Override
    public Text title() {
        return this.title;
    }

    @Override
    public InfoBar setTitle(final Text title) {
        this.title = Objects.requireNonNull(title, "title");
        this.invalidateLayout();
        return this;
    }

    @Override
    public Text text() {
        return this.text;
    }

    @Override
    public InfoBar setText(final Text message) {
        this.text = Objects.requireNonNull(message, "message");
        this.invalidateLayout();
        return this;
    }

    @Override
    public Optional<Component<?>> action() {
        return Optional.ofNullable(this.action);
    }

    @Override
    public InfoBar setAction(final Component<?> action) {
        if (action == this.action) {
            return this;
        }
        if (action != null) {
            final Node node = this.nodeOf(action);
            if (node.parentNode() != null || node.root() != null) {
                throw new IllegalArgumentException(action + " is already part of a tree");
            }
        }
        if (this.action != null && this.open) {
            this.removeChild(this.action);
        }
        this.action = action;
        if (action != null && this.open) {
            // The action comes before the close button in the tab order, as it does on screen
            this.removeChild(this.closeButton);
            this.addContent(action);
            if (this.closable) {
                this.addChild(this.closeButton);
            }
        }
        this.invalidateLayout();
        return this;
    }

    @Override
    public InfoBar setAction(final Text caption, final Runnable action) {
        Objects.requireNonNull(action, "action");
        final Button button = this.ui.button(caption).setLink(true);
        button.onEvent(Button.PRESSED, event -> action.run());
        return this.setAction(button);
    }

    @Override
    public boolean isClosable() {
        return this.closable;
    }

    @Override
    public InfoBar setClosable(final boolean closable) {
        if (this.closable != closable) {
            this.closable = closable;
            if (this.open) {
                if (closable) {
                    this.addChild(this.closeButton);
                } else {
                    this.removeChild(this.closeButton);
                }
            }
            this.invalidateLayout();
        }
        return this;
    }

    @Override
    public boolean isIconShown() {
        return this.iconShown;
    }

    @Override
    public InfoBar setIconShown(final boolean shown) {
        this.iconShown = shown;
        this.invalidateLayout();
        return this;
    }

    // ------------------------------------------------------------------
    // Opening and closing
    // ------------------------------------------------------------------

    @Override
    public boolean isOpen() {
        return this.open;
    }

    @Override
    public InfoBar open() {
        if (this.open) {
            return this;
        }
        this.open = true;
        if (this.action != null) {
            this.addContent(this.action);
        }
        if (this.closable) {
            this.addChild(this.closeButton);
        }
        this.invalidateLayout();
        this.report(OPENED);
        return this;
    }

    @Override
    public InfoBar close() {
        if (!this.open) {
            return this;
        }
        this.open = false;
        this.clearChildren();
        this.invalidateLayout();
        this.report(CLOSED);
        return this;
    }

    /**
     * Closes for the user through {@link #CLOSE}, while open and enabled.
     */
    void userClose() {
        if (this.open && this.isEnabled() && this.request(CLOSE)) {
            this.close();
        }
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
        final Paint background = theme.paintOr(STYLE_BACKGROUND, Paint.NONE);
        for (int i = 0; i < 4; i++) {
            this.backgrounds[i] = theme.paintOr(BACKGROUNDS[i], background);
            this.accents[i] = theme.paintOr(ACCENTS[i], Paint.NONE);
            final Paint icon = theme.paintOr(ICONS[i], Paint.NONE);
            this.icons[i] = icon.equals(Paint.NONE) ? null : icon;
        }
        this.textStyle = theme.textStyleOr(STYLE_TEXT, TextStyle.DEFAULT);
        this.titleStyle = theme.textStyleOr(STYLE_TITLE, this.textStyle.withBold(true));
        this.closeStyle = theme.textStyleOr(STYLE_CLOSE, theme.textStyleOr(Window.STYLE_CLOSE, this.textStyle));
        this.closeHoveredStyle = theme.textStyleOr(STYLE_CLOSE_HOVERED,
                theme.textStyleOr(Window.STYLE_CLOSE_HOVERED, this.closeStyle));
        this.focusRing = theme.paintOr(Button.STYLE_FOCUS, Paint.NONE);
        this.padding = theme.insetsOr(STYLE_PADDING, DEFAULT_PADDING);
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        this.lines.clear();
        this.lineX.clear();
        this.lineY.clear();
        if (!this.open) {
            return new Size(0f, 0f);
        }
        final TextMetrics metrics = context.text();
        final float lineHeight = metrics.lineHeight(this.textStyle);
        this.iconSize = this.iconShown && this.icons[this.severity.ordinal()] != null ? lineHeight : 0f;
        final Size close = this.closable ? context.measure(this.closeButton, Float.POSITIVE_INFINITY, availableHeight)
                : Size.ZERO;
        final float left = this.iconSize > 0f ? this.iconSize + GAP : 0f;
        final float right = this.closable ? GAP + close.width() : 0f;
        final float chrome = ACCENT_WIDTH + this.padding.horizontal();

        final List<TextSpan> titleRuns = this.title.isEmpty() ? List.of() : this.title.resolve(this.titleStyle);
        final List<TextSpan> textRuns = this.text.isEmpty() ? List.of() : this.text.resolve(this.textStyle);
        final List<List<TextSpan>> titleWhole = titleRuns.isEmpty() ? List.of()
                : RichText.wrap(titleRuns, Float.POSITIVE_INFINITY, metrics);
        final List<List<TextSpan>> textWhole = textRuns.isEmpty() ? List.of()
                : RichText.wrap(textRuns, Float.POSITIVE_INFINITY, metrics);
        Size actionSize = this.action == null ? Size.ZERO
                : context.measure(this.action, Float.POSITIVE_INFINITY, availableHeight);

        final TextLine titleLine = titleWhole.size() == 1 ? TextLine.of(titleWhole.get(0), this.titleStyle, metrics)
                : null;
        final TextLine textLine = textWhole.size() == 1 ? TextLine.of(textWhole.get(0), this.textStyle, metrics)
                : null;
        float oneLine = left + right;
        int parts = 0;
        if (titleLine != null) {
            oneLine += titleLine.width();
            parts++;
        }
        if (textLine != null) {
            oneLine += textLine.width();
            parts++;
        }
        if (this.action != null) {
            oneLine += actionSize.width();
            parts++;
        }
        oneLine += GAP * Math.max(0, parts - 1);
        final boolean single = titleWhole.size() <= 1 && textWhole.size() <= 1
                && (Float.isInfinite(availableWidth) || chrome + oneLine <= availableWidth);

        final float height;
        final float width;
        if (single) {
            height = Math.max(Math.max(Math.max(titleLine == null ? 0f : titleLine.height(),
                    textLine == null ? 0f : textLine.height()), Math.max(actionSize.height(), close.height())),
                    Math.max(this.iconSize, parts == 0 ? lineHeight : 0f));
            float x = left;
            if (titleLine != null) {
                this.place(titleLine, x, (height - titleLine.height()) * 0.5f);
                x += titleLine.width() + GAP;
            }
            if (textLine != null) {
                this.place(textLine, x, (height - textLine.height()) * 0.5f);
                x += textLine.width() + GAP;
            }
            this.actionX = x;
            this.actionY = (height - actionSize.height()) * 0.5f;
            this.closeY = (height - close.height()) * 0.5f;
            this.iconY = (height - this.iconSize) * 0.5f;
            width = chrome + oneLine;
        } else {
            // Unbounded, the lines stay as the breaks in the text make them
            final boolean unbounded = Float.isInfinite(availableWidth);
            final float wrapAt = Math.max(0f, availableWidth - chrome - left - right);
            final List<List<TextSpan>> titleLines = unbounded || titleRuns.isEmpty() ? titleWhole
                    : RichText.wrap(titleRuns, wrapAt, metrics);
            final List<List<TextSpan>> textLines = unbounded || textRuns.isEmpty() ? textWhole
                    : RichText.wrap(textRuns, wrapAt, metrics);
            float y = 0f;
            float widest = actionSize.width();
            for (final List<TextSpan> line : titleLines) {
                final TextLine made = TextLine.of(line, this.titleStyle, metrics);
                this.place(made, left, y);
                y += made.height();
                widest = Math.max(widest, made.width());
            }
            for (final List<TextSpan> line : textLines) {
                final TextLine made = TextLine.of(line, this.textStyle, metrics);
                this.place(made, left, y);
                y += made.height();
                widest = Math.max(widest, made.width());
            }
            final float column = unbounded ? widest : wrapAt;
            if (this.action != null) {
                actionSize = context.measure(this.action, column, availableHeight);
                this.actionX = left;
                this.actionY = y > 0f ? y + GAP : 0f;
                y = this.actionY + actionSize.height();
            }
            final float first = this.lines.isEmpty() ? lineHeight : this.lines.get(0).height();
            this.closeY = Math.max(0f, (first - close.height()) * 0.5f);
            this.iconY = Math.max(0f, (first - this.iconSize) * 0.5f);
            height = Math.max(y, Math.max(this.closeY + close.height(), this.iconY + this.iconSize));
            width = unbounded ? chrome + left + column + right : availableWidth;
        }
        return new Size(width, height + this.padding.vertical());
    }

    private void place(final TextLine line, final float x, final float y) {
        this.lines.add(line);
        this.lineX.add(x);
        this.lineY.add(y);
    }

    @Override
    protected void arrange(final ArrangeContext context, final Rect bounds) {
        if (!this.open) {
            return;
        }
        final float left = bounds.x() + ACCENT_WIDTH + this.padding.left();
        final float top = bounds.y() + this.padding.top();
        final int count = this.lines.size();
        if (this.drawX.length != count) {
            this.drawX = new float[count];
            this.drawY = new float[count];
        }
        for (int i = 0; i < count; i++) {
            this.drawX[i] = left + this.lineX.get(i);
            this.drawY[i] = top + this.lineY.get(i);
        }
        this.accentRect = new Rect(bounds.x(), bounds.y(), ACCENT_WIDTH, bounds.height());
        this.iconRect = new Rect(left, top + this.iconY, this.iconSize, this.iconSize);
        if (this.action != null) {
            final Size size = context.outerSize(this.action);
            context.arrange(this.action, new Rect(left + this.actionX, top + this.actionY, size.width(), size.height()));
        }
        if (this.closable) {
            final Size size = context.outerSize(this.closeButton);
            context.arrange(this.closeButton, new Rect(bounds.right() - this.padding.right() - size.width(),
                    top + this.closeY, size.width(), size.height()));
        }
    }

    @Override
    protected void paint(final Painter painter, final Rect bounds) {
        if (!this.open) {
            return;
        }
        final int index = this.severity.ordinal();
        this.backgrounds[index].paint(painter, bounds);
        this.accents[index].paint(painter, this.accentRect);
        if (this.iconSize > 0f && this.icons[index] != null) {
            this.icons[index].paint(painter, this.iconRect);
        }
        for (int i = 0; i < this.lines.size(); i++) {
            this.lines.get(i).draw(painter, this.drawX[i], this.drawY[i]);
        }
    }

    @Override
    protected Object snapshotState() {
        return new InfoBar.State(this.severity, this.title, this.text, this.open);
    }

    /**
     * The close button: an {@code x} in the close style, {@link #CLOSE_WIDTH} wide and a line tall, that closes the
     * bar for the user on a left click, Enter or Space.
     */
    private static final class CloseButton extends BuiltInComponent<CloseButton> implements Focusable<CloseButton>,
            HasCursor<CloseButton>, HasTooltip<CloseButton> {
        private final InfoBarImpl bar;
        private TextLine normal;
        private TextLine hovered;

        @SuppressWarnings("this-escape")
        CloseButton(final ComponentFactoryImpl ui, final InfoBarImpl bar) {
            super(ui);
            this.bar = bar;
            this.normal = TextLine.EMPTY;
            this.hovered = TextLine.EMPTY;
            this.setCursor(CursorKind.HAND);
            this.setTooltip(Tooltip.text(Text.translatable("strata.button.close")));
        }

        @Override
        protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
            final Text x = Text.of("x");
            this.normal = TextLine.of(x, this.bar.closeStyle, context.text());
            this.hovered = TextLine.of(x, this.bar.closeHoveredStyle, context.text());
            return new Size(CLOSE_WIDTH, Math.max(this.normal.height(), this.hovered.height()));
        }

        @Override
        protected void paint(final Painter painter, final Rect bounds) {
            final TextLine line = this.isHovered() || this.isFocusVisible() ? this.hovered : this.normal;
            line.draw(painter, bounds.x() + (bounds.width() - line.width()) * 0.5f,
                    bounds.y() + (bounds.height() - line.height()) * 0.5f);
            if (this.isFocusVisible() && !this.bar.focusRing.equals(Paint.NONE)) {
                Rings.paint(painter, this.bar.focusRing, bounds);
            }
        }

        @Override
        protected void onClick(final PointerButtonEvent event) {
            if (event.button() == MouseButton.LEFT) {
                this.bar.userClose();
                event.consume();
            }
        }

        @Override
        protected void onKey(final KeyEvent event) {
            if (event.action() == KeyAction.PRESS && (event.key().isEnter() || event.key() == Key.SPACE)) {
                this.bar.userClose();
                event.consume();
            }
        }
    }
}
