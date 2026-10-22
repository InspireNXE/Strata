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
package org.inspirenxe.strata.core.node;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.extension.ArrangeContext;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.text.TextMetrics;

/**
 * The context passed to measure and arrange hooks. Each host has one and reuses it for every layout.
 */
public final class LayoutPass implements MeasureContext, ArrangeContext {
    private final ComponentFactoryImpl ui;
    private final TextMetrics text;

    /**
     * Creates the context for a host.
     *
     * @param ui the host's factory, which maps components to their nodes
     * @param text the platform's text metrics
     */
    public LayoutPass(final ComponentFactoryImpl ui, final TextMetrics text) {
        this.ui = ui;
        this.text = text;
    }

    @Override
    public TextMetrics text() {
        return this.text;
    }

    @Override
    public Size measure(final Component<?> child, final float availableWidth, final float availableHeight) {
        final Node node = this.ui.nodeOf(child);
        node.measure(this, availableWidth, availableHeight);
        return new Size(node.outerWidth(), node.outerHeight());
    }

    @Override
    public Size outerSize(final Component<?> child) {
        final Node node = this.ui.nodeOf(child);
        return new Size(node.outerWidth(), node.outerHeight());
    }

    @Override
    public Size minimumSize(final Component<?> child) {
        return this.ui.nodeOf(child).minimumSize(this);
    }

    @Override
    public void arrange(final Component<?> child, final Rect slot) {
        this.ui.nodeOf(child).arrange(this, slot);
    }
}
