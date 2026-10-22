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
package org.inspirenxe.strata.core;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.control.collection.ListView;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.event.input.InputEvents;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.inspirenxe.strata.api.screen.Screen;
import org.inspirenxe.strata.api.transfer.DataFormat;
import org.inspirenxe.strata.api.transfer.DropEffect;
import org.inspirenxe.strata.api.transfer.Transfer;
import org.inspirenxe.strata.testing.Matching;
import org.inspirenxe.strata.testing.ScreenTester;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The test kit's drag and drop helpers (design 4.48), used the way an application's tests would.
 */
class ScreenTesterDragTest {
    /**
     * A queue whose rows the user reorders, and a drop zone for files.
     */
    static final class QueueScreen implements Screen {
        final List<Path> imported = new ArrayList<>();

        @Override
        public Component<?> build(final ComponentFactory ui) {
            final ListView<String> queue = ui.<String>listView().setItems(List.of("ana", "ben", "cleo"))
                    .setDragTransfer(rows -> Transfer.text(String.join(",", rows)));
            queue.setId("queue").setSize(200, 80);
            final Label zone = ui.label("Drop files here");
            zone.setId("zone");
            zone.onEvent(InputEvents.DRAG_OVER, event -> {
                if (event.transfer().has(DataFormat.FILES)) {
                    event.accept(DropEffect.COPY);
                }
            });
            zone.onEvent(InputEvents.DROP, event -> event.transfer().get(DataFormat.FILES).ifPresent(this.imported::addAll));
            return ui.stackPanel().add(queue).add(zone);
        }
    }

    @Test
    void aRowDraggedOntoAnotherMovesInFrontOfIt() {
        try (ScreenTester tester = ScreenTester.open(new QueueScreen())) {
            @SuppressWarnings("unchecked")
            final ListView<String> queue = tester.byId("queue", ListView.class);
            final Label cleo = tester.find(Label.class, Matching.text("cleo"));
            final Label ana = tester.find(Label.class, Matching.text("ana"));

            // The middle of a row rounds to the gap below it, so cleo lands after ana
            tester.dragTo(cleo, ana);

            assertEquals(List.of("ana", "cleo", "ben"), queue.items());
        }
    }

    @Test
    void filesFromAnotherApplicationDropOnTheZone() {
        final QueueScreen screen = new QueueScreen();
        try (ScreenTester tester = ScreenTester.open(screen)) {
            final Label zone = tester.byId("zone", Label.class);
            assertEquals(Optional.of(DropEffect.COPY), tester.drop(zone, Transfer.files(List.of(Path.of("a.csv")))));
            assertEquals(List.of(Path.of("a.csv")), screen.imported);
            assertEquals(Optional.empty(), tester.drop(zone, Transfer.text("not files")));
        }
    }

    @Test
    void aSourceThatCannotBePressedFails() {
        try (ScreenTester tester = ScreenTester.open(new QueueScreen())) {
            final Label zone = tester.byId("zone", Label.class);
            final StackPanel panel = tester.find(StackPanel.class, component -> true);
            zone.setEnabled(false);
            assertThrows(AssertionError.class, () -> tester.dragTo(zone, panel));
        }
    }
}
