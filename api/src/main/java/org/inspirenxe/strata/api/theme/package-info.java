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

 * Themes: how components look.
 *
 * <p>A {@link org.inspirenxe.strata.api.theme.Theme} maps style keys, such as {@code button.hovered}, to
 * {@link org.inspirenxe.strata.api.theme.Paint}s and {@link org.inspirenxe.strata.api.text.TextStyle}s. Each control
 * documents the keys it reads as constants on its interface ({@code Button.STYLE_HOVERED}). Some keys are required:
 * a host refuses a tree whose theme lacks them and lists every missing key in a
 * {@link org.inspirenxe.strata.api.theme.ThemeException}. Other keys are optional and fall back to a documented
 * default.</p>
 *
 * <p>Every component in one host shares the host's theme. Change it on the host; components re-read their keys and
 * a new layout runs.</p>
 *
 * <p>A {@link org.inspirenxe.strata.api.theme.ThemeRef} names a theme by id, so the engine's packs can restyle it.
 * {@link org.inspirenxe.strata.api.theme.ThemeRegistry}, from {@link org.inspirenxe.strata.api.Strata#themes()},
 * changes such a theme for every screen, or defines a new one on top of another, with
 * {@link org.inspirenxe.strata.api.theme.ThemeOverrides}.</p>
 */
package org.inspirenxe.strata.api.theme;
