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

import org.inspirenxe.strata.core.node.Node;

/**
 * The checks a container makes before it takes a child, made ahead of time by a control that swaps one part for
 * another, so a part it cannot take leaves the control as it was.
 */
public final class Adoption {
    private Adoption() {
    }

    /**
     * Checks that a parent can take a child: the child has no parent, is not a host's content, and is neither the
     * parent nor one of its ancestors.
     *
     * @param parent the node of the control taking the child
     * @param child the child's node
     * @throws IllegalArgumentException if the parent cannot take the child
     */
    public static void require(final Node parent, final Node child) {
        if (child.parentNode() != null) {
            throw new IllegalArgumentException(child.facade() + " already belongs to " + child.parentNode().facade());
        }
        if (child.root() != null) {
            throw new IllegalArgumentException(child.facade() + " is the content of a host and cannot be added");
        }
        for (Node node = parent; node != null; node = node.parentNode()) {
            if (node == child) {
                throw new IllegalArgumentException("Adding " + child.facade() + " to " + parent.facade()
                        + " would create a cycle");
            }
        }
    }
}
