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
 * What the clipboard and drag and drop carry: a {@link org.inspirenxe.strata.api.transfer.Transfer} holds one
 * payload in one or more {@link org.inspirenxe.strata.api.transfer.DataFormat}s, and a drop takes it with a
 * {@link org.inspirenxe.strata.api.transfer.DropEffect}. Read and write the clipboard through
 * {@code ComponentFactory.clipboard()} or a component's {@code Environment}.
 */
package org.inspirenxe.strata.api.transfer;
