/**
 * Events: the one way code hears about anything a component does.
 *
 * <p>Register handlers with {@link org.inspirenxe.strata.api.event.EventTarget#onEvent}, optionally with a
 * priority; on one component, handlers run in ascending priority and the highest runs last. Events come in two
 * kinds:</p>
 * <ul>
 *     <li>Pre events ask to do something and are {@link org.inspirenxe.strata.api.event.Cancelable}: input (the
 *     types in {@link org.inspirenxe.strata.api.event.input.InputEvents}) and a control's requests, such as
 *     {@code TextInput.EDIT}. Requests fire only for what the user does, and a request with a value can be rewritten
 *     ({@link org.inspirenxe.strata.api.event.control.ValueRequestEvent#setValue}).</li>
 *     <li>Post events report that something happened, such as {@code TextInput.EDITED}, whether the user or code
 *     caused it. They cannot be canceled.</li>
 * </ul>
 * <p>For every component an input event reaches, its handlers run, then its default behavior unless the event was
 * canceled. A <em>bubbling</em> event then moves to the parent and repeats, up to the screen's root, unless
 * something {@linkplain org.inspirenxe.strata.api.event.Event#stopPropagation() stopped} it. A <em>direct</em> event
 * stays at its target. A control fires its request through its whole path first and acts afterwards, unless a
 * handler canceled it; a control's events stop at the first composite above it, unless they came from content the
 * composite holds for its user. When an input event finishes, the host reports it as consumed if it was canceled or
 * stopped; an adapter then keeps the input from reaching the engine's own UI.</p>
 *
 * <pre>{@code
 * button.onEvent(Button.PRESS, event -> {
 *     if (!confirmed) {
 *         event.cancel();            // the button does not press
 *     }
 * }).onEvent(Button.PRESSED, event -> save());
 * }</pre>
 *
 * <p>This package holds what every event shares: {@link org.inspirenxe.strata.api.event.Event},
 * {@link org.inspirenxe.strata.api.event.EventType}, the handlers and their registration. The events themselves are
 * grouped below it: {@code input} for what the user does, {@code control} for the requests and reports of controls,
 * and {@code component} for drawing and sizing.</p>
 *
 * <p>Only Strata creates events. Do not implement the event interfaces.</p>
 */
package org.inspirenxe.strata.api.event;
