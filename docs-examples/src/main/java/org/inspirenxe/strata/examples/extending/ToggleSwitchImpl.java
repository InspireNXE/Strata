package org.inspirenxe.strata.examples.extending;

// --8<-- [start:impl]
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
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * The switch implementation. Other code sees only {@link ToggleSwitch}.
 */
public final class ToggleSwitchImpl extends CustomComponent<ToggleSwitch> implements ToggleSwitch {
    private static final float TRACK_WIDTH = 20f;
    private static final float TRACK_HEIGHT = 10f;
    private static final float KNOB = 8f;
    private static final float GAP = 4f;

    private final List<Consumer<Boolean>> listeners;
    private final String text;
    private boolean on;

    // Read from the theme in onApplyTheme
    private Paint track;
    private Paint trackOn;
    private Paint knob;
    private TextStyle textStyle;

    // Cached during measure, used during paint
    private TextSpan span;
    private float lineHeight;

    public ToggleSwitchImpl(final ComponentFactory factory, final String text) {
        super(factory);
        this.text = Objects.requireNonNull(text, "text");
        this.listeners = new ArrayList<>();
        this.track = Paint.NONE;
        this.trackOn = Paint.NONE;
        this.knob = Paint.NONE;
        this.textStyle = TextStyle.DEFAULT;
        this.span = new TextSpan(text, this.textStyle);
        this.setFocusable(true);
        this.setTabbable(true);
        this.setCursor(CursorKind.HAND);
    }

    @Override
    public boolean isOn() {
        return this.on;
    }

    @Override
    public ToggleSwitch setOn(final boolean on) {
        if (this.on != on) {
            this.on = on;
            // Being on does not change the size, so no invalidateLayout() here
            for (final Consumer<Boolean> listener : this.listeners) {
                listener.accept(on);
            }
        }
        return this.self();
    }

    @Override
    public ToggleSwitch onChange(final Consumer<Boolean> listener) {
        this.listeners.add(Objects.requireNonNull(listener, "listener"));
        return this.self();
    }

    // Theme

    @Override
    protected Set<String> requiredStyleKeys() {
        return Set.of(STYLE_TRACK, STYLE_TRACK_ON, STYLE_KNOB);
    }

    @Override
    protected void onApplyTheme(final Theme theme) {
        this.track = theme.paintOr(STYLE_TRACK, Paint.NONE);
        this.trackOn = theme.paintOr(STYLE_TRACK_ON, Paint.NONE);
        this.knob = theme.paintOr(STYLE_KNOB, Paint.NONE);
        // Optional keys get a fallback instead of being required
        this.textStyle = theme.textStyleOr(STYLE_TEXT, TextStyle.DEFAULT);
    }

    // Layout and drawing

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        this.span = new TextSpan(this.text, this.textStyle);
        this.lineHeight = context.text().lineHeight(this.textStyle);
        final float textWidth = context.text().width(this.span);
        return new Size(TRACK_WIDTH + GAP + textWidth, Math.max(TRACK_HEIGHT, this.lineHeight));
    }

    @Override
    protected void paint(final Painter painter, final Rect bounds) {
        final float middle = bounds.y() + bounds.height() / 2f;
        final Rect trackArea = new Rect(bounds.x(), middle - TRACK_HEIGHT / 2f, TRACK_WIDTH, TRACK_HEIGHT);
        (this.on ? this.trackOn : this.track).paint(painter, trackArea);

        final float knobX = this.on ? trackArea.right() - KNOB - 1f : trackArea.x() + 1f;
        this.knob.paint(painter, new Rect(knobX, trackArea.y() + 1f, KNOB, KNOB));

        painter.text(this.span, trackArea.right() + GAP, middle - this.lineHeight / 2f);
    }

    // Input

    @Override
    protected void onClick(final PointerButtonEvent event) {
        if (event.button() == MouseButton.LEFT) {
            this.setOn(!this.on);
            event.consume();
        }
    }

    @Override
    protected void onKey(final KeyEvent event) {
        if (event.action() == KeyAction.PRESS && event.key() == Key.SPACE) {
            this.setOn(!this.on);
            event.consume();
        }
    }

    // Snapshots

    @Override
    protected Object snapshotState() {
        return new State(this.on, this.text);
    }
}
// --8<-- [end:impl]
