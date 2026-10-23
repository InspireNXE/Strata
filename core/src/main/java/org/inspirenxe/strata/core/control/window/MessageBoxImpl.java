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
import org.inspirenxe.strata.api.control.display.InfoBar;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.control.window.MessageBox;
import org.inspirenxe.strata.api.control.window.Window;
import org.inspirenxe.strata.api.event.input.KeyEvent;
import org.inspirenxe.strata.api.extension.ArrangeContext;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.inspirenxe.strata.api.layout.VerticalAlignment;
import org.inspirenxe.strata.api.overlay.Overlay;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.core.control.Adoption;
import org.inspirenxe.strata.core.node.BuiltInComponent;
import org.inspirenxe.strata.core.node.BuiltInComposite;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;
import org.inspirenxe.strata.core.node.ContainerNode;
import org.inspirenxe.strata.core.node.InitialFocus;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * The core {@link MessageBox}: a {@link WindowImpl} holding a column of the wrapped message and a right-aligned row
 * of buttons, each {@link #BUTTON_WIDTH} units wide, 4 units apart, unless the theme's metrics say otherwise. With
 * a button width of 0 the message is given its whole wrap width and the buttons split it evenly, less the gaps, each
 * no wider than the theme's maximum, in a centered row. The box itself is centered and the window fills it. An Escape
 * press sent to the box itself, as when its dialog has nothing focused, counts as one on the window. The default
 * result's button is a default button, which the root focuses when the box opens as a dialog, and the dismiss
 * result's button is a cancel button, which Escape presses in place of closing the window. A box the host
 * lets go of before it answered, as when the overlay showing it closes from code, answers the dismiss result without
 * a request.
 */
public final class MessageBoxImpl extends BuiltInComposite<MessageBox> implements MessageBox, InitialFocus {
    /**
     * The width of each button, in UI units.
     */
    public static final float BUTTON_WIDTH = 64f;
    /**
     * The space between the window's padding and the message and buttons when the theme sets no
     * {@value #STYLE_PADDING}: 4 on every side.
     */
    public static final Insets DEFAULT_PADDING = Insets.of(4f);
    /**
     * The space between the severity's icon and the message, in UI units.
     */
    public static final float ICON_GAP = 8f;
    private static final String[] ICONS = {STYLE_ICON_INFO, STYLE_ICON_SUCCESS, STYLE_ICON_WARNING, STYLE_ICON_ERROR};
    private static final String[] INFO_BAR_ICONS = {InfoBar.STYLE_ICON_INFO, InfoBar.STYLE_ICON_SUCCESS,
            InfoBar.STYLE_ICON_WARNING, InfoBar.STYLE_ICON_ERROR};

    private final ComponentFactoryImpl ui;
    private final WindowImpl window;
    private final Map<Result, Button> buttons;
    private final Map<Result, Text> captions;
    private final Label text;
    private final StackPanel row;
    private final StackPanel column;
    private final StackPanel messageRow;
    private final SeverityIcon icon;
    private final Paint[] icons;
    private List<Result> results;
    private Result dismiss;
    private InfoBar.Severity severity;
    private Component<?> content;
    // Null for the first result of the set
    private Result defaultResult;
    private boolean done;
    private float messageWidth = MESSAGE_WIDTH;
    private float buttonWidth = BUTTON_WIDTH;
    private float buttonWidthMaximum;

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
        this.buttons = new LinkedHashMap<>();
        this.captions = new HashMap<>();
        this.icons = new Paint[InfoBar.Severity.values().length];

        this.text = ui.label(message).setWrap(true);
        this.text.setMaxSize(MESSAGE_WIDTH, Float.POSITIVE_INFINITY);
        this.row = ui.stackPanel(Orientation.HORIZONTAL).setSpacing(4f);
        this.row.setAlignment(HorizontalAlignment.RIGHT, VerticalAlignment.TOP);
        this.setButtons(buttonSet);
        this.icon = new SeverityIcon(ui);
        this.icon.setVisible(false);
        this.messageRow = ui.stackPanel(Orientation.HORIZONTAL).setSpacing(ICON_GAP).add(this.icon).add(this.text);
        this.messageRow.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);
        this.column = ui.stackPanel().setSpacing(8f).setPadding(DEFAULT_PADDING).add(this.messageRow).add(this.row);
        this.column.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);

        this.window = new WindowImpl(ui, title);
        this.window.add(this.column);
        this.window.setAnchor(Anchor.FILL);
        // Closing the window from code answers the dismiss result without a request
        this.window.onEvent(Closable.CLOSED, event -> this.finish(this.dismiss));
        // The window's close button and Escape answer the dismiss result through the box's request
        this.window.setUserCloseHook(this::userDismiss);
        this.addChild(this.window);
        this.setAnchor(Anchor.CENTER);
    }

    @Override
    protected void onApplyTheme(final Theme theme) {
        this.column.setPadding(theme.insetsOr(STYLE_PADDING, DEFAULT_PADDING));
        this.column.setSpacing(Math.max(0f, theme.metricOr(STYLE_SPACING, 8f)));
        this.messageWidth = Math.max(0f, theme.metricOr(STYLE_MESSAGE_WIDTH, MESSAGE_WIDTH));
        this.buttonWidth = Math.max(0f, theme.metricOr(STYLE_BUTTON_WIDTH, BUTTON_WIDTH));
        this.buttonWidthMaximum = Math.max(0f, theme.metricOr(STYLE_BUTTON_WIDTH_MAXIMUM, 0f));
        this.row.setSpacing(Math.max(0f, theme.metricOr(STYLE_BUTTON_GAP, 4f)));
        this.text.setTextAlignment(theme.metricOr(STYLE_MESSAGE_CENTER, 0f) != 0f
                ? HorizontalAlignment.CENTER : HorizontalAlignment.LEFT);
        this.window.setCloseButtonShown(theme.metricOr(STYLE_CLOSE, 1f) != 0f);
        for (int i = 0; i < ICONS.length; i++) {
            final Paint paint = theme.paintOr(ICONS[i], theme.paintOr(INFO_BAR_ICONS[i], Paint.NONE));
            this.icons[i] = paint.equals(Paint.NONE) ? null : paint;
        }
        this.showIcon();
        this.sizeButtons();
    }

    /**
     * Shows the severity's icon when there is a severity and the theme has an icon for it, and hides it otherwise,
     * so a box without one keeps no space for it.
     */
    private void showIcon() {
        final Paint paint = this.severity == null ? null : this.icons[this.severity.ordinal()];
        this.icon.paint = paint == null ? Paint.NONE : paint;
        this.icon.setVisible(paint != null);
    }

    /**
     * Sizes the message and the buttons for the theme's widths.
     */
    private void sizeButtons() {
        final boolean share = this.buttonWidth <= 0f;
        this.text.setMaxSize(this.messageWidth, Float.POSITIVE_INFINITY);
        this.text.setMinSize(share ? this.messageWidth : 0f, 0f);
        this.row.setAlignment(share ? HorizontalAlignment.CENTER : HorizontalAlignment.RIGHT, VerticalAlignment.TOP);
        final int count = this.buttons.size();
        float width = this.buttonWidth;
        if (share && count > 0) {
            width = (this.messageWidth - this.row.spacing() * (count - 1)) / count;
            if (this.buttonWidthMaximum > 0f) {
                width = Math.min(width, this.buttonWidthMaximum);
            }
        }
        for (final Button button : this.buttons.values()) {
            button.setWidth(Math.max(0f, (float) Math.floor(width)));
        }
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
    public List<Result> results() {
        return this.results;
    }

    @Override
    public Result dismissResult() {
        return this.dismiss;
    }

    @Override
    public MessageBox setButtons(final List<Result> results, final Result dismiss) {
        Objects.requireNonNull(dismiss, "dismiss");
        final List<Result> row = List.copyOf(results);
        if (row.isEmpty()) {
            throw new IllegalArgumentException("A message box needs at least one button");
        }
        if (new HashSet<>(row).size() != row.size()) {
            throw new IllegalArgumentException("Two buttons have the same result id: " + row);
        }
        this.results = row;
        this.dismiss = dismiss;
        if (this.defaultResult != null && !row.contains(this.defaultResult)) {
            this.defaultResult = null;
        }
        this.row.clear();
        this.buttons.clear();
        for (final Result result : row) {
            final Button button = this.ui.button(this.captions.getOrDefault(result, result.caption()))
                    .onEvent(Button.PRESSED, event -> this.answer(result));
            button.setCancel(result.equals(dismiss));
            this.buttons.put(result, button);
            this.row.add(button);
        }
        this.markDefault();
        this.sizeButtons();
        return this;
    }

    @Override
    public Optional<InfoBar.Severity> severity() {
        return Optional.ofNullable(this.severity);
    }

    @Override
    public MessageBox setSeverity(final InfoBar.Severity severity) {
        this.severity = severity;
        this.showIcon();
        return this;
    }

    @Override
    public Optional<Component<?>> content() {
        return Optional.ofNullable(this.content);
    }

    /**
     * Puts the content in the column between the message and the buttons, added to the column's node as content, so
     * its events pass the window and the box although the column is a private part.
     */
    @Override
    public MessageBox setContent(final Component<?> content) {
        if (content == this.content) {
            return this;
        }
        if (content != null) {
            Adoption.require(this.node(), this.nodeOf(content));
        }
        final ContainerNode column = (ContainerNode) this.nodeOf(this.column);
        if (this.content != null) {
            column.remove(this.content);
        }
        this.content = content;
        if (content != null) {
            column.insertContent(1, content);
        }
        return this;
    }

    @Override
    public Result defaultResult() {
        return this.defaultResult != null ? this.defaultResult : this.results.get(0);
    }

    @Override
    public MessageBox setDefaultResult(final Result result) {
        Objects.requireNonNull(result, "result");
        if (!this.results.contains(result)) {
            throw new IllegalArgumentException(this.results + " has no button for " + result);
        }
        this.defaultResult = result;
        this.markDefault();
        return this;
    }

    private void markDefault() {
        final Result chosen = this.defaultResult();
        this.buttons.forEach((result, button) -> button.setDefault(result.equals(chosen)));
    }

    /**
     * Returns the default result's button, which the root focuses when the box opens as a dialog.
     */
    @Override
    public Component<?> initialFocus() {
        return this.buttons.get(this.defaultResult());
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
            this.request(ANSWER, result, answer -> {
                // A handler that rewrote the answer to null leaves the box open, as canceling does
                if (answer != null) {
                    this.finish(answer);
                }
            });
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
        this.request(ANSWER, this.dismiss, result -> {
            if (result != null) {
                this.finish(result);
                this.window.close();
            }
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
        if (!event.isHandled() && event.action() == KeyAction.PRESS && event.key().isEnter()
                && event.modifiers().isEmpty() && this.hostRoot() != null
                && this.hostRoot().pressDialogButton(this.nodeOf(this.window), false)) {
            event.consume();
        }
    }

    /**
     * Answers the dismiss result without a request when the host let go of the box before it answered, as when the
     * overlay showing it was closed from code.
     */
    @Override
    protected void onReleased() {
        this.finish(this.dismiss);
    }

    /**
     * The severity's icon: a square of {@link MessageBox#ICON_SIZE} filled with the theme's paint.
     */
    private static final class SeverityIcon extends BuiltInComponent<SeverityIcon> {
        private Paint paint = Paint.NONE;

        SeverityIcon(final ComponentFactoryImpl ui) {
            super(ui);
        }

        @Override
        protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
            return new Size(ICON_SIZE, ICON_SIZE);
        }

        @Override
        protected void paint(final Painter painter, final Rect bounds) {
            this.paint.paint(painter, bounds);
        }
    }
}
