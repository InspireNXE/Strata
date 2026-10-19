/**
 * The events controls fire about their own changes: requests that ask before the user changes something, and
 * reports that tell of every change.
 *
 * <p>{@link org.inspirenxe.strata.api.event.control.RequestEvent} and
 * {@link org.inspirenxe.strata.api.event.control.ValueRequestEvent} are the Pre events; a handler may cancel them,
 * and rewrite a request with a value. {@link org.inspirenxe.strata.api.event.control.ControlEvent} and
 * {@link org.inspirenxe.strata.api.event.control.ValueEvent} are the Post events; a value report also carries the
 * value from before. The types are constants on the attributes that own them, such as {@code Toggle.TOGGLED}, or on
 * the control itself, such as {@code Button.PRESSED}.</p>
 *
 * <p>Only Strata creates events. Do not implement these interfaces.</p>
 */
package org.inspirenxe.strata.api.event.control;
