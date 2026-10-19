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
 * Immutable geometry values used everywhere in Strata.
 *
 * <p>All coordinates are in UI units, as floats. The origin is the top-left corner of the area a host shows, x
 * grows to the right and y grows downward. How a UI unit maps to screen pixels is up to the engine adapter. An
 * adapter may divide screen pixels by a scale the player picks, and the Swing adapter uses a fixed scale factor.</p>
 */
package org.inspirenxe.strata.api.geometry;
