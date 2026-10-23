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
import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.cause.CauseFrame;
import org.inspirenxe.strata.api.event.input.InputEvents;
import org.inspirenxe.strata.api.event.input.TransferEvent;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.transfer.DropEffect;
import org.inspirenxe.strata.api.transfer.Transfer;
import org.inspirenxe.strata.core.event.DragEventImpl;
import org.inspirenxe.strata.core.event.TransferEventImpl;

import java.util.Objects;
import java.util.Set;

/**
 * One drag that carries a transfer, from a drag start handler in this host or from another application, as a
 * {@link Root} runs it. It tracks the component under the pointer, which hears {@code DRAG_ENTER} and
 * {@code DRAG_EXIT}, and the one that accepted the last {@code DRAG_OVER}, which the release drops on. Every event
 * fires in a frame with the source pushed, inside the input frame the root opened for the pointer event.
 */
final class TransferDrag {
    private final Root root;
    private final Node source;
    private final Set<DropEffect> allowed;
    private Transfer transfer;
    private Node image;
    private Node over;
    private Node acceptor;
    private DropEffect effect;
    private float x;
    private float y;
    private Modifiers modifiers = Modifiers.NONE;
    private boolean ended;
    private boolean abandoned;

    private TransferDrag(final Root root, final Node source, final Transfer transfer, final Set<DropEffect> allowed) {
        this.root = root;
        this.source = source;
        this.transfer = transfer;
        this.allowed = allowed;
    }

    /**
     * Starts a drag a drag start handler asked for, building its image.
     *
     * @param root the root
     * @param source the source's node
     * @param request what the handler asked for
     * @return the drag
     * @throws IllegalArgumentException if the image builder returns a component already in a tree
     */
    static TransferDrag internal(final Root root, final Node source, final DragEventImpl.TransferRequest request) {
        final TransferDrag drag = new TransferDrag(root, source, request.transfer(), request.allowed());
        if (request.image() != null) {
            final Node image = root.factory().nodeOf(request.image().apply(root.factory()));
            if (image.parentNode() != null || image.root() != null) {
                throw new IllegalArgumentException("A drag image builder must return a new component, not one already in a tree");
            }
            drag.image = image;
            root.attachTree(image);
        }
        return drag;
    }

    /**
     * Starts a drag from another application.
     *
     * @param root the root
     * @param transfer what the drag carries
     * @param allowed the effects the other application allows, never empty
     * @return the drag
     */
    static TransferDrag external(final Root root, final Transfer transfer, final Set<DropEffect> allowed) {
        return new TransferDrag(root, null, Objects.requireNonNull(transfer, "transfer"), allowed);
    }

    /**
     * Returns whether the drag started in this host.
     */
    boolean isInternal() {
        return this.source != null;
    }

    /**
     * Returns the drag image's node, or null for none.
     */
    Node image() {
        return this.image;
    }

    float x() {
        return this.x;
    }

    float y() {
        return this.y;
    }

    /**
     * Returns the effect the component under the pointer accepted on the last move, or null.
     */
    DropEffect effect() {
        return this.effect;
    }

    /**
     * Returns the cursor for the effect accepted: the copy cursor for a copy, the closed hand for a move or a link,
     * and the no-drop sign while nothing accepts.
     */
    CursorKind cursor() {
        if (this.effect == null) {
            return CursorKind.NO_DROP;
        }
        return this.effect == DropEffect.COPY ? CursorKind.COPY : CursorKind.GRABBING;
    }

    /**
     * Replaces what a drag from another application carries, for its drop, whose data can be read.
     */
    void setTransfer(final Transfer transfer) {
        this.transfer = Objects.requireNonNull(transfer, "transfer");
    }

    /**
     * Handles a move: tells the component under the pointer, or its nearest usable ancestor, that the drag entered,
     * the one before it that the drag left, and then fires {@code DRAG_OVER} from it up until one accepts.
     */
    void moved(final float x, final float y, final Modifiers modifiers) {
        this.x = x;
        this.y = y;
        this.modifiers = modifiers;
        this.root.layout();
        final Node hit = Root.usableAncestor(this.root.hitTest(x, y));
        if (hit != this.over) {
            this.exit();
            this.over = hit;
            if (hit != null) {
                this.fire(hit, InputEvents.DRAG_ENTER, TransferEventImpl.Kind.NOTICE, null);
            }
        }
        this.acceptor = null;
        this.effect = null;
        final Node target = this.over;
        if (target != null && target.root() == this.root) {
            final TransferEventImpl event = this.fire(target, InputEvents.DRAG_OVER, TransferEventImpl.Kind.OVER, null);
            if (event.acceptor() != null && event.effect().isPresent()) {
                this.acceptor = this.root.factory().nodeOf(event.acceptor());
                this.effect = event.effect().get();
            }
        }
        if (this.image != null) {
            this.root.requestLayout();
        }
    }

    /**
     * Handles the pointer leaving the host: the component under it hears that the drag left, and nothing accepts.
     */
    void left() {
        this.exit();
        this.acceptor = null;
        this.effect = null;
    }

    /**
     * Drops the transfer: moves first when the pointer moved since the last move or {@code refresh} asks for it,
     * fires {@code DROP} at the component that accepted, then ends the drag.
     *
     * @return the effect the target took, or null for none
     */
    DropEffect release(final float x, final float y, final Modifiers modifiers, final boolean refresh) {
        if (refresh || x != this.x || y != this.y || !modifiers.equals(this.modifiers)) {
            this.moved(x, y, modifiers);
        }
        DropEffect taken = null;
        final Node target = this.acceptor;
        if (target != null && this.effect != null && target.root() == this.root) {
            final TransferEventImpl event = this.fire(target, InputEvents.DROP, TransferEventImpl.Kind.DROP, this.effect);
            taken = event.effect().orElse(null);
        }
        this.finish(taken);
        return taken;
    }

    /**
     * Ends the drag with no effect, as Escape does.
     */
    void cancel() {
        this.finish(null);
    }

    /**
     * Marks the drag for ending without events, because its source is leaving the tree. The root ends it with
     * {@link #endAbandoned()} once the tree is detached.
     */
    void abandon() {
        this.abandoned = true;
    }

    boolean isAbandoned() {
        return this.abandoned && !this.ended;
    }

    void endAbandoned() {
        this.end();
    }

    /**
     * Forgets a node that left the tree.
     */
    void detached(final Node node) {
        if (node == this.over) {
            this.over = null;
        }
        if (node == this.acceptor) {
            this.acceptor = null;
            this.effect = null;
        }
    }

    private void finish(final DropEffect taken) {
        if (this.ended) {
            return;
        }
        try {
            this.exit();
            if (this.source != null && this.source.root() == this.root) {
                this.fire(this.source, InputEvents.TRANSFER_DONE, TransferEventImpl.Kind.NOTICE, taken);
            }
        } finally {
            this.end();
        }
    }

    private void end() {
        if (this.ended) {
            return;
        }
        this.ended = true;
        this.over = null;
        this.acceptor = null;
        this.effect = null;
        final Node shown = this.image;
        this.image = null;
        this.root.transferEnded(this);
        if (shown != null) {
            try {
                this.root.detachTree(shown);
            } finally {
                Root.release(shown);
            }
        }
    }

    private void exit() {
        final Node previous = this.over;
        this.over = null;
        if (previous != null && previous.root() == this.root) {
            this.fire(previous, InputEvents.DRAG_EXIT, TransferEventImpl.Kind.NOTICE, null);
        }
    }

    private TransferEventImpl fire(final Node target, final EventType<TransferEvent> type,
                                   final TransferEventImpl.Kind kind, final DropEffect effect) {
        final Component<?> from = this.source == null ? null : this.source.facade();
        final TransferEventImpl event = this.root.at(new TransferEventImpl(target.facade(), this.x, this.y,
                this.modifiers, this.transfer, this.allowed, from, kind, effect));
        try (CauseFrame frame = this.root.factory().causes().pushFrame()) {
            if (from != null) {
                frame.push(from);
            }
            return target.fire(type, event);
        }
    }
}
