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
import org.inspirenxe.strata.api.control.button.ToggleSwitch;
import org.inspirenxe.strata.api.event.input.KeyEvent;
import org.inspirenxe.strata.api.event.input.PointerButtonEvent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.core.control.Activation;
import org.inspirenxe.strata.core.control.BoxCaption;
import org.inspirenxe.strata.core.control.Commands;
import org.inspirenxe.strata.core.control.Motion;
import org.inspirenxe.strata.core.control.RectCache;
import org.inspirenxe.strata.core.control.Rings;
import org.inspirenxe.strata.core.node.BuiltInComponent;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * The core {@link ToggleSwitch}. The track sits at the left edge, vertically centered, and the caption
 * {@link #GAP} units to its right. The knob slides between its two ends over the theme's
 * {@value ToggleSwitch#STYLE_ANIMATION} metric, through a {@link Motion} of its place from 0 (off) to 1 (on), and
 * jumps when the metric is 0 or the switch is detached. The track, the knob and the focus ring are kept from one
 * frame to the next while they stay in place, so painting allocates nothing then.
 */
public final class ToggleSwitchImpl extends BuiltInComponent<ToggleSwitch> implements ToggleSwitch {
    /**
     * The gap between the track and the caption, in UI units.
     */
    public static final float GAP = 4f;
    /**
     * How far the track grows around a box a line tall and two lines wide when the theme sets no
     * {@value #STYLE_TRACK_PADDING}: 4 at the sides and 2 at the top and bottom.
     */
    public static final Insets DEFAULT_TRACK_PADDING = Insets.of(4f, 2f);
    /**
     * The space between the knob and the track's edges when the theme sets no {@value #STYLE_KNOB_PADDING}: 2 on
     * every side.
     */
    public static final Insets DEFAULT_KNOB_PADDING = Insets.of(2f);
    private static final Set<String> REQUIRED = Set.of(STYLE_TRACK, STYLE_TRACK_ON, STYLE_KNOB);

    private Text text;
    private boolean on;
    // Where the knob is drawn, from 0 at the left end to 1 at the right
    private float knobPlace;
    private long animationMillis;
    private final Motion knobMotion = new Motion(place -> {
        this.knobPlace = place;
        this.repaint();
    });

    private Paint track;
    private Paint trackOn;
    private Paint trackDisabled;
    private Paint knob;
    private Paint knobHovered;
    private Paint knobDisabled;
    private Paint focusRing;
    private Insets trackPadding = DEFAULT_TRACK_PADDING;
    private Insets knobPadding = DEFAULT_KNOB_PADDING;
    private TextStyle textStyle;
    private TextStyle disabledTextStyle;

    // Computed during measure
    private final BoxCaption layout = new BoxCaption(GAP);
    private float trackHeight;
    private float trackWidth;

    // Kept between frames
    private final RectCache knobRect = new RectCache();
    private final Rings.Ring ring = new Rings.Ring();
    private final Activation activation;
    private Command.Toggle command;

    /**
     * Creates a switch that is off.
     *
     * @param ui the factory that owns the switch
     * @param text the caption
     */
    @SuppressWarnings("this-escape")
    public ToggleSwitchImpl(final ComponentFactoryImpl ui, final Text text) {
        super(ui);
        this.text = Objects.requireNonNull(text, "text");
        this.track = Paint.NONE;
        this.trackOn = Paint.NONE;
        this.trackDisabled = Paint.NONE;
        this.knob = Paint.NONE;
        this.knobHovered = Paint.NONE;
        this.knobDisabled = Paint.NONE;
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
    public ToggleSwitch setText(final Text text) {
        this.text = Objects.requireNonNull(text, "text");
        this.invalidateLayout();
        return this;
    }

    @Override
    public boolean isSelected() {
        return this.on;
    }

    @Override
    public ToggleSwitch setSelected(final boolean on) {
        if (this.on != on) {
            this.on = on;
            this.knobMotion.move(this.motionAnimator(), this.animationMillis, this.knobPlace, on ? 1f : 0f);
            this.report(TOGGLED, on, !on);
        }
        return this;
    }

    @Override
    public ToggleSwitch toggle() {
        if (this.isEnabled()) {
            this.setSelected(!this.on);
        }
        return this;
    }

    /**
     * Flips the switch for the user through {@link ToggleSwitch#TOGGLE}, applying the final value unless it is
     * null. Does nothing while disabled.
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
    protected Set<String> requiredStyleKeys() {
        return REQUIRED;
    }

    @Override
    protected void onApplyTheme(final Theme theme) {
        this.track = theme.paintOr(STYLE_TRACK, Paint.NONE);
        this.trackOn = theme.paintOr(STYLE_TRACK_ON, this.track);
        this.trackDisabled = theme.paintOr(STYLE_TRACK_DISABLED, this.track);
        this.knob = theme.paintOr(STYLE_KNOB, Paint.NONE);
        this.knobHovered = theme.paintOr(STYLE_KNOB_HOVERED, this.knob);
        this.knobDisabled = theme.paintOr(STYLE_KNOB_DISABLED, this.knob);
        this.focusRing = theme.paintOr(STYLE_FOCUS, Paint.NONE);
        this.textStyle = theme.textStyleOr(STYLE_TEXT, TextStyle.DEFAULT);
        this.disabledTextStyle = theme.textStyleOr(STYLE_TEXT_DISABLED, this.textStyle);
        this.trackPadding = theme.insetsOr(STYLE_TRACK_PADDING, DEFAULT_TRACK_PADDING);
        this.knobPadding = theme.insetsOr(STYLE_KNOB_PADDING, DEFAULT_KNOB_PADDING);
        this.animationMillis = Motion.millis(theme, STYLE_ANIMATION);
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
        this.trackHeight = captionHeight + this.trackPadding.vertical();
        this.trackWidth = captionHeight * 2f + this.trackPadding.horizontal();
        return this.layout.size(this.trackWidth, this.trackHeight);
    }

    @Override
    protected float baseline() {
        return this.layout.baseline(this.desiredHeight());
    }

    @Override
    protected void paint(final Painter painter, final Rect bounds) {
        final Rect bar = this.layout.box(bounds);
        final boolean enabled = this.isEnabled();
        (enabled ? (this.on ? this.trackOn : this.track) : this.trackDisabled).paint(painter, bar);

        final Insets inset = this.knobPadding;
        final float knobSize = Math.max(0f, this.trackHeight - inset.vertical());
        final float left = bar.x() + inset.left();
        final float knobX = left + (bar.right() - inset.right() - knobSize - left) * this.knobPlace;
        final Paint face;
        if (!enabled) {
            face = this.knobDisabled;
        } else {
            face = this.isHovered() || this.isFocused() ? this.knobHovered : this.knob;
        }
        face.paint(painter, this.knobRect.at(knobX, bar.y() + inset.top(), knobSize, knobSize));

        if (this.isFocused() && this.focusRing != Paint.NONE) {
            this.ring.paint(painter, this.focusRing, bar);
        }
        this.layout.paintCaption(painter, bounds, enabled);
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
        return new ToggleSwitch.State(this.on, this.text);
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
    public ToggleSwitch setCommand(final Command.Toggle command) {
        this.command = command;
        Commands.follow(this.node(), command);
        return this;
    }
}
