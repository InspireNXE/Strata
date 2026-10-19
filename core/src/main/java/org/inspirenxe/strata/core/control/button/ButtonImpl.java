package org.inspirenxe.strata.core.control.button;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.control.button.Button;
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

import java.util.Objects;
import java.util.Set;

/**
 * The core {@link Button}. Its face is the disabled paint when disabled, the pressed paint while held under the
 * pointer, the hovered paint while hovered or focused, and the normal paint otherwise. A focused button also draws
 * a one-unit focus ring when the theme has one. The caption is centered.
 *
 * <p>The button is focusable and tabbable, shows a hand cursor, and is at least 20 by 20 units. Its desired size is
 * the caption plus 8 units of padding on the left and right and 5 on the top and bottom, unless that padding is
 * changed. Measuring takes the wider of the normal and disabled captions, so enabling or disabling it needs no
 * relayout.</p>
 */
public final class ButtonImpl extends CustomComponent<Button> implements Button {
    private static final Set<String> REQUIRED = Set.of(STYLE_NORMAL, STYLE_HOVERED, STYLE_PRESSED, STYLE_DISABLED);

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

    /**
     * Creates a button.
     *
     * @param ui the factory that owns the button
     * @param text the caption
     */
    @SuppressWarnings("this-escape")
    public ButtonImpl(final ComponentFactory ui, final String text) {
        super(ui);
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
