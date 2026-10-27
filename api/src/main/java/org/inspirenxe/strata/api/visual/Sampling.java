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

/**
 * How a sprite or a pixel buffer drawn at another size than its own picks the color of each device pixel. Pixel art
 * wants {@link #NEAREST}, which is the default; photos, gradients and blurred pictures want {@link #LINEAR}. Choose
 * it for an {@link org.inspirenxe.strata.api.control.display.Image} with its {@code setSampling}, or for anything a
 * custom component draws with {@link org.inspirenxe.strata.api.extension.Painter#sampled}. An engine that cannot
 * choose draws with its own sampling either way.
 */
public enum Sampling {
    /** Each device pixel takes the color of the nearest source pixel, so edges stay hard when scaled up. */
    NEAREST,
    /** Each device pixel blends the source pixels around it, so a picture scales smoothly. */
    LINEAR
}
