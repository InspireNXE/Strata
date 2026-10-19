/**
 * Input events: what the user does with the pointer and the keyboard, and focus moving.
 *
 * <p>The types are the constants in {@link org.inspirenxe.strata.api.event.input.InputEvents}, such as
 * {@code InputEvents.CLICK} and {@code InputEvents.KEY}. Input events are Pre events: canceling one skips the default
 * behavior of the components it has not reached yet, and the host reports it as consumed.</p>
 *
 * <p>Only Strata creates events. Do not implement these interfaces.</p>
 */
package org.inspirenxe.strata.api.event.input;
