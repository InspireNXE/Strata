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
package org.inspirenxe.strata.api.control.pane;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.Focusable;
import org.inspirenxe.strata.api.component.HasContextMenu;
import org.inspirenxe.strata.api.component.HasCursor;
import org.inspirenxe.strata.api.component.HasTooltip;
import org.inspirenxe.strata.api.control.attribute.value.Ranged;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.api.layout.ScrollPanel;

/**
 * A scroll bar on its own, for a component that scrolls its content itself, such as a custom list, a map or a chat
 * log. A {@link ScrollPanel}, a list and a text area draw their own bars and do not need one.
 *
 * <h2>Values</h2>
 * <p>The value is the scroll offset: how far the content is scrolled down (vertical) or right (horizontal). The range
 * runs from 0 to the content's length minus the viewport's, and the {@linkplain #visibleAmount() visible amount} is
 * the viewport's length, in the same units. {@link #setExtent(double, double)} sets both from those two lengths,
 * which is all most callers need. The range may be empty ({@code min == max}), which means there is nothing to
 * scroll; a new bar starts that way. The value is clamped to the range and kept as given, with no step.</p>
 *
 * <h2>Input</h2>
 * <ul>
 *     <li>A left press on the thumb grabs it, and dragging moves it, even outside the bar. A left press on the track
 *     beside the thumb moves one page toward the pointer.</li>
 *     <li>Scrolling over the bar moves it by the {@linkplain #scrollRate() scroll rate} per notch. A horizontal bar
 *     takes vertical scrolling too when there is no horizontal scrolling. The bar consumes a scroll only when the
 *     value changed, so a scroll at the end reaches its ancestors.</li>
 *     <li>While focused: Up and Down (vertical) or Left and Right (horizontal) move by the scroll rate, Page Up and
 *     Page Down by a page, and Home and End go to the ends. Up and Left move toward the start. A bar is not focusable
 *     unless {@linkplain #setFocusable(boolean) made so}, since the content it scrolls usually takes the keys.</li>
 * </ul>
 * <p>A page is the visible amount less one scroll rate, and at least one scroll rate. A disabled bar ignores input
 * and draws its thumb with the disabled look, as does a bar with nothing to scroll.</p>
 *
 * <h2>Drawing</h2>
 * <p>The track covers the bounds. The thumb spans the bar across, and along it its length is the track's length times
 * the visible amount over the content's length (the range plus the visible amount), at least
 * {@link ScrollPanel#MIN_THUMB_LENGTH} units ({@link #SLIM_MIN_THUMB_LENGTH} for a slim bar) and at most the track
 * minus 8, but never longer than the track. On a track shorter than twice that minimum, the minimum is half the
 * track, so the thumb can always move. With nothing to scroll the thumb fills the track. A vertical bar wants
 * {@link #THICKNESS} by 100 units and a horizontal one 100 by {@link #THICKNESS}; a {@linkplain #setSlim slim} bar
 * is {@link #SLIM_THICKNESS} thick. Stretch it along the content it scrolls.</p>
 *
 * <h2>Events</h2>
 * <ul>
 *     <li>{@link Ranged#CHANGE}: Fired when the user is about to scroll: a press on the track, each move of a thumb
 *     drag, a scroll, or a key while focused. The value is the new offset, already clamped to the range. Cancel the
 *     event to stay put, or set another value, which is clamped again. Input that would leave the offset as it is fires
 *     nothing. Fires only for the user's actions: {@link #setValue}, {@link #setRange} and {@link #setExtent} fire
 *     none.</li>
 *     <li>{@link Ranged#CHANGED}: Fired after the offset changed: by the user, by {@link #setValue}, or by
 *     {@link #setRange} or {@link #setExtent} clamping it. The value is the new offset and {@code previous()} the old
 *     one. A change of the visible amount alone fires nothing. Fires for changes made from code too.</li>
 * </ul>
 *
 * <h2>Theme keys</h2>
 * <p>A standard bar uses the same keys as a scroll panel's bars, so they look alike:</p>
 * <ul>
 *     <li>{@value #STYLE_TRACK} (paint, optional): the track. Nothing is drawn if it is missing.</li>
 *     <li>{@value #STYLE_THUMB} (paint, required): the thumb.</li>
 *     <li>{@value #STYLE_THUMB_HOVERED} (paint, optional): the thumb while the pointer is over it or dragging it.
 *     Falls back to {@value #STYLE_THUMB}.</li>
 *     <li>{@value #STYLE_THUMB_DISABLED} (paint, optional): the thumb when disabled or when there is nothing to
 *     scroll. Falls back to {@value #STYLE_THUMB}.</li>
 * </ul>
 * <p>A slim bar has keys of its own, each falling back to its standard key, so a theme that has none still draws
 * it:</p>
 * <ul>
 *     <li>{@value #STYLE_SLIM_TRACK} (paint, optional): falls back to {@value #STYLE_TRACK}.</li>
 *     <li>{@value #STYLE_SLIM_THUMB} (paint, optional): falls back to {@value #STYLE_THUMB}.</li>
 *     <li>{@value #STYLE_SLIM_THUMB_HOVERED} (paint, optional): falls back to {@value #STYLE_SLIM_THUMB}.</li>
 *     <li>{@value #STYLE_SLIM_THUMB_DISABLED} (paint, optional): falls back to {@value #STYLE_SLIM_THUMB}.</li>
 * </ul>
 *
 * <pre>{@code
 * ScrollBar bar = factory.scrollBar()
 *         .setExtent(log.viewHeight(), log.contentHeight())
 *         .onEvent(Ranged.CHANGED, event -> log.setScrollY(event.value()));
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#scrollBar()}. Do not implement this
 * interface.</p>
 */
public interface ScrollBar extends Component<ScrollBar>, Focusable<ScrollBar>, HasCursor<ScrollBar>,
        HasTooltip<ScrollBar>, HasContextMenu<ScrollBar>, Ranged<ScrollBar> {
    /**
     * Style key for the track (paint, optional). The same key as {@link ScrollPanel#STYLE_TRACK}.
     */
    String STYLE_TRACK = ScrollPanel.STYLE_TRACK;
    /**
     * Style key for the thumb (paint, required). The same key as {@link ScrollPanel#STYLE_THUMB}.
     */
    String STYLE_THUMB = ScrollPanel.STYLE_THUMB;
    /**
     * Style key for the thumb while hovered or dragged (paint, optional). The same key as
     * {@link ScrollPanel#STYLE_THUMB_HOVERED}.
     */
    String STYLE_THUMB_HOVERED = ScrollPanel.STYLE_THUMB_HOVERED;
    /**
     * Style key for the thumb when disabled or when there is nothing to scroll (paint, optional).
     */
    String STYLE_THUMB_DISABLED = "scroll.thumb.disabled";
    /**
     * Style key for a slim bar's track (paint, optional).
     */
    String STYLE_SLIM_TRACK = "scroll.slim.track";
    /**
     * Style key for a slim bar's thumb (paint, optional).
     */
    String STYLE_SLIM_THUMB = "scroll.slim.thumb";
    /**
     * Style key for a slim bar's thumb while hovered or dragged (paint, optional).
     */
    String STYLE_SLIM_THUMB_HOVERED = "scroll.slim.thumb.hovered";
    /**
     * Style key for a slim bar's thumb when disabled or when there is nothing to scroll (paint, optional).
     */
    String STYLE_SLIM_THUMB_DISABLED = "scroll.slim.thumb.disabled";
    /**
     * How thick a standard bar is, in UI units: the same as a scroll panel's bars.
     */
    float THICKNESS = ScrollPanel.BAR_WIDTH;
    /**
     * How thick a slim bar is, in UI units.
     */
    float SLIM_THICKNESS = 2f;
    /**
     * The shortest a slim bar's thumb gets, in UI units.
     */
    float SLIM_MIN_THUMB_LENGTH = 16f;

    /**
     * Returns the scroll offset.
     *
     * @return the offset, within the range; 0 by default
     */
    @Override
    double value();

    /**
     * Sets the scroll offset, clamped to the range. {@link Ranged#CHANGED} fires if it changed; {@link Ranged#CHANGE}
     * does not. Works while disabled too.
     *
     * @param value the offset
     * @return this bar
     * @throws IllegalArgumentException if {@code value} is NaN
     */
    @Override
    ScrollBar setValue(double value);

    /**
     * Returns the smallest offset.
     *
     * @return the minimum; 0 by default
     */
    @Override
    double min();

    /**
     * Returns the largest offset.
     *
     * @return the maximum; 0 by default, so a new bar has nothing to scroll
     */
    @Override
    double max();

    /**
     * Sets the range of the offset and clamps the offset into it. {@link Ranged#CHANGED} fires if that changed it.
     * An empty range, with {@code min} equal to {@code max}, means there is nothing to scroll.
     *
     * @param min the smallest offset, usually 0
     * @param max the largest offset, usually the content's length minus the viewport's
     * @return this bar
     * @throws IllegalArgumentException if either end is not finite, or {@code min} is greater than {@code max}
     */
    @Override
    ScrollBar setRange(double min, double max);

    /**
     * Returns how much of the content shows at once, in the units of the value.
     *
     * @return the visible amount; 0 by default
     */
    double visibleAmount();

    /**
     * Sets how much of the content shows at once, in the units of the value, usually the viewport's length. It sizes
     * the thumb and the page. The offset does not change, and no event fires.
     *
     * @param visibleAmount the visible amount
     * @return this bar
     * @throws IllegalArgumentException if {@code visibleAmount} is negative or not finite
     */
    ScrollBar setVisibleAmount(double visibleAmount);

    /**
     * Sets the bar up for content of one length seen through a viewport of another: the range becomes 0 to
     * {@code max(0, extent - viewport)} and the visible amount {@code viewport}. The offset is clamped into the new
     * range, and {@link Ranged#CHANGED} fires if that changed it.
     *
     * @param viewport the length of the part that shows
     * @param extent the length of the whole content
     * @return this bar
     * @throws IllegalArgumentException if either length is negative or not finite
     */
    ScrollBar setExtent(double viewport, double extent);

    /**
     * Returns how far one scroll notch or arrow key moves the offset.
     *
     * @return the scroll rate; {@link ScrollPanel#DEFAULT_SCROLL_RATE} by default
     */
    double scrollRate();

    /**
     * Sets how far one scroll notch or arrow key moves the offset, in the units of the value. Precise scrolls are
     * multiplied by it too.
     *
     * @param rate the scroll rate
     * @return this bar
     * @throws IllegalArgumentException if {@code rate} is not greater than 0 or not finite
     */
    ScrollBar setScrollRate(double rate);

    /**
     * Returns which way the bar runs.
     *
     * @return the orientation; {@link Orientation#VERTICAL} by default
     */
    Orientation orientation();

    /**
     * Sets which way the bar runs. A new layout runs so the desired size follows.
     *
     * @param orientation {@link Orientation#VERTICAL} for content that scrolls up and down,
     *                    {@link Orientation#HORIZONTAL} for content that scrolls sideways
     * @return this bar
     */
    ScrollBar setOrientation(Orientation orientation);

    /**
     * Returns whether the bar is slim.
     *
     * @return true for a slim bar; false by default
     */
    boolean isSlim();

    /**
     * Makes the bar slim or standard. A slim bar is {@link #SLIM_THICKNESS} thick, has a thumb at least
     * {@link #SLIM_MIN_THUMB_LENGTH} long, and draws with the {@code scroll.slim.*} keys; it suits chat logs,
     * tooltips and other tight places. Input works the same. A new layout runs so the desired size follows.
     *
     * @param slim true for a slim bar
     * @return this bar
     */
    ScrollBar setSlim(boolean slim);

    /**
     * A scroll bar's own state in a {@link org.inspirenxe.strata.api.component.ComponentSnapshot}.
     *
     * @param value the {@linkplain #value() offset}
     * @param min the {@linkplain #min() smallest offset}
     * @param max the {@linkplain #max() largest offset}
     * @param visibleAmount the {@linkplain #visibleAmount() visible amount}
     */
    record State(double value, double min, double max, double visibleAmount) {
    }
}
