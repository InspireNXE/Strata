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
 * Styled text: what controls show, how it looks, and how it is measured.
 *
 * <p>{@link org.inspirenxe.strata.api.text.Text} is what captions, titles, messages and tooltips take. It is a
 * description that a control resolves when it lays out, against the style it would draw plain text in, into
 * {@link org.inspirenxe.strata.api.text.TextSpan}s: runs of characters in one
 * {@link org.inspirenxe.strata.api.text.TextStyle}. Translated text reads the current language each time, and the
 * static methods on {@code Text} make it through the {@link org.inspirenxe.strata.api.text.TextFactory} that Strata's
 * core provides, so text can be made before any screen exists.</p>
 *
 * <p>A span can hold an {@link org.inspirenxe.strata.api.text.InlineElement} in place of characters: an
 * {@link org.inspirenxe.strata.api.text.InlineSprite} or an {@link org.inspirenxe.strata.api.text.InlineVisual},
 * made with {@link org.inspirenxe.strata.api.text.Text#sprite(org.inspirenxe.strata.api.theme.SpriteId)} and
 * {@link org.inspirenxe.strata.api.text.Text#visual}. Text fields style their text with a
 * {@link org.inspirenxe.strata.api.text.TextHighlighter}, which returns
 * {@link org.inspirenxe.strata.api.text.StyleRun}s.</p>
 *
 * <p>Text values carry no engine objects. The engine adapter decides the font, renders the span and measures it,
 * so the same span can measure differently on two engines. Build layouts that measure text through
 * {@link org.inspirenxe.strata.api.text.TextMetrics} instead of assuming character widths.</p>
 */
package org.inspirenxe.strata.api.text;
