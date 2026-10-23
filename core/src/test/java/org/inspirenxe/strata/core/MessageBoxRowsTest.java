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
import org.inspirenxe.strata.api.control.attribute.value.Toggle;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.button.CheckBox;
import org.inspirenxe.strata.api.control.display.InfoBar;
import org.inspirenxe.strata.api.control.window.MessageBox;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.Themes;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A message box's own rows of results, its severity's icon and the content under its message (design 4.57).
 */
class MessageBoxRowsTest {
    private static final MessageBox.Result SAVE = MessageBox.Result.of("save", "Save");
    private static final MessageBox.Result DISCARD = MessageBox.Result.of("discard", "Don't save");

    private final TestHost test = new TestHost(400, 300);
    private final ComponentFactory ui = this.test.factory();
    private final List<MessageBox.Result> answers = new ArrayList<>();

    private MessageBox open(final MessageBox box) {
        this.test.show(this.ui.button("Target").setAnchor(Anchor.TOP_LEFT));
        box.onEvent(MessageBox.ANSWERED, event -> this.answers.add(event.value()));
        this.ui.overlays().dialog(box);
        this.test.host().layout();
        return box;
    }

    private Button button(final String caption) {
        return this.test.tabOrder().stream().filter(component -> component instanceof Button)
                .map(component -> (Button) component).filter(button -> button.text().plain().equals(caption))
                .findFirst().orElseThrow();
    }

    private void clickCenter(final Component<?> component) {
        final Rect bounds = component.bounds();
        this.test.click(bounds.x() + bounds.width() / 2f, bounds.y() + bounds.height() / 2f);
    }

    @Test
    void resultsAreEqualByIdAndTheCommonOnesKeepTheirCaptions() {
        assertEquals(MessageBox.Result.OK, MessageBox.Result.of("ok", "Fine"));
        assertEquals(MessageBox.Result.OK.hashCode(), MessageBox.Result.of("ok", Text.of("x")).hashCode());
        assertNotEquals(MessageBox.Result.OK, MessageBox.Result.CANCEL);
        assertEquals("yes", MessageBox.Result.YES.id());
        assertEquals(List.of(MessageBox.Result.OK), this.ui.messageBox().results());
        assertThrows(IllegalArgumentException.class, () -> MessageBox.Result.of("", "Empty"));
    }

    @Test
    void anyRowOfResultsAnswersAndItsDismissResultIsTheCancelButton() {
        final MessageBox box = this.open(this.ui.messageBox("Plan", "Save changes to Plan?", MessageBox.Buttons.OK)
                .setButtons(List.of(SAVE, DISCARD, MessageBox.Result.CANCEL), MessageBox.Result.CANCEL));
        assertEquals(List.of(SAVE, DISCARD, MessageBox.Result.CANCEL), box.results());
        assertEquals(MessageBox.Result.CANCEL, box.dismissResult());
        assertEquals(SAVE, box.defaultResult());
        assertTrue(this.button("Cancel").isCancel());
        assertTrue(this.button("Save").isDefault());
        this.clickCenter(this.button("Don't save"));
        assertEquals(List.of(DISCARD), this.answers);
    }

    @Test
    void escapeAnswersADismissResultThatHasNoButton() {
        final MessageBox.Result later = MessageBox.Result.of("later", "Later");
        this.open(this.ui.messageBox().setButtons(List.of(SAVE, DISCARD), later));
        this.test.host().key(Key.ESCAPE, KeyAction.PRESS, Modifiers.NONE);
        assertEquals(List.of(later), this.answers);
    }

    @Test
    void aCaptionSetForAResultIsKeptByIdAndTheRowIsChecked() {
        final MessageBox box = this.ui.messageBox().setButtonText(MessageBox.Result.of("save", "ignored"), "Store");
        this.open(box.setButtons(List.of(SAVE), SAVE));
        assertEquals("Store", this.button("Store").text().plain());
        assertThrows(IllegalArgumentException.class, () -> box.setButtons(List.of(), SAVE));
        assertThrows(IllegalArgumentException.class,
                () -> box.setButtons(List.of(SAVE, MessageBox.Result.of("save", "Again")), SAVE));
        assertThrows(IllegalArgumentException.class, () -> box.setDefaultResult(DISCARD));
    }

    @Test
    void aSeverityDrawsItsIconFromTheBoxKeyOrTheInfoBarKey() {
        final Theme theme = Themes.flat().toBuilder()
                .paint(MessageBox.STYLE_ICON_WARNING, Paint.solid(0xFFFFAA00))
                .paint(InfoBar.STYLE_ICON_ERROR, Paint.solid(0xFFFF0000))
                .build();
        final TestHost host = new TestHost(new TestPlatform(), theme, 400, 300);
        final MessageBox box = host.factory().messageBox("Disk", "The disk is almost full.", MessageBox.Buttons.OK);
        host.show(host.factory().button("Target").setAnchor(Anchor.TOP_LEFT));
        host.factory().overlays().dialog(box);
        final float plainHeight = box.window().bounds().height();
        assertEquals(Optional.empty(), box.severity());
        box.setSeverity(InfoBar.Severity.WARNING);
        host.host().layout();
        DrawLog log = new DrawLog();
        host.host().render(log);
        assertTrue(log.lines().stream().anyMatch(line -> line.startsWith("fill") && line.endsWith("24x24 #FFFFAA00")),
                log.lines().toString());
        assertTrue(box.window().bounds().height() >= plainHeight);
        box.setSeverity(InfoBar.Severity.ERROR);
        log = new DrawLog();
        host.host().render(log);
        assertTrue(log.lines().stream().anyMatch(line -> line.endsWith("24x24 #FFFF0000")));
        final float width = box.window().bounds().width();
        box.setSeverity(InfoBar.Severity.SUCCESS);
        host.host().layout();
        assertTrue(box.window().bounds().width() <= width);
    }

    @Test
    void contentSitsUnderTheMessageAndItsEventsPassTheBox() {
        final CheckBox again = this.ui.checkBox("Don't ask again");
        final List<Boolean> heard = new ArrayList<>();
        final MessageBox box = this.ui.messageBox("Delete", "Delete it?", MessageBox.Buttons.YES_NO).setContent(again);
        final var around = this.ui.stackPanel().add(box).onEvent(Toggle.TOGGLED, event -> heard.add(event.value()));
        assertEquals(Optional.of(again), box.content());
        this.test.show(around.setAnchor(Anchor.FILL));
        this.test.host().layout();
        assertTrue(again.bounds().y() > box.window().bounds().y());
        this.clickCenter(again);
        assertEquals(List.of(true), heard);
        final Button yes = (Button) this.test.tabOrder().stream()
                .filter(component -> component instanceof Button button && button.text().plain().equals("Yes"))
                .findFirst().orElseThrow();
        assertTrue(again.bounds().bottom() <= yes.bounds().y());
        box.setContent(null);
        assertTrue(again.parent().isEmpty());
    }
}
