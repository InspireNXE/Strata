package org.inspirenxe.strata.examples;

import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.ThemeException;
import org.inspirenxe.strata.api.theme.Themes;
import org.inspirenxe.strata.examples.adapter.ToyAdapter;
import org.inspirenxe.strata.examples.controls.ControlExamples;
import org.inspirenxe.strata.examples.events.EventExamples;
import org.inspirenxe.strata.examples.extending.ExtendingExamples;
import org.inspirenxe.strata.examples.layout.LayoutExamples;
import org.inspirenxe.strata.examples.quickstart.SettingsScreen;
import org.inspirenxe.strata.examples.screens.ScreenExamples;
import org.inspirenxe.strata.examples.theming.ThemeExamples;
import org.inspirenxe.strata.swing.SwingSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that the documentation's examples do what the pages say.
 */
class ExamplesTest {

    @Test
    void everyExampleScreenOpensWithItsTheme() {
        final Theme theme = ExtendingExamples.checkBoxTheme();
        for (final var screen : List.of(new SettingsScreen(), LayoutExamples.anchorPanel(), LayoutExamples.stackPanel(),
                LayoutExamples.gridPanel(), LayoutExamples.weightedGrid(), LayoutExamples.spacer(),
                LayoutExamples.absolutePanel(), LayoutExamples.marginAndPadding(), LayoutExamples.wrapping(),
                LayoutExamples.sizes(), EventExamples.dragging(), new EventExamples.ConfirmScreen(),
                ExtendingExamples.customComponents(), new ScreenExamples.ShoppingList(), new ScreenExamples.TitleScreen(),
                ThemeExamples.sampler(0xFF2B2D31), ControlExamples.controls(), LayoutExamples.frame(),
                LayoutExamples.spread(), ControlExamples.optionRows())) {
            SwingSnapshot.render(screen, theme, 320, 200, 1f);
        }
        SwingSnapshot.render(ThemeExamples.sampler(0xFF2B2D31), ThemeExamples.derived(), 320, 200, 1f);
        SwingSnapshot.render(ThemeExamples.sampler(0xFF2B2D31), ThemeExamples.fromScratch(), 320, 200, 1f);
    }

    @Test
    void anEmptyThemeListsEveryMissingKey() {
        final ThemeException e = assertThrows(ThemeException.class,
                () -> SwingSnapshot.render(new SettingsScreen(), Theme.builder("empty").build(), 320, 200, 1f));
        assertTrue(e.missingKeys().containsAll(List.of("button.normal", "button.hovered", "button.pressed",
                "button.disabled", "textbox.normal", "textbox.focused", "textbox.disabled")));
    }

    @Test
    void theSwitchNeedsItsKeys() {
        final ThemeException e = assertThrows(ThemeException.class,
                () -> SwingSnapshot.render(ExtendingExamples.customComponents(), Themes.flat(), 320, 200, 1f));
        assertEquals(java.util.Set.of("examplemod.switch.track", "examplemod.switch.track.on", "examplemod.switch.knob"),
                e.missingKeys());
    }

    @Test
    void theToyAdapterDrawsAndTakesInput() {
        final ToyAdapter adapter = new ToyAdapter(Themes.flat(), 2f);
        adapter.onResize(640, 400);
        adapter.open(new SettingsScreen());
        final List<String> commands = adapter.onFrame();
        assertTrue(commands.stream().anyMatch(command -> command.startsWith("text 'Save'")), commands::toString);

        // Tab focuses the text box, the first tabbable component; typing goes into it
        assertTrue(adapter.onKey(9, true, 0));
        assertTrue(adapter.onChar('H'));
        assertTrue(adapter.onChar('i'));
        assertTrue(adapter.onFrame().stream().anyMatch(command -> command.startsWith("text 'Hi'")));
    }
}
