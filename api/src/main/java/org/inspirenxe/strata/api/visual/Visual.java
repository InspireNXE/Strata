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
package org.inspirenxe.strata.api.visual;

import org.inspirenxe.strata.api.geometry.Size;

/**
 * Something an engine knows how to draw that Strata cannot describe itself, such as a game item or a loaded image
 * on Swing. Strata never looks inside a visual; it passes it to the engine's canvas, which draws the kinds it made
 * and a placeholder for any other.
 *
 * <p>Engine adapters make visuals in their own public classes ({@code SwingVisuals.image(...)} on Swing), so code
 * that has an engine object in hand can show it while the rest of the screen stays engine-free. Show one with
 * {@link org.inspirenxe.strata.api.ComponentFactory#image(Visual)} or draw it with
 * {@link org.inspirenxe.strata.api.extension.Painter#visual}.</p>
 *
 * <h2>Requirements for implementors</h2>
 * <ul>
 *     <li>Only engine adapters implement this interface, for objects their own canvas can draw.</li>
 *     <li>Make visuals immutable. Copy mutable engine objects when the visual is made, so a later change to the
 *     original does not show; make a new visual to show the change.</li>
 *     <li>Implement {@code equals} and {@code hashCode} by what is drawn, so two visuals of the same thing are
 *     equal.</li>
 * </ul>
 */
public interface Visual {

    /**
     * Returns the size the visual is meant to be drawn at, which an image control asks for by default.
     *
     * @return the natural size in UI units, for example 16 by 16 for a small icon
     */
    Size size();
}
