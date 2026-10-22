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
import org.inspirenxe.strata.api.event.Event;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.extension.ArrangeContext;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.extension.peer.ComponentHooks;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.input.TextInputKind;
import org.inspirenxe.strata.api.theme.Theme;

import java.util.Objects;
import java.util.Set;

/**
 * The hooks of a developer's {@code CustomComponent}, as a node calls them. Each call forwards to the public
 * {@link ComponentHooks} the component gave its peer, except {@link #disabled()}, {@link #released()} and
 * {@link #pressedWithin}, which only Strata's own controls answer and which do nothing here.
 */
final class ExtensionHooks implements NodeHooks {
    private final ComponentHooks hooks;

    ExtensionHooks(final ComponentHooks hooks) {
        this.hooks = Objects.requireNonNull(hooks, "hooks");
    }

    @Override
    public Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        return this.hooks.measure(context, availableWidth, availableHeight);
    }

    @Override
    public Size measureMinimum(final MeasureContext context) {
        return this.hooks.measureMinimum(context);
    }

    @Override
    public void arrange(final ArrangeContext context, final Rect bounds) {
        this.hooks.arrange(context, bounds);
    }

    @Override
    public void paint(final Painter painter, final Rect bounds) {
        this.hooks.paint(painter, bounds);
    }

    @Override
    public Set<String> requiredStyleKeys() {
        return this.hooks.requiredStyleKeys();
    }

    @Override
    public void applyTheme(final Theme theme) {
        this.hooks.applyTheme(theme);
    }

    @Override
    public boolean acceptsTextInput() {
        return this.hooks.acceptsTextInput();
    }

    @Override
    public TextInputKind textInputKind() {
        return this.hooks.textInputKind();
    }

    @Override
    public void handle(final EventType<?> type, final Event event) {
        this.hooks.handle(type, event);
    }

    @Override
    public Object snapshotState() {
        return this.hooks.snapshotState();
    }

    @Override
    public void childRemoved(final Component<?> child) {
        this.hooks.childRemoved(child);
    }

    @Override
    public Rect childClip() {
        return this.hooks.childClip();
    }

    @Override
    public void paintOverChildren(final Painter painter, final Rect bounds) {
        this.hooks.paintOverChildren(painter, bounds);
    }

    @Override
    public void descendantFocused(final Component<?> descendant) {
        this.hooks.descendantFocused(descendant);
    }

    @Override
    public void disabled() {
    }

    @Override
    public void released() {
    }

    @Override
    public void pressedWithin(final Component<?> target) {
    }
}
