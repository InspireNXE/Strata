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
import org.inspirenxe.strata.api.component.Container;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.theme.Border;
import org.inspirenxe.strata.api.theme.Paint;

import java.util.List;

/**
 * Base class of Strata's own panels and of controls whose children callers add and remove, the counterpart of the
 * public {@link org.inspirenxe.strata.api.extension.CustomContainer}. It is a {@link BuiltInComposite} whose children
 * are public, so control events from them pass it as they pass any container. A subclass includes the
 * {@linkplain #padding() padding} itself and places children inside {@link #contentBox()}.
 *
 * @param <T> the type callers see the container as
 */
public abstract class BuiltInContainer<T extends Container<T>> extends BuiltInComposite<T> implements Container<T> {

    /**
     * Creates the container and its node.
     *
     * @param factory the factory of the host that will show this container
     */
    protected BuiltInContainer(final ComponentFactoryImpl factory) {
        super(factory, false);
    }

    /**
     * Returns the area children are laid out in: the bounds minus the padding. Valid during {@link #arrange} and
     * after layout.
     *
     * @return the content box
     */
    protected final Rect contentBox() {
        return this.bounds().shrink(this.padding());
    }

    // ------------------------------------------------------------------
    // Container, forwarded to the node. Documented on Container.
    // ------------------------------------------------------------------

    @Override
    public final List<Component<?>> children() {
        return this.node().children();
    }

    @Override
    public final T add(final Component<?> child) {
        this.node().add(child);
        return this.self();
    }

    @Override
    public final T insert(final int index, final Component<?> child) {
        this.node().insert(index, child);
        return this.self();
    }

    @Override
    public final T move(final Component<?> child, final int index) {
        this.node().move(child, index);
        return this.self();
    }

    @Override
    public final int indexOf(final Component<?> child) {
        return this.node().indexOf(child);
    }

    @Override
    public final T raise(final Component<?> child) {
        this.node().raise(child);
        return this.self();
    }

    @Override
    public final T lower(final Component<?> child) {
        this.node().lower(child);
        return this.self();
    }

    @Override
    public final List<Component<?>> paintOrder() {
        return this.node().paintOrder();
    }

    @Override
    public final T remove(final Component<?> child) {
        this.node().remove(child);
        return this.self();
    }

    @Override
    public final T clear() {
        this.node().clear();
        return this.self();
    }

    @Override
    public final Insets padding() {
        return this.node().padding();
    }

    @Override
    public final T setPadding(final Insets padding) {
        this.node().setPadding(padding);
        return this.self();
    }

    @Override
    public final boolean clipsChildren() {
        return this.node().clipsChildren();
    }

    @Override
    public final T setClipChildren(final boolean clipChildren) {
        this.node().setClipChildren(clipChildren);
        return this.self();
    }

    @Override
    public final Paint background() {
        return super.background();
    }

    @Override
    public final T setBackground(final Paint background) {
        return super.setBackground(background);
    }

    @Override
    public final Border border() {
        return super.border();
    }

    @Override
    public final T setBorder(final Border border) {
        return super.setBorder(border);
    }
}
