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
package org.inspirenxe.strata.api.control.window;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.HasContextMenu;
import org.inspirenxe.strata.api.component.HasCursor;
import org.inspirenxe.strata.api.component.HasTooltip;
import org.inspirenxe.strata.api.control.attribute.text.HasText;
import org.inspirenxe.strata.api.control.attribute.text.Titled;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.display.InfoBar;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.control.ValueEvent;
import org.inspirenxe.strata.api.event.control.ValueRequestEvent;
import org.inspirenxe.strata.api.text.Text;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A {@link Window} with a message and a row of buttons, for confirmations and notices. Show it with
 * {@link org.inspirenxe.strata.api.overlay.Overlays#dialog}.
 *
 * <p>The message wraps at {@link #MESSAGE_WIDTH} units unless the theme says otherwise. The buttons sit at the
 * bottom right, or share the row when the theme says so, in the order of the {@linkplain #results() results}: one of
 * the common {@link Buttons} sets, or any row of {@link Result}s from {@link #setButtons(List, Result)}, such as Save,
 * Don't save and Cancel. The {@linkplain #defaultResult() default result}'s button, the first one unless set
 * otherwise, is the default button and is focused when the dialog opens, and the dismiss result's button is the
 * cancel button. A box made without a set has {@link Buttons#OK}. A button press, the close button or Escape answers
 * once: the box reports a {@link Result} through {@link #ANSWERED} and closes the overlay showing it. The close
 * button and Escape answer the {@linkplain #dismissResult() dismiss result}.</p>
 *
 * <p>A box with a {@linkplain #setSeverity severity} draws the severity's icon before the message, a square
 * {@value #ICON_SIZE} units on a side, 8 units before it, when the theme has an icon for it. A
 * component set with {@link #setContent}, such as a "Don't ask again" check box, sits between the message and the
 * buttons, and its events pass the box on to its ancestors.</p>
 *
 * <p>A box that closes any other way before it answered, such as by its overlay's {@code close()}, reports the
 * dismiss result through {@link #ANSWERED} without asking {@link #ANSWER} first.</p>
 *
 * <p>The box is a composite: the events of its window and buttons reach handlers on those parts and on the box,
 * and nothing outside it, while those of its {@linkplain #setContent content} go on. To hear the user's answer,
 * handle {@link #ANSWER} and {@link #ANSWERED}.</p>
 *
 * <p>It uses the theme keys of {@link Window}, {@link Label} and {@link Button}, and its own:
 * {@value #STYLE_PADDING} (insets, optional), the space between the window's padding and the message and buttons,
 * 4 on every side by default, and the metrics {@value #STYLE_SPACING}, {@value #STYLE_MESSAGE_WIDTH},
 * {@value #STYLE_MESSAGE_CENTER},
 * {@value #STYLE_BUTTON_WIDTH}, {@value #STYLE_BUTTON_WIDTH_MAXIMUM}, {@value #STYLE_BUTTON_GAP} and
 * {@value #STYLE_CLOSE}. The icons of the severities are the paints {@value #STYLE_ICON_INFO},
 * {@value #STYLE_ICON_SUCCESS}, {@value #STYLE_ICON_WARNING} and {@value #STYLE_ICON_ERROR} (optional), which fall
 * back to the info bar's {@value InfoBar#STYLE_ICON_INFO} and the others; a box whose theme has neither shows no
 * icon and leaves no space for it.</p>
 *
 * <pre>{@code
 * factory.overlays().dialog(factory.messageBox("Delete", "Delete this exchange?", MessageBox.Buttons.YES_NO)
 *         .onEvent(MessageBox.ANSWERED, event -> {
 *             if (event.value().equals(MessageBox.Result.YES)) {
 *                 delete(exchange);
 *             }
 *         }));
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#messageBox}. Do not implement this
 * interface.</p>
 */
public interface MessageBox extends Component<MessageBox>, HasCursor<MessageBox>, HasTooltip<MessageBox>,
        HasContextMenu<MessageBox>, Titled<MessageBox>, HasText<MessageBox> {
    /**
     * The width the message wraps at, in UI units.
     */
    float MESSAGE_WIDTH = 280f;
    /**
     * The size of the severity's icon, a square, in UI units.
     */
    float ICON_SIZE = 24f;
    /**
     * Style key for the icon of {@link InfoBar.Severity#INFO} (paint, optional). Falls back to
     * {@value InfoBar#STYLE_ICON_INFO}.
     */
    String STYLE_ICON_INFO = "messagebox.icon.info";
    /**
     * Style key for the icon of {@link InfoBar.Severity#SUCCESS} (paint, optional). Falls back to
     * {@value InfoBar#STYLE_ICON_SUCCESS}.
     */
    String STYLE_ICON_SUCCESS = "messagebox.icon.success";
    /**
     * Style key for the icon of {@link InfoBar.Severity#WARNING} (paint, optional). Falls back to
     * {@value InfoBar#STYLE_ICON_WARNING}.
     */
    String STYLE_ICON_WARNING = "messagebox.icon.warning";
    /**
     * Style key for the icon of {@link InfoBar.Severity#ERROR} (paint, optional). Falls back to
     * {@value InfoBar#STYLE_ICON_ERROR}.
     */
    String STYLE_ICON_ERROR = "messagebox.icon.error";
    /**
     * Style key for the space between the window's padding and the message and buttons (insets, optional).
     * Defaults to 4 on every side.
     */
    String STYLE_PADDING = "messagebox.padding";
    /**
     * Style key for the space between the message and the buttons (metric, optional), in UI units. Defaults to 8.
     */
    String STYLE_SPACING = "messagebox.spacing";
    /**
     * Style key for the width the message wraps at (metric, optional), in UI units. Defaults to
     * {@value #MESSAGE_WIDTH}.
     */
    String STYLE_MESSAGE_WIDTH = "messagebox.message.width";
    /**
     * Style key for centering the message's lines (metric, optional): any value but 0 centers them. Defaults to 0,
     * which starts them at the left.
     */
    String STYLE_MESSAGE_CENTER = "messagebox.message.center";
    /**
     * Style key for each button's width (metric, optional), in UI units. Defaults to 64, with the buttons at the
     * right. A value of 0 makes the box as wide as {@value #STYLE_MESSAGE_WIDTH} and has the buttons share that
     * width evenly, each at most {@value #STYLE_BUTTON_WIDTH_MAXIMUM} wide, centered.
     */
    String STYLE_BUTTON_WIDTH = "messagebox.button.width";
    /**
     * Style key for the widest a button gets when the buttons share the row (metric, optional), in UI units.
     * Defaults to 0, which sets no limit.
     */
    String STYLE_BUTTON_WIDTH_MAXIMUM = "messagebox.button.width.maximum";
    /**
     * Style key for the space between buttons (metric, optional), in UI units. Defaults to 4.
     */
    String STYLE_BUTTON_GAP = "messagebox.button.gap";
    /**
     * Style key for whether the window shows its close button (metric, optional): 0 hides it, and Escape still
     * answers. Defaults to 1, which shows it.
     */
    String STYLE_CLOSE = "messagebox.close";

    /**
     * Requested when the user answers: one of the buttons pressed with a click, Enter or Space, or the window's
     * close button or Escape, which answer the {@linkplain #dismissResult() dismiss result}. An Escape press counts when focus is
     * inside the box, or when nothing has focus and the box is the content of the top dialog. The value is the
     * answer. Unless a handler cancels the request, the box reports {@link #ANSWERED} and closes the overlay showing
     * it; cancel it to keep the box open. A handler may rewrite the answer to any result, even one the box has no
     * button for, and the box reports it as it is; an answer rewritten to null keeps the box open, as canceling
     * does. Once the box has answered, it fires no more requests. Fires only for the user's actions.
     */
    EventType<ValueRequestEvent<MessageBox, Result>> ANSWER = EventType.valueRequest("message_box.answer");
    /**
     * Reported once, when the box answers, before the overlay showing it closes. The value is the answer, and
     * {@link ValueEvent#previous()} is the same answer. Closing {@link #window()} from code answers the
     * {@linkplain #dismissResult() dismiss result} and reports it too, without an {@link #ANSWER} request. So does a box that
     * closes any other way before it answered, after the overlay closed.
     */
    EventType<ValueEvent<MessageBox, Result>> ANSWERED = EventType.valueReport("message_box.answered");

    /**
     * Returns the window the box is built on, to change its size.
     *
     * @return the window
     */
    Window window();

    /**
     * Returns the window's title.
     *
     * @return the title, never null
     */
    @Override
    Text title();

    /**
     * Replaces the window's title.
     *
     * @param title the title, or empty text for none
     * @return this box
     */
    @Override
    MessageBox setTitle(Text title);

    /**
     * Returns the message.
     *
     * @return the message, never null
     */
    @Override
    Text text();

    /**
     * Replaces the message. It wraps at {@value #MESSAGE_WIDTH} units, and {@code \n} starts a new line.
     *
     * @param message the message
     * @return this box
     */
    @Override
    MessageBox setText(Text message);

    /**
     * Returns the results of the box's buttons, left to right.
     *
     * @return an unmodifiable list of the results; {@link Buttons#OK}'s for a box made without a set
     */
    List<Result> results();

    /**
     * Returns what closing the box without a button answers: the close button, Escape, or the overlay closing from
     * code.
     *
     * @return the dismiss result
     */
    Result dismissResult();

    /**
     * Replaces the buttons with one of the common sets, as {@code setButtons(buttons.results(),
     * buttons.dismissResult())} does.
     *
     * @param buttons which buttons the box has
     * @return this box
     */
    default MessageBox setButtons(final Buttons buttons) {
        return this.setButtons(buttons.results(), buttons.dismissResult());
    }

    /**
     * Replaces the buttons with a row of any results, left to right, each captioned with its
     * {@linkplain Result#caption() caption} unless {@link #setButtonText} set another. Captions set with
     * {@code setButtonText} are kept for the results that still have a button. The dismiss result's button, if it has
     * one, is the cancel button; a dismiss result with no button is still what the close button and Escape answer.
     *
     * @param results the results of the buttons, left to right
     * @param dismiss what closing the box without a button answers
     * @return this box
     * @throws IllegalArgumentException if {@code results} is empty or holds two results with the same id
     */
    MessageBox setButtons(List<Result> results, Result dismiss);

    /**
     * Returns the severity whose icon the box draws.
     *
     * @return the severity, or empty for none, as for a new box
     */
    Optional<InfoBar.Severity> severity();

    /**
     * Sets the severity whose icon the box draws before the message, or none. A new layout runs.
     *
     * @param severity the severity, or null for no icon
     * @return this box
     */
    MessageBox setSeverity(InfoBar.Severity severity);

    /**
     * Returns the component between the message and the buttons.
     *
     * @return the component, or empty for none
     */
    Optional<Component<?>> content();

    /**
     * Puts a component between the message and the buttons, such as a check box to stop asking, in place of the one
     * there before, which is removed from the tree. It is added as content, so its events pass the box on to its
     * ancestors. It is laid out as wide as the message's column and as tall as it wants.
     *
     * @param content the component, or null for none; it must not have a parent
     * @return this box
     * @throws IllegalArgumentException if {@code content} already has a parent, is a host's content, or is this box
     *     or one of its ancestors
     */
    MessageBox setContent(Component<?> content);

    /**
     * Returns the result whose button is the box's {@linkplain Button#setDefault default button}, which takes focus
     * when the box opens as a dialog and is pressed by an Enter that nothing else consumed.
     *
     * @return the result set with {@link #setDefaultResult}, or the first result of the button set when none is set
     *     or the set has no button for it
     */
    Result defaultResult();

    /**
     * Makes a result's button the default button, in place of the first one. A question whose yes answer destroys
     * something calls {@code setDefaultResult(Result.NO)}, so Enter keeps what is there. The button of the
     * {@linkplain #dismissResult() dismiss result} is always the {@linkplain Button#setCancel cancel button}.
     * Replacing the buttons with a row that has no button for the result makes the first result the default
     * again.
     *
     * @param result the result
     * @return this box
     * @throws IllegalArgumentException if the button set has no button for the result
     */
    MessageBox setDefaultResult(Result result);

    /**
     * Replaces a button's caption, in place of its result's {@linkplain Result#caption() caption}. The common
     * results' captions are the translated {@code strata.button.ok}, {@code strata.button.cancel},
     * {@code strata.button.yes}, {@code strata.button.no} and {@code strata.button.close}, which read OK, Cancel, Yes,
     * No and Close in English. A caption set for a result the box has no button for is kept, and used if
     * {@link #setButtons} adds that button. Results are matched by id.
     *
     * @param result the result of the button
     * @param caption the caption
     * @return this box
     */
    MessageBox setButtonText(Result result, Text caption);

    /**
     * Does the same as {@link #setButtonText(Result, Text)} with literal text.
     *
     * @param result the result of the button
     * @param caption the caption
     * @return this box
     */
    default MessageBox setButtonText(final Result result, final String caption) {
        return this.setButtonText(result, Text.of(caption));
    }

    /**
     * What a message box reports: an answer, named by an id, with the caption its button shows. The common answers
     * are constants, and {@link #of(String, Text)} makes any other, such as {@code Result.of("save", "Save")}.
     *
     * <p>Two results are equal when their ids are, whatever their captions, so a result made again with the same id
     * matches the one the box reports. Compare results with {@link #equals}, or switch on {@link #id()}.</p>
     *
     * @param id what the answer is, such as {@code "ok"}; not empty
     * @param caption the caption of the answer's button
     */
    record Result(String id, Text caption) {
        /** The OK button, with the id {@code "ok"}. */
        public static final Result OK = new Result("ok", Text.translatable("strata.button.ok"));
        /** The Cancel button, with the id {@code "cancel"}. */
        public static final Result CANCEL = new Result("cancel", Text.translatable("strata.button.cancel"));
        /** The Yes button, with the id {@code "yes"}. */
        public static final Result YES = new Result("yes", Text.translatable("strata.button.yes"));
        /** The No button, with the id {@code "no"}. */
        public static final Result NO = new Result("no", Text.translatable("strata.button.no"));
        /** The Close button, with the id {@code "close"}. */
        public static final Result CLOSE = new Result("close", Text.translatable("strata.button.close"));

        /**
         * Checks the id and the caption.
         *
         * @throws IllegalArgumentException if the id is empty
         */
        public Result {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(caption, "caption");
            if (id.isEmpty()) {
                throw new IllegalArgumentException("A result's id must not be empty");
            }
        }

        /**
         * Makes a result.
         *
         * @param id what the answer is; not empty
         * @param caption the caption of its button
         * @return the result
         * @throws IllegalArgumentException if the id is empty
         */
        public static Result of(final String id, final Text caption) {
            return new Result(id, caption);
        }

        /**
         * Does the same as {@link #of(String, Text)} with a literal caption.
         *
         * @param id what the answer is; not empty
         * @param caption the caption of its button
         * @return the result
         * @throws IllegalArgumentException if the id is empty
         */
        public static Result of(final String id, final String caption) {
            return new Result(id, Text.of(caption));
        }

        /**
         * Returns whether another object is a result with the same id.
         *
         * @param other the other object; may be null
         * @return true for a result with the same id
         */
        @Override
        public boolean equals(final Object other) {
            return other instanceof final Result result && result.id.equals(this.id);
        }

        /**
         * Returns the id's hash code, so equal results hash alike.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return this.id.hashCode();
        }
    }

    /**
     * Which buttons a message box has.
     */
    enum Buttons {
        /** OK alone. */
        OK(List.of(Result.OK), Result.OK),
        /** OK and Cancel. */
        OK_CANCEL(List.of(Result.OK, Result.CANCEL), Result.CANCEL),
        /** Yes and No. */
        YES_NO(List.of(Result.YES, Result.NO), Result.NO),
        /** Yes, No and Cancel. */
        YES_NO_CANCEL(List.of(Result.YES, Result.NO, Result.CANCEL), Result.CANCEL),
        /** Close alone. */
        CLOSE(List.of(Result.CLOSE), Result.CLOSE);

        private final List<Result> results;
        private final Result dismissResult;

        Buttons(final List<Result> results, final Result dismissResult) {
            this.results = results;
            this.dismissResult = dismissResult;
        }

        /**
         * Returns the buttons, left to right.
         *
         * @return the result of each button
         */
        public List<Result> results() {
            return this.results;
        }

        /**
         * Returns what closing the box without a button reports.
         *
         * @return the result for the close button and Escape
         */
        public Result dismissResult() {
            return this.dismissResult;
        }
    }
}
