package org.inspirenxe.strata.examples.snapshots;

import org.inspirenxe.strata.api.component.ComponentSnapshot;
import org.inspirenxe.strata.api.control.attribute.text.TextInput;
import org.inspirenxe.strata.api.control.attribute.value.Toggle;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.button.CheckBox;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.event.input.InputEvents;
import org.inspirenxe.strata.api.layout.StackPanel;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * The snapshot guide's examples.
 */
public final class SnapshotExamples {

    private SnapshotExamples() {
    }

    static Deque<TextBox.State> undo(final TextBox name) {
        // --8<-- [start:compare]
        // Every edit asks first, so the snapshot holds the text, caret and selection from before it
        final Deque<TextBox.State> undo = new ArrayDeque<>();
        name.onEvent(TextInput.EDIT, event ->
                event.snapshot().state(TextBox.State.class).ifPresent(undo::push));
        // --8<-- [end:compare]
        return undo;
    }

    static void common(final Button save) {
        // --8<-- [start:common]
        save.onEvent(InputEvents.CLICK, event -> {
            final ComponentSnapshot before = event.snapshot();
            // Every snapshot has what every component has: flags, layout inputs and bounds
            System.out.println("Clicked at " + before.bounds() + ", focused: " + before.focused());
        });
        // --8<-- [end:common]
    }

    static void ancestor(final StackPanel palette) {
        // --8<-- [start:ancestor]
        // A handler on a container: the snapshots cover the target and this container, not the other children
        palette.onEvent(InputEvents.POINTER_PRESS, event -> {
            final ComponentSnapshot target = event.snapshot();
            final ComponentSnapshot self = event.currentSnapshot();
            System.out.println(target.component() + " pressed while " + self.component() + " was at " + self.bounds());
        });
        // --8<-- [end:ancestor]
    }

    static List<CheckBox.State> history(final CheckBox box) {
        // --8<-- [start:keep]
        // Snapshots stay valid after the event, so they can be kept for undo or logging
        final List<CheckBox.State> history = new ArrayList<>();
        box.onEvent(Toggle.TOGGLE, event ->
                event.snapshot().state(CheckBox.State.class).ifPresent(history::add));
        // --8<-- [end:keep]
        return history;
    }

    static void previous(final TextBox name) {
        // --8<-- [start:previous]
        // A Post event fires after its change, so it carries the value from before instead of a snapshot
        name.onEvent(TextInput.EDITED, event ->
                System.out.println("\"" + event.previous() + "\" became \"" + event.value() + "\""));
        // --8<-- [end:previous]
    }
}
