/**
 * Extending Strata: new components, new layouts, and changes to existing components.
 *
 * <p>Pick the lightest way that does the job:</p>
 * <ul>
 *     <li>Change a built-in through its events: cancel or rewrite its requests, react to its reports, draw over
 *     it with {@code Component.PAINTED} or change its size with {@code Component.MEASURED}. See
 *     {@link org.inspirenxe.strata.api.event}.</li>
 *     <li>Compose: put built-in components inside a container and expose the group through your own
 *     interface.</li>
 *     <li>Write a new component: extend {@link org.inspirenxe.strata.api.extension.CustomComponent}, or
 *     {@link org.inspirenxe.strata.api.extension.CustomContainer} for a new layout. Your class can implement your own
 *     interface, so other code sees it as a type of its own, and declare and fire events of its own with
 *     {@link org.inspirenxe.strata.api.extension.CustomComponent}'s {@code request} and {@code report}.</li>
 * </ul>
 * <p>Do not implement the component interfaces ({@code Component}, {@code Button} and so on) directly. Such a class
 * has no Strata implementation behind it, and containers reject it.</p>
 *
 * <p>The {@code peer} subpackage is plumbing between the base classes and Strata's implementation. Developers do
 * not use it.</p>
 */
package org.inspirenxe.strata.api.extension;
