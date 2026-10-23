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
package org.inspirenxe.strata.core.control.field;

import org.inspirenxe.strata.api.command.Command;
import org.inspirenxe.strata.api.control.menu.Menu;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyStroke;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;

import java.util.function.BooleanSupplier;

/**
 * Builds the default context menu of the text fields: Undo and Redo, Cut, Copy and Paste, Delete, and Select all,
 * captioned from {@code strata.edit.*} keys and showing their keys as hints. Each item is enabled only while it
 * would do something. A field for secrets leaves out Undo, Redo, Cut and Copy.
 *
 * <p>Opening the menu takes focus from the field, and focus coming back moves the field's caret, so the menu keeps
 * the field's selection from the moment it opened and puts it back when an item is chosen, before its command runs. Each item acts as the user,
 * through the field's editor, so the field's requests fire as they do for the keys.</p>
 */
final class TextMenus {
    private TextMenus() {
    }

    /**
     * Makes the menu for one field.
     *
     * @param ui the field's factory
     * @param editor the field's editor
     * @param settle reports a moved caret after an item acted, as the field does after a key
     * @param canUndo whether the field has an edit to undo, or null for a field without history
     * @param canRedo whether the field has an edit to redo, or null for a field without history
     * @return the menu, closed
     */
    static Menu create(final ComponentFactoryImpl ui, final TextEditor editor, final Runnable settle,
                       final BooleanSupplier canUndo, final BooleanSupplier canRedo) {
        final Menu menu = ui.menu();
        final int[] saved = new int[2];
        menu.onEvent(Menu.OPENED, event -> {
            saved[0] = editor.anchor();
            saved[1] = editor.caret();
        });
        // Focus coming back to the field moved its caret; the selection goes back before the item's command runs
        menu.onEvent(Menu.CHOSEN, event -> editor.select(saved[0], saved[1]));
        if (canUndo != null && canRedo != null) {
            menu.addItem(ui.menuItem().setCommand(item("undo", KeyStroke.shortcut(Key.Z), canUndo, settle,
                    editor::undoEdit)));
            menu.addItem(ui.menuItem().setCommand(item("redo", KeyStroke.shortcut(Key.Y), canRedo, settle,
                    editor::redoEdit)));
            menu.addSeparator();
            menu.addItem(ui.menuItem().setCommand(item("cut", KeyStroke.shortcut(Key.X), editor::canCut, settle,
                    editor::cutSelection)));
            menu.addItem(ui.menuItem().setCommand(item("copy", KeyStroke.shortcut(Key.C), editor::canCopy, settle,
                    editor::copySelection)));
        }
        menu.addItem(ui.menuItem().setCommand(item("paste", KeyStroke.shortcut(Key.V), editor::canPaste, settle,
                editor::pasteOverSelection)));
        menu.addItem(ui.menuItem().setCommand(item("delete", KeyStroke.of(Key.DELETE), editor::canDelete, settle,
                editor::deleteSelected)));
        menu.addSeparator();
        menu.addItem(ui.menuItem().setCommand(item("select_all", KeyStroke.shortcut(Key.A), editor::canSelectAll,
                settle, editor::selectEverything)));
        return menu;
    }

    private static Command.Action item(final String name, final KeyStroke stroke, final BooleanSupplier enabled,
                                       final Runnable settle, final Runnable act) {
        return Command.of(Text.translatable("strata.edit." + name), () -> {
            act.run();
            settle.run();
        }).withShortcut(stroke).withEnabled(enabled::getAsBoolean);
    }
}
