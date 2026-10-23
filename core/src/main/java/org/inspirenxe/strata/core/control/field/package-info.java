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
 * {@link org.inspirenxe.strata.core.node.BuiltInComponent} or one of its subclasses, core's counterparts of the
 * public extension bases, which give it the same hooks a developer's component gets and direct access to its node.
 *
 * <p>The text box, the text area and the password box share their editing parts, which are package-private: an
 * {@code EditBuffer}, either a {@code PlainBuffer} or a {@code SecretBuffer} that zeroes a password's chars,
 * {@code TextBoundaries} for character and word boundaries, and a {@code TextEditor} with the caret, selection, keys
 * and pointer. A {@code LineEditor} adds the sideways scroll and drawing of a single-line field, and the text area
 * lays out and draws its own rows. The text box and the text area turn a user's edit into new text through the same
 * {@code PlainEdits}.</p>
 */
package org.inspirenxe.strata.core.control.field;
