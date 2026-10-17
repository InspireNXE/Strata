package org.inspirenxe.strata.core.widget;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.extension.ArrangeContext;
import org.inspirenxe.strata.api.extension.CustomComposite;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.geom.Size;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.inspirenxe.strata.api.layout.VerticalAlignment;
import org.inspirenxe.strata.api.overlay.Overlay;
import org.inspirenxe.strata.api.widget.Button;
import org.inspirenxe.strata.api.widget.Label;
import org.inspirenxe.strata.api.widget.MessageBox;
import org.inspirenxe.strata.api.widget.Window;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * The core {@link MessageBox}: a {@link WindowImpl} holding a column of the wrapped message and a right-aligned row
 * of buttons, each {@link #BUTTON_WIDTH} units wide, 4 units apart. The box itself is centered and the window fills
 * it.
 */
public final class MessageBoxImpl extends CustomComposite<MessageBox> implements MessageBox {
    /**
     * The width of each button, in UI units.
     */
    public static final float BUTTON_WIDTH = 64f;

    private final ComponentFactory ui;
    private final WindowImpl window;
    private final Map<Result, Button> buttons;
    private final List<Consumer<Result>> listeners;
    private final Result dismissResult;
    private boolean done;

    /**
     * Creates a message box.
     *
     * @param ui the factory that owns the box
     * @param title the window's title
     * @param message the message
     * @param buttonSet which buttons it has
     */
    @SuppressWarnings("this-escape")
    public MessageBoxImpl(final ComponentFactory ui, final String title, final String message, final Buttons buttonSet) {
        super(ui);
        this.ui = ui;
        this.buttons = new EnumMap<>(Result.class);
        this.listeners = new ArrayList<>();
        this.dismissResult = buttonSet.dismissResult();

        final Label text = ui.label(message).setWrap(true);
        text.setMaxSize(MESSAGE_WIDTH, Float.POSITIVE_INFINITY);
        final StackPanel row = ui.stackPanel(Orientation.HORIZONTAL).setSpacing(4f);
        row.setAlignment(HorizontalAlignment.RIGHT, VerticalAlignment.TOP);
        for (final Result result : buttonSet.results()) {
            final Button button = ui.button(caption(result)).onAction(() -> this.finish(result));
            button.setWidth(BUTTON_WIDTH);
            this.buttons.put(result, button);
            row.add(button);
        }
        final StackPanel column = ui.stackPanel().setSpacing(8f).setPadding(4f).add(text).add(row);
        column.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);

        this.window = new WindowImpl(ui, title);
        this.window.add(column);
        this.window.setAnchor(Anchor.FILL);
        this.window.onClose(() -> this.finish(this.dismissResult));
        this.addChild(this.window);
        this.setAnchor(Anchor.CENTER);
    }

    private static String caption(final Result result) {
        return switch (result) {
            case OK -> "OK";
            case CANCEL -> "Cancel";
            case YES -> "Yes";
            case NO -> "No";
            case CLOSE -> "Close";
        };
    }

    @Override
    public Window window() {
        return this.window;
    }

    @Override
    public MessageBox setButtonText(final Result result, final String caption) {
        final Button button = this.buttons.get(Objects.requireNonNull(result, "result"));
        if (button != null) {
            button.setText(caption);
        }
        return this;
    }

    @Override
    public MessageBox onResult(final Consumer<Result> listener) {
        this.listeners.add(Objects.requireNonNull(listener, "listener"));
        return this;
    }

    /**
     * Reports a result once, then closes the overlay showing the box.
     */
    private void finish(final Result result) {
        if (this.done) {
            return;
        }
        this.done = true;
        for (final Consumer<Result> listener : List.copyOf(this.listeners)) {
            listener.accept(result);
        }
        if (this.isAttached()) {
            this.ui.overlays().of(this).ifPresent(Overlay::close);
        }
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        return context.measure(this.window, availableWidth, availableHeight);
    }

    @Override
    protected void arrange(final ArrangeContext context, final Rect bounds) {
        for (final Component<?> child : this.childList()) {
            context.arrange(child, bounds);
        }
    }
}
