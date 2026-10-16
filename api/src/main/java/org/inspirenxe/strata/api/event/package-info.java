/**
 * Events: how input reaches components, and how code reacts to it.
 *
 * <p>The host turns engine input into events and fires each at one target component. The event types are listed
 * in {@link org.inspirenxe.strata.api.event.EventTypes}. For every component an event reaches, three steps run in
 * order:</p>
 * <ol>
 *     <li>the component's {@link org.inspirenxe.strata.api.event.Phase#BEFORE} handlers;</li>
 *     <li>the component's default behavior, skipped if the event was
 *     {@linkplain org.inspirenxe.strata.api.event.Event#cancel() canceled};</li>
 *     <li>the component's {@link org.inspirenxe.strata.api.event.Phase#AFTER} handlers.</li>
 * </ol>
 * <p>A <em>bubbling</em> event then moves to the parent and repeats, up to the screen's root, unless something
 * {@linkplain org.inspirenxe.strata.api.event.Event#stopPropagation() stopped} it. A <em>direct</em> event stays at
 * its target. When the event finishes, the host reports it as consumed if it was canceled or stopped; an adapter
 * then keeps the input from reaching the engine's own UI.</p>
 *
 * <pre>{@code
 * button.on(EventTypes.CLICK, event -> {
 *     if (!confirmed) {
 *         event.cancel();            // the button does not press
 *     }
 * });
 * panel.on(EventTypes.SCROLL, Phase.AFTER, event -> scrollBy(event.scrollY()));
 * }</pre>
 *
 * <p>Only Strata creates events. Do not implement the event interfaces.</p>
 */
package org.inspirenxe.strata.api.event;
