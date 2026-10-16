package org.inspirenxe.strata.core.widget;

import org.inspirenxe.strata.api.Ui;
import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.event.KeyEvent;
import org.inspirenxe.strata.api.event.PointerButtonEvent;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geom.Insets;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.geom.Size;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.widget.Button;
import org.inspirenxe.strata.api.widget.ButtonBehavior;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class ButtonImpl extends CustomComponent<Button> implements Button {
    private static final Set<String> REQUIRED = Set.of(STYLE_NORMAL, STYLE_HOVERED, STYLE_PRESSED, STYLE_DISABLED);

    private final List<Runnable> actions;
    private String text;
    private Insets textPadding;

    private Paint normal;
    private Paint hoveredPaint;
    private Paint pressedPaint;
    private Paint disabled;
    private Paint focusRing;
    private TextStyle textStyle;
    private TextStyle disabledTextStyle;

    // Computed during measure. Both captions are kept so enabling or disabling the button needs no relayout
    private TextSpan caption;
    private TextSpan disabledCaption;
    private float captionWidth;
    private float captionHeight;

    @SuppressWarnings("this-escape")
    public ButtonImpl(final Ui ui, final String text, final ButtonBehavior behavior) {
        super(ui, behavior);
        this.actions = new ArrayList<>();
        this.text = Objects.requireNonNull(text, "text");
        this.textPadding = Insets.of(8f, 5f);
        this.normal = Paint.NONE;
        this.hoveredPaint = Paint.NONE;
        this.pressedPaint = Paint.NONE;
        this.disabled = Paint.NONE;
        this.focusRing = Paint.NONE;
        this.textStyle = TextStyle.DEFAULT;
        this.disabledTextStyle = TextStyle.DEFAULT;
        this.caption = TextSpan.of(text);
        this.disabledCaption = this.caption;
        this.setFocusable(true);
        this.setTabbable(true);
        this.setCursor(CursorKind.HAND);
        this.setMinSize(20f, 20f);
    }

    @Override
    public String text() {
        return this.text;
    }

    @Override
    public Button setText(final String text) {
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
    public Button onAction(final Runnable action) {
        this.actions.add(Objects.requireNonNull(action, "action"));
        return this;
    }

    @Override
    public void press() {
        if (!this.isEnabled()) {
            return;
        }
        for (final Runnable action : List.copyOf(this.actions)) {
            action.run();
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
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        final TextMetrics metrics = context.text();
        this.caption = new TextSpan(this.text, this.textStyle);
        this.disabledCaption = new TextSpan(this.text, this.disabledTextStyle);
        this.captionWidth = Math.max(metrics.width(this.caption), metrics.width(this.disabledCaption));
        this.captionHeight = Math.max(metrics.lineHeight(this.textStyle), metrics.lineHeight(this.disabledTextStyle));
        return new Size(this.captionWidth + this.textPadding.horizontal(), this.captionHeight + this.textPadding.vertical());
    }

    @Override
    protected void paint(final Painter painter, final Rect bounds) {
        final Paint face;
        if (!this.isEnabled()) {
            face = this.disabled;
        } else if (this.isPressed() && this.isHovered()) {
            face = this.pressedPaint;
        } else if (this.isHovered() || this.isFocused()) {
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

        painter.text(this.isEnabled() ? this.caption : this.disabledCaption,
                bounds.x() + (bounds.width() - this.captionWidth) * 0.5f,
                bounds.y() + (bounds.height() - this.captionHeight) * 0.5f);
    }

    @Override
    protected void onClick(final PointerButtonEvent event) {
        if (event.button() == MouseButton.LEFT) {
            this.press();
            event.consume();
        }
    }

    @Override
    protected void onKey(final KeyEvent event) {
        if (event.action() == KeyAction.PRESS && (event.key().isEnter() || event.key() == Key.SPACE)) {
            this.press();
            event.consume();
        }
    }
}
