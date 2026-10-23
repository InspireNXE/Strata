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
package org.inspirenxe.strata.core.control;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.Focusable;
import org.inspirenxe.strata.api.overlay.Overlay;

import java.util.Objects;

/**
 * What core's popups, the menu and the flyout, share about being shown: the overlay they are in, the component that
 * had focus before they opened, and the control that holds them. Each popup keeps one, starts it with the overlay it
 * opened, and lets it run the close however the overlay closed.
 *
 * <p>Once the overlay closes, the session forgets it, runs the popup's own cleanup, gives focus back to the
 * component that had it if that is still attached and focusable, reports the popup's {@code CLOSED}, and then tells
 * the holder, in that order.</p>
 */
public final class PopupSession {
    private final Runnable cleanup;
    private final Runnable reportClosed;
    private Overlay overlay;
    private Component<?> returnFocus;
    private CorePopup.Holder holder;

    /**
     * Creates a session for a popup that is not shown.
     *
     * @param cleanup what the popup does first once its overlay closed
     * @param reportClosed reports the popup's {@code CLOSED}
     */
    public PopupSession(final Runnable cleanup, final Runnable reportClosed) {
        this.cleanup = Objects.requireNonNull(cleanup, "cleanup");
        this.reportClosed = Objects.requireNonNull(reportClosed, "reportClosed");
    }

    /**
     * Starts the session for an overlay the popup just opened in, and listens for its close.
     *
     * @param overlay the overlay
     * @param returnFocus the component to give focus back to on close, or {@code null} for none
     */
    public void start(final Overlay overlay, final Component<?> returnFocus) {
        this.overlay = Objects.requireNonNull(overlay, "overlay");
        this.returnFocus = returnFocus;
        overlay.onEvent(Overlay.CLOSED, event -> this.closed());
    }

    /**
     * Returns the overlay the popup is shown in.
     *
     * @return the overlay, or {@code null} while the popup is not shown
     */
    public Overlay overlay() {
        return this.overlay;
    }

    /**
     * Returns whether the popup is shown.
     *
     * @return true while its overlay is open
     */
    public boolean isOpen() {
        return this.overlay != null && this.overlay.isOpen();
    }

    /**
     * Closes the popup's overlay, which ends the session. Does nothing while the popup is not shown.
     */
    public void close() {
        if (this.overlay != null) {
            this.overlay.close();
        }
    }

    /**
     * Returns the control that holds the popup.
     *
     * @return the holder, or {@code null} when none holds it
     */
    public CorePopup.Holder holder() {
        return this.holder;
    }

    /**
     * Sets the control that holds the popup. The popup checks whether it may be held first.
     *
     * @param holder the holder, or {@code null} for none
     */
    public void setHolder(final CorePopup.Holder holder) {
        this.holder = holder;
    }

    private void closed() {
        this.overlay = null;
        this.cleanup.run();
        if (this.returnFocus != null) {
            final Component<?> focus = this.returnFocus;
            this.returnFocus = null;
            if (focus.isAttached() && focus instanceof final Focusable<?> focusable) {
                focusable.requestFocus();
            }
        }
        this.reportClosed.run();
        if (this.holder != null) {
            this.holder.popupClosed();
        }
    }
}
