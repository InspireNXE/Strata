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
package org.inspirenxe.strata.api.control.button;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.Focusable;
import org.inspirenxe.strata.api.component.HasCursor;
import org.inspirenxe.strata.api.component.HasTooltip;
import org.inspirenxe.strata.api.control.attribute.item.ItemSource;
import org.inspirenxe.strata.api.control.attribute.item.SingleSelection;
import org.inspirenxe.strata.api.control.attribute.text.HasText;
import org.inspirenxe.strata.api.text.Text;

import java.util.function.Function;

/**
 * A button that steps through a list of values, one on each click, and shows the current one after its label, as in
 * {@code Difficulty: Hard}.
 *
 * <p>A left click, or Enter or Space while the button has focus, selects the next value, and the first after the
 * last. A right click, or Shift held with any of those, selects the previous value, and the last before the first.
 * The button consumes those inputs. A disabled button ignores them and draws its disabled face. With one value or
 * none, nothing changes, so no events fire.</p>
 *
 * <p>The text is the label that comes before the value. It is shown as {@code label: value}, or as the value alone
 * when the label is empty, and as the label alone while there is no value. Values are drawn with the
 * {@linkplain #setValueLabelFunction value label function}, {@link String#valueOf(Object)} by default. When the
 * items are set, the selection moves to the item with the selected item's {@linkplain #setKeyFunction key}, or to
 * the first item if there is none. Code can clear the selection with {@link #clearSelection()}.</p>
 *
 * <p>The button is focusable and tabbable, shows the hand cursor, and has a minimum size of 20 by 20. It asks for
 * the label plus its widest value, so cycling does not change its size, and the same padding as a {@link Button}
 * (8 units left and right, 5 top and bottom). The text is centered.</p>
 *
 * <h2>Events</h2>
 * <ul>
 *     <li>{@link SingleSelection#SELECT}: Asked before the user steps to another value. The value is the index of the
 *     value about to be selected. A handler may rewrite it to another item's index, or to -1 to clear the selection;
 *     an index outside the items is ignored. Fires only for the user's actions, and only when there is more than one
 *     item.</li>
 *     <li>{@link SingleSelection#SELECTED}: Told after the selection changed. The value is the selected index, or -1,
 *     and the previous value the index before. Fires for changes made from code too, including {@link #cycle} and
 *     {@link #setItems}.</li>
 * </ul>
 *
 * <h2>Theme keys</h2>
 * <p>It draws as a {@link Button}, with the same keys: {@value Button#STYLE_NORMAL}, {@value Button#STYLE_HOVERED},
 * {@value Button#STYLE_PRESSED}, {@value Button#STYLE_DISABLED}, {@value Button#STYLE_FOCUS},
 * {@value Button#STYLE_TEXT} and {@value Button#STYLE_TEXT_DISABLED}.</p>
 *
 * <pre>{@code
 * CycleButton<Difficulty> difficulty = factory.<Difficulty>cycleButton("Difficulty")
 *         .setItems(List.of(Difficulty.values()))
 *         .onEvent(SingleSelection.SELECTED, e -> this.apply());
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#cycleButton()}. Do not implement this
 * interface.</p>
 *
 * @param <T> the type of the values
 */
public interface CycleButton<T> extends Component<CycleButton<T>>, Focusable<CycleButton<T>>,
        HasCursor<CycleButton<T>>, HasTooltip<CycleButton<T>>, HasText<CycleButton<T>>,
        ItemSource<T, CycleButton<T>>, SingleSelection<T, CycleButton<T>> {

    /**
     * Returns the label that comes before the value.
     *
     * @return the label; empty for none
     */
    @Override
    Text text();

    /**
     * Replaces the label that comes before the value. A new layout runs.
     *
     * @param text the label, on one line, or an empty string for none
     * @return this button
     */
    @Override
    CycleButton<T> setText(Text text);

    /**
     * Sets how a value is turned into the text shown after the label. A new layout runs.
     *
     * @param valueLabelFunction returns what to show for a value: a {@link Text}, or any other value, shown by its
     *     appearance's name; the value itself by default
     * @return this button
     */
    CycleButton<T> setValueLabelFunction(Function<? super T, ?> valueLabelFunction);

    /**
     * Moves the selection by a number of steps, wrapping at the ends, as a click does for the user. Fires
     * {@link SingleSelection#SELECTED} if the selection changed, and never {@link SingleSelection#SELECT}. Does
     * nothing when there are no items. With nothing selected, one step forward selects the first item and one step
     * back the last.
     *
     * @param steps how many items to move forward, or backward when negative
     * @return this button
     */
    CycleButton<T> cycle(int steps);
}
