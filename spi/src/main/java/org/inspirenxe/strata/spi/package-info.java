/**
 * The service provider interface: what an engine adapter implements and drives to run Strata on an engine.
 *
 * <h2>Writing an adapter</h2>
 * <ol>
 *     <li>Implement {@link org.inspirenxe.strata.spi.Platform}: text measurement, a clock, the clipboard, text input
 *     signalling and {@link org.inspirenxe.strata.spi.Capabilities}.</li>
 *     <li>Implement {@link org.inspirenxe.strata.spi.Canvas} on the engine's 2D drawing.</li>
 *     <li>Create a host with {@code StrataCore.createScreenHost} (one screen) or {@code StrataCore.createScreenStack}
 *     (a stack of screens), from the {@code core} module.</li>
 *     <li>Each frame, call {@code render} with your canvas. Forward window size changes to {@code resize}, and input
 *     to the {@link org.inspirenxe.strata.spi.InputSink} methods, converted to UI units and Strata's input types.</li>
 * </ol>
 *
 * <h2>Rules for every adapter</h2>
 * <ul>
 *     <li>Call hosts on one thread, the engine's UI or render thread. Strata does no locking.</li>
 *     <li>Coordinates are UI units with the origin at the top left of the host's area. Choose the unit (screen
 *     pixels, scaled pixels) and convert both drawing and input the same way.</li>
 *     <li>Never let engine types into Strata calls: map keys to {@link org.inspirenxe.strata.api.input.Key}, buttons
 *     to {@link org.inspirenxe.strata.api.input.MouseButton}, and so on.</li>
 *     <li>Check your adapter with the contract tests in Strata's SPI test fixtures
 *     ({@code testFixtures(project(':spi'))}): extend {@code CanvasContract}, {@code TextMetricsContract} and
 *     {@code PlatformContract} in your adapter's tests.</li>
 * </ul>
 */
package org.inspirenxe.strata.spi;
