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
package org.inspirenxe.strata.api.component;

/**
 * Mouse cursor shapes a component can ask for with {@link HasCursor#setCursor}. An engine without one of these
 * shapes shows the closest it has; the Swing adapter shows the default arrow for {@link #NOT_ALLOWED}.
 */
public enum CursorKind {
    /** The default pointer. */
    ARROW,
    /** The text I-beam, for editable text. */
    TEXT,
    /** The pointing hand, for clickable things. */
    HAND,
    /** A crosshair, for precise picking. */
    CROSSHAIR,
    /** A left-right arrow, for resizing horizontally. */
    RESIZE_HORIZONTAL,
    /** An up-down arrow, for resizing vertically. */
    RESIZE_VERTICAL,
    /** A four-way arrow, for moving. */
    RESIZE_ALL,
    /** A "no" sign, for something that cannot be used now. */
    NOT_ALLOWED
}
