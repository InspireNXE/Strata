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
 * Something an engine knows how to draw that Strata cannot describe itself, such as a model the engine renders or a
 * loaded image on Swing. Strata never looks inside a visual; it passes it to the engine's canvas, which draws the
 * kinds it made and a placeholder for any other.
 *
 * <p>Engine adapters make visuals in their own public classes ({@code SwingVisuals.image(...)} on Swing), so code
 * that has an engine object in hand can show it while the rest of the screen stays engine-free. Show one with
 * {@link org.inspirenxe.strata.api.ComponentFactory#image(Visual)} or draw it with
 * {@link org.inspirenxe.strata.api.extension.Painter#visual}.</p>
 *
 * <p>A visual is either a snapshot or live, and the adapter method that makes it says which. A snapshot shows the
 * object as it was when the visual was made; to show a change, make a new visual. A live visual holds an object the
 * caller made and owns, such as an image the caller draws into, and shows each change on the next paint.</p>
 *
 * <h2>Requirements for implementors</h2>
 * <ul>
 *     <li>Only engine adapters implement this interface, for objects their own canvas can draw.</li>
 *     <li>Keep the visual's own fields immutable. Its size never changes.</li>
 *     <li>Make a snapshot of an object that something other than the caller can change, such as an engine object
 *     the engine updates or shares: copy it when the visual is made, and implement {@code equals} and
 *     {@code hashCode} by what is drawn, so two snapshots of the same thing are equal.</li>
 *     <li>A visual may instead be live for an object the caller made and owns. It holds the object without copying
 *     it, its canvas draws what the object holds at each paint with nothing cached that would hide a change, and two
 *     live visuals are equal only when they hold the same object, by identity, at the same size.</li>
 *     <li>State on each method that makes visuals whether they are snapshots or live.</li>
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
