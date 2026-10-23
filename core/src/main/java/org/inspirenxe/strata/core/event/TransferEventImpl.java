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
package org.inspirenxe.strata.core.event;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.event.input.TransferEvent;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.transfer.DropEffect;
import org.inspirenxe.strata.api.transfer.Transfer;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * The core {@link TransferEvent}. The root's drag session makes one for each enter, move, exit, drop and end of a
 * drag that carries a transfer. In a move, {@link #accept} records the current component as the one that would take
 * the drop and consumes the event; the session reads it with {@link #acceptor()}.
 */
public final class TransferEventImpl extends PointerEventImpl implements TransferEvent {
    /**
     * Which of the five events this is, which decides what {@link #accept} and {@link #reject} do.
     */
    public enum Kind {
        /** {@code DRAG_ENTER}, {@code DRAG_EXIT} or {@code TRANSFER_DONE}: the effect cannot change. */
        NOTICE,
        /** {@code DRAG_OVER}: a component may accept. */
        OVER,
        /** {@code DROP}: the effect may change. */
        DROP
    }

    private final Transfer transfer;
    private final Set<DropEffect> allowed;
    private final Component<?> source;
    private final Kind kind;
    private DropEffect effect;
    private Component<?> acceptor;

    /**
     * Creates the event.
     *
     * @param target the component the event is fired at
     * @param x the pointer's x, in the host's coordinates
     * @param y the pointer's y, in the host's coordinates
     * @param modifiers the modifiers held
     * @param transfer what is being dragged
     * @param allowed the effects the source allows, never empty
     * @param source the source, or {@code null} for a drag from another application
     * @param kind which event this is
     * @param effect the effect so far, or {@code null} for none
     */
    public TransferEventImpl(final Component<?> target, final float x, final float y, final Modifiers modifiers,
                             final Transfer transfer, final Set<DropEffect> allowed, final Component<?> source,
                             final Kind kind, final DropEffect effect) {
        super(target, x, y, modifiers);
        this.transfer = Objects.requireNonNull(transfer, "transfer");
        this.allowed = Objects.requireNonNull(allowed, "allowed");
        this.source = source;
        this.kind = Objects.requireNonNull(kind, "kind");
        this.effect = effect;
    }

    /**
     * Returns the effect the modifiers ask for among the allowed ones, as {@link TransferEvent#preferred()}
     * describes.
     *
     * @param allowed the allowed effects, never empty
     * @param modifiers the modifiers held
     * @return the preferred effect
     */
    public static DropEffect preferred(final Set<DropEffect> allowed, final Modifiers modifiers) {
        if (modifiers.control() && modifiers.shift() && allowed.contains(DropEffect.LINK)) {
            return DropEffect.LINK;
        }
        if (modifiers.control() && !modifiers.shift() && allowed.contains(DropEffect.COPY)) {
            return DropEffect.COPY;
        }
        if (allowed.contains(DropEffect.MOVE)) {
            return DropEffect.MOVE;
        }
        return allowed.contains(DropEffect.COPY) ? DropEffect.COPY : DropEffect.LINK;
    }

    @Override
    public Transfer transfer() {
        return this.transfer;
    }

    @Override
    public Set<DropEffect> allowed() {
        return this.allowed;
    }

    @Override
    public Optional<Component<?>> source() {
        return Optional.ofNullable(this.source);
    }

    @Override
    public DropEffect preferred() {
        return TransferEventImpl.preferred(this.allowed, this.modifiers());
    }

    @Override
    public Optional<DropEffect> effect() {
        return Optional.ofNullable(this.effect);
    }

    @Override
    public void accept(final DropEffect effect) {
        Objects.requireNonNull(effect, "effect");
        this.checkCanChange();
        if (!this.allowed.contains(effect)) {
            throw new IllegalArgumentException("The source does not allow " + effect + ", only " + this.allowed);
        }
        this.effect = effect;
        if (this.kind == Kind.OVER) {
            this.acceptor = this.current();
            this.consume();
        }
    }

    @Override
    public void reject() {
        this.checkCanChange();
        this.effect = null;
        if (this.kind == Kind.OVER) {
            this.acceptor = null;
            this.consume();
        }
    }

    private void checkCanChange() {
        if (this.kind == Kind.NOTICE) {
            throw new IllegalStateException("Only a drag over or a drop can be accepted or rejected");
        }
    }

    /**
     * Returns the component that accepted a move.
     *
     * @return the component current when {@link #accept} was called, or {@code null} if none accepted
     */
    public Component<?> acceptor() {
        return this.acceptor;
    }
}
