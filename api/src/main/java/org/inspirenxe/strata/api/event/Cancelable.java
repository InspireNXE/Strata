package org.inspirenxe.strata.api.event;

/**
 * A Pre event: one that asks to do something, so a handler may refuse. Input events and a control's request events
 * are cancelable; Post events, which report something that already happened, are not.
 *
 * <p>Only Strata creates events. Do not implement this interface.</p>
 */
public interface Cancelable extends Event {

    /**
     * Refuses what the event asks for. For an input event, the default behavior of the current component and of
     * every ancestor the event still reaches is skipped. For a request event, the control does not make the change.
     * Handlers keep running, and the event keeps bubbling unless stopped too.
     */
    void cancel();

    /**
     * Returns whether the event was canceled.
     *
     * @return true after {@link #cancel()} or {@link #consume()}
     */
    boolean isCancelled();

    /**
     * Cancels the event and stops it: "handled here". Built-in controls consume the input they act on.
     */
    default void consume() {
        this.cancel();
        this.stopPropagation();
    }

    /**
     * Returns whether something canceled or stopped the event. The host reports such input to the engine as
     * consumed.
     *
     * @return true if canceled or stopped
     */
    default boolean isHandled() {
        return this.isCancelled() || this.isPropagationStopped();
    }
}
