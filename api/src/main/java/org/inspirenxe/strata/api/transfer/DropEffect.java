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
package org.inspirenxe.strata.api.transfer;

/**
 * What a drop does with what was dragged. The source of a drag says which effects it allows, and the target takes
 * one of them.
 */
public enum DropEffect {
    /** The target takes a copy, and the source keeps what it had. */
    COPY,
    /** The target takes it, and the source removes what it gave away. */
    MOVE,
    /** The target keeps a reference to it, and the source keeps what it had. */
    LINK
}
