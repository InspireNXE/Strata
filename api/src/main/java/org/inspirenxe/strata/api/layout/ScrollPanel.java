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
package org.inspirenxe.strata.api.layout;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.Container;
import org.inspirenxe.strata.api.component.Focusable;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.control.ValueEvent;
import org.inspirenxe.strata.api.geometry.Rect;

/**
 * A container that shows part of content larger than itself and scrolls the rest into view.
 *
 * <h2>Layout</h2>
 * <p>Children are laid out as in an {@link AnchorPanel}, each given the whole scrolled area, which is the size of the
 * largest child plus its margin and at least the viewport. Most panels hold one {@link StackPanel} or
 * {@link GridPanel}. The viewport is the bounds minus the padding and minus the gutter of each shown scroll bar
 * ({@link #BAR_WIDTH} plus {@link #BAR_GAP}). Each axis has a {@link ScrollPolicy}; by default the vertical axis
 * scrolls ({@link ScrollPolicy#AUTO}) and the horizontal one does not ({@link ScrollPolicy#NEVER}), so children are
 * measured with the viewport's width and text wraps.</p>
 *
 * <p>The panel asks for its content's size plus padding and shown bars, but no more than it is offered on a
 * scrolling axis. Give it a fixed or maximum height, or stretch it in a parent with room, for it to scroll.</p>
 *
 * <h2>Scrolling</h2>
 * <ul>
 *     <li>The offset is in UI units, positive when scrolled down or right, and clamped to
 *     {@code [0, maxScroll]} on every change and after every layout. Before the first layout there is no range yet,
 *     so an offset set then is only kept from going below 0, and the first layout clamps it. Children are moved by
 *     the offset rounded to
 *     whole units, so whole-pixel engines do not shimmer; the stored offset keeps its fraction.</li>
 *     <li>A wheel notch scrolls {@linkplain #setScrollRate the scroll rate}, 10 units by default; precise scrolls
 *     are multiplied by the same rate. A panel that scrolls only sideways takes vertical wheel input too, and a
 *     vertical scroll with Shift held scrolls sideways while the horizontal axis scrolls; an engine that already
 *     turns it sends a horizontal scroll, which the panel takes as it is. The panel consumes a scroll only when its
 *     offset changes, so a scroll past the end reaches an outer panel.</li>
 *     <li>Keys no focused descendant consumed scroll it: Up and Down by the rate, Page Up and Page Down by the
 *     viewport less one rate, and Home and End to the ends, while it scrolls vertically, and Left and Right by the
 *     rate while it scrolls sideways. Held keys repeat. The panel leaves them alone with Control or Alt held, and
 *     consumes one only when the offset changed.</li>
 *     <li>A left press on a scroll bar's thumb grabs it and dragging moves it; a left press on the track pages
 *     toward the pointer.</li>
 *     <li>When a descendant gains focus, the panel scrolls the least distance that shows it.</li>
 * </ul>
 *
 * <h2>Drawing</h2>
 * <p>Children are clipped to the viewport and can only be hit inside it. Scroll bars draw over the children: the
 * track along the gutter and a thumb whose length is the visible fraction of the track, at least
 * {@link #MIN_THUMB_LENGTH} units and at most the track minus 8. On a track shorter than twice the minimum, the
 * thumb is at least half the track instead, so it can still move.</p>
 *
 * <h2>Theme keys</h2>
 * <ul>
 *     <li>{@value #STYLE_THUMB} (paint, required): the thumb.</li>
 *     <li>{@value #STYLE_THUMB_HOVERED} (paint, optional): the thumb while the pointer is over it or dragging it.
 *     Falls back to {@value #STYLE_THUMB}.</li>
 *     <li>{@value #STYLE_TRACK} (paint, optional): the track. Nothing is drawn if it is missing.</li>
 * </ul>
 *
 * <pre>{@code
 * StackPanel rows = factory.stackPanel().setSpacing(1);
 * rows.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);
 * ScrollPanel scroll = factory.scrollPanel().add(rows);
 * scroll.setHeight(200);
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#scrollPanel()}. Do not implement this
 * interface.</p>
 */
public interface ScrollPanel extends Container<ScrollPanel>, Focusable<ScrollPanel> {
    /**
     * Style key for the scroll bar thumb (paint, required).
     */
    String STYLE_THUMB = "scroll.thumb";
    /**
     * Style key for the thumb while hovered or dragged (paint, optional).
     */
    String STYLE_THUMB_HOVERED = "scroll.thumb.hovered";
    /**
     * Style key for the scroll bar track (paint, optional).
     */
    String STYLE_TRACK = "scroll.track";
    /**
     * Style key for how long a scroll by the wheel, a key or {@link #scrollToVisible(Rect)} takes to glide to its
     * new offset, in milliseconds (metric, optional). A further step while one glides adds to where it is headed.
     * {@link #setScroll}, {@link #scrollBy} and dragging the thumb move at once. Lists, tables, trees and text areas
     * read the same key. Defaults to 0: every scroll moves at once.
     */
    String STYLE_SMOOTH = "scroll.smooth";
    /**
     * How wide a scroll bar is, in UI units.
     */
    float BAR_WIDTH = 6f;
    /**
     * The space between a scroll bar and the viewport, in UI units.
     */
    float BAR_GAP = 2f;
    /**
     * The shortest a thumb gets, in UI units.
     */
    float MIN_THUMB_LENGTH = 32f;
    /**
     * How far one wheel notch scrolls by default, in UI units.
     */
    float DEFAULT_SCROLL_RATE = 10f;

    /**
     * Fired after the offset changed, by input or by a setter. The value is the panel's state after the change and
     * the previous value its state before. Offsets clamped during layout because the content shrank are not
     * reported. There is no request event for scrolling.
     */
    EventType<ValueEvent<ScrollPanel, State>> SCROLLED = EventType.valueReport("scroll_panel.scrolled");

    /**
     * Returns the horizontal offset.
     *
     * @return how far the content is scrolled right, in UI units
     */
    float scrollX();

    /**
     * Returns the vertical offset.
     *
     * @return how far the content is scrolled down, in UI units
     */
    float scrollY();

    /**
     * Returns the largest horizontal offset, as of the last layout.
     *
     * @return the content width less the viewport width, or 0
     */
    float maxScrollX();

    /**
     * Returns the largest vertical offset, as of the last layout.
     *
     * @return the content height less the viewport height, or 0
     */
    float maxScrollY();

    /**
     * Scrolls to an offset, clamped to the range. The children move before the next frame without being measured
     * again.
     *
     * @param x the horizontal offset
     * @param y the vertical offset
     * @return this panel
     * @throws IllegalArgumentException if either offset is NaN
     */
    ScrollPanel setScroll(float x, float y);

    /**
     * Scrolls by an amount, clamped to the range.
     *
     * @param dx the change to the horizontal offset
     * @param dy the change to the vertical offset
     * @return this panel
     * @throws IllegalArgumentException if either change is NaN
     */
    ScrollPanel scrollBy(float dx, float dy);

    /**
     * Scrolls the least distance that brings an area into the viewport, or its top-left part when it is larger.
     * Uses the bounds of the last layout.
     *
     * @param area the area, in the host's coordinates
     * @return this panel
     */
    ScrollPanel scrollToVisible(Rect area);

    /**
     * Scrolls the least distance that brings a descendant's bounds into the viewport.
     *
     * @param descendant a component inside this panel
     * @return this panel
     */
    ScrollPanel scrollToVisible(Component<?> descendant);

    /**
     * Returns the horizontal policy.
     *
     * @return {@link ScrollPolicy#NEVER} by default
     */
    ScrollPolicy horizontalPolicy();

    /**
     * Returns the vertical policy.
     *
     * @return {@link ScrollPolicy#AUTO} by default
     */
    ScrollPolicy verticalPolicy();

    /**
     * Sets whether each axis scrolls and when its scroll bar shows.
     *
     * @param horizontal the horizontal policy
     * @param vertical the vertical policy
     * @return this panel
     */
    ScrollPanel setPolicy(ScrollPolicy horizontal, ScrollPolicy vertical);

    /**
     * Returns the scroll rate.
     *
     * @return the units one wheel notch or arrow key scrolls; {@value #DEFAULT_SCROLL_RATE} by default
     */
    float scrollRate();

    /**
     * Sets how far one wheel notch or arrow key scrolls.
     *
     * @param unitsPerNotch the distance in UI units
     * @return this panel
     * @throws IllegalArgumentException if the rate is not greater than 0 or not finite
     */
    ScrollPanel setScrollRate(float unitsPerNotch);

    /**
     * Returns the viewport, as of the last layout.
     *
     * @return the area children show through, in the host's coordinates
     */
    Rect viewport();

    /**
     * Whether an axis scrolls and when its scroll bar shows.
     */
    enum ScrollPolicy {
        /** The axis does not scroll, and children are measured with the viewport's size on it. */
        NEVER,
        /** The axis scrolls, and its scroll bar shows only while the content is larger than the viewport. */
        AUTO,
        /** The axis scrolls, and its scroll bar always shows. */
        ALWAYS
    }

    /**
     * A scroll panel's own state in a {@link org.inspirenxe.strata.api.component.ComponentSnapshot}, and what
     * {@link #SCROLLED} reports.
     *
     * @param scrollX the horizontal offset
     * @param scrollY the vertical offset
     * @param maxScrollX the largest horizontal offset
     * @param maxScrollY the largest vertical offset
     */
    record State(float scrollX, float scrollY, float maxScrollX, float maxScrollY) {
    }
}
