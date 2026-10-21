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
 * Popups, dialogs and toasts drawn above a screen.
 *
 * <p>{@link org.inspirenxe.strata.api.overlay.Overlays} opens them and
 * {@link org.inspirenxe.strata.api.overlay.Overlay} is one that is open. Each host has one {@code Overlays}, from
 * {@link org.inspirenxe.strata.api.ComponentFactory#overlays()}. Drop-down lists, menus and flyouts use popups;
 * message boxes and quantity prompts use dialogs, which are modal and keep the screen visible behind them; short
 * notices use toasts, which close themselves.</p>
 *
 * <p>An overlay's content is outside the screen's tree. Nothing in the screen clips it or makes room for it, and its
 * control events reach only handlers inside it and on the {@code Overlay}, never the screen's content. The host
 * draws the screen first, then each open overlay in the order it opened, then the tooltip.</p>
 */
package org.inspirenxe.strata.api.overlay;
