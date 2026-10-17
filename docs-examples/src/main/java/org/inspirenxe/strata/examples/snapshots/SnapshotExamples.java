package org.inspirenxe.strata.examples.snapshots;

import org.inspirenxe.strata.api.component.ComponentSnapshot;
import org.inspirenxe.strata.api.event.EventTypes;
import org.inspirenxe.strata.api.event.Phase;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.inspirenxe.strata.api.widget.Button;
import org.inspirenxe.strata.api.widget.CheckBox;
import org.inspirenxe.strata.api.widget.Label;
import org.inspirenxe.strata.api.widget.TextBox;

import java.util.ArrayList;
import java.util.List;

/**
 * The snapshot guide's examples.
 */
public final class SnapshotExamples {

    private SnapshotExamples() {
    }

    static void beforeAndAfter(final TextBox name, final Label log) {
        // --8<-- [start:compare]
        // onWithSnapshot instead of on: Strata records the text box before anything handles the key
        name.onWithSnapshot(EventTypes.KEY, Phase.AFTER, event -> {
            final TextBox.State before = event.snapshot().state(TextBox.State.class).orElseThrow();
            if (!before.text().equals(name.text())) {
                log.setText("\"" + before.text() + "\" became \"" + name.text() + "\"");
            } else if (before.caret() != name.caret()) {
                log.setText("Caret moved from " + before.caret() + " to " + name.caret());
            }
        });
        // --8<-- [end:compare]
    }

    static void common(final Button save) {
        // --8<-- [start:common]
        // The button disables itself when pressed; the AFTER handler sees it before and after
        save.onAction(() -> save.setEnabled(false));
        save.onWithSnapshot(EventTypes.CLICK, Phase.AFTER, event -> {
            final ComponentSnapshot before = event.snapshot();
            // Every snapshot has what every component has: flags, layout inputs and bounds
            if (before.enabled() && !save.isEnabled()) {
                System.out.println("This click disabled the button at " + before.bounds());
            }
        });
        // --8<-- [end:common]
    }

    static void ancestor(final StackPanel form) {
        // --8<-- [start:ancestor]
        // A handler on a container: the snapshots cover the target and this container, not the other children
        form.onWithSnapshot(EventTypes.CHAR, Phase.AFTER, event -> {
            final ComponentSnapshot target = event.snapshot();
            final ComponentSnapshot self = event.currentSnapshot();
            System.out.println(target.component() + " typed while " + self.component() + " was at " + self.bounds());
            // Any other component on the path, or off it, has no snapshot
            System.out.println(event.snapshotOf(form.children().getFirst()).isPresent());
        });
        // --8<-- [end:ancestor]
    }

    static List<CheckBox.State> history(final CheckBox box) {
        // --8<-- [start:keep]
        // Snapshots stay valid after the event, so they can be kept for undo or logging
        final List<CheckBox.State> history = new ArrayList<>();
        box.onWithSnapshot(EventTypes.CLICK, event ->
                event.snapshot().state(CheckBox.State.class).ifPresent(history::add));
        // --8<-- [end:keep]
        return history;
    }
}
