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
package org.inspirenxe.strata.spi;

/**
 * What an engine does for itself, so a host can fill in the rest.
 *
 * @param sendsKeyRepeat true when the adapter delivers {@link org.inspirenxe.strata.api.input.KeyAction#REPEAT}
 *                       for held keys itself. When false, the host makes repeats for navigation and delete keys
 *                       (after 400 ms, then every 33 ms, timed by the platform's clock while {@code render} is
 *                       called). An adapter whose engine reports repeats as more presses must tell them apart and
 *                       send {@code REPEAT}; then this is true.
 * @param monospaceFont true when the engine has a monospaced font, so a span whose style is
 *                      {@linkplain org.inspirenxe.strata.api.text.TextStyle#monospace() monospace} is measured and
 *                      drawn with every character the same width. When false, such a span uses the engine's normal
 *                      font; it is still measured and drawn alike, so layout stays correct and only columns stop
 *                      lining up.
 * @param slicesSprites true when the engine stores slicing with its images and applies it whenever it draws them,
 *                      in {@link Canvas#sprite} as well as {@link Canvas#nineSlice}. The host then passes a sized
 *                      {@linkplain org.inspirenxe.strata.api.theme.Paint.NineSlice nine-slice} to {@code nineSlice}
 *                      and a {@linkplain org.inspirenxe.strata.api.theme.Paint.Tiled tiled} sprite to
 *                      {@code sprite}, and the engine's slicing decides how they look. When false, the host draws
 *                      both itself, each part being the whole sprite drawn with {@code sprite} inside a clip, so
 *                      the canvas must draw a sprite exactly over the rectangle it is given, even where the
 *                      rectangle reaches past the clip.
 */
public record Capabilities(boolean sendsKeyRepeat, boolean monospaceFont, boolean slicesSprites) {
    /**
     * Creates capabilities for an engine that stores no slicing with its images.
     *
     * @param sendsKeyRepeat true when the adapter delivers key repeats itself
     * @param monospaceFont true when the engine has a monospaced font
     */
    public Capabilities(final boolean sendsKeyRepeat, final boolean monospaceFont) {
        this(sendsKeyRepeat, monospaceFont, false);
    }
}
