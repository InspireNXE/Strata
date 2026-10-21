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
import org.inspirenxe.strata.api.component.HasCursor;
import org.inspirenxe.strata.api.component.HasTooltip;
import org.inspirenxe.strata.api.control.attribute.text.HasText;
import org.inspirenxe.strata.api.control.attribute.text.Titled;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.control.ValueEvent;
import org.inspirenxe.strata.api.event.control.ValueRequestEvent;
import org.inspirenxe.strata.api.text.Text;

import java.util.List;

/**
 * A {@link Window} with a message and a row of buttons, for confirmations and notices. Show it with
 * {@link org.inspirenxe.strata.api.overlay.Overlays#dialog}.
 *
 * <p>The message wraps at {@link #MESSAGE_WIDTH} units. The buttons sit at the bottom right, in the order of the
 * {@link Buttons} set, the first one focused when the dialog opens. A button press, the close button or Escape
 * answers once: the box reports a {@link Result} through {@link #ANSWERED} and closes the overlay showing it. The
 * close button and Escape answer {@link Buttons#dismissResult()}.</p>
 *
 * <p>The box is a composite: the events of its window and buttons reach handlers on those parts and on the box,
 * and nothing outside it. To hear the user's answer, handle {@link #ANSWER} and {@link #ANSWERED}.</p>
 *
 * <p>It uses the theme keys of {@link Window}, {@link Label} and {@link Button}.</p>
 *
 * <pre>{@code
 * factory.overlays().dialog(factory.messageBox("Delete", "Delete this exchange?", MessageBox.Buttons.YES_NO)
 *         .onEvent(MessageBox.ANSWERED, event -> {
 *             if (event.value() == MessageBox.Result.YES) {
 *                 delete(exchange);
 *             }
 *         }));
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#messageBox}. Do not implement this
 * interface.</p>
 */
public interface MessageBox extends Component<MessageBox>, HasCursor<MessageBox>, HasTooltip<MessageBox>,
        Titled<MessageBox>, HasText<MessageBox> {
    /**
     * The width the message wraps at, in UI units.
     */
    float MESSAGE_WIDTH = 280f;
    /**
     * Style key for the space between the window's padding and the message and buttons (insets, optional).
     * Defaults to 4 on every side.
     */
    String STYLE_PADDING = "messagebox.padding";

    /**
     * Requested when the user answers: one of the buttons pressed with a click, Enter or Space, or the window's
     * close button or Escape, which answer {@link Buttons#dismissResult()}. An Escape press counts when focus is
     * inside the box, or when nothing has focus and the box is the content of the top dialog. The value is the
     * answer. Unless a handler cancels the request, the box reports {@link #ANSWERED} and closes the overlay showing
     * it; cancel it to keep the box open. A handler may rewrite the answer to any result, even one the box has no
     * button for, and the box reports it as it is. Once the box has answered, it fires no more requests. Fires only
     * for the user's actions.
     */
    EventType<ValueRequestEvent<MessageBox, Result>> ANSWER = EventType.valueRequest("message_box.answer");
    /**
     * Reported once, when the box answers, before the overlay showing it closes. The value is the answer, and
     * {@link ValueEvent#previous()} is the same answer. Closing {@link #window()} from code answers
     * {@link Buttons#dismissResult()} and reports it too, without an {@link #ANSWER} request.
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
     * Returns which buttons the box has.
     *
     * @return the button set; {@link Buttons#OK} for a box made without one
     */
    Buttons buttons();

    /**
     * Replaces the buttons. Captions set with {@link #setButtonText} are kept for the results that still have a
     * button, and closing the box answers the new set's {@link Buttons#dismissResult()}.
     *
     * @param buttons which buttons the box has
     * @return this box
     */
    MessageBox setButtons(Buttons buttons);

    /**
     * Replaces a button's caption, such as to translate it. The default captions are {@code OK}, {@code Cancel},
     * {@code Yes}, {@code No} and {@code Close}.
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
     * What a message box reports.
     */
    enum Result {
        /** The OK button. */
        OK,
        /** The Cancel button. */
        CANCEL,
        /** The Yes button. */
        YES,
        /** The No button. */
        NO,
        /** The Close button. */
        CLOSE
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
