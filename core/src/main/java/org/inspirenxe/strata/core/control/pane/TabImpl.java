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
package org.inspirenxe.strata.core.control.pane;

import org.inspirenxe.strata.api.component.Tooltip;
import org.inspirenxe.strata.api.control.pane.Tab;
import org.inspirenxe.strata.api.text.Text;

import java.util.Objects;
import java.util.Optional;

/**
 * The core {@link Tab}: enabled, with no tooltip. A bar that holds it draws it through a private component of its
 * own and is told of every change.
 */
public final class TabImpl implements Tab {
    private Text text;
    private boolean enabled;
    private Tooltip tooltip;
    private TabBarImpl bar;

    /**
     * Creates a tab.
     *
     * @param text the tab's text
     */
    public TabImpl(final Text text) {
        this.text = Objects.requireNonNull(text, "text");
        this.enabled = true;
    }

    @Override
    public Text text() {
        return this.text;
    }

    @Override
    public Tab setText(final Text text) {
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
        return "Tab[" + this.text.plain() + "]";
    }
}
