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
 * The implementations of the controls in {@link org.inspirenxe.strata.api.control.field}. Each one extends
 * {@link org.inspirenxe.strata.api.extension.CustomComponent} or one of its subclasses and reaches the node tree
 * only through the public extension hooks, the same ones a developer's own component gets.
 *
 * <p>The text box, the text area and the password box share their editing parts, which are package-private: an
 * {@code EditBuffer}, either a {@code PlainBuffer} or a {@code SecretBuffer} that zeroes a password's chars,
 * {@code TextBoundaries} for code point and word boundaries, and a {@code LineEditor} with the caret, selection,
 * scroll, keys and pointer of a single-line field. The text area keeps its own row handling.</p>
 */
package org.inspirenxe.strata.core.control.field;
