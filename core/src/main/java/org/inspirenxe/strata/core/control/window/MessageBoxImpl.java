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
package org.inspirenxe.strata.core.control.window;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.control.attribute.popup.Closable;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.control.window.MessageBox;
import org.inspirenxe.strata.api.control.window.Window;
import org.inspirenxe.strata.api.event.input.KeyEvent;
import org.inspirenxe.strata.api.extension.ArrangeContext;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.inspirenxe.strata.api.layout.VerticalAlignment;
import org.inspirenxe.strata.api.overlay.Overlay;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.core.node.BuiltInComposite;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * The core {@link MessageBox}: a {@link WindowImpl} holding a column of the wrapped message and a right-aligned row
 * of buttons, each {@link #BUTTON_WIDTH} units wide, 4 units apart. The box itself is centered and the window fills
 * it. An Escape press sent to the box itself, as when its dialog has nothing focused, counts as one on the window.
 */
public final class MessageBoxImpl extends BuiltInComposite<MessageBox> implements MessageBox {
    /**
     * The width of each button, in UI units.
     */
    public static final float BUTTON_WIDTH = 64f;
    /**
     * The space between the window's padding and the message and buttons when the theme sets no
     * {@value #STYLE_PADDING}: 4 on every side.
     */
    public static final Insets DEFAULT_PADDING = Insets.of(4f);

    private final ComponentFactoryImpl ui;
    private final WindowImpl window;
    private final Map<Result, Button> buttons;
    private final Map<Result, Text> captions;
    private final Label text;
    private final StackPanel row;
    private final StackPanel column;
    private Buttons buttonSet;
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
    public MessageBoxImpl(final ComponentFactoryImpl ui, final Text title, final Text message,
                          final Buttons buttonSet) {
        super(ui);
        this.ui = ui;
        this.buttons = new EnumMap<>(Result.class);
        this.captions = new EnumMap<>(Result.class);

        this.text = ui.label(message).setWrap(true);
        this.text.setMaxSize(MESSAGE_WIDTH, Float.POSITIVE_INFINITY);
        this.row = ui.stackPanel(Orientation.HORIZONTAL).setSpacing(4f);
        this.row.setAlignment(HorizontalAlignment.RIGHT, VerticalAlignment.TOP);
        this.setButtons(buttonSet);
        this.column = ui.stackPanel().setSpacing(8f).setPadding(DEFAULT_PADDING).add(this.text).add(this.row);
        this.column.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);

        this.window = new WindowImpl(ui, title);
        this.window.add(this.column);
        this.window.setAnchor(Anchor.FILL);
        // Closing the window from code answers the dismiss result without a request
        this.window.onEvent(Closable.CLOSED, event -> this.finish(this.buttonSet.dismissResult()));
        // The window's close button and Escape answer the dismiss result through the box's request
        this.window.setUserCloseHook(this::userDismiss);
        this.addChild(this.window);
        this.setAnchor(Anchor.CENTER);
    }

    private static Text caption(final Result result) {
        return switch (result) {
            case OK -> Text.translatable("strata.button.ok");
            case CANCEL -> Text.translatable("strata.button.cancel");
            case YES -> Text.translatable("strata.button.yes");
            case NO -> Text.translatable("strata.button.no");
            case CLOSE -> Text.translatable("strata.button.close");
        };
    }

    @Override
    protected void onApplyTheme(final Theme theme) {
        this.column.setPadding(theme.insetsOr(STYLE_PADDING, DEFAULT_PADDING));
    }

    @Override
    public Window window() {
        return this.window;
    }

    @Override
    public Text title() {
        return this.window.title();
    }

    @Override
    public MessageBox setTitle(final Text title) {
        this.window.setTitle(title);
        return this;
    }

    @Override
    public Text text() {
        return this.text.text();
    }

    @Override
    public MessageBox setText(final Text message) {
        this.text.setText(message);
        return this;
    }

    @Override
    public Buttons buttons() {
        return this.buttonSet;
    }

    @Override
    public MessageBox setButtons(final Buttons buttons) {
        this.buttonSet = Objects.requireNonNull(buttons, "buttons");
        this.row.clear();
        this.buttons.clear();
        for (final Result result : buttons.results()) {
            final Button button = this.ui.button(this.captions.getOrDefault(result, caption(result)))
                    .onEvent(Button.PRESSED, event -> this.answer(result));
            button.setWidth(BUTTON_WIDTH);
            this.buttons.put(result, button);
            this.row.add(button);
        }
        return this;
    }

    @Override
    public MessageBox setButtonText(final Result result, final Text caption) {
        this.captions.put(Objects.requireNonNull(result, "result"), Objects.requireNonNull(caption, "caption"));
        final Button button = this.buttons.get(result);
        if (button != null) {
            button.setText(caption);
        }
        return this;
    }

    /**
     * Answers for the user when a button is pressed, through an {@link #ANSWER} request, unless the box has already
     * answered.
     */
    private void answer(final Result result) {
        if (!this.done) {
            this.request(ANSWER, result, this::finish);
        }
    }

    /**
     * Answers the dismiss result for the user once the window accepted a close by its close button or Escape,
     * through an {@link #ANSWER} request, and closes the window if the request was not canceled. A box that has
     * already answered just closes the window.
     */
    private void userDismiss() {
        if (this.done) {
            this.window.close();
            return;
        }
        this.request(ANSWER, this.buttonSet.dismissResult(), result -> {
            this.finish(result);
            this.window.close();
        });
    }

    /**
     * Reports an answer once through {@link #ANSWERED}, then closes the overlay showing the box.
     */
    private void finish(final Result result) {
        if (this.done) {
            return;
        }
        this.done = true;
        this.report(ANSWERED, result, result);
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

    @Override
    protected void onKey(final KeyEvent event) {
        // With nothing focused, as after a press on the window's background, a dialog's keys come to the box itself
        this.window.escape(event);
    }
}
