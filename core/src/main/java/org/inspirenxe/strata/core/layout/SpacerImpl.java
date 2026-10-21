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
package org.inspirenxe.strata.core.layout;

import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.Spacer;
import org.inspirenxe.strata.core.node.BuiltInComponent;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;

/**
 * The core {@link Spacer}. It measures as 0 by 0, so its fixed size, minimum size or slot decides how big it is, and
 * it draws nothing. It stretches on both axes by default, so
 * it fills a weighted share without being told to.
 */
public final class SpacerImpl extends BuiltInComponent<Spacer> implements Spacer {

    /**
     * Creates a stretched spacer with no size.
     *
     * @param ui the factory that owns the spacer
     */
    @SuppressWarnings("this-escape")
    public SpacerImpl(final ComponentFactoryImpl ui) {
        super(ui);
        this.setAnchor(Anchor.FILL);
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        return Size.ZERO;
    }
}
