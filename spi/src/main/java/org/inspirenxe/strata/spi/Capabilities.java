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
 *                      both itself, each piece a {@link Canvas#spriteRegion} call.
 * @param drawsShapes true when the canvas implements every shape call: {@link Canvas#line},
 *                    {@link Canvas#polyline}, {@link Canvas#fillRoundRect}, {@link Canvas#strokeRect},
 *                    {@link Canvas#fillEllipse}, {@link Canvas#strokeEllipse}, {@link Canvas#strokeArc},
 *                    {@link Canvas#fillShape}, {@link Canvas#strokeShape},
 *                    {@link Canvas#fillGradient(org.inspirenxe.strata.api.geometry.Rect,
 *                    org.inspirenxe.strata.api.extension.Gradient) fillGradient} with a {@code Gradient} and
 *                    {@link Canvas#shadow}, and honors {@link Canvas#pushRoundClip} and
 *                    {@link Canvas#pushSmoothing}. When false, the host never calls them: it draws every shape from
 *                    fills one device pixel high, a gradient at an angle from columns of vertical gradients, a
 *                    shadow from a few rings of falling alpha, and a round clip as a clip to the whole
 *                    rectangle. Those look stepped and work on every canvas.
 * @param shapesRightToLeft true when the engine orders and shapes right-to-left text itself, joining Arabic letters,
 *                          so core passes each right-to-left run to
 *                          {@link Canvas#text(org.inspirenxe.strata.api.text.TextSpan, float, float, boolean)} in
 *                          logical order with its direction. When false, core passes each such run to
 *                          {@link Canvas#text(org.inspirenxe.strata.api.text.TextSpan, float, float)} with its
 *                          characters in reverse order and its brackets mirrored, which reads correctly for Hebrew
 *                          and draws Arabic unjoined. Either way core runs the bidirectional algorithm and places the
 *                          runs of a line in visual order.
 * @param blursBackdrop true when the canvas implements {@link Canvas#backdrop}, blurring a backdrop it has or can
 *                      read. When false, core draws a backdrop paint as a rounded rectangle of its tint.
 */
public record Capabilities(boolean sendsKeyRepeat, boolean monospaceFont, boolean slicesSprites,
                           boolean drawsShapes, boolean shapesRightToLeft, boolean blursBackdrop) {
    /**
     * Creates capabilities for an engine that cannot blur a backdrop.
     *
     * @param sendsKeyRepeat true when the adapter delivers key repeats itself
     * @param monospaceFont true when the engine has a monospaced font
     * @param slicesSprites true when the engine stores slicing with its images
     * @param drawsShapes true when the canvas implements every shape call
     * @param shapesRightToLeft true when the engine shapes right-to-left text itself
     */
    public Capabilities(final boolean sendsKeyRepeat, final boolean monospaceFont, final boolean slicesSprites,
                        final boolean drawsShapes, final boolean shapesRightToLeft) {
        this(sendsKeyRepeat, monospaceFont, slicesSprites, drawsShapes, shapesRightToLeft, false);
    }

    /**
     * Creates capabilities for an engine that stores no slicing with its images and draws no shapes.
     *
     * @param sendsKeyRepeat true when the adapter delivers key repeats itself
     * @param monospaceFont true when the engine has a monospaced font
     */
    public Capabilities(final boolean sendsKeyRepeat, final boolean monospaceFont) {
        this(sendsKeyRepeat, monospaceFont, false, false);
    }

    /**
     * Creates capabilities for an engine that draws no shapes.
     *
     * @param sendsKeyRepeat true when the adapter delivers key repeats itself
     * @param monospaceFont true when the engine has a monospaced font
     * @param slicesSprites true when the engine stores slicing with its images
     */
    public Capabilities(final boolean sendsKeyRepeat, final boolean monospaceFont, final boolean slicesSprites) {
        this(sendsKeyRepeat, monospaceFont, slicesSprites, false);
    }

    /**
     * Creates capabilities for an engine that does not shape right-to-left text.
     *
     * @param sendsKeyRepeat true when the adapter delivers key repeats itself
     * @param monospaceFont true when the engine has a monospaced font
     * @param slicesSprites true when the engine stores slicing with its images
     * @param drawsShapes true when the canvas implements every shape call
     */
    public Capabilities(final boolean sendsKeyRepeat, final boolean monospaceFont, final boolean slicesSprites,
                        final boolean drawsShapes) {
        this(sendsKeyRepeat, monospaceFont, slicesSprites, drawsShapes, false);
    }
}
