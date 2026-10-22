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
 * Mouse cursor shapes a component can ask for with {@link HasCursor#setCursor}. A canvas that cannot show a kind
 * says so through {@code Canvas.showsCursor}, and the host then asks for the kind's {@link #fallback()}, and that
 * kind's, down to {@link #ARROW}, so every kind shows something close on every engine.
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
    NOT_ALLOWED,
    /** A diagonal arrow from the top left to the bottom right, for resizing from those corners. */
    RESIZE_DIAGONAL_DOWN,
    /** A diagonal arrow from the bottom left to the top right, for resizing from those corners. */
    RESIZE_DIAGONAL_UP,
    /** The busy cursor, for when nothing can be done until work finishes. */
    WAIT,
    /** The arrow with a busy sign, for work going on while the user can still act. */
    PROGRESS,
    /** An open hand, for something that can be dragged. */
    GRAB,
    /** A closed hand, for something being dragged. */
    GRABBING,
    /** The arrow with a plus sign, for a drag that copies where it is dropped. */
    COPY,
    /** The sign for a drag over a place that takes nothing. */
    NO_DROP,
    /** The arrow with a question mark, for help on what is under it. */
    HELP;

    /**
     * Returns the kind to show where this one is missing. The chain from every kind ends at {@link #ARROW}, whose
     * fallback is itself: the diagonal arrows fall back to {@link #RESIZE_ALL}, the grab cursors to {@link #HAND},
     * {@link #NO_DROP} to {@link #NOT_ALLOWED}, and the others to the arrow.
     *
     * @return the fallback kind
     */
    public CursorKind fallback() {
        return switch (this) {
            case RESIZE_DIAGONAL_DOWN, RESIZE_DIAGONAL_UP -> RESIZE_ALL;
            case GRAB, GRABBING -> HAND;
            case NO_DROP -> NOT_ALLOWED;
            default -> ARROW;
        };
    }
}
