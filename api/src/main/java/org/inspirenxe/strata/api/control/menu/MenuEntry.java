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
package org.inspirenxe.strata.api.control.menu;

/**
 * One entry of a {@link Menu}: a {@link MenuItem} that runs a command, a {@link CheckMenuItem} that flips a setting,
 * a {@link SubmenuItem} that opens another menu, or a {@link MenuSeparator} line. A {@link MenuBar}'s titles are
 * {@link SubmenuItem}s too.
 *
 * <p>An entry is made by the {@link org.inspirenxe.strata.api.ComponentFactory}, or by a menu's shorthands such as
 * {@link Menu#addItem(String, Runnable)}. It belongs to at most one menu or bar at a time. It is mutable: a change
 * to it lays out or redraws the menu or bar it is in. Entries are not components and have no events of their own;
 * the menu's {@link Menu#CHOOSE} and {@link Menu#CHOSEN} carry the entry chosen.</p>
 *
 * <p>Call entries on the UI thread. Do not implement this interface.</p>
 */
public sealed interface MenuEntry permits MenuItem, CheckMenuItem, SubmenuItem, MenuSeparator {
}
