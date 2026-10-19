package org.inspirenxe.strata.core;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.control.attribute.item.SingleSelection;
import org.inspirenxe.strata.api.control.attribute.value.Ranged;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.collection.ListView;
import org.inspirenxe.strata.api.control.field.Slider;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.AnchorPanel;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.core.host.ScreenHostImpl;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Rows of controls in a list view, the way an options screen builds them: each cell is a row with a label on the
 * left and a control on the right. The controls work as anywhere else, and their events are heard around the list.
 */
class OptionRowsTest {
    private final TestHost test = new TestHost(300, 200);
    private final ComponentFactory ui = this.test.factory();
    private final Map<String, Button> buttons = new HashMap<>();
    private final Map<String, Slider> sliders = new HashMap<>();
    private final List<String> heard = new ArrayList<>();
    private ListView<String> list;

    /**
     * Shows a 200 wide list with one row per name: "Volume" rows get a slider, the others a reset button.
     */
    private void show(final String... names) {
        final ComponentFactory ui = this.ui;
        this.list = ui.<String>listView(name -> {
                    final Component<?> control;
                    if (name.startsWith("Volume")) {
                        final Slider slider = ui.slider().setRange(0, 100).setValue(50);
                        slider.setSize(100, 20);
                        this.sliders.put(name, slider);
                        control = slider;
                    } else {
                        final Button button = ui.button("Reset");
                        this.buttons.put(name, button);
                        control = button.setAnchor(Anchor.MIDDLE_LEFT);
                    }
                    return ui.stackPanel(Orientation.HORIZONTAL)
                            .add(ui.label(name).setAnchor(Anchor.MIDDLE_LEFT))
                            .add(ui.spacer(), 1)
                            .add(control)
                            .setAnchor(Anchor.FILL);
                })
                .setItems(List.of(names));
        this.list.setSize(200, 100);
        final AnchorPanel form = ui.anchorPanel()
                .add(this.list)
                .setAnchor(Anchor.FILL)
                .onEvent(Button.PRESSED, event -> this.heard.add("pressed " + this.nameOf(event.target())))
                .onEvent(Ranged.CHANGED, event -> this.heard.add("changed " + event.value()));
        this.test.show(form);
    }

    private String nameOf(final Component<?> control) {
        return this.buttons.entrySet().stream()
                .filter(entry -> entry.getValue() == control)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("?");
    }

    private void clickCenter(final Component<?> component) {
        final Rect bounds = component.bounds();
        final float x = bounds.x() + bounds.width() / 2f;
        final float y = bounds.y() + bounds.height() / 2f;
        this.test.host().pointerMoved(x, y, Modifiers.NONE);
        this.test.click(x, y);
    }

    @Test
    void aButtonInARowPressesAndIsHeardAroundTheList() {
        this.show("Music", "Sound", "Voices");
        final Button sound = this.buttons.get("Sound");
        // The spacer pushes the button to the right end of its row
        assertEquals(sound.parent().bounds().right(), sound.bounds().right());

        this.clickCenter(sound);
        assertEquals(List.of("pressed Sound"), this.heard);
        // The press reached the list too, so the row is selected, and the button kept its cell
        assertEquals(1, this.list.selectedIndex());
        assertSame(sound, this.buttons.get("Sound"));
    }

    @Test
    void aSliderInARowDragsAndIsHeardAroundTheList() {
        this.show("Volume", "Sound");
        final Slider volume = this.sliders.get("Volume");
        final Rect bounds = volume.bounds();
        final float y = bounds.y() + bounds.height() / 2f;
        final ScreenHostImpl host = this.test.host();
        host.pointerMoved(bounds.x() + bounds.width() / 2f, y, Modifiers.NONE);
        host.pointerButton(bounds.x() + bounds.width() / 2f, y, MouseButton.LEFT, true, Modifiers.NONE);
        host.pointerMoved(bounds.right() + 20, y, Modifiers.NONE);
        host.pointerButton(bounds.right() + 20, y, MouseButton.LEFT, false, Modifiers.NONE);

        assertEquals(100, volume.value());
        assertEquals(List.of("changed 100.0"), this.heard);
        // The slider consumed the press, so the list left the selection alone
        assertEquals(-1, this.list.selectedIndex());
    }

    @Test
    void rowsCanStayUnselectedWhileTheirControlsWork() {
        this.show("Music", "Sound");
        this.list.onEvent(SingleSelection.SELECT, event -> event.cancel());

        this.clickCenter(this.buttons.get("Music"));
        // On the second row's label
        final Rect bounds = this.list.bounds();
        this.test.click(bounds.x() + 10, bounds.y() + 30);

        assertEquals(List.of("pressed Music"), this.heard);
        assertEquals(-1, this.list.selectedIndex());
    }
}
