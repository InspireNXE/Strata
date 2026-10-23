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
package org.inspirenxe.strata.core.control.collection;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.control.attribute.item.CellEditor;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;
import org.inspirenxe.strata.core.node.Node;
import org.inspirenxe.strata.core.node.Root;

import java.util.List;
import java.util.function.Consumer;

/**
 * The one cell a collection control is editing in place. The control builds the editor through {@link #begin}, puts it
 * where the cell was, and gives a way to put the cell back. The editor's {@code commit} and {@code cancel} count only
 * for the edit they were made for, and only the first time, so an editor that calls both, or calls one after the
 * control canceled the edit, changes nothing. Putting the cell back takes focus back to the control when focus was
 * inside the editor.
 */
final class CellEditSession {
    private final ComponentFactoryImpl ui;
    private boolean active;
    private int generation;
    private Node editor;
    private Runnable restore;
    private Runnable refocus;

    /**
     * Creates a session with nothing being edited.
     *
     * @param ui the factory editors are built with
     */
    CellEditSession(final ComponentFactoryImpl ui) {
        this.ui = ui;
    }

    /**
     * Returns whether a cell is being edited.
     */
    boolean isEditing() {
        return this.active;
    }

    /**
     * Builds an editor for an item, ending any edit that is going on.
     *
     * @param editor builds the editor
     * @param item the item being edited
     * @param committed told the new item after the cell was put back, when the editor commits
     * @param restore puts the cell back in place of the editor
     * @param refocus gives the control focus again
     * @return the editor's component, for the control to put in place of the cell
     */
    <T> Component<?> begin(final CellEditor<? super T> editor, final T item, final Consumer<? super T> committed,
                           final Runnable restore, final Runnable refocus) {
        this.cancel();
        final int edit = ++this.generation;
        final Consumer<T> commit = value -> {
            if (this.active && this.generation == edit) {
                this.end();
                committed.accept(value);
            }
        };
        final Runnable cancel = () -> {
            if (this.active && this.generation == edit) {
                this.end();
            }
        };
        @SuppressWarnings("unchecked") final CellEditor<T> typed = (CellEditor<T>) editor;
        final Component<?> component = typed.edit(this.ui, item, commit, cancel);
        this.editor = this.ui.nodeOf(component);
        this.restore = restore;
        this.refocus = refocus;
        this.active = true;
        return component;
    }

    /**
     * Focuses the editor, now in the tree: its first tab stop, or the editor itself. A character that started the
     * edit goes to it next.
     *
     * @param codePoint the character that started the edit, or -1
     * @param modifiers the modifiers held with it
     */
    void focus(final int codePoint, final Modifiers modifiers) {
        final Root root = this.ui.root();
        if (!this.active || root == null) {
            return;
        }
        final List<Node> order = root.tabOrderWithin(this.editor);
        if (!(order.isEmpty() ? this.editor.requestFocus() : order.get(0).requestFocus())) {
            return;
        }
        if (codePoint >= 0) {
            root.charTyped(codePoint, modifiers);
        }
    }

    /**
     * Ends the edit with no change. Does nothing while no cell is being edited.
     */
    void cancel() {
        if (this.active) {
            this.end();
        }
    }

    private void end() {
        final Root root = this.ui.root();
        boolean inside = false;
        if (root != null) {
            for (Node node = root.focused(); node != null; node = node.parentNode()) {
                if (node == this.editor) {
                    inside = true;
                    break;
                }
            }
        }
        final Runnable restore = this.restore;
        final Runnable refocus = this.refocus;
        this.active = false;
        this.editor = null;
        this.restore = null;
        this.refocus = null;
        restore.run();
        if (inside) {
            refocus.run();
        }
    }
}
