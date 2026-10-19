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

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.control.button.ToggleSwitch;
import org.inspirenxe.strata.api.event.input.KeyEvent;
import org.inspirenxe.strata.api.event.input.PointerButtonEvent;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
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
 * The core {@link ToggleSwitch}. The track sits at the left edge, vertically centered, and the caption
 * {@link #GAP} units to its right. The knob jumps between its two ends; there is no animation.
 */
public final class ToggleSwitchImpl extends CustomComponent<ToggleSwitch> implements ToggleSwitch {
    /**
     * The gap between the track and the caption, in UI units.
     */
    public static final float GAP = 4f;
    /**
     * Added to the caption's line height for the track's height, in UI units.
     */
    public static final float TRACK_PADDING = 4f;
    /**
     * The space between the knob and the track's edges, in UI units.
     */
    public static final float KNOB_INSET = 2f;
    private static final Set<String> REQUIRED = Set.of(STYLE_TRACK, STYLE_TRACK_ON, STYLE_KNOB);

    private String text;
    private boolean on;

    private Paint track;
    private Paint trackOn;
    private Paint trackDisabled;
    private Paint knob;
    private Paint knobHovered;
    private Paint knobDisabled;
    private Paint focusRing;
    private TextStyle textStyle;
    private TextStyle disabledTextStyle;

    // Computed during measure
    private TextSpan caption;
    private TextSpan disabledCaption;
    private float captionHeight;
    private float trackHeight;

    /**
     * Creates a switch that is off.
     *
     * @param ui the factory that owns the switch
     * @param text the caption
     */
    @SuppressWarnings("this-escape")
    public ToggleSwitchImpl(final ComponentFactory ui, final String text) {
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
        this.caption = TextSpan.of(text);
        this.disabledCaption = this.caption;
        this.setCursor(CursorKind.HAND);
    }

    @Override
    public String text() {
        return this.text;
    }

    @Override
    public ToggleSwitch setText(final String text) {
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
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        final TextMetrics metrics = context.text();
        this.caption = new TextSpan(this.text, this.textStyle);
        this.disabledCaption = new TextSpan(this.text, this.disabledTextStyle);
        this.captionHeight = Math.max(metrics.lineHeight(this.textStyle), metrics.lineHeight(this.disabledTextStyle));
        this.trackHeight = this.captionHeight + TRACK_PADDING;
        final float trackWidth = this.trackHeight * 2f;
        final float captionWidth = Math.max(metrics.width(this.caption), metrics.width(this.disabledCaption));
        final float width = this.text.isEmpty() ? trackWidth : trackWidth + GAP + captionWidth;
        return new Size(width, Math.max(this.trackHeight, this.captionHeight));
    }

    @Override
    protected void paint(final Painter painter, final Rect bounds) {
        final float middle = bounds.y() + bounds.height() * 0.5f;
        final Rect bar = new Rect(bounds.x(), middle - this.trackHeight * 0.5f, this.trackHeight * 2f, this.trackHeight);
        final boolean enabled = this.isEnabled();
        (enabled ? (this.on ? this.trackOn : this.track) : this.trackDisabled).paint(painter, bar);

        final float knobSize = Math.max(0f, this.trackHeight - KNOB_INSET * 2f);
        final float knobX = this.on ? bar.right() - KNOB_INSET - knobSize : bar.x() + KNOB_INSET;
        final Paint face;
        if (!enabled) {
            face = this.knobDisabled;
        } else {
            face = this.isHovered() || this.isFocused() ? this.knobHovered : this.knob;
        }
        face.paint(painter, new Rect(knobX, bar.y() + KNOB_INSET, knobSize, knobSize));

        if (this.isFocused() && this.focusRing != Paint.NONE) {
            Rings.paint(painter, this.focusRing, bar);
        }
        if (!this.text.isEmpty()) {
            painter.text(enabled ? this.caption : this.disabledCaption, bar.right() + GAP, middle - this.captionHeight * 0.5f);
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
        return new ToggleSwitch.State(this.on, this.text);
    }
}
