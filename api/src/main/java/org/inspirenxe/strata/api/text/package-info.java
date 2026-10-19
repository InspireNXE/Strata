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
 * Styled text: spans of characters with a {@link org.inspirenxe.strata.api.text.TextStyle}, and the
 * {@link org.inspirenxe.strata.api.text.TextMetrics} that measures them.
 *
 * <p>Text values carry no engine objects. The engine adapter decides the font, renders the span and measures it,
 * so the same span is a little wider on one engine than another. Build layouts that measure text through
 * {@code TextMetrics} instead of assuming character widths.</p>
 */
package org.inspirenxe.strata.api.text;
