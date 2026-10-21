/*
 * Copyright (c) InspireNXE <https://github.com/InspireNXE>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * The service provider interface: what an engine adapter implements and drives to run Strata on an engine.
 *
 * <h2>Writing an adapter</h2>
 * <ol>
 *     <li>Implement {@link org.inspirenxe.strata.spi.Platform}: text measurement, a
 *     {@link org.inspirenxe.strata.spi.Clock}, the {@link org.inspirenxe.strata.spi.Clipboard}, a
 *     {@link org.inspirenxe.strata.spi.TextInputSink}, {@link org.inspirenxe.strata.spi.Capabilities} and a
 *     {@link org.inspirenxe.strata.spi.ThemeSource} for themes named by id.</li>
 *     <li>Implement {@link org.inspirenxe.strata.spi.Canvas} on the engine's 2D drawing.</li>
 *     <li>When the adapter starts, before any project code runs, hand core what is shared by the whole process, from
 *     the {@code core} module's {@code StrataCore}: a {@link org.inspirenxe.strata.spi.Translator} with
 *     {@code setTranslator}, the screen controller {@code Strata.screens()} returns with {@code setScreenController},
 *     and appearances for the engine's own types with {@code registerDefaultAppearance}. Wrap engine text with
 *     {@code StrataCore.text} and a {@link org.inspirenxe.strata.spi.TextSource}.</li>
 *     <li>Create a host with {@code StrataCore.createScreenHost} (one screen) or {@code StrataCore.createScreenStack}
 *     (a stack of screens).</li>
 *     <li>Each frame, call {@code render} with your canvas. Forward window size changes to {@code resize}, and input
 *     to the {@link org.inspirenxe.strata.spi.InputSink} methods, converted to UI units and Strata's input types.
 *     {@link org.inspirenxe.strata.spi.HeldKeys} tells repeats from presses for engines that report both as
 *     presses.</li>
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
