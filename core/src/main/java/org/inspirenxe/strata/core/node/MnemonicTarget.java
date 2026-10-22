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
package org.inspirenxe.strata.core.node;

/**
 * A built-in component whose caption has a mnemonic that Alt with the letter acts on, as {@link Root} routes it.
 * The mnemonics are those of the captions as last measured, in lower case.
 */
public interface MnemonicTarget {
    /**
     * Returns whether one of the component's captions has a mnemonic.
     *
     * @param mnemonic the mnemonic in lower case
     * @return true if Alt with it acts on this component
     */
    boolean hasMnemonic(int mnemonic);

    /**
     * Acts for the user as Alt with the mnemonic does: presses a button, toggles a box, selects a tab, opens a menu
     * bar's menu. The root has already focused the component when it can take focus.
     *
     * @param mnemonic the mnemonic in lower case, one {@link #hasMnemonic} said yes to
     */
    void activateMnemonic(int mnemonic);
}
