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

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.Tooltip;
import org.inspirenxe.strata.api.control.pane.NavigationEntry;
import org.inspirenxe.strata.api.control.pane.NavigationItem;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.visual.Visual;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * The core {@link NavigationItem}: enabled, selectable, collapsed, with no icon, badge, page or children, and an
 * icon size of 16 by 16. It keeps
 * the page its view built for it until the page is discarded.
 */
public final class NavigationItemImpl extends NavigationEntryImpl implements NavigationItem {
    private Text text;
    private SpriteId iconSprite;
    private Visual iconVisual;
    private float iconWidth;
    private float iconHeight;
    private boolean enabled;
    private Tooltip tooltip;
    private boolean selectable;
    private int badge;
    private Function<ComponentFactory, ? extends Component<?>> page;
    private Component<?> keptPage;
    private final List<NavigationItemImpl> children;
    private boolean expanded;

    /**
     * Creates an item.
     *
     * @param text the item's text
     */
    public NavigationItemImpl(final Text text) {
        this.text = Objects.requireNonNull(text, "text");
        this.iconWidth = 16f;
        this.iconHeight = 16f;
        this.enabled = true;
        this.selectable = true;
        this.children = new ArrayList<>();
    }

    @Override
    public Text text() {
        return this.text;
    }

    @Override
    public NavigationItem setText(final Text text) {
        this.text = Objects.requireNonNull(text, "text");
        this.changed();
        return this;
    }

    @Override
    public Optional<SpriteId> iconSprite() {
        return Optional.ofNullable(this.iconSprite);
    }

    @Override
    public Optional<Visual> iconVisual() {
        return Optional.ofNullable(this.iconVisual);
    }

    @Override
    public NavigationItem setIcon(final SpriteId sprite) {
        this.iconSprite = Objects.requireNonNull(sprite, "sprite");
        this.iconVisual = null;
        this.changed();
        return this;
    }

    @Override
    public NavigationItem setIcon(final Visual visual) {
        this.iconVisual = Objects.requireNonNull(visual, "visual");
        this.iconSprite = null;
        this.iconWidth = visual.size().width();
        this.iconHeight = visual.size().height();
        this.changed();
        return this;
    }

    @Override
    public NavigationItem clearIcon() {
        this.iconSprite = null;
        this.iconVisual = null;
        this.changed();
        return this;
    }

    @Override
    public float iconWidth() {
        return this.iconWidth;
    }

    @Override
    public float iconHeight() {
        return this.iconHeight;
    }

    @Override
    public NavigationItem setIconSize(final float width, final float height) {
        if (!(width >= 0f) || !(height >= 0f) || Float.isInfinite(width) || Float.isInfinite(height)) {
            throw new IllegalArgumentException("Icon size must be finite and not negative: " + width + " x " + height);
        }
        this.iconWidth = width;
        this.iconHeight = height;
        this.changed();
        return this;
    }

    /**
     * Returns the sprite drawn as the icon, or null.
     */
    SpriteId sprite() {
        return this.iconSprite;
    }

    /**
     * Returns the visual drawn as the icon, or null.
     */
    Visual visual() {
        return this.iconVisual;
    }

    /**
     * Returns whether the item has an icon.
     */
    boolean hasIcon() {
        return this.iconSprite != null || this.iconVisual != null;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    @Override
    public NavigationItem setEnabled(final boolean enabled) {
        this.enabled = enabled;
        this.changed();
        return this;
    }

    @Override
    public Optional<Tooltip> tooltip() {
        return Optional.ofNullable(this.tooltip);
    }

    @Override
    public NavigationItem setTooltip(final Tooltip tooltip) {
        this.tooltip = tooltip;
        this.changed();
        return this;
    }

    @Override
    public boolean isSelectable() {
        return this.selectable;
    }

    @Override
    public NavigationItem setSelectable(final boolean selectable) {
        this.selectable = selectable;
        this.changed();
        return this;
    }

    @Override
    public int badge() {
        return this.badge;
    }

    @Override
    public NavigationItem setBadge(final int count) {
        if (count < 0) {
            throw new IllegalArgumentException("A badge count cannot be negative: " + count);
        }
        this.badge = count;
        this.changed();
        return this;
    }

    @Override
    public NavigationItem setPage(final Function<ComponentFactory, ? extends Component<?>> page) {
        this.page = page;
        final NavigationViewImpl view = this.view();
        if (view != null) {
            view.pageChanged(this);
        } else {
            this.keptPage = null;
        }
        return this;
    }

    /**
     * Returns how the page is built.
     */
    Function<ComponentFactory, ? extends Component<?>> page() {
        return this.page;
    }

    /**
     * Returns the page the view built and keeps for this item.
     */
    Component<?> keptPage() {
        return this.keptPage;
    }

    void setKeptPage(final Component<?> page) {
        this.keptPage = page;
    }

    @Override
    public List<NavigationItem> items() {
        return List.copyOf(this.children);
    }

    /**
     * Returns the children, live, for the view.
     */
    List<NavigationItemImpl> children() {
        return this.children;
    }

    @Override
    public NavigationItem addItem(final NavigationItem child) {
        if (!(child instanceof final NavigationItemImpl impl)) {
            throw new IllegalArgumentException(child + " is not a Strata navigation item");
        }
        for (Object at = this; at instanceof final NavigationItemImpl item; at = item.owner()) {
            if (item == impl) {
                throw new IllegalArgumentException("Adding " + child + " to " + this + " would create a cycle");
            }
        }
        impl.setOwner(this);
        this.children.add(impl);
        final NavigationViewImpl view = this.view();
        if (view != null) {
            view.structureChanged();
        }
        return this;
    }

    @Override
    public NavigationItem removeItem(final NavigationItem child) {
        if (!(child instanceof final NavigationItemImpl impl) || !this.children.remove(impl)) {
            return this;
        }
        final NavigationViewImpl view = this.view();
        impl.setOwner(null);
        if (view != null) {
            view.entryRemoved(impl);
        } else {
            impl.discardPages();
        }
        return this;
    }

    /**
     * Drops the pages kept for this item and its children, for an item that left its view.
     */
    void discardPages() {
        this.keptPage = null;
        for (final NavigationItemImpl child : this.children) {
            child.discardPages();
        }
    }

    @Override
    public boolean isExpanded() {
        return this.expanded;
    }

    @Override
    public NavigationItem setExpanded(final boolean expanded) {
        if (this.expanded != expanded) {
            this.expanded = expanded;
            final NavigationViewImpl view = this.view();
            if (view != null) {
                view.itemExpanded(this, expanded);
            }
        }
        return this;
    }

    @Override
    NavigationEntry asEntry() {
        return this;
    }

    @Override
    public String toString() {
        return "NavigationItem[" + this.text.plain() + "]";
    }
}
