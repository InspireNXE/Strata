package org.inspirenxe.strata.examples.extending;

// --8<-- [start:impl]
import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.CursorKind;
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
 * The check box implementation. Other code sees only {@link CheckBox}.
 */
public final class CheckBoxImpl extends CustomComponent<CheckBox> implements CheckBox {
    private static final float BOX = 10f;
    private static final float GAP = 4f;

    private final List<Consumer<Boolean>> listeners;
    private final String text;
    private boolean checked;

    // Read from the theme in onApplyTheme
    private Paint box;
    private Paint check;
    private TextStyle textStyle;

    // Cached during measure, used during paint
    private TextSpan span;
    private float lineHeight;

    public CheckBoxImpl(final ComponentFactory factory, final String text) {
        super(factory);
        this.text = Objects.requireNonNull(text, "text");
        this.listeners = new ArrayList<>();
        this.box = Paint.NONE;
        this.check = Paint.NONE;
        this.textStyle = TextStyle.DEFAULT;
        this.span = new TextSpan(text, this.textStyle);
        this.setFocusable(true);
        this.setTabbable(true);
        this.setCursor(CursorKind.HAND);
    }

    @Override
    public boolean isChecked() {
        return this.checked;
    }

    @Override
    public CheckBox setChecked(final boolean checked) {
        if (this.checked != checked) {
            this.checked = checked;
            // Being checked does not change the size, so no invalidateLayout() here
            for (final Consumer<Boolean> listener : this.listeners) {
                listener.accept(checked);
            }
        }
        return this.self();
    }

    @Override
    public CheckBox onChange(final Consumer<Boolean> listener) {
        this.listeners.add(Objects.requireNonNull(listener, "listener"));
        return this.self();
    }

    // Theme

    @Override
    protected Set<String> requiredStyleKeys() {
        return Set.of(STYLE_BOX, STYLE_CHECK);
    }

    @Override
    protected void onApplyTheme(final Theme theme) {
        this.box = theme.paintOr(STYLE_BOX, Paint.NONE);
        this.check = theme.paintOr(STYLE_CHECK, Paint.NONE);
        // Optional keys get a fallback instead of being required
        this.textStyle = theme.textStyleOr(STYLE_TEXT, TextStyle.DEFAULT);
    }

    // Layout and drawing

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        this.span = new TextSpan(this.text, this.textStyle);
        this.lineHeight = context.text().lineHeight(this.textStyle);
        final float textWidth = context.text().width(this.span);
        return new Size(BOX + GAP + textWidth, Math.max(BOX, this.lineHeight));
    }

    @Override
    protected void paint(final Painter painter, final Rect bounds) {
        final Rect square = new Rect(bounds.x(), bounds.y() + (bounds.height() - BOX) / 2f, BOX, BOX);
        this.box.paint(painter, square);
        if (this.checked) {
            this.check.paint(painter, square.shrink(Insets.of(2f)));
        }
        painter.text(this.span, square.right() + GAP, bounds.y() + (bounds.height() - this.lineHeight) / 2f);
    }

    // Input

    @Override
    protected void onClick(final PointerButtonEvent event) {
        if (event.button() == MouseButton.LEFT) {
            this.setChecked(!this.checked);
            event.consume();
        }
    }

    @Override
    protected void onKey(final KeyEvent event) {
        if (event.action() == KeyAction.PRESS && event.key() == Key.SPACE) {
            this.setChecked(!this.checked);
            event.consume();
        }
    }

    // Snapshots

    @Override
    protected Object snapshotState() {
        return new State(this.checked, this.text);
    }
}
// --8<-- [end:impl]
