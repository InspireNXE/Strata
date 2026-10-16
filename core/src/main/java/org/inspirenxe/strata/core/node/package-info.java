/**
 * The node tree behind the public components.
 *
 * <p>Every component a developer holds is a {@link org.inspirenxe.strata.api.extension.CustomComponent} whose
 * {@code final} methods forward to a {@link org.inspirenxe.strata.core.node.Node} (or a
 * {@link org.inspirenxe.strata.core.node.ContainerNode} for containers). The host's
 * {@link org.inspirenxe.strata.core.node.ComponentFactoryImpl} makes those nodes and maps components back to them,
 * rejecting components that have no node or that belong to another host.
 * {@link org.inspirenxe.strata.core.node.Root} owns a shown tree: its layout, painting, and the pointer, keyboard
 * and focus state.</p>
 */
package org.inspirenxe.strata.core.node;
