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
package org.inspirenxe.strata.core.control.button;

import org.inspirenxe.strata.api.control.attribute.item.Selection;
import org.inspirenxe.strata.api.control.button.RadioButton;
import org.inspirenxe.strata.api.control.button.RadioGroup;
import org.inspirenxe.strata.api.event.Event;
import org.inspirenxe.strata.api.event.EventHandler;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.Subscription;
import org.inspirenxe.strata.api.event.cause.CauseFrame;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.core.event.EventDispatcher;
import org.inspirenxe.strata.core.event.ValueEventImpl;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The core {@link RadioGroup}. It holds its buttons and their values side by side and tracks the selected button;
 * the buttons ask it whether they are selected. Its handlers live in a dispatcher of its own, created on the first
 * registration, and the group's {@code SELECTED} reaches only them. After each new button and each change of the
 * selection it sets which buttons are tabbable: the selected one, or the first while none is selected. A button
 * taken out with {@link #remove} is no longer in its lists, so {@link #choose} ignores it.
 *
 * @param <T> the type of the values the buttons stand for
 */
public final class RadioGroupImpl<T> implements RadioGroup<T> {
    private final ComponentFactoryImpl ui;
    private final List<RadioButtonImpl> buttons;
    private final List<RadioButton> view;
    private final List<T> values;
    private EventDispatcher dispatcher;
    private RadioButtonImpl selected;
    // The button a choice in progress selected but has not reported yet
    private RadioButtonImpl arriving;

    /**
     * Creates an empty group.
     *
     * @param ui the factory that makes the group's buttons
     */
    public RadioGroupImpl(final ComponentFactoryImpl ui) {
        this.ui = Objects.requireNonNull(ui, "ui");
        this.buttons = new ArrayList<>();
        this.view = Collections.unmodifiableList(this.buttons);
        this.values = new ArrayList<>();
    }

    @Override
    public RadioButton add(final Text text, final T value) {
        final RadioButtonImpl button = new RadioButtonImpl(this.ui, this, text);
        this.buttons.add(button);
        this.values.add(value);
        this.updateTabStops();
        return button;
    }

    /**
     * Takes a button out. The selected button is deselected first, through {@link #choose}, so it reports
     * {@code TOGGLED} and the group {@code SELECTED} with -1. A button before the selected one moves the selected
     * index down, which the group reports with the selected button as the target.
     */
    @Override
    public RadioGroup<T> remove(final RadioButton button) {
        if (!(button instanceof final RadioButtonImpl impl) || !this.buttons.contains(impl)) {
            return this;
        }
        if (impl == this.selected) {
            this.choose(null);
        }
        final int before = this.buttons.indexOf(this.selected);
        final int index = this.buttons.indexOf(impl);
        this.buttons.remove(index);
        this.values.remove(index);
        this.updateTabStops();
        if (this.selected != null && index < before) {
            this.fireSelected(this.selected, before - 1, before);
        }
        return this;
    }

    /**
     * Takes every button out: the selected one is deselected first, through {@link #choose}, and then the lists are
     * emptied.
     */
    @Override
    public RadioGroup<T> clear() {
        if (this.selected != null) {
            this.choose(null);
        }
        this.buttons.clear();
        this.values.clear();
        return this;
    }

    boolean contains(final RadioButtonImpl button) {
        return this.buttons.contains(button);
    }

    /**
     * Makes the selected button the group's only tab stop, or the first enabled button while none is selected, so
     * Tab enters and leaves the group in one step and the arrows move inside it. A button whose tabbable flag is not
     * what the group last set, because code called {@code setTabbable} on it, keeps its own setting from then on.
     */
    void updateTabStops() {
        RadioButtonImpl stop = this.selected;
        for (int i = 0; stop == null && i < this.buttons.size(); i++) {
            if (this.buttons.get(i).isEnabled()) {
                stop = this.buttons.get(i);
            }
        }
        for (final RadioButtonImpl button : this.buttons) {
            button.groupTabStop(button == stop);
        }
    }

    @Override
    public List<RadioButton> buttons() {
        return this.view;
    }

    @Override
    public T value(final int index) {
        return this.values.get(index);
    }

    @Override
    public Optional<RadioButton> selectedButton() {
        return Optional.ofNullable(this.selected);
    }

    @Override
    public Optional<T> selected() {
        return this.selected == null ? Optional.empty() : Optional.ofNullable(this.values.get(this.buttons.indexOf(this.selected)));
    }

    @Override
    public int selectedIndex() {
        return this.buttons.indexOf(this.selected);
    }

    @Override
    public RadioGroup<T> select(final int index) {
        if (index != -1) {
            Objects.checkIndex(index, this.buttons.size());
        }
        this.choose(index == -1 ? null : this.buttons.get(index));
        return this;
    }

    @Override
    public RadioGroup<T> selectItem(final T value) {
        return this.select(this.values.indexOf(value));
    }

    @Override
    public <E extends Event> RadioGroup<T> onEvent(final EventType<E> type, final int priority,
                                                   final EventHandler<? super E> handler) {
        this.subscribe(type, priority, handler);
        return this;
    }

    @Override
    public <E extends Event> Subscription subscribe(final EventType<E> type, final int priority,
                                                    final EventHandler<? super E> handler) {
        if (this.dispatcher == null) {
            this.dispatcher = new EventDispatcher();
        }
        return this.dispatcher.subscribe(type, priority, handler);
    }

    boolean isSelected(final RadioButtonImpl button) {
        return this.selected == button;
    }

    /**
     * Makes a button the selected one, or none. If that changed anything, the deselected and the selected button
     * report {@code TOGGLED}, in that order, and the group fires {@code SELECTED} to its own handlers. That event's
     * target is the newly selected button, or the deselected one when none is selected now. A handler of either
     * report that selects another button makes that change report itself, and this one reports nothing more, so the
     * last events always describe the selection the group has.
     */
    void choose(final RadioButtonImpl button) {
        final RadioButtonImpl previous = this.selected;
        if (previous == button || button != null && !this.buttons.contains(button)) {
            return;
        }
        // A button selected by a choice that has not reported yet has no deselect to report
        final RadioButtonImpl told = previous == this.arriving ? null : previous;
        this.selected = button;
        this.arriving = button;
        this.updateTabStops();
        if (told != null) {
            // The group deselects the old button, so it is on that report's cause chain
            try (CauseFrame _ = this.ui.causes().pushFrame().push(this)) {
                told.reportToggled(false);
            }
            if (this.selected != button) {
                return;
            }
        }
        this.arriving = null;
        if (button != null) {
            button.reportToggled(true);
            if (this.selected != button) {
                return;
            }
        }
        this.fireSelected(button != null ? button : told != null ? told : previous, this.buttons.indexOf(button),
                this.buttons.indexOf(told));
    }

    /**
     * Fires {@code SELECTED} to the group's own handlers, if it has any.
     */
    private void fireSelected(final RadioButtonImpl target, final int index, final int previous) {
        if (this.dispatcher != null && this.dispatcher.hasHandlers(SELECTED)) {
            final ValueEventImpl<Selection<?, ?>, Integer> event = new ValueEventImpl<>(target, this, index, previous);
            event.setCauses(this.ui.causes());
            this.dispatcher.fire(SELECTED, event);
        }
    }

    /**
     * Selects and focuses the next usable button after {@code from} in a direction for the user, wrapping at the
     * ends. A handler may cancel the target button's {@code TOGGLE}, which leaves the selection and focus as they
     * are.
     *
     * @return true if one was found
     */
    boolean moveFrom(final RadioButtonImpl from, final int direction) {
        final int count = this.buttons.size();
        final int start = this.buttons.indexOf(from);
        if (start < 0) {
            return false;
        }
        for (int step = 1; step < count; step++) {
            final RadioButtonImpl next = this.buttons.get(Math.floorMod(start + step * direction, count));
            if (next.isAttached() && next.isVisible() && next.isEnabled()) {
                next.userSelect(true);
                return true;
            }
        }
        return false;
    }
}
