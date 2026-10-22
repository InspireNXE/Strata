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

import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.control.button.RadioButton;
import org.inspirenxe.strata.api.control.button.RadioGroup;
import org.inspirenxe.strata.api.event.input.KeyEvent;
import org.inspirenxe.strata.api.event.input.PointerButtonEvent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.core.control.Activation;
import org.inspirenxe.strata.core.control.BoxCaption;
import org.inspirenxe.strata.core.control.RectCache;
import org.inspirenxe.strata.core.control.Rings;
import org.inspirenxe.strata.core.node.BuiltInComponent;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;
import org.inspirenxe.strata.core.node.MnemonicTarget;

import java.util.Objects;
import java.util.Set;

/**
 * The core {@link RadioButton}. It lays out and paints like {@link CheckBoxImpl}: the box, the caption's line height
 * plus {@link CheckBoxImpl#BOX_PADDING} rounded to a whole unit, at the left edge, vertically centered, and the
 * caption {@link CheckBoxImpl#GAP} units to its right. A selected box draws the mark paint inset by three tenths of
 * the box size, rounded to a whole unit, and a focused box a one-unit focus ring. The box, the mark and the ring are
 * kept from one frame to the next while the bounds stay the same. Its group holds the selection.
 */
public final class RadioButtonImpl extends BuiltInComponent<RadioButton> implements RadioButton, MnemonicTarget {
    private static final Set<String> REQUIRED = Set.of(STYLE_NORMAL, STYLE_HOVERED, STYLE_SELECTED, STYLE_SELECTED_HOVERED);

    private final RadioGroupImpl<?> group;
    private Text text;

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
    private final BoxCaption layout = new BoxCaption(CheckBoxImpl.GAP);
    private float boxSize;

    // Kept between frames
    private final RectCache markArea = new RectCache();
    private final Rings.Ring ring = new Rings.Ring();
    private final Activation activation;

    /**
     * Creates a button in a group.
     *
     * @param ui the factory that owns the button
     * @param group the group it belongs to
     * @param text the caption
     */
    @SuppressWarnings("this-escape")
    RadioButtonImpl(final ComponentFactoryImpl ui, final RadioGroupImpl<?> group, final Text text) {
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
        this.activation = Activation.leftButton(this::activatesOnPress, () -> {
            if (this.isEnabled()) {
                this.userSelect(false);
            }
        });
        this.setCursor(CursorKind.HAND);
    }

    @Override
    public Text text() {
        return this.text;
    }

    @Override
    public RadioButton setText(final Text text) {
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
    protected Size measureMinimum(final MeasureContext context) {
        // The caption does not shrink, and measuring does not depend on the space offered
        return this.measure(context, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        this.boxSize = CheckBoxImpl.defaultBoxSize(this.layout.measureCaption(this.text, this.textStyle,
                this.disabledTextStyle, context.text()));
        return this.layout.size(this.boxSize, this.boxSize);
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
            final float inset = Math.round(this.boxSize * 0.3f);
            (enabled ? this.mark : this.markDisabled).paint(painter, this.markArea.at(box.x() + inset,
                    box.y() + inset, box.width() - inset * 2f, box.height() - inset * 2f));
        }
        if (this.isFocused() && this.focusRing != Paint.NONE) {
            this.ring.paint(painter, this.focusRing, box);
        }
        this.layout.paintCaption(painter, bounds, enabled,
                this.hostRoot() != null && this.hostRoot().showsMnemonics());
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
        if (!event.action().isDown() || !this.isEnabled()) {
            return;
        }
        if (Activation.key(event, () -> this.userSelect(false))) {
            return;
        }
        final Key key = event.key();
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

    @Override
    public boolean hasMnemonic(final int mnemonic) {
        return this.layout.mnemonic() == mnemonic;
    }

    /**
     * Selects the button for the user, as Alt with the caption's mnemonic does.
     */
    @Override
    public void activateMnemonic(final int mnemonic) {
        this.userSelect(false);
    }
}
