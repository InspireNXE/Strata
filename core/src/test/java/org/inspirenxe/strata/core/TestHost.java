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
package org.inspirenxe.strata.core;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.Themes;
import org.inspirenxe.strata.core.host.ScreenHostImpl;
import org.inspirenxe.strata.core.node.Node;

import java.util.List;

/**
 * A host for tests: build components with {@link #factory()} first, then {@link #show} the tree.
 */
public final class TestHost {
    private final TestPlatform platform;
    private final ScreenHostImpl host;

    public TestHost(final float width, final float height) {
        this(new TestPlatform(), Themes.flat(), width, height);
    }

    public TestHost(final TestPlatform platform, final Theme theme, final float width, final float height) {
        this.platform = platform;
        this.host = (ScreenHostImpl) StrataCore.createScreenHost(platform, theme);
        this.host.resize(width, height);
        // Tooltips show at once unless a test sets a delay
        this.host.setTooltipDelay(0L);
    }

    public TestPlatform platform() {
        return this.platform;
    }

    public ScreenHostImpl host() {
        return this.host;
    }

    public ComponentFactory factory() {
        return this.host.factory();
    }

    /**
     * Opens a screen whose tree is {@code content}, and lays it out.
     */
    public TestHost show(final Component<?> content) {
        this.host.open(ui -> content);
        this.host.layout();
        return this;
    }

    public void click(final float x, final float y) {
        this.host.pointerButton(x, y, MouseButton.LEFT, true, Modifiers.NONE);
        this.host.pointerButton(x, y, MouseButton.LEFT, false, Modifiers.NONE);
    }

    public Component<?> focused() {
        return this.host.focused().orElse(null);
    }

    public List<Component<?>> tabOrder() {
        return this.host.root().tabOrder().stream().<Component<?>>map(Node::facade).toList();
    }
}
