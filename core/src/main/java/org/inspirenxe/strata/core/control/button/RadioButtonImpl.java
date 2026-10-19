package org.inspirenxe.strata.core.control.button;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.control.button.RadioButton;
import org.inspirenxe.strata.api.control.button.RadioGroup;
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
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.core.control.Rings;

import java.util.Objects;
import java.util.Set;

/**
 * The core {@link RadioButton}. It lays out and paints like {@link CheckBoxImpl}: the box at the left edge,
 * vertically centered, and the caption {@link CheckBoxImpl#GAP} units to its right. A selected box draws the mark
 * paint inset by three tenths of the box size, and a focused box a one-unit focus ring. Its group holds the
 * selection.
 */
public final class RadioButtonImpl extends CustomComponent<RadioButton> implements RadioButton {
    private static final Set<String> REQUIRED = Set.of(STYLE_NORMAL, STYLE_HOVERED, STYLE_SELECTED, STYLE_SELECTED_HOVERED);

    private final RadioGroupImpl<?> group;
    private String text;

    private Paint normal;
    private Paint hoveredPaint;
    private Paint selectedPaint;
    private Paint selectedHovered;
    private Paint disabled;
    private Paint selectedDisabled;
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
     * Creates a button in a group.
     *
     * @param ui the factory that owns the button
     * @param group the group it belongs to
     * @param text the caption
     */
    @SuppressWarnings("this-escape")
    RadioButtonImpl(final ComponentFactory ui, final RadioGroupImpl<?> group, final String text) {
        super(ui);
        this.group = group;
        this.text = Objects.requireNonNull(text, "text");
        this.normal = Paint.NONE;
        this.hoveredPaint = Paint.NONE;
        this.selectedPaint = Paint.NONE;
        this.selectedHovered = Paint.NONE;
        this.disabled = Paint.NONE;
        this.selectedDisabled = Paint.NONE;
        this.mark = Paint.NONE;
        this.markDisabled = Paint.NONE;
        this.focusRing = Paint.NONE;
        this.textStyle = TextStyle.DEFAULT;
        this.disabledTextStyle = TextStyle.DEFAULT;
        this.caption = TextSpan.of(text);
        this.disabledCaption = this.caption;
        this.setCursor(CursorKind.HAND);
    }

    @Override
    public String text() {
        return this.text;
    }

    @Override
    public RadioButton setText(final String text) {
        this.text = Objects.requireNonNull(text, "text");
        this.invalidateLayout();
        return this;
    }

    @Override
    public RadioGroup<?> group() {
        return this.group;
    }

    @Override
    public boolean isSelected() {
        return this.group.isSelected(this);
    }

    @Override
    public RadioButton setSelected(final boolean selected) {
        if (selected) {
            this.group.choose(this);
        } else if (this.isSelected()) {
            this.group.choose(null);
        }
        return this;
    }

    @Override
    public RadioButton toggle() {
        if (this.isEnabled()) {
            this.setSelected(!this.isSelected());
        }
        return this;
    }

    /**
     * Selects this button for the user through {@link org.inspirenxe.strata.api.control.attribute.value.Toggle#TOGGLE}
     * with true, and selects it if the final value is true. A button that is already selected skips the request and
     * is only focused if asked.
     *
     * @param focus whether to focus the button once it is selected
     */
    void userSelect(final boolean focus) {
        if (this.isSelected()) {
            this.applySelect(focus);
            return;
        }
        this.request(TOGGLE, true, selected -> {
            if (Boolean.TRUE.equals(selected)) {
                this.applySelect(focus);
            }
        });
    }

    /**
     * Reports a change of this button's state, for the group.
     *
     * @param selected the new state
     */
    void reportToggled(final boolean selected) {
        this.report(TOGGLED, selected, !selected);
    }

    /**
     * Makes this button the group's selected one, and focuses it if asked.
     */
    private void applySelect(final boolean focus) {
        this.group.choose(this);
        if (focus) {
            this.requestFocus();
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
        this.selectedPaint = theme.paintOr(STYLE_SELECTED, this.normal);
        this.selectedHovered = theme.paintOr(STYLE_SELECTED_HOVERED, this.selectedPaint);
        this.disabled = theme.paintOr(STYLE_DISABLED, this.normal);
        this.selectedDisabled = theme.paintOr(STYLE_SELECTED_DISABLED, this.selectedPaint);
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
        this.boxSize = this.captionHeight + CheckBoxImpl.BOX_PADDING;
        final float captionWidth = Math.max(metrics.width(this.caption), metrics.width(this.disabledCaption));
        final float width = this.text.isEmpty() ? this.boxSize : this.boxSize + CheckBoxImpl.GAP + captionWidth;
        return new Size(width, Math.max(this.boxSize, this.captionHeight));
    }

    @Override
    protected void paint(final Painter painter, final Rect bounds) {
        final float middle = bounds.y() + bounds.height() * 0.5f;
        final Rect box = new Rect(bounds.x(), middle - this.boxSize * 0.5f, this.boxSize, this.boxSize);
        final boolean enabled = this.isEnabled();
        final boolean lit = this.isHovered() || this.isFocused();
        final boolean selected = this.isSelected();

        final Paint face;
        if (!enabled) {
            face = selected ? this.selectedDisabled : this.disabled;
        } else if (selected) {
            face = lit ? this.selectedHovered : this.selectedPaint;
        } else {
            face = lit ? this.hoveredPaint : this.normal;
        }
        face.paint(painter, box);

        if (selected) {
            (enabled ? this.mark : this.markDisabled).paint(painter, box.shrink(Insets.of(this.boxSize * 0.3f)));
        }
        if (this.isFocused() && this.focusRing != Paint.NONE) {
            Rings.paint(painter, this.focusRing, box);
        }
        if (!this.text.isEmpty()) {
            painter.text(enabled ? this.caption : this.disabledCaption, box.right() + CheckBoxImpl.GAP,
                    middle - this.captionHeight * 0.5f);
        }
    }

    @Override
    protected void onClick(final PointerButtonEvent event) {
        if (event.button() == MouseButton.LEFT) {
            if (this.isEnabled()) {
                this.userSelect(false);
            }
            event.consume();
        }
    }

    @Override
    protected void onKey(final KeyEvent event) {
        if (!event.action().isDown() || !this.isEnabled()) {
            return;
        }
        final Key key = event.key();
        if (event.action() == KeyAction.PRESS && (key.isEnter() || key == Key.SPACE)) {
            this.userSelect(false);
            event.consume();
            return;
        }
        final int direction = switch (key) {
            case UP, LEFT -> -1;
            case DOWN, RIGHT -> 1;
            default -> 0;
        };
        if (direction != 0 && !event.modifiers().control() && !event.modifiers().alt()) {
            this.group.moveFrom(this, direction);
            event.consume();
        }
    }

    @Override
    protected Object snapshotState() {
        return new RadioButton.State(this.isSelected(), this.text);
    }
}
