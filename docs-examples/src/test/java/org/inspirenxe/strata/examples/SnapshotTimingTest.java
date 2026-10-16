package org.inspirenxe.strata.examples;

import org.inspirenxe.strata.api.component.ComponentSnapshot;
import org.inspirenxe.strata.api.event.EventTypes;
import org.inspirenxe.strata.api.event.Phase;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.theme.Themes;
import org.inspirenxe.strata.api.widget.Button;
import org.inspirenxe.strata.api.widget.TextBox;
import org.inspirenxe.strata.core.StrataCore;
import org.inspirenxe.strata.examples.adapter.ToyPlatform;
import org.inspirenxe.strata.spi.ScreenHost;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks what the snapshot guide says about when snapshots are taken.
 */
class SnapshotTimingTest {
    private ScreenHost host;
    private TextBox box;
    private Button button;

    @BeforeEach
    void open() {
        this.host = StrataCore.createScreenHost(new ToyPlatform(), Themes.flat());
        this.host.resize(200, 100);
        this.host.open(factory -> {
            this.box = factory.textBox().setText("ab");
            this.button = factory.button("Go");
            return factory.stackPanel().add(this.box).add(this.button).setAnchor(Anchor.FILL);
        });
        this.host.layout();
    }

    @Test
    void aPressSnapshotAlreadyShowsFocusAndPress() {
        final List<ComponentSnapshot> taken = new ArrayList<>();
        this.box.onWithSnapshot(EventTypes.POINTER_PRESS, event -> taken.add(event.snapshot()));
        final Rect bounds = this.box.bounds();
        this.host.pointerButton(bounds.x() + 2, bounds.y() + 2, MouseButton.LEFT, true, Modifiers.NONE);
        assertTrue(taken.getFirst().focused());
        assertTrue(taken.getFirst().pressed());
    }

    @Test
    void aKeySnapshotHoldsTheTextBeforeTheEdit() {
        final List<TextBox.State> before = new ArrayList<>();
        this.box.onWithSnapshot(EventTypes.KEY, Phase.AFTER,
                event -> event.snapshot().state(TextBox.State.class).ifPresent(before::add));
        this.box.requestFocus();
        this.host.key(Key.BACKSPACE, KeyAction.PRESS, Modifiers.NONE);
        assertEquals("ab", before.getFirst().text());
        assertEquals("a", this.box.text());
    }

    @Test
    void aClickSnapshotHoldsTheStateBeforeTheAction() {
        final List<ComponentSnapshot> taken = new ArrayList<>();
        this.button.onAction(() -> this.button.setEnabled(false));
        this.button.onWithSnapshot(EventTypes.CLICK, Phase.AFTER, event -> taken.add(event.snapshot()));
        final Rect bounds = this.button.bounds();
        this.host.pointerButton(bounds.x() + 2, bounds.y() + 2, MouseButton.LEFT, true, Modifiers.NONE);
        this.host.pointerButton(bounds.x() + 2, bounds.y() + 2, MouseButton.LEFT, false, Modifiers.NONE);
        assertTrue(taken.getFirst().enabled());
        assertFalse(taken.getFirst().pressed());
        assertFalse(this.button.isEnabled());
    }
}
