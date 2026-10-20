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
package org.inspirenxe.strata.core.node;

/**
 * A component that holds secret text, such as a password box. Core calls it at the moments it must forget or hide
 * that text. Only core's own controls implement it; a facade that does is found by an {@code instanceof} check on
 * the node's facade.
 */
public interface SecretHolder {

    /**
     * Zeroes and empties what the component holds, and hides it if it showed, firing nothing. {@link Root} calls it
     * for every holder in a tree it lets go of: the old content after {@link Root#setContent} replaced it, which the
     * host does after {@code Screen.closed()} and on a rebuild, and an overlay's content after the overlay closed. A
     * holder that the new content took over is not wiped.
     */
    void wipe();

    /**
     * Called after the component itself was disabled, so it can hide what it shows. Disabling an ancestor instead
     * takes focus from the component, which a holder handles as it handles any focus loss.
     */
    void disabled();
}
