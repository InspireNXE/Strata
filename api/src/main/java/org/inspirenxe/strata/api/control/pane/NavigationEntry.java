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

/**
 * One entry of a {@link NavigationView}'s pane: a {@link NavigationItem} the user can pick, a
 * {@link NavigationHeader} caption over a group of items, or a {@link NavigationSeparator} line.
 *
 * <p>Entries are made by the {@link org.inspirenxe.strata.api.ComponentFactory}, or by the view's shorthands. They
 * are mutable, and a change to one lays out or redraws its view. An entry belongs to at most one view, or for an
 * item one parent item, at a time. Entries are not components and not event targets; the view fires events
 * for them. The view's selection and activation events carry an item's index in {@link NavigationView#allItems()},
 * and {@link NavigationView#EXPAND_ITEM} and {@link NavigationView#ITEM_EXPANDED} carry the item.</p>
 *
 * <p>Call entries on the UI thread. Do not implement this interface.</p>
 */
public sealed interface NavigationEntry permits NavigationItem, NavigationHeader, NavigationSeparator {
}
