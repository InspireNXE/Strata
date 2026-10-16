package org.inspirenxe.strata.spi;

/**
 * @param sendsKeyRepeat true when the engine delivers key repeats itself; otherwise core synthesizes them
 */
public record Capabilities(boolean sendsKeyRepeat) {
}
