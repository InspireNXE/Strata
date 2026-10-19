package org.inspirenxe.strata.core.widget;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.event.KeyEvent;
import org.inspirenxe.strata.api.event.PointerButtonEvent;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
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
import org.inspirenxe.strata.api.widget.ToggleSwitch;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

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

    private final List<Consumer<Boolean>> listeners;
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
        this.listeners = new ArrayList<>();
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
        this.setFocusable(true);
        this.setTabbable(true);
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
    public boolean isOn() {
        return this.on;
    }

    @Override
    public ToggleSwitch setOn(final boolean on) {
        if (this.on != on) {
            this.on = on;
            for (final Consumer<Boolean> listener : List.copyOf(this.listeners)) {
                listener.accept(on);
            }
        }
        return this;
    }

    @Override
    public void toggle() {
        if (this.isEnabled()) {
            this.setOn(!this.on);
        }
    }

    @Override
    public ToggleSwitch onToggled(final Consumer<Boolean> listener) {
        this.listeners.add(Objects.requireNonNull(listener, "listener"));
        return this;
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
            this.toggle();
            event.consume();
        }
    }

    @Override
    protected void onKey(final KeyEvent event) {
        if (event.action() == KeyAction.PRESS && (event.key().isEnter() || event.key() == Key.SPACE)) {
            this.toggle();
            event.consume();
        }
    }

    @Override
    protected Object snapshotState() {
        return new ToggleSwitch.State(this.on, this.text);
    }
}
