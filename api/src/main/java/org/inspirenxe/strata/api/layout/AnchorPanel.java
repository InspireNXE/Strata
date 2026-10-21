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
package org.inspirenxe.strata.api.layout;

import org.inspirenxe.strata.api.component.Container;

/**
 * A container that gives every child the whole content area (its bounds minus padding). Each child then places
 * itself by its own anchor or alignment, so children overlap: use it to pin a title to the top, a close button to a
 * corner and a dialog to the center of the same area. Where children overlap, they are drawn in the panel's
 * {@linkplain org.inspirenxe.strata.api.component.Container#paintOrder() drawing order}: later ones on top, until
 * a raise, a lower or a z index changes it.
 *
 * <p>Its desired size is the largest child's size plus margin, plus the padding. A screen's root is usually an
 * {@code AnchorPanel} anchored with {@link Anchor#FILL}, so it covers the whole host.</p>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#anchorPanel()}. Do not implement this
 * interface.</p>
 */
public interface AnchorPanel extends Container<AnchorPanel> {
}
