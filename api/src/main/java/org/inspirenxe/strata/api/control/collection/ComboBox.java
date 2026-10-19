package org.inspirenxe.strata.api.control.collection;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.event.ControlEvent;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.RequestEvent;
import org.inspirenxe.strata.api.event.ValueEvent;
import org.inspirenxe.strata.api.event.ValueRequestEvent;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * A drop-down: shows the selected option and opens a list of all options to pick from.
 *
 * <p>A click opens the list as a popup below the box (above it if there is more room there), at least as wide as
 * the box. A click on an option selects it and closes the list; a click anywhere else, or Escape, closes it without
 * changing the selection. The list shows at most {@linkplain #setMaxVisibleOptions the maximum} rows and scrolls
 * the rest.</p>
 *
 * <p>While focused and closed: Up and Down select the previous and next option, Home and End the first and last,
 * and Enter, Space or Alt+Down open the list. While open: Up and Down move the highlight, Enter or Space selects it,
 * and Escape closes. A disabled box ignores input.</p>
 *
 * <p>Options are drawn with the {@linkplain #setLabelFunction label function}, {@code toString} by default. When
 * options are set, the selection moves to an equal option, or to the first option if there is none. A box with
 * options always has one selected.</p>
 *
 * <p>The selection events carry option indexes, so read the option from the box:
 * {@code box.options().get(e.value())}. The open list is private to the box and fires no events of its own.</p>
 *
 * <p>The box is focusable and tabbable and shows the hand cursor. It asks for its widest option plus padding and
 * the arrow, and one line plus 3 units above and below.</p>
 *
 * <h2>Theme keys</h2>
 * <ul>
 *     <li>{@value #STYLE_NORMAL} (paint, required): the box.</li>
 *     <li>{@value #STYLE_HOVERED} (paint, optional): the box while hovered, focused or open. Falls back to
 *     {@value #STYLE_NORMAL}.</li>
 *     <li>{@value #STYLE_DISABLED} (paint, optional): the box when disabled. Falls back to
 *     {@value #STYLE_NORMAL}.</li>
 *     <li>{@value #STYLE_POPUP} (paint, required): behind the open list.</li>
 *     <li>{@value #STYLE_TEXT} (text style, optional): the selected option and the options in the list. Falls back
 *     to the default style.</li>
 *     <li>{@value #STYLE_TEXT_DISABLED} (text style, optional): the selected option when disabled. Falls back to
 *     {@value #STYLE_TEXT}.</li>
 *     <li>The row keys of {@link ListView} and the scroll bar keys of
 *     {@link org.inspirenxe.strata.api.layout.ScrollPanel}, for the open list.</li>
 * </ul>
 *
 * <pre>{@code
 * ComboBox<SortType> sort = factory.comboBox(SortType::label)
 *         .setOptions(List.of(SortType.values()))
 *         .onEvent(ComboBox.SELECTED, e -> search());
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#comboBox(Function)}. Do not implement this
 * interface.</p>
 *
 * @param <T> the type of the options
 */
public interface ComboBox<T> extends Component<ComboBox<T>> {
    /**
     * Style key for the box (paint, required).
     */
    String STYLE_NORMAL = "combobox.normal";
    /**
     * Style key for the box while hovered, focused or open (paint, optional).
     */
    String STYLE_HOVERED = "combobox.hovered";
    /**
     * Style key for the box when disabled (paint, optional).
     */
    String STYLE_DISABLED = "combobox.disabled";
    /**
     * Style key for the background of the open list (paint, required).
     */
    String STYLE_POPUP = "combobox.popup";
    /**
     * Style key for the option text (text style, optional).
     */
    String STYLE_TEXT = "combobox.text";
    /**
     * Style key for the selected option when disabled (text style, optional).
     */
    String STYLE_TEXT_DISABLED = "combobox.text.disabled";
    /**
     * How many rows the open list shows by default before it scrolls.
     */
    int DEFAULT_MAX_VISIBLE_OPTIONS = 8;
    /**
     * Asked before the user opens the list: a click on the closed box, or Enter, Space or Alt and Down while it has
     * focus. It fires only when the list can open, which needs the box attached, enabled and with options. A handler
     * may change the options before the list opens; the box checks again afterwards and stays closed if it has no
     * options left. Fires only for the user's actions.
     */
    EventType<RequestEvent<ComboBox<?>>> OPEN = EventType.request("combo_box.open");
    /**
     * Told after the list opened. Fires for {@link #open()} called from code too.
     */
    EventType<ControlEvent<ComboBox<?>>> OPENED = EventType.report("combo_box.opened");
    /**
     * Asked before the user changes the selection: a click on a row of the open list, Enter or Space on the
     * highlighted row of the open list, or the Up, Down, Home and End keys on the closed box. A choice from the open
     * list closes it before this fires. Moving the highlight in the open list does not fire it, and neither does a
     * choice of the option already selected. The value is the index of the option about to be selected. A handler
     * may rewrite it to another option's index; an index outside the options is ignored. Fires only for the user's
     * actions.
     */
    EventType<ValueRequestEvent<ComboBox<?>, Integer>> SELECT = EventType.valueRequest("combo_box.select");
    /**
     * Told after the selection changed. The value is the selected index, -1 once there are no options, and the
     * previous value is the index before. It also fires when {@link #setOptions} keeps the index but the option
     * there is no longer equal to the one before, so the value can equal the previous value. Fires for changes made
     * from code too, including {@link #setOptions}.
     */
    EventType<ValueEvent<ComboBox<?>, Integer>> SELECTED = EventType.valueReport("combo_box.selected");

    /**
     * Returns the options.
     *
     * @return an unmodifiable copy of the options
     */
    List<T> options();

    /**
     * Replaces the options and closes the list. The selection moves to an equal option, or to the first one.
     * {@link #SELECTED} fires if the selected option or its index changed.
     *
     * @param options the options; the box keeps a copy
     * @return this box
     */
    ComboBox<T> setOptions(List<? extends T> options);

    /**
     * Sets how options are drawn.
     *
     * @param labelFunction returns the text for an option
     * @return this box
     */
    ComboBox<T> setLabelFunction(Function<? super T, String> labelFunction);

    /**
     * Returns the selected option.
     *
     * @return the selected option, or empty while there are no options
     */
    Optional<T> selected();

    /**
     * Returns the selected index.
     *
     * @return the index of the selected option, or -1 while there are no options
     */
    int selectedIndex();

    /**
     * Selects the option at an index.
     *
     * @param index the index
     * @return this box
     * @throws IndexOutOfBoundsException if the index is not an option's index
     */
    ComboBox<T> select(int index);

    /**
     * Selects the first option equal to the given one. Does nothing if there is none.
     *
     * @param option the option
     * @return this box
     */
    ComboBox<T> selectOption(T option);

    /**
     * Returns how many rows the open list shows before it scrolls.
     *
     * @return the maximum; {@value #DEFAULT_MAX_VISIBLE_OPTIONS} by default
     */
    int maxVisibleOptions();

    /**
     * Sets how many rows the open list shows before it scrolls.
     *
     * @param count the maximum
     * @return this box
     * @throws IllegalArgumentException if the count is less than 1
     */
    ComboBox<T> setMaxVisibleOptions(int count);

    /**
     * Returns whether the list is open.
     *
     * @return true while the popup shows
     */
    boolean isOpen();

    /**
     * Opens the list. Does nothing while detached, disabled, without options or already open.
     *
     * @return this box
     */
    ComboBox<T> open();

    /**
     * Closes the list without changing the selection.
     *
     * @return this box
     */
    ComboBox<T> close();
}
