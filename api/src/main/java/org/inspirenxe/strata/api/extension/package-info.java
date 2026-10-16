/**
 * Extending Strata: new components, new layouts, and changes to existing components.
 *
 * <p>Pick the lightest way that does the job:</p>
 * <ul>
 *     <li>Decorate a built-in: pass a {@link org.inspirenxe.strata.api.extension.ComponentBehavior} (such as a
 *     {@code ButtonBehavior}) to a factory method. Its hooks wrap the component's own, with a {@code next} handle
 *     that runs the original behavior. Use this to add drawing, filter input, or change a size.</li>
 *     <li>Compose: put built-in components inside a container and expose the group through your own
 *     interface.</li>
 *     <li>Write a new component: extend {@link org.inspirenxe.strata.api.extension.CustomComponent}, or
 *     {@link org.inspirenxe.strata.api.extension.CustomContainer} for a new layout. Your class can implement your own
 *     interface, so other code sees it as a type of its own.</li>
 * </ul>
 * <p>Do not implement the component interfaces ({@code Component}, {@code Button} and so on) directly. Such a class
 * has no Strata implementation behind it, and containers reject it.</p>
 *
 * <p>The {@code peer} subpackage is plumbing between the base classes and Strata's implementation. Developers do
 * not use it.</p>
 */
package org.inspirenxe.strata.api.extension;
