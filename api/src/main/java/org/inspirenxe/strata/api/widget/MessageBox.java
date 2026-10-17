package org.inspirenxe.strata.api.widget;

import org.inspirenxe.strata.api.component.Component;

import java.util.List;
import java.util.function.Consumer;

/**
 * A {@link Window} with a message and a row of buttons, for confirmations and notices. Show it with
 * {@link org.inspirenxe.strata.api.overlay.Overlays#dialog}.
 *
 * <p>The message wraps at {@link #MESSAGE_WIDTH} units. The buttons sit at the bottom right, in the order of the
 * {@link Buttons} set, the first one focused when the dialog opens. A button press, the close button or Escape
 * reports a {@link Result} to the listeners once and closes the overlay showing the box. The close button and Escape
 * report {@link Buttons#dismissResult()}.</p>
 *
 * <p>It uses the theme keys of {@link Window}, {@link Label} and {@link Button}.</p>
 *
 * <pre>{@code
 * factory.overlays().dialog(factory.messageBox("Delete", "Delete this exchange?", MessageBox.Buttons.YES_NO)
 *         .onResult(result -> {
 *             if (result == MessageBox.Result.YES) {
 *                 delete(exchange);
 *             }
 *         }));
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#messageBox}. Do not implement this
 * interface.</p>
 */
public interface MessageBox extends Component<MessageBox> {
    /**
     * The width the message wraps at, in UI units.
     */
    float MESSAGE_WIDTH = 280f;

    /**
     * Returns the window the box is built on, to change its title or size.
     *
     * @return the window
     */
    Window window();

    /**
     * Replaces a button's caption, such as to translate it. The default captions are {@code OK}, {@code Cancel},
     * {@code Yes}, {@code No} and {@code Close}.
     *
     * @param result the result of the button
     * @param caption the caption
     * @return this box
     */
    MessageBox setButtonText(Result result, String caption);

    /**
     * Adds a listener told which button closed the box.
     *
     * @param listener receives the result
     * @return this box
     */
    MessageBox onResult(Consumer<Result> listener);

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
