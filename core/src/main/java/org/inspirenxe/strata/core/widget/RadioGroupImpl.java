package org.inspirenxe.strata.core.widget;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.widget.RadioButton;
import org.inspirenxe.strata.api.widget.RadioGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * The core {@link RadioGroup}. It holds its buttons and their values side by side and tracks the selected button;
 * the buttons ask it whether they are selected.
 *
 * @param <T> the type of the values the buttons stand for
 */
public final class RadioGroupImpl<T> implements RadioGroup<T> {
    private final ComponentFactory ui;
    private final List<RadioButtonImpl> buttons;
    private final List<RadioButton> view;
    private final List<T> values;
    private final List<Consumer<Optional<T>>> listeners;
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
        this.listeners = new ArrayList<>();
    }

    @Override
    public RadioButton add(final String text, final T value) {
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
    public RadioGroup<T> select(final T value) {
        final int index = this.values.indexOf(value);
        if (index < 0) {
            throw new IllegalArgumentException("No button stands for " + value);
        }
        this.choose(this.buttons.get(index));
        return this;
    }

    @Override
    public RadioGroup<T> clearSelection() {
        this.choose(null);
        return this;
    }

    @Override
    public RadioGroup<T> onSelectionChanged(final Consumer<Optional<T>> listener) {
        this.listeners.add(Objects.requireNonNull(listener, "listener"));
        return this;
    }

    boolean isSelected(final RadioButtonImpl button) {
        return this.selected == button;
    }

    /**
     * Makes a button the selected one, or none, and tells the listeners if that changed anything.
     */
    void choose(final RadioButtonImpl button) {
        if (this.selected == button) {
            return;
        }
        this.selected = button;
        final Optional<T> value = this.selected();
        for (final Consumer<Optional<T>> listener : List.copyOf(this.listeners)) {
            listener.accept(value);
        }
    }

    /**
     * Selects and focuses the next usable button after {@code from} in a direction, wrapping at the ends.
     *
     * @return true if one was found
     */
    boolean moveFrom(final RadioButtonImpl from, final int direction) {
        final int count = this.buttons.size();
        final int start = this.buttons.indexOf(from);
        for (int step = 1; step < count; step++) {
            final RadioButtonImpl next = this.buttons.get(Math.floorMod(start + step * direction, count));
            if (next.isAttached() && next.isVisible() && next.isEnabled()) {
                this.choose(next);
                next.requestFocus();
                return true;
            }
        }
        return false;
    }
}
