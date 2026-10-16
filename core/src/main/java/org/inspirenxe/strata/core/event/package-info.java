/**
 * The event classes core creates, and their delivery.
 *
 * <p>{@link org.inspirenxe.strata.core.event.EventRouter} delivers each event to its target and, for bubbling
 * types, up through the ancestors. Each component keeps its handlers in an
 * {@link org.inspirenxe.strata.core.event.EventDispatcher}, made on its first subscription.</p>
 */
package org.inspirenxe.strata.core.event;
