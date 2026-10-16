/**
 * Strata's implementation of the API and SPI. Engine adapters depend on it; developers compile against {@code api}
 * only and never see these classes.
 *
 * <p>{@link org.inspirenxe.strata.core.StrataCore} is the only entry point adapters need. Everything else is public
 * so the packages can reach each other and so tests can inspect state. No class here is a stable API: names and
 * signatures change without notice.</p>
 *
 * <p>Core is single-threaded. Every call into a host, and through it into the tree, must come from the thread the
 * engine runs its UI on.</p>
 */
package org.inspirenxe.strata.core;
