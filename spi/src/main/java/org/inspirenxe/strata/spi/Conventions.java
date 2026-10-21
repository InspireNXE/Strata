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
 * How the engine's own controls behave, so Strata's behave the same way and players meet one set of habits. Looks
 * belong in the theme; these are the behaviors a theme does not reach.
 *
 * @param activateOnPress true when a button acts as soon as the pointer's left button goes down on it, as some
 *                        engines' buttons do. This covers buttons, cycle buttons, checkboxes, radio buttons, toggle
 *                        switches and tabs. When false, they act when the button comes back up still over them,
 *                        and moving off first cancels.
 */
public record Conventions(boolean activateOnPress) {
    /**
     * The conventions a platform has unless it says otherwise: buttons act on release.
     */
    public static final Conventions DEFAULT = new Conventions(false);
}
