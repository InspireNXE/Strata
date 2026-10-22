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

import org.inspirenxe.strata.api.command.Command;
import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.control.button.CheckBox;
import org.inspirenxe.strata.api.control.button.CheckState;
import org.inspirenxe.strata.api.event.input.KeyEvent;
import org.inspirenxe.strata.api.event.input.PointerButtonEvent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.core.control.Activation;
import org.inspirenxe.strata.core.control.BoxCaption;
import org.inspirenxe.strata.core.control.Commands;
import org.inspirenxe.strata.core.control.RectCache;
import org.inspirenxe.strata.core.control.Rings;
import org.inspirenxe.strata.core.node.BuiltInComponent;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.inspirenxe.strata.api.component.Validation;
import org.inspirenxe.strata.core.control.Validity;
import org.inspirenxe.strata.core.state.ConversionTarget;
import java.util.function.Function;

/**
 * The core {@link CheckBox}. The box sits at the left edge, vertically centered, and the caption 4 units to its
 * right, also centered on the box's middle. The box uses the hovered paints while hovered or focused, and the
 * disabled paints when disabled. A checked box also draws a check in the mark paint, made of small squares inside a
 * fifth of the box size from each edge, with strokes an eighth of the box size thick; a mixed box draws the unchecked
 * paints and the mixed mark as a square at the same inset. A focused box draws a one-unit focus ring.
 *
 * <p>Measuring takes the wider and taller of the normal and disabled captions, so enabling or disabling it needs no
 * relayout. Without a fixed box size, the box is the caption's line height plus 8, rounded to a whole unit. The box,
 * the mark's squares and the focus ring are kept from one frame to the next while the bounds and the box size stay
 * the same, so painting allocates nothing then.</p>
 */
public final class CheckBoxImpl extends BuiltInComponent<CheckBox> implements CheckBox, ConversionTarget {
    /**
     * The gap between the box and the caption.
     */
    public static final float GAP = 4f;
    /**
     * Added to the caption's line height for the default box size.
     */
    public static final float BOX_PADDING = 8f;
    private static final Set<String> REQUIRED = Set.of(STYLE_NORMAL, STYLE_HOVERED, STYLE_CHECKED, STYLE_CHECKED_HOVERED);

    private final Validity validity;
    private Text text;
    private CheckState state;
    private boolean allowsMixed;
    private float fixedBoxSize;

    private Paint normal;
    private Paint hoveredPaint;
    private Paint checkedPaint;
    private Paint checkedHovered;
    private Paint disabled;
    private Paint checkedDisabled;
    private Paint mark;
    private Paint markDisabled;
    private Paint mixedMark;
    private Paint mixedMarkDisabled;
    private Paint focusRing;
    private TextStyle textStyle;
    private TextStyle disabledTextStyle;

    // Computed during measure
    private final BoxCaption layout = new BoxCaption(GAP);
    private float boxSize;

    // Kept between frames: the mark's area, and the check's squares for the area they were placed in
    private final RectCache markArea = new RectCache();
    private final List<Rect> checkSquares = new ArrayList<>();
    private Rect checkArea;
    private final Rings.Ring ring = new Rings.Ring();
    private final Activation activation;
    private Command.Toggle command;

    /**
     * Creates a checkbox.
     *
     * @param ui the factory that owns the checkbox
     * @param text the caption
     */
    @SuppressWarnings("this-escape")
    public CheckBoxImpl(final ComponentFactoryImpl ui, final Text text) {
        super(ui);
        this.validity = new Validity(this.node(), this::validated);
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
        this.mixedMark = Paint.NONE;
        this.mixedMarkDisabled = Paint.NONE;
        this.state = CheckState.UNCHECKED;
        this.focusRing = Paint.NONE;
        this.textStyle = TextStyle.DEFAULT;
        this.disabledTextStyle = TextStyle.DEFAULT;
        this.activation = Activation.leftButton(this::activatesOnPress, this::userToggle);
        this.setCursor(CursorKind.HAND);
    }

    @Override
    public Text text() {
        return this.text;
    }

    @Override
    public CheckBox setText(final Text text) {
        this.text = Objects.requireNonNull(text, "text");
        this.invalidateLayout();
        return this;
    }

    @Override
    public boolean isSelected() {
        return this.state.isChecked();
    }

    @Override
    public CheckBox setSelected(final boolean checked) {
        return this.setState(checked ? CheckState.CHECKED : CheckState.UNCHECKED);
    }

    @Override
    public CheckBox toggle() {
        if (this.isEnabled()) {
            this.setSelected(!this.isSelected());
        }
        return this;
    }

    @Override
    public CheckState state() {
        return this.state;
    }

    @Override
    public CheckBox setState(final CheckState state) {
        Objects.requireNonNull(state, "state");
        final CheckState previous = this.state;
        if (previous != state) {
            this.state = state;
            this.report(CHANGED, state, previous);
            if (previous.isChecked() != state.isChecked()) {
                this.report(TOGGLED, state.isChecked(), previous.isChecked());
            }
            this.validity.check();
        }
        return this;
    }

    @Override
    public boolean allowsMixed() {
        return this.allowsMixed;
    }

    @Override
    public CheckBox setAllowsMixed(final boolean allowsMixed) {
        this.allowsMixed = allowsMixed;
        return this;
    }

    /**
     * Returns the state a change by the user moves to from the current one.
     */
    private CheckState next() {
        if (this.allowsMixed) {
            return switch (this.state) {
                case UNCHECKED -> CheckState.CHECKED;
                case CHECKED -> CheckState.MIXED;
                case MIXED -> CheckState.UNCHECKED;
            };
        }
        return this.state.isChecked() ? CheckState.UNCHECKED : CheckState.CHECKED;
    }

    /**
     * Changes the box for the user: {@link CheckBox#CHANGE} first, then {@link CheckBox#TOGGLE} when the final state
     * would change whether the box is checked. A null value from either keeps the box as it is. Does nothing while
     * disabled.
     */
    private void userToggle() {
        if (!this.isEnabled()) {
            return;
        }
        this.request(CHANGE, this.next(), wanted -> {
            if (wanted == null || wanted == this.state) {
                return;
            }
            if (wanted.isChecked() == this.isSelected()) {
                this.setState(wanted);
            } else {
                this.request(TOGGLE, wanted.isChecked(), checked -> {
                    if (checked != null) {
                        this.setState(checked == wanted.isChecked() ? wanted : checked ? CheckState.CHECKED : CheckState.UNCHECKED);
                    }
                });
            }
        });
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

    // ------------------------------------------------------------------
    // Validatable
    // ------------------------------------------------------------------

    @Override
    public Validation validation() {
        return this.validity.validation();
    }

    @Override
    public CheckBox setValidation(final Validation validation) {
        this.validity.set(validation);
        return this;
    }

    @Override
    public CheckBox setValidator(final Function<? super CheckBox, Validation> validator) {
        this.validity.setValidator(validator == null ? null : () -> validator.apply(this));
        return this;
    }

    @Override
    public void setConversionError(final Text message) {
        this.validity.setConversionError(message);
    }

    private void validated(final Validation validation, final Validation previous) {
        this.report(VALIDATED, validation, previous);
    }

    @Override
    protected void onApplyTheme(final Theme theme) {
        this.validity.applyTheme(theme, STYLE_INVALID);
        this.normal = theme.paintOr(STYLE_NORMAL, Paint.NONE);
        this.hoveredPaint = theme.paintOr(STYLE_HOVERED, this.normal);
        this.checkedPaint = theme.paintOr(STYLE_CHECKED, this.normal);
        this.checkedHovered = theme.paintOr(STYLE_CHECKED_HOVERED, this.checkedPaint);
        this.disabled = theme.paintOr(STYLE_DISABLED, this.normal);
        this.checkedDisabled = theme.paintOr(STYLE_CHECKED_DISABLED, this.checkedPaint);
        this.mark = theme.paintOr(STYLE_MARK, Paint.NONE);
        this.markDisabled = theme.paintOr(STYLE_MARK_DISABLED, this.mark);
        this.mixedMark = theme.paintOr(STYLE_MARK_MIXED, this.mark);
        this.mixedMarkDisabled = theme.paintOr(STYLE_MARK_MIXED_DISABLED,
                theme.paint(STYLE_MARK_DISABLED).orElse(this.mixedMark));
        this.focusRing = theme.paintOr(STYLE_FOCUS, Paint.NONE);
        this.textStyle = theme.textStyleOr(STYLE_TEXT, TextStyle.DEFAULT);
        this.disabledTextStyle = theme.textStyleOr(STYLE_TEXT_DISABLED, this.textStyle);
    }

    @Override
    protected Size measureMinimum(final MeasureContext context) {
        // The caption does not shrink, and measuring does not depend on the space offered
        return this.measure(context, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        final float captionHeight = this.layout.measureCaption(this.text, this.textStyle, this.disabledTextStyle,
                context.text());
        this.boxSize = Float.isNaN(this.fixedBoxSize) ? CheckBoxImpl.defaultBoxSize(captionHeight) : this.fixedBoxSize;
        return this.layout.size(this.boxSize, this.boxSize);
    }

    /**
     * Returns the box size for a caption height when none is fixed: the height plus {@link #BOX_PADDING}, rounded to
     * a whole unit so a mark inset by whole units is centered exactly. A radio button sizes its box the same way.
     *
     * @param captionHeight the caption's height
     * @return the box size
     */
    static float defaultBoxSize(final float captionHeight) {
        return Math.round(captionHeight + BOX_PADDING);
    }

    @Override
    protected float baseline() {
        return this.layout.baseline(this.desiredHeight());
    }

    @Override
    protected void paint(final Painter painter, final Rect bounds) {
        final Rect box = this.layout.box(bounds);
        final boolean enabled = this.isEnabled();
        final boolean lit = this.isHovered() || this.isFocused();

        final boolean checked = this.state.isChecked();
        final Paint face;
        if (!enabled) {
            face = checked ? this.checkedDisabled : this.disabled;
        } else if (checked) {
            face = this.validity.face(lit ? this.checkedHovered : this.checkedPaint);
        } else {
            face = this.validity.face(lit ? this.hoveredPaint : this.normal);
        }
        face.paint(painter, box);
        if (enabled) {
            this.validity.paintOutline(painter, box);
        }

        final float inset = Math.round(this.boxSize * 0.2f);
        final Rect inner = this.markArea.at(box.x() + inset, box.y() + inset, box.width() - inset * 2f,
                box.height() - inset * 2f);
        if (checked) {
            this.drawCheck(painter, enabled ? this.mark : this.markDisabled, inner);
        } else if (this.state == CheckState.MIXED) {
            (enabled ? this.mixedMark : this.mixedMarkDisabled).paint(painter, inner);
        }
        if (this.isFocused() && this.focusRing != Paint.NONE) {
            this.ring.paint(painter, this.focusRing, box);
        }
        this.layout.paintCaption(painter, bounds, enabled);
    }

    /**
     * Draws a check in a square: a short stroke down to the right from the left middle, then a long one up to the top
     * right, each a run of squares a stroke thick. The squares are placed again only when the area changed.
     */
    private void drawCheck(final Painter painter, final Paint paint, final Rect area) {
        if (paint == Paint.NONE) {
            return;
        }
        if (area != this.checkArea) {
            this.checkArea = area;
            this.checkSquares.clear();
            final float size = Math.min(area.width(), area.height());
            final float stroke = Math.max(1f, Math.round(this.boxSize / 8f));
            final float span = size - stroke;
            final float bottomX = area.x() + span * 0.375f;
            final float bottomY = area.y() + span;
            this.stroke(area.x(), area.y() + span * 0.5f, bottomX, bottomY, stroke);
            this.stroke(bottomX, bottomY, area.x() + span, area.y(), stroke);
        }
        for (int i = 0; i < this.checkSquares.size(); i++) {
            paint.paint(painter, this.checkSquares.get(i));
        }
    }

    private void stroke(final float fromX, final float fromY, final float toX, final float toY, final float stroke) {
        final int steps = Math.max(1, Math.round(Math.max(Math.abs(toX - fromX), Math.abs(toY - fromY)) / (stroke * 0.5f)));
        for (int i = 0; i <= steps; i++) {
            final float t = (float) i / steps;
            this.checkSquares.add(new Rect(fromX + (toX - fromX) * t, fromY + (toY - fromY) * t, stroke, stroke));
        }
    }

    @Override
    protected void onPointerPress(final PointerButtonEvent event) {
        this.activation.press(event);
    }

    @Override
    protected void onClick(final PointerButtonEvent event) {
        this.activation.click(event);
    }

    @Override
    protected void onKey(final KeyEvent event) {
        Activation.key(event, this::userToggle);
    }

    @Override
    protected Object snapshotState() {
        return new CheckBox.State(this.state, this.text);
    }

    @Override
    public Optional<Command.Toggle> command() {
        return Optional.ofNullable(this.command);
    }

    /**
     * Shows a toggle: takes its text, binds {@link #ENABLED} to its enabled source and {@link #SELECTED} to its state
     * both ways, or takes the bindings off again for null.
     */
    @Override
    public CheckBox setCommand(final Command.Toggle command) {
        this.command = command;
        Commands.follow(this.node(), command);
        return this;
    }
}
