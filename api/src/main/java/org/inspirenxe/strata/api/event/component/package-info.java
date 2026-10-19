/**
 * The events every component fires about drawing and sizing itself: {@code Component.PAINT},
 * {@code Component.PAINTED} and {@code Component.MEASURED}.
 *
 * <p>A component fires them only when it has a handler for them, and reuses one event object for each, so they cost
 * nothing otherwise and allocate nothing per frame. They do not bubble.</p>
 *
 * <p>Only Strata creates events. Do not implement these interfaces.</p>
 */
package org.inspirenxe.strata.api.event.component;
