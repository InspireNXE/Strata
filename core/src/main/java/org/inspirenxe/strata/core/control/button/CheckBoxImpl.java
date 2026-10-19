package org.inspirenxe.strata.core.control.button;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.control.button.CheckBox;
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
 * The core {@link CheckBox}. The box sits at the left edge, vertically centered, and the caption 4 units to its
 * right, also centered on the box's middle. The box uses the hovered paints while hovered or focused, and the
 * disabled paints when disabled. A checked box also draws the mark paint, inset by a fifth of the box size on each
 * side, and a focused box a one-unit focus ring.
 *
 * <p>Measuring takes the wider and taller of the normal and disabled captions, so enabling or disabling it needs no
 * relayout. Without a fixed box size, the box is the caption's line height plus 8.</p>
 */
public final class CheckBoxImpl extends CustomComponent<CheckBox> implements CheckBox {
    /**
     * The gap between the box and the caption.
     */
    public static final float GAP = 4f;
    /**
     * Added to the caption's line height for the default box size.
     */
    public static final float BOX_PADDING = 8f;
    private static final Set<String> REQUIRED = Set.of(STYLE_NORMAL, STYLE_HOVERED, STYLE_CHECKED, STYLE_CHECKED_HOVERED);

    private String text;
    private boolean checked;
    private float fixedBoxSize;

    private Paint normal;
    private Paint hoveredPaint;
    private Paint checkedPaint;
    private Paint checkedHovered;
    private Paint disabled;
    private Paint checkedDisabled;
    private Paint mark;
    private Paint markDisabled;
    private Paint focusRing;
    private TextStyle textStyle;
    private TextStyle disabledTextStyle;

    // Computed during measure
    private TextSpan caption;
    private TextSpan disabledCaption;
    private float captionHeight;
    private float boxSize;

    /**
     * Creates a check box.
     *
     * @param ui the factory that owns the check box
     * @param text the caption
     */
    @SuppressWarnings("this-escape")
    public CheckBoxImpl(final ComponentFactory ui, final String text) {
        super(ui);
        this.text = Objects.requireNonNull(text, "text");
        this.fixedBoxSize = Float.NaN;
        this.normal = Paint.NONE;
        this.hoveredPaint = Paint.NONE;
        this.checkedPaint = Paint.NONE;
        this.checkedHovered = Paint.NONE;
        this.disabled = Paint.NONE;
        this.checkedDisabled = Paint.NONE;
        this.mark = Paint.NONE;
        this.markDisabled = Paint.NONE;
        this.focusRing = Paint.NONE;
        this.textStyle = TextStyle.DEFAULT;
        this.disabledTextStyle = TextStyle.DEFAULT;
        this.caption = TextSpan.of(text);
        this.disabledCaption = this.caption;
        this.setFocusable(true);
        this.setTabbable(true);
        this.setCursor(CursorKind.HAND);
    }

    @Override
    public String text() {
        return this.text;
    }

    @Override
    public CheckBox setText(final String text) {
        this.text = Objects.requireNonNull(text, "text");
        this.invalidateLayout();
        return this;
    }

    @Override
    public boolean isChecked() {
        return this.checked;
    }

    @Override
    public CheckBox setChecked(final boolean checked) {
        if (this.checked != checked) {
            this.checked = checked;
            this.report(TOGGLED, checked, !checked);
        }
        return this;
    }

    @Override
    public void toggle() {
        if (this.isEnabled()) {
            this.setChecked(!this.checked);
        }
    }

    /**
     * Toggles the box for the user through {@link CheckBox#TOGGLE}, applying the final value unless it is null. Does
     * nothing while disabled.
     */
    private void userToggle() {
        if (this.isEnabled()) {
            this.request(TOGGLE, !this.checked, checked -> {
                if (checked != null) {
                    this.setChecked(checked);
                }
            });
        }
    }

    @Override
    public float boxSize() {
        return Float.isNaN(this.fixedBoxSize) ? this.boxSize : this.fixedBoxSize;
    }

    @Override
    public CheckBox setBoxSize(final float size) {
        if (size < 0f || Float.isInfinite(size)) {
            throw new IllegalArgumentException("Box size must be finite and not negative: " + size);
        }
        this.fixedBoxSize = size;
        this.invalidateLayout();
        return this;
    }

    @Override
    protected Set<String> requiredStyleKeys() {
        return REQUIRED;
    }

    @Override
    protected void onApplyTheme(final Theme theme) {
        this.normal = theme.paintOr(STYLE_NORMAL, Paint.NONE);
        this.hoveredPaint = theme.paintOr(STYLE_HOVERED, this.normal);
        this.checkedPaint = theme.paintOr(STYLE_CHECKED, this.normal);
        this.checkedHovered = theme.paintOr(STYLE_CHECKED_HOVERED, this.checkedPaint);
        this.disabled = theme.paintOr(STYLE_DISABLED, this.normal);
        this.checkedDisabled = theme.paintOr(STYLE_CHECKED_DISABLED, this.checkedPaint);
        this.mark = theme.paintOr(STYLE_MARK, Paint.NONE);
        this.markDisabled = theme.paintOr(STYLE_MARK_DISABLED, this.mark);
        this.focusRing = theme.paintOr(STYLE_FOCUS, Paint.NONE);
        this.textStyle = theme.textStyleOr(STYLE_TEXT, TextStyle.DEFAULT);
        this.disabledTextStyle = theme.textStyleOr(STYLE_TEXT_DISABLED, this.textStyle);
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        final TextMetrics metrics = context.text();
        this.caption = new TextSpan(this.text, this.textStyle);
        this.disabledCaption = new TextSpan(this.text, this.disabledTextStyle);
        this.captionHeight = Math.max(metrics.lineHeight(this.textStyle), metrics.lineHeight(this.disabledTextStyle));
        this.boxSize = Float.isNaN(this.fixedBoxSize) ? this.captionHeight + BOX_PADDING : this.fixedBoxSize;
        final float captionWidth = Math.max(metrics.width(this.caption), metrics.width(this.disabledCaption));
        final float width = this.text.isEmpty() ? this.boxSize : this.boxSize + GAP + captionWidth;
        return new Size(width, Math.max(this.boxSize, this.captionHeight));
    }

    @Override
    protected void paint(final Painter painter, final Rect bounds) {
        final float middle = bounds.y() + bounds.height() * 0.5f;
        final Rect box = new Rect(bounds.x(), middle - this.boxSize * 0.5f, this.boxSize, this.boxSize);
        final boolean enabled = this.isEnabled();
        final boolean lit = this.isHovered() || this.isFocused();

        final Paint face;
        if (!enabled) {
            face = this.checked ? this.checkedDisabled : this.disabled;
        } else if (this.checked) {
            face = lit ? this.checkedHovered : this.checkedPaint;
        } else {
            face = lit ? this.hoveredPaint : this.normal;
        }
        face.paint(painter, box);

        if (this.checked) {
            (enabled ? this.mark : this.markDisabled).paint(painter, box.shrink(Insets.of(this.boxSize * 0.2f)));
        }
        if (this.isFocused() && this.focusRing != Paint.NONE) {
            this.focusRing.paint(painter, new Rect(box.x(), box.y(), box.width(), 1f));
            this.focusRing.paint(painter, new Rect(box.x(), box.bottom() - 1f, box.width(), 1f));
            this.focusRing.paint(painter, new Rect(box.x(), box.y() + 1f, 1f, box.height() - 2f));
            this.focusRing.paint(painter, new Rect(box.right() - 1f, box.y() + 1f, 1f, box.height() - 2f));
        }
        if (!this.text.isEmpty()) {
            painter.text(enabled ? this.caption : this.disabledCaption, box.right() + GAP, middle - this.captionHeight * 0.5f);
        }
    }

    @Override
    protected void onClick(final PointerButtonEvent event) {
        if (event.button() == MouseButton.LEFT) {
            this.userToggle();
            event.consume();
        }
    }

    @Override
    protected void onKey(final KeyEvent event) {
        if (event.action() == KeyAction.PRESS && (event.key().isEnter() || event.key() == Key.SPACE)) {
            this.userToggle();
            event.consume();
        }
    }

    @Override
    protected Object snapshotState() {
        return new CheckBox.State(this.checked, this.text);
    }
}
