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
package org.inspirenxe.strata.api.control.pane;

import org.inspirenxe.strata.api.control.attribute.text.HasText;

/**
 * A caption over a group of items in a {@link NavigationView}'s pane, drawn in the view's header text style. The user
 * cannot pick it, the keyboard passes over it, and it is hidden while the pane shows as a column of icons.
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#navigationHeader(String)} or
 * {@link NavigationView#addHeader(String)}. Do not implement this interface.</p>
 */
public non-sealed interface NavigationHeader extends NavigationEntry, HasText<NavigationHeader> {
}
