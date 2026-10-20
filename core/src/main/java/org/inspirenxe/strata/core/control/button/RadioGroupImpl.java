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

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.control.button.RadioButton;
import org.inspirenxe.strata.api.control.button.RadioGroup;
import org.inspirenxe.strata.api.event.Event;
import org.inspirenxe.strata.api.event.EventHandler;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.Subscription;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.core.event.EventDispatcher;
import org.inspirenxe.strata.core.event.ValueEventImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The core {@link RadioGroup}. It holds its buttons and their values side by side and tracks the selected button;
 * the buttons ask it whether they are selected. Its handlers live in a dispatcher of its own, created on the first
 * registration, and the group's {@code SELECTED} reaches only them.
 *
 * @param <T> the type of the values the buttons stand for
 */
public final class RadioGroupImpl<T> implements RadioGroup<T> {
    private final ComponentFactory ui;
    private final List<RadioButtonImpl> buttons;
    private final List<RadioButton> view;
    private final List<T> values;
    private EventDispatcher dispatcher;
    private RadioButtonImpl selected;

    /**
     * Creates an empty group.
     *
     * @param ui the factory that makes the group's buttons
     */
    public RadioGroupImpl(final ComponentFactory ui) {
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
        return button;
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
     * target is the newly selected button, or the deselected one when none is selected now.
     */
    void choose(final RadioButtonImpl button) {
        final RadioButtonImpl previous = this.selected;
        if (previous == button) {
            return;
        }
        this.selected = button;
        if (previous != null) {
            previous.reportToggled(false);
        }
        if (button != null) {
            button.reportToggled(true);
        }
        if (this.dispatcher != null && this.dispatcher.hasHandlers(SELECTED)) {
            this.dispatcher.fire(SELECTED, new ValueEventImpl<>(button == null ? previous : button, this,
                    this.buttons.indexOf(button), this.buttons.indexOf(previous)));
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
