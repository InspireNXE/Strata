/**
 * Engine-independent input values: keys, mouse buttons, modifiers and how keyboard shortcuts match them.
 *
 * <p>Components never see engine key codes. An adapter maps its engine's input to these types before handing it
 * to Strata, and components receive it through the events in {@link org.inspirenxe.strata.api.event}.</p>
 */
package org.inspirenxe.strata.api.input;
