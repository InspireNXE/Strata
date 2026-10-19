package org.inspirenxe.strata.core.control.pane;

import org.inspirenxe.strata.api.component.Tooltip;
import org.inspirenxe.strata.api.control.pane.Tab;

import java.util.Objects;
import java.util.Optional;

/**
 * The core {@link Tab}: enabled, with no tooltip. A bar that holds it draws it through a private component of its
 * own and is told of every change.
 */
public final class TabImpl implements Tab {
    private String text;
    private boolean enabled;
    private Tooltip tooltip;
    private TabBarImpl bar;

    /**
     * Creates a tab.
     *
     * @param text the tab's text
     */
    public TabImpl(final String text) {
        this.text = Objects.requireNonNull(text, "text");
        this.enabled = true;
    }

    @Override
    public String text() {
        return this.text;
    }

    @Override
    public Tab setText(final String text) {
        this.text = Objects.requireNonNull(text, "text");
        this.changed();
        return this;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    @Override
    public Tab setEnabled(final boolean enabled) {
        this.enabled = enabled;
        this.changed();
        return this;
    }

    @Override
    public Optional<Tooltip> tooltip() {
        return Optional.ofNullable(this.tooltip);
    }

    @Override
    public Tab setTooltip(final Tooltip tooltip) {
        this.tooltip = tooltip;
        this.changed();
        return this;
    }

    TabBarImpl bar() {
        return this.bar;
    }

    /**
     * Gives the tab to a bar, or takes it back with null.
     *
     * @throws IllegalArgumentException if the tab is given while a bar already has it
     */
    void setBar(final TabBarImpl bar) {
        if (bar != null && this.bar != null) {
            throw new IllegalArgumentException(this + " is already in a tab bar");
        }
        this.bar = bar;
    }

    private void changed() {
        if (this.bar != null) {
            this.bar.tabChanged(this);
        }
    }

    @Override
    public String toString() {
        return "Tab[" + this.text + "]";
    }
}
