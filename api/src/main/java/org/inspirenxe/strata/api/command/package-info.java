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
/**
 * Commands, which buttons, menu items and keys share, and the shortcuts that run them.
 *
 * <p>A {@link org.inspirenxe.strata.api.command.Command} is a value with a caption, an optional key stroke and a
 * source that says whether it can run. Controls show it through
 * {@link org.inspirenxe.strata.api.control.attribute.command.HasCommand}, and a host runs it from its key stroke once
 * it is added to the host's {@link org.inspirenxe.strata.api.command.Shortcuts}.</p>
 */
package org.inspirenxe.strata.api.command;
