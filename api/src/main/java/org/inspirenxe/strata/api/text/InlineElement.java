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
package org.inspirenxe.strata.api.text;

/**
 * Something drawn in a line of text in place of characters: a sprite or an engine visual. Text holds one with
 * {@link Text#sprite(org.inspirenxe.strata.api.theme.SpriteId)} or
 * {@link Text#visual(org.inspirenxe.strata.api.visual.Visual)}, and it resolves to a {@link TextSpan} whose
 * {@linkplain TextSpan#element() element} it is.
 */
public sealed interface InlineElement permits InlineSprite, InlineVisual {
}
