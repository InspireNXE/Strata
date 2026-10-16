package org.inspirenxe.strata.spi;

/**
 * What an engine does for itself, so a host can fill in the rest.
 *
 * @param sendsKeyRepeat true when the adapter delivers {@link org.inspirenxe.strata.api.input.KeyAction#REPEAT}
 *                       for held keys itself. When false, the host makes repeats for navigation and delete keys
 *                       (after 400 ms, then every 33 ms, timed by the platform's clock while {@code render} is
 *                       called). An adapter whose engine reports repeats as more presses must tell them apart and
 *                       send {@code REPEAT}; then this is true.
 */
public record Capabilities(boolean sendsKeyRepeat) {
}
