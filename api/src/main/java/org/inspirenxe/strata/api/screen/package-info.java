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
 * Screens: the unit a host shows.
 *
 * <p>A {@link org.inspirenxe.strata.api.screen.Screen} builds a component tree when the host opens it, and can ask
 * for a rebuild or to be closed through the {@link org.inspirenxe.strata.api.screen.ScreenHandle} it receives.
 * Open a screen with {@link org.inspirenxe.strata.api.Strata#screens()}, the platform's
 * {@link org.inspirenxe.strata.api.screen.ScreenController}, or above an open screen with its handle. Engine
 * adapters decide where a screen opens.</p>
 */
package org.inspirenxe.strata.api.screen;
