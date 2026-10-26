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
import org.inspirenxe.strata.api.control.button.IconPlacement;
import org.inspirenxe.strata.api.control.button.ToggleButton;
import org.inspirenxe.strata.api.event.input.KeyEvent;
import org.inspirenxe.strata.api.event.input.PointerButtonEvent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.visual.Visual;
import org.inspirenxe.strata.core.control.Activation;
import org.inspirenxe.strata.core.control.Commands;
import org.inspirenxe.strata.core.node.BuiltInComponent;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;
import org.inspirenxe.strata.core.node.MnemonicTarget;
import org.inspirenxe.strata.core.text.TextLine;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * The core {@link ToggleButton}: a {@link ButtonImpl} that stays down while on. It shares {@link ButtonContent} with
 * the other buttons, so it measures, pads and draws its caption and icon as they do. Its face is the disabled paint
 * when disabled (the selected disabled paint while on), the pressed paint while held under the pointer, and
 * otherwise the selected or selected hovered paint while on and the normal or hovered paint while off. A focused
 * button also draws a one-unit focus ring when the theme has one.
 *
 * <p>It is focusable and tabbable, shows a hand cursor, is at least {@link ButtonImpl#MIN_WIDTH} wide and measures
 * at least {@link ButtonImpl#MIN_HEIGHT} tall, as a framed button does.</p>
 */
public final class ToggleButtonImpl extends BuiltInComponent<ToggleButton> implements ToggleButton, MnemonicTarget {
    private final ButtonContent content;
    private final Activation activation;
    private boolean on;
    private Command.Toggle command;
    private Paint selected;
    private Paint selectedHovered;
    private Paint selectedDisabled;

    /**
     * Creates a toggle button that is off.
     *
     * @param ui the factory that owns the button
     * @param text the caption
     */
    @SuppressWarnings("this-escape")
    public ToggleButtonImpl(final ComponentFactoryImpl ui, final Text text) {
        super(ui);
        this.content = new ButtonContent(text);
        this.activation = Activation.leftButton(this::activatesOnPress, this::userToggle);
        this.selected = Paint.NONE;
        this.selectedHovered = Paint.NONE;
        this.selectedDisabled = Paint.NONE;
        this.setCursor(CursorKind.HAND);
        this.setMinSize(ButtonImpl.MIN_WIDTH, 0f);
    }

    @Override
    public Text text() {
        return this.content.text();
    }

    @Override
    public ToggleButton setText(final Text text) {
        this.content.setText(text);
        this.invalidateLayout();
        return this;
    }

    @Override
    public ToggleButton setTextPadding(final Insets padding) {
        this.content.setTextPadding(Objects.requireNonNull(padding, "padding"));
        this.invalidateLayout();
        return this;
    }

    @Override
    public ToggleButton clearTextPadding() {
        this.content.setTextPadding(null);
        this.invalidateLayout();
        return this;
    }

    @Override
    public Optional<SpriteId> iconSprite() {
        return Optional.ofNullable(this.content.iconSprite());
    }

    @Override
    public Optional<Visual> iconVisual() {
        return Optional.ofNullable(this.content.iconVisual());
    }

    @Override
    public ToggleButton setIcon(final SpriteId sprite) {
        this.content.setIcon(sprite);
        this.invalidateLayout();
        return this;
    }

    @Override
    public ToggleButton setIcon(final Visual visual) {
        this.content.setIcon(visual);
        this.invalidateLayout();
        return this;
    }

    @Override
    public ToggleButton clearIcon() {
        this.content.clearIcon();
        this.invalidateLayout();
        return this;
    }

    @Override
    public float iconWidth() {
        return this.content.iconWidth();
    }

    @Override
    public float iconHeight() {
        return this.content.iconHeight();
    }

    @Override
    public ToggleButton setIconSize(final float width, final float height) {
        this.content.setIconSize(width, height);
        this.invalidateLayout();
        return this;
    }

    @Override
    public IconPlacement iconPlacement() {
        return this.content.placement();
    }

    @Override
    public ToggleButton setIconPlacement(final IconPlacement placement) {
        this.content.setPlacement(placement);
        this.invalidateLayout();
        return this;
    }

    @Override
    public HorizontalAlignment captionAlignment() {
        return this.content.alignment();
    }

    @Override
    public ToggleButton setCaptionAlignment(final HorizontalAlignment alignment) {
        this.content.setAlignment(alignment);
        this.repaint();
        return this;
    }

    @Override
    public boolean isSelected() {
        return this.on;
    }

    @Override
    public ToggleButton setSelected(final boolean on) {
        if (this.on != on) {
            this.on = on;
            this.report(TOGGLED, on, !on);
        }
        return this;
    }

    @Override
    public ToggleButton toggle() {
        if (this.isEnabled()) {
            this.setSelected(!this.on);
        }
        return this;
    }

    /**
     * Flips the button for the user through {@link ToggleButton#TOGGLE}, applying the final value unless it is null.
     * Does nothing while disabled.
     */
    private void userToggle() {
        if (this.isEnabled()) {
            this.request(TOGGLE, !this.on, on -> {
                if (on != null) {
                    this.setSelected(on);
                }
            });
        }
    }

    @Override
    public Optional<Command.Toggle> command() {
        return Optional.ofNullable(this.command);
    }

    /**
     * Shows a toggle: takes its text, binds {@link ToggleButton#ENABLED} to its enabled source and
     * {@link ToggleButton#SELECTED} to its state both ways, or takes the bindings off again for null.
     */
    @Override
    public ToggleButton setCommand(final Command.Toggle command) {
        this.command = command;
        Commands.follow(this.node(), command);
        return this;
    }

    @Override
    public boolean hasMnemonic(final int mnemonic) {
        return this.content.mnemonic() == mnemonic;
    }

    /**
     * Flips the button for the user, as Alt with its mnemonic does.
     */
    @Override
    public void activateMnemonic(final int mnemonic) {
        this.userToggle();
    }

    @Override
    protected Set<String> requiredStyleKeys() {
        return ButtonContent.REQUIRED;
    }

    @Override
    protected void onApplyTheme(final Theme theme) {
        this.content.applyTheme(theme);
        this.selected = theme.paintOr(STYLE_SELECTED, this.content.pressed);
        this.selectedHovered = theme.paintOr(STYLE_SELECTED_HOVERED, this.selected);
        this.selectedDisabled = theme.paintOr(STYLE_SELECTED_DISABLED, this.content.disabled);
    }

    @Override
    protected Size measureMinimum(final MeasureContext context) {
        // The caption does not shrink, and measuring does not depend on the space offered
        return this.measure(context, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        this.content.measure(context.text(), false);
        final Insets padding = this.content.padding(false);
        return new Size(this.content.contentWidth() + padding.horizontal(),
                Math.max(ButtonImpl.MIN_HEIGHT, this.content.contentHeight() + padding.vertical()));
    }

    @Override
    protected float baseline() {
        return this.content.baseline(this.desiredHeight(), this.content.padding(false));
    }

    @Override
    protected void paint(final Painter painter, final Rect bounds) {
        final ButtonContent content = this.content;
        final boolean enabled = this.isEnabled();
        final boolean lit = this.isHovered() || this.isFocused();
        final boolean held = this.isPressed() && this.isHovered();
        final Paint face;
        if (!enabled) {
            face = this.on ? this.selectedDisabled : content.disabled;
        } else if (held) {
            face = content.pressed;
        } else if (this.on) {
            face = lit ? this.selectedHovered : this.selected;
        } else {
            face = lit ? content.hovered : content.normal;
        }
        face.paint(painter, bounds);
        if (this.isFocused()) {
            content.paintFocusRing(painter, bounds);
        }
        TextLine shown = enabled ? content.caption : content.disabledCaption;
        if (this.hostRoot() != null && this.hostRoot().showsMnemonics()) {
            shown = content.marked(shown);
        }
        if (content.paintContent(painter, bounds, content.padding(false), enabled, shown, this.environment().millis(),
                this.isRightToLeft())) {
            this.repaintNextFrame();
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
        return new ToggleButton.State(this.on, this.content.text());
    }
}
