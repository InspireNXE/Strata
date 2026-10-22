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
 * A test kit for applications: open a {@link org.inspirenxe.strata.api.screen.Screen} without an engine or a
 * display, find its components, drive it with simulated input and check what it shows.
 *
 * <pre>{@code
 * try (ScreenTester tester = ScreenTester.open(new ShopScreen(shop))) {
 *     tester.click(tester.byId("quantity", TextBox.class)).type("3")
 *           .click(tester.find(Button.class, Matching.text("Buy")));
 *     assertEquals("29.97", tester.byId("total", Label.class).text().plain());
 * }
 * }</pre>
 *
 * <p>The kit ships in the test fixtures of {@code strata-core}; a Gradle build adds it with
 * {@code testImplementation(testFixtures("org.inspirenxe:strata-core:<version>"))}. Its API uses the types of
 * {@code strata-api} and {@code strata-spi} only, and it brings core at run time. Tests that compare pixels need a
 * real renderer, such as Strata-Swing's snapshots.</p>
 */
package org.inspirenxe.strata.testing;
