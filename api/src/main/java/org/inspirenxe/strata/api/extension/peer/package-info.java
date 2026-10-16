/**
 * Plumbing between {@link org.inspirenxe.strata.api.extension.CustomComponent} and Strata's implementation.
 *
 * <p>Each custom component has a peer, created by the {@link org.inspirenxe.strata.api.ComponentFactory} in the
 * component's constructor. The component's {@code final} methods delegate to the peer, and the peer reaches the
 * component's protected hooks through a private {@link org.inspirenxe.strata.api.extension.peer.ComponentHooks}.</p>
 *
 * <p>Only Strata's implementation implements these interfaces, and only the extension base classes call them.
 * Developers never need this package.</p>
 */
package org.inspirenxe.strata.api.extension.peer;
