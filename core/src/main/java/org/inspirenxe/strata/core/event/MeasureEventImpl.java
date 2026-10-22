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
import org.inspirenxe.strata.api.event.component.MeasureEvent;
import org.inspirenxe.strata.api.extension.MeasureContext;

/**
 * The {@link Component#MEASURED} event of one component. The component keeps one and resets it before each firing.
 */
public final class MeasureEventImpl extends EventImpl implements MeasureEvent {
    private MeasureContext context;
    private float availableWidth;
    private float availableHeight;
    private float width;
    private float height;

    /**
     * Creates the measure event of a component.
     *
     * @param target the component
     */
    public MeasureEventImpl(final Component<?> target) {
        super(target);
    }

    /**
     * Readies the event for one firing.
     *
     * @param context the measure context of the pass
     * @param availableWidth the width offered for the content
     * @param availableHeight the height offered for the content
     * @param width the content width the component asked for
     * @param height the content height the component asked for
     * @return this event
     */
    public MeasureEventImpl begin(final MeasureContext context, final float availableWidth,
                                  final float availableHeight, final float width, final float height) {
        this.reset();
        this.context = context;
        this.availableWidth = availableWidth;
        this.availableHeight = availableHeight;
        this.width = width;
        this.height = height;
        return this;
    }

    @Override
    public MeasureContext context() {
        return this.context;
    }

    @Override
    public float availableWidth() {
        return this.availableWidth;
    }

    @Override
    public float availableHeight() {
        return this.availableHeight;
    }

    @Override
    public float width() {
        return this.width;
    }

    @Override
    public float height() {
        return this.height;
    }

    @Override
    public void setWidth(final float width) {
        this.width = Math.max(0f, finite(width, "width"));
    }

    @Override
    public void setHeight(final float height) {
        this.height = Math.max(0f, finite(height, "height"));
    }

    private static float finite(final float length, final String name) {
        if (!Float.isFinite(length)) {
            throw new IllegalArgumentException("A measured " + name + " must be finite: " + length);
        }
        return length;
    }
}
